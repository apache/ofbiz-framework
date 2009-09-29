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
import java.util.Iterator;
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
import org.apache.http.conn.scheme.SocketFactory;
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
import org.jdom.Element;


public class RemoteRequest {

    public static final String module = RemoteRequest.class.getName();

    /**     
     * The default parameters.
     * Instantiated in {@link #setup setup}.
     */     
    private static HttpParams defaultParameters = null;
            
    /**
     * The scheme registry.
     * Instantiated in {@link #setup setup}.
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
    
    private int currentRowIndx;
    
    static {
        
        supportedSchemes = new SchemeRegistry();
        
        // Register the "http" protocol scheme, it is required
        // by the default operator to look up socket factories.
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));

        // prepare parameters
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        //HttpClientParams.setAuthenticating(params, true);
        defaultParameters = params;

    }
    public RemoteRequest(SeleniumXml parent, List<Element> children, List<Element> loginAs, String requestUrl, String hostString, String responseHandlerMode) {
   
        this(parent, children, requestUrl, hostString, responseHandlerMode);
        if(loginAs != null && !loginAs.isEmpty()) {
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
        System.out.println("RemoteRequest, requestUrl: " + this.requestUrl);
        System.out.println("RemoteRequest, host: " + this.host);
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
                System.out.println("RemoteRequest, param-in, name: " + name + ", value: " + value);
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
        return;
    }
    
    public void runTest() {

        ClientConnectionManager ccm =
            new ThreadSafeClientConnManager(defaultParameters, supportedSchemes);
        //  new SingleClientConnManager(getParams(), supportedSchemes);

        DefaultHttpClient client = new DefaultHttpClient(ccm, defaultParameters);
        client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());

        //
        // We first try to login with the loginAs to set the session.
        // Then we call the remote service.
        //
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
                System.out.println("loginAsUrl:" + loginAsUri + loginAsParamString);
                
                req2.setHeader("Connection","Keep-Alive");
                HttpResponse rsp = client.execute(req2, localContext);

                Header[] headers = rsp.getAllHeaders();
                for (int i=0; i<headers.length; i++) {
                    Header hdr = headers[i];
                    String headerValue = hdr.getValue();
                    if (headerValue.startsWith("JSESSIONID")) {
                        sessionHeader = hdr;
                    }
                    System.out.println("login: " + hdr.getName() + " : " + hdr.getValue());
                }
                List<Cookie> cookies = cookieStore.getCookies();
                System.out.println("cookies.size(): " + cookies.size());
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("Local cookie(0): " + cookies.get(i));
                }
            } 
            //String paramString2 = "USERNAME=" + this.parent.getUserName()
            //                   + "&PASSWORD=" + this.parent.getPassword();
            //String thisUri2 = this.host + "/eng/control/login?" + paramString2;
            //HttpGet req2 = new HttpGet ( thisUri2 );
            //req2.setHeader("Connection","Keep-Alive");
            //HttpResponse rsp = client.execute(req2, localContext);

            //Header sessionHeader = null;
            //Header[] headers = rsp.getAllHeaders();
            //for (int i=0; i<headers.length; i++) {
            //    Header hdr = headers[i];
            //    String headerValue = hdr.getValue();
            //    if (headerValue.startsWith("JSESSIONID")) {
            //        sessionHeader = hdr;
            //    }
            //    System.out.println(headers[i]);
            //    System.out.println(hdr.getName() + " : " + hdr.getValue());
            //}
            
            //List<Cookie> cookies = cookieStore.getCookies();
            //System.out.println("cookies.size(): " + cookies.size());
            //for (int i = 0; i < cookies.size(); i++) {
            //    System.out.println("Local cookie(0): " + cookies.get(i));
            //}
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
            //String sessionHeaderValue = sessionHeader.getValue();
            //int pos1 = sessionHeaderValue.indexOf("=");
            //int pos2 = sessionHeaderValue.indexOf(";");
            //String sessionId = sessionHeaderValue.substring(pos1 + 1, pos2);
            //System.out.println("sessionId: " + sessionId);
            //String thisUri = this.host + this.requestUrl + ";jsessionid=" + sessionId + "?"  + paramString;
            //String thisUri = this.host + this.requestUrl + "?"  + paramString;
            System.out.println("thisUri: " + thisUri);
            
            HttpGet req = new HttpGet ( thisUri );
            if(sessionHeader != null) {
                req.setHeader(sessionHeader);
            }

            String responseBody = client.execute( req, responseHandler, localContext);
            /*
            entity = rsp.getEntity();

            System.out.println("----------------------------------------");
            System.out.println(rsp.getStatusLine());
            Header[] headers = rsp.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
                System.out.println(headers[i]);
            }
            System.out.println("----------------------------------------");

            if (entity != null) {
                System.out.println(EntityUtils.toString(rsp.getEntity()));
            }
            */
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
                entity.consumeContent(); // release connection gracefully
            } catch(IOException e) {
                System.out.println("in 'finally'  " + e.getMessage());
            }

        }
        return;
    }
    
    private void login(DefaultHttpClient client, BasicHttpContext localContext) throws IOException{
        
        String paramString = "USERNAME=" + this.parent.getUserName()
                           + "&PASSWORD=" + this.parent.getPassword();
        String thisUri = this.host + "/eng/control/login?" + paramString;
        HttpGet req = new HttpGet ( thisUri );
        req.setHeader("Connection","Keep-Alive");
        client.execute(req, localContext);
        
        //client.getCredentialsProvider().setCredentials(new AuthScope("localhost", 8080), 
        //        new UsernamePasswordCredentials(this.parent.getUserName(),                        this.parent.getPassword()));
        
        return;
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
        public String handleResponse(org.apache.http.HttpResponse response) 
            throws HttpResponseException, IOException {
            
            String bodyString = super.handleResponse(response);
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.fromObject( bodyString );
            } catch(JSONException e) {
                throw new HttpResponseException(0, e.getMessage());
            }
            Set<Map.Entry<String, String>> paramSet = this.parentRemoteRequest.outMap.entrySet();
            Iterator<Map.Entry<String, String>> paramIter = paramSet.iterator();
            Map<String, Object> parentDataMap = this.parentRemoteRequest.parent.getMap();
            while (paramIter.hasNext()) {
                Map.Entry<String, String> paramPair = paramIter.next();
                if (jsonObject.containsKey(paramPair.getKey())) {
                   Object obj = jsonObject.get(paramPair.getKey());
                   System.out.println("RemoteRequest, param-out, name: " + paramPair.getKey() + ", value: " + obj);
                   parentDataMap.put(paramPair.getKey(), obj);
                }
            }
            return bodyString;
        }
        
    }
}
