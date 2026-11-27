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

import com.github._1c_syntax.bsl.languageserver.context.compilation.ExecutionContext;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Parameters;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Query;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Ref;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Value;
import com.github._1c_syntax.bsl.languageserver.utils.Trees;
import com.github._1c_syntax.bsl.parser.BSLParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Set;

import static com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterFlow.key;
import static com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Unknown.VALUE;
import static com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.known;

/** Перенос фактов в порядке вычисления выражений BSL. Ссылка и результат вызова имеют разные значения. */
final class QueryParameterExpressions {
  record Guard(String name, boolean negated) { }
  private static final Set<String> EXECUTE = Set.of("execute", "executebatch", "executebatchwithintermediatedata",
    "выполнить", "выполнитьпакет", "выполнитьпакетспромежуточнымиданными");
  private final QueryParameterFlow flow;
  private final ParserRuleContext caller;
  private final Set<ExecutionContext> context;

  QueryParameterExpressions(QueryParameterFlow flow, ParserRuleContext caller, Set<ExecutionContext> context) {
    this.flow = flow;
    this.caller = caller;
    this.context = context;
  }

  void statement(ParserRuleContext node, QueryParameterState state) {
    if (!state.normal) {
      return;
    }
    if (node instanceof BSLParser.AssignmentContext assignment) {
      assign(assignment, state);
    } else if (node instanceof BSLParser.CallStatementContext call) {
      if (call.globalMethodCall() != null && call.modifier().isEmpty() && call.accessCall() == null) {
        global(call.globalMethodCall(), state, false);
        return;
      }
      var receiver = call.globalMethodCall() == null ? lookup(call.IDENTIFIER(), state) : global(call.globalMethodCall(), state);
      receiver = modifiers(receiver, call.modifier(), state);
      if (call.accessCall() != null) {
        method(receiver, call.accessCall().methodCall(), state);
      }
    } else if (node instanceof BSLParser.ReturnStatementContext ret) {
      state.returned = expression(ret.expression(), state);
    } else {
      node.getRuleContexts(BSLParser.ExpressionContext.class).forEach(expr -> expression(expr, state));
    }
  }

  void loopHeader(ParserRuleContext node, QueryParameterState state) {
    if (node instanceof BSLParser.ForEachStatementContext loop) {
      expression(loop.expression(), state);
      state.bind(key(loop.IDENTIFIER().getText()), VALUE);
    } else if (node instanceof BSLParser.ForStatementContext loop) {
      loop.expression().forEach(expr -> expression(expr, state));
      state.bind(key(loop.IDENTIFIER().getText()), VALUE);
    }
  }

  private void assign(BSLParser.AssignmentContext assignment, QueryParameterState state) {
    var target = assignment.lValue();
    var value = Trees.nodeContainsErrors(assignment) ? VALUE : expression(assignment.expression(), state);
    if (target == null || target.IDENTIFIER() == null) {
      state.escape(value);
      return;
    }
    var name = key(target.IDENTIFIER().getText());
    var acceptor = target.acceptor();
    if (acceptor == null) {
      if (value instanceof Ref ref && flow.persistent(target)) {
        state.queries.computeIfPresent(ref, (ignored, query) -> query.transfer());
      }
      state.bind(name, value);
      return;
    }
    var receiver = modifiers(lookup(target.IDENTIFIER(), state), acceptor.modifier(), state);
    if (acceptor.accessIndex() != null) {
      expression(acceptor.accessIndex().expression(), state);
    }
    if (receiver instanceof Ref ref && acceptor.accessProperty() != null
      && textProperty(acceptor.accessProperty().IDENTIFIER().getText())) {
      state.queries.computeIfPresent(ref, (ignored, query) ->
        query.setText(assignment, value instanceof QueryText text ? text : null));
    } else {
      if (receiver instanceof Parameters) {
        state.escape(receiver);
      }
      state.escape(value);
    }
  }

