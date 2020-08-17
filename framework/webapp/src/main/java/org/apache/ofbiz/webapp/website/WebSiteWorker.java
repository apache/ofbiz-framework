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
package org.apache.ofbiz.webapp.website;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * WebSiteWorker - Worker class for web site related functionality
 */
public final class WebSiteWorker {

    private static final String MODULE = WebSiteWorker.class.getName();

    private WebSiteWorker() { }

    public static String getWebSiteId(ServletRequest request) {
        ServletContext ctx = request.getServletContext();
        return (ctx == null) ? null : ctx.getInitParameter("webSiteId");
    }

    public static GenericValue getWebSite(ServletRequest request) {
        String webSiteId = getWebSiteId(request);
        if (webSiteId == null) {
            return null;
        }

        return findWebSite((Delegator) request.getAttribute("delegator"), webSiteId, true);
    }

    /**
     * returns a WebSite-GenericValue
     * @param delegator
     * @param webSiteId
     * @param useCache
     * @return GenericValue
     */
    public static GenericValue findWebSite(Delegator delegator, String webSiteId, boolean useCache) {
        GenericValue result = null;
        try {
            result = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache(useCache).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError("Error looking up website with id " + webSiteId, MODULE);
        }
        return result;
    }
}
