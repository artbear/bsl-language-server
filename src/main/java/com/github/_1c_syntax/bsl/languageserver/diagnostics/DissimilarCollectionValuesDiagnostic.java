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

import com.github._1c_syntax.bsl.languageserver.cfg.BasicBlockVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.CfgBuildOptions;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraphIndex;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BinaryOperationNode;
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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.BADPRACTICE}
)
@RequiredArgsConstructor
public class DissimilarCollectionValuesDiagnostic extends AbstractVisitorDiagnostic {

  private final ControlFlowGraphIndex graphs;
  private final ExpressionTypeInferencer inferencer;

  @Override
  public ParseTree visitSubCodeBlock(BSLParser.SubCodeBlockContext ctx) {
    checkBody(ctx.codeBlock());
    return ctx;
  }

  @Override
  public ParseTree visitFileCodeBlockBeforeSub(BSLParser.FileCodeBlockBeforeSubContext ctx) {
    checkBody(ctx.codeBlock());
    return ctx;
  }

  @Override
  public ParseTree visitFileCodeBlock(BSLParser.FileCodeBlockContext ctx) {
    checkBody(ctx.codeBlock());
    return ctx;
  }

  private void checkBody(BSLParser.@Nullable CodeBlockContext body) {
    if (body == null) {
      return;
    }
    var graph = graphs.graphOf(documentContext, body, CfgBuildOptions.defaults());
    for (var vertex : graph.vertexSet()) {
      if (vertex instanceof BasicBlockVertex block) {
        checkBlock(block);
      }
    }
  }

  private void checkBlock(BasicBlockVertex block) {
    var known = new HashSet<TypeRef>();
    var currentReceiver = "";
    for (var statement : block.statements()) {
      var addition = arrayAddition(statement);
      if (addition == null) {
        currentReceiver = "";
        known.clear();
        continue;
      }
      if (!currentReceiver.equalsIgnoreCase(addition.receiver())) {
        currentReceiver = addition.receiver();
        known.clear();
      }
      var types = addition.types().refs();
      if (!types.isEmpty() && types.stream().noneMatch(DissimilarCollectionValuesDiagnostic::isUncertain)) {
        if (!known.isEmpty() && types.stream().noneMatch(known::contains)) {
          diagnosticStorage.addDiagnostic(Ranges.create(addition.location()), info.getMessage(currentReceiver));
        }
        if (types.size() == 1) {
          known.add(types.iterator().next());
        }
      }
    }
  }

  private @Nullable Addition arrayAddition(ParserRuleContext statement) {
    if (!(statement instanceof BSLParser.CallStatementContext call)
      || !(ExpressionTreeBuildingVisitor.buildExpressionTree(call) instanceof BinaryOperationNode access)
      || access.getOperator() != BslOperator.DEREFERENCE
      || !(access.getLeft() instanceof TerminalSymbolNode receiver)
      || !(receiver.getRepresentingAst() instanceof TerminalNode identifier)
      || !(access.getRight() instanceof MethodCallNode method)) {
      return null;
    }
    var argument = switch (method.getName().getText().toLowerCase(Locale.ROOT)) {
      case "добавить", "add" -> 0;
      case "вставить", "insert" -> 1;
      default -> -1;
    };
    if (argument < 0 || method.arguments().size() != argument + 1) {
      return null;
    }
    var receiverTypes = inferencer.infer(receiver, documentContext);
    if (receiverTypes.isEmpty() || !receiverTypes.refs().stream().allMatch(DissimilarCollectionValuesDiagnostic::isArray)) {
      return null;
    }
    var value = method.arguments().get(argument);
    var location = value.getRepresentingAst();
    return location == null ? null
      : new Addition(identifier.getText(), inferencer.infer(value, documentContext), location);
  }

  private static boolean isArray(TypeRef type) {
    return type.canonicalKey().equals("массив") || type.canonicalKey().equals("array");
  }

  private static boolean isUncertain(TypeRef type) {
    return type.equals(TypeRef.UNKNOWN) || type.equals(TypeRef.ANY) || type.equals(TypeRef.UNDEFINED);
  }

  private record Addition(String receiver, TypeSet types, ParseTree location) {
  }
}
