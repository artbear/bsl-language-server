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
import com.github._1c_syntax.bsl.languageserver.types.compilation.CompilationContextResolver;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.OpenDataObjectInference;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.types.registry.FormByNameResolver;
import com.github._1c_syntax.bsl.languageserver.types.registry.TypeRegistry;
import com.github._1c_syntax.bsl.languageserver.types.registry.ValueTypes;
import com.github._1c_syntax.bsl.languageserver.utils.Positions;
import com.github._1c_syntax.bsl.languageserver.utils.expressiontree.ExpressionTreeBuildingVisitor;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.mdo.Form;
import com.github._1c_syntax.bsl.mdo.support.FormType;
import com.github._1c_syntax.bsl.types.MdoReference;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Collections;
import java.util.Optional;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class ArbitraryFormParameterDiagnostic extends AbstractVisitorDiagnostic {

  private final ExpressionTypeInferencer inferencer;
  private final FormByNameResolver forms;
  private final TypeRegistry typeRegistry;
  private final CompilationContextResolver contexts;

  @Override
  public ParseTree visitGlobalMethodCall(BSLParser.GlobalMethodCallContext ctx) {
    var methodName = ctx.methodName().getText();
    if ((!methodName.equalsIgnoreCase("ОткрытьФорму") && !methodName.equalsIgnoreCase("OpenForm"))
      || shadowsPlatformMethod(ctx, methodName)) {
      return super.visitGlobalMethodCall(ctx);
    }
    var args = ctx.doCall().callParamList().callParam();
    if (args.size() < 2 || args.get(0).expression() == null || args.get(1).expression() == null) {
      return super.visitGlobalMethodCall(ctx);
    }
    var name = ExpressionTreeBuildingVisitor.buildExpressionTree(args.getFirst().expression());
    var values = ExpressionTreeBuildingVisitor.buildExpressionTree(args.get(1).expression());
    if (name == null || values == null
      || !(name.getRepresentingAst() instanceof BSLParser.ConstValueContext literal) || literal.string() == null) {
      return super.visitGlobalMethodCall(ctx);
    }

    var formName = OpenDataObjectInference.stringLiteralOf(name);
    if (formName != null) {
      forms.resolve(documentContext, formName).flatMap(this::managedForm)
        .ifPresent(form -> checkParameters(form, args.get(1), inferencer.infer(values, documentContext)));
    }
    return super.visitGlobalMethodCall(ctx);
  }

  private boolean shadowsPlatformMethod(BSLParser.GlobalMethodCallContext call, String name) {
    var symbols = documentContext.getSymbolTree();
    if (symbols.getMethodSymbol(name).isEmpty()) {
      return false;
    }
    var callingContexts = contexts.narrowedContextsAt(documentContext, call, Positions.create(call))
      .or(() -> contexts.possibleContexts(documentContext, call))
      .orElseGet(() -> contexts.supportedContexts(documentContext));
    if (callingContexts.isEmpty()) {
      return true;
    }
    return symbols.getMethods().stream().filter(method -> method.getName().equalsIgnoreCase(name)).anyMatch(method -> {
      var declaration = symbols.getMethodPreprocessorContexts(method);
      // Неизвестное условие оставляет возможность видимости пользовательского метода.
      return declaration == null || !Collections.disjoint(callingContexts, declaration);
    });
  }

  private Optional<Form> managedForm(TypeRef ref) {
    var name = ref.qualifiedName();
    return MdoReference.find(name.substring(name.indexOf('.') + 1))
      .flatMap(mdo -> documentContext.getServerContext().getConfiguration().findChild(mdo))
      .filter(Form.class::isInstance).map(Form.class::cast)
      .filter(form -> form.getFormType() == FormType.MANAGED);
  }

  private void checkParameters(Form form, BSLParser.CallParamContext argument, TypeSet values) {
    if (!isStructure(values)) {
      return;
    }
    for (var parameter : form.getData().getParameters()) {
      var valueType = parameter.getValueType();
      var field = values.getFieldTypes(parameter.getName());
      if ((valueType.isEmpty() || ValueTypes.resolve(typeRegistry, valueType).refs().contains(TypeRef.ANY))
        && isStructure(field)) {
        diagnosticStorage.addDiagnostic(argument.expression(), info.getMessage(parameter.getName()));
      }
    }
  }

  private static boolean isStructure(TypeSet types) {
    return !types.isEmpty() && types.refs().stream().allMatch(type -> {
      var name = type.canonicalKey();
      return name.equals("структура") || name.equals("structure")
        || name.equals("фиксированнаяструктура") || name.equals("fixedstructure");
    });
  }
}
