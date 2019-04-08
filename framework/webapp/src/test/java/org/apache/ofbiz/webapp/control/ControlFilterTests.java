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
package org.apache.ofbiz.webapp.control;

import org.junit.Test;

import javax.servlet.FilterConfig;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ControlFilterTests {
    @Test
    public void initRetrievesAllInitParameters() throws Exception {
        FilterConfig config = mock(FilterConfig.class);
        ControlFilter cf = new ControlFilter();
        cf.init(config);
        verify(config).getInitParameter("redirectPath");
        verify(config).getInitParameter("forceRedirectAll");
        verify(config).getInitParameter("errorCode");
        verify(config).getInitParameter("allowedPaths");
        verifyNoMoreInteractions(config);
    }

    @Test
    public void initSetsProperErrorCode() throws Exception {
        FilterConfig config = mock(FilterConfig.class);
        ControlFilter cf = new ControlFilter();
        // if no errorCode parameter is specified then the default error code is 403
        cf.init(config);
        assertEquals(cf.errorCode, 403);
        // if the errorCode parameter is empty then the default error code is 403
        when(config.getInitParameter("errorCode")).thenReturn("");
        cf.init(config);
        assertEquals(cf.errorCode, 403); // default error code is 403
        // if an invalid errorCode parameter is specified then the default error code is 403
        when(config.getInitParameter("errorCode")).thenReturn("NaN");
        cf.init(config);
        assertEquals(cf.errorCode, 403);
        // if the errorCode parameter is specified then it is set in the filter
        when(config.getInitParameter("errorCode")).thenReturn("404");
        cf.init(config);
        assertEquals(cf.errorCode, 404);
    }
}
