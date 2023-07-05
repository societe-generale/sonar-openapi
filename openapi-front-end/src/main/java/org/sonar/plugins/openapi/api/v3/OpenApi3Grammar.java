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
package org.sonar.plugins.openapi.api.v3;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.yaml.grammar.YamlGrammarBuilder;

@java.lang.SuppressWarnings("squid:S1192") // Voluntarily ignoring string constants redefinitions in this file
public enum OpenApi3Grammar implements GrammarRuleKey {
  ROOT,
  INFO,
  PATHS,
  COMPONENTS,
  PARAMETER,
  RESPONSE,
  SECURITY_SCHEME,
  SECURITY_REQUIREMENT,
  TAG,
  REF,
  EXTERNAL_DOC,
  CONTACT,
  LICENSE,

  PATH,
  OPERATION,
  LINK,
  CALLBACK,
  RESPONSES,
  REQUEST_BODY,

  SCHEMA,
  DISCRIMINATOR,
  HEADER,
  EXAMPLE,
  XML,
  SERVER,
  SERVER_VARIABLE,
  HTTP_SECURITY_SCHEME,
  API_KEY_SECURITY_SCHEME,
  OAUTH2_SECURITY_SCHEME,
  OPENID_SECURITY_SCHEME,
  MEDIA_TYPE,
  ENCODING,
  FLOWS,
  IMPLICIT_FLOW,
  PASSWORD_FLOW,
  CREDENTIALS_FLOW,
  AUTH_FLOW,
  SCHEMAS_COMPONENT,
  RESPONSES_COMPONENT,
  PARAMETERS_COMPONENT,
  EXAMPLES_COMPONENT,
  BODIES_COMPONENT,
  HEADERS_COMPONENT,
  SECURITY_SCHEMES,
  LINKS_COMPONENT,
  CALLBACKS_COMPONENT,
  SCHEMA_PROPERTIES,
  DESCRIPTION;

  private static final String EXTENSION_PATTERN = "^x-.*";

