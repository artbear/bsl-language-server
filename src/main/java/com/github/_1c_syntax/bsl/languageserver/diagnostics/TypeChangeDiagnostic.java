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
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.OpenDataObjectInference;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BinaryOperationNode;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslExpression;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.BslOperator;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.ExpressionTreeBuildingVisitor;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.BADPRACTICE}
)
@RequiredArgsConstructor
public class TypeChangeDiagnostic extends AbstractVisitorDiagnostic {

  private final TypeService typeService;
  private final ExpressionTypeInferencer inferencer;

  @Override
  public ParseTree visitAssignment(BSLParser.AssignmentContext ctx) {
    var target = ctx.lValue();
    var value = ExpressionTreeBuildingVisitor.buildExpressionTree(ctx.expression());
    if (target == null || value == null) {
      return ctx;
    }

    var acceptor = target.acceptor();
    if (acceptor == null && target.IDENTIFIER() != null) {
      var name = target.IDENTIFIER();
      documentContext.getSymbolTree().getVariableSymbolInScope(target, name.getText())
        .filter(variable -> variable.getScope() instanceof MethodSymbol && variable.getKind() != VariableKind.PARAMETER)
        .ifPresent(variable -> checkAssignment(Ranges.create(name), name.getText(),
          typeService.typesOfVariableAt(variable, Ranges.create(name).getStart()),
          inferencer.infer(value, documentContext)));
    } else if (acceptor != null && acceptor.accessProperty() != null && acceptor.accessProperty().IDENTIFIER() != null) {
      var property = acceptor.accessProperty().IDENTIFIER();
      var range = Ranges.create(property);
      var receiver = typeService.receiverTypesAt(documentContext, range.getStart());
      checkAssignment(range, property.getText(), structureKeyTypes(receiver, property.getText()),
        inferencer.infer(value, documentContext));
    } else if (acceptor != null && acceptor.accessIndex() != null) {
      checkIndexAssignment(target, acceptor.accessIndex(), value);
    }

    return ctx;
  }

  private void checkIndexAssignment(BSLParser.LValueContext target, BSLParser.AccessIndexContext index,
                                    BslExpression value) {
    if (!(ExpressionTreeBuildingVisitor.buildExpressionTree(target) instanceof BinaryOperationNode access)
      || access.getOperator() != BslOperator.INDEX_ACCESS || access.getRepresentingAst() != index) {
      return;
    }
    var key = OpenDataObjectInference.stringLiteralOf(access.getRight());
    if (key != null) {
      var receiver = inferencer.infer(access.getLeft(), documentContext);
      checkAssignment(Ranges.create(index.expression()), key, structureKeyTypes(receiver, key),
        inferencer.infer(value, documentContext));
    }
  }

  private static TypeSet structureKeyTypes(TypeSet receiver, String key) {
    var knownStructure = !receiver.isEmpty() && receiver.refs().stream()
      .allMatch(type -> type.canonicalKey().equals("структура") || type.canonicalKey().equals("structure"));
    return knownStructure ? receiver.getFieldTypes(key) : TypeSet.EMPTY;
  }

  private void checkAssignment(Range range, String key, TypeSet previous, TypeSet assigned) {
    var before = concreteTypes(previous);
    var after = concreteTypes(assigned);
    if (!before.isEmpty() && !after.isEmpty() && Collections.disjoint(before, after)) {
      diagnosticStorage.addDiagnostic(range, info.getMessage(key));
    }
  }

  private static Set<TypeRef> concreteTypes(TypeSet types) {
    if (types.refs().contains(TypeRef.UNKNOWN) || types.refs().contains(TypeRef.ANY)) {
      return Set.of();
    }
    return types.refs().stream()
      .filter(type -> !type.equals(TypeRef.UNDEFINED))
      .collect(Collectors.toUnmodifiableSet());
  }
}
