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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Normalizes REST list query parameters into reusable paging, sorting, and
 * filter options for framework-level handlers.
 */
public final class RestQueryOptions {

    public static final int DEFAULT_PAGE_INDEX = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> RESERVED_PARAMETERS = Set.of(
            "pageIndex", "VIEW_INDEX", "pageSize", "VIEW_SIZE", "sort", "orderBy");

    private final int pageIndex;
    private final int pageSize;
    private final String sort;
    private final Map<String, Object> filters;

    private RestQueryOptions(int pageIndex, int pageSize, String sort, Map<String, Object> filters) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.sort = sort;
        this.filters = Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }

    /**
     * Creates a normalized collection query from request parameters.
     *
     * @param parameters raw request parameters
     * @return a normalized query object with paging, sorting, and filters
     * @throws IllegalArgumentException when paging values are invalid
     */
    public static RestQueryOptions fromParameters(Map<String, ?> parameters) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (UtilValidate.isNotEmpty(parameters)) {
            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                if (RestApiUtil.isReservedParameter(entry.getKey(), RESERVED_PARAMETERS)
                        || UtilValidate.isEmpty(entry.getValue())) {
                    continue;
                }
                filters.put(entry.getKey(), entry.getValue());
            }
        }

        Object pageIndexParam = RestApiUtil.getParameterValueIgnoreCase(parameters, "pageIndex", "VIEW_INDEX");
        Object pageSizeParam = RestApiUtil.getParameterValueIgnoreCase(parameters, "pageSize", "VIEW_SIZE");
        Integer pageIndexValue = UtilMisc.toIntegerObject(pageIndexParam);
        Integer pageSizeValue = UtilMisc.toIntegerObject(pageSizeParam);
        if (UtilValidate.isNotEmpty(pageIndexParam) && pageIndexValue == null) {
            throw new IllegalArgumentException("pageIndex must be a valid integer");
        }
        if (UtilValidate.isNotEmpty(pageSizeParam) && pageSizeValue == null) {
            throw new IllegalArgumentException("pageSize must be a valid integer");
        }
        int pageIndex = pageIndexValue != null ? pageIndexValue : DEFAULT_PAGE_INDEX;
        int pageSize = pageSizeValue != null ? pageSizeValue : DEFAULT_PAGE_SIZE;
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be greater than or equal to 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }

        String sort = null;
        Object sortValue = RestApiUtil.getParameterValueIgnoreCase(parameters, "sort", "orderBy");
        if (UtilValidate.isNotEmpty(sortValue)) {
            sort = sortValue.toString().trim();
            if (UtilValidate.isEmpty(sort)) {
                sort = null;
            }
        }

        return new RestQueryOptions(pageIndex, pageSize, sort, filters);
    }

    /**
     * Returns the zero-based page index.
     *
     * @return the current page index
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * Returns the requested page size.
     *
     * @return the page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns the normalized sort expression, when present.
     *
     * @return the sort expression, or {@code null} when not requested
     */
    public String getSort() {
        return sort;
    }

    /**
     * Returns the remaining non-reserved request parameters as filters in
     * insertion order.
     *
     * @return immutable filter parameters
     */
    public Map<String, Object> getFilters() {
        return filters;
    }
}
