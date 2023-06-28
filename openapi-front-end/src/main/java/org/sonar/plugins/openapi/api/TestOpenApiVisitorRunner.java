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
    Path path;
    try {
      // create a temp file without empty new lines
      Path temp = Files.createTempFile( "temp", file.getName());
      path  = Files.writeString(temp, Files.readString(file.toPath()).replaceAll("(?m)^[ \t]*\r?\n", ""));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create temp file, or write to file " + file, e);
    }
    File tempFile = new File(path.toUri());
    JsonNode rootTree = parser.parse(tempFile);
    TestOpenApiFile openApiFile = new TestOpenApiFile(tempFile);
    tempFile.deleteOnExit();
    return new OpenApiVisitorContext(rootTree, parser.getIssues(), openApiFile);
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
