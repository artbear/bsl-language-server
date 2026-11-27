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
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.github._1c_syntax.bsl.languageserver.util.Assertions.assertThat;

class MissingQueryParameterDiagnosticTest extends AbstractDiagnosticTest<MissingQueryParameterDiagnostic> {
  MissingQueryParameterDiagnosticTest() {
    super(MissingQueryParameterDiagnostic.class);
  }

  @Test
  void historicalFixture() {
    assertThat(getDiagnostics(), true)
      .hasRange(7, 34, 44)
      .hasRange(35, 34, 44)
      .hasRange(63, 34, 44)
      .hasSize(3);
  }

  @Test
  void testNewQuery() {
    var sample =
      "  Запрос1 = Новый Запрос(\n" +
        "    \"ВЫБРАТЬ\n" +
        "    |  Справочник1.Ссылка КАК Ссылка\n" +
        "    |ИЗ\n" +
        "    |  Справочник.Справочник1 КАК Справочник1\n" +
        "    |ГДЕ\n" +
        "    |  Справочник1.Ссылка = &Параметр\");\n" +
        "\n" +
        "  РезультатЗапроса = Запрос1.Выполнить();\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).hasSize(1);
  }

  @Test
  void testNewQueryNoError() {
    var sample =
      "  Запрос2 = Новый Запрос(\n" +
        "    \"ВЫБРАТЬ\n" +
        "    |  Справочник2.Ссылка КАК Ссылка\n" +
        "    |ИЗ\n" +
        "    |  Справочник.Справочник2 КАК Справочник2\n" +
        "    |ГДЕ\n" +
        "    |  Справочник2.Ссылка = &Параметр2\");\n" +
        "Запрос2.УстановитьПараметр(\"Параметр2\", Ссылка);\n" +
        "  РезультатЗапроса = Запрос2.Выполнить();\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testQueryTextNoError() {
    var sample =
      "Процедура Четвертая_НетОшибки_ЗапросТекст()\n" +
        "\n" +
        "    Запрос4 = Новый Запрос;\n" +
        "    Запрос4.Текст = \"ВЫБРАТЬ\n" +
        "        |    Справочник4.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник4 КАК Справочник4\n" +
        "        |ГДЕ\n" +
        "        |    Справочник4.Ссылка = &Параметр4\"; // нет ошибки\n" +
        "\n" +
        "    Запрос4.УстановитьПараметр(\"Параметр4\", Ссылка);\n" +
        "\n" +
        "    РезультатЗапроса = Запрос4.Выполнить();\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testQueryTextWithoutNewQuery() {
    var sample =
      "Процедура Шестой_ТекстБезСозданияЗапроса()\n" +
        "    НеиспользуемыйТекст6 = \"ВЫБРАТЬ\n" +
        "        |    Справочник6.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник6 КАК Справочник6\n" +
        "        |ГДЕ\n" +
        "        |    Справочник6.Ссылка = &Параметр6\"; // не ошибка\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testQueryTextWithNewQueryForOtherText() {
    var sample =
      "Процедура Седьмой_СначалаТекстПотомЗапросСДругимТекстом(Текст)\n" +
        "    НеиспользуемыйТекст7 = \"ВЫБРАТЬ\n" +
        "        |    Справочник7.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник7 КАК Справочник7\n" +
        "        |ГДЕ\n" +
        "        |    Справочник7.Ссылка = &Параметр7\"; // не ошибка\n" +
        "    Запрос7 = Новый Запрос(Текст);\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testQueryTextThenNewQuery() {
    var sample =
      "Процедура ПятаяОшибка_СначалаТекстПотомНовыйЗапрос()\n" +
        "\n" +
        "    Текст5 = \"ВЫБРАТЬ\n" +
        "        |    Справочник1.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник1 КАК Справочник1\n" +
        "        |ГДЕ\n" +
        "        |    Справочник1.Ссылка = &Параметр5\"; // ошибка\n" +
        "    Запрос5 = Новый Запрос(Текст5);\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).hasSize(1);
  }

