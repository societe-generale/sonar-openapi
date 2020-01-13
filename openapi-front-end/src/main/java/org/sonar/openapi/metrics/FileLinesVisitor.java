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
package org.sonar.openapi.metrics;

import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.openapi.api.OpenApiVisitor;
import org.sonar.sslr.yaml.grammar.JsonNode;

/**
 * Visitor that computes {@link CoreMetrics#NCLOC_DATA_KEY} and {@link CoreMetrics#COMMENT_LINES_DATA_KEY} metrics used by the DevCockpit.
 */
public class FileLinesVisitor extends OpenApiVisitor {

  private Set<Integer> noSonar = new HashSet<>();
  private Set<Integer> linesOfCode = new HashSet<>();      // the list of all line numbers
  private Set<Integer> linesOfComments = new HashSet<>();  // the list of all line numbers

  @Override
  protected boolean isSubscribed(AstNodeType nodeType) {
    return true;
  }

  @Override
  public void visitFile(JsonNode node) {
    noSonar.clear();
    linesOfCode.clear();
    linesOfComments.clear();
    extractNoSonarLines(node, "x-nosonar");
  }

  @Override
  protected void visitNode(JsonNode node) {
    if (node.isObject()) {
      extractNoSonarLines(node, "x-sonar-disable");
      extractNoSonarLines(node, "x-sonar-enable");
    }
  }

  /**
   * Gets the lines of codes and lines of comments (with character #).
   */
  @Override
  public void visitToken(Token token) {
    if (token.getType().equals(GenericTokenType.EOF)) {
      return;
    }

    addTokenLines(token, linesOfCode);

    for (Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        visitComment(trivia);
      }
    }
  }

  private void extractNoSonarLines(JsonNode node, String s) {
    JsonNode child2 = node.get(s);
    if (!child2.isMissing()) {
      addTokenLines(child2.getToken(), noSonar);
    }
  }

  private void addTokenLines(Token token, Set<Integer> lines) {
    // String tokens are rather complicated in Yaml. We need to work on the original value to reason on lines,
    // and remove any trailing newline that could be left by the parser
    String[] tokenLines = token.getOriginalValue().trim().split("\n", -1);
    for (int line = token.getLine(); line < token.getLine() + tokenLines.length; line++) {
      lines.add(line);
    }
  }

  private void visitComment(Trivia trivia) {
    int line = trivia.getToken().getLine();
    linesOfComments.add(line);
  }

  public Set<Integer> getLinesWithNoSonar() {
    return Collections.unmodifiableSet(new HashSet<>(noSonar));
  }

  public Set<Integer> getLinesOfCode() {
    return Collections.unmodifiableSet(new HashSet<>(linesOfCode));
  }

  public Set<Integer> getLinesOfComments() {
    return Collections.unmodifiableSet(new HashSet<>(linesOfComments));
  }
}
