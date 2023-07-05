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
package org.sonar.openapi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Ordering;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.plugins.openapi.api.IssueLocation;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.OpenApiVisitor;
import org.sonar.plugins.openapi.api.PreciseIssue;
import org.sonar.plugins.openapi.api.TestOpenApiVisitorRunner;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   paths:
 *     # Noncompliant@+1 {{do not use terminal '/'}}
 *     /pets/:
 * </pre>
 * Full syntax:
 * <pre>
 *   // Noncompliant@+1 [[startColumn=1;endLine=+1;endColumn=2;effortToFix=4;secondary=3,4]] {{issue message}}
 * </pre>
 * Attributes between [[]] are optional:
 * <ul>
 *   <li>startColumn: column where the highlight starts</li>
 *   <li>endLine: relative endLine where the highlight ends (i.e. +1), same line if omitted</li>
 *   <li>endColumn: column where the highlight ends</li>
 *   <li>effortToFix: the cost to fix as integer</li>
 *   <li>secondary: a comma separated list of integers identifying the lines of secondary locations if any</li>
 * </ul>
 */
public class OpenApiCheckVerifier {

  private List<TestIssue> expectedIssues = new ArrayList<>();

  public static List<PreciseIssue> scanFileForIssues(File file, OpenApiCheck check, boolean isV2) {
    return check.scanFileForIssues(TestOpenApiVisitorRunner.createContext(file, isV2));
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check.
   *
   * @param path The file to be analyzed
   * @param check The check to be used for the analysis
   * @param isV2 <code>true</code> if OpenApiSpecification version 2 or <code>false</code> if version 3
   */
  public static void verify(String path, OpenApiCheck check, boolean isV2) {
    OpenApiCheckVerifier verifier = new OpenApiCheckVerifier();
    OpenApiVisitor collector = new ExpectedIssueCollector(verifier);
    File file = new File(path);
    TestOpenApiVisitorRunner.scanFileForComments(file, isV2, collector);

    Iterator<PreciseIssue> actualIssues = getActualIssues(file, check, isV2);
    verifier.checkIssues(actualIssues);

    if (actualIssues.hasNext()){
      PreciseIssue issue = actualIssues.next();
      throw new AssertionError("Unexpected issue at line " + line(issue) + ": \"" + issue.primaryLocation().message() + "\" : pointer: " + issue.primaryLocation().pointer());
    }

  }

  private static int line(PreciseIssue issue) {
    return issue.primaryLocation().startLine();
  }

  private void checkIssues(Iterator<PreciseIssue> actualIssues) {
    for (TestIssue expected : expectedIssues) {
      if (actualIssues.hasNext()) {
        verifyIssue(expected, actualIssues.next());
      } else {
        throw new AssertionError("Missing issue at line " + expected.line());
      }
    }
  }

  private void verifyIssue(TestIssue expected, PreciseIssue actual) {
    if (line(actual) > expected.line()) {
      fail("Missing issue at line " + expected.line());
    }
    if (line(actual) < expected.line()) {
      fail("Unexpected issue at line " + line(actual) + ": \"" + actual.primaryLocation().message() + "\"");
    }
    if (expected.message() != null) {
      assertThat(actual.primaryLocation().message()).as("Bad message at line " + expected.line()).isEqualTo(expected.message());
    }
    if (expected.effortToFix() != null) {
      assertThat(actual.cost().intValue()).as("Bad effortToFix at line " + expected.line()).isEqualTo(expected.effortToFix());
    }
    if (expected.startColumn() != null) {
      assertThat(actual.primaryLocation().startLineOffset()).as("Bad start column at line " +
        expected.line()).isEqualTo(expected.startColumn());
    }
    if (expected.endColumn() != null) {
      assertThat(actual.primaryLocation().endLineOffset()).as("Bad end column at line " + expected.line()).isEqualTo(expected.endColumn());
    }
    if (expected.endLine() != null) {
      assertThat(actual.primaryLocation().endLine()).as("Bad end line at line " + expected.line()).isEqualTo(expected.endLine());
    }
    if (expected.secondaryLines() != null) {
      assertThat(secondary(actual)).as("Bad secondary locations at line " + expected.line()).isEqualTo(expected.secondaryLines());
    }
  }

  private static List<Integer> secondary(PreciseIssue issue) {
    List<Integer> result = new ArrayList<>();

    for (IssueLocation issueLocation : issue.secondaryLocations()) {
      result.add(issueLocation.startLine());
    }

    return Ordering.natural().sortedCopy(result);
  }

  private static Iterator<PreciseIssue> getActualIssues(File file, OpenApiCheck check, boolean isV2) {
    List<PreciseIssue> issues = scanFileForIssues(file, check, isV2);
    List<PreciseIssue> sortedIssues = Ordering.natural().<PreciseIssue>onResultOf(OpenApiCheckVerifier::line).sortedCopy(issues);
    return sortedIssues.iterator();
  }

  public void collectExpectedIssue(Trivia trivia) {
    String text = trivia.getToken().getValue().trim();
    String marker = "Noncompliant";

    if (text.startsWith(marker)) {
      int issueLine = trivia.getToken().getLine();
      String paramsAndMessage = text.substring(marker.length()).trim();

      if (paramsAndMessage.startsWith("@")) {
        String[] spaceSplit = paramsAndMessage.split("[\\s\\[{]", 2);
        String lineMarker = spaceSplit[0].substring(1); // remove @
        issueLine = lineValue(issueLine, lineMarker);
        paramsAndMessage = spaceSplit.length > 1 ? spaceSplit[1] : "";
      }

      TestIssue issue = TestIssue.create(null, issueLine);

      if (paramsAndMessage.startsWith("[[")) {
        int endIndex = paramsAndMessage.indexOf("]]");
        addParams(issue, paramsAndMessage.substring(2, endIndex));
        paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim();
      }

      if (paramsAndMessage.startsWith("{{")) {
        int endIndex = paramsAndMessage.indexOf("}}");
        String message = paramsAndMessage.substring(2, endIndex);
        issue.message(message);
      }

      expectedIssues.add(issue);

    } else if (text.startsWith("^")) {
      addPreciseLocation(trivia);
    }
  }

  private static void addParams(TestIssue issue, String params) {
    for (String param : Splitter.on(';').split(params)) {
      int equalIndex = param.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalStateException("Invalid param at line 1: " + param);
      }
      String name = param.substring(0, equalIndex);
      String value = param.substring(equalIndex + 1);

      if ("effortToFix".equalsIgnoreCase(name)) {
        issue.effortToFix(Integer.valueOf(value));

      } else if ("startColumn".equalsIgnoreCase(name)) {
        issue.startColumn(Integer.valueOf(value));

      } else if ("endColumn".equalsIgnoreCase(name)) {
        issue.endColumn(Integer.valueOf(value));

      } else if ("endLine".equalsIgnoreCase(name)) {
        issue.endLine(lineValue(issue.line(), value));

      } else if ("secondary".equalsIgnoreCase(name)) {
        addSecondaryLines(issue, value);

      } else {
        throw new IllegalStateException("Invalid param at line 1: " + name);
      }
    }
  }

