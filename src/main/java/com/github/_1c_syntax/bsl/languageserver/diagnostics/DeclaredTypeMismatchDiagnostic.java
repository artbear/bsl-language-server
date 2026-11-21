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
import com.github._1c_syntax.bsl.languageserver.context.symbol.variable.VariableKind;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.CommentTypeResolver;
import com.github._1c_syntax.bsl.languageserver.types.index.SymbolTypeIndex;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.OpenDataObjectInference;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BinaryOperationNode;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslExpression;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslOperator;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.ExpressionTreeBuildingVisitor;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.MethodCallNode;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.TerminalSymbolNode;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class DeclaredTypeMismatchDiagnostic extends AbstractVisitorDiagnostic {

  private final CommentTypeResolver comments;
  private final SymbolTypeIndex symbols;
  private final ExpressionTypeInferencer inferencer;

  @Override
  public ParseTree visitCallStatement(BSLParser.CallStatementContext ctx) {
    if (ExpressionTreeBuildingVisitor.buildExpressionTree(ctx) instanceof BinaryOperationNode access
      && access.getOperator() == BslOperator.DEREFERENCE && access.getRight() instanceof MethodCallNode call
      && (call.getName().getText().equalsIgnoreCase("Вставить") || call.getName().getText().equalsIgnoreCase("Insert"))
      && call.arguments().size() == 2) {
      var field = literalKey(call.arguments().getFirst());
      checkField(ctx, access.getLeft(), field, call.arguments().get(1));
    }
    return super.visitCallStatement(ctx);
  }

  @Override
  public ParseTree visitAssignment(BSLParser.AssignmentContext ctx) {
    if (ExpressionTreeBuildingVisitor.buildExpressionTree(ctx.lValue()) instanceof BinaryOperationNode access) {
      var key = switch (access.getOperator()) {
        case DEREFERENCE -> access.getRight().getRepresentingAst() instanceof TerminalNode member ? member.getText() : null;
        case INDEX_ACCESS -> literalKey(access.getRight());
        default -> null;
      };
      checkField(ctx, access.getLeft(), key, ExpressionTreeBuildingVisitor.buildExpressionTree(ctx.expression()));
    }
    return super.visitAssignment(ctx);
  }

  private void checkField(ParserRuleContext statement, BslExpression receiver, @Nullable String key,
                          @Nullable BslExpression value) {
    if (key == null || value == null || !(receiver instanceof TerminalSymbolNode)
      || !(receiver.getRepresentingAst() instanceof TerminalNode name)) {
      return;
    }

    var declared = declaredTypes(statement, name.getText());
    if (declared.isEmpty() || declared.refs().stream().anyMatch(DeclaredTypeMismatchDiagnostic::isUncertain)) {
      return;
    }
    var field = declared.getFieldTypes(key);
    if (field.isEmpty() || field.refs().stream().anyMatch(DeclaredTypeMismatchDiagnostic::isUncertain)) {
      return;
    }
    var assigned = inferencer.infer(value, documentContext);
    if (!assigned.isEmpty() && assigned.refs().stream()
      .noneMatch(type -> isUncertain(type) || field.refs().contains(type))) {
      if (value.getRepresentingAst() instanceof ParserRuleContext valueNode) {
        diagnosticStorage.addDiagnostic(valueNode, info.getMessage(key));
      } else if (value.getRepresentingAst() instanceof TerminalNode terminal) {
        diagnosticStorage.addDiagnostic(terminal, info.getMessage(key));
      }
    }
  }

  private TypeSet declaredTypes(ParserRuleContext statement, String name) {
    return documentContext.getSymbolTree().getVariableSymbolInScope(statement, name).map(variable -> {
      if (variable.getKind() == VariableKind.PARAMETER && variable.getScope() instanceof MethodSymbol method) {
        return method.getParameters().stream().filter(parameter -> parameter.getName().equalsIgnoreCase(name))
          .findFirst().map(parameter -> symbols.getDeclaredParameterTypes(parameter, documentContext))
          .orElse(TypeSet.EMPTY);
      }
      return comments.typesOf(variable);
    }).orElse(TypeSet.EMPTY);
  }

  private static @Nullable String literalKey(BslExpression node) {
    return node.getRepresentingAst() instanceof BSLParser.ConstValueContext literal && literal.string() != null
      ? OpenDataObjectInference.stringLiteralOf(node) : null;
  }

  private static boolean isUncertain(TypeRef type) {
    return type.equals(TypeRef.UNKNOWN) || type.equals(TypeRef.ANY);
  }
}
