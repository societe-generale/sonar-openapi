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
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = ProvideRequestBodyDescriptionCheck.CHECK_KEY)
public class ProvideRequestBodyDescriptionCheck extends OpenApiCheck {
  public static final String CHECK_KEY = "ProvideRequestBodyDescription";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION, OpenApi3Grammar.REQUEST_BODY);
  }

  @Override
  protected void visitNode(JsonNode node) {
    if (node.getType() == OpenApi3Grammar.REQUEST_BODY) {
      visitOpenApi3(node);
    } else {
      visitOpenApi2(node);
    }
  }

  private void visitOpenApi2(JsonNode node) {
    JsonNode requestBody = node.get("requestBody");
    if (requestBody.isMissing()){
      return;
    }
    JsonNode description = requestBody.get("description");
    if (description.isMissing()) {
      addIssue("Provide a description for each request body.", node.key());
    }
  }

  private void visitOpenApi3(JsonNode node) {
    JsonNode description = node.get("description");
    if (description.isMissing()) {
      addIssue("Provide a description for each request body.", node.key());
    }
  }

}
