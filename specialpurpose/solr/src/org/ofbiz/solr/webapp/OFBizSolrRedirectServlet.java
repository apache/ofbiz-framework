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
package org.ofbiz.solr.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.solr.servlet.RedirectServlet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.OfbizUrlBuilder;
import org.ofbiz.webapp.control.WebAppConfigurationException;

/**
 * OFBizSolrRedirectServlet.java - Master servlet for the ofbiz-solr application.
 */
@SuppressWarnings("serial")
public class OFBizSolrRedirectServlet extends RedirectServlet {

    public static final String module = OFBizSolrRedirectServlet.class.getName();
    
    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	boolean isForwarded = forwardUrl(request, response);
    	if (isForwarded) {
    		return;
    	}
    	
    	super.doGet(request, response);
    }

	protected static boolean forwardUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        boolean forwardToLogin = false;
        if (UtilValidate.isEmpty(userLogin)) {
        	forwardToLogin = true;
        }
		
    	// check request schema
    	if (forwardToLogin || !request.getScheme().equals("https")) {
            StringBuilder newURL = new StringBuilder(250);
            // Build the scheme and host part
            try {
                OfbizUrlBuilder builder = OfbizUrlBuilder.from(request);
                builder.buildHostPart(newURL, "", true);
            } catch (GenericEntityException e) {
                // If the entity engine is throwing exceptions, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while getting web site properties: ", module);
                return false;
            } catch (WebAppConfigurationException e) {
                // If we can't read the controller.xml file, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                return false;
            } catch (IOException e) {
                // If we can't write to StringBuilder, then there is no point in continuing.
                Debug.logError(e, "Exception thrown while writing to StringBuilder: ", module);
                return false;
            }
            if (forwardToLogin) {
            	String contextPath = request.getContextPath();
            	if (UtilValidate.isNotEmpty(contextPath)) {
            		newURL.append(contextPath);
            	}
            	newURL.append("/control/checkLogin");
            	String uri = request.getRequestURI();
            	if (UtilValidate.isNotEmpty(contextPath) && uri.startsWith(contextPath)) {
            		uri = uri.replaceFirst(request.getContextPath(), "");
            	}
            	String servletPath = request.getServletPath();
            	if (UtilValidate.isNotEmpty(servletPath) && uri.startsWith(servletPath)) {
            		uri = uri.replaceFirst(servletPath, "");
            	}
                newURL.append(uri);
            } else {
                newURL.append(request.getRequestURI());
            }

            // send the redirect
            try {            
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.setHeader("Location", newURL.toString());
                response.setHeader("Connection", "close");
            } catch (IllegalStateException ise) {
                throw new IOException(ise.getMessage(), ise);
            }
    		return true;
    	}
		return false;
	}
}
