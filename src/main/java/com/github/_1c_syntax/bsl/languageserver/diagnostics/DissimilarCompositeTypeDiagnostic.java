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
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.types.index.SymbolTypeIndex;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.types.MDOType;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 10,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.DESIGN}
)
@RequiredArgsConstructor
public class DissimilarCompositeTypeDiagnostic extends AbstractSymbolTreeDiagnostic {

  private static final Map<String, String> METADATA_FAMILIES = metadataFamilies();

  private final SymbolTypeIndex symbolTypeIndex;

  @Override
  public void visitMethod(MethodSymbol method) {
    if (method.getDescription().isEmpty()) {
      return;
    }
    for (var parameter : method.getParameters()) {
      if (containsDissimilarTypes(symbolTypeIndex.getDeclaredParameterTypes(parameter, documentContext))) {
        diagnosticStorage.addDiagnostic(parameter.getRange());
      }
    }
    if (method.isFunction() && containsDissimilarTypes(symbolTypeIndex.getDeclaredReturnTypes(method))) {
      diagnosticStorage.addDiagnostic(method.getSubNameRange());
    }
  }

  private static boolean containsDissimilarTypes(TypeSet types) {
    return !types.refs().contains(TypeRef.ANY) && types.refs().stream()
      .filter(type -> !type.equals(TypeRef.UNKNOWN) && !type.equals(TypeRef.UNDEFINED))
      .filter(type -> !type.canonicalKey().equals("null"))
      .map(DissimilarCompositeTypeDiagnostic::typeFamily)
      .distinct()
      .limit(2)
      .count() > 1;
  }

  private static String typeFamily(TypeRef type) {
    var name = type.canonicalKey();
    var separator = name.indexOf('.');
    var prefix = separator < 0 ? name : name.substring(0, separator);
    return METADATA_FAMILIES.getOrDefault(prefix, name);
  }

  private static Map<String, String> metadataFamilies() {
    var families = new HashMap<String, String>();
    for (var type : List.of(MDOType.CATALOG, MDOType.DOCUMENT, MDOType.CHART_OF_CHARACTERISTIC_TYPES,
      MDOType.CHART_OF_ACCOUNTS, MDOType.CHART_OF_CALCULATION_TYPES, MDOType.BUSINESS_PROCESS,
      MDOType.TASK, MDOType.EXCHANGE_PLAN)) {
      addFamily(families, type, "Ссылка", "Ref");
      addFamily(families, type, "Объект", "Object");
    }
    addFamily(families, MDOType.ENUM, "Ссылка", "Ref");
    addFamily(families, MDOType.REPORT, "Объект", "Object");
    addFamily(families, MDOType.DATA_PROCESSOR, "Объект", "Object");
    return Map.copyOf(families);
  }

  private static void addFamily(Map<String, String> families, MDOType type, String suffixRu, String suffixEn) {
    var names = type.fullName();
    var family = (names.getRu() + suffixRu).toLowerCase(Locale.ROOT);
    families.put(family, family);
    families.put((names.getEn() + suffixEn).toLowerCase(Locale.ROOT), family);
  }
}
