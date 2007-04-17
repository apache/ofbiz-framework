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
package org.ofbiz.base.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Send HTTP GET/POST requests.
 *
 */
public class HttpClient {
    
    public static final String module = HttpClient.class.getName();
    
    private int timeout = 30000;
    private boolean debug = false;
    private boolean lineFeed = true;
    private boolean followRedirects = true;
    
    private String url = null;
    private String rawStream = null;
    private String clientCertAlias = null;
    private Map parameters = null;
    private Map headers = null;
    
    private URL requestUrl = null;
    private URLConnection con = null;

    /** Creates an empty HttpClient object. */
    public HttpClient() {}

    /** Creates a new HttpClient object. */
    public HttpClient(URL url) {
        this.url = url.toExternalForm();
    }

    /** Creates a new HttpClient object. */
    public HttpClient(String url) {
        this.url = url;
    }

    /** Creates a new HttpClient object. */
    public HttpClient(String url, Map parameters) {
        this.url = url;
        this.parameters = parameters;      
    }

    /** Creates a new HttpClient object. */
    public HttpClient(URL url, Map parameters) {
        this.url = url.toExternalForm();
        this.parameters = parameters;
    }

    /** Creates a new HttpClient object. */
    public HttpClient(String url, Map parameters, Map headers) {
        this.url = url;
        this.parameters = parameters;
        this.headers = headers;
    }

    /** Creates a new HttpClient object. */
    public HttpClient(URL url, Map parameters, Map headers) {
        this.url = url.toExternalForm();
        this.parameters = parameters;
        this.headers = headers;
    }

