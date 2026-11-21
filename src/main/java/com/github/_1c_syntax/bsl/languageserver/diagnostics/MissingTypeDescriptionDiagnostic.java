/*
 * This file is a part of BSL Language Server.
 *
 * Copyright (c) 2018-2026
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Fedkin <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver.diagnostics;

import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.references.ReferenceIndex;
import com.github._1c_syntax.bsl.languageserver.references.model.OccurrenceType;
import com.github._1c_syntax.bsl.languageserver.utils.Trees;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.parser.description.MethodDescription;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 5,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class MissingTypeDescriptionDiagnostic extends AbstractSymbolTreeDiagnostic {

  private static final Pattern DESCRIPTION_LINK = Pattern.compile("(?iu)(?:см\\.|see)\\h+[^\\r\\n]+");

  private final ReferenceIndex referenceIndex;

  @Override
  public void visitMethod(MethodSymbol method) {
    var description = method.getDescription();
    if (description.filter(MissingTypeDescriptionDiagnostic::delegatesDescription).isPresent()) {
      return;
    }
    if (method.isExport() || !hasDirectLocalCall(method)) {
      for (var parameter : method.getParameters()) {
        if (parameter.getDescription().filter(value -> !value.types().isEmpty()).isEmpty()) {
          diagnosticStorage.addDiagnostic(parameter.getRange(), info.getMessage(parameter.getName()));
        }
      }
    }
    if (method.isExport() && method.isFunction()
      && description.map(MethodDescription::getReturnedValue).filter(types -> !types.isEmpty()).isEmpty()) {
      diagnosticStorage.addDiagnostic(method.getSubNameRange(), info.getResourceString("missingReturnType"));
    }
  }

  private static boolean delegatesDescription(MethodDescription description) {
    return description.getParameters().isEmpty() && description.getReturnedValue().isEmpty()
      && !description.getLinks().isEmpty() && DESCRIPTION_LINK.matcher(description.getPurposeDescription()).matches();
  }

  private boolean hasDirectLocalCall(MethodSymbol method) {
    return referenceIndex.getReferencesTo(method).stream()
      .filter(reference -> reference.occurrenceType() == OccurrenceType.REFERENCE
        && reference.uri().equals(documentContext.getUri()) && !reference.from().equals(method))
      .anyMatch(reference -> Trees.findTerminalNodeContainsPosition(documentContext.getAst(),
          reference.selectionRange().getStart())
        .filter(node -> node.getParent() instanceof BSLParser.MethodNameContext)
        .isPresent());
  }
}
