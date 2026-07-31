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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Builds a generic list response payload containing pagination metadata, list
 * data, and REST-style navigation links.
 */
public final class RestListResponseBuilder {

    private final String collectionName;
    private final Object collectionData;
    private int pageIndex = RestQueryOptions.DEFAULT_PAGE_INDEX;
    private int pageSize = RestQueryOptions.DEFAULT_PAGE_SIZE;
    private long totalCount;
    private String requestPath;

    private RestListResponseBuilder(String collectionName, Object collectionData) {
        if (UtilValidate.isEmpty(collectionName)) {
            throw new IllegalArgumentException("collectionName must not be empty");
        }
        this.collectionName = collectionName;
        this.collectionData = collectionData;
    }

    /**
     * Creates a builder for a named collection payload.
     *
     * @param collectionName the JSON property name for the collection payload
     * @param collectionData the collection payload value
     * @return a new response builder
     */
    public static RestListResponseBuilder forList(String collectionName, Object collectionData) {
        return new RestListResponseBuilder(collectionName, collectionData);
    }

    /**
     * Sets the zero-based page index.
     *
     * @param pageIndex the current page index
     * @return this builder
     */
    public RestListResponseBuilder pageIndex(int pageIndex) {
        this.pageIndex = validatePageIndex(pageIndex);
        return this;
    }

    /**
     * Sets the page size.
     *
     * @param pageSize the current page size
     * @return this builder
     */
    public RestListResponseBuilder pageSize(int pageSize) {
        this.pageSize = validatePageSize(pageSize);
        return this;
    }

    /**
     * Sets the total number of matching records.
     *
     * @param totalCount the total matching record count
     * @return this builder
     */
    public RestListResponseBuilder totalCount(long totalCount) {
        this.totalCount = Math.max(0, totalCount);
        return this;
    }

    /**
     * Sets the originating request path used to construct pagination links.
     *
     * @param requestPath the request path, optionally including a query string
     * @return this builder
     */
    public RestListResponseBuilder requestPath(String requestPath) {
        this.requestPath = requestPath;
        return this;
    }

    /**
     * Builds the collection response payload.
     *
     * @return a map containing pagination metadata, collection data, and links
     */
    public Map<String, Object> build() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pageIndex", pageIndex);
        response.put("pageSize", pageSize);
        response.put("totalCount", totalCount);
        response.put("totalPages", getTotalPages());
        response.put("hasNext", hasNext());
        response.put("previousPageCount", getPreviousPageCount());
        response.put("nextPageCount", getNextPageCount());
        response.put(collectionName, collectionData);

        Map<String, Object> links = buildLinks();
        if (UtilValidate.isNotEmpty(links)) {
            response.put("links", links);
        }
        return response;
    }

    private boolean hasNext() {
        return totalCount > ((long) pageIndex + 1L) * pageSize;
    }

    private long getTotalPages() {
        if (totalCount <= 0) {
            return 0L;
        }
        return (totalCount / pageSize) + ((totalCount % pageSize == 0) ? 0L : 1L);
    }

    private long getPreviousPageCount() {
        return Math.max(0L, pageIndex);
    }

    private long getNextPageCount() {
        return Math.max(getTotalPages() - pageIndex - 1L, 0L);
    }

    private Map<String, Object> buildLinks() {
        Map<String, Object> links = new LinkedHashMap<>();
        if (UtilValidate.isEmpty(requestPath)) {
            return links;
        }

        String basePath = getBasePath(requestPath);
        if (UtilValidate.isEmpty(basePath)) {
            return links;
        }

        long totalPages = getTotalPages();
        links.put("self", RestApiUtil.makeLinkMap(buildPageHref(basePath, pageIndex), UtilMisc.toMap("rel", "self")));
        if (totalPages > 0) {
            links.put("first", RestApiUtil.makeLinkMap(buildPageHref(basePath, 0), UtilMisc.toMap("rel", "first")));
            if (pageIndex > 0) {
                links.put("prev", RestApiUtil.makeLinkMap(buildPageHref(basePath, pageIndex - 1), UtilMisc.toMap("rel", "prev")));
            }
            Integer nextPageIndex = getNextPageIndex();
            if (hasNext() && nextPageIndex != null) {
                links.put("next", RestApiUtil.makeLinkMap(buildPageHref(basePath, nextPageIndex), UtilMisc.toMap("rel", "next")));
            }
            Integer lastPageIndex = getLastPageIndex();
            if (lastPageIndex != null) {
                links.put("last", RestApiUtil.makeLinkMap(buildPageHref(basePath, lastPageIndex), UtilMisc.toMap("rel", "last")));
            }
        }
        return links;
    }

    private Integer getLastPageIndex() {
        long lastPageIndex = getTotalPages() - 1L;
        if (lastPageIndex > Integer.MAX_VALUE) {
            return null;
        }
        return (int) lastPageIndex;
    }

    private Integer getNextPageIndex() {
        if (pageIndex == Integer.MAX_VALUE) {
            return null;
        }
        return pageIndex + 1;
    }

    private String buildPageHref(String basePath, int targetPageIndex) {
        List<RestApiUtil.QueryParameter> queryParameters = new ArrayList<>(RestApiUtil.extractQueryParameters(requestPath));
        queryParameters.removeIf(parameter ->
                RestApiUtil.isReservedParameter(parameter.getName(), Set.of("pageIndex", "VIEW_INDEX", "pageSize", "VIEW_SIZE")));
        queryParameters.add(new RestApiUtil.QueryParameter("pageIndex", Integer.toString(targetPageIndex)));
        queryParameters.add(new RestApiUtil.QueryParameter("pageSize", Integer.toString(pageSize)));

        return new StringBuilder(basePath).append('?').append(RestApiUtil.encodeQueryParameters(queryParameters)).toString();
    }

    private static String getBasePath(String path) {
        int querySeparatorIndex = path.indexOf('?');
        String basePath = querySeparatorIndex >= 0 ? path.substring(0, querySeparatorIndex) : path;
        return UtilValidate.isEmpty(basePath) ? null : basePath;
    }

    private static int validatePageIndex(int pageIndex) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex must be greater than or equal to 0");
        }
        return pageIndex;
    }

    private static int validatePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > RestQueryOptions.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
        return pageSize;
    }
}
