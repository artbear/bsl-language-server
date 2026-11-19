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

import com.github._1c_syntax.bsl.languageserver.context.AbstractServerContextAwareTest;
import com.github._1c_syntax.bsl.languageserver.diagnostics.infrastructure.DiagnosticObjectProvider;
import com.github._1c_syntax.bsl.languageserver.types.registry.BslContextHolder;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@TestPropertySource(properties = {
  "app.platform-context.enabled=true",
  "app.configuration.path=.bsl-language-server.json"
})
@EnabledIfEnvironmentVariable(named = "BSL_LANGUAGE_SERVER_RUN_HBK_TESTS", matches = "true",
  disabledReason = "Requires the installed 1C syntax helper")
class MissedRequiredParameterHbkTest extends AbstractServerContextAwareTest {

  @Autowired
  private BslContextHolder bslContextHolder;

  @Autowired
  private DiagnosticObjectProvider diagnosticObjectProvider;

  @Test
  void checksRequiredAndOptionalPlatformParameters(@TempDir Path workspace) throws IOException {
    FileUtils.copyDirectory(Path.of("src/test/resources/metadata/designer").toFile(), workspace.toFile());
    var binPath = System.getenv("BSL_LANGUAGE_SERVER_HBK_BIN");
    if (binPath != null && !binPath.isBlank()) {
      new JsonMapper().writeValue(workspace.resolve(".bsl-language-server.json").toFile(),
        Map.of("v8platform", Map.of("binPath", binPath)));
    }
    initServerContext(workspace, false);
    assertThat(bslContextHolder.get()).as("1C syntax helper").isPresent();
    var rule = diagnosticObjectProvider.get(MissedRequiredParameterDiagnostic.class);

    var example = TestUtils.getDocumentContext(workspace.resolve("example.bsl").toUri(), """
      Запрос = Новый Запрос(КакойТоТекстЗапроса);
      Запрос.УстановитьПараметр(1);
      """, context);
    assertThat(rule.getDiagnostics(example)).singleElement().satisfies(diagnostic -> {
      assertThat(diagnostic.getCode().getLeft()).isEqualTo("MissedRequiredParameter");
      assertThat(diagnostic.getRange().getStart().getLine()).isEqualTo(1);
      assertThat(diagnostic.getMessage().getLeft()).isEqualTo("Укажите обязательный параметр 'Значение'");
    });

    var variants = TestUtils.getDocumentContext(workspace.resolve("parameter-calls.bsl").toUri(), """
      Query = New Query("SELECT 1");
      Query.SetParameter("Name", 1);
      Query.SetParameter(, 1);
      Query.SetParameter("Name", );
      Query.SetParameter();
      Query.SetParameter("Name");
      Неизвестный.УстановитьПараметр();
      Структура = Новый Структура;
      Структура.Вставить("Имя");
      Структура.Вставить(, 1);
      Результат = Формат(1);
      Результат = Format();
      Результат = Лев("abc");
      Результат = Left("abc", 1);
      """, context);
    assertThat(rule.getDiagnostics(variants))
      .extracting(diagnostic -> diagnostic.getRange().getStart().getLine(), diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactly(
        tuple(2, "Укажите обязательный параметр 'Имя'"),
        tuple(3, "Укажите обязательный параметр 'Значение'"),
        tuple(4, "Укажите обязательный параметр 'Имя', 'Значение'"),
        tuple(5, "Укажите обязательный параметр 'Значение'"),
        tuple(9, "Укажите обязательный параметр 'Ключ'"),
        tuple(11, "Укажите обязательный параметр 'Значение'"),
        tuple(12, "Укажите обязательный параметр 'ЧислоСимволов'"));

    var metadataCalls = TestUtils.getDocumentContext(workspace.resolve("metadata-calls.bsl").toUri(), """
      Результат = Справочники.Справочник1.НайтиПоКоду();
      Результат = Catalogs.Справочник1.FindByCode("0001");
      Результат = Catalogs.Справочник1.FindByCode(, True);
      """, context);
    assertThat(rule.getDiagnostics(metadataCalls))
      .extracting(diagnostic -> diagnostic.getRange().getStart().getLine(), diagnostic -> diagnostic.getMessage().getLeft())
      .containsExactly(
        tuple(0, "Укажите обязательный параметр 'Код'"),
        tuple(2, "Укажите обязательный параметр 'Код'"));
  }
}
