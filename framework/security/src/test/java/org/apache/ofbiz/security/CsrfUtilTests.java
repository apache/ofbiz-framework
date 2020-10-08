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
package org.apache.ofbiz.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CsrfUtilTests {

    @Test
    public void testGetTokenMap() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        // prepare the token map to be retrieved from session
        Map<String, String> tokenMap = new LinkedHashMap<>();
        tokenMap.put("uri_1", "abcd");
        when(session.getAttribute("CSRF-Token")).thenReturn(tokenMap);

        // without userLogin in session, test token map is retrieved from session
        Map<String, String> resultMap = CsrfUtil.getTokenMap(request, "");
        assertEquals("abcd", resultMap.get("uri_1"));

        // add userLogin to session
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.get("partyId")).thenReturn("10000");
        when(userLogin.getString("partyId")).thenReturn("10000");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);

        // with userLogin in session, test token map is not retrieved from session
        resultMap = CsrfUtil.getTokenMap(request, "/partymgr");
        assertNull(resultMap.get("uri_1"));

    }

    @Test
    public void testGetRequestUriWithSubFolderLimit() {
        CsrfUtil.setStrategy(new CsrfDefenseStrategy());

        // limit only when request uri starts with 'entity'
        String limitRequestUri = CsrfUtil.getRequestUriWithSubFolderLimit("entity/find/Budget/0002");
        assertEquals("entity/find/Budget", limitRequestUri);

        limitRequestUri = CsrfUtil.getRequestUriWithSubFolderLimit("a/b/c/d");
        assertEquals("a/b/c/d", limitRequestUri);
    }

    @Test
    public void testGetRequestUriFromPath() {
        String requestUri = CsrfUtil.getRequestUriFromPath("/viewprofile?partyId=Company");
        assertEquals("viewprofile", requestUri);

        requestUri = CsrfUtil.getRequestUriFromPath("/partymgr/control/viewprofile");
        assertEquals("viewprofile", requestUri);

        requestUri = CsrfUtil.getRequestUriFromPath("view/entityref_main#org.apache.ofbiz.accounting.budget");
        assertEquals("view/entityref_main", requestUri);
    }

    @Test
    public void testGenerateTokenForNonAjax() throws ParserConfigurationException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        // add userLogin to session
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.get("partyId")).thenReturn("10000");
        when(userLogin.getString("partyId")).thenReturn("10000");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);

        String token = CsrfUtil.generateTokenForNonAjax(request, "");
        assertEquals("", token);

        token = CsrfUtil.generateTokenForNonAjax(request, "javascript:");
        assertEquals("", token);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();
        Element requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "checkLogin");
        ConfigXMLReader.RequestMap requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);

        requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "entity/find/{entityName}/{pkValues: .*}");
        requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);

        when(request.getAttribute("requestMapMap")).thenReturn(requestMapMap);

        token = CsrfUtil.generateTokenForNonAjax(request, "checkLogin");
        assertNotEquals("", token);

        CsrfUtil.setStrategy(new CsrfDefenseStrategy());

        token = CsrfUtil.generateTokenForNonAjax(request, "entity/find/Budget/0001");
        assertNotEquals("", token);

        String token2 = CsrfUtil.generateTokenForNonAjax(request, "entity/find&#x2f;Budget/0001");
        // test support for treating "&#x2f;" as "/"
        assertEquals(token2, token);

        token2 = CsrfUtil.generateTokenForNonAjax(request, "entity/find/Budget/0002");
        // token only generated for up to 3 subfolders in the path
        assertEquals(token2, token);
    }

    @Test
    public void testFindRequestMapWithoutControlPath() throws ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.newDocument();

        Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();
        Element requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "checkLogin");
        ConfigXMLReader.RequestMap requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);
        // REST request like /entity/find/AccommodationClass
        requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "entity/find/{entityName}");
        requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);

        // View override like /view/ModelInduceFromDb
        requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "view");
        requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);

        requestMapElement = doc.createElement("request-map");
        requestMapElement.setAttribute("uri", "ModelInduceFromDb");
        requestMap = new ConfigXMLReader.RequestMap(requestMapElement);
        requestMapMap.put(requestMap.getUri(), requestMap);

        // test usual request
        requestMap = CsrfUtil.findRequestMap(requestMapMap, "/checkLogin");
        assertEquals(requestMap.getUri(), "checkLogin");

        // test usual request
        requestMap = CsrfUtil.findRequestMap(requestMapMap, "checkLogin");
        assertEquals(requestMap.getUri(), "checkLogin");

        // test REST request
        requestMap = CsrfUtil.findRequestMap(requestMapMap, "/entity/find/AccommodationClass");
        assertEquals(requestMap.getUri(), "entity/find/{entityName}");

        // test view orderride
        requestMap = CsrfUtil.findRequestMap(requestMapMap, "/view/ModelInduceFromDb");
        assertEquals(requestMap.getUri(), "view");

    }

    @Test
    public void testGenerateTokenForAjax() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("X-CSRF-Token")).thenReturn("abcd");

        String token = CsrfUtil.generateTokenForAjax(request);
        assertEquals("abcd", token);
    }

    @Test
    public void testGetTokenForAjax() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("X-CSRF-Token")).thenReturn("abcd");

        String token = CsrfUtil.getTokenForAjax(session);
        assertEquals("abcd", token);
    }

    @Test
    public void testAddOrUpdateTokenInUrl() {
        CsrfUtil.setTokenNameNonAjax("csrfToken");

        // test link without csrfToken
        String url = CsrfUtil.addOrUpdateTokenInUrl("https://localhost:8443/catalog/control/login", "abcd");
        assertEquals("https://localhost:8443/catalog/control/login?csrfToken=abcd", url);

        // test link with query string and without csrfToken
        url = CsrfUtil.addOrUpdateTokenInUrl(
                "https://localhost:8443/partymgr/control/visitdetail?visitId=10301", "abcd");
        assertEquals(
                "https://localhost:8443/partymgr/control/visitdetail?visitId=10301&csrfToken=abcd",
                url);

        // test link with csrfToken
        url = CsrfUtil.addOrUpdateTokenInUrl("https://localhost:8443/catalog/control/login?csrfToken=abcd", "efgh");
        assertEquals("https://localhost:8443/catalog/control/login?csrfToken=efgh", url);

        // test link with csrfToken amd empty csrfToken replacement
        url = CsrfUtil.addOrUpdateTokenInUrl("https://localhost:8443/catalog/control/login?csrfToken=abcd", "");
        assertEquals("https://localhost:8443/catalog/control/login?csrfToken=", url);
    }

    @Test
    public void testAddOrUpdateTokenInQueryString() {
        CsrfUtil.setTokenNameNonAjax("csrfToken");

        String queryString = CsrfUtil.addOrUpdateTokenInQueryString("", "abcd");
        assertEquals(queryString, "csrfToken=abcd");

        queryString = CsrfUtil.addOrUpdateTokenInQueryString("csrfToken=abcd&a=b", "efgh");
        assertEquals(queryString, "csrfToken=efgh&a=b");

        queryString = CsrfUtil.addOrUpdateTokenInQueryString("csrfToken=abcd&a=b", "");
        assertEquals(queryString, "csrfToken=&a=b");

        queryString = CsrfUtil.addOrUpdateTokenInQueryString("a=b", "abcd");
        assertEquals(queryString, "a=b&csrfToken=abcd");

        queryString = CsrfUtil.addOrUpdateTokenInQueryString("a=b", "");
        assertEquals(queryString, "a=b");
    }
}
