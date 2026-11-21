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
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@CleanupContextBeforeClassAndAfterClass
class UndeclaredFormParameterDiagnosticTest extends AbstractDiagnosticTest<UndeclaredFormParameterDiagnostic> {

  @Test
  void checksActualFormParametersAndIgnoresUnrelatedMembers() {
    initServerContext(TestUtils.PATH_TO_METADATA);
    var module = Path.of(TestUtils.PATH_TO_METADATA,
      "Documents/Документ1/Forms/ФормаДокумента/Ext/Form/Module.bsl").toAbsolutePath();
    var document = TestUtils.getDocumentContext(module.toUri(), """
      &НаСервере
      Процедура ПриСозданииНаСервере(Отказ, СтандартнаяОбработка)
        Значение = Параметры.НеописанныйПараметр;
        Режим = Параметры.РежимПодбора;
        Элемент = Элементы.НеизвестныйЭлемент;
        Данные = Новый Структура("Ключ", 10);
        Ключ = Данные.НеизвестныйКлюч;
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange)
      .containsExactly(Ranges.create(2, 23, 2, 42));
  }

  @Test
  void ignoresParametersOfOrdinaryModuleMethods() {
    var document = TestUtils.getDocumentContext("""
      Процедура Проверить(Параметры)
        Значение = Параметры.НеописанныйПараметр;
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document)).isEmpty();
  }
}
