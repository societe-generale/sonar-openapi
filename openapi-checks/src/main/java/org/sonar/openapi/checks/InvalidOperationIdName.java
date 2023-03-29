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
package org.sonar.openapi.checks;

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNodeType;
import org.apache.commons.text.CaseUtils;
import org.sonar.check.Rule;
import org.sonar.plugins.openapi.api.OpenApiCheck;
import org.sonar.plugins.openapi.api.v2.OpenApi2Grammar;
import org.sonar.plugins.openapi.api.v3.OpenApi3Grammar;
import org.sonar.sslr.yaml.grammar.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Rule(key = InvalidOperationIdName.CHECK_KEY)
public class InvalidOperationIdName extends OpenApiCheck {
        public static final String CHECK_KEY = "InvalidOperationIdName";

        //tocamelcase needs to have the delimiter used before uppsercase letters, otherwise it will lowercase the letters and we don't want that
        private static final String FIND_BY_ID = "/Find/By/Id";
        private static final String OPERATION_ID = "operationId";
        Map<String, String> operationIds;

        @Override
        public Set<AstNodeType> subscribedKinds() {
            return Sets.newHashSet(OpenApi2Grammar.OPERATION,OpenApi3Grammar.OPERATION);
        }

    @Override
    protected final void visitNode(JsonNode node) {
        JsonNode opIdNode = node.propertyMap().get(OPERATION_ID);
        if (opIdNode != null) {
            String nodeOperationId = opIdNode.value().getToken().getValue();
            String valueFromPathAndOperationType = operationIds.get(nodeOperationId).replaceAll("/\\{(.*?)}", "");
            valueFromPathAndOperationType = CaseUtils.toCamelCase(valueFromPathAndOperationType, false, '/');
            if (!nodeOperationId.equals(valueFromPathAndOperationType)) {
                addIssue(String.format("Found %s: `%s` does not match expected format: `%s`", OPERATION_ID, nodeOperationId, valueFromPathAndOperationType), opIdNode.key());
            }
        }
    }
    @Override
    protected void visitFile(JsonNode root) {
        Map<String, JsonNode> paths = root.at("/paths").propertyMap();

        this.operationIds = extractOperationIdsFromPaths(paths);
    }

    private static Map<String, String> extractOperationIdsFromPaths(Map<String, JsonNode> paths) {
        // key is operationId, value is path + crudType
        Map<String, String> operationIds = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : paths.entrySet()) {
            // get path values /infos
            String path = entry.getKey();
            JsonNode crud = entry.getValue();

            //get CRUD values : GET, PUT, Del
            for (Map.Entry<String, JsonNode> crudEntry : crud.propertyMap().entrySet()) {
                String crudKey = crudEntry.getKey();
                JsonNode operationIdNode = crudEntry.getValue().propertyMap().get(OPERATION_ID);
                if (operationIdNode != null) {
                    String operationId = operationIdNode.getToken().getValue();
                    boolean findById = path.endsWith("}") && crudKey.equals("get") ;
                    crudKey = findById ? FIND_BY_ID: crudKey;
                    //add a `/` because we need a token to be able to capitalize crud operation for case sensitivity
                    operationIds.put(operationId, path + "/" + crudKey);



                }
            }
        }
        return operationIds;
    }
}
