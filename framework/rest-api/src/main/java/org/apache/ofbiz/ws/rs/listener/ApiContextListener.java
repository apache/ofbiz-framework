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
package org.apache.ofbiz.ws.rs.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.WebAppUtil;

public class ApiContextListener implements ServletContextListener {

    public static final String MODULE = ApiContextListener.class.getName();
    private static ServletContext servletContext;

    /**
     */
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        Delegator delegator = WebAppUtil.getDelegator(servletContext);
        LocalDispatcher dispatcher = WebAppUtil.getDispatcher(servletContext);
        Debug.logInfo("Api Jersey Context initialized, delegator " + delegator + ", dispatcher", MODULE);
        servletContext.setAttribute("delegator", delegator);
        servletContext.setAttribute("dispatcher", dispatcher);
        servletContext.setAttribute("security", WebAppUtil.getSecurity(servletContext));
    }

    /**
     */
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Debug.logInfo("Api Jersey Context destroyed, removing delegator and dispatcher ", MODULE);
        context.removeAttribute("delegator");
        context.removeAttribute("dispatcher");
        context.removeAttribute("security");
        context = null;
    }

    public static ServletContext getApplicationCntx() {
        return servletContext;
    }
}
