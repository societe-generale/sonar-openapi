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

import java.io.IOException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.openapi.api.OpenApiFile;

public abstract class SonarQubeOpenApiFile implements OpenApiFile {

  private final InputFile inputFile;

  private SonarQubeOpenApiFile(InputFile inputFile) {
    this.inputFile = inputFile;
  }

  public static OpenApiFile create(InputFile inputFile) {
    return new Sq62File(inputFile);
  }

  @Override
  public String fileName() {
    return inputFile.filename();
  }

  public InputFile inputFile() {
    return inputFile;
  }

  private static class Sq62File extends SonarQubeOpenApiFile {

    public Sq62File(InputFile inputFile) {
      super(inputFile);
    }

    @Override
    public String content() {
      try {
        return inputFile().contents();
      } catch (IOException e) {
        throw new IllegalStateException("Could not read content of input file " + inputFile(), e);
      }
    }

  }

}
