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

import com.github._1c_syntax.bsl.languageserver.util.CleanupContextBeforeClassAndAfterClass;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.utils.Absolute;
import org.assertj.core.groups.Tuple;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static com.github._1c_syntax.bsl.languageserver.util.TestUtils.PATH_TO_METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@CleanupContextBeforeClassAndAfterClass
class LostVariableDiagnosticTest extends AbstractDiagnosticTest<LostVariableDiagnostic> {

  @Test
  void originalExamples() {
    assertThat(getDiagnostics()).extracting(Diagnostic::getRange, diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactlyInAnyOrder(
        assignment(3, 4, "Значение"), assignment(4, 4, "Значение"), assignment(5, 4, "МояПеременная"),
        assignment(6, 4, "МояПеременная"), assignment(10, 4, "ТекстЗапроса"), assignment(24, 4, "ТекстЗапроса"),
        assignment(32, 4, "ТекстЗапроса"), assignment(54, 7, "ТекстЗапроса"), assignment(70, 2, "ТекстЗапроса"),
        assignment(81, 6, "Запрос"), assignment(83, 2, "Запрос"), assignment(92, 10, "Запрос"),
        assignment(96, 2, "Запрос"), assignment(100, 2, "ТекстЗапроса"), assignment(102, 6, "ТекстЗапроса"),
        assignment(112, 4, "Файл"), assignment(117, 4, "Файл"), assignment(122, 4, "ЛокальнаяПеременная"),
        assignment(124, 8, "ЛокальнаяПеременная"), assignment(126, 8, "ЛокальнаяПеременная"),
        assignment(128, 8, "ЛокальнаяПеременная"), assignment(140, 4, "Комментарий"),
        assignment(140, 21, "Комментарий"), assignment(148, 4, "Комментарий"),
        assignment(151, 4, "ТекстОповещения"), assignment(154, 4, "ТекстЗапроса"),
        assignment(160, 4, "ВидПрава"), assignment(161, 4, "ВидПрава"), assignment(164, 4, "НовыйПереход"),
        assignment(165, 4, "НовыйПереход"), assignment(179, 4, "ПорцияЭлементовДанных"),
        assignment(206, 8, "ЭтоОшибкаБлокировки"), assignment(211, 8, "ЭтоОшибкаБлокировки"),
        assignment(217, 4, "ДоступныйОбъем"), assignment(240, 4, "мСохраненныйДок"),
        assignment(244, 4, "мСохраненныйДок"), assignment(255, 4, "Представление"),
        assignment(276, 8, "ЛишниеТэги"), assignment(277, 8, "ЛишниеТэги"), assignment(298, 4, "ТекстЗапроса"),
        assignment(306, 4, "ЗначениеМодуля"), assignment(312, 4, "ТекстЗапроса"),
        assignment(315, 4, "ТекстЗапроса"), assignment(319, 4, "ЗначениеМодуля"),
        expected(337, 20, "Элем22", "nestedIterator"), assignment(345, 12, "Значение23"),
        assignment(346, 12, "Значение23"), assignment(352, 4, "Значение24"), assignment(355, 12, "Значение24"),
        assignment(377, 0, "ТекстЗапросаВБлоке"), assignment(381, 0, "ЗначениеМодуля"));
  }

  @Test
  void controlFlowAndLoopHeaders() {
    assertThat(getDiagnostics("LostVariableFlow"))
      .extracting(Diagnostic::getRange, diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactlyInAnyOrder(
        assignment(4, 4, "Х"), assignment(7, 4, "Х"), assignment(8, 4, "Выход"), assignment(11, 4, "Локальный"),
        assignment(34, 4, "Результат"), expected(52, 16, "кОЛЛекция", "collectionIterator"),
        expected(63, 20, "Сч", "nestedIterator"), expected(67, 16, "ОбщийСчетчик", "moduleIterator"),
        expected(70, 8, "ОбщийСчетчик", "moduleIterator"), assignment(79, 4, "Значение"),
        expected(85, 13, "ITEMS", "collectionIterator"), assignment(130, 8, "Элемент"),
        expected(146, 16, "кОЛЛекция", "collectionIterator"), assignment(166, 4, "Индекс"),
        assignment(183, 8, "Значение"), assignment(207, 4, "Значение"));
  }

  @Test
  void formShadowingUsesOnlyTheOwningFormAndExplicitDeclarations() {
    initServerContext(Absolute.path(PATH_TO_METADATA));
    var root = Path.of(PATH_TO_METADATA);
    var document = context.addDocument(Absolute.uri(root.resolve(
      "Documents/Документ1/Forms/ФормаДокумента/Ext/Form/Module.bsl").toFile()));
    var catalog = context.addDocument(Absolute.uri(root.resolve(
      "Catalogs/Справочник1/Forms/ФормаЭлемента/Ext/Form/Module.bsl").toFile()));
    var content = getText("LostVariableForm");
    context.rebuildDocument(document, content, 1);
    context.rebuildDocument(catalog, content, 1);
    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange, diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactly(assignment(13, 4, "НовыйИтог"));
    diagnosticInstance.configure(Map.of("checkFormShadowing", true));
    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange, diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactlyInAnyOrder(
        expected(2, 20, "таблицаПодбора", "formShadow"), expected(2, 36, "Parameters", "formShadow"),
        expected(3, 10, "Параметры", "formShadow"), assignment(13, 4, "НовыйИтог"));
    assertThat(getDiagnostics(catalog)).extracting(Diagnostic::getRange, diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactlyInAnyOrder(
        expected(2, 36, "Parameters", "formShadow"), expected(3, 10, "Параметры", "formShadow"),
        assignment(11, 4, "ТаблицаПодбора"), assignment(12, 4, "ТаблицаПодбора"), assignment(13, 4, "НовыйИтог"));
    assertThat(getDiagnostics("LostVariableForm")).noneMatch(d -> d.getRange().getStart().getLine() < 3);
  }

  private Tuple assignment(int line, int column, String name) {
    return expected(line, column, name, "diagnosticMessage");
  }

  private Tuple expected(int line, int column, String name, String messageKey) {
    return tuple(Ranges.create(line - 1, column, column + name.length()),
      diagnosticInstance.getInfo().getResourceString(messageKey, name));
  }
}
