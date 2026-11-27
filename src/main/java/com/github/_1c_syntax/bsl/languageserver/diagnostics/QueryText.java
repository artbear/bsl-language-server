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

import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.parser.SDBLParser;
import com.github._1c_syntax.bsl.parser.SDBLParserBaseVisitor;
import com.github._1c_syntax.bsl.parser.SDBLTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Вычисленная строка и непрерывные фрагменты исходника, из которых она собрана. */
@Slf4j
record QueryText(String value, List<Span> spans, Range origin) implements QueryParameterState.Value {
  private static final int MAX_LENGTH = 262144;

  record Span(int offset, int length, int line, int column) {
    Range range(int start, int end) {
      return new Range(new Position(line, column + start - offset), new Position(line, column + end - offset));
    }
  }

  record Parameter(String name, int start, int end) { }
  record Source(Range range, List<Range> fragments) { }

  static Position position(Token token, String[] lines) {
    int line = token.getLine() - 1;
    return new Position(line, lines[line].offsetByCodePoints(0, token.getCharPositionInLine()));
  }

  static Range range(ParserRuleContext node, String[] lines) {
    var stop = node.getStop();
    var end = position(stop, lines);
    end.setCharacter(end.getCharacter() + stop.getText().length());
    return new Range(position(node.getStart(), lines), end);
  }

  static QueryText literal(BSLParser.StringContext literal, String[] lines) {
    var text = new StringBuilder();
    var spans = new ArrayList<Span>();
    int previousLine = -1;
    for (var token : literal.getTokens()) {
      int type = token.getType();
      if (type != BSLLexer.STRING && type != BSLLexer.STRINGSTART && type != BSLLexer.STRINGPART
        && type != BSLLexer.STRINGTAIL && type != BSLLexer.BAR) {
        return null;
      }
      var pos = position(token, lines);
      var raw = token.getText();
      if (previousLine >= 0 && previousLine != pos.getLine()) {
        text.append("\n".repeat(pos.getLine() - previousLine));
      }
      previousLine = pos.getLine();
      int end = raw.length() - (type == BSLLexer.STRING || type == BSLLexer.STRINGTAIL ? 1 : 0);
      int start = 1; // открывающая кавычка или вертикальная черта
      for (int i = start; i < end; i++) {
        if (raw.charAt(i) == '"' && i + 1 < end && raw.charAt(i + 1) == '"') {
          appendRun(text, spans, raw, start, i + 1, pos);
          start = ++i + 1;
        }
      }
      appendRun(text, spans, raw, start, end, pos);
    }
    return new QueryText(text.toString(), List.copyOf(spans), range(literal, lines));
  }

  private static void appendRun(StringBuilder text, List<Span> spans, String raw, int from, int to, Position pos) {
    if (from < to) {
      spans.add(new Span(text.length(), to - from, pos.getLine(), pos.getCharacter() + from));
      text.append(raw, from, to);
    }
  }

  QueryText plus(QueryText other, Range expression) {
    if (value.length() + other.value.length() > MAX_LENGTH) {
      return null;
    }
    var combined = new ArrayList<>(spans);
    other.copySpans(combined, 0, other.value.length(), value.length());
    return new QueryText(value + other.value, List.copyOf(combined), expression);
  }

  QueryText replace(QueryText needle, QueryText replacement, Range expression) {
    if (needle.value.isEmpty() || !value.contains(needle.value)) {
      return this;
    }
    var result = new StringBuilder();
    var sources = new ArrayList<Span>();
    int from = 0;
    for (int at = value.indexOf(needle.value); at >= 0; at = value.indexOf(needle.value, from)) {
      if ((long) result.length() + at - from + replacement.value.length() > MAX_LENGTH) {
        return null;
      }
      copySpans(sources, from, at, result.length());
      result.append(value, from, at);
      replacement.copySpans(sources, 0, replacement.value.length(), result.length());
      result.append(replacement.value);
      from = at + needle.value.length();
    }
    copySpans(sources, from, value.length(), result.length());
    result.append(value, from, value.length());
    return new QueryText(result.toString(), List.copyOf(sources), expression);
  }

  private void copySpans(List<Span> target, int from, int to, int offset) {
    for (var span : spans) {
      int start = Math.max(span.offset, from);
      int end = Math.min(span.offset + span.length, to);
      if (start < end) {
        target.add(new Span(offset + start - from, end - start, span.line, span.column + start - span.offset));
      }
    }
  }

  Source source(Parameter parameter) {
    var fragments = new ArrayList<Range>();
    for (var span : spans) {
      int from = Math.max(parameter.start, span.offset);
      int to = Math.min(parameter.end, span.offset + span.length);
      if (from < to) {
        var part = span.range(from, to);
        if (from == parameter.start && to == parameter.end) {
          return new Source(part, List.of());
        }
        fragments.add(part);
      }
    }
    return new Source(origin, List.copyOf(fragments));
  }

  /** Один прямой разбор на различный вычисленный текст в пределах анализа документа. */
  static List<Parameter> parse(String text) {
    if (text.indexOf('&') < 0) {
      return List.of();
    }
    LOGGER.debug("MissingQueryParameter: direct SDBL parse");
    var query = new ParsedQuery(text);
    var tree = query.getAst();
    if (!query.valid()) {
      return List.of();
    }
    var names = new LinkedHashMap<String, Parameter>();
    class ParametersVisitor extends SDBLParserBaseVisitor<Void> {
      boolean complete = true;
      @Override
      public Void visitParameter(SDBLParser.ParameterContext ctx) {
        if (ctx.AMPERSAND() == null || ctx.name == null
          || ctx.AMPERSAND().getSymbol().getStopIndex() + 1 != ctx.name.getStartIndex()) {
          // При наборе «& КАК Поле» лексер допускает КАК в роли имени параметра.
          complete = false;
        } else {
          String name = ctx.name.getText();
          int start = text.offsetByCodePoints(0, ctx.AMPERSAND().getSymbol().getStartIndex());
          int end = text.offsetByCodePoints(start, ctx.name.getStopIndex() + 1 - ctx.getStart().getStartIndex());
          names.putIfAbsent(name.toLowerCase(Locale.ROOT), new Parameter(name, start, end));
        }
        return null;
      }
    }
    var visitor = new ParametersVisitor();
    visitor.visit(tree);
    return visitor.complete ? List.copyOf(names.values()) : List.of();
  }

  private static final class ParsedQuery extends SDBLTokenizer {
    private ParsedQuery(String text) {
      super(text);
    }

    private boolean valid() {
      return parser.getNumberOfSyntaxErrors() == 0;
    }
  }
}
