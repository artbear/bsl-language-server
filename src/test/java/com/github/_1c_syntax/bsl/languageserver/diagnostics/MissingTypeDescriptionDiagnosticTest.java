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

import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMessage;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactlyInAnyOrder(
        tuple(Ranges.create(0, 8, 0, 16), "Опишите тип возвращаемого значения"),
        tuple(Ranges.create(0, 17, 0, 23), "Опишите тип параметра «Данные»"));
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
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(tuple(Ranges.create(8, 27, 8, 33), "Опишите тип параметра «Данные»"));
  }

  @Test
  void preservesMissingTypesWhenOnlyOneParameterUsesASeeLink() {
    var document = TestUtils.getDocumentContext("""
      // Параметры:
      //  Вход - См. Документированная
      Функция Частичная(Вход, Остаток) Экспорт
        Возврат 1;
      КонецФункции

      // См. Документированная
      Функция Переиспользует(Вход) Экспорт
        Возврат 1;
      КонецФункции
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactlyInAnyOrder(
        tuple(Ranges.create(2, 8, 2, 17), "Опишите тип возвращаемого значения"),
        tuple(Ranges.create(2, 24, 2, 31), "Опишите тип параметра «Остаток»"));
  }
}
