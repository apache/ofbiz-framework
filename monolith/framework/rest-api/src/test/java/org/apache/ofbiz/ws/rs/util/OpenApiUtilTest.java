/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.ofbiz.base.model.DomainModel;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.StringSchema;

class OpenApiUtilTest {

    public static class TestModel extends DomainModel {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    @Test
    void domainModelSubclassResolvesToMapSchema() {
        Class<?> result = OpenApiUtil.getOpenApiTypeForAttributeType(TestModel.class.getName());
        assertEquals(MapSchema.class, result);
    }

    @Test
    void classInAliasMapIsNotTreatedAsDomainModel() {
        Class<?> result = OpenApiUtil.getOpenApiTypeForAttributeType("java.sql.Date");
        assertEquals(StringSchema.class, result);
    }

    @Test
    void nullClassNameResolvesToNullWithoutThrowing() {
        Class<?> result = OpenApiUtil.getOpenApiTypeForAttributeType(null);
        assertNull(result);
    }

    @Test
    void nonexistentClassNameResolvesToNullWithoutThrowing() {
        Class<?> result = OpenApiUtil.getOpenApiTypeForAttributeType("non.existing.Class");
        assertNull(result);
    }

    @Test
    void unmappedUnrelatedClassResolvesToNull() {
        Class<?> result = OpenApiUtil.getOpenApiTypeForAttributeType(Object.class.getName());
        assertNull(result);
    }
}
