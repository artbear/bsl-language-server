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

class MissingTypeDescriptionDiagnosticTest extends AbstractDiagnosticTest<MissingTypeDescriptionDiagnostic> {

  @Test
  void requiresTypesForExportedParametersAndReturnValues() {
    var document = TestUtils.getDocumentContext("""
      Функция Получить(Данные) Экспорт
        Возврат 1;
      КонецФункции

      // Параметры:
      //  Данные - Число - начальное значение.
      // Возвращаемое значение:
      //  Число - результат.
      Функция ПолучитьОписанный(Данные) Экспорт
        Возврат Данные;
      КонецФункции
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(diagnostic -> diagnostic.getRange().getStart().getCharacter())
      .containsExactlyInAnyOrder(8, 17);
  }

  @Test
  void checksDetachedLocalParametersAndKeepsDirectLocalCalls() {
    var document = TestUtils.getDocumentContext("""
      Процедура Выполнить()
        Внутренняя(1);
      КонецПроцедуры

      Процедура Внутренняя(Значение)
        Сообщить(Значение);
      КонецПроцедуры

      Процедура БезПрямогоВызова(Данные)
        Сообщить(Данные);
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(diagnostic -> diagnostic.getRange().getStart().getLine())
      .containsExactly(8);
  }
}
