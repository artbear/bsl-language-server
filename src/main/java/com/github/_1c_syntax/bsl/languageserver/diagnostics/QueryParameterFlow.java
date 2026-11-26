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

import com.github._1c_syntax.bsl.languageserver.cfg.BasicBlockVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.CfgBuildOptions;
import com.github._1c_syntax.bsl.languageserver.cfg.CfgEdge;
import com.github._1c_syntax.bsl.languageserver.cfg.CfgEdgeType;
import com.github._1c_syntax.bsl.languageserver.cfg.CfgVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.ConditionalVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraph;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraphIndex;
import com.github._1c_syntax.bsl.languageserver.cfg.PreprocessorConditionVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.PreprocessorFlow;
import com.github._1c_syntax.bsl.languageserver.cfg.TryExceptVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.WhileLoopVertex;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.compilation.ExecutionContext;
import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.variable.VariableKind;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Query;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Ref;
import com.github._1c_syntax.bsl.languageserver.diagnostics.QueryParameterState.Value;
import com.github._1c_syntax.bsl.languageserver.types.TypeService;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeKind;
import com.github._1c_syntax.bsl.languageserver.types.compilation.CompilationContextResolver;
import com.github._1c_syntax.bsl.languageserver.utils.Trees;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.types.ModuleType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.lsp4j.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Анализ достижимых альтернатив одного документа, включая однозначные вызовы в том же модуле. */
final class QueryParameterFlow {
  private static final int ALTERNATIVES = 16;
  private static final int CALL_DEPTH = 5;

  record Body(BSLParser.CodeBlockContext block, List<? extends BSLParser.ParamContext> params, boolean method) { }
  record Cause(BSLParser.NewExpressionContext allocation, ParserRuleContext version, String name, QueryText.Source source) { }
  record Site(Range range, boolean exit) { }
  record Finding(Cause cause, String messageKey, List<Range> executions) { }
  private record Step(CfgVertex vertex, QueryParameterState state) { }
  private record Invocation(Body body, ParserRuleContext caller, Set<ExecutionContext> contexts,
                            QueryParameterState arguments) { }

  private final DocumentContext document;
  private final ControlFlowGraphIndex graphs;
  private final CompilationContextResolver contexts;
  private final Predicate<String> selfProperty;
  private final TypeService types;
  private final Map<String, Boolean> queryTypes = new HashMap<>();
  final String[] lines;
  private final Map<String, List<Body>> methods = new HashMap<>();
  private final Map<String, List<QueryText.Parameter>> parameters = new HashMap<>();
  final Map<BSLParser.StringContext, QueryText> literals = new HashMap<>();
  private final Map<Cause, Map<Site, Evidence>> observations = new LinkedHashMap<>();
  private final Set<Site> unknownReceiver = new HashSet<>();
  private final Set<BSLParser.CodeBlockContext> active = new HashSet<>();
  private final Set<Body> candidates = new LinkedHashSet<>();
  private final Map<Body, Set<String>> repeatedGuards = new HashMap<>();
  private final Map<Body, Set<ExecutionContext>> bodyContexts = new HashMap<>();
  private final Set<Body> leafMethods = new HashSet<>();
  private final Map<Invocation, QueryParameterState> summaries = new HashMap<>();
  private final Map<BSLParser.LValueContext, Boolean> persistent = new HashMap<>();

  private static final class Evidence {
    boolean missing;
    boolean other;
  }

  QueryParameterFlow(DocumentContext document, ControlFlowGraphIndex graphs, CompilationContextResolver contexts,
                     TypeService types) {
    this.document = document;
    this.graphs = graphs;
    this.contexts = contexts;
    this.types = types;
    selfProperty = types.bareSelfPropertiesOf(document);
    lines = document.getContentList();
  }

