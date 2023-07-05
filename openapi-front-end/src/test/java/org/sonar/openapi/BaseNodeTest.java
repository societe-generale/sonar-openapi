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

import com.sonar.sslr.api.AstNode;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.Assert;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.JsonNode;
import org.sonar.sslr.yaml.grammar.ValidationIssue;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;
import org.sonar.sslr.yaml.grammar.YamlParser;
import org.sonar.sslr.yaml.snakeyaml.parser.Tokens;

import static java.lang.reflect.Modifier.isStatic;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseNodeTest<T> {

  private final Class<? extends T> modelClass;
  protected List<ValidationIssue> issues = Collections.emptyList();

  protected BaseNodeTest() {
    modelClass = getModelClass();
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertPropertyKeys(JsonNode node) {
    return assertThat(node.properties()).extracting(n -> n.key().getTokenValue());
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertPropertyKeys(JsonNode node, String path) {
    return assertPropertyKeys(node.at(path));
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertProperties(JsonNode node, String path) {
    return assertThat(node.at(path).properties()).extracting(AstNode::getTokenValue);
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertElements(JsonNode node) {
    return assertThat(node.elements()).extracting(AstNode::getTokenValue);
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertElements(JsonNode node, String path) {
    return assertElements(node.at(path));
  }

  protected static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertKeys(Collection<JsonNode> nodes) {
    return assertThat(nodes).extracting(n -> n.key().getTokenValue());
  }

  protected static void assertEquals(String expected, JsonNode node, String path) {
    Assert.assertEquals(expected, node.at(path).getTokenValue());
  }

  protected static void assertTrue(JsonNode node, String path) {
    Assert.assertEquals(Tokens.TRUE, node.at(path).getToken().getType());
  }

  protected static void assertFalse(JsonNode node, String path) {
    Assert.assertEquals(Tokens.FALSE, node.at(path).getToken().getType());
  }

  protected static void assertMissing(JsonNode node) {
    Assert.assertTrue(node.isMissing());
  }

  protected static void assertIsRef(String expected, JsonNode node, String path) {
    assertEquals(expected, node, path + "/$ref");
  }

  private Class<? extends T> getModelClass() {
    ParameterizedType superClass = (ParameterizedType) this.getClass().getGenericSuperclass();
    String modelName = superClass.getActualTypeArguments()[0].getTypeName();
    Class<? extends T> modelClass;
    try {
      modelClass = (Class<? extends T>) Class.forName(modelName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
    return modelClass;
  }

  private YamlGrammarBuilder makeGrammar(Class<?> modelClass) {
    for (Method method : modelClass.getMethods()) {
      if (isStatic(method.getModifiers()) && method.getName().equals("create")) {
        try {
          return (YamlGrammarBuilder) method.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IllegalArgumentException(e);
        }
      }
    }
    throw new IllegalArgumentException("No static create() method found in class \"" + modelClass + "\"");
  }

  private YamlParser makeGrammarFor(GrammarRuleKey root) {
    YamlGrammarBuilder grammar = makeGrammar(modelClass);
    grammar.setRootRule(root);
    return YamlParser.builder().withStrictValidation(false).withCharset(Charset.forName("UTF-8")).withGrammar(grammar).build();
  }

  protected final JsonNode parseResource(GrammarRuleKey root, String path) {
    URL resource = BaseNodeTest.class.getResource(path);
    if (null == resource) {
      throw new IllegalArgumentException("Cannot load test resource <" + path + ">, please check test code.");
    }
    YamlParser parser = makeGrammarFor(root);
    JsonNode parsed = parser.parse(new File(resource.getFile()));
    this.issues = parser.getIssues();
    return parsed;
  }

  protected final JsonNode parseText(GrammarRuleKey root, String text) {
    YamlParser parser = makeGrammarFor(root);
    return parser.parse(text);
  }
}
