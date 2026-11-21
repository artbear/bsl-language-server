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

class DeclaredTypeMismatchDiagnosticTest extends AbstractDiagnosticTest<DeclaredTypeMismatchDiagnostic> {

  @Test
  void checksAFieldAgainstTheDeclaredContract() {
    var document = TestUtils.getDocumentContext("""
      // Параметры:
      //  Данные - Структура:
      //  * Сумма - Число
      Процедура Проверить(Данные, Имя)
        Данные.Вставить("Сумма", "текст");
        Данные.Сумма = "текст";
        Данные["Сумма"] = "текст";
        Данные["Сумма"] = 10;
        Данные[Имя] = "текст";
        Данные.НеописанныйКлюч = "текст";
        Данные.Сумма = ВнешнийПоставщик();
      КонецПроцедуры
      """, context);

    var message = "Присвойте полю «Сумма» значение объявленного типа";
    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(tuple(Ranges.create(4, 27, 4, 34), message),
        tuple(Ranges.create(5, 17, 5, 24), message),
        tuple(Ranges.create(6, 20, 6, 27), message));
  }

  @Test
  void doesNotTreatInferredKeysAsAnExplicitContract() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить()
        Данные = Новый Структура("Сумма", 0);
        Данные.Вставить("Сумма", "текст");
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }
}
