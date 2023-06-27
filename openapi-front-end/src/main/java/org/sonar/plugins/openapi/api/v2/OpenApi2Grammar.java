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
package org.sonar.plugins.openapi.api.v2;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;

@java.lang.SuppressWarnings("squid:S1192") // Voluntarily ignoring string constants redefinitions in this file
public enum OpenApi2Grammar implements GrammarRuleKey {
  ROOT,
  INFO,
  PATHS,
  SCHEMA,
  PARAMETER,
  RESPONSE,
  SECURITY_SCHEME,
  BASIC_SECURITY_SCHEME,
  API_KEY_SECURITY_SCHEME,
  OAUTH2_SECURITY_SCHEME,
  SECURITY_REQUIREMENT,
  TAG,

  REF,
  EXTERNAL_DOC,

  CONTACT,
  LICENSE,

  PATH,
  OPERATION,
  RESPONSES,

  BODY_PARAM,
  OTHER_PARAM,
  ITEMS,
  HEADER,
  EXAMPLE,
  XML,
  SCOPES,
  DEFINITIONS,
  PARAMETERS,
  RESPONSES_DEFINITIONS,
  SECURITY_DEFINITIONS,
  DESCRIPTION;

  private static final String EXTENSION_PATTERN = "^x-.*";

  public static YamlGrammarBuilder create() {
    YamlGrammarBuilder b = new YamlGrammarBuilder();
    b.setRootRule(ROOT);

    b.rule(ROOT).is(b.object(
      b.mandatoryProperty("swagger", "2.0"),
      b.mandatoryProperty("info", INFO),
      b.property("host", b.string()),
      b.property("basePath", b.string()),
      b.property("schemes", b.array(b.firstOf("http", "https", "ws", "wss"))),
      b.property("consumes", b.array(b.string())),
      b.property("produces", b.array(b.string())),
      b.mandatoryProperty("paths", PATHS),
      b.property("definitions", DEFINITIONS),
      b.property("parameters", PARAMETERS),
      b.property("responses", RESPONSES_DEFINITIONS),
      b.property("securityDefinitions", SECURITY_DEFINITIONS),
      b.property("security", b.array(SECURITY_REQUIREMENT)),
      b.property("tags", b.array(TAG)),
      b.property("externalDocs", EXTERNAL_DOC),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(DEFINITIONS).is(b.object(b.patternProperty(".*", SCHEMA)));
    b.rule(PARAMETERS).is(b.object(b.patternProperty(".*", PARAMETER)));
    b.rule(RESPONSES_DEFINITIONS).is(b.object(b.patternProperty(".*", RESPONSE)));
    b.rule(SECURITY_DEFINITIONS).is(b.object(
      b.patternProperty(".*", SECURITY_SCHEME)));
    b.rule(REF).is(b.object(
      b.mandatoryProperty("$ref", b.string())));
    b.rule(EXTERNAL_DOC).is(b.object(
      b.mandatoryProperty("url", b.string()),
      b.property("description", DESCRIPTION),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

    b.rule(DESCRIPTION).is(b.string()).skip();
    buildInfo(b);
    buildPaths(b);
    buildDefinitions(b);
    buildParameters(b);
    buildResponses(b);
    buildSecurityDefinitions(b);
    buildTags(b);

    return b;
  }

  private static void buildTags(YamlGrammarBuilder b) {
    b.rule(TAG).is(b.object(
      b.mandatoryProperty("name", b.string()),
      b.property("description", DESCRIPTION),
      b.property("externalDocs", EXTERNAL_DOC),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildSecurityDefinitions(YamlGrammarBuilder b) {
    b.rule(SECURITY_SCHEME).is(b.firstOf(
        BASIC_SECURITY_SCHEME,
        API_KEY_SECURITY_SCHEME,
        OAUTH2_SECURITY_SCHEME));
    b.rule(BASIC_SECURITY_SCHEME).is(b.object(
      b.discriminant("type","basic"),
      b.property("description", DESCRIPTION),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(API_KEY_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "apiKey"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("name", b.string()),
      b.mandatoryProperty("in", b.firstOf("query", "header")),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(OAUTH2_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "oauth2"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("flow", b.firstOf("implicit", "password", "application", "accessCode")),
      b.property("authorizationUrl", b.string()),
      b.property("tokenUrl", b.string()),
      b.mandatoryProperty("scopes", SCOPES),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(SCOPES).is(b.object(
      b.patternProperty("^[^x]{2}.*", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(SECURITY_REQUIREMENT).is(b.object(
      b.patternProperty(".*", b.array(b.string()))));
  }

  private static void buildResponses(YamlGrammarBuilder b) {
    b.rule(RESPONSES).is(b.object(
      b.property("default", b.firstOf(RESPONSE, REF)),
      b.patternProperty("^[0-9xX]+", b.firstOf(RESPONSE, REF)),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(RESPONSE).is(b.object(
      b.mandatoryProperty("description", DESCRIPTION),
      b.property("schema", SCHEMA),
      b.property("headers", b.object(b.patternProperty(".*", HEADER))),
      b.property("example", EXAMPLE),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(HEADER).is(b.object(
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("type", b.firstOf("string", "number", "integer", "boolean", "array")),
      b.property("format", b.string()),
      b.property("items", b.firstOf(REF, ITEMS)),
      b.property("collectionFormat", b.firstOf("csv", "ssv", "tsv", "pipes")),
      b.property("default", b.anything()),
      b.property("maximum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMaximum", b.bool()),
      b.property("minimum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMinimum", b.bool()),
      b.property("maxLength", b.integer()),
      b.property("minLength", b.integer()),
      b.property("pattern", b.string()),
      b.property("maxItems", b.integer()),
      b.property("minItems", b.integer()),
      b.property("uniqueItems", b.bool()),
      b.property("enum", b.array(b.anything())),
      b.property("multipleOf", b.firstOf(b.integer(), b.floating())),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(EXAMPLE).is(b.object(
      b.patternProperty("^[a-zA-Z]+/[a-zA-Z0-9-.]+(; [a-zA-Z0-9]+=[a-zA-Z0-9]+)?", b.anything())));
  }

  private static void buildParameters(YamlGrammarBuilder b) {
    b.rule(PARAMETER).is(b.firstOf("in", BODY_PARAM, OTHER_PARAM));

    b.rule(BODY_PARAM).is(b.object(
      b.mandatoryProperty("name", b.string()),
      b.discriminant("in", "body"),
      b.property("description", DESCRIPTION),
      b.property("required", b.bool()),
      b.property("deprecated", b.bool()),
      b.mandatoryProperty("schema", SCHEMA),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();

    b.rule(OTHER_PARAM).is(b.object(
      b.mandatoryProperty("name", b.string()),
      b.discriminant("in", b.firstOf("path", "query", "header", "formData")),
      b.property("description", DESCRIPTION),
      b.property("required", b.bool()),
      b.property("deprecated", b.bool()),
      b.mandatoryProperty("type", b.firstOf("string", "number", "integer", "boolean", "array", "file")),
      b.property("format", b.string()),
      b.property("allowEmptyValue", b.bool()),
      b.property("items", ITEMS),
      b.property("collectionFormat", b.firstOf("csv", "ssv", "tsv", "pipes", "multi")),
      b.property("default", b.anything()),
      b.property("maximum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMaximum", b.bool()),
      b.property("minimum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMinimum", b.bool()),
      b.property("maxLength", b.integer()),
      b.property("minLength", b.integer()),
      b.property("pattern", b.string()),
      b.property("maxItems", b.integer()),
      b.property("minItems", b.integer()),
      b.property("uniqueItems", b.bool()),
      b.property("enum", b.array(b.anything())),
      b.property("multipleOf", b.firstOf(b.integer(), b.floating())),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(ITEMS).is(b.object(
      b.mandatoryProperty("type", b.firstOf("string", "number", "integer", "boolean", "array")),
      b.property("format", b.string()),
      b.property("items", ITEMS),
      b.property("collectionFormat", b.firstOf("csv", "ssv", "tsv", "pipes")),
      b.property("default", b.anything()),
      b.property("maximum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMaximum", b.bool()),
      b.property("minimum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMinimum", b.bool()),
      b.property("maxLength", b.integer()),
      b.property("minLength", b.integer()),
      b.property("pattern", b.string()),
      b.property("maxItems", b.integer()),
      b.property("minItems", b.integer()),
      b.property("uniqueItems", b.bool()),
      b.property("enum", b.array(b.anything())),
      b.property("multipleOf", b.firstOf(b.integer(), b.floating())),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildDefinitions(YamlGrammarBuilder b) {
    Object typeFieldValues = b.firstOf("object", "string", "number", "integer", "boolean", "array", "null");
    b.rule(SCHEMA).is(b.object(
      b.property("$ref", b.string()),
      b.property("type", b.firstOf(typeFieldValues, b.array(typeFieldValues))),
      b.property("format", b.string()),
      b.property("title", b.string()),
      b.property("description", DESCRIPTION),
      b.property("default", b.anything()),
      b.property("multipleOf", b.firstOf(b.integer(), b.floating())),
      b.property("maximum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMaximum", b.bool()),
      b.property("minimum", b.firstOf(b.integer(), b.floating())),
      b.property("exclusiveMinimum", b.bool()),
      b.property("maxLength", b.integer()),
      b.property("minLength", b.integer()),
      b.property("pattern", b.string()),
      b.property("maxItems", b.integer()),
      b.property("minItems", b.integer()),
      b.property("uniqueItems", b.bool()),
      b.property("maxProperties", b.integer()),
      b.property("minProperties", b.integer()),
      b.property("required", b.array(b.string())),
      b.property("deprecated", b.bool()),
      b.property("enum", b.array(b.anything())),
      b.property("items", SCHEMA),
      b.property("allOf", b.array(SCHEMA)),
      b.property("properties", b.object(
        b.patternProperty(".*", SCHEMA))),
      b.property("additionalProperties", b.firstOf(b.bool(false), SCHEMA)),
      b.property("discriminator", b.string()),
      b.property("readOnly", b.bool()),
      b.property("xml", XML),
      b.property("externalDocs", EXTERNAL_DOC),
      b.property("example", b.anything()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(XML).is(b.object(
      b.property("name", b.string()),
      b.property("namespace", b.string()),
      b.property("prefix", b.string()),
      b.property("attribute", b.bool()),
      b.property("wrapped", b.bool()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())

    ));

  }

  private static void buildPaths(YamlGrammarBuilder b) {
    b.rule(PATHS).is(b.object(
      b.patternProperty("^/.*", PATH),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(PATH).is(b.object(
      b.property("$ref", b.string()),
      b.property("get", OPERATION),
      b.property("put", OPERATION),
      b.property("post", OPERATION),
      b.property("delete", OPERATION),
      b.property("options", OPERATION),
      b.property("head", OPERATION),
      b.property("patch", OPERATION),
      b.property("parameters", b.array(b.firstOf(REF, PARAMETER))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(OPERATION).is(b.object(
      b.property("tags", b.array(b.string())),
      b.property("summary", b.string()),
      b.property("description", DESCRIPTION),
      b.property("externalDocs", EXTERNAL_DOC),
      b.property("operationId", b.string()),
      b.property("consumes", b.array(b.string())),
      b.property("produces", b.array(b.string())),
      b.property("parameters", b.array(b.firstOf(REF, PARAMETER))),
      b.mandatoryProperty("responses", RESPONSES),
      b.property("schemes", b.array(b.string())),
      b.property("deprecated", b.bool()),
      b.property("security", b.array(SECURITY_REQUIREMENT)),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildInfo(YamlGrammarBuilder b) {
    b.rule(INFO).is(b.object(
      b.mandatoryProperty("title", b.string()),
      b.mandatoryProperty("version", b.string()),
      b.property("description", DESCRIPTION),
      b.property("termsOfService", b.string()),
      b.property("contact", CONTACT),
      b.property("license", LICENSE),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

    b.rule(CONTACT).is(b.object(
      b.property("name", b.string()),
      b.property("url", b.string()),
      b.property("email", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

    b.rule(LICENSE).is(b.object(
      b.mandatoryProperty("name", b.string()),
      b.property("url", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }
}