  Value expression(BSLParser.ExpressionContext expr, QueryParameterState state) {
    if (expr == null || expr.member().isEmpty() || expr.member().size() != expr.operation().size() + 1) {
      return VALUE;
    }
    Value result = member(expr.member(0), state);
    for (int i = 1; i < expr.member().size(); i++) {
      var op = expr.operation(i - 1);
      // Правый операнд И/ИЛИ может не вычисляться. Без доказательства достижимости
      // не используем его возможные побочные эффекты как обязательные.
      if (op != null && op.boolOperation() != null) {
        var condition = truthMember(expr.member(0), state);
        boolean and = op.getText().equalsIgnoreCase("И") || op.getText().equalsIgnoreCase("And");
        if (i == 1 && condition != null && condition != and) {
          return VALUE;
        }
        if (condition == null || i != 1) {
          var optional = state.copy();
          member(expr.member(i), optional);
          state.adopt(state.merge(optional));
          result = VALUE;
          continue;
        }
      }
      var next = member(expr.member(i), state);
      result = op != null && op.PLUS() != null && result instanceof QueryText left && next instanceof QueryText right
        ? known(left.plus(right, QueryText.range(expr, flow.lines))) : VALUE;
    }
    return result;
  }

  private Value member(BSLParser.MemberContext node, QueryParameterState state) {
    Value value;
    if (node.constValue() != null && node.constValue().string() != null) {
      var string = node.constValue().string();
      value = known(flow.literals.computeIfAbsent(string, literal -> QueryText.literal(literal, flow.lines)));
    } else if (node.constValue() != null && node.constValue().TRUE() != null) {
      value = QueryParameterState.Truth.TRUE;
    } else if (node.constValue() != null && node.constValue().FALSE() != null) {
      value = QueryParameterState.Truth.FALSE;
    } else if (node.complexIdentifier() != null) {
      var id = node.complexIdentifier();
      if (id.newExpression() != null) {
        value = creation(id.newExpression(), state);
      } else if (id.globalMethodCall() != null) {
        value = global(id.globalMethodCall(), state);
      } else if (id.ternaryOperator() != null) {
        value = ternary(id.ternaryOperator(), state);
      } else {
        value = lookup(id.IDENTIFIER(), state);
      }
      value = modifiers(value, id.modifier(), state);
    } else if (node.globalMethodCall() != null) {
      value = global(node.globalMethodCall(), state);
    } else if (node.expression() != null) {
      value = expression(node.expression(), state);
    } else if (node.waitExpression() != null) {
      value = expression(node.waitExpression().expression(), state);
    } else {
      value = lookup(node.IDENTIFIER(), state);
    }
    value = modifiers(value, node.modifier(), state);
    return node.unaryModifier() == null ? value : VALUE;
  }

  private Value ternary(BSLParser.TernaryOperatorContext ternary, QueryParameterState state) {
    if (ternary.expression().size() != 3) {
      return VALUE;
    }
    expression(ternary.expression(0), state);
    var condition = truth(ternary.expression(0), state);
    if (condition != null) {
      return expression(ternary.expression(condition ? 1 : 2), state);
    }
    var other = state.copy();
    var left = expression(ternary.expression(1), state);
    var right = expression(ternary.expression(2), other);
    state.adopt(state.merge(other));
    return left.equals(right) ? left : VALUE;
  }

  private static Value lookup(TerminalNode identifier, QueryParameterState state) {
    if (identifier == null) {
      return VALUE;
    }
    var name = key(identifier.getText());
    var guard = state.guards.get(name);
    if (guard != null) {
      return guard ? QueryParameterState.Truth.TRUE : QueryParameterState.Truth.FALSE;
    }
    return state.variables.getOrDefault(name, VALUE);
  }

  static String variable(BSLParser.ExpressionContext expr) {
    if (expr == null || expr.member().size() != 1) {
      return null;
    }
    var node = expr.member(0);
    if (node.unaryModifier() != null || !node.modifier().isEmpty()) {
      return null;
    }
    if (node.expression() != null) {
      return variable(node.expression());
    }
    var id = node.complexIdentifier();
    var identifier = id == null ? node.IDENTIFIER() : id.modifier().isEmpty() ? id.IDENTIFIER() : null;
    return identifier == null ? null : key(identifier.getText());
  }

