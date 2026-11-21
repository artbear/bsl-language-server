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
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.ExpressionTreeBuildingVisitor;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

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
        .ifPresent(variable -> checkAssignment(name,
          typeService.typesOfVariableAt(variable, Ranges.create(name).getStart()),
          inferencer.infer(value, documentContext)));
    } else if (acceptor != null && acceptor.accessProperty() != null && acceptor.accessProperty().IDENTIFIER() != null) {
      var property = acceptor.accessProperty().IDENTIFIER();
      checkAssignment(property, structureKeyTypes(property), inferencer.infer(value, documentContext));
    }

    return ctx;
  }

  private TypeSet structureKeyTypes(TerminalNode property) {
    var receiver = typeService.receiverTypesAt(documentContext, Ranges.create(property).getStart());
    var knownStructure = !receiver.isEmpty() && receiver.refs().stream()
      .allMatch(type -> type.canonicalKey().equals("структура") || type.canonicalKey().equals("structure"));
    return knownStructure ? receiver.getFieldTypes(property.getText()) : TypeSet.EMPTY;
  }

  private void checkAssignment(TerminalNode target, TypeSet previous, TypeSet assigned) {
    var before = concreteTypes(previous);
    var after = concreteTypes(assigned);
    if (!before.isEmpty() && !after.isEmpty() && Collections.disjoint(before, after)) {
      diagnosticStorage.addDiagnostic(target, info.getMessage(target.getText()));
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
