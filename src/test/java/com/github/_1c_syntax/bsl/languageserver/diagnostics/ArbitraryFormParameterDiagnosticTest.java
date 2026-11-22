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
import com.github._1c_syntax.bsl.languageserver.util.CleanupContextBeforeClassAndAfterClass;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@CleanupContextBeforeClassAndAfterClass
class ArbitraryFormParameterDiagnosticTest extends AbstractDiagnosticTest<ArbitraryFormParameterDiagnostic> {

  @Test
  void checksOnlyAnArbitraryParameterUsedToPackFormInitializationProperties() {
    initServerContext(TestUtils.PATH_TO_METADATA);
    var document = TestUtils.getDocumentContext("""
      &НаКлиенте
      Процедура Проверить(ИмяФормы)
        Данные = Новый Структура("Число", 10);
        ПараметрыОткрытия = Новый Структура("НачальныеДанные", Данные);
        ОткрытьФорму("Документ.Документ1.Форма.ФормаДокумента", ПараметрыОткрытия);
        OpenForm("Document.Документ1.Form.ФормаДокумента", New Structure("НачальныеДанные", 11));
        ОткрытьФорму("Документ.Документ1.Форма.ФормаДокумента", Новый Структура("РежимПодбора", Данные));
        ОткрытьФорму(ИмяФормы, ПараметрыОткрытия);
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(tuple(Ranges.create(4, 58, 4, 75),
        "Опишите поля структуры отдельными параметрами формы вместо «НачальныеДанные»"));
  }

  @Test
  void detectsAClientCallWhenTheUserMethodExistsOnlyOnTheServer() {
    initServerContext(TestUtils.PATH_TO_METADATA);
    var form = Path.of(TestUtils.PATH_TO_METADATA,
      "Documents/Документ1/Forms/ФормаДокумента/Ext/Form/Module.bsl").toAbsolutePath();
    var document = TestUtils.getDocumentContext(form.toUri(), """
      #Если Сервер Тогда
      &НаСервере
      Процедура ОткрытьФорму(Имя, ПараметрыОткрытия)
      КонецПроцедуры
      #КонецЕсли

      #Если Клиент Тогда
      &НаКлиенте
      Процедура Проверить()
        Данные = Новый Структура("Число", 10);
        ПараметрыОткрытия = Новый Структура("НачальныеДанные", Данные);
        ОткрытьФорму("Документ.Документ1.Форма.ФормаДокумента", ПараметрыОткрытия);
      КонецПроцедуры
      #КонецЕсли
      """, context);

    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange)
      .containsExactly(Ranges.create(11, 58, 11, 75));
  }

  @Test
  void ignoresAUserMethodNamedLikeThePlatformMethod() {
    initServerContext(TestUtils.PATH_TO_METADATA);
    var document = TestUtils.getDocumentContext("""
      Процедура ОткрытьФорму(Имя, Настройки)
      КонецПроцедуры

      Процедура Проверить()
        Данные = Новый Структура("Число", 10);
        ОткрытьФорму("Документ.Документ1.Форма.ФормаДокумента", Новый Структура("НачальныеДанные", Данные));
      КонецПроцедуры
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();
  }
}
