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

import com.sonar.sslr.api.RecognitionException;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.openapi.metrics.FileLinesVisitor;
import org.sonar.openapi.metrics.FileMetrics;
import org.sonar.openapi.metrics.OpenApiMetrics;
import org.sonar.openapi.parser.OpenApiParser;
import org.sonar.plugins.openapi.api.IssueLocation;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.OpenApiFile;
import org.sonar.plugins.openapi.api.OpenApiVisitorContext;
import org.sonar.plugins.openapi.api.PreciseIssue;
import org.sonar.plugins.openapi.cpd.OpenApiCpdAnalyzer;
import org.sonar.sslr.yaml.grammar.ValidationException;
import org.sonar.sslr.yaml.grammar.YamlParser;

public class OpenApiAnalyzer {
  private static final Logger LOG = Loggers.get(OpenApiAnalyzer.class);

  private final SensorContext context;
  private final List<InputFile> inputFiles;
  private final OpenApiChecks checks;
  private final YamlParser parser;
  private final NoSonarFilter noSonarFilter;
  private final OpenApiCpdAnalyzer cpdAnalyzer;
  private FileLinesContextFactory fileLinesContextFactory;

  public OpenApiAnalyzer(SensorContext context, OpenApiChecks checks, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, List<InputFile> inputFiles, boolean isv2) {
    this.context = context;
    this.checks = checks;
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
    this.cpdAnalyzer = new OpenApiCpdAnalyzer(context);
    this.inputFiles = inputFiles;
    OpenApiConfiguration configuration = new OpenApiConfiguration(context.fileSystem().encoding(), true);
    if (isv2) {
      this.parser = OpenApiParser.createV2(configuration);
    } else {
      this.parser = OpenApiParser.createV3(configuration);
    }
  }

  private static NewIssueLocation newLocation(InputFile inputFile, NewIssue issue, IssueLocation location) {
    NewIssueLocation newLocation = issue.newLocation().on(inputFile);
    if (location.startLine() != IssueLocation.UNDEFINED_LINE) {
      TextRange range;
      if (location.startLineOffset() == IssueLocation.UNDEFINED_OFFSET) {
        range = inputFile.selectLine(location.startLine());
      } else {
        range = inputFile.newRange(location.startLine(), location.startLineOffset(), location.endLine(), location.endLineOffset());
      }
      newLocation.at(range);
    }

    String message = location.message();
    if (message != null) {
      newLocation.message(message);
    }
    return newLocation;
  }

  public void scanFiles() {
    for (InputFile openApiFile : inputFiles) {
      if (context.isCancelled()) {
        return;
      }
      scanFile(openApiFile);
    }
  }

  private void scanFile(InputFile inputFile) {
    OpenApiFile openApiFile = SonarQubeOpenApiFile.create(inputFile);
    OpenApiVisitorContext visitorContext;

    try {
      visitorContext = new OpenApiVisitorContext(parser.parse(inputFile.file()), parser.getIssues(), openApiFile);
      saveMeasures(inputFile, visitorContext);
    } catch (ValidationException e) {
      visitorContext = new OpenApiVisitorContext(openApiFile, e);
      LOG.error("Error during file validation: " + inputFile.filename() + "\"\n" + e.formatMessage());
      for (ValidationException cause : e.getCauses()) {
        dumpException(cause, inputFile);
      }

    } catch (RecognitionException e) {
      visitorContext = new OpenApiVisitorContext(openApiFile, e);
      LOG.error("Unable to parse file: " + inputFile.filename() + "\"\n" + e.getMessage());
      dumpException(e, inputFile);
    }

    for (OpenApiCheck check : checks.all()) {
      saveIssues(inputFile, check, check.scanFileForIssues(visitorContext));
    }
  }

  private void dumpException(RecognitionException e, InputFile inputFile) {
    int line = e.getLine();
    if (line == 0) {
      line = 1;
    }
    int column = 0;
    if (e instanceof ValidationException) {
      column = ((ValidationException) e).getNode().getToken().getColumn();
      for (ValidationException cause : ((ValidationException) e).getCauses()) {
        dumpException(cause, inputFile);
      }
    }
    context.newAnalysisError()
        .onFile(inputFile)
        .at(inputFile.newPointer(line, column))
        .message(e.getMessage())
        .save();
  }

  private void saveIssues(InputFile inputFile, OpenApiCheck check, List<PreciseIssue> issues) {
    RuleKey ruleKey = checks.ruleKeyFor(check);
    for (PreciseIssue preciseIssue : issues) {

      NewIssue newIssue = context
        .newIssue()
        .forRule(ruleKey);

      Integer cost = preciseIssue.cost();
      if (cost != null) {
        newIssue.gap(cost.doubleValue());
      }

      newIssue.at(newLocation(inputFile, newIssue, preciseIssue.primaryLocation()));

      for (IssueLocation secondaryLocation : preciseIssue.secondaryLocations()) {
        newIssue.addLocation(newLocation(inputFile, newIssue, secondaryLocation));
      }

      newIssue.save();
    }
  }

  private void saveMeasures(InputFile inputFile, OpenApiVisitorContext visitorContext) {
    FileMetrics fileMetrics = new FileMetrics(visitorContext);
    FileLinesVisitor fileLinesVisitor = fileMetrics.fileLinesVisitor();

    cpdAnalyzer.pushCpdTokens(inputFile, visitorContext);
    noSonarFilter.noSonarInFile(inputFile, fileLinesVisitor.getLinesWithNoSonar());

    Set<Integer> linesOfCode = fileLinesVisitor.getLinesOfCode();
    Set<Integer> linesOfComments = fileLinesVisitor.getLinesOfComments();

    saveMetricOnFile(inputFile, CoreMetrics.NCLOC, linesOfCode.size());
    saveMetricOnFile(inputFile, CoreMetrics.COMMENT_LINES, linesOfComments.size());

    saveMetricOnFile(inputFile, OpenApiMetrics.SCHEMAS_COUNT, fileMetrics.numberOfSchemas());
    saveMetricOnFile(inputFile, OpenApiMetrics.OPERATIONS_COUNT, fileMetrics.numberOfOperations());
    saveMetricOnFile(inputFile, OpenApiMetrics.PATHS_COUNT, fileMetrics.numberOfPaths());

    saveMetricOnFile(inputFile, CoreMetrics.COMPLEXITY, fileMetrics.complexity());

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
    for (int line : linesOfCode) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1);
    }
    for (int line : linesOfComments) {
      fileLinesContext.setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, line, 1);
    }
    fileLinesContext.save();
  }

  private void saveMetricOnFile(InputFile inputFile, Metric<Integer> metric, Integer value) {
    context.<Integer>newMeasure()
        .withValue(value)
        .forMetric(metric)
        .on(inputFile)
        .save();
  }
}