  public static YamlGrammarBuilder create() {
    YamlGrammarBuilder b = new YamlGrammarBuilder();
    b.setRootRule(ROOT);

    b.rule(ROOT).is(b.object(
      b.mandatoryProperty("openapi", b.firstOf("3.0.0", "3.0.1", "3.0.2", "3.0.3")),
      b.mandatoryProperty("info", INFO),
      b.property("servers", b.array(SERVER)),
      b.mandatoryProperty("paths", PATHS),
      b.property("components", COMPONENTS),
      b.property("security", b.array(SECURITY_REQUIREMENT)),
      b.property("tags", b.array(TAG)),
      b.property("externalDocs", EXTERNAL_DOC),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(REF).is(b.object(
      b.mandatoryProperty("$ref", b.string())));
    b.rule(EXTERNAL_DOC).is(b.object(
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("url", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

    b.rule(DESCRIPTION).is(b.string()).skip();
    buildInfo(b);
    buildServer(b);
    buildPaths(b);
    buildComponents(b);
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
    b.rule(SECURITY_SCHEME).is(
      b.firstOf(HTTP_SECURITY_SCHEME, API_KEY_SECURITY_SCHEME, OAUTH2_SECURITY_SCHEME, OPENID_SECURITY_SCHEME));
    b.rule(HTTP_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "http"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("scheme", b.string()),
      b.property("bearerFormat", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(API_KEY_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "apiKey"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("name", b.string()),
      b.mandatoryProperty("in", b.firstOf("query", "header", "cookie")),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(OAUTH2_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "oauth2"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("flows", FLOWS),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(OPENID_SECURITY_SCHEME).is(b.object(
      b.discriminant("type", "openIdConnect"),
      b.property("description", DESCRIPTION),
      b.mandatoryProperty("openIdConnectUrl", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything()))).skip();
    b.rule(FLOWS).is(b.object(
      b.property("implicit", IMPLICIT_FLOW),
      b.property("password", PASSWORD_FLOW),
      b.property("clientCredentials", CREDENTIALS_FLOW),
      b.property("authorizationCode", AUTH_FLOW),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(IMPLICIT_FLOW).is(b.object(
      b.mandatoryProperty("authorizationUrl", b.string()),
      b.property("refreshUrl", b.string()),
      b.mandatoryProperty("scopes", b.object(
        b.patternProperty(".*", b.string()))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(PASSWORD_FLOW).is(b.object(
      b.mandatoryProperty("tokenUrl", b.string()),
      b.property("refreshUrl", b.string()),
      b.mandatoryProperty("scopes", b.object(
        b.patternProperty(".*", b.string()))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(CREDENTIALS_FLOW).is(b.object(
      b.mandatoryProperty("tokenUrl", b.string()),
      b.property("refreshUrl", b.string()),
      b.mandatoryProperty("scopes", b.object(
        b.patternProperty(".*", b.string()))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(AUTH_FLOW).is(b.object(
      b.mandatoryProperty("authorizationUrl", b.string()),
      b.mandatoryProperty("tokenUrl", b.string()),
      b.property("refreshUrl", b.string()),
      b.mandatoryProperty("scopes", b.object(
        b.patternProperty(".*", b.string()))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(SECURITY_REQUIREMENT).is(b.object(
      b.patternProperty(".*", b.array(b.string()))));
  }

  private static void buildCallbacks(YamlGrammarBuilder b) {
    b.rule(CALLBACK).is(b.object(
      b.patternProperty("^[^x].*", PATH),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(LINK).is(b.object(
      b.property("operationRef", b.string()),
      b.property("operationId", b.string()),
      b.property("parameters", b.object(
        b.patternProperty(".*", b.anything()))),
      b.property("requestBody", b.anything()),
      b.property("description", DESCRIPTION),
      b.property("server", SERVER),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildResponses(YamlGrammarBuilder b) {
    b.rule(RESPONSES).is(b.object(
      b.property("default", b.firstOf(RESPONSE, REF)),
      b.patternProperty("^[0-9xX]+", b.firstOf(RESPONSE, REF)),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(RESPONSE).is(b.object(
      b.mandatoryProperty("description", DESCRIPTION),
      b.property("headers", b.object(
      b.patternProperty(".*", b.firstOf(REF, HEADER)))),
      b.property("content", b.object(
        b.patternProperty(".*", MEDIA_TYPE))),
      b.property("links", b.object(
        b.patternProperty(".*", b.firstOf(REF, LINK)))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(HEADER).is(b.object(
      b.property("description", DESCRIPTION),
      b.property("required", b.bool()),
      b.property("deprecated", b.bool()),
      b.property("allowEmptyValue", b.bool()),

      b.property("style", "simple"),
      b.property("explode", b.bool()),
      b.property("allowReserved", b.bool()),
      b.property("schema", b.firstOf(REF, SCHEMA)),
      b.property("example", b.anything()),
      b.property("examples", b.object(
        b.patternProperty(".*", b.firstOf(REF, EXAMPLE)))),
      b.property("content", b.object(
        b.patternProperty(".*", MEDIA_TYPE))),

      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(EXAMPLE).is(b.object(
      b.property("summary", b.string()),
      b.property("description", DESCRIPTION),
      b.property("value", b.anything()),
      b.property("externalValue", b.string()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildParameters(YamlGrammarBuilder b) {
    b.rule(PARAMETER).is(b.object(
      b.mandatoryProperty("name", b.string()),
      b.mandatoryProperty("in", b.firstOf("path", "query", "header", "cookie")),
      b.property("description", DESCRIPTION),
      b.property("required", b.bool()),
      b.property("deprecated", b.bool()),
      b.property("defaultValue", b.string()),
      b.property("allowEmptyValue", b.bool()),

      b.property("style", b.firstOf("matrix", "label", "form", "simple", "spaceDelimited", "pipeDelimited", "deepObject")),
      b.property("explode", b.bool()),
      b.property("allowReserved", b.bool()),
      b.property("schema", b.firstOf(REF, SCHEMA)),
      b.property("example", b.anything()),
      b.property("examples", b.object(
        b.patternProperty(".*", b.firstOf(REF, EXAMPLE)))),
      b.property("content", b.object(
        b.patternProperty(".*", MEDIA_TYPE))),

      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(REQUEST_BODY).is(b.object(
      b.property("description", DESCRIPTION),
      b.property("required", b.bool()),
      b.property("content", b.object(
        b.patternProperty(".*", b.firstOf(REF, MEDIA_TYPE)))),

      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(MEDIA_TYPE).is(b.object(
      b.property("schema", b.firstOf(REF, SCHEMA)),
      b.property("example", b.anything()),
      b.property("examples", b.object(
        b.patternProperty(".*", b.firstOf(REF, EXAMPLE)))),
      b.property("encoding", b.object(
        b.patternProperty(".*", ENCODING))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(ENCODING).is(b.object(
      b.property("contentType", b.string()),
      b.property("headers", b.object(
        b.patternProperty(".*", b.firstOf(REF, HEADER)))),
      b.property("style", b.firstOf("matrix", "label", "form", "simple", "spaceDelimited", "pipeDelimited", "deepObject")),
      b.property("explode", b.bool()),
      b.property("allowReserved", b.bool()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildComponents(YamlGrammarBuilder b) {
    b.rule(COMPONENTS).is(b.object(
      b.property("schemas", SCHEMAS_COMPONENT),
      b.property("responses", RESPONSES_COMPONENT),
      b.property("parameters", PARAMETERS_COMPONENT),
      b.property("examples", EXAMPLES_COMPONENT),
      b.property("requestBodies", BODIES_COMPONENT),
      b.property("headers", HEADERS_COMPONENT),
      b.property("securitySchemes", SECURITY_SCHEMES),
      b.property("links", LINKS_COMPONENT),
      b.property("callbacks", CALLBACKS_COMPONENT),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(SCHEMAS_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, SCHEMA))));
    b.rule(RESPONSES_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, RESPONSE))));
    b.rule(PARAMETERS_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, PARAMETER))));
    b.rule(EXAMPLES_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, EXAMPLE))));
    b.rule(BODIES_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, REQUEST_BODY))));
    b.rule(HEADERS_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, HEADER))));
    b.rule(SECURITY_SCHEMES).is(b.object(b.patternProperty(".*", b.firstOf(REF, SECURITY_SCHEME))));
    b.rule(LINKS_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, LINK))));
    b.rule(CALLBACKS_COMPONENT).is(b.object(b.patternProperty(".*", b.firstOf(REF, CALLBACK))));

    buildParameters(b);
    buildResponses(b);
    buildSchema(b);
    buildCallbacks(b);
  }

  private static void buildSchema(YamlGrammarBuilder b) {
    b.rule(SCHEMA).is(b.object(
      b.property("title", b.string()),
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
      b.property("enum", b.array(b.anything())),
      b.property("type", b.firstOf("object", "string", "number", "integer", "boolean", "array", "null")),
      b.property("allOf", b.array(b.firstOf(REF, SCHEMA))),
      b.property("oneOf", b.array(b.firstOf(REF, SCHEMA))),
      b.property("anyOf", b.array(b.firstOf(REF, SCHEMA))),
      b.property("not", b.firstOf(REF, SCHEMA)),
      b.property("items", b.firstOf(REF, SCHEMA)),
      b.property("properties", SCHEMA_PROPERTIES),
      b.property("additionalProperties", b.firstOf(b.bool(), REF, SCHEMA)),
      b.property("description", DESCRIPTION),
      b.property("format", b.string()),
      b.property("default", b.anything()),
      b.property("nullable", b.bool()),
      b.property("discriminator", DISCRIMINATOR),
      b.property("readOnly", b.bool()),
      b.property("writeOnly", b.bool()),
      b.property("xml", XML),
      b.property("externalDocs", EXTERNAL_DOC),
      b.property("example", b.anything()),
      b.property("deprecated", b.bool()),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(SCHEMA_PROPERTIES).is(b.object(b.patternProperty(".*", b.firstOf(REF, SCHEMA))));
    b.rule(DISCRIMINATOR).is(b.object(
      b.property("propertyName", b.string()),
      b.property("mapping", b.object(
        b.patternProperty(".*", b.string())))));
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
      b.property("summary", b.string()),
      b.property("description", DESCRIPTION),
      b.property("get", OPERATION),
      b.property("put", OPERATION),
      b.property("post", OPERATION),
      b.property("delete", OPERATION),
      b.property("options", OPERATION),
      b.property("head", OPERATION),
      b.property("patch", OPERATION),
      b.property("trace", OPERATION),
      b.property("servers", b.array(SERVER)),
      b.property("parameters", b.array(b.firstOf(REF, PARAMETER))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
    b.rule(OPERATION).is(b.object(
      b.property("tags", b.array(b.string())),
      b.property("summary", b.string()),
      b.property("description", DESCRIPTION),
      b.property("externalDocs", EXTERNAL_DOC),
      b.property("operationId", b.string()),
      b.property("parameters", b.array(b.firstOf(REF, PARAMETER))),
      b.property("requestBody", b.firstOf(REF, REQUEST_BODY)),
      b.mandatoryProperty("responses", RESPONSES),
      b.property("callbacks", b.object(
        b.patternProperty(".*", b.firstOf(REF, CALLBACK)))),
      b.property("deprecated", b.bool()),
      b.property("security", b.array(SECURITY_REQUIREMENT)),
      b.property("servers", b.array(SERVER)),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));
  }

  private static void buildServer(YamlGrammarBuilder b) {
    b.rule(SERVER).is(b.object(
      b.mandatoryProperty("url", b.string()),
      b.property("description", DESCRIPTION),
      b.property("variables", b.object(
        b.patternProperty(".*", SERVER_VARIABLE))),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

    b.rule(SERVER_VARIABLE).is(b.object(
      b.property("enum", b.array(b.string())),
      b.mandatoryProperty("default", b.string()),
      b.property("description", DESCRIPTION),
      b.patternProperty(EXTENSION_PATTERN, b.anything())));

  }

  private static void buildInfo(YamlGrammarBuilder b) {
    b.rule(INFO).is(b.object(
      b.mandatoryProperty("title", b.string()),
      b.property("description", DESCRIPTION),
      b.property("termsOfService", b.string()),
      b.property("contact", CONTACT),
      b.property("license", LICENSE),
      b.mandatoryProperty("version", b.string()),
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
