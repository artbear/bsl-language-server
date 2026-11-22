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

class DissimilarCollectionValuesDiagnosticTest extends AbstractDiagnosticTest<DissimilarCollectionValuesDiagnostic> {

  @Test
  void comparesSuccessiveAdditionsToAnArray() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить()
        Данные = Новый Массив;
        Данные.Добавить(10);
        ДАННЫЕ.Add(20);
        Данные.Вставить(0, "текст");
        Данные.Добавить(Истина);
        Данные.Добавить(НеизвестныеДанные());
      КонецПроцедуры
      """, context);

    var message = "Разделите значения разных типов в массиве «Данные»";
    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(
        tuple(Ranges.create(4, 21, 4, 28), message),
        tuple(Ranges.create(5, 18, 5, 24), message));
  }

  @Test
  void checksBothModuleBodiesAndTheMethodOnlyOnce() {
    var document = TestUtils.getDocumentContext("""
      Верх = Новый Массив;
      Верх.Добавить(10);
      Верх.Добавить("до");

      Процедура Проверить()
        Внутри = Новый Массив;
        Внутри.Добавить(10);
        Внутри.Добавить("внутри");
      КонецПроцедуры

      Низ = Новый Массив;
      Низ.Добавить(10);
      Низ.Добавить("после");
      """, context);

    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange)
      .containsExactlyInAnyOrder(Ranges.create(2, 14, 2, 18), Ranges.create(7, 18, 7, 26),
        Ranges.create(12, 13, 12, 20));
  }

  @Test
  void retainsTheDefiniteTypeAcrossAUnionValuedAddition() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить(Условие)
        Данные = Новый Массив;
        Данные.Добавить(10);
        Данные.Добавить(?(Условие, 1, "смесь"));
        Данные.Добавить("третье");
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange)
      .containsExactly(Ranges.create(4, 18, 4, 26));
  }

  @Test
  void keepsSeparateInstancesAndBranchesApart() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить(Условие)
        Данные = Новый Массив;
        Данные.Добавить(10);
        Данные.Очистить();
        Данные.Добавить("текст");
        Данные = Новый Массив;
        Данные.Добавить(20);
        Если Условие Тогда
          Вариант = Новый Массив;
          Вариант.Добавить("текст");
        Иначе
          Вариант = Новый Массив;
          Вариант.Добавить(10);
        КонецЕсли;
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }
}
