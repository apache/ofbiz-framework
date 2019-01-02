/*
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
 */
package org.apache.ofbiz.webapp.control;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

public class ControlFilterTests {

    private FilterConfig config;
    private ControlFilter filter;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private FilterChain next;
    private HttpSession session;

    @Before
    public void setUp() {
        config = mock(FilterConfig.class);
        when(config.getInitParameter(anyString())).thenReturn(null);
        session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenReturn(null);
        req = mock(HttpServletRequest.class);
        when(req.getSession()).thenReturn(session);
        when(req.getContextPath()).thenReturn("");
        resp = mock(HttpServletResponse.class);
        next = mock(FilterChain.class);
        filter = new ControlFilter();
     }

    @Test
    public void filterWithExactAllowedPath() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/foo");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo:/bar");
        when(req.getRequestURI()).thenReturn("/servlet/bar");
        when(req.getContextPath()).thenReturn("/servlet");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(next).doFilter(req, resp);
    }

    @Test
    public void filterWithAllowedSubPath() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/foo");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo:/bar");
        when(req.getRequestURI()).thenReturn("/servlet/bar/baz");
        when(req.getContextPath()).thenReturn("/servlet");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(next).doFilter(req, resp);
    }

    @Test
    public void filterWithRedirection() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/foo");
        when(config.getInitParameter("allowedPaths")).thenReturn("/bar:/baz");
        when(req.getRequestURI()).thenReturn("/missing/path");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendRedirect("/foo");
    }

    @Test
    public void filterWithURIredirection() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("http://example.org/foo");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo:/bar");
        when(req.getRequestURI()).thenReturn("/baz");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendRedirect("http://example.org/foo");
    }

    @Test
    public void bailsOutWithVariousErrorCodes() throws Exception {
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo");
        when(req.getRequestURI()).thenReturn("/baz");

        // if no errorCode parameter is specified then the default error code is 403
        when(config.getInitParameter("errorCode")).thenReturn(null);
        filter.init(config);
        filter.doFilter(req, resp, next);

        // if the errorCode parameter is empty then the default error code is 403
        when(config.getInitParameter("errorCode")).thenReturn("");
        filter.init(config);
        filter.doFilter(req, resp, next);

        // if an invalid errorCode parameter is specified then the default error code is 403
        when(config.getInitParameter("errorCode")).thenReturn("NaN");
        filter.init(config);
        filter.doFilter(req, resp, next);

        verify(resp, times(3)).sendError(403, "/baz");

        // if the errorCode parameter is specified then it is set in the filter
        when(config.getInitParameter("errorCode")).thenReturn("404");
        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendError(404, "/baz");
    }

    @Test
    public void redirectAllAllowed() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/bar");
        when(config.getInitParameter("forceRedirectAll")).thenReturn("Y");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo");
        when(req.getRequestURI()).thenReturn("/foo");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendRedirect("/bar");
    }

    @Test
    public void redirectAllNotAllowed() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/bar");
        when(config.getInitParameter("forceRedirectAll")).thenReturn("Y");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo");
        when(req.getRequestURI()).thenReturn("/baz");

        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendRedirect("/bar");
    }

    @Test
    public void redirectAllRecursive() throws Exception {
        when(config.getInitParameter("redirectPath")).thenReturn("/foo");
        when(config.getInitParameter("forceRedirectAll")).thenReturn("Y");
        when(config.getInitParameter("allowedPaths")).thenReturn("/foo");
        when(req.getRequestURI()).thenReturn("/foo");

        // Initial Call
        when(session.getAttribute("_FORCE_REDIRECT_")).thenReturn(null);
        filter.init(config);
        filter.doFilter(req, resp, next);
        verify(resp).sendRedirect("/foo");
        verify(session).setAttribute("_FORCE_REDIRECT_", "true");

        // Recursive Call
        when(session.getAttribute("_FORCE_REDIRECT_")).thenReturn("true");
        filter.doFilter(req, resp, next);
        verify(next).doFilter(req, resp);
        verify(session).removeAttribute("_FORCE_REDIRECT_");
    }
}
