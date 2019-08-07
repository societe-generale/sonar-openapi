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

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PathUtils {
  public static final Pattern SPINAL_CASE_PATTERN = Pattern.compile("[a-z0-9-]+");

  public static boolean isVariable(String fragment) {
    return fragment.startsWith("{") && fragment.endsWith("}");
  }

  public static boolean checkPath(String path, Predicate<String> segmentChecker) {
    for (String fragment : path.split("/")) {
      if (fragment.isEmpty() || isVariable(fragment)) {
        continue;
      }
      if (!segmentChecker.test(fragment)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isSpinalCase(String fragment) {
    return SPINAL_CASE_PATTERN.matcher(fragment).matches();
  }

  /**
   * A resource path is a path that does not end with a variable.
   * Examples:
   * <ul>
   * <li>{@code /some/parrots/{parrotId}} is not resource</li>
   * <li>{@code /some/parrots/{parrotId}} is not a resource</li>
   * <li>{@code /some/parrots/{parrotId}/head-color} is a resource</li>
   * </ul>
   * @param path the path to examine
   * @return true if the path is a resource path
   */
  public static boolean isResourcePath(String path) {
    String[] fragments = path.split("/");
    if (fragments.length == 0) {
      return true;
    }
    if (isVariable(fragments[fragments.length - 1])) {
      return false;
    }
    return true;
  }

  public static String terminalSegment(String path) {
    path = trimTrailingSlash(path);
    String[] split = path.split("/");
    return split[split.length - 1];
  }

  public static String trimTrailingSlash(String path) {
    if (path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    } else {
      return path;
    }
  }
}
