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

import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.Symbol;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.references.ReferenceIndex;
import com.github._1c_syntax.bsl.languageserver.references.model.Reference;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.utils.CaseInsensitivePattern;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 5,
  tags = {
    DiagnosticTag.BADPRACTICE,
    DiagnosticTag.SUSPICIOUS,
  }

)
public class UsageWriteLogEventWhenProcessingExceptionDiagnostic extends AbstractVisitorDiagnostic {
  private static final Pattern WRITELOGEVENT = CaseInsensitivePattern.compile(
    "записьжурналарегистрации|writelogevent"
  );
  private final ReferenceIndex referenceIndex;
  private final Set<String> methodsWithRequiredCall = new HashSet<>();
  private final List<CodeBlockData> blocksForChecking = new ArrayList<>();
  //  private boolean insideException;
  private boolean hasRequiredCallInsideExceptBlock;
  private String currentMethodName;
  private List<Reference> referenceToMethodCalls = Collections.emptyList();

  public UsageWriteLogEventWhenProcessingExceptionDiagnostic(ReferenceIndex referenceIndex) {
    this.referenceIndex = referenceIndex;
  }

  private static boolean isRightMethod(BSLParser.GlobalMethodCallContext context) {
    return WRITELOGEVENT.matcher(context.methodName().getText()).matches();
  }

  private static boolean isMethodSymbol(Symbol symbol) {
    return symbol.getSymbolKind() == SymbolKind.Method;
  }

  @Override
  public ParseTree visitProcedure(BSLParser.ProcedureContext ctx) {
    currentMethodName = ctx.procDeclaration().subName().getText();
    return super.visitProcedure(ctx);
  }

  @Override
  public ParseTree visitFunction(BSLParser.FunctionContext ctx) {
    currentMethodName = ctx.funcDeclaration().subName().getText();
    return super.visitFunction(ctx);
  }

  @Override
  public ParseTree visitExceptCodeBlock(BSLParser.ExceptCodeBlockContext ctx) {
//    insideException = true; // для отбрасывания ненужных узлов
    hasRequiredCallInsideExceptBlock = false;

    // TODO проверить вложенные блоки Попытка-Попытка-Исключение-КонецПопытки-Исключение-КонецПопытки
    super.visitExceptCodeBlock(ctx); // обработка всех вложенных вызовов
//    insideException = false;

    if (!hasRequiredCallInsideExceptBlock) {
      if (!hasRequiredCallInsideMethods(Ranges.create(ctx))) {
        fireIssue(ctx);
      } else {
        blocksForChecking.add(new CodeBlockData(documentContext, ctx, getReferenceToMethodCalls()));
      }
      hasRequiredCallInsideExceptBlock = false;
    }

    return super.defaultResult(); // внутрь вызова уже сходили
  }

  private boolean hasRequiredCallInsideMethods(Range range) {
    return getReferenceToMethodCalls().stream()
      .anyMatch(reference -> Ranges.containsRange(range, reference.getSelectionRange()));
  }

  private List<Reference> getReferenceToMethodCalls() {
    if (referenceToMethodCalls.isEmpty()) {
      referenceToMethodCalls = referenceIndex.getReferencesFrom(documentContext.getUri()).stream()
        .filter(reference -> isMethodSymbol(reference.getSymbol()))
        .collect(Collectors.toList());
    }
    return referenceToMethodCalls;
  }

  private void fireIssue(BSLParser.ExceptCodeBlockContext ctx) {
    final var tryStatementContext = (BSLParser.TryStatementContext) ctx.getParent();
    diagnosticStorage.addDiagnostic(tryStatementContext.ENDTRY_KEYWORD());
  }

  @Override
  public ParseTree visitGlobalMethodCall(BSLParser.GlobalMethodCallContext context) {
    if (!hasRequiredCallInsideExceptBlock && isRightMethod(context)) {
      hasRequiredCallInsideExceptBlock = true;
      methodsWithRequiredCall.add(currentMethodName);
    }
    return super.defaultResult(); // внутрь вызова нет смысла идти
  }

  @Override
  public ParseTree visitFile(BSLParser.FileContext ctx) {
//    referenceMethodCalls = Collections.emptyList();

    super.visitFile(ctx); // обработка всего модуля - получение всех вызовов ЗаписьЖурналаРегистрации

    blocksForChecking.forEach(this::checkCodeBlock);

    referenceToMethodCalls = Collections.emptyList();
    blocksForChecking.clear();
    methodsWithRequiredCall.clear();

    return super.defaultResult();
  }

  private void checkCodeBlock(CodeBlockData dto) {
    if (hasRequiredCallInsideLocalMethods(dto.invokedLocalMethods.stream())) {
      return;
    }
    final var hasRequiredCallInsideLocalMethods = dto.invokedLocalMethods.stream()
      .anyMatch(this::hasRequiredCallForFirstLevelOfNestingMethods);
    if (hasRequiredCallInsideLocalMethods) {
      return;
    }
    fireIssue(dto.context);
  }

  private boolean hasRequiredCallInsideLocalMethods(Stream<MethodSymbol> stream) {
    return stream
      .anyMatch(methodSymbol -> methodsWithRequiredCall.contains(methodSymbol.getName()));
  }

  private boolean hasRequiredCallForFirstLevelOfNestingMethods(MethodSymbol methodSymbol) {
    final var methodSymbolStream = referenceIndex.getReferencesFrom(methodSymbol).stream()
      .map(Reference::getSymbol)
      .filter(UsageWriteLogEventWhenProcessingExceptionDiagnostic::isMethodSymbol)
      .filter(MethodSymbol.class::isInstance)
      .map(MethodSymbol.class::cast);
    return hasRequiredCallInsideLocalMethods(methodSymbolStream);
  }

  private static class CodeBlockData {
    final BSLParser.ExceptCodeBlockContext context;
    final Collection<MethodSymbol> invokedLocalMethods;
    final Collection<MethodSymbol> invokedExternalMethods;

    public CodeBlockData(DocumentContext documentContext, BSLParser.ExceptCodeBlockContext context,
                         List<Reference> referenceMethodCalls) {
      this.context = context;

      final Predicate<Reference> referencePredicate = reference -> reference.getUri().equals(documentContext.getUri());

      final var localReferenceStream = referenceMethodCalls.stream().filter(referencePredicate);
      this.invokedLocalMethods = getMethodList(context, localReferenceStream);

      final var externalReferenceStream = referenceMethodCalls.stream().filter(referencePredicate.negate());
      this.invokedExternalMethods = getMethodList(context, externalReferenceStream);
    }

    private static List<MethodSymbol> getMethodList(BSLParser.ExceptCodeBlockContext exceptCodeBlockContext,
                                                    Stream<Reference> referenceStream) {
      return referenceStream
        .filter(reference -> Ranges.containsRange(Ranges.create(exceptCodeBlockContext), reference.getSelectionRange()))
        .map(Reference::getSymbol)
        .filter(MethodSymbol.class::isInstance)
        .map(MethodSymbol.class::cast)
        .collect(Collectors.toList());
    }
  }
}
