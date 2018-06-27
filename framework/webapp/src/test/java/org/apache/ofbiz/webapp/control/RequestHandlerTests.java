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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ViewMap;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class RequestHandlerTests {
    public static class ResolveURITests {
        private MultivaluedMap<String,RequestMap> reqMaps;
        private Map<String, ViewMap> viewMaps;
        private HttpServletRequest req;
        private Element dummyElement;
        private RequestHandler.Controller ctrl;

        @Before
        public void setUp() {
            ctrl = mock(RequestHandler.Controller.class);
            reqMaps = new MultivaluedHashMap<>();
            viewMaps = new HashMap<>();
            when(ctrl.getDefaultRequest()).thenReturn(null);
            when(ctrl.getRequestMapMap()).thenReturn(reqMaps);
            when(ctrl.getViewMapMap()).thenReturn(viewMaps);
            req = mock(HttpServletRequest.class);
            dummyElement = mock(Element.class);
            when(dummyElement.getAttribute("method")).thenReturn("all");
            when(req.getMethod()).thenReturn("get");
        }

        @Test
        public void resolveURIBasic() throws RequestHandlerException {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);
            when(req.getPathInfo()).thenReturn("/foo");
            assertThat(RequestHandler.resolveURI(ctrl, req),
                    both(hasItem(foo)).and(not(hasItem(bar))));
            assertThat(RequestHandler.resolveURI(ctrl, req).size(), is(1));
        }

        @Test
        public void resolveURIBasicPut() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("put");
            when(req.getPathInfo()).thenReturn("/foo");
            when(req.getMethod()).thenReturn("put");

            RequestMap foo = new RequestMap(dummyElement);

            assertTrue(RequestHandler.resolveURI(ctrl, req).isEmpty());
            reqMaps.putSingle("foo", foo);
            assertFalse(RequestHandler.resolveURI(ctrl, req).isEmpty());
        }

        @Test
        public void resolveURIUpperCase() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("get");
            RequestMap foo = new RequestMap(dummyElement);
            when(dummyElement.getAttribute("method")).thenReturn("put");
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/foo");
            when(req.getMethod()).thenReturn("GET");
            assertThat(RequestHandler.resolveURI(ctrl, req), hasItem(foo));

            when(req.getPathInfo()).thenReturn("/bar");
            when(req.getMethod()).thenReturn("PUT");
            assertThat(RequestHandler.resolveURI(ctrl, req), hasItem(bar));
        }


        @Test
        public void resolveURIDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/baz");
            when(ctrl.getDefaultRequest()).thenReturn("bar");
            assertThat(RequestHandler.resolveURI(ctrl, req), hasItem(bar));
        }

        @Test
        public void resolveURIOverrideView() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            viewMaps.put("baz", new ViewMap(dummyElement));

            when(req.getPathInfo()).thenReturn("/foo/baz");
            when(ctrl.getDefaultRequest()).thenReturn("bar");
            assertThat(RequestHandler.resolveURI(ctrl, req), hasItem(bar));
        }

        @Test
        public void resolveURINoDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/baz");
            assertTrue(RequestHandler.resolveURI(ctrl, req).isEmpty());
        }

        @Test
        public void resolveMethodCatchAll() throws RequestHandlerException {
            when(req.getPathInfo()).thenReturn("/foo");
            RequestMap foo = new RequestMap(dummyElement);
            Collection<RequestMap> rmaps = RequestHandler.resolveURI(ctrl, req);
            assertFalse(RequestHandler.resolveMethod("get", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("post", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("put", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("delete", rmaps).isPresent());

            reqMaps.putSingle("foo", foo);
            Collection<RequestMap> rmaps2 = RequestHandler.resolveURI(ctrl, req);
            assertTrue(RequestHandler.resolveMethod("get", rmaps2).isPresent());
            assertTrue(RequestHandler.resolveMethod("post", rmaps2).isPresent());
            assertTrue(RequestHandler.resolveMethod("put", rmaps2).isPresent());
            assertTrue(RequestHandler.resolveMethod("delete", rmaps2).isPresent());
        }

        @Test
        public void resolveMethodBasic() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("put");
            RequestMap fooPut = new RequestMap(dummyElement);
            when(dummyElement.getAttribute("method")).thenReturn("all");
            RequestMap fooAll = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", fooAll);
            reqMaps.add("foo", fooPut);

            when(req.getPathInfo()).thenReturn("/foo");
            Collection<RequestMap> rmaps = RequestHandler.resolveURI(ctrl, req);
            assertThat(rmaps, hasItems(fooPut, fooAll));
            assertThat(RequestHandler.resolveMethod("put", rmaps).get(), is(fooPut));
            assertThat(RequestHandler.resolveMethod("get", rmaps).get(), is(fooAll));
        }
    }
}
