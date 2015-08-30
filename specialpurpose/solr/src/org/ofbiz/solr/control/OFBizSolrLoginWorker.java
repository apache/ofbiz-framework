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
package org.ofbiz.solr.control;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.webapp.control.LoginWorker;

/**
 * OFBiz Solr Login Workers
 */
public class OFBizSolrLoginWorker extends LoginWorker {

    public final static String module = OFBizSolrLoginWorker.class.getName();

    /**
     * An HTTP WebEvent handler that logs in a userLogin. This should run before the security check.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return Return a boolean which specifies whether or not the calling Servlet or
     *         JSP should generate its own content. This allows an event to override the default content.
     */
    public static String login(HttpServletRequest request, HttpServletResponse response) {
    	String result = LoginWorker.login(request, response);
    	if (result.equals("success")) {
            // send the redirect
            try {            
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.setHeader("Location", request.getContextPath());
                response.setHeader("Connection", "close");
            } catch (IllegalStateException ise) {
                Debug.logError(ise.getMessage(), module);
                return "error";
            }
    	}
    	return result;
    }

    public static String extensionCheckLogin(HttpServletRequest request, HttpServletResponse response) {
    	String result = LoginWorker.extensionCheckLogin(request, response);
    	if (result.equals("success")) {
            // send the redirect
            try {            
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.setHeader("Location", request.getContextPath());
                response.setHeader("Connection", "close");
            } catch (IllegalStateException ise) {
                Debug.logError(ise.getMessage(), module);
                return "error";
            }
    	}
        return result;
    }
}
