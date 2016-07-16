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
package org.apache.ofbiz.product.category;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.DefaultServlet;
import org.apache.jasper.servlet.JspServlet;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.ControlServlet;

/**
 * SeoControlServlet.java - SEO Master servlet for the web application.
 */
@SuppressWarnings("serial")
public class SeoControlServlet extends ControlServlet {

    public static final String module = SeoControlServlet.class.getName();

    protected static String defaultPage = null;
    protected static String controlServlet = null;
    
    public static final String REQUEST_IN_ALLOW_LIST = "_REQUEST_IN_ALLOW_LIST_";

    public SeoControlServlet() {
        super();
    }

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = this.getServletContext();
        if (UtilValidate.isEmpty(defaultPage)) {
            defaultPage = context.getInitParameter("defaultPage");
        }
        if (UtilValidate.isEmpty(defaultPage)) {
            defaultPage = "/main";
        }

        if (defaultPage.startsWith("/") && defaultPage.lastIndexOf("/") > 0) {
            controlServlet = defaultPage.substring(1);
            controlServlet = controlServlet.substring(0, controlServlet.indexOf("/"));
        }

        SeoConfigUtil.init();
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String uri = URLEncoder.encode(request.getRequestURI(), "UTF-8");
        if (request.getAttribute(REQUEST_IN_ALLOW_LIST) != null || request.getAttribute("_jsp_" + uri) != null) {
            if (request.getRequestURI().toLowerCase().endsWith(".jsp") || request.getRequestURI().toLowerCase().endsWith(".jspx") ) {
                JspServlet jspServlet = new JspServlet();
                jspServlet.init(this.getServletConfig());
                jspServlet.service(request, response);
            } else {
                DefaultServlet defaultServlet = new DefaultServlet();
                defaultServlet.init(this.getServletConfig());
                defaultServlet.service(request, response);
            }
            return;
        }
        super.doGet(request, response);
    }
}
