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

package org.ofbiz.testtools.seleniumxml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jdom.Element;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;


public class RemoteRequest {

    public static final String module = RemoteRequest.class.getName();

    /**
     * The default parameters.
     */
    private static HttpParams defaultParameters = null;

    /**
     * The scheme registry.
     */
    private static SchemeRegistry supportedSchemes;
    final private static String JsonHandleMode = "JSON_HANDLE";
    final private static String HttpHandleMode = "HTTP_HANDLE";

    private SeleniumXml parent;
    private SeleniumXml currentTest;
    private List <Element>children;
    private Map <String, Object> inMap;
    private Map <String, String> outMap;
    private String requestUrl;
    private String host;
    private String responseHandlerMode;

    //Login-As parameters
    private String loginAsUrl;
    private String loginAsUserParam;
    private String loginAsPasswordParam;

    static {

        supportedSchemes = new SchemeRegistry();

        // Register the "http" protocol scheme, it is required
        // by the default operator to look up socket factories.
        SchemeSocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", 80, sf));

        // prepare parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        defaultParameters = params;

    }
    public RemoteRequest(SeleniumXml parent, List<Element> children, List<Element> loginAs, String requestUrl, String hostString, String responseHandlerMode) {

        this(parent, children, requestUrl, hostString, responseHandlerMode);
        if (UtilValidate.isNotEmpty(loginAs)) {
            Element elem = loginAs.get(0);

            this.loginAsUserParam = elem.getAttributeValue("username-param");
            this.loginAsPasswordParam = elem.getAttributeValue("password-param");
            this.loginAsUrl = elem.getAttributeValue("url");

        }
    }

    public RemoteRequest(SeleniumXml parent, List<Element> children, String requestUrl, String hostString, String responseHandlerMode) {
        super();
        this.parent = parent;
        this.requestUrl = requestUrl;
        this.host = hostString;
        this.children = children;
        this.responseHandlerMode = (HttpHandleMode.equals(responseHandlerMode)) ? HttpHandleMode : JsonHandleMode;
        if(Debug.infoOn()) {
            Debug.logInfo("RemoteRequest, requestUrl: " + this.requestUrl, module);
            Debug.logInfo("RemoteRequest, host: " + this.host, module);
        }
        initData();
    }

    private void initData() {

        this.inMap = FastMap.newInstance();
        this.outMap = FastMap.newInstance();
        String nm, name, value, fieldName = null;
        for(Element elem: this.children) {
            nm = elem.getName();
            if (nm.equals("param-in")) {
                name = elem.getAttributeValue("name");
                value = this.parent.replaceParam(elem.getAttributeValue("value"));
                if(Debug.infoOn()) Debug.logInfo("RemoteRequest, param-in, name: " + name
                        + ", value: " + value, module);
                this.inMap.put(name, value);
            } else if (nm.equals("param-out")) {
                name = elem.getAttributeValue("result-name");
                fieldName = elem.getAttributeValue("field-name");
                if (fieldName == null || fieldName.length() == 0) {
                    fieldName = name;
                }
                this.outMap.put(name, fieldName);
            }
        }
    }

    public void runTest() {

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(supportedSchemes);
        DefaultHttpClient client = new DefaultHttpClient(ccm, defaultParameters);
        client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());

