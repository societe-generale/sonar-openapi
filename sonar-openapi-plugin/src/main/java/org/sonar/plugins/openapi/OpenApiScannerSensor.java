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
package org.sonar.plugins.openapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.openapi.OpenApiAnalyzer;
import org.sonar.openapi.OpenApiChecks;
import org.sonar.openapi.checks.CheckList;
import org.sonar.plugins.openapi.api.OpenApiCustomRuleRepository;

public class OpenApiScannerSensor implements Sensor {
  private static final Logger LOGGER = Loggers.get(OpenApiScannerSensor.class);
  private final OpenApiChecks checks;
  private FileLinesContextFactory fileLinesContextFactory;
  private final NoSonarFilter noSonarFilter;

  public OpenApiScannerSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter) {
    this(checkFactory, fileLinesContextFactory, noSonarFilter, null);
  }

  public OpenApiScannerSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, @Nullable OpenApiCustomRuleRepository[] customRuleRepositories) {
    // customRulesRepositories is injected by the context, if present
    this.checks = OpenApiChecks.createOpenApiCheck(checkFactory)
      .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
      .addCustomChecks(customRuleRepositories);
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.noSonarFilter = noSonarFilter;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("OpenAPI Scanner Sensor")
      .onlyOnFileType(InputFile.Type.MAIN)
      .onlyOnLanguage(OpenApi.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicates p = context.fileSystem().predicates();
    OpenApiProperties openApiProperties = new OpenApiProperties();

    //scanFiles(context, p, openApiProperties.getV2FilesPattern(context), true);
    scanFiles(context, p, openApiProperties.getV3FilesPattern(context), false);
  }

  public void scanFiles(SensorContext context, FilePredicates p, String[] pathPatterns, boolean isV2) {
    Iterable<InputFile> it = context.fileSystem().inputFiles(
      p.and(p.hasType(InputFile.Type.MAIN),
        p.hasLanguage(OpenApi.KEY),
        p.matchesPathPatterns(pathPatterns)));
    List<InputFile> v3list = new ArrayList<>();
    List<InputFile> v2list = new ArrayList<>();
    for (InputFile inputFile : it) {
      if (isVersionMatch(inputFile.path(), "\\\"{0,}openapi\\\"{0,}\\s?:\\s?[\\\"\\']?3\\.0\\.[0,1,2,3][\\\"\\']?")) {
        LOGGER.info("Identified version v3 for : {}.", inputFile.absolutePath());

        v3list.add(inputFile);
      } else if (isVersionMatch(inputFile.path(), "\\\"{0,}swagger\\\"{0,}\\:\\s?[\\\"\\']?2\\.0[\\\"\\']?")) {
        LOGGER.info("Identified version v2 for : {}.", inputFile.absolutePath());
        v2list.add(inputFile);
      } else {
        LOGGER.warn("OpenAPI Scanner could not detect version of: {}. It will be parsed as openapi 3.0.+", inputFile.absolutePath());
        v3list.add(inputFile);
      }
    }

    List<InputFile> inputFiles = Collections.unmodifiableList(v3list);
    if (!inputFiles.isEmpty()) {
      OpenApiAnalyzer scanner = new OpenApiAnalyzer(context, checks, fileLinesContextFactory, noSonarFilter, inputFiles, false);
      LOGGER.info("OpenAPI Scanner called for the following files: {}.", inputFiles);
      scanner.scanFiles();
    }

    inputFiles = Collections.unmodifiableList(v2list);
    if (!inputFiles.isEmpty()) {
      OpenApiAnalyzer scanner = new OpenApiAnalyzer(context, checks, fileLinesContextFactory, noSonarFilter, inputFiles, true);
      LOGGER.info("OpenAPI Scanner called for the following files: {}.", inputFiles);
      scanner.scanFiles();
    }
}

  private static boolean isVersionMatch(Path path, String version) {
    Pattern regex = Pattern.compile(version);

    try (Stream<String> stream = Files.lines(path)) {
      return stream.anyMatch( l -> regex.matcher(l).find());
    } catch (IOException e) {
      LOGGER.error("exception for file {} : {}.", path, e.getMessage());
      e.printStackTrace();
    }
    return false;
  }
}
