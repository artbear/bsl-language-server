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

import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static com.github._1c_syntax.bsl.languageserver.util.Assertions.assertThat;

class AssignToFormAttributeInNoContextDiagnosticTest
  extends AbstractDiagnosticTest<AssignToFormAttributeInNoContextDiagnostic> {
  private static final Path METADATA = Path.of("src/test/resources/metadata/designer");
  private static final Path FORM = METADATA.resolve("Catalogs/Справочник1/Forms/ФормаЭлемента/Ext/Form/Module.bsl");

  AssignToFormAttributeInNoContextDiagnosticTest() {
    super(AssignToFormAttributeInNoContextDiagnostic.class);
  }

  @Test
  void actualFormAttributesInNoContextMethods() {
    initServerContext(METADATA);
    var document = TestUtils.getDocumentContext(FORM.toUri(), """
      &НаСервереБезКонтекста
      Процедура ПолучитьКартинкуИзХранилища(Знач Картинка)
        ПолеКартинки = ПоместитьВоВременноеХранилище(Картинка);
        Объект.Наименование = "Новое имя";
        Неизвестный = 1;
      КонецПроцедуры
      &AtClientAtServerNoContext
      Procedure UpdatePicture()
        ПолеКартинки = "address";
      EndProcedure
      &НаСервере
      Процедура КонтекстныйМетод()
        ПолеКартинки = "адрес";
      КонецПроцедуры
      &НаКлиенте
      Процедура КлиентскийМетод()
        ПолеКартинки = "адрес";
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).hasSize(3);
  }

  @Test
  void explicitLocalsAndParametersShadowFormAttributes() {
    initServerContext(METADATA);
    var document = TestUtils.getDocumentContext(FORM.toUri(), """
      &НаСервереБезКонтекста
      Процедура Проверка(Объект)
        Перем ПолеКартинки;
        ПолеКартинки = "адрес";
        Объект.Наименование = "имя";
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }

  @Test
  void explicitModuleVariableShadowsAttribute() {
    initServerContext(METADATA);
    var document = TestUtils.getDocumentContext(FORM.toUri(), """
      Перем ПолеКартинки;
      &НаСервереБезКонтекста
      Процедура Проверка()
        ПолеКартинки = "локальная копия";
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }

  @Test
  void metadataIsRequired() {
    initServerContext();
    var document = TestUtils.getDocumentContext("""
      &НаСервереБезКонтекста
      Процедура Проверка()
        ПолеКартинки = "адрес";
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }
}
