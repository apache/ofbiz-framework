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
package org.apache.ofbiz.widget.content;

import org.apache.ofbiz.webapp.taglib.ContentUrlTag;

import javax.servlet.http.HttpServletRequest;

/**
 * Generates URL strings for addressing static content based properties configured on an HttpRequest's website or
 * configured properties in url.properties.
 *
 * @see ContentUrlTag
 */
public class StaticContentUrlProvider {
    // HttpServletRequest used to find the URL of the current website.
    private final HttpServletRequest request;

    // Cached copy of the URL prefix to use for static content.
    private String prefix;

    /**
     * Create a new URL provider for given HttpServletRequest's website.
     *
     * @param request The HttpServletRequest request to look up the website for.
     */
    public StaticContentUrlProvider(final HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Given a path to a static content resource, return the URL string that a client can use to retrieve that resource.
     *
     * @param resourcePath Path to static resource
     * @return String representation of the URL which can be used to retrieve the static resource.
     */
    public String pathAsContentUrlString(final String resourcePath) {
        return getPrefix() + resourcePath;
    }

    private String getPrefix() {
        if (prefix == null) {
            this.prefix = ContentUrlTag.getContentPrefix(this.request);
        }
        return prefix;
    }
}
