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

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.collections.MultivaluedMapContext;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ViewMap;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class RequestHandlerTests {
    public static class ResolveURITests {
        private MultivaluedMapContext<String,RequestMap> reqMaps;
        private Map<String, ViewMap> viewMaps;
        private HttpServletRequest req;
        private Element dummyElement;
        private RequestHandler.ControllerConfig ccfg;

        @Before
        public void setUp() {
            ccfg = mock(RequestHandler.ControllerConfig.class);
            reqMaps = new MultivaluedMapContext<>();
            viewMaps = new HashMap<>();
            when(ccfg.getDefaultRequest()).thenReturn(null);
            when(ccfg.getRequestMapMap()).thenReturn(reqMaps);
            when(ccfg.getViewMapMap()).thenReturn(viewMaps);
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
            assertThat(RequestHandler.resolveURI(ccfg, req),
                    both(hasItem(foo)).and(not(hasItem(bar))));
            assertThat(RequestHandler.resolveURI(ccfg, req).size(), is(1));
        }

        @Test
        public void resolveURIBasicPut() throws RequestHandlerException {
            when(dummyElement.getAttribute("method")).thenReturn("put");
            when(req.getPathInfo()).thenReturn("/foo");
            when(req.getMethod()).thenReturn("put");

            RequestMap foo = new RequestMap(dummyElement);

            assertTrue(RequestHandler.resolveURI(ccfg, req).isEmpty());
            reqMaps.putSingle("foo", foo);
            assertFalse(RequestHandler.resolveURI(ccfg, req).isEmpty());
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
            assertThat(RequestHandler.resolveURI(ccfg, req), hasItem(foo));

            when(req.getPathInfo()).thenReturn("/bar");
            when(req.getMethod()).thenReturn("PUT");
            assertThat(RequestHandler.resolveURI(ccfg, req), hasItem(bar));
        }

        @Test
        public void resolveURIDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/baz");
            when(ccfg.getDefaultRequest()).thenReturn("bar");
            assertThat(RequestHandler.resolveURI(ccfg, req), hasItem(bar));
        }

        @Test
        public void resolveURIBasicOverrideView() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            viewMaps.put("baz", new ViewMap(dummyElement));

            when(req.getPathInfo()).thenReturn("/foo/baz");
            when(ccfg.getDefaultRequest()).thenReturn("bar");
            assertThat(RequestHandler.resolveURI(ccfg, req), hasItem(foo));
        }

        @Test
        public void resolveURIMissingOverrideView() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/foo/baz");
            when(ccfg.getDefaultRequest()).thenReturn("bar");
            assertThat(RequestHandler.resolveURI(ccfg, req), hasItem(bar));
        }

        @Test
        public void resolveURINoDefault() throws Exception {
            RequestMap foo = new RequestMap(dummyElement);
            RequestMap bar = new RequestMap(dummyElement);
            reqMaps.putSingle("foo", foo);
            reqMaps.putSingle("bar", bar);

            when(req.getPathInfo()).thenReturn("/baz");
            assertTrue(RequestHandler.resolveURI(ccfg, req).isEmpty());
        }
    }

    public static class ResolveMethodTests {
        private Element dummyElement;
        private Collection<RequestMap> rmaps;

        @Before
        public void setUp() {
            dummyElement = mock(Element.class);
            rmaps = new ArrayList<>();
        }

        @Test
        public void resolveMethodBasic() throws RequestHandlerException {
            RequestMap fooPut = new RequestMap(dummyElement);
            fooPut.method = "put";
            rmaps.add(fooPut);

            RequestMap fooAll = new RequestMap(dummyElement);
            fooAll.method = "all";
            rmaps.add(fooAll);

            assertThat(RequestHandler.resolveMethod("put", rmaps).get(), is(fooPut));
            assertThat(RequestHandler.resolveMethod("get", rmaps).get(), is(fooAll));
        }

        @Test
        public void resolveMethodCatchAll() throws RequestHandlerException {
            assertFalse(RequestHandler.resolveMethod("get", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("post", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("put", rmaps).isPresent());
            assertFalse(RequestHandler.resolveMethod("delete", rmaps).isPresent());

            RequestMap foo = new RequestMap(dummyElement);
            foo.method = "all";
            rmaps.add(foo);
            assertTrue(RequestHandler.resolveMethod("get", rmaps).isPresent());
            assertTrue(RequestHandler.resolveMethod("post", rmaps).isPresent());
            assertTrue(RequestHandler.resolveMethod("put", rmaps).isPresent());
            assertTrue(RequestHandler.resolveMethod("delete", rmaps).isPresent());
        }
    }

    public static class checkCertificatesTests {
        private HttpServletRequest req;

        @Before
        public void setUp() {
            req = mock(HttpServletRequest.class);
            when(req.getAttribute(anyString())).thenReturn(null);
        }

        @Test
        // Check that the verification fails when the request does not contain any certificate.
        public void checkCertificatesFailure() {
            assertFalse(RequestHandler.checkCertificates(req, x -> true));
        }

        @Test
        // Check that certificates with 2.2 spec are handled correctly.
        public void checkCertificates22() {
            when(req.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(new X509Certificate[] {});
            assertTrue(RequestHandler.checkCertificates(req, x -> true));
            assertFalse(RequestHandler.checkCertificates(req, x -> false));
        }

        @Test
        // Check that certificates with 2.1 spec are handled correctly.
        public void checkCertificates21() {
            when(req.getAttribute("javax.net.ssl.peer_certificates")).thenReturn(new X509Certificate[] {});
            assertTrue(RequestHandler.checkCertificates(req, x -> true));
            assertFalse(RequestHandler.checkCertificates(req, x -> false));
        }

        @Test
        // Check that certificates in an invalid attribute are ignored.
        public void checkCertificatesUnrecognized() {
            when(req.getAttribute("NOT_RECOGNIZED")).thenReturn(new X509Certificate[] {});
            assertFalse(RequestHandler.checkCertificates(req, x -> true));
        }
    }
}