  List<Finding> analyze() {
    var ast = document.getAst();
    var bodies = new ArrayList<Body>();
    if (ast.subs() != null) {
      for (var sub : ast.subs().sub()) {
        var proc = sub.procedure();
        var func = sub.function();
        if (proc == null && func == null) {
          continue;
        }
        var subBlock = proc == null ? func.subCodeBlock() : proc.subCodeBlock();
        if (subBlock == null || subBlock.codeBlock() == null) {
          continue;
        }
        var name = proc == null ? func.funcDeclaration().subName() : proc.procDeclaration().subName();
        if (name == null || name.IDENTIFIER() == null) {
          continue;
        }
        var params = proc == null ? func.funcDeclaration().paramList() : proc.procDeclaration().paramList();
        var body = new Body(subBlock.codeBlock(), params == null ? List.of() : params.param(), true);
        methods.computeIfAbsent(key(name.getText()), ignored -> new ArrayList<>()).add(body);
        bodies.add(body);
      }
    }
    if (ast.fileCodeBlock() != null && ast.fileCodeBlock().codeBlock() != null) {
      bodies.add(new Body(ast.fileCodeBlock().codeBlock(), List.of(), false));
    }
    if (ast.fileCodeBlockBeforeSub() != null && ast.fileCodeBlockBeforeSub().codeBlock() != null) {
      bodies.add(new Body(ast.fileCodeBlockBeforeSub().codeBlock(), List.of(), false));
    }

    var callers = new HashMap<Body, Set<Body>>();
    for (var body : bodies) {
      if (Trees.<BSLParser.NewExpressionContext>findAllRuleNodes(body.block, BSLParser.RULE_newExpression)
        .stream().anyMatch(this::queryConstructor)) {
        candidates.add(body);
      }
      boolean leaf = true;
      for (var call : Trees.<BSLParser.GlobalMethodCallContext>findAllRuleNodes(body.block, BSLParser.RULE_globalMethodCall)) {
        var target = method(call.methodName().getText());
        if (target != null) {
          leaf = false;
          if (target.block.getParent().getParent() instanceof BSLParser.FunctionContext
            || target.params.stream().anyMatch(param -> param.VAL_KEYWORD() == null)) {
            callers.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(body);
          }
        }
      }
      if (leaf) {
        leafMethods.add(body);
      }
    }
    var queue = new ArrayDeque<>(candidates);
    while (!queue.isEmpty()) {
      for (var caller : callers.getOrDefault(queue.removeFirst(), Set.of())) {
        if (candidates.add(caller)) {
          queue.add(caller);
        }
      }
    }
    if (!candidates.isEmpty() && PreprocessorDeadCode.collect(document, contexts, ast).wellFormed()) {
      for (var body : candidates) {
        run(body, new QueryParameterState(), null, null);
      }
    }
    return findings();
  }

  static String key(String name) {
    return name.toLowerCase(Locale.ROOT);
  }

  boolean queryConstructor(BSLParser.NewExpressionContext creation) {
    if (creation.typeName() == null) {
      return false;
    }
    var name = key(creation.typeName().getText());
    return (name.equals("query") || name.equals("запрос")) && queryTypes.computeIfAbsent(name,
      spelling -> types.resolve(spelling, document.getFileType()).map(type -> type.kind() == TypeKind.PLATFORM)
        .orElse(true));
  }

  Body method(String name) {
    var targets = methods.get(key(name));
    return targets != null && targets.size() == 1 ? targets.getFirst() : null;
  }

  boolean canEnter(Body body) {
    return !active.contains(body.block) && active.size() < CALL_DEPTH;
  }

  QueryParameterState call(Body body, QueryParameterState arguments, ParserRuleContext caller,
                           Set<ExecutionContext> callerContexts) {
    if (!leafMethods.contains(body)) {
      return run(body, arguments, caller, callerContexts);
    }
    var invocation = new Invocation(body, caller, callerContexts, arguments);
    if (summaries.containsKey(invocation)) {
      return summaries.get(invocation);
    }
    var result = run(body, arguments, caller, callerContexts);
    if (summaries.size() < 128) {
      summaries.put(invocation, result);
    }
    return result;
  }

  boolean persistent(BSLParser.LValueContext target) {
    return persistent.computeIfAbsent(target, node -> {
      var name = node.IDENTIFIER().getText();
      return document.getSymbolTree().getVariableSymbolInScope(node, name)
        .map(variable -> variable.getKind() == VariableKind.MODULE).orElseGet(() -> selfProperty.test(name));
    });
  }

