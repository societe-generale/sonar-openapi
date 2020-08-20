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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = DefinedResponseCheck.CHECK_KEY)
public class DefinedResponseCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "DefinedResponse";
  private static final String MESSAGE_NO_RESPONSE = "Define the responses of your operations.";
  private static final String MESSAGE_NO_MODEL = "Define the model of your response.";
  private static Set<String> HTTP_CODES_WHICH_CAN_HAVE_EMPTY_RESPONSE = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("204")));

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(
          OpenApi2Grammar.RESPONSES, OpenApi3Grammar.RESPONSES);
  }

  @Override
  protected void visitNode(JsonNode node) {
    Map<String, JsonNode> properties = node.propertyMap();
    if (properties.isEmpty()) {
      addIssue(MESSAGE_NO_RESPONSE, node.key());
    } else if (node.getType() == OpenApi2Grammar.RESPONSES) {
      visitV2Responses(properties);
    } else {
      visitV3Responses(properties);
    }
  }

  private void visitV2Responses(Map<String, JsonNode> responses) {
    JsonNode defaultResponse = responses.remove("default");
    boolean hasDefaultSchema = defaultResponse != null && visitResponseV2OrMediaType(defaultResponse, false);

    for (Map.Entry<String, JsonNode> entry : responses.entrySet()) {
      if (HTTP_CODES_WHICH_CAN_HAVE_EMPTY_RESPONSE.contains(entry.getKey())) {
        continue;
      }
      visitResponseV2OrMediaType(entry.getValue(), hasDefaultSchema);
    }
  }

  private boolean visitResponseV2OrMediaType(JsonNode node, boolean hasDefaultContent) {
    JsonNode actual = node.resolve();
    Map<String, JsonNode> properties = actual.propertyMap();
    if (!properties.containsKey("schema") && !hasDefaultContent) {
      addIssue(MESSAGE_NO_MODEL, node.key());
      return false;
    }
    return true;
  }

  private void visitV3Responses(Map<String, JsonNode> responses) {
    JsonNode defaultResponse = responses.remove("default");
    Map<String, Boolean> defaultSchemas = new HashMap<>();
    if (defaultResponse != null) {
      defaultSchemas.putAll(visitResponseV3(defaultResponse, Collections.emptyMap()));
    }

    for (Map.Entry<String, JsonNode> entry : responses.entrySet()) {
      if (HTTP_CODES_WHICH_CAN_HAVE_EMPTY_RESPONSE.contains(entry.getKey())) {
        continue;
      }
      visitResponseV3(entry.getValue(), defaultSchemas);
    }
  }

  private Map<String, Boolean> visitResponseV3(JsonNode node, Map<String, Boolean> defaultSchemas) {
    Map<String, JsonNode> contents = getContents(node);
    if (contents.isEmpty() && defaultSchemas.isEmpty()) {
      addIssue(MESSAGE_NO_MODEL, node.key());
      return Collections.emptyMap();
    } else {
      Map<String, Boolean> result = new HashMap<>();
      for (Map.Entry<String, JsonNode> entry : contents.entrySet()) {
        Boolean isDefaultValid = defaultSchemas.get(entry.getKey());
        boolean hasDefaultSchema = isDefaultValid != null && isDefaultValid;
        result.put(entry.getKey(), visitResponseV2OrMediaType(entry.getValue(), hasDefaultSchema));
      }
      return result;
    }
  }

  private Map<String, JsonNode> getContents(JsonNode node) {
    JsonNode actual = node.resolve();
    Map<String, JsonNode> properties = actual.propertyMap();
    JsonNode content = properties.get("content");
    if (content == null) {
      return Collections.emptyMap();
    } else {
      return content.propertyMap();
    }
  }
}
