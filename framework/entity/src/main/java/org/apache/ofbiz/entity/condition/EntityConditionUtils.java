/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.entity.condition;

import java.util.List;

import org.apache.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;

/**
 * Auxiliary methods used by condition expressions.
 */
final class EntityConditionUtils {

    /**
     * Calls {@link ModelEntity#getField(String)} if the entity model is not null.
     *
     * @param modelEntity the entity model to query
     * @param fieldName the name of the field to get from {@code ModelEntity}
     * @return the field corresponding to {@code fieldName} in {@code ModelEntity}
     */
    static ModelField getField(ModelEntity modelEntity, String fieldName) {
        return (modelEntity == null) ? null : modelEntity.getField(fieldName);
    }

    /**
     * Calls {@link SqlJdbcUtil#addValue(StringBuilder, ModelField, Object, List)}
     * if the condition parameters are not null.
     *
     * @param buffer the buffer that will receive the SQL dump
     * @param field the field to dump
     * @param value the value to dump
     * @param params the condition parameters
     */
    static void addValue(StringBuilder buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        SqlJdbcUtil.addValue(buffer, params == null ? null : field, value, params);
    }
}
