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

import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.Mode;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMessage;
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MissingTypeDiagnosticTest extends AbstractDiagnosticTest<MissingTypeDiagnostic> {

  @Autowired
  private DefaultDiagnosticComputer diagnosticComputer;

  @Autowired
  private TypeService typeService;

  @Test
  void checksTheFirstWriteInAMarkedModule() {
    var document = TestUtils.getDocumentContext("""
      //@strict-types

      Процедура Проверить(Аргумент)
        Перем Данные;
        Данные = ВнешниеДанные();
        Данные = ПовторнаяЗагрузка();
        Аргумент = ПовторнаяЗагрузка();
        Число = 1;
        Настройки = ВнешниеДанные(); // Структура
      КонецПроцедуры
      """, context);

    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(tuple(Ranges.create(4, 2, 4, 8), "Объявите тип переменной «Данные» при инициализации"));
  }

  @Test
  void distinguishesInferredAnyFromAnExplicitDeclaration() {
    initServerContext();
    var document = TestUtils.getDocumentContext("""
      //@strict-types
      // Параметры:
      //  Вход - Произвольный
      //  Адрес - Строка
      Процедура Проверить(Вход, Адрес)
        Данные = Вход;
        ИзХранилища = ПолучитьИзВременногоХранилища(Адрес);
        Описанные = Вход; // Произвольный
      КонецПроцедуры
      """, context);

    assertThat(typeService.expressionTypesAt(document, new Position(5, 11)).refs()).containsExactly(TypeRef.ANY);
    assertThat(typeService.expressionTypesAt(document, new Position(6, 17)).refs()).doesNotContain(TypeRef.ANY);
    assertThat(getDiagnostics(document))
      .extracting(Diagnostic::getRange, diagnostic -> DiagnosticMessage.getStringValue(diagnostic.getMessage()))
      .containsExactly(
        tuple(Ranges.create(5, 2, 5, 8), "Объявите тип переменной «Данные» при инициализации"),
        tuple(Ranges.create(6, 2, 6, 13), "Объявите тип переменной «ИзХранилища» при инициализации"));
  }

  @Test
  void honorsOnModeOverridesInTheDiagnosticComputer() {
    initServerContext();
    var source = """
      Процедура Проверить()
        Данные = НеизвестнаяФункция();
      КонецПроцедуры
      """;
    var options = context.getLanguageServerConfiguration().getDiagnosticsOptions();
    var root = Path.of(context.getWorkspaceUri());
    var previous = options.getParameters();
    try {
      options.setParameters(Map.of("MissingType", Either.forLeft(true)));
      var enabled = TestUtils.getDocumentContext(root.resolve("Enabled.bsl").toUri(), source, context);
      assertThat(diagnosticComputer.compute(enabled))
        .filteredOn(diagnostic -> "MissingType".equals(diagnostic.getCode().getLeft()))
        .extracting(Diagnostic::getRange).containsExactly(Ranges.create(1, 2, 1, 8));

      options.setParameters(Map.of("MissingType", Either.forLeft(false)));
      var disabled = TestUtils.getDocumentContext(root.resolve("Disabled.bsl").toUri(),
        "//@strict-types\n" + source, context);
      assertThat(diagnosticComputer.compute(disabled))
        .filteredOn(diagnostic -> "MissingType".equals(diagnostic.getCode().getLeft())).isEmpty();
    } finally {
      options.setParameters(previous);
    }
  }

  @Test
  void respectsExplicitModeAndIgnoresALateMarkerInDefaultMode() {
    var document = TestUtils.getDocumentContext("""
      #Область Код
      //@strict-types
      Процедура Проверить()
        Данные = ВнешниеДанные();
      КонецПроцедуры
      #КонецОбласти
      """, context);
    assertThat(getDiagnostics(document)).isEmpty();

    var options = configuration.getDiagnosticsOptions();
    var previousMode = options.getMode();
    try {
      options.setMode(Mode.ONLY);
      assertThat(getDiagnostics(document)).extracting(Diagnostic::getRange)
        .containsExactly(Ranges.create(3, 2, 3, 8));
    } finally {
      options.setMode(previousMode);
    }
  }
}
