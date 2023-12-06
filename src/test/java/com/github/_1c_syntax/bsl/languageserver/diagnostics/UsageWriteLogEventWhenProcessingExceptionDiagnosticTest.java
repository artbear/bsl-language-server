/*
 * This file is a part of BSL Language Server.
 *
 * Copyright (c) 2018-2023
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

import java.util.List;
import java.util.stream.Collectors;

import static com.github._1c_syntax.bsl.languageserver.util.Assertions.assertThat;

class UsageWriteLogEventWhenProcessingExceptionDiagnosticTest extends AbstractDiagnosticTest<UsageWriteLogEventWhenProcessingExceptionDiagnostic> {
  UsageWriteLogEventWhenProcessingExceptionDiagnosticTest() {
    super(UsageWriteLogEventWhenProcessingExceptionDiagnostic.class);
  }

  @Test
  void test() {

    List<Diagnostic> diagnostics = getDiagnostics().stream()
      .sorted((o1, o2) -> Ranges.compare(o1.getRange(), o2.getRange()))
      .collect(Collectors.toList());

    assertThat(diagnostics, true)
      .hasRange(4, 4, 16)
      .hasRange(13, 4, 16)
      .hasRange(36, 4, 16)
      .hasRange(50, 4, 16)
      .hasRange(79, 4, 16)
      .hasRange(156, 8, 20)
      .hasRange(166, 8, 20)
      .hasSize(7)
    ;
  }

  // TODO удалить все тесты для строк, оставить только общий тест всего файла
  @Test
  void testEmptyExceptionBlock() {

    var code = "Процедура Тест1()\n" +
      "    Попытка\n" +
      "        Объект.Записать();\n" +
      "    Исключение\n" +
      "    КонецПопытки; // замечание\n" +
      "КонецПроцедуры\n";

    final var documentContext = TestUtils.getDocumentContext(code);
    List<Diagnostic> diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics, true)
      .hasSize(1);
  }

  @Test
  void testRaiseInsideExceptBlock() {

    var code = "Процедура Тест9()\n" +
      "    СтрокаXML = \"\";\n" +
      "    Попытка\n" +
      "        СтрокаXML = НастройкиПодключенияВXML(НастройкиПодключения);\n" +
      "    Исключение\n" +
      "        Возврат;\n" +
      "    КонецПопытки; // нет замечания\n" +
      "КонецПроцедуры\n";

    final var documentContext = TestUtils.getDocumentContext(code);
    List<Diagnostic> diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics, true).isEmpty();
  }

  @Test
  void testSimpleCodeInsideTry() {

    var code = "Процедура Тест1()\n" +
      "    //Попытка\n" +
      "        Объект.Записать();\n" +
      "    //Исключение\n" +
      "    //КонецПопытки; // замечание\n" +
      "КонецПроцедуры\n" +
      "\n" +
      "Процедура Тест2()\n" +
      "    //Попытка\n" +
      "        Объект.Записать();\n" +
      "    //Исключение\n" +
      "        Отказ = Истина;\n" +
      "        ОписаниеОшибки = ОписаниеОшибки();\n" +
      "    //КонецПопытки; // замечание\n" +
      "КонецПроцедуры\n" +
      "Процедура Тест12()\n" +
      "    Попытка\n" +
      "        СписокРеквизит = Форма.Список;\n" +
      "    Исключение\n" +
      "        СписокРеквизит = Неопределено;\n" +
      "    КонецПопытки; // TODO стоит игнорировать подобные простые вызовы без вызова любых неплатформенных методов? можно опцию сделать\n" +
      "\n" +
      "    Попытка\n" +
      "        НаЧтоЗаменить = ?(ВырезатьОбрамление, \"\", НачалоПараметра) + Формат(КлючИЗначение.Значение, ФорматнаяСтрока) + ?(ВырезатьОбрамление, \"\", КонецПараметра);\n" +
      "    Исключение\n" +
      "        НаЧтоЗаменить = ?(ВырезатьОбрамление, \"\", НачалоПараметра) + КлючИЗначение.Значение + ?(ВырезатьОбрамление, \"\", КонецПараметра);\n" +
      "    КонецПопытки; // TODO стоит игнорировать подобные простые вызовы без вызова любых неплатформенных методов? можно опцию сделать\n" +
      "КонецПроцедуры\n";

    final var documentContext = TestUtils.getDocumentContext(code);
    List<Diagnostic> diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics, true).isEmpty();
  }

  @Test
  void testRaiseInsideMethodCalledFromExceptBlock() {

    var code = "Процедура НеУдалосьСформироватьПечатнуюФорму_Тест15(ИнформацияОбОшибке)\n" +
      "    ВызватьИсключение ИнформацияОбОшибке;\n" +
      "КонецПроцедуры\n" +
      "\n" +
      "Процедура Тест15()\n" +
      "    Попытка\n" +
      "        COMОбъект = Новый COMОбъект(\"Word.Application\");\n" +
      "    Исключение\n" +
      "        НеУдалосьСформироватьПечатнуюФорму_Тест15(ИнформацияОбОшибке());\n" +
      "    КонецПопытки; // TODO нет замечания\n" +
      "КонецПроцедуры\n";

    final var documentContext = TestUtils.getDocumentContext(code);
    List<Diagnostic> diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics, true).isEmpty();
  }
}
