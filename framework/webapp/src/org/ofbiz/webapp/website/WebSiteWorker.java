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
package org.ofbiz.webapp.website;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * WebSiteWorker - Worker class for web site related functionality
 */
public class WebSiteWorker {

    public static final String module = WebSiteWorker.class.getName();

    public static String getWebSiteId(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession();
        Map<String, Object> requestParameters = UtilHttp.getParameterMap((HttpServletRequest) request);
        String webSiteId = null;
        boolean fromSession = false;

        // first see if a new webSiteId was specified as a parameter
        webSiteId = (String) requestParameters.get("webSiteId");
        // if no parameter, try from session
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = (String) session.getAttribute("webSiteId");
            if (webSiteId != null) fromSession = true;
        }
        // get it from the servlet context
        if (UtilValidate.isEmpty(webSiteId)) {
            ServletContext application = ((ServletContext) request.getAttribute("servletContext"));
            if (application != null) webSiteId = application.getInitParameter("webSiteId");
        }

        if (UtilValidate.isNotEmpty(webSiteId) && !fromSession) {
            session.setAttribute("webSiteId", webSiteId);
        }

        return webSiteId;
    }

    public static GenericValue getWebSite(ServletRequest request) {
        String webSiteId = getWebSiteId(request);
        if (webSiteId == null) {
            return null;
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        try {
            return delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up website with id " + webSiteId, module);
        }
        return null;
    }
}
