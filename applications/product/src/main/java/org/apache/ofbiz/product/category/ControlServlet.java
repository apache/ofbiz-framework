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

    private static final String MODULE = ControlServlet.class.getName();

    private static String defaultPage = null;
    private static String pageNotFound = null;
    private static String controlServlet = null;

    public ControlServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = this.getServletContext();
        if (UtilValidate.isEmpty(getDefaultPage())) {
            setDefaultPage(context.getInitParameter("defaultPage"));
        }
        if (UtilValidate.isEmpty(getDefaultPage())) {
            setDefaultPage("/main");
        }
        if (UtilValidate.isEmpty(getPageNotFound())) {
            setPageNotFound(context.getInitParameter("pageNotFound"));
        }
        if (UtilValidate.isEmpty(getPageNotFound())) {
            setPageNotFound("/pagenotfound");
        }

        if (getDefaultPage().startsWith("/") && getDefaultPage().lastIndexOf('/') > 0) {
            setControlServlet(getDefaultPage().substring(1));
            setControlServlet(getControlServlet().substring(0, getControlServlet().indexOf('/')));
        }
    }

    public static String getDefaultPage() {
        return defaultPage;
    }

    public static void setDefaultPage(String defaultPage) {
        ControlServlet.defaultPage = defaultPage;
    }

    public static String getPageNotFound() {
        return pageNotFound;
    }

    public static void setPageNotFound(String pageNotFound) {
        ControlServlet.pageNotFound = pageNotFound;
    }

    public static String getControlServlet() {
        return controlServlet;
    }

    public static void setControlServlet(String controlServlet) {
        ControlServlet.controlServlet = controlServlet;
    }

}
