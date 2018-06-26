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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class RequestHandlerTests {
    public static class ResolveURITests {
        private MultivaluedMap<String,RequestMap> reqMaps;
        private HttpServletRequest req;
        private Element dummyElement;

        @Before
        public void setUp() {
            reqMaps = new MultivaluedHashMap<>();
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
            assertSame(foo, RequestHandler.resolveURI(reqMaps, req, null).get());
        }

        @Test
        public void resolveURIBasicPut() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("put");
            when(req.getPathInfo()).thenReturn("/foo");
            when(req.getMethod()).thenReturn("put");

            RequestMap foo = new RequestMap(dummyElement);

            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            reqMaps.putSingle("foo", foo);
            assertTrue(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
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
            assertSame(foo, RequestHandler.resolveURI(reqMaps, req, null).get());

            when(req.getPathInfo()).thenReturn("/bar");
            when(req.getMethod()).thenReturn("PUT");
            assertSame(bar, RequestHandler.resolveURI(reqMaps, req, null).get());
        }

        @Test
        public void resolveURICatchAll() throws RequestHandlerException {
            when(req.getPathInfo()).thenReturn("/foo");
            RequestMap foo = new RequestMap(dummyElement);
            when(req.getMethod()).thenReturn("get");
            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("post");
            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("put");
            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("delete");
            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());

            reqMaps.putSingle("foo", foo);
            when(req.getMethod()).thenReturn("get");
            assertTrue(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("post");
            assertTrue(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("put");
            assertTrue(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
            when(req.getMethod()).thenReturn("delete");
            assertTrue(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
        }

        @Test
        public void resolveURISegregate() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("put");
            RequestMap fooPut = new RequestMap(dummyElement);
            when(dummyElement.getAttribute("method")).thenReturn("all");
            RequestMap fooAll = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", fooAll);
            reqMaps.add("foo", fooPut);

            when(req.getPathInfo()).thenReturn("/foo");
            when(req.getMethod()).thenReturn("put");
            assertSame(fooPut, RequestHandler.resolveURI(reqMaps, req, null).get());
            when(req.getMethod()).thenReturn("get");
            assertSame(fooAll, RequestHandler.resolveURI(reqMaps, req, null).get());
        }

        @Test
        public void resolveURIDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/bar");
            when(req.getMethod()).thenReturn("get");
            assertSame(bar, RequestHandler.resolveURI(reqMaps, req, "bar").get());
        }

        @Test
        public void resolveURINoDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/baz");
            when(req.getMethod()).thenReturn("get");
            assertFalse(RequestHandler.resolveURI(reqMaps, req, null).isPresent());
        }
    }
}
