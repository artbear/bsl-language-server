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
package com.github._1c_syntax.bsl.languageserver.types;

import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.infrastructure.WorkspaceScope;
import com.github._1c_syntax.bsl.languageserver.references.ReferenceResolver;
import com.github._1c_syntax.bsl.languageserver.types.compilation.CompilationContextResolver;
import com.github._1c_syntax.bsl.languageserver.types.index.SymbolTypeIndex;
import com.github._1c_syntax.bsl.languageserver.types.inferencer.ExpressionTypeInferencer;
import com.github._1c_syntax.bsl.languageserver.types.model.MemberKind;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeKind;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeRef;
import com.github._1c_syntax.bsl.languageserver.types.model.TypeSet;
import com.github._1c_syntax.bsl.languageserver.types.registry.GlobalScopeProvider;
import com.github._1c_syntax.bsl.languageserver.types.registry.TypeRegistry;
import com.github._1c_syntax.bsl.parser.BSLParser;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/** Сопоставляет аргументы вызова с полным набором применимых сигнатур. */
@Component
@WorkspaceScope
@RequiredArgsConstructor
public class MethodArgumentTypes {

  private final ExpressionTypeInferencer inferencer;
  private final DereferenceMemberMatcher dereferenceMatcher;
  private final TypeService typeService;
  private final ReferenceResolver references;
  private final CompilationContextResolver contexts;
  private final GlobalScopeProvider globalScope;
  private final SymbolTypeIndex symbols;
  private final TypeRegistry types;

  public Session in(DocumentContext document) {
    return new Session(document);
  }

  public record Mismatch(BSLParser.ExpressionContext expression, TypeSet actual, TypeSet expected, boolean overloadConflict) {
  }

  public final class Session {
    private final DocumentContext document;
    private final ExpressionTypeInferencer.ArgumentTypes inference;
    private final GlobalMemberTargets globals;

    private Session(DocumentContext document) {
      this.document = document;
      inference = inferencer.argumentTypes(document);
      globals = new GlobalMemberTargets(document, contexts, globalScope);
    }

    public List<Mismatch> mismatches(TerminalNode name, BSLParser.DoCallContext call, boolean qualified) {
      var params = call.callParamList();
      if (params == null) {
        return List.of();
      }
      var arguments = params.callParam();
      int arity = arguments.size();
      while (arity > 0 && arguments.get(arity - 1).expression() == null) {
        arity--;
      }
      if (arity == 0) {
        return List.of();
      }
      var passed = arguments.subList(0, arity);
      var applicable = (qualified ? qualifiedSignatures(name) : bareSignatures(name)).stream()
        .filter(signature -> signature.acceptsShape(passed))
        .toList();
      if (applicable.isEmpty()) {
        return List.of();
      }

      var actualTypes = passed.stream().map(argument ->
        argument.expression() == null ? TypeSet.EMPTY : inference.of(argument.expression())).toList();
      List<BitSet> conflicts = new ArrayList<>();
      var anyConflict = new BitSet();
      var commonConflicts = new BitSet();
      commonConflicts.set(0, arity);
      for (var signature : applicable) {
        var conflicting = new BitSet();
        for (int position = 0; position < arity; position++) {
          if (incompatible(actualTypes.get(position), signature.parameterAt(position).types())) {
            conflicting.set(position);
          }
        }
        if (conflicting.isEmpty()) {
          return List.of();
        }
        commonConflicts.and(conflicting);
        anyConflict.or(conflicting);
        conflicts.add(conflicting);
      }

      // Если общей позиции нет, ошибка относится к сочетанию аргументов.
      boolean overloadConflict = commonConflicts.isEmpty();
      var positions = overloadConflict ? anyConflict : commonConflicts;
      List<Mismatch> result = new ArrayList<>();
      for (int position = positions.nextSetBit(0); position >= 0; position = positions.nextSetBit(position + 1)) {
        var expected = TypeSet.EMPTY;
        for (int variant = 0; variant < applicable.size(); variant++) {
          if (conflicts.get(variant).get(position)) {
            expected = expected.union(applicable.get(variant).parameterAt(position).types());
          }
        }
        result.add(new Mismatch(passed.get(position).expression(), actualTypes.get(position), expected, overloadConflict));
      }
      return result;
    }