    /** When true overrides Debug.verboseOn() and forces debugging for this instance */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Sets the timeout for waiting for the connection (default 30sec) */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    /** Enables this request to follow redirect 3xx codes (default true) */
     public void followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }
    
    /** Turns on or off line feeds in the request. (default is on) */
    public void setLineFeed(boolean lineFeed) {
        this.lineFeed = lineFeed;
    }
    
    /** Set the raw stream for posts. */
    public void setRawStream(String stream) {
        this.rawStream = stream;
    }
    
    /** Set the URL for this request. */
    public void setUrl(URL url) {
        this.url = url.toExternalForm();
    }

    /** Set the URL for this request. */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Set the parameters for this request. */
    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    /** Set an individual parameter for this request. */
    public void setParameter(String name, String value) {
        if (parameters == null)
            parameters = new HashMap();
        parameters.put(name, value);
    }

    /** Set the headers for this request. */
    public void setHeaders(Map headers) {
        this.headers = headers;
    }

    /** Set an individual header for this request. */
    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap();
        headers.put(name, value);
    }

    /** Return a Map of headers. */
    public Map getHeaders() {
        return headers;
    }

    /** Return a Map of parameters. */
    public Map getParameters() {
        return parameters;
    }

    /** Return a string representing the requested URL. */
    public String getUrl() {
        return url;
    }
    
    /** Sets the client certificate alias (from the keystore) to use for this SSL connection. */
    public void setClientCertificateAlias(String alias) {
        this.clientCertAlias = alias;
    }
    
    /** Returns the alias of the client certificate to be used for this SSL connection. */
    public String getClientCertificateAlias() {
        return this.clientCertAlias;
    }

    /** Invoke HTTP request GET. */
    public String get() throws HttpClientException {
        return sendHttpRequest("get");
    }

    /** Invoke HTTP request GET. */
    public InputStream getStream() throws HttpClientException {
        return sendHttpRequestStream("get");
    }

    /** Invoke HTTP request POST. */
    public String post() throws HttpClientException {
        return sendHttpRequest("post");
    }
    
    /** Invoke HTTP request POST and pass raw stream. */
    public String post(String stream) throws HttpClientException {
        this.rawStream = stream;
        return sendHttpRequest("post");
    }

    /** Invoke HTTP request POST. */
    public InputStream postStream() throws HttpClientException {
        return sendHttpRequestStream("post");
    }

    /** Returns the value of the specified named response header field. */
    public String getResponseHeader(String header) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderField(header);
    }

    /** Returns the key for the nth response header field. */
    public String getResponseHeaderFieldKey(int n) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderFieldKey(n);
    }

    /** Returns the value for the nth response header field. It returns null of there are fewer then n fields. */
    public String getResponseHeaderField(int n) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderField(n);
    }

    /** Returns the content of the response. */
    public Object getResponseContent() throws java.io.IOException, HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContent();
    }

    /** Returns the content-type of the response. */
    public String getResponseContentType() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentType();
    }

    /** Returns the content length of the response */
    public int getResponseContentLength() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentLength();
    }

    /** Returns the content encoding of the response. */
    public String getResponseContentEncoding() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentEncoding();
    }

    public int getResponseCode() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        if (!(con instanceof HttpURLConnection)) {
            throw new HttpClientException("Connection is not HTTP; no response code");
        }

        try {
            return ((HttpURLConnection) con).getResponseCode();
        } catch (IOException e) {
            throw new HttpClientException(e.getMessage(), e);
        }
    }

    private String sendHttpRequest(String method) throws HttpClientException {
        InputStream in = sendHttpRequestStream(method);
        if (in == null) return null;

        StringBuffer buf = new StringBuffer();
        try {
            if (Debug.verboseOn() || debug) {
                try {
                    Debug.log("ContentEncoding: " + con.getContentEncoding() + "; ContentType: " +
                            con.getContentType() + " or: " + URLConnection.guessContentTypeFromStream(in), module);                            
                } catch (IOException ioe) {
                    Debug.logWarning(ioe, "Caught exception printing content debugging information", module);
                }
            }
            
            String charset = null;
            String contentType = con.getContentType();
            if (contentType == null) {                
                try {                 
                    contentType = URLConnection.guessContentTypeFromStream(in);
                } catch (IOException ioe) {
                    Debug.logWarning(ioe, "Problems guessing content type from steam", module);
                }
            }
            
            if (Debug.verboseOn() || debug) Debug.log("Content-Type: " + contentType, module);
            
            if (contentType != null) {
                contentType = contentType.toUpperCase();
                int charsetEqualsLoc = contentType.indexOf("=", contentType.indexOf("CHARSET"));
                int afterSemiColon = contentType.indexOf(";", charsetEqualsLoc);
                if (charsetEqualsLoc >= 0 && afterSemiColon >= 0) {
                    charset = contentType.substring(charsetEqualsLoc + 1, afterSemiColon);
                } else if (charsetEqualsLoc >= 0) {
                    charset = contentType.substring(charsetEqualsLoc + 1);
                }
                
                if (charset != null) charset = charset.trim();
                if (Debug.verboseOn() || debug) Debug.log("Getting text from HttpClient with charset: " + charset, module);
            }
            
            BufferedReader post = new BufferedReader(charset == null ? new InputStreamReader(in) : new InputStreamReader(in, charset));
            String line = new String();

            if (Debug.verboseOn() || debug) Debug.log("---- HttpClient Response Content ----", module);
            while ((line = post.readLine()) != null) {
                if (Debug.verboseOn() || debug) Debug.log("[HttpClient] : " + line, module);
                buf.append(line);
                if (lineFeed) {
                    buf.append("\n");
                }
            }
        } catch (Exception e) {
            throw new HttpClientException("Error processing input stream", e);
        }
        return buf.toString();
    }

    private InputStream sendHttpRequestStream(String method) throws HttpClientException {                
        // setup some SSL variables
        SSLUtil.loadJsseProperties();
            
        String arguments = null;
        InputStream in = null;                     

        if (url == null) {
            throw new HttpClientException("Cannot process a null URL.");
        }

        if (rawStream != null) {
            arguments = rawStream;
        } else if (parameters != null && parameters.size() > 0) {
            arguments = UtilHttp.urlEncodeArgs(parameters, false);
        }

        // Append the arguments to the query string if GET.
        if (method.equalsIgnoreCase("get") && arguments != null) {
            url = url + "?" + arguments;
        }

        // Create the URL and open the connection.
        try {
            requestUrl = new URL(url);
            con = URLConnector.openConnection(requestUrl, timeout, clientCertAlias, SSLUtil.HOSTCERT_NORMAL_CHECK);
            if (Debug.verboseOn() || debug) Debug.log("Connection opened to : " + requestUrl.toExternalForm(), module);

            if ((con instanceof HttpURLConnection)) {
                ((HttpURLConnection) con).setInstanceFollowRedirects(followRedirects);
                if (Debug.verboseOn() || debug) Debug.log("Connection is of type HttpURLConnection", module);
            }

            con.setDoOutput(true);
            con.setUseCaches(false);
            if (Debug.verboseOn() || debug) Debug.log("Do Input = true / Use Caches = false", module);

            if (method.equalsIgnoreCase("post")) {
                con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                con.setDoInput(true);
                if (Debug.verboseOn() || debug) Debug.log("Set content type to : application/x-www-form-urlencoded", module);
            }

            if (headers != null && headers.size() > 0) {
                Set headerSet = headers.keySet();
                Iterator i = headerSet.iterator();

                while (i.hasNext()) {
                    String headerName = (String) i.next();
                    String headerValue = (String) headers.get(headerName);
                    con.setRequestProperty(headerName, headerValue);
                    if (Debug.verboseOn() || debug) Debug.log("Header : " + headerName + " -> " + headerValue, module);
                }
            } else {
                if (Debug.verboseOn() || debug) Debug.log("No headers to set", module);
            }

            if (method.equalsIgnoreCase("post")) {
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                if (Debug.verboseOn() || debug) Debug.log("Opened output stream", module);

                out.writeBytes(arguments);
                if (Debug.verboseOn() || debug) Debug.log("Wrote arguements (parameters) : " + arguments, module);

                out.flush();
                out.close();
                if (Debug.verboseOn() || debug) Debug.log("Flushed and closed buffer", module);
            }

            if (Debug.verboseOn() || debug) {
                Map headerFields = con.getHeaderFields();
                Debug.log("Header Fields : " + headerFields, module);
            }

            in = con.getInputStream();
        } catch (IOException ioe) {
            throw new HttpClientException("IO Error processing request", ioe);
        } catch (Exception e) {
            throw new HttpClientException("Error processing request", e);
        }

        return in;
    }


    public static String getUrlContent(String url) throws HttpClientException {
        HttpClient client = new HttpClient(url);
        return client.get();
    }

    public static int checkHttpRequest(String url) throws HttpClientException {
        HttpClient client = new HttpClient(url);
        client.get();
        return client.getResponseCode();
    }
}
