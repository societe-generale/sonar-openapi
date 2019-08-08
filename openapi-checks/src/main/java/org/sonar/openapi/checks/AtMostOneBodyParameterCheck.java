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
import org.sonar.sslr.yaml.grammar.JsonNode;

@Rule(key = AtMostOneBodyParameterCheck.CHECK_KEY)
public class AtMostOneBodyParameterCheck extends OpenApiCheck {
  public static final String CHECK_KEY = "AtMostOneBodyParameter";

  @Override
  public Set<AstNodeType> subscribedKinds() {
    return Sets.newHashSet(OpenApi2Grammar.OPERATION);
  }

  @Override
  protected void visitNode(JsonNode node) {
    long params = node.at("/parameters").elements().stream()
        .filter(this::isBodyParam)
        .count();
    if (params > 1) {
      addIssue("An operation can have at most one body parameter.", node.key());
    }
  }

  private boolean isBodyParam(JsonNode n) {
    return n.resolve().at("/in").getTokenValue().equals("body");
  }

}