    private List<Signature> bareSignatures(TerminalNode name) {
      var declarations = contexts.declarations(document).getOrDefault(name.getText(), List.of());
      if (!declarations.isEmpty()) {
        return document.getSymbolTree().getMethodSymbol(name.getText()).stream()
          .flatMap(method -> sourceSignatures(method).stream()).toList();
      }
      if (!globals.isPlatformFunction(name.getText())) {
        var elsewhere = globals.sourceTargets(name.getText());
        if (!elsewhere.isEmpty()) {
          return elsewhere.size() == 1
            ? elsewhere.getFirst().owner().getSymbolTree().getMethodSymbol(name.getText()).stream()
              .flatMap(method -> sourceSignatures(method).stream()).toList()
            : List.of();
        }
        var source = references.findTargetSymbol(document.getUri(), name).orElse(null);
        if (source instanceof MethodSymbol method) {
          return sourceSignatures(method);
        }
      }
      return memberSignatures(typeService.accessTargetsAt(document, name, null));
    }

    private List<Signature> qualifiedSignatures(TerminalNode name) {
      var dereference = DereferenceLocator.locate(name);
      if (dereference == null) {
        return List.of();
      }
      var receiver = inference.of(dereference.getLeft());
      if (uncertain(receiver)) {
        return List.of();
      }
      var members = dereferenceMatcher.matchReceiverAt(name, document, receiver).members();
      if (receiver.refs().stream().anyMatch(ref -> members.stream().noneMatch(member -> ref.equals(member.owner())))) {
        return List.of();
      }
      return memberSignatures(members);
    }

    private List<Signature> sourceSignatures(MethodSymbol method) {
      var declarations = contexts.declarations(method.getOwner()).getOrDefault(method.getName(), List.of());
      if (declarations.size() > 1) {
        return List.of();
      }
      var parameters = method.getParameters().stream()
        .map(p -> new Parameter(symbols.getCompleteParameterTypes(p, method.getOwner()), p.isOptional(), false))
        .toList();
      return List.of(new Signature(parameters));
    }

    private List<Signature> memberSignatures(Collection<TypeService.TypedMember> members) {
      List<Signature> signatures = new ArrayList<>();
      for (var target : members) {
        var member = target.descriptor();
        if (member.kind() != MemberKind.METHOD) {
          return List.of();
        }
        var variants = member.getSourceSymbol().orElse(null) instanceof MethodSymbol method
          ? sourceSignatures(method)
          : member.signatures().stream().map(signature -> new Signature(signature.parameters().stream()
            .map(p -> new Parameter(p.types(), p.optional(), p.variadic())).toList())).toList();
        if (variants.isEmpty()) {
          return List.of();
        }
        signatures.addAll(variants);
      }
      return signatures;
    }
  }

  private boolean incompatible(TypeSet actual, TypeSet expected) {
    return !uncertain(actual) && !uncertainExpected(expected) && actual.refs().stream().noneMatch(from ->
      expected.refs().stream().anyMatch(to -> possiblyCompatible(from, to)));
  }

  private boolean possiblyCompatible(TypeRef from, TypeRef to) {
    return from.equals(to) || types.genericOf(from).filter(to::equals).isPresent()
      || types.genericOf(to).filter(from::equals).isPresent()
      || types.extensionsOf(from).contains(to) || types.extensionsOf(to).contains(from);
  }

  private static boolean uncertain(TypeSet set) {
    return set.isEmpty() || set.refs().stream().anyMatch(ref -> unknown(ref) || !ref.placeholders().isEmpty());
  }

  private boolean uncertainExpected(TypeSet set) {
    return set.isEmpty() || set.refs().stream().anyMatch(ref -> unknown(ref)
      || (!ref.placeholders().isEmpty() && types.getTypeParameters(ref).isEmpty()));
  }

  private static boolean unknown(TypeRef ref) {
    return ref.kind() == TypeKind.UNKNOWN || ref.kind() == TypeKind.ANY || ref.kind() == TypeKind.USER;
  }

  private record Parameter(TypeSet types, boolean optional, boolean variadic) {
  }

  private record Signature(List<Parameter> parameters) {
    private boolean acceptsShape(List<? extends BSLParser.CallParamContext> arguments) {
      boolean variadic = !parameters.isEmpty() && parameters.getLast().variadic();
      if (arguments.size() > parameters.size() && !variadic) {
        return false;
      }
      for (int index = 0; index < Math.max(parameters.size(), arguments.size()); index++) {
        var parameter = parameterAt(index);
        if (!parameter.optional() && (index >= arguments.size() || arguments.get(index).expression() == null)) {
          return false;
        }
      }
      return true;
    }

    private Parameter parameterAt(int index) {
      return parameters.get(Math.min(index, parameters.size() - 1));
    }
  }
}
