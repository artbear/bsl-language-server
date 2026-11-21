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

import static org.assertj.core.api.Assertions.assertThat;

class TypeChangeDiagnosticTest extends AbstractDiagnosticTest<TypeChangeDiagnostic> {

  @Test
  void identifiesIncompatibleReassignmentsOfVariablesAndStructureKeys() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить()
        Итог = 10;
        Итог = 20;
        Итог = "строка";
        Данные = Новый Структура("Ключ", 0);
        Данные.КЛЮЧ = 1;
        Данные.ключ = "строка";
        Данные.НовыйКлюч = "строка";
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(diagnostic -> diagnostic.getRange().getStart().getLine())
      .containsExactly(3, 6);
  }

  @Test
  void acceptsAlternativeInitializationAndUndefined() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить(Условие)
        Если Условие Тогда
          Значение = 1;
        Иначе
          Значение = "строка";
        КонецЕсли;
        Значение = 2;
        Начальное = Неопределено;
        Начальное = 1;
        Начальное = Неопределено;
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document)).isEmpty();
  }
}