  static Boolean truth(BSLParser.ExpressionContext expr, QueryParameterState state) {
    if (expr == null || expr.member().size() != 1) {
      return null;
    }
    return truthMember(expr.member(0), state);
  }

  static Guard guard(BSLParser.ExpressionContext expression) {
    if (expression == null || expression.member().size() != 1) {
      return null;
    }
    return guard(expression.member(0));
  }

  private static Guard guard(BSLParser.MemberContext node) {
    if (!node.modifier().isEmpty()) {
      return null;
    }
    boolean negated = node.unaryModifier() != null;
    if (negated && !Set.of("не", "not").contains(key(node.unaryModifier().getText()))) {
      return null;
    }
    if (node.expression() != null) {
      var inner = guard(node.expression());
      return inner == null ? null : new Guard(inner.name, negated != inner.negated);
    }
    var complex = node.complexIdentifier();
    var id = complex == null ? node.IDENTIFIER() : complex.modifier().isEmpty() ? complex.IDENTIFIER() : null;
    return id == null ? null : new Guard(key(id.getText()), negated);
  }

  private static Boolean truthMember(BSLParser.MemberContext node, QueryParameterState state) {
    if (node.unaryModifier() == null && node.constValue() != null) {
      if (node.constValue().TRUE() != null) {
        return true;
      }
      if (node.constValue().FALSE() != null) {
        return false;
      }
    }
    var guard = guard(node);
    if (guard == null) {
      return null;
    }
    Boolean value = state.guards.get(guard.name);
    if (state.variables.get(guard.name) instanceof QueryParameterState.Truth constant) {
      value = constant == QueryParameterState.Truth.TRUE;
    }
    return value == null ? null : value != guard.negated;
  }

  private Value modifiers(Value receiver, List<? extends BSLParser.ModifierContext> modifiers, QueryParameterState state) {
    for (var modifier : modifiers) {
      if (modifier.accessCall() != null) {
        receiver = method(receiver, modifier.accessCall().methodCall(), state);
      } else if (modifier.accessProperty() != null && receiver instanceof Ref ref) {
        var name = key(modifier.accessProperty().IDENTIFIER().getText());
        var query = state.queries.get(ref);
        if (textProperty(name) && query != null) {
          receiver = known(query.text());
        } else {
          receiver = name.equals("параметры") || name.equals("parameters") ? new Parameters(ref) : VALUE;
        }
      } else {
        if (modifier.accessIndex() != null) {
          expression(modifier.accessIndex().expression(), state);
        }
        receiver = VALUE;
      }
    }
    return receiver;
  }

  private static boolean textProperty(String name) {
    return name.equalsIgnoreCase("текст") || name.equalsIgnoreCase("text");
  }

  private Value creation(BSLParser.NewExpressionContext creation, QueryParameterState state) {
    var args = arguments(creation.doCall(), state);
    if (!flow.queryConstructor(creation) || creation.doCall() != null && !complete(creation.doCall())) {
      args.forEach(state::escape);
      return VALUE;
    }
    var ref = new Ref(creation, caller);
    // При повторной аллокации оставшиеся ссылки на предыдущую итерацию больше не точны.
    state.variables.values().removeIf(ref::equals);
    var text = args.size() == 1 && args.getFirst() instanceof QueryText string ? string : null;
    state.queries.put(ref, Query.fresh(creation, text));
    return ref;
  }

  private List<Value> arguments(BSLParser.DoCallContext call, QueryParameterState state) {
    if (call == null || call.callParamList() == null || call.callParamList().getText().isEmpty()) {
      return List.of();
    }
    return call.callParamList().callParam().stream().map(arg -> expression(arg.expression(), state)).toList();
  }

