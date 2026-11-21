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
import com.github._1c_syntax.bsl.languageserver.cfg.CfgVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.ConditionalVertex;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraph;
import com.github._1c_syntax.bsl.languageserver.cfg.ControlFlowGraphIndex;
import com.github._1c_syntax.bsl.languageserver.context.symbol.VariableSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.variable.VariableKind;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticScope;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.references.ReferenceIndex;
import com.github._1c_syntax.bsl.languageserver.references.model.OccurrenceType;
import com.github._1c_syntax.bsl.languageserver.references.model.Reference;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Range;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MAJOR,
  minutesToFix = 5,
  scope = DiagnosticScope.BSL,
  activatedByDefault = false,
  tags = {DiagnosticTag.STANDARD}
)
@RequiredArgsConstructor
public class VariableInitializationInLoopDiagnostic extends AbstractVisitorDiagnostic {

  private final ControlFlowGraphIndex graphs;
  private final ReferenceIndex references;

  @Override
  public ParseTree visitSubCodeBlock(BSLParser.SubCodeBlockContext ctx) {
    var graph = graphs.graphOf(documentContext, ctx.codeBlock(), CfgBuildOptions.defaults());
    var assignments = new HashMap<VariableSymbol, Map<ParserRuleContext, List<Range>>>();
    for (var vertex : graph.vertexSet()) {
      if (vertex instanceof BasicBlockVertex block) {
        for (var statement : block.statements()) {
          if (statement instanceof BSLParser.AssignmentContext assignment) {
            recordAssignment(assignment, assignments);
          }
        }
      }
    }
    assignments.forEach((variable, writes) -> checkVariable(graph, variable, writes));
    return ctx;
  }

  private void recordAssignment(BSLParser.AssignmentContext assignment,
                                Map<VariableSymbol, Map<ParserRuleContext, List<Range>>> assignments) {
    var target = assignment.lValue();
    if (target == null || target.acceptor() != null || target.IDENTIFIER() == null) {
      return;
    }
    documentContext.getSymbolTree().getVariableSymbolInScope(target, target.IDENTIFIER().getText())
      .filter(variable -> variable.getKind() == VariableKind.DYNAMIC || variable.getKind() == VariableKind.LOCAL)
      .ifPresent(variable -> assignments.computeIfAbsent(variable, ignored -> new HashMap<>())
        .put(assignment, conditionalRegions(assignment)));
  }

  private static List<Range> conditionalRegions(ParserRuleContext statement) {
    var regions = new ArrayList<Range>();
    for (var parent = statement.getParent(); parent != null && !(parent instanceof BSLParser.SubCodeBlockContext);
         parent = parent.getParent()) {
      if (parent instanceof BSLParser.IfStatementContext || parent instanceof BSLParser.ForStatementContext
        || parent instanceof BSLParser.ForEachStatementContext || parent instanceof BSLParser.WhileStatementContext) {
        regions.add(Ranges.create(parent));
      }
    }
    return regions;
  }

  private void checkVariable(ControlFlowGraph graph, VariableSymbol variable,
                             Map<ParserRuleContext, List<Range>> writes) {
    var regions = writes.values().stream().flatMap(List::stream).distinct().toList();
    if (regions.isEmpty()) {
      return;
    }
    var reads = references.getReferencesTo(variable).stream()
      .filter(reference -> reference.occurrenceType() == OccurrenceType.REFERENCE)
      .map(Reference::selectionRange)
      .filter(read -> regions.stream().anyMatch(region -> Ranges.compare(region.getEnd(), read.getStart()) <= 0))
      .toList();
    if (reads.isEmpty()) {
      return;
    }

    var uninitializedReads = new HashSet<Range>();
    var visited = new HashSet<CfgVertex>();
    var pending = new ArrayDeque<CfgVertex>();
    pending.add(graph.getEntryPoint());
    while (!pending.isEmpty()) {
      var vertex = pending.removeFirst();
      if (visited.add(vertex) && canLeaveUninitialized(vertex, writes.keySet(), reads, uninitializedReads)) {
        graph.outgoingEdgesOf(vertex).stream().map(graph::getEdgeTarget).forEach(pending::add);
      }
    }
    uninitializedReads.stream().min(Ranges::compare)
      .ifPresent(read -> diagnosticStorage.addDiagnostic(read, info.getMessage(variable.getName())));
  }

  private static boolean canLeaveUninitialized(CfgVertex vertex, Set<ParserRuleContext> writes, List<Range> reads,
                                                Set<Range> uninitializedReads) {
    if (vertex instanceof BasicBlockVertex block) {
      for (var statement : block.statements()) {
        collectReads(statement, reads, uninitializedReads);
        if (writes.contains(statement)) {
          return false;
        }
      }
    } else if (vertex instanceof ConditionalVertex condition) {
      collectReads(condition.getExpression(), reads, uninitializedReads);
    }
    return true;
  }

  private static void collectReads(ParserRuleContext expression, List<Range> reads, Set<Range> uninitializedReads) {
    var range = Ranges.create(expression);
    reads.stream().filter(read -> Ranges.containsRange(range, read)).forEach(uninitializedReads::add);
  }
}
