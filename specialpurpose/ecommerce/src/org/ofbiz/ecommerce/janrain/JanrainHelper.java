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
package org.ofbiz.ecommerce.janrain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.webapp.control.LoginWorker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Java Helper Class for Janrain Engage
 */
public class JanrainHelper {

    public static final String module = JanrainHelper.class.getName();
    private static String apiKey = UtilProperties.getPropertyValue("ecommerce", "janrain.apiKey");
    private static String baseUrl = UtilProperties.getPropertyValue("ecommerce", "janrain.baseUrl");
    public JanrainHelper(String apiKey, String baseUrl) {
        while (baseUrl.endsWith("/"))
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public static Element authInfo(String token) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("token", token);
        return apiCall("auth_info", query);
    }
    public HashMap<String, List<String>> allMappings() {
        Element rsp = apiCall("all_mappings", null);
        Element mappings_node = (Element)rsp.getFirstChild();
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        NodeList mappings = getNodeList("/rsp/mappings/mapping", rsp);
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element)mappings.item(i);
            List<String> identifiers = new ArrayList<String>();
            NodeList rk_list = getNodeList("primaryKey", mapping);
            NodeList id_list = getNodeList("identifiers/identifier", mapping);
            String remote_key = ((Element)rk_list.item(0)).getTextContent();
            for (int j = 0; j < id_list.getLength(); j++) {
                Element ident = (Element) id_list.item(j);
                identifiers.add(ident.getTextContent());
            }
            result.put(remote_key, identifiers);
        }
        return result;
    }
    private NodeList getNodeList(String xpath_expr, Element root) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            return (NodeList) xpath.evaluate(xpath_expr, root, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }
    }
    public List<String> mappings(Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("primaryKey", primaryKey);
        Element rsp = apiCall("mappings", query);
        Element oids = (Element)rsp.getFirstChild();
        List<String> result = new ArrayList<String>();
        NodeList nl = oids.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element)nl.item(i);
            result.add(e.getTextContent());
        }
        return result;
    }
    public void map(String identifier, Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("identifier", identifier);
        query.put("primaryKey", primaryKey);
        apiCall("map", query);
    }
    public void unmap(String identifier, Object primaryKey) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("identifier", identifier);
        query.put("primaryKey", primaryKey);
        apiCall("unmap", query);
    }
    private static Element apiCall(String methodName, Map<String, Object> partialQuery) {
        Map<String, Object> query = null;
        if (partialQuery == null) {
            query = new HashMap<String, Object>();
        } else {
            query = new HashMap<String, Object>(partialQuery);
        }
        query.put("format", "xml");
        query.put("apiKey", apiKey);
        StringBuffer sb = new StringBuffer();
        for (Iterator<Map.Entry<String, Object>> it = query.entrySet().iterator(); it.hasNext();) {
            if (sb.length() > 0)
                sb.append('&');
            try {
                Map.Entry<String, Object> e = it.next();
                sb.append(URLEncoder.encode(e.getKey().toString(), "UTF-8"));
                sb.append('=');
                sb.append(URLEncoder.encode(e.getValue().toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unexpected encoding error", e);
            }
        }
        String data = sb.toString();
        try {
            URL url = new URL(baseUrl + "/api/v2/" + methodName);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.connect();
            OutputStreamWriter osw = new OutputStreamWriter(
                conn.getOutputStream(), "UTF-8");
            osw.write(data);
            osw.close();
            
            BufferedReader post = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            StringBuilder buf = new StringBuilder();
            while ((line = post.readLine()) != null) {
                 buf.append(line);
            }
            post.close();
            Document tagXml = UtilXml.readXmlDocument(buf.toString());
            Element response = tagXml.getDocumentElement();
            if (!response.getAttribute("stat").equals("ok")) {
                throw new RuntimeException("Unexpected API error");
            }
            return response;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected URL error", e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO error", e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unexpected XML error", e);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected XML error", e);
        }
    }

    public static String janrainCheckLogin(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String token =  request.getParameter("token");
        String errMsg = "";
        if (UtilValidate.isNotEmpty(token)) {
            JanrainHelper janrainHelper = new JanrainHelper(apiKey, baseUrl);
            Element authInfo = janrainHelper.authInfo(token);
            Element profileElement = UtilXml.firstChildElement(authInfo, "profile");
            Element nameElement = UtilXml.firstChildElement(profileElement, "name");
            
            // profile element
            String displayName = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "displayName"));
            String email = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "email"));
            String identifier = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "identifier"));
            String preferredUsername = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "preferredUsername"));
            String providerName = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "providerName"));
            String url = UtilXml.elementValue(UtilXml.firstChildElement(profileElement, "url"));
            
            // name element
            String givenName = UtilXml.elementValue(UtilXml.firstChildElement(nameElement, "givenName"));
            String familyName = UtilXml.elementValue(UtilXml.firstChildElement(nameElement, "familyName"));
            String formatted = UtilXml.elementValue(UtilXml.firstChildElement(nameElement, "formatted"));
            
            if (UtilValidate.isEmpty("preferredUsername")) {
                errMsg = UtilProperties.getMessage("SecurityextUiLabels", "loginevents.username_not_found_reenter", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            Map<String, String> result = new HashMap<String, String>();
            result.put("displayName", displayName);
            result.put("email", email);
            result.put("identifier", identifier);
            result.put("preferredUsername", preferredUsername);
            result.put("providerName", providerName);
            result.put("url", url);
            result.put("givenName", givenName);
            result.put("familyName", familyName);
            result.put("formatted", formatted);
            request.setAttribute("userInfoMap", result);
            
            try {
                GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", preferredUsername).cache().queryOne();
                if (UtilValidate.isNotEmpty(userLogin)) {
                    LoginWorker.doBasicLogin(userLogin, request);
                    LoginWorker.autoLoginSet(request, response);
                    return "success";
                } else {
                    return "userLoginMissing";
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding the userLogin for distributed cache clear", module);
            }
        }
        return "success";
    }
}
