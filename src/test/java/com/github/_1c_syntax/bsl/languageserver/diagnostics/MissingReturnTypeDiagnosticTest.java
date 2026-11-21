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
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissingReturnTypeDiagnosticTest extends AbstractDiagnosticTest<MissingReturnTypeDiagnostic> {

  @Test
  void reportsAnUnknownReturnEvenWhenAnotherBranchReturnsAKnownType() {
    var document = TestUtils.getDocumentContext("""
      Функция Получить(Условие)
        Если Условие Тогда
          Возврат НеизвестнаяФункция();
        КонецЕсли;
        Возврат 1;
      КонецФункции
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange)
      .containsExactly(Ranges.create(2, 12, 2, 32));
  }

  @Test
  void acceptsADescribedOrInferredResultAndIgnoresProcedures() {
    var document = TestUtils.getDocumentContext("""
      // Возвращаемое значение:
      //  Строка - внешний результат.
      Функция ОписанныйРезультат()
        Возврат НеизвестнаяФункция();
      КонецФункции

      Функция ИзвестныйРезультат()
        Возврат 1;
      КонецФункции

      Функция ПустойРезультат()
        Возврат Неопределено;
      КонецФункции

      Процедура Проверить()
        Значение = ИзвестныйРезультат();
        Значение.НесуществующееПоле = 1;
        Возврат;
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document)).isEmpty();
  }
}
