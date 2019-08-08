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

import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.List;
import org.sonar.sslr.yaml.grammar.JsonNode;

/**
 * Base visitor for the OpenAPI checks.
 */
public class OpenApiVisitor {

  private OpenApiVisitorContext context;

  public final OpenApiVisitorContext getContext() {
    return context;
  }

  /**
   * Called before visiting a node. Gives a chance to decide if this node type interests the visitor.
   * @param nodeType the type of node that will be visited
   * @return {@code true} to proceed with nodes of this type
   */
  protected boolean isSubscribed(AstNodeType nodeType) {
    return false;
  }

  /**
   * Called before visiting a node. If the method returns {@code true}, the visitor method is not invoked.
   * @param node the node being visited
   * @return {@code true} to skip the node
   */
  protected boolean isSkipped(JsonNode node) {
    return false;
  }

  /**
   * Called before the visitor starts exploring the AST hierarchy.
   * @param root the AST that will be visited
   */
  protected void visitFile(JsonNode root) {
    // empty default implementation
  }

  /**
   * Called after the visitor is done exploring the AST hierarchy.
   * @param node the AST that has been visited
   */
  protected void leaveFile(JsonNode node) {
    // empty default implementation
  }

  /**
   * Called when then visitor enters a node matching {@link #isSubscribed(AstNodeType)}, before visiting its children or tokens.
   * @param node the node to visit
   */
  protected void visitNode(JsonNode node) {
    // empty default implementation
  }

  /**
   * Called when then visitor is done visiting the children of a node matching {@link #isSubscribed(AstNodeType)}.
   * @param node the node that has been visited
   */
  protected void leaveNode(JsonNode node) {
    // empty default implementation
  }

  /**
   * Called after visiting a terminal node (a node without children). Always called, even if the node doesn't match {@link #isSubscribed(AstNodeType)}.
   * @param token the node's token
   */
  protected void visitToken(Token token) {
    // empty default implementation
  }

  /**
   * Visit the AST node carried by the supplied {@code context}.
   * @param context the description of the file to scan
   */
  public void scanFile(OpenApiVisitorContext context) {
    this.context = context;
    JsonNode rootNode = context.rootTree();
    if (rootNode != null) {
      visitFile(rootNode);
      scanNode(rootNode);
      leaveFile(rootNode);
    }
  }

  /**
   * Visit the AST node.
   * @param node the node to visit
   */
  public void scanNode(JsonNode node) {
    boolean isSubscribedType = isSubscribed(node.getType());
    boolean isSkipped = isSkipped(node);

    if (isSubscribedType && !isSkipped) {
      visitNode(node);
    }

    List<JsonNode> children = node.getJsonChildren();
    if (children.isEmpty() && !isSkipped) {
      for (Token token : node.getTokens()) {
        visitToken(token);
      }
    } else {
      for (JsonNode child : children) {
        scanNode(child);
      }
    }

    if (isSubscribedType && !isSkipped) {
      leaveNode(node);
    }
  }


}
