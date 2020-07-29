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

import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNodeType;

@Rule(key = UrlFormatCheck.CHECK_KEY)
public class UrlFormatCheck extends OpenApiCheck {
    protected static final String CHECK_KEY = "UrlFormat";

    protected static final String MESSAGE = "Make sure to only use an valid URL.";

    @Override
    public Set<AstNodeType> subscribedKinds() {
        return ImmutableSet.of(OpenApi2Grammar.INFO, OpenApi3Grammar.INFO);
    }

    @Override
    protected void visitNode(JsonNode node) {
        if (node.getType() == OpenApi2Grammar.INFO || node.getType() == OpenApi3Grammar.INFO) {
            checkInfo(node);
        }
    }

    private void checkInfo(JsonNode node) {
        JsonNode termsOfServiceUrlNode = node.at("/termsOfService");
        JsonNode contactUrlNode = node.at("/contact/url");
        JsonNode licenseUrlNode = node.at("/license/url");
        checkUrl(termsOfServiceUrlNode, contactUrlNode, licenseUrlNode);
    }

    private void checkUrl(JsonNode... urlNodes) {
        UrlValidator urlValidator = UrlValidator.getInstance();
        String url;
        for (JsonNode node : urlNodes) {
            url = node.getTokenValue();
            if (url != null && !url.trim().isEmpty() && !url.equals("null") && !urlValidator.isValid(node.getTokenValue())) {
                addIssue(MESSAGE, node);
            }
        }
    }
}
