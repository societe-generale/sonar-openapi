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

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.openapi.metrics.OpenApiMetrics;

public class OpenApiPlugin implements Plugin {

  public static final String FILE_SUFFIXES_KEY = "sonar.openapi.file.suffixes";
  public static final String OPENAPI_CATEGORY = "OpenApi";
  // Subcategories
  private static final String GENERAL = "General";

  @Override
  public void define(Context context) {

    context.addExtensions(
      PropertyDefinition.builder(FILE_SUFFIXES_KEY)
        .index(10)
        .name("File Suffixes")
        .description("A list of suffixes of OpenAPI files to analyze.")
        .category(OPENAPI_CATEGORY)
        .subCategory(GENERAL)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .defaultValue("yml,json")
        .build(),
      PropertyDefinition.builder(OpenApiProperties.V3_PATH_KEY)
        .index(11)
        .name("Paths to OpenAPI contract(s)")
        .description("Path to OpenAPI contracts. Ant patterns are accepted for relative path. The contracts can be in JSON or in YML.")
        .category(OPENAPI_CATEGORY)
        .subCategory(GENERAL)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .defaultValue(OpenApiProperties.DEFAULT_V3_PATH)
        .build(),
      OpenApi.class,
      OpenApiProfileDefinition.class,
      OpenApiScannerSensor.class,
      OpenApiRulesDefinition.class,
      OpenApiMetrics.class);
  }

}
