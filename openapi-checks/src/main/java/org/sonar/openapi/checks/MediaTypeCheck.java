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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = MediaTypeCheck.CHECK_KEY)
public class MediaTypeCheck extends OpenApiCheck {
  protected static final String CHECK_KEY = "MediaType";
  protected static final String MESSAGE_V2 = "Declared mime type should conform to RFC6838.";
  protected static final String MESSAGE_V3 = "Declared media type range should conform to RFC7231.";

  @VisibleForTesting
  static final Pattern MIME_TYPE_PATTERN = Pattern.compile("[a-zA-Z.0-9][a-zA-Z.0-9!#$&-_^+]+/[a-zA-Z.0-9][a-zA-Z.0-9!#$&-_^+]+(; charset=[a-zA-Z0-9-_]+)?");
  @VisibleForTesting
  static final Pattern MEDIA_RANGE_PATTERN = Pattern.compile("[a-zA-Z.0-9][a-zA-Z.0-9!#$&-_^+]+/(\\*|[a-zA-Z.0-9][a-zA-Z.0-9!#$&-_^+]+(; charset=[a-zA-Z0-9-_]+)?)");

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.ROOT, OpenApi2Grammar.OPERATION, OpenApi3Grammar.RESPONSE, OpenApi3Grammar.REQUEST_BODY, OpenApi3Grammar.PARAMETER);
  }

  @Override
  protected void visitNode(JsonNode node) {
    if (node.getType() instanceof OpenApi2Grammar) {
      visitOpenApi2(node);
    } else {
      visitOpenApi3(node);
    }
  }

  private void visitOpenApi2(JsonNode node) {
    verifyMimeTypeArray(node.at("/produces"));
    verifyMimeTypeArray(node.at("/consumes"));
  }

  private void verifyMimeTypeArray(JsonNode node) {
    for (JsonNode element : node.elements()) {
      if (!MIME_TYPE_PATTERN.matcher(element.getTokenValue()).matches()) {
        addIssue(MESSAGE_V2, element);
      }
    }
  }

  private void visitOpenApi3(JsonNode node) {
    if (node.getType() == OpenApi3Grammar.PARAMETER) {
      verifyParameterContent(node);
    } else {
      verifyContent(node);
    }
  }

  private void verifyParameterContent(JsonNode node) {
    JsonNode content = node.at("/content");
    Map<String, JsonNode> properties = content.propertyMap();
    for (JsonNode property : properties.values()) {
      JsonNode keyNode = property.key();
      String key = keyNode.getTokenValue();
      if (!MIME_TYPE_PATTERN.matcher(key).matches()) {
        addIssue(MESSAGE_V2, keyNode);
      }
    }
  }

  private void verifyContent(JsonNode node) {
    JsonNode content = node.at("/content");
    for (JsonNode property : content.propertyMap().values()) {
      JsonNode keyNode = property.key();
      String key = keyNode.getTokenValue();
      if (!MEDIA_RANGE_PATTERN.matcher(key).matches()) {
        addIssue(MESSAGE_V3, keyNode);
      }
    }
  }
}
