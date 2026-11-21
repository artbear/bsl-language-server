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
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionAtPosition;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.model.MemberKind;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BinaryOperationNode;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslExpression;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslOperator;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.UnaryOperationNode;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MINOR,
  minutesToFix = 2,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class StringIndexAccessDiagnostic extends AbstractVisitorDiagnostic {

  private static final Pattern IDENTIFIER = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");
  private final ExpressionTypeInferencer inferencer;
  private final TypeService typeService;

  @Override
  public ParseTree visitAccessIndex(BSLParser.AccessIndexContext ctx) {
    if (ctx.LBRACK() == null) {
      return super.visitAccessIndex(ctx);
    }
    var index = ExpressionAtPosition.findExpressionTree(ctx.LBRACK())
      .map(tree -> findIndex(tree, ctx)).orElse(null);
    if (index != null && index.getRight().getRepresentingAst() instanceof BSLParser.ConstValueContext literal) {
      var string = literal.string();
      if (string != null && string.STRING().size() == 1 && string.multilineString().isEmpty()) {
        var value = string.STRING(0).getText();
        var name = value.substring(1, value.length() - 1);
        if (IDENTIFIER.matcher(name).matches() && canUseProperty(index.getLeft(), name)) {
          diagnosticStorage.addDiagnostic(ctx, info.getMessage(name));
        }
      }
    }
    return super.visitAccessIndex(ctx);
  }

  private boolean canUseProperty(BslExpression receiver, String name) {
    var types = inferencer.infer(receiver, documentContext);
    return !types.isEmpty() && types.refs().stream().allMatch(type -> isFormItems(type)
      && typeService.getMembers(type, documentContext.getFileType()).stream()
        .anyMatch(member -> member.kind() == MemberKind.PROPERTY && member.matches(name)));
  }

  private static boolean isFormItems(TypeRef type) {
    var name = type.canonicalKey();
    return name.startsWith("всеэлементыформы.") || name.startsWith("элементыформы.");
  }

  private static @Nullable BinaryOperationNode findIndex(BslExpression expression, BSLParser.AccessIndexContext ctx) {
    if (expression instanceof BinaryOperationNode binary) {
      if (binary.getOperator() == BslOperator.INDEX_ACCESS && binary.getRepresentingAst() == ctx) {
        return binary;
      }
      var left = findIndex(binary.getLeft(), ctx);
      return left == null ? findIndex(binary.getRight(), ctx) : left;
    }
    if (expression instanceof UnaryOperationNode unary) {
      return findIndex(unary.getOperand(), ctx);
    }
    return null;
  }
}
