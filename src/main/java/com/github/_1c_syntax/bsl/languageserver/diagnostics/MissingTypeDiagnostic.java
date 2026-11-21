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

import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.Mode;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.VariableSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.variable.VariableKind;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.CommentTypeResolver;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.ExpressionTreeBuildingVisitor;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 5,
  scope = DiagnosticScope.BSL,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class MissingTypeDiagnostic extends AbstractVisitorDiagnostic {

  private final ExpressionTypeInferencer inferencer;
  private final CommentTypeResolver commentTypes;
  private final Set<VariableSymbol> initialized = new HashSet<>();

  @Override
  public List<Diagnostic> getDiagnostics(DocumentContext document) {
    var options = document.getServerContext().getLanguageServerConfiguration().getDiagnosticsOptions();
    if (options.getMode() == Mode.ON && !options.getParameters().containsKey("MissingType")
      && !hasStrictTypesHeader(document)) {
      return List.of();
    }
    initialized.clear();
    try {
      return super.getDiagnostics(document);
    } finally {
      initialized.clear();
    }
  }

  @Override
  public ParseTree visitAssignment(BSLParser.AssignmentContext ctx) {
    var target = ctx.lValue();
    if (target == null || target.acceptor() != null || target.IDENTIFIER() == null) {
      return ctx;
    }

    var name = target.IDENTIFIER();
    var variable = documentContext.getSymbolTree().getVariableSymbolInScope(target, name.getText()).orElse(null);
    if (variable == null || !(variable.getScope() instanceof MethodSymbol)
      || variable.getKind() == VariableKind.PARAMETER || !initialized.add(variable)) {
      return ctx;
    }

    var value = ExpressionTreeBuildingVisitor.buildExpressionTree(ctx.expression());
    if (value != null && !hasType(commentTypes.typesOf(variable))
      && !hasType(commentTypes.ofAssignment(documentContext, ctx))
      && !hasType(inferencer.infer(value, documentContext))) {
      diagnosticStorage.addDiagnostic(name, info.getMessage(name.getText()));
    }
    return ctx;
  }

  private static boolean hasStrictTypesHeader(DocumentContext document) {
    return document.getTokens().stream()
      .takeWhile(token -> token.getChannel() != Token.DEFAULT_CHANNEL && token.getType() != BSLLexer.HASH)
      .anyMatch(token -> token.getType() == BSLLexer.LINE_COMMENT
        && token.getText().substring(2).strip().equalsIgnoreCase("@strict-types"));
  }

  private static boolean hasType(TypeSet types) {
    return types.refs().stream().anyMatch(type -> !type.equals(TypeRef.UNKNOWN));
  }
}
