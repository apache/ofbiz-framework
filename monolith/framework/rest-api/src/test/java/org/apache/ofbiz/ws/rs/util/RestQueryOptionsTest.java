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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.ofbiz.base.util.UtilMisc;
import org.junit.jupiter.api.Test;

public final class RestQueryOptionsTest {

    @Test
    public void normalizesPagingAndCompatibilityAliases() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "VIEW_INDEX", "1",
                "VIEW_SIZE", "25",
                "sort", "-fieldOne"));

        assertEquals(1, query.getPageIndex());
        assertEquals(25, query.getPageSize());
        assertEquals("-fieldOne", query.getSort());
    }

    @Test
    public void rejectsInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageSize", "0")));
    }

    @Test
    public void rejectsMalformedPagingValues() {
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageIndex", "abc")));
        assertThrows(IllegalArgumentException.class, () ->
                RestQueryOptions.fromParameters(UtilMisc.toMap("pageSize", "xyz")));
    }

    @Test
    public void usesDefaultPagingWhenMissing() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap("filterOne", "valueOne"));

        assertEquals(RestQueryOptions.DEFAULT_PAGE_INDEX, query.getPageIndex());
        assertEquals(RestQueryOptions.DEFAULT_PAGE_SIZE, query.getPageSize());
        assertEquals("valueOne", query.getFilters().get("filterOne"));
    }

    @Test
    public void preservesFilterParameters() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "filterOne", "valueOne",
                "filterTwo", "valueTwo",
                "pageIndex", "0",
                "pageSize", "20"));

        assertEquals("valueOne", query.getFilters().get("filterOne"));
        assertEquals("valueTwo", query.getFilters().get("filterTwo"));
    }

    @Test
    public void normalizesPagingParametersDirectly() {
        RestQueryOptions query = RestQueryOptions.fromParameters(UtilMisc.toMap(
                "pageIndex", "2",
                "pageSize", "30",
                "sort", "fieldOne"));

        assertEquals(2, query.getPageIndex());
        assertEquals(30, query.getPageSize());
        assertEquals("fieldOne", query.getSort());
    }
}
