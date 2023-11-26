/*
 * This file is a part of BSL Language Server.
 *
 * Copyright (c) 2018-2023
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
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.parser.BSLParserRuleContext;
import com.github._1c_syntax.bsl.types.ModuleType;
import com.github._1c_syntax.utils.CaseInsensitivePattern;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@DiagnosticMetadata(
  type = DiagnosticType.VULNERABILITY,
  severity = DiagnosticSeverity.MAJOR,
  modules = {
    ModuleType.ObjectModule,
    ModuleType.UNKNOWN
  },
  minutesToFix = 5,
  tags = {
    DiagnosticTag.SUSPICIOUS
  },
  scope = DiagnosticScope.BSL,
  activatedByDefault = true
)

public class DisableSafeModeForExternalDataProcessorsDiagnostic extends AbstractVisitorDiagnostic {

  public static final String MAIN_METHOD_NAME = "СведенияОВнешнейОбработке";
  private static final String SAFE_MODE_PARAMETER = "БЕЗОПАСНЫЙРЕЖИМ";
  private static final String SAFE_MODE_PARAMETER_WITH_QUOTES = "\"" + SAFE_MODE_PARAMETER + "\"";
  private static final Pattern INSERT_METHOD_PATTERN = CaseInsensitivePattern.compile("вставить|insert");
  private static final Pattern ADD_INSERT_METHOD_PATTERN = CaseInsensitivePattern.compile("add|добавить|вставить|insert");
  private static final Pattern PERMISSIONS_PATTERN = CaseInsensitivePattern.compile("разрешения");

  private final ReferenceIndex referenceIndex;
  private final Map<String, List<BSLParserRuleContext>> safeModeUnsetNodes = new HashMap<>();
  private MethodSymbol methodSymbol;
  private String returnedVariableNameFromMainMethod = "";

  public DisableSafeModeForExternalDataProcessorsDiagnostic(ReferenceIndex referenceIndex) {
    this.referenceIndex = referenceIndex;
  }

  @Override
  public ParseTree visitFile(BSLParser.FileContext ctx) {
    processFile(ctx);
    return null;

  }

  public void processFile(BSLParser.FileContext ctx) {
    final var mainMethodSymbol = getMainMethodSymbol();
    if (mainMethodSymbol
      .isEmpty()) {
      return;
    }
    methodSymbol = mainMethodSymbol.get();

    final var references = referenceIndex.getReferencesFrom(methodSymbol);
    if (references.isEmpty()) {
      return;
    }

    super.visitFile(ctx); // вычисляю имя возвращаемой переменной в returnedVariablesFromMainMethod
    if (!returnedVariableNameFromMainMethod.isEmpty() && !safeModeUnsetNodes.isEmpty()) {
      safeModeUnsetNodes.get(returnedVariableNameFromMainMethod)
        .forEach(diagnosticStorage::addDiagnostic);
    }
  }

  @NotNull
  private Optional<MethodSymbol> getMainMethodSymbol() {
    return documentContext.getSymbolTree().getMethodSymbol(MAIN_METHOD_NAME)
      .filter(MethodSymbol::isFunction)
      .filter(MethodSymbol::isExport);
  }

  @Override
  public ParseTree visitFunction(BSLParser.FunctionContext ctx) {
    if (Ranges.containsRange(methodSymbol.getRange(), Ranges.create(ctx))) {
      final var isMainFunction = Optional.ofNullable(ctx.funcDeclaration())
        .map(BSLParser.FuncDeclarationContext::subName)
        .map(BSLParser.SubNameContext::IDENTIFIER)
        .map(ParseTree::getText)
        .filter(s -> s.equalsIgnoreCase(MAIN_METHOD_NAME))
        .isPresent();
      if (isMainFunction) {
        return super.visitFunction(ctx);
      }
    }
    return null;  // нет смысла анализировать другие методы
  }

  @Override
  public ParseTree visitProcedure(BSLParser.ProcedureContext ctx) {
    return null; // нет смысла анализировать другие методы
  }

  @Override
  public ParseTree visitReturnStatement(BSLParser.ReturnStatementContext ctx) {
    if (returnedVariableNameFromMainMethod.isEmpty()
      && Ranges.containsRange(methodSymbol.getRange(), Ranges.create(ctx))) {

      returnedVariableNameFromMainMethod = Optional.ofNullable(ctx.expression())
        .map(BSLParser.ExpressionContext::member)
        .filter(memberContexts -> !memberContexts.isEmpty())
        .map(memberContexts -> memberContexts.get(0))
        .map(BSLParser.MemberContext::complexIdentifier)
        .map(BSLParser.ComplexIdentifierContext::IDENTIFIER)
        .map(ParseTree::getText)
        .orElse("");
    }
    return null; //нет смысла идти глубже
  }

  @Override
  public ParseTree visitAssignment(BSLParser.AssignmentContext ctx) {
    processAssignment(ctx);
    return null; // спускаться глубже нет смысла
  }

  public void processAssignment(BSLParser.AssignmentContext ctx) {
    final var lValueContext = Optional.of(ctx.lValue());
    final var varName = lValueContext.map(BSLParser.LValueContext::IDENTIFIER)
      .map(ParseTree::getText);
    if (varName.isEmpty()) {
      return;
    }
    final var terminalNodeSafeMode = lValueContext
      .map(BSLParser.LValueContext::acceptor)
      .map(BSLParser.AcceptorContext::accessProperty)
      .map(BSLParser.AccessPropertyContext::IDENTIFIER)
      .filter(terminalNode1 -> terminalNode1.getText().equalsIgnoreCase(SAFE_MODE_PARAMETER));

    if (terminalNodeSafeMode.isPresent() && isFalseExpression(Optional.ofNullable(ctx.expression()))) {
      safeModeUnsetNodes.computeIfAbsent(varName.get(), name -> new ArrayList<>()).add(ctx);
    }
  }

  @Override
  public ParseTree visitCallStatement(BSLParser.CallStatementContext ctx) {
    processCallStatement(ctx);
    return null;
  }

  public void processCallStatement(BSLParser.CallStatementContext ctx) {
    final var varName = Optional.ofNullable(ctx.IDENTIFIER())
      .map(ParseTree::getText);
    if (varName.isEmpty()) {
      return;
    }

    final var callContext = Optional.of(ctx);
    if (isPermissionAdding(callContext)) {
      safeModeUnsetNodes.computeIfAbsent(varName.get(), name -> new ArrayList<>()).add(ctx);
      return;
    }

    if (isSafeModeUnsetCalling(callContext)) {
      safeModeUnsetNodes.computeIfAbsent(varName.get(), name -> new ArrayList<>()).add(ctx);
    }
  }

  private static boolean isFalseExpression(Optional<BSLParser.ExpressionContext> expressionContext) {
    return expressionContext
      .map(BSLParser.ExpressionContext::member)
      .filter(memberContexts -> !memberContexts.isEmpty())
      .map(memberContexts -> memberContexts.get(0))
      .map(BSLParser.MemberContext::constValue)
      .map(BSLParser.ConstValueContext::FALSE)
      .isPresent();
  }

  private static boolean isPermissionAdding(Optional<BSLParser.CallStatementContext> callContext) {
    final var isPermissionReferenced = callContext
      .map(BSLParser.CallStatementContext::modifier)
      .filter(modifierContexts -> !modifierContexts.isEmpty())
      .map(modifierContexts -> modifierContexts.get(0))
      .map(BSLParser.ModifierContext::accessProperty)
      .map(BSLParser.AccessPropertyContext::IDENTIFIER)
      .filter(terminalNode -> PERMISSIONS_PATTERN.matcher(terminalNode.getText()).matches())
      .isPresent();
    if (!isPermissionReferenced) {
      return false;
    }
    return getMethodCallContext(callContext, ADD_INSERT_METHOD_PATTERN)
      .isPresent();
  }

  private static boolean isSafeModeUnsetCalling(Optional<BSLParser.CallStatementContext> callContext) {
    final var methodCallContext = getMethodCallContext(callContext, INSERT_METHOD_PATTERN);
    if (methodCallContext.isEmpty()) {
      return false;
    }
    final var expressionContext = methodCallContext
      .map(BSLParser.MethodCallContext::doCall)
      .map(BSLParser.DoCallContext::callParamList)
      .map(BSLParser.CallParamListContext::callParam)
      .filter(callParamContexts -> callParamContexts.size() == 2)
      .filter(callParamContexts -> haveSafeModeText(callParamContexts.get(0)))
      .map(callParamContexts -> callParamContexts.get(1))
      .map(BSLParser.CallParamContext::expression);

    return isFalseExpression(expressionContext);
  }

  private static Optional<BSLParser.MethodCallContext> getMethodCallContext(Optional<BSLParser.CallStatementContext> callContext, Pattern insertMethodPattern) {
    final var methodCallContext = callContext
      .map(BSLParser.CallStatementContext::accessCall)
      .map(BSLParser.AccessCallContext::methodCall);
    final var methodNameContext = methodCallContext
      .map(BSLParser.MethodCallContext::methodName)
      .filter(methodName -> insertMethodPattern.matcher(methodName.getText()).matches());
    if (methodNameContext.isEmpty()) {
      return Optional.empty();
    }
    return methodCallContext;
  }

  private static boolean haveSafeModeText(BSLParser.CallParamContext callParamContext) {
    return Optional.of(callParamContext)
      .map(BSLParser.CallParamContext::expression)
      .map(BSLParser.ExpressionContext::member)
      .filter(memberContexts -> !memberContexts.isEmpty())
      .map(memberContexts -> memberContexts.get(0))
      .map(BSLParser.MemberContext::constValue)
      .map(BSLParser.ConstValueContext::string)
      .map(BSLParserRuleContext::getText)
      .filter(name -> name.equalsIgnoreCase(SAFE_MODE_PARAMETER_WITH_QUOTES))
      .isPresent();
  }
}