  @Test
  void testSetParamBeforeQueryText() {
    var sample =
      "Процедура Восьмой_СначалаНовыйЗапросДалееУстановкаПараметраЗатемТекстЗапросаСПовторениемПараметра(Ссылка)\n" +
        "    Запрос = Новый Запрос;\n" +
        "    Запрос.УстановитьПараметр(\"Параметр\", Ссылка);\n" +
        "    Запрос.Текст = \"ВЫБРАТЬ\n" +
        "        |    Справочник1.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник1 КАК Справочник1\n" +
        "        |ГДЕ\n" +
        "        |    Справочник1.Ссылка = &Параметр\n" +
        "         |\"; // нет ошибки\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testSetParamBeforeQueryTextWithFewParams() {
    var sample =
      "Процедура Девятый_СначалаНовыйЗапросДалееУстановкаПараметраЗатемТекстЗапросаСПовторениемПараметра(Ссылка)\n" +
        "\n" +
        "    Запрос = Новый Запрос;\n" +
        "    Запрос.УстановитьПараметр(\"Параметр\", Ссылка);\n" +
        "    Запрос.Текст = \"ВЫБРАТЬ\n" +
        "        |    Справочник1.Ссылка КАК Ссылка\n" +
        "        |ИЗ\n" +
        "        |    Справочник.Справочник1 КАК Справочник1\n" +
        "        |ГДЕ\n" +
        "        |    Справочник1.Ссылка = &Параметр\n" +
        "        |\n" +
        "        |ОБЪЕДИНИТЬ ВСЕ\n" +
        "        |\n" +
        "        |ВЫБРАТЬ\n" +
        "         |    Справочник1.Ссылка КАК Ссылка\n" +
        "         |ИЗ\n" +
        "         |    Справочник.Справочник1 КАК Справочник1\n" +
        "         |ГДЕ\n" +
        "         |    Справочник1.Ссылка = &Параметр\n" +
        "         |\"; // нет ошибки\n" +
        "\n" +
        "\n" +
        "КонецПроцедуры\n";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void testStrReplaceForQueryText() {
    var sample =
      "Процедура Десятый_СначалаНовыйЗапросДалееУстановкаПараметраЗатемТекстЗапросаСПовторениемПараметра(Ссылка)\n" +
        "\n" +
        "    ТекстЗапроса = \"ВЫБРАТЬ * ИЗ &ИмяТаблицы ГДЕ ЛОЖЬ\"; // не ошибка\n" +
        "    ТекстЗапроса = СтрЗаменить(ТекстЗапроса, \"&ИмяТаблицы\", ИсточникДанных);\n" +
        "    Запрос = Новый Запрос(ТекстЗапроса);\n" +
        "КонецПроцедуры";

    var documentContext = TestUtils.getDocumentContext(sample, context);
    var diagnostics = getDiagnostics(documentContext);

    assertThat(diagnostics).isEmpty();
  }

  static Stream<Arguments> scenarios() {
    return Stream.of(
      Arguments.of("Q01", 1, 4, "1/30/32", "missing", ""),
      Arguments.of("Q02", 6, 11, "6/30/37", "missing", "7/10/19"),
      Arguments.of("Q03", 13, 18, "", "clear", ""),
      Arguments.of("Q04", 20, 26, "21/30/35", "missing", ""),
      Arguments.of("Q05", 28, 35, "31/30/40", "missing", ""),
      Arguments.of("Q06", 37, 41, "38/23/31", "missing", ""),
      Arguments.of("Q07", 43, 47, "43/43/48,43/56/62", "missing", ""),
      Arguments.of("Q08", 49, 52, "49/52/58", "missing", ""),
      Arguments.of("Q09", 54, 58, "54/35/43", "missing", ""),
      Arguments.of("Q10", 60, 63, "60/68/75", "missing", ""),
      Arguments.of("Q11", 65, 71, "65/30/35", "possible", ""),
      Arguments.of("Q12", 73, 81, "", "clear", ""),
      Arguments.of("Q13", 83, 89, "83/30/35", "possible", ""),
      Arguments.of("Q14", 91, 102, "", "clear", ""),
      Arguments.of("Q15", 104, 114, "", "clear", ""),
      Arguments.of("Q16", 116, 121, "116/30/32", "missing", ""),
      Arguments.of("Q17", 123, 126, "", "unknown", ""),
      Arguments.of("Q18", 128, 132, "", "unknown", ""),
      Arguments.of("Q19", 134, 138, "", "unknown", ""),
      Arguments.of("Q20", 140, 143, "", "clear", ""),
      Arguments.of("Q21", 145, 148, "", "unknown", ""),
      Arguments.of("Q22", 150, 157, "151/30/32", "missing", ""),
      Arguments.of("Q23", 159, 163, "", "clear", ""),
      Arguments.of("Q24", 165, 168, "242/21/31", "missing", ""),
      Arguments.of("Q25", 170, 175, "171/15/22", "missing", ""),
      Arguments.of("Q26", 177, 182, "178/35/40", "missing", ""),
      Arguments.of("Q27", 184, 188, "", "unknown", ""),
      Arguments.of("Q28", 190, 196, "190/30/35", "possible", ""),
      Arguments.of("Q29", 198, 205, "198/30/35", "missing", "200/14/23,202/14/28"),
      Arguments.of("Q30", 207, 212, "", "clear", ""),
      Arguments.of("Q31", 214, 223, "214/30/35", "possible", ""),
      Arguments.of("Q32", 225, 236, "225/30/35", "possible", "")
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  void scenario(String id, int first, int last, String primary, String outcome, String related) {
    var lines = getText("MissingQueryParameterScenarios").lines().toList();
    var source = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      if (i + 1 >= first && i + 1 <= last || i + 1 >= 238) {
        source.append(lines.get(i));
      }
      source.append('\n');
    }
    var diagnostics = getDiagnostics(TestUtils.getDocumentContext(source.toString(), context));
    org.assertj.core.api.Assertions.assertThat(diagnostics).as(id)
      .extracting(diagnostic -> coordinates(diagnostic.getRange()))
      .containsExactlyInAnyOrderElementsOf(split(primary));
    for (Diagnostic diagnostic : diagnostics) {
      var name = source.toString().lines().toList().get(diagnostic.getRange().getStart().getLine())
        .substring(diagnostic.getRange().getStart().getCharacter() + 1, diagnostic.getRange().getEnd().getCharacter());
      var messageKey = outcome.equals("possible") ? "possible" : "diagnosticMessage";
      org.assertj.core.api.Assertions.assertThat(diagnostic.getMessage().getLeft())
        .isEqualTo(diagnosticInstance.getInfo().getResourceString(messageKey, name));
      if (!related.isEmpty()) {
        org.assertj.core.api.Assertions.assertThat(diagnostic.getRelatedInformation())
          .extracting(info -> coordinates(info.getLocation().getRange()))
          .containsExactlyInAnyOrderElementsOf(split(related));
      }
    }
  }

  @Test
  void querySnapshotAndMutation() throws Exception {
    var original = Files.readString(Path.of("src/test/resources/semantictokens/snapshots/query.bsl"));
    assertThat(getDiagnostics(TestUtils.getDocumentContext(original, context))).isEmpty();
    var mutant = original.replaceFirst("(?m)^.*УстановитьПараметр\\(\"Дата\", Дата\\);\\R", "");
    var diagnostics = getDiagnostics(TestUtils.getDocumentContext(mutant, context));
    assertThat(diagnostics, true).hasRange(13, 45, 50).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(diagnostics.getFirst().getRelatedInformation())
      .extracting(info -> coordinates(info.getLocation().getRange())).containsExactly("22/16/25");
  }

  private static List<String> split(String ranges) {
    return ranges.isEmpty() ? List.of() : Arrays.asList(ranges.split(","));
  }

  @Test
  void queryTextPassedByReference() {
    var source = """
      Procedure Run()
          Q = New Query("SELECT &Template AS X");
          Prepare(Q.Text);
          Q.Execute();
      EndProcedure
      Procedure Prepare(Text)
          Text = StrReplace(Text, "&Template", "&Actual");
      EndProcedure
      """;
    var diagnostics = getDiagnostics(TestUtils.getDocumentContext(source, context));
    assertThat(diagnostics, true).hasRange(6, 42, 49).hasSize(1);
    var external = source.replace("Prepare(Q.Text);", "Service.Prepare(Q.Text);");
    assertThat(getDiagnostics(TestUtils.getDocumentContext(external, context))).isEmpty();
  }

  @Test
  void byValueRebindingDoesNotReplaceTheCallersObject() {
    var source = """
      Procedure Run()
          Q = New Query("SELECT &Original");
          Prepare(Q);
          Q.Execute();
      EndProcedure
      Procedure Prepare(Val Q)
          Q = New Query("SELECT &Replacement");
          Q.SetParameter("Replacement", 1);
      EndProcedure
      """;
    assertThat(getDiagnostics(TestUtils.getDocumentContext(source, context)), true).hasRange(1, 26, 35).hasSize(1);
  }

  @Test
  void splitParameterRetainsItsTwoSourceFragments() {
    var source = """
      Q = New Query("SELECT &" + "Name AS Field");
      Q.Execute();
      """;
    var diagnostics = getDiagnostics(TestUtils.getDocumentContext(source, context));
    assertThat(diagnostics, true).hasRange(0, 14, 42).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(diagnostics.getFirst().getRelatedInformation())
      .extracting(info -> coordinates(info.getLocation().getRange()))
      .containsExactlyInAnyOrder("1/2/9", "0/22/23", "0/28/32");
  }

  @Test
  void negatedGuardEscapesTheUnpreparedQuery() {
    var source = """
      Function Run(Flag)
          Q = New Query("SELECT &P");
          If Flag Then
              Q.SetParameter("P", 1);
          EndIf;
          If Not (Flag) Then
              Return Q;
          EndIf;
          Return Q.Execute();
      EndFunction
      """;
    assertThat(getDiagnostics(TestUtils.getDocumentContext(source, context))).isEmpty();
  }

  @Test
  void helperCanCreateAQueryInAnOutputParameter() {
    var source = """
      Procedure Create(Q)
          Q = New Query("SELECT &P");
      EndProcedure
      Procedure Run()
          Create(Q);
          Q.Execute();
      EndProcedure
      """;
    var diagnostics = getDiagnostics(TestUtils.getDocumentContext(source, context));
    assertThat(diagnostics, true).hasRange(1, 26, 28).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(diagnostics.getFirst().getRelatedInformation())
      .extracting(info -> coordinates(info.getLocation().getRange())).containsExactly("5/6/13");
  }

  private static String coordinates(org.eclipse.lsp4j.Range range) {
    return range.getStart().getLine() + "/" + range.getStart().getCharacter() + "/" + range.getEnd().getCharacter();
  }
}