  private Set<ExecutionContext> allowedContexts(Body body) {
    var allowed = ExecutionContext.copyOf(contexts.possibleContexts(document, body.block)
      .orElseGet(() -> contexts.supportedContexts(document)));
    if (body.method && document.getModuleType() == ModuleType.UNKNOWN) {
      // У одиночного файла нет метаданных модуля, но явная директива у метода сохраняется.
      BSLParser.SubContext sub = Trees.getAncestorByRuleIndex(body.block, BSLParser.RULE_sub);
      document.getSymbolTree().getMethodSymbol(sub).flatMap(MethodSymbol::getCompilerDirectiveKind)
        .ifPresent(directive -> {
          switch (directive) {
            case AT_CLIENT -> allowed.retainAll(ExecutionContext.CLIENTS);
            case AT_SERVER, AT_SERVER_NO_CONTEXT -> allowed.retainAll(ExecutionContext.SERVER_CONTEXTS);
            default -> { /* метод доступен с обеих сторон */ }
          }
        });
    }
    return allowed;
  }

  QueryParameterState run(Body body, QueryParameterState initial, ParserRuleContext caller,
                          Set<ExecutionContext> callerContexts) {
    active.add(body.block);
    try {
      var graph = graphs.graphOf(document, body.block, CfgBuildOptions.defaults());
      var allowed = ExecutionContext.copyOf(bodyContexts.computeIfAbsent(body, this::allowedContexts));
      if (callerContexts != null) {
        allowed.retainAll(callerContexts);
      }
      if (allowed.isEmpty()) {
        return null;
      }
      var guards = repeatedGuards.computeIfAbsent(body, ignored -> guardNames(graph));
      if (graph.vertexSet().stream().noneMatch(PreprocessorConditionVertex.class::isInstance)) {
        return walk(body, graph, graph.edgeSet(), initial, caller, allowed, guards);
      }
      QueryParameterState result = null;
      for (var variant : allowed) {
        var edges = PreprocessorFlow.of(graph, Set.of(variant), document.getFileType()).edges();
        var exit = walk(body, graph, edges, initial, caller, Set.of(variant), guards);
        if (exit != null) {
          result = result == null ? exit : result.merge(exit);
        }
      }
      return result;
    } finally {
      active.remove(body.block);
    }
  }

  private static Set<String> guardNames(ControlFlowGraph graph) {
    var once = new HashSet<String>();
    var repeated = new HashSet<String>();
    for (var vertex : graph.vertexSet()) {
      var expr = condition(vertex);
      var guard = QueryParameterExpressions.guard(expr);
      if (guard != null && (!once.add(guard.name()) || vertex instanceof WhileLoopVertex)) {
        repeated.add(guard.name());
      }
    }
    return repeated;
  }

  private static BSLParser.ExpressionContext condition(CfgVertex vertex) {
    if (vertex instanceof ConditionalVertex branch) {
      return branch.getExpression();
    }
    return vertex instanceof WhileLoopVertex loop ? loop.getExpression() : null;
  }

  private QueryParameterState walk(Body body, ControlFlowGraph graph, Set<CfgEdge> edges, QueryParameterState initial,
                                   ParserRuleContext caller, Set<ExecutionContext> context, Set<String> guards) {
    var transfer = new QueryParameterExpressions(this, caller, context);
    var pending = new ArrayDeque<Step>();
    var visited = new HashMap<CfgVertex, Set<QueryParameterState>>();
    var overflow = new HashMap<CfgVertex, QueryParameterState>();
    pending.add(new Step(graph.getEntryPoint(), initial));
    QueryParameterState exit = null;
    while (!pending.isEmpty()) {
      var step = pending.removeFirst();
      var vertex = step.vertex;
      var input = step.state;
      var seen = visited.computeIfAbsent(vertex, ignored -> new LinkedHashSet<>());
      if (seen.contains(input)) {
        continue;
      }
      if (seen.size() < ALTERNATIVES) {
        seen.add(input);
      } else {
        var previous = overflow.get(vertex);
        input = (previous == null ? seen.iterator().next() : previous).merge(input);
        if (input.equals(previous)) {
          continue;
        }
        overflow.put(vertex, input);
      }
      var state = input.copy();
      if (vertex == graph.getExitPoint()) {
        if (caller == null && body.method) {
          localExit(body, state);
        }
        exit = exit == null ? state : exit.merge(state);
        continue;
      }
      var expr = condition(vertex);
      if (vertex instanceof BasicBlockVertex block) {
        for (var statement : block.statements()) {
          transfer.statement(statement, state);
        }
      } else if (expr != null) {
        transfer.expression(expr, state);
      } else {
        vertex.getAst().ifPresent(node -> transfer.loopHeader(node, state));
      }
      if (!state.normal) {
        continue;
      }
      for (var edge : graph.outgoingEdgesOf(vertex)) {
        var target = graph.getEdgeTarget(edge);
        if (!edges.contains(edge) || vertex instanceof TryExceptVertex && edge.getType() == CfgEdgeType.FALSE_BRANCH
          || target == graph.getExitPoint() && raises(vertex)) {
          continue;
        }
        var next = branch(expr, edge, state, guards);
        if (next != null) {
          pending.addLast(new Step(target, next));
        }
      }
    }
    return exit;
  }