        // We first try to login with the loginAs to set the session.
        // Then we call the remote service.
        HttpEntity entity = null;
        ResponseHandler <String> responseHandler = null;
        try {
            BasicHttpContext localContext = new BasicHttpContext();
            // Create a local instance of cookie store
            CookieStore cookieStore = new BasicCookieStore();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            Header sessionHeader = null;

            if(this.loginAsUrl != null ) {

                String loginAsUri = this.host + this.loginAsUrl;
                String loginAsParamString = "?" + this.loginAsUserParam + "&" + this.loginAsPasswordParam;

                HttpGet req2 = new HttpGet ( loginAsUri + loginAsParamString );
                if(Debug.infoOn()) Debug.logInfo("loginAsUrl:" + loginAsUri + loginAsParamString, module);

                req2.setHeader("Connection","Keep-Alive");
                HttpResponse rsp = client.execute(req2, localContext);

                Header[] headers = rsp.getAllHeaders();
                for (Header hdr : headers) {
                    String headerValue = hdr.getValue();
                    if (headerValue.startsWith("JSESSIONID")) {
                        sessionHeader = hdr;
                    }
                    if(Debug.infoOn()) Debug.logInfo("login: " + hdr.getName() + " : " + hdr.getValue(), module);
                }
                List<Cookie> cookies = cookieStore.getCookies();
                if(Debug.infoOn()) Debug.logInfo("cookies.size(): " + cookies.size(), module);
                for (Cookie cookie : cookies) {
                    if(Debug.infoOn()) Debug.logInfo("Local cookie(0): " + cookie, module);
                }
            }

            if (HttpHandleMode.equals(this.responseHandlerMode)) {

            } else {
                responseHandler = new JsonResponseHandler(this);
            }

            String paramString = urlEncodeArgs(this.inMap, false);

            String thisUri = null;
            if(sessionHeader != null) {
                String sessionHeaderValue = sessionHeader.getValue();
                int pos1 = sessionHeaderValue.indexOf("=");
                int pos2 = sessionHeaderValue.indexOf(";");
                String sessionId = sessionHeaderValue.substring(pos1 + 1, pos2);
                thisUri = this.host + this.requestUrl + ";jsessionid=" + sessionId + "?"  + paramString;
            } else {
                thisUri = this.host + this.requestUrl + "?" + paramString;
            }
            if(Debug.infoOn()) Debug.logInfo("thisUri: " + thisUri, module);

            HttpGet req = new HttpGet ( thisUri );
            if(sessionHeader != null) {
                req.setHeader(sessionHeader);
            }

            String responseBody = client.execute( req, responseHandler, localContext);
        } catch(HttpResponseException e) {
            System.out.println(e.getMessage());
        } catch(IOException e) {
            System.out.println(e.getMessage());
        } finally {
            // If we could be sure that the stream of the entity has been
            // closed, we wouldn't need this code to release the connection.
            // However, EntityUtils.toString(...) can throw an exception.

            // if there is no entity, the connection is already released
            try {
              if (entity != null)
                EntityUtils.consume(entity); // release connection gracefully
            } catch(IOException e) {
                System.out.println("in 'finally'  " + e.getMessage());
            }

        }
    }

        /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map<String, ? extends Object> args, boolean useExpandedEntites) {
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (Map.Entry<String, ? extends Object> entry: args.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name != null && value != null) {
                    if (value instanceof String) {
                        valueStr = (String) value;
                    } else {
                        valueStr = value.toString();
                    }

                    if (valueStr != null && valueStr.length() > 0) {
                        if (buf.length() > 0) {
                            if (useExpandedEntites) {
                                buf.append("&amp;");
                            } else {
                                buf.append("&");
                            }
                        }
                        try {
                            buf.append(URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            //Debug.logError(e, module);
                        }
                        buf.append('=');
                        try {
                            buf.append(URLEncoder.encode(valueStr, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                           // Debug.logError(e, module);
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    public class JsonResponseHandler extends BasicResponseHandler {

        private RemoteRequest parentRemoteRequest;

        public JsonResponseHandler(RemoteRequest parentRemoteRequest) {
            super();
            this.parentRemoteRequest = parentRemoteRequest;
        }

        public String handleResponse(org.apache.http.HttpResponse response) throws IOException {
            String bodyString = super.handleResponse(response);
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.fromObject( bodyString );
            } catch(JSONException e) {
                throw new HttpResponseException(0, e.getMessage());
            }
            Set<Map.Entry<String, String>> paramSet = this.parentRemoteRequest.outMap.entrySet();
            Map<String, Object> parentDataMap = this.parentRemoteRequest.parent.getMap();
            for (Map.Entry<String, String> paramPair : paramSet) {
                if (jsonObject.containsKey(paramPair.getKey())) {
                   Object obj = jsonObject.get(paramPair.getKey());
                    if(Debug.infoOn()) Debug.logInfo("RemoteRequest, param-out, name: " + paramPair.getKey()
                            + ", value: " + obj, module);
                   parentDataMap.put(paramPair.getKey(), obj);
                }
            }
            return bodyString;
        }

    }
}
