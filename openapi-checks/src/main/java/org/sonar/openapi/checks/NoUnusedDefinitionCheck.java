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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.Utils;
import org.sonar.sslr.yaml.grammar.impl.MissingNode;

@Rule(key = NoUnusedDefinitionCheck.CHECK_KEY)
public class NoUnusedDefinitionCheck extends OpenApiCheck {
  public static final String CHECK_KEY = "NoUnusedDefinition";

  private final Map<String, JsonNode> unusedTags = new HashMap<>();

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION);
  }

  @Override
  protected void visitFile(JsonNode root) {
    findTags(root);
    if (root.getType() == OpenApi2Grammar.ROOT) {
      inspectOpenApi2(root);
    } else {
      inspectOpenApi3(root);
    }
  }

  @Override
  public void visitNode(JsonNode operation) {
    JsonNode tagsArray = operation.at("/tags").value();
    if (tagsArray != MissingNode.MISSING) {
      for (JsonNode element : tagsArray.elements()) {
        unusedTags.remove(element.getTokenValue());
      }
    }
  }

  @Override
  protected void leaveFile(JsonNode node) {
    for (JsonNode tag : unusedTags.values()) {
      addIssue("Unused tag.", tag);
    }
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
    Set<JsonPointer> used = usedReferences(root, NoUnusedDefinitionCheck::openApi2Dicriminators);
    reportUnused(root, "/definitions", "Unused schema", used);
    reportUnused(root, "/parameters", "Unused parameter", used);
    reportUnused(root, "/responses", "Unused response", used);
  }

  private void inspectOpenApi3(JsonNode root) {
    Set<JsonPointer> used = usedReferences(root, NoUnusedDefinitionCheck::openApi3Dicriminators);
    reportUnused(root, "/components/schemas", "Unused schema", used);
    reportUnused(root, "/components/parameters", "Unused parameter", used);
    reportUnused(root, "/components/responses", "Unused response", used);
    reportUnused(root, "/components/examples", "Unused example", used);
    reportUnused(root, "/components/requestBodies", "Unused request body", used);
    reportUnused(root, "/components/headers", "Unused header", used);
    reportUnused(root, "/components/links", "Unused link", used);
    reportUnused(root, "/components/callbacks", "Unused callback", used);
  }

  private void findTags(JsonNode root) {
    unusedTags.clear();
    JsonNode tagsArray = root.at("/tags").value();
    if (tagsArray != null) {
      for (JsonNode element : tagsArray.elements()) {
        unusedTags.putIfAbsent(element.at("/name").stringValue(), element);
      }
    }
  }

  private void reportUnused(JsonNode root, String pointer, String message, Set<JsonPointer> used) {
    JsonPointer jsonPointer = JsonPointer.compile(pointer);
    Set<JsonPointer> pointers = root.at(pointer)
        .propertyNames().stream()
        .map(s -> jsonPointer.append(Utils.escape(s)))
        .collect(Collectors.toSet());

    Sets.difference(pointers, used).forEach(p ->
        addIssue(message, root.at(p).key())
        );
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
      return Collections.singletonList(JsonPointer.compile(node.at("/$ref").getTokenValue().substring(1)));
    } else {
      return Collections.emptyList();
    }
  }
}
