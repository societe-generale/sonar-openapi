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

import com.sonar.sslr.api.Token;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.sslr.yaml.grammar.JsonNode;

public abstract class IssueLocation {

  public static final int UNDEFINED_OFFSET = -1;

  public static final int UNDEFINED_LINE = 0;
  public static final String EMPTY_POINTER = "";

  private String message;

  private IssueLocation(@Nullable String message) {
    this.message = message;
  }

  public static IssueLocation atFileLevel(@Nullable String message) {
    return new FileLevelIssueLocation(message);
  }

  public static IssueLocation atLineLevel(@Nullable String message, int lineNumber) {
    return new LineLevelIssueLocation(message, lineNumber);
  }

  public static IssueLocation preciseLocation(@Nullable String message, JsonNode startNode) {
    return new PreciseIssueLocation(startNode, message);
  }

  public static IssueLocation preciseLocation(JsonNode startNode, JsonNode endNode, @Nullable String message) {
    return new PreciseIssueLocation(startNode, endNode, message);
  }

  @CheckForNull
  public String message() {
    return message;
  }

  public abstract int startLine();

  public abstract int startLineOffset();

  public abstract int endLine();

  public abstract int endLineOffset();

  @Override
  public final boolean equals(Object o){
    if (o instanceof IssueLocation) {
      IssueLocation o1 = (IssueLocation) o;
      boolean equals =  o1.startLine() == this.startLine() &&
        o1.endLine() == this.endLine() &&
        o1.endLineOffset() == this.endLineOffset() &&
        o1.startLineOffset() == this.startLineOffset();
      if (this.message != null){
        equals &= this.message.equals(o1.message());
      }else{
        equals &= (o1.message() == null);
      }
      return equals;
    }
    return false;
  }

  @Override
  public final int hashCode(){
    int hashCode = startLine() + endLine() + startLineOffset() + endLineOffset();
    if (message != null){
           hashCode += message.hashCode();
    }
    return hashCode;
  }

  public String pointer() {
    return EMPTY_POINTER;
  }


  public static class TokenLocation {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;

    public TokenLocation(Token token) {
      this.startLine = token.getLine();
      this.startLineOffset = token.getColumn();
      String value = token.getOriginalValue();
      String[] lines = value.split("\r\n|\n|\r", -1);

      if (lines.length > 1) {
        endLine = token.getLine() + lines.length - 1;
        endLineOffset = lines[lines.length - 1].length();

      } else {
        this.endLine = this.startLine;
        this.endLineOffset = this.startLineOffset + token.getOriginalValue().length();
      }
    }

    public int startLine() {
      return startLine;
    }

    public int startLineOffset() {
      return startLineOffset;
    }

    public int endLine() {
      return endLine;
    }

    public int endLineOffset() {
      return endLineOffset;
    }

  }

  private static class PreciseIssueLocation extends IssueLocation {

    private final TokenLocation firstTokenLocation;
    private final TokenLocation lastTokenLocation;
    private final String pointer;

    public PreciseIssueLocation(JsonNode node, @Nullable String message) {
      super(message);
      this.firstTokenLocation = new TokenLocation(node.getToken());
      this.lastTokenLocation = new TokenLocation(node.getLastToken());
      this.pointer = node.getPointer()
          .replace("~1", "/")
          .replace("~0", "~")
          .replace("paths//", "paths/");
    }

    public PreciseIssueLocation(JsonNode startNode, JsonNode endNode, @Nullable String message) {
      super(message);
      this.firstTokenLocation = new TokenLocation(startNode.getToken());
      this.lastTokenLocation = new TokenLocation(endNode.getLastToken());
      this.pointer = startNode.getPointer();
    }

    @Override
    public int startLine() {
      return firstTokenLocation.startLine();
    }

    @Override
    public int startLineOffset() {
      return firstTokenLocation.startLineOffset();
    }

    @Override
    public int endLine() {
      return lastTokenLocation.endLine();
    }

    @Override
    public int endLineOffset() {
      return lastTokenLocation.endLineOffset();
    }

    @Override
    public String pointer() {
      return pointer;
    }
  }


  private static class LineLevelIssueLocation extends IssueLocation {

    private final int lineNumber;

    public LineLevelIssueLocation(@Nullable String message, int lineNumber) {
      super(message);
      this.lineNumber = lineNumber;
    }

    @Override
    public int startLine() {
      return lineNumber;
    }

    @Override
    public int startLineOffset() {
      return UNDEFINED_OFFSET;
    }

    @Override
    public int endLine() {
      return lineNumber;
    }

    @Override
    public int endLineOffset() {
      return UNDEFINED_OFFSET;
    }

  }

  private static class FileLevelIssueLocation extends IssueLocation {

    public FileLevelIssueLocation(@Nullable String message) {
      super(message);
    }

    @Override
    public int startLine() {
      return UNDEFINED_LINE;
    }

    @Override
    public int startLineOffset() {
      return UNDEFINED_OFFSET;
    }

    @Override
    public int endLine() {
      return UNDEFINED_LINE;
    }

    @Override
    public int endLineOffset() {
      return UNDEFINED_OFFSET;
    }

  }
}
