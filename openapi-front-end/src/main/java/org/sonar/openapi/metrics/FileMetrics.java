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

import com.sonar.sslr.api.AstNode;
import org.sonar.plugins.openapi.api.OpenApiVisitorContext;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

/**
 * Computes metrics that will be exposed by the plugin on each file.
 */
public class FileMetrics {

  private int numberOfSchemas;
  private int numberOfPaths;
  private int numberOfOperations;

  private final ComplexityVisitor complexityVisitor = new ComplexityVisitor();
  private final FileLinesVisitor fileLinesVisitor;

  public FileMetrics(OpenApiVisitorContext context) {
    countObjects(context);
    complexityVisitor.scanFile(context);
    fileLinesVisitor = new FileLinesVisitor();
    fileLinesVisitor.scanFile(context);
  }

  private void countObjects(OpenApiVisitorContext context) {
    AstNode rootTree = context.rootTree();
    if (rootTree != null) {
      numberOfSchemas = (int)rootTree.getDescendants(OpenApi2Grammar.SCHEMA, OpenApi3Grammar.SCHEMA)
          .stream().filter(FileMetrics::isNotRef)
          .count();
      numberOfPaths = (int)rootTree.getDescendants(OpenApi2Grammar.PATH, OpenApi3Grammar.PATH)
          .stream().filter(FileMetrics::isNotRef)
          .count();
      numberOfOperations = rootTree.getDescendants(OpenApi2Grammar.OPERATION, OpenApi3Grammar.OPERATION).size();
    } else {
      numberOfSchemas = 0;
      numberOfPaths = 0;
      numberOfOperations = 0;
    }
  }

  public int numberOfOperations() {
    return numberOfOperations;
  }

  public int numberOfPaths() {
    return numberOfPaths;
  }

  public int numberOfSchemas() {
    return numberOfSchemas;
  }

  public int complexity() {
    return complexityVisitor.getComplexity();
  }

  public FileLinesVisitor fileLinesVisitor() {
    return fileLinesVisitor;
  }

  private static boolean isNotRef(AstNode node) {
    return !((JsonNode)node).isRef();
  }

}
