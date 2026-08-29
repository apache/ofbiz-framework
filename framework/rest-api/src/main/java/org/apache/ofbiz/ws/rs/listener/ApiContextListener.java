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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.webapp.WebAppUtil;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Servlet context listener that initializes and destroys the OFBiz
 * {@link Delegator} and {@link LocalDispatcher} instances for the REST API
 * web application.
 *
 * <p>On startup, the delegator and dispatcher are retrieved using the
 * {@code entityDelegatorName} and {@code localDispatcherName} init parameters
 * from {@code web.xml} and stored as servlet context attributes, making them
 * available to all JAX-RS resources via {@code @Context ServletContext}.</p>
 *
 * <p>On shutdown, both attributes are removed from the servlet context.</p>
 */
public class ApiContextListener implements ServletContextListener {

    public static final String MODULE = ApiContextListener.class.getName();
    // TODO: remove after the refactoring of OFBizOpenApiReader
    private static ServletContext servletContext = null;

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the {@link Delegator} and {@link LocalDispatcher} using the
     * {@code entityDelegatorName} and {@code localDispatcherName} init parameters
     * from {@code web.xml} and stores them as servlet context attributes for use
     * by JAX-RS resources.</p>
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        Delegator delegator = DelegatorFactory.getDelegator(servletContext.getInitParameter("entityDelegatorName"));
        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(servletContext.getInitParameter("localDispatcherName"), delegator);
        Debug.logInfo("Api Jersey Context initialized, delegator " + delegator + ", dispatcher", MODULE);
        servletContext.setAttribute("delegator", delegator);
        servletContext.setAttribute("dispatcher", dispatcher);
        servletContext.setAttribute("security", WebAppUtil.getSecurity(servletContext));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes the {@code delegator} and {@code dispatcher} attributes
     * from the servlet context.</p>
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Debug.logInfo("Api Jersey Context destroyed, removing delegator and dispatcher ", MODULE);
        context.removeAttribute("delegator");
        context.removeAttribute("dispatcher");
        context.removeAttribute("security");
        context = null;
    }


    /**
     * Returns the servlet context stored at initialization time.
     *
     * @return the application {@link ServletContext}, or {@code null} if the
     *         context has not yet been initialized
     * @deprecated Temporary workaround to provide servlet context access to
     *             {@code OFBizOpenApiReader}; will be removed once that class
     *             is refactored
     */
    // TODO: remove after the refactoring of OFBizOpenApiReader
    @Deprecated
    public static ServletContext getApplicationCntx() {
        return servletContext;
    }
}
