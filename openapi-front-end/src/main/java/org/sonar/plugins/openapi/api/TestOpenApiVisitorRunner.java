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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonar.openapi.OpenApiConfiguration;
import org.sonar.openapi.parser.OpenApiParser;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.YamlParser;

public class TestOpenApiVisitorRunner {

  private TestOpenApiVisitorRunner() {
  }

  public static void scanFile(File file, OpenApiVisitor... visitors) {
    OpenApiVisitorContext context = createContext(file);
    for (OpenApiVisitor visitor : visitors) {
      visitor.scanFile(context);
    }
  }

  public static void scanFileForComments(File file, boolean isV2, OpenApiVisitor... visitors) {
    OpenApiVisitorContext context = createContext(file, isV2);
    for (OpenApiVisitor visitor : visitors) {
      visitor.scanFile(context);
    }
  }

  public static OpenApiVisitorContext createContext(File file) {
    return createContext(file, false);
  }

  public static OpenApiVisitorContext createContext(File file, boolean v2) {
    OpenApiConfiguration configuration = new OpenApiConfiguration(StandardCharsets.UTF_8, false);
    YamlParser parser = v2 ? OpenApiParser.createV2(configuration) : OpenApiParser.createV3(configuration);
    return createContext(file, parser);
  }

  public static OpenApiVisitorContext createContext(File file, YamlParser parser) {
    try {
      modifyFile(file.getAbsolutePath(), Files.readString(file.toPath()));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + file, e);
    }
    TestOpenApiFile openApiFile = new TestOpenApiFile(file);
    JsonNode rootTree = parser.parse(file);
    return new OpenApiVisitorContext(rootTree, parser.getIssues(), openApiFile);
  }
  // removes empty newlines from file
  public static void modifyFile(String filePath,String content)
  {
    var contentWithoutEmptyNewLines = content.replaceAll("(?m)^[ \t]*\r?\n", "");
    File fileToBeModified = new File(filePath);
    FileWriter writer = null;

    try
    {
      //Rewriting the input text file with newContent
      writer = new FileWriter(fileToBeModified);
      writer.write(contentWithoutEmptyNewLines);
      //Closing the resources
      writer.close();
    }
    catch (IOException e)
    {
      throw new IllegalStateException("Cannot write to " + fileToBeModified, e);
    }
  }

  private static class TestOpenApiFile implements OpenApiFile {

    private final File file;

    public TestOpenApiFile(File file) {
      this.file = file;
    }

    @Override
    public String content() {
      try {
        return Files.readString(file.toPath()).replaceAll("(?m)^[ \t]*\r?\n", "");
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read " + file, e);
      }
    }

    @Override
    public String fileName() {
      return file.getName();
    }

  }

}
