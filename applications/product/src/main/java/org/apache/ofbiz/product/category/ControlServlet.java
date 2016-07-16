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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.ofbiz.base.util.UtilValidate;

/**
 * ControlServlet.java - Master servlet for the web application.
 */
@SuppressWarnings("serial")
public class ControlServlet extends org.apache.ofbiz.webapp.control.ControlServlet {

    public static final String module = ControlServlet.class.getName();

    protected static String defaultPage = null;
    protected static String pageNotFound = null;
    protected static String controlServlet = null;

    public ControlServlet() {
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
        if (UtilValidate.isEmpty(pageNotFound)) {
            pageNotFound = context.getInitParameter("pageNotFound");
        }
        if (UtilValidate.isEmpty(pageNotFound)) {
            pageNotFound = "/pagenotfound";
        }

        if (defaultPage.startsWith("/") && defaultPage.lastIndexOf("/") > 0) {
            controlServlet = defaultPage.substring(1);
            controlServlet = controlServlet.substring(0, controlServlet.indexOf("/"));
        }
    }
}