  private static boolean raises(CfgVertex vertex) {
    return vertex instanceof BasicBlockVertex block && !block.statements().isEmpty()
      && block.statements().getLast() instanceof BSLParser.RaiseStatementContext;
  }

  private static QueryParameterState branch(BSLParser.ExpressionContext expr, CfgEdge edge, QueryParameterState state,
                                            Set<String> guards) {
    if (expr == null || edge.getType() != CfgEdgeType.TRUE_BRANCH && edge.getType() != CfgEdgeType.FALSE_BRANCH) {
      return state;
    }
    boolean truth = edge.getType() == CfgEdgeType.TRUE_BRANCH;
    var guard = QueryParameterExpressions.guard(expr);
    var known = QueryParameterExpressions.truth(expr, state);
    if (known != null && known != truth) {
      return null;
    }
    if (guard != null && guards.contains(guard.name())) {
      var next = state.copy();
      next.guards.put(guard.name(), truth != guard.negated());
      return next;
    }
    return state;
  }

  private void localExit(Body body, QueryParameterState state) {
    var retained = new HashSet<>(state.variables.values());
    retained.remove(state.returned);
    for (var formal : body.params) {
      if (formal.VAL_KEYWORD() == null) {
        retained.remove(state.variables.get(key(formal.IDENTIFIER().getText())));
      }
    }
    for (var value : retained) {
      if (value instanceof Ref ref) {
        var query = state.queries.get(ref);
        if (query != null && !query.executed() && !query.escaped()) {
          inspect(ref, query, new Site(null, true));
        }
      }
    }
  }

  void execute(Value receiver, QueryParameterState state, BSLParser.MethodNameContext call) {
    var site = new Site(QueryText.range(call, lines), false);
    var query = receiver instanceof Ref ref ? state.queries.get(ref) : null;
    if (receiver instanceof Ref ref && query != null && query.text() != null) {
      inspect(ref, query, site);
      state.queries.put(ref, query.execute());
    } else if (!state.queries.isEmpty()) {
      unknownReceiver.add(site);
    }
  }

  private void inspect(Ref ref, Query query, Site site) {
    if (query.text() == null) {
      return;
    }
    for (var parameter : parameters.computeIfAbsent(query.text().value(), QueryText::parse)) {
      var cause = new Cause(ref.allocation(), query.version(), parameter.name(), query.text().source(parameter));
      var status = observations.computeIfAbsent(cause, ignored -> new LinkedHashMap<>())
        .computeIfAbsent(site, ignored -> new Evidence());
      var name = key(parameter.name());
      if (query.bound().contains(name) || query.unknownName() || query.uncertain().contains(name)) {
        status.other = true;
      } else {
        status.missing = true;
      }
    }
  }

  private List<Finding> findings() {
    var result = new ArrayList<Finding>();
    observations.forEach((cause, sites) -> {
      var executions = new ArrayList<Range>();
      boolean definite = false;
      boolean local = false;
      for (var entry : sites.entrySet()) {
        if (!entry.getValue().missing) {
          continue;
        }
        var site = entry.getKey();
        if (site.exit) {
          local = true;
        } else {
          executions.add(site.range);
          definite |= !entry.getValue().other && !unknownReceiver.contains(site);
        }
      }
      if (local || !executions.isEmpty()) {
        result.add(new Finding(cause, executions.isEmpty() ? "localExit" : definite ? "diagnosticMessage" : "possible",
          executions));
      }
    });
    return result;
  }
}
