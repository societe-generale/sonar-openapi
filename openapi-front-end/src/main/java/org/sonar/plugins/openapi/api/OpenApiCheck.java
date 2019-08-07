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
package org.sonar.plugins.openapi.api;

import com.sonar.sslr.api.AstNodeType;
import java.util.*;
import org.sonar.check.Rule;
import org.sonar.sslr.yaml.grammar.JsonNode;

/**
 * Base class for verification rules.
 */
public class OpenApiCheck extends OpenApiVisitor {
  private Set<PreciseIssue> issues = new LinkedHashSet<>();

  /**
   * Scan the file provided in the context. If no file is provided, does nothing. This method clears the previous state
   * at the beginning of its execution.
   * @param context the analysis context
   * @return the list of collected issues
   */
  public List<PreciseIssue> scanFileForIssues(OpenApiVisitorContext context) {
    issues.clear();
    scanFile(context);
    return Collections.unmodifiableList(new ArrayList<>(issues));
  }

  /**
   * The kind of nodes the visitor is interested in. Override and return a non-empty set if you want to be notified
   * on {@link #visitNode(JsonNode)} and {@link #leaveNode(JsonNode)}.
   * <p>
   * By default, returns an empty set.
   * @return the list of node types to consider in the analysis
   */
  public Set<AstNodeType> subscribedKinds() {
    return Collections.emptySet();
  }

  @Override
  protected final boolean isSubscribed(AstNodeType nodeType) {
    return subscribedKinds().contains(nodeType);
  }

  @Override
  protected final boolean isSkipped(JsonNode node) {
    Optional<String> ruleId = getRuleId();
    if (ruleId.isPresent()) {
      return !getContext().isEnabled(ruleId.get(), node);
    } else {
      return false;
    }
  }

  public final Optional<String> getRuleId() {
    Rule rule = this.getClass().getAnnotation(Rule.class);
    if (rule == null) {
      return Optional.empty();
    } else {
      return Optional.of(rule.key());
    }
  }

  /**
   * Record an issue on the supplied node's exact location.
   * @param message Message to record
   * @param node Location of the issue
   * @return the created issue, for customization
   */
  protected final PreciseIssue addIssue(String message, JsonNode node) {
    PreciseIssue newIssue = new PreciseIssue(IssueLocation.preciseLocation(message, node));
    issues.add(newIssue);
    return newIssue;
  }

  /**
   * Record an issue on a given line.
   * @param message Message to record
   * @param lineNumber The line of the issue (starting at 1).
   * @return the created issue, for customization
   */
  protected final PreciseIssue addLineIssue(String message, int lineNumber) {
    PreciseIssue newIssue = new PreciseIssue(IssueLocation.atLineLevel(message, lineNumber));
    issues.add(newIssue);
    return newIssue;
  }

}
