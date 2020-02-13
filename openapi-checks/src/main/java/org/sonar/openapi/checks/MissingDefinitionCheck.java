/*
 * SonarQube OpenAPI Plugin
 * Copyright (C) 2018-2019 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.openapi.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Rule(key = MissingDefinitionCheck.CHECK_KEY)
public class MissingDefinitionCheck extends OpenApiCheck {

  private static Map<JsonPointer, Set<JsonNode>> missingPointerNodes = new HashMap<>();

  public static final String CHECK_KEY = "MissingDefinition";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
  }

  @Override
  protected void visitFile(JsonNode root) {
    if (root.getType() == OpenApi2Grammar.ROOT) {
      inspectOpenApi2(root);
    } else {
      inspectOpenApi3(root);
    }
  }

  @Override
  public void visitNode(JsonNode operation) {
  }

  private static Set<JsonPointer> openApi2Dicriminators(JsonNode n) {
    JsonNode d = n.at("/discriminator");
    if (d.isMissing()) {
      return Collections.emptySet();
    }
    JsonNode at = n.at("/properties/" + d.getTokenValue() + "/enum");
    if (at.isArray()) {
      return at.elements().stream()
        .map(JsonNode::getTokenValue)
        .map(Utils::escape)
        .map(p -> JsonPointer.compile("/definitions").append(p))
        .collect(Collectors.toSet());
    } else {
      return Collections.emptySet();
    }
  }

  private static Set<JsonPointer> openApi3Dicriminators(JsonNode n) {
    JsonNode d = n.at("/discriminator");
    if (d.isMissing()) {
      return Collections.emptySet();
    }
    return d.at("/mapping").propertyMap().values().stream()
      .map(JsonNode::getTokenValue)
      .map(s -> s.substring(1))
      .map(JsonPointer::compile)
      .collect(Collectors.toSet());
  }

  private void inspectOpenApi2(JsonNode root) {
    Set<JsonPointer> used = usedReferences(root, MissingDefinitionCheck::openApi2Dicriminators);
    // used:
    // [/responses/Missing, /definitions/Used, /responses/Used, /parameters/Missing, /definitions/Missing, /parameters/Used]
    reportMissing(root, "/definitions", "Missing schema", used);
    reportMissing(root, "/parameters", "Missing parameter", used);
    reportMissing(root, "/responses", "Missing response", used);
  }

  private void inspectOpenApi3(JsonNode root) {
    Set<JsonPointer> used = usedReferences(root, MissingDefinitionCheck::openApi3Dicriminators);
    reportMissing(root, "/components/schemas", "Missing schema", used);
    reportMissing(root, "/components/parameters", "Missing parameter", used);
    reportMissing(root, "/components/responses", "Missing response", used);
    reportMissing(root, "/components/examples", "Missing example", used);
    reportMissing(root, "/components/requestBodies", "Missing request body", used);
    reportMissing(root, "/components/headers", "Missing header", used);
    reportMissing(root, "/components/links", "Missing link", used);
    reportMissing(root, "/components/callbacks", "Missing callback", used);
  }

  private void reportMissing(JsonNode root, String pointer, String message, Set<JsonPointer> used) {
    JsonPointer jsonPointer = JsonPointer.compile(pointer);
    // pointers = existing definitions, parameters or responses
    Set<JsonPointer> pointers = root.at(pointer)
      .propertyNames().stream()
      .map(s -> jsonPointer.append(Utils.escape(s)))
      .collect(Collectors.toSet());
    used.stream().filter(u -> u.toString().startsWith(pointer)).filter(u -> !pointers.contains(u))
      .forEach(u -> {
        if (missingPointerNodes.containsKey(u)) {
          Set<JsonNode> missingNodeReferences = missingPointerNodes.get(u);
          missingNodeReferences.forEach(n -> addIssue(message, n));
          missingPointerNodes.remove(u);
        }
      });
  }

  private static Set<JsonPointer> usedReferences(JsonNode node, Function<JsonNode, Set<JsonPointer>> discriminators) {
    if (node.isArray()) {
      return node.elements().stream().flatMap(n -> usedReferences(n, discriminators).stream()).collect(Collectors.toSet());
    } else if (node.isObject()) {
      Set<JsonPointer> refs = new HashSet<>();
      refs.addAll(getReference(node));
      refs.addAll(discriminators.apply(node));
      refs.addAll(node.propertyMap()
        .values().stream()
        .flatMap(n -> usedReferences(n, discriminators).stream())
        .collect(Collectors.toSet()));
      return refs;
    } else {
      return Collections.emptySet();
    }
  }

  private static List<JsonPointer> getReference(JsonNode node) {
    if (node.isRef()) {
      JsonNode ref = node.at("/$ref");
      JsonPointer jsonPointer = JsonPointer.compile(ref.getTokenValue().substring(1));
      registerMissingPointer(jsonPointer, ref);
      return Collections.singletonList(jsonPointer);
    } else {
      return Collections.emptyList();
    }
  }

  private static void registerMissingPointer(JsonPointer jsonPointer, JsonNode jsonNode) {
    Set<JsonNode> jsonNodes;
    if (missingPointerNodes.containsKey(jsonPointer)) {
      jsonNodes = missingPointerNodes.get(jsonPointer);
    } else {
      jsonNodes = new HashSet<>();
    }
    jsonNodes.add(jsonNode);
    missingPointerNodes.put(jsonPointer, jsonNodes);
  }
}
