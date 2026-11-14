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
import com.github._1c_syntax.bsl.languageserver.context.symbol.annotations.CompilerDirectiveKind;
import com.github._1c_syntax.bsl.languageserver.context.symbol.variable.VariableKind;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.model.MemberKind;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.languageserver.utils.Trees;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.types.ModuleType;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.Nullable;

@DiagnosticMetadata(
  type = DiagnosticType.ERROR,
  severity = DiagnosticSeverity.MAJOR,
  scope = DiagnosticScope.BSL,
  modules = {ModuleType.FormModule},
  minutesToFix = 5,
  tags = {DiagnosticTag.SUSPICIOUS}
)
@RequiredArgsConstructor
public class AssignToFormAttributeInNoContextDiagnostic extends AbstractVisitorDiagnostic {
  private final TypeService typeService;

  @Override
  public @Nullable ParseTree visitAssignment(BSLParser.AssignmentContext ctx) {
    var target = ctx.lValue().IDENTIFIER();
    if (documentContext.getModuleType() != ModuleType.FormModule || target == null) {
      return super.visitAssignment(ctx);
    }
    var method = (BSLParser.SubContext) Trees.getAncestorByRuleIndex(ctx, BSLParser.RULE_sub);
    if (method == null || documentContext.getSymbolTree().getMethodSymbol(method)
      .flatMap(MethodSymbol::getCompilerDirectiveKind)
      .filter(directive -> directive == CompilerDirectiveKind.AT_SERVER_NO_CONTEXT
        || directive == CompilerDirectiveKind.AT_CLIENT_AT_SERVER_NO_CONTEXT).isEmpty()) {
      return super.visitAssignment(ctx);
    }
    var name = target.getText();
    var explicitVariable = documentContext.getSymbolTree().getVariableSymbolInScope(ctx, name)
      .filter(variable -> variable.getKind() != VariableKind.DYNAMIC).isPresent();
    // Context filtering hides these members in a no-context method. Consult the
    // actual form's inventory, not ordinary lookup at the assignment location.
    if (!explicitVariable && typeService.findSelfMember(documentContext, name, MemberKind.PROPERTY).isPresent()) {
      diagnosticStorage.addDiagnostic(Ranges.create(target), info.getMessage(name));
    }
    return super.visitAssignment(ctx);
  }
}
