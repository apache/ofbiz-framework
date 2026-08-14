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
package org.apache.ofbiz.webapp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

/** Covers the JSON-request-body attribute merge that fed the reported
 * anonymous mainDecoratorLocation override (login-page widget-injection RCE):
 * a request body must never be able to shadow a name the webapp already
 * exposes as a trusted, application-owned ServletContext attribute. */
public class WebAppUtilTests {

    private static ServletInputStream inputStreamOf(String content) {
        ByteArrayInputStream bytes = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bytes.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() {
                return bytes.read();
            }
        };
    }

    @Test
    public void doesNotOverrideAnExistingServletContextAttribute() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(request.getServletContext()).thenReturn(servletContext);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getInputStream()).thenReturn(
                inputStreamOf("{\"mainDecoratorLocation\":\"file:/dev/fd/292\"}"));
        when(servletContext.getAttribute("mainDecoratorLocation"))
                .thenReturn("component://order/widget/ordermgr/CommonScreens.xml");

        WebAppUtil.setAttributesFromRequestBody(request);

        verify(request, never()).setAttribute("mainDecoratorLocation", "file:/dev/fd/292");
    }

    @Test
    public void stillSetsAttributesThatDoNotShadowContextConfig() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(request.getServletContext()).thenReturn(servletContext);
        when(request.getContentType()).thenReturn("application/json");
        when(request.getInputStream()).thenReturn(
                inputStreamOf("{\"searchString\":\"widgets\"}"));
        when(servletContext.getAttribute("searchString")).thenReturn(null);

        WebAppUtil.setAttributesFromRequestBody(request);

        verify(request).setAttribute("searchString", "widgets");
    }
}
