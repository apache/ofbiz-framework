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
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.webapp.control.LoginWorker;

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
        } else {
            Security security = (Security) request.getAttribute("security");
            if (security == null) {
                security = (Security) session.getAttribute("security");
                if (security != null) {
                    request.setAttribute("security", security);
                }
            }
            if (security == null) {
                security = (Security) request.getServletContext().getAttribute("security");
                if (security != null) {
                    request.setAttribute("security", security);
                }
            }
            if (!LoginWorker.hasBasePermission(userLogin, request)) {
                forwardToLogin = true;
            }
        }
        
        if (forwardToLogin) {
            String contextPath = request.getContextPath();
            String uri = request.getRequestURI();
            if (UtilValidate.isNotEmpty(contextPath) && uri.startsWith(contextPath)) {
                uri = uri.replaceFirst(request.getContextPath(), "");
            }
            String servletPath = request.getServletPath();
            if (UtilValidate.isNotEmpty(servletPath) && uri.startsWith(servletPath)) {
                uri = uri.replaceFirst(servletPath, "");
            }
            response.sendRedirect(contextPath + "/control/checkLogin" + uri);
            return true;
        }
        
        return false;
    }
}
