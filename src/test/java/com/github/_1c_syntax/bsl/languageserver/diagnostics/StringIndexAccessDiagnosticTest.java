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
class StringIndexAccessDiagnosticTest extends AbstractDiagnosticTest<StringIndexAccessDiagnostic> {

  @Test
  void checksOnlyLiteralNamesOfExistingFormItems() {
    initServerContext(TestUtils.PATH_TO_METADATA);
    var module = Path.of(TestUtils.PATH_TO_METADATA,
      "Documents/Документ1/Forms/ФормаДокумента/Ext/Form/Module.bsl").toAbsolutePath();
    var document = TestUtils.getDocumentContext(module.toUri(), """
      &НаКлиенте
      Процедура Проверить(Имя)
        Элементы["Номер"].Видимость = Ложь;
        Элементы[Имя].Видимость = Ложь;
        Элементы["НеизвестныйЭлемент"].Видимость = Ложь;
        Элементы[0].Видимость = Ложь;
        Соответствие = Новый Соответствие;
        Значение = Соответствие["Номер"];
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(tuple(Ranges.create(2, 10, 2, 19), "Обратитесь к элементу через точку по имени «Номер»"));
  }
}
