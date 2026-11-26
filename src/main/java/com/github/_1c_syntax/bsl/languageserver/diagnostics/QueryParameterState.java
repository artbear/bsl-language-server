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

import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.EqualsAndHashCode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Состояние одной альтернативы CFG. Значения и состояния экземпляров неизменяемы. */
@EqualsAndHashCode
final class QueryParameterState {
  interface Value { }
  enum Unknown implements Value { VALUE }
  enum Truth implements Value { TRUE, FALSE }
  record Ref(BSLParser.NewExpressionContext allocation, ParserRuleContext caller) implements Value { }
  record Parameters(Ref owner) implements Value { }
  record Query(ParserRuleContext version, QueryText text, Set<String> bound, Set<String> uncertain,
               boolean unknownName, boolean executed, boolean escaped) {
    static Query fresh(ParserRuleContext site, QueryText text) {
      return new Query(site, text, Set.of(), Set.of(), false, false, false);
    }

    Query setText(ParserRuleContext site, QueryText value) {
      return new Query(site, value, bound, uncertain, unknownName, false, escaped);
    }

    Query setParameter(String key) {
      if (key == null) {
        return new Query(version, text, bound, uncertain, true, executed, escaped);
      }
      var names = new HashSet<>(bound);
      names.add(key);
      return new Query(version, text, Set.copyOf(names), uncertain, unknownName, executed, escaped);
    }

    Query execute() {
      return new Query(version, text, bound, uncertain, unknownName, true, escaped);
    }

    Query escape() {
      return new Query(version, null, Set.of(), Set.of(), true, executed, true);
    }

    Query transfer() {
      return new Query(version, text, bound, uncertain, unknownName, executed, true);
    }

    Query unknownParameters() {
      return new Query(version, text, Set.of(), Set.of(), true, executed, escaped);
    }

    Query merge(Query other) {
      var common = new HashSet<>(bound);
      common.retainAll(other.bound);
      var maybe = new HashSet<>(uncertain);
      maybe.addAll(other.uncertain);
      maybe.addAll(bound);
      maybe.addAll(other.bound);
      maybe.removeAll(common);
      boolean sameVersion = version == other.version && Objects.equals(text, other.text);
      return new Query(sameVersion ? version : null, sameVersion ? text : null, Set.copyOf(common),
        Set.copyOf(maybe), unknownName || other.unknownName, executed || other.executed, escaped || other.escaped);
    }
  }

  final Map<String, Value> variables = new HashMap<>();
  final Map<Ref, Query> queries = new HashMap<>();
  final Map<String, Boolean> guards = new HashMap<>();
  Value returned = Unknown.VALUE;
  boolean normal = true;

  QueryParameterState copy() {
    var copy = new QueryParameterState();
    copy.variables.putAll(variables);
    copy.queries.putAll(queries);
    copy.guards.putAll(guards);
    copy.returned = returned;
    copy.normal = normal;
    return copy;
  }

  /** На границе лимита сохраняются только совместимые значения и гарантированные установки. */
  QueryParameterState merge(QueryParameterState other) {
    var merged = copy();
    merged.variables.entrySet().removeIf(e -> !e.getValue().equals(other.variables.get(e.getKey())));
    merged.guards.entrySet().removeIf(e -> !e.getValue().equals(other.guards.get(e.getKey())));
    merged.queries.keySet().retainAll(other.queries.keySet());
    merged.queries.replaceAll((ref, query) -> query.merge(other.queries.get(ref)));
    merged.returned = returned.equals(other.returned) ? returned : Unknown.VALUE;
    return merged;
  }

  void bind(String name, Value value) {
    guards.remove(name);
    if (value == Unknown.VALUE) {
      variables.remove(name);
    } else {
      variables.put(name, value);
    }
  }

  void adopt(QueryParameterState other) {
    variables.clear();
    variables.putAll(other.variables);
    queries.clear();
    queries.putAll(other.queries);
    guards.clear();
    guards.putAll(other.guards);
    returned = other.returned;
    normal = other.normal;
  }

  void escape(Value value) {
    if (value instanceof Ref ref) {
      queries.computeIfPresent(ref, (key, query) -> query.escape());
    } else if (value instanceof Parameters parameters) {
      queries.computeIfPresent(parameters.owner, (key, query) -> query.unknownParameters());
    }
  }

  static Value known(Value value) {
    return value == null ? Unknown.VALUE : value;
  }
}
