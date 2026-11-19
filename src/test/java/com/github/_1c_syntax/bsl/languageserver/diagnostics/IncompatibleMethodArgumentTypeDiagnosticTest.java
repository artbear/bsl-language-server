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

import com.github._1c_syntax.bsl.languageserver.context.FileType;
import com.github._1c_syntax.bsl.languageserver.types.model.MemberDescriptor;
import com.github._1c_syntax.bsl.languageserver.types.model.ParameterDescriptor;
import com.github._1c_syntax.bsl.languageserver.types.model.SignatureDescriptor;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.types.registry.TypeRegistry;
import com.github._1c_syntax.bsl.languageserver.util.CleanupContextBeforeClassAndAfterEachTestMethod;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import com.github._1c_syntax.utils.Absolute;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.github._1c_syntax.bsl.languageserver.util.Assertions.assertThat;

@CleanupContextBeforeClassAndAfterEachTestMethod
class IncompatibleMethodArgumentTypeDiagnosticTest
  extends AbstractDiagnosticTest<IncompatibleMethodArgumentTypeDiagnostic> {

  @Autowired
  private TypeRegistry typeRegistry;

  IncompatibleMethodArgumentTypeDiagnosticTest() {
    super(IncompatibleMethodArgumentTypeDiagnostic.class);
  }

  @Test
  void reportsOnlyProvenMismatchesOnArgumentRanges() {
    var diagnostics = getDiagnostics();
    assertThat(diagnostics, true).hasSize(5)
      .hasRange(8, 10, 8, 22)
      .hasRange(9, 10, 9, 22)
      .hasRange(14, 10, 14, 18)
      .hasRange(29, 10, 29, 17)
      .hasRange(30, 10, 30, 34);
    org.assertj.core.api.Assertions.assertThat(diagnostics)
      .extracting(Diagnostic::getSeverity).containsOnly(DiagnosticSeverity.Error);
  }

  @Test
  void checksExternalModulesAndCatalogObjectMethods() {
    var fixture = Absolute.path("src/test/resources/metadata/designer");
    initServerContext(fixture);
    var method = """
      // Параметры:
      //  Значение - СправочникСсылка.Справочник1 - ссылка.
      Процедура Принять(Значение) Экспорт
      КонецПроцедуры
      """;
    TestUtils.getDocumentContext(fixture.resolve("CommonModules/ПервыйОбщийМодуль/Ext/Module.bsl").toUri(),
      method, context);
    TestUtils.getDocumentContext(fixture.resolve("Catalogs/Справочник1/Ext/ObjectModule.bsl").toUri(),
      method, context);
    var document = TestUtils.getDocumentContext("""
      // Параметры:
      //  Объект - СправочникОбъект.Справочник1 - объект.
      //  Ссылка - СправочникСсылка.Справочник1 - ссылка.
      Процедура Проверка(Объект, Ссылка, Неизвестно)
          ПервыйОбщийМодуль.Принять(42);
          ПервыйОбщийМодуль.Принять(Ссылка);
          Объект.Принять(Неизвестно);
          Объект.Принять(Объект);
          Справочники.Справочник1.СоздатьЭлемент().Принять(42);
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document), true).hasSize(3)
      .hasRange(4, 30, 4, 32)
      .hasRange(7, 19, 7, 25)
      .hasRange(8, 53, 8, 55);
  }

  @Test
  void checksEachOverloadAsAWhole() {
    initServerContext();
    var receiver = typeRegistry.registerConfigurationType("ТестовыйОбъект");
    var number = TypeSet.of(typeRegistry.resolve("Число").orElseThrow());
    var text = TypeSet.of(typeRegistry.resolve("Строка").orElseThrow());
    typeRegistry.registerOwnMemberSource(receiver, () -> List.of(MemberDescriptor.method("Метод", List.of(
      overload(number, text), overload(text, number)))), FileType.BSL);

    var document = TestUtils.getDocumentContext("""
      // Параметры:
      //  Объект - ТестовыйОбъект - объект.
      Процедура Проверка(Объект, Неизвестно)
          Объект.Метод(1, "строка");
          Объект.Метод("строка", 1);
          Объект.Метод(Неизвестно, 1);
          Объект.Метод(1, Неизвестно);
          Объект.Метод(1, 2);
          Объект.Метод(Ложь, 1);
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document), true).hasSize(3)
      .hasRange(7, 17, 7, 18)
      .hasRange(7, 20, 7, 21)
      .hasRange(8, 17, 8, 21);
  }

  private static SignatureDescriptor overload(TypeSet first, TypeSet second) {
    return SignatureDescriptor.of(List.of(
      new ParameterDescriptor("Первый", first, false, ""),
      new ParameterDescriptor("Второй", second, false, "")));
  }
}