  private static void addSecondaryLines(TestIssue issue, String value) {
    List<Integer> secondaryLines = new ArrayList<>();
    if (!"".equals(value)) {
      for (String secondary : Splitter.on(',').split(value)) {
        secondaryLines.add(lineValue(issue.line(), secondary));
      }
    }
    issue.secondary(secondaryLines);
  }

  private static int lineValue(int baseLine, String shift) {
    if (shift.startsWith("+")) {
      return baseLine + Integer.valueOf(shift.substring(1));
    }
    if (shift.startsWith("-")) {
      return baseLine - Integer.valueOf(shift.substring(1));
    }
    return Integer.valueOf(shift);
  }

  private void addPreciseLocation(Trivia trivia) {
    Token token = trivia.getToken();
    int line = token.getLine();
    String text = token.getValue();
    if (token.getColumn() > 1) {
      throw new IllegalStateException("Line " + line + ": comments asserting a precise location should start at column 1");
    }
    String missingAssertionMessage = String.format("Invalid test file: a precise location is provided at line %s but no issue is asserted at line %s", line, line - 1);
    if (expectedIssues.isEmpty()) {
      throw new IllegalStateException(missingAssertionMessage);
    }
    TestIssue issue = expectedIssues.get(expectedIssues.size() - 1);
    if (issue.line() != line - 1) {
      throw new IllegalStateException(missingAssertionMessage);
    }
    issue.endLine(issue.line());
    issue.startColumn(text.indexOf('^') + 1);
    issue.endColumn(text.lastIndexOf('^') + 2);
  }

  @VisibleForTesting
  List<TestIssue> getCollectedIssues() {
    return Collections.unmodifiableList(expectedIssues);
  }

  private static final class ExpectedIssueCollector extends OpenApiVisitor {
    private final OpenApiCheckVerifier verifier;

    private ExpectedIssueCollector(OpenApiCheckVerifier verifier) {
      this.verifier = verifier;
    }

    @Override
    protected void visitToken(Token token) {
      for (Trivia trivia : token.getTrivia()) {
        verifier.collectExpectedIssue(trivia);
      }
    }

  }

}
