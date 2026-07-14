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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

class ApiContextListenerTest {

    @Test
    void testContextInitializedWiresDelegatorAndDispatcherUnderCorrectKeys() {
        ServletContext servletContext = mock(ServletContext.class);
        ServletContextEvent event = mock(ServletContextEvent.class);
        when(event.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameter("entityDelegatorName")).thenReturn("default");
        when(servletContext.getInitParameter("localDispatcherName")).thenReturn("api-dispatcher");

        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);

        try (MockedStatic<DelegatorFactory> delegatorFactory = mockStatic(DelegatorFactory.class);
                MockedStatic<ServiceContainer> serviceContainer = mockStatic(ServiceContainer.class)) {
            delegatorFactory.when(() -> DelegatorFactory.getDelegator("default")).thenReturn(delegator);
            serviceContainer.when(() -> ServiceContainer.getLocalDispatcher("api-dispatcher", delegator)).thenReturn(dispatcher);

            new ApiContextListener().contextInitialized(event);

            verify(servletContext).setAttribute("delegator", delegator);
            verify(servletContext).setAttribute("dispatcher", dispatcher);
        }
    }

    @Test
    void testContextDestroyedRemovesDelegatorAndDispatcherAttributes() {
        ServletContext servletContext = mock(ServletContext.class);
        ServletContextEvent event = mock(ServletContextEvent.class);
        when(event.getServletContext()).thenReturn(servletContext);

        new ApiContextListener().contextDestroyed(event);

        verify(servletContext).removeAttribute("delegator");
        verify(servletContext).removeAttribute("dispatcher");
    }
}
