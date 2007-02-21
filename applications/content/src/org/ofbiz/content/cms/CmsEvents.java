/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.content.cms;

import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * CmsEvents
 */
public class CmsEvents {

    public static final String module = CmsEvents.class.getName();

    public static String cms(HttpServletRequest request, HttpServletResponse response) {       
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();

        String webSiteId = (String) session.getAttribute("webSiteId");
        if (webSiteId == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Not able to run CMS application; no webSiteId defined for WebApp!");
            return "error";
        }

        // is this a default request or called from a defined request mapping
        String targetRequest = (String) request.getAttribute("targetRequestUri");
        String actualRequest = (String) request.getAttribute("thisRequestUri");
        if (targetRequest != null) {
            targetRequest = targetRequest.replaceAll("\\W", "");
        } else {
            targetRequest = "";
        }
        if (actualRequest != null) {
            actualRequest = actualRequest.replaceAll("\\W", "");
        } else {
            actualRequest = "";
        }

        String pathInfo = request.getPathInfo();
        if (targetRequest.equals(actualRequest)) {
            // was called directly -- path info is everything after the request
            String[] pathParsed = pathInfo.split("/", 3);
            if (pathParsed != null && pathParsed.length > 2) {
                pathInfo = "/" + pathParsed[2];
            } else {
                pathInfo = null;
            }
        } // if called through the default request, there is no request in pathinfo

        // check for path alias first
        if (pathInfo != null) {
            Debug.log("Path INFO for Alias: " + pathInfo, module);
            
            GenericValue pathAlias = null;
            try {
                pathAlias = delegator.findByPrimaryKeyCache("WebSitePathAlias", UtilMisc.toMap("webSiteId", webSiteId, "pathAlias", pathInfo));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (pathAlias != null) {
                String alias = pathAlias.getString("aliasTo");
                if (UtilValidate.isNotEmpty(alias)) {
                    if (!alias.startsWith("/")) {
                       alias = "/" + alias;
                    }

                    RequestDispatcher rd = request.getRequestDispatcher(request.getServletPath() + alias);
                    try {
                        rd.forward(request, response);
                    } catch (ServletException e) {
                        Debug.logError(e, module);
                        return "error";
                    } catch (IOException e) {
                        Debug.logError(e, module);
                        return "error";
                    }

                    return null; // null to not process any views
                }
            }
        }

        // process through CMS
        // TODO: implement me!

        // throw an unknown request error
        throw new RuntimeException("Unknown request; this request does not exist or cannot be called directly.");
    }
}
