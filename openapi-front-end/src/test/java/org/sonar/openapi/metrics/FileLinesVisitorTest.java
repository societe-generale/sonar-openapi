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

import java.io.File;
import org.junit.Test;
import org.sonar.plugins.openapi.api.TestOpenApiVisitorRunner;

import static org.assertj.core.api.Assertions.assertThat;


public class FileLinesVisitorTest {
  private static final File BASE_DIR = new File("src/test/resources/metrics");

  @Test
  public void can_report_metrics() {
    FileLinesVisitor visitor = new FileLinesVisitor();

    TestOpenApiVisitorRunner.scanFile(new File(BASE_DIR, "file-lines.yaml"), visitor);

    // sonar extensions are counted as lines of code
    assertThat(visitor.getLinesOfCode()).hasSize(16);
    assertThat(visitor.getLinesOfCode()).containsOnly(1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 17, 18, 19);

    assertThat(visitor.getLinesOfComments()).hasSize(3);
    assertThat(visitor.getLinesOfComments()).containsOnly(8, 15, 16);

    // x-nosonar is a global modifier, it is ignored in the report
    assertThat(visitor.getLinesWithNoSonar()).hasSize(2);
    assertThat(visitor.getLinesWithNoSonar()).containsOnly(9, 12);
  }

  @Test
  public void correctly_reports_strings_with_embedded_newline() {
    FileLinesVisitor visitor = new FileLinesVisitor();

    TestOpenApiVisitorRunner.scanFile(new File(BASE_DIR, "embedded-newlines-lines.yaml"), visitor);

    assertThat(visitor.getLinesOfCode()).hasSize(16);
    assertThat(visitor.getLinesOfCode()).containsOnly(1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 17, 18, 19);
  }

  @Test
  public void correctly_reports_multiline_strings() {
    FileLinesVisitor visitor = new FileLinesVisitor();

    TestOpenApiVisitorRunner.scanFile(new File(BASE_DIR, "multiline-lines.yaml"), visitor);

    assertThat(visitor.getLinesOfCode()).hasSize(18);
    assertThat(visitor.getLinesOfCode()).containsOnly(1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21);
  }
}