  private Value method(Value receiver, BSLParser.MethodCallContext method, QueryParameterState state) {
    if (method == null || !complete(method.doCall())) {
      return VALUE;
    }
    var args = arguments(method.doCall(), state);
    var name = key(method.methodName().getText());
    if (EXECUTE.contains(name) && args.isEmpty()) {
      flow.execute(receiver, state, method.methodName());
    } else if (receiver instanceof Ref ref && (name.equals("установитьпараметр") || name.equals("setparameter"))
      && args.size() == 2) {
      var parameter = args.getFirst() instanceof QueryText text ? key(text.value()) : null;
      state.queries.computeIfPresent(ref, (ignored, query) -> query.setParameter(parameter));
      state.escape(args.get(1));
    } else {
      state.escape(receiver);
      opaque(method.doCall(), args, state);
    }
    return VALUE;
  }

  private Value global(BSLParser.GlobalMethodCallContext call, QueryParameterState state) {
    return global(call, state, true);
  }

  private Value global(BSLParser.GlobalMethodCallContext call, QueryParameterState state, boolean resultNeeded) {
    if (!complete(call.doCall())) {
      return VALUE;
    }
    var args = arguments(call.doCall(), state);
    var name = key(call.methodName().getText());
    var body = flow.method(name);
    if (body != null && args.size() <= body.params().size() && flow.canEnter(body)
      && (resultNeeded || !args.isEmpty())) {
      return local(call, args, body, state);
    }
    if (body == null && (name.equals("стрзаменить") || name.equals("strreplace"))) {
      if (args.size() == 3 && args.get(0) instanceof QueryText source && args.get(1) instanceof QueryText needle
        && args.get(2) instanceof QueryText replacement) {
        return known(source.replace(needle, replacement, QueryText.range(call, flow.lines)));
      }
      return VALUE;
    }
    opaque(call.doCall(), args, state);
    return VALUE;
  }

  private static boolean complete(BSLParser.DoCallContext call) {
    return call != null && call.RPAREN() != null && call.RPAREN().getSymbol().getTokenIndex() >= 0;
  }

  private Value local(BSLParser.GlobalMethodCallContext call, List<Value> args, QueryParameterFlow.Body body,
                      QueryParameterState state) {
    var frame = new QueryParameterState();
    for (int i = 0; i < args.size(); i++) {
      Value arg = args.get(i);
      frame.bind(key(body.params().get(i).IDENTIFIER().getText()), arg);
      var owner = arg instanceof Parameters parameters ? parameters.owner() : arg;
      if (owner instanceof Ref ref && state.queries.containsKey(ref)) {
        frame.queries.put(ref, state.queries.get(ref));
      }
    }
    var exit = flow.call(body, frame, call, context);
    if (exit == null) {
      state.normal = false;
      return VALUE;
    }
    state.queries.putAll(exit.queries);
    for (int i = 0; i < args.size(); i++) {
      var param = body.params().get(i);
      if (param.VAL_KEYWORD() == null) {
        var result = exit.variables.getOrDefault(key(param.IDENTIFIER().getText()), VALUE);
        if (!result.equals(args.get(i))) {
          writeArgument(call.doCall().callParamList().callParam(i).expression(), result, state);
        }
      }
    }
    return exit.returned;
  }

  private static void opaque(BSLParser.DoCallContext call, List<Value> args, QueryParameterState state) {
    for (int i = 0; i < args.size(); i++) {
      state.escape(args.get(i));
      writeArgument(call.callParamList().callParam(i).expression(), VALUE, state);
    }
  }

  private static void writeArgument(BSLParser.ExpressionContext argument, Value value, QueryParameterState state) {
    var name = variable(argument);
    if (name != null) {
      state.bind(name, value);
      return;
    }
    if (argument == null || argument.member().size() != 1) {
      return;
    }
    var access = argument.member(0).complexIdentifier();
    if (access == null || access.IDENTIFIER() == null || access.modifier().size() != 1) {
      return;
    }
    var property = access.modifier(0).accessProperty();
    if (property != null && textProperty(property.IDENTIFIER().getText())
      && lookup(access.IDENTIFIER(), state) instanceof Ref ref) {
      state.queries.computeIfPresent(ref, (ignored, query) ->
        query.setText(argument, value instanceof QueryText text ? text : null));
    }
  }
}
