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

import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraphIndex;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.compilation.CompilationContextResolver;
import lombok.RequiredArgsConstructor;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.Location;

import java.util.ArrayList;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.INFO,
  minutesToFix = 1,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = DiagnosticTag.SUSPICIOUS
)
@RequiredArgsConstructor
public class MissingQueryParameterDiagnostic extends AbstractDiagnostic {
  private final ControlFlowGraphIndex graphs;
  private final CompilationContextResolver contexts;
  private final TypeService types;

  @Override
  protected void check() {
    String uri = documentContext.getUri().toString();
    var flow = new QueryParameterFlow(documentContext, graphs, contexts, types);
    for (var finding : flow.analyze()) {
      var cause = finding.cause();
      var related = new ArrayList<DiagnosticRelatedInformation>();
      for (var site : finding.executions()) {
        related.add(new DiagnosticRelatedInformation(new Location(uri, site), info.getResourceString("execution")));
      }
      for (var fragment : cause.source().fragments()) {
        related.add(new DiagnosticRelatedInformation(new Location(uri, fragment), info.getResourceString("fragment")));
      }
      diagnosticStorage.addDiagnostic(cause.source().range(),
        info.getResourceString(finding.messageKey(), cause.name()), related);
    }
  }

}
