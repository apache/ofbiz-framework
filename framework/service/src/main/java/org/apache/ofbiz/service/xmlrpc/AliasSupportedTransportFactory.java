/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.service.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcHttpClientConfig;
import org.apache.xmlrpc.client.XmlRpcHttpTransport;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.util.HttpUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.SSLUtil;
import org.xml.sax.SAXException;

/**
 * AliasSupportedTransportFactory
 */
public class AliasSupportedTransportFactory extends XmlRpcTransportFactoryImpl {

    private final AliasSupportedTransport transport;

    public AliasSupportedTransportFactory(org.apache.xmlrpc.client.XmlRpcClient client, KeyStore ks, String password, String alias) {
        super(client);
        transport = new AliasSupportedTransport(client, ks, password, alias);
    }

    public XmlRpcTransport getTransport() {
        return transport;
    }

    class AliasSupportedTransport extends XmlRpcHttpTransport {

        protected static final String userAgent = USER_AGENT + " (Sun HTTP Transport)";
        private URLConnection con;
        private String password;
        private String alias;
        private KeyStore ks;

        protected AliasSupportedTransport(org.apache.xmlrpc.client.XmlRpcClient client, KeyStore ks, String password, String alias) {
            super(client, userAgent);
            this.password = password;
            this.alias = alias;
            this.ks = ks;
        }

        @Override
        public Object sendRequest(XmlRpcRequest req) throws XmlRpcException {
            XmlRpcHttpClientConfig config = (XmlRpcHttpClientConfig) req.getConfig();
            URL serverUrl = config.getServerURL();
            if (serverUrl == null) {
                throw new XmlRpcException("Invalid server URL");
            }

            try {
                con = openConnection(serverUrl);
                con.setUseCaches(false);
                con.setDoInput(true);
                con.setDoOutput(true);
            } catch (IOException e) {
                throw new XmlRpcException("Failed to create URLConnection: " + e.getMessage(), e);
            }
            return super.sendRequest(req);
        }

        protected URLConnection openConnection(URL url) throws IOException {
            URLConnection con = url.openConnection();
            if ("HTTPS".equalsIgnoreCase(url.getProtocol())) {
                HttpsURLConnection scon = (HttpsURLConnection) con;
                try {
                    scon.setSSLSocketFactory(SSLUtil.getSSLSocketFactory(ks, password, alias));
                    scon.setHostnameVerifier(SSLUtil.getHostnameVerifier(SSLUtil.getHostCertMinCheck()));
                } catch (GeneralException e) {
                    throw new IOException(e.getMessage());
                } catch (GeneralSecurityException e) {
                    throw new IOException(e.getMessage());
                }
            }

            return con;
        }

        @Override
        protected void setRequestHeader(String header, String value) {
            con.setRequestProperty(header, value);
        }

        @Override
        protected void close() throws XmlRpcClientException {
            if (con instanceof HttpURLConnection) {
                ((HttpURLConnection) con).disconnect();
            }
        }

        @Override
        protected boolean isResponseGzipCompressed(XmlRpcStreamRequestConfig config) {
            return HttpUtil.isUsingGzipEncoding(con.getHeaderField("Content-Encoding"));
        }

        @Override
        protected InputStream getInputStream() throws XmlRpcException {
            try {
                return con.getInputStream();
            } catch (IOException e) {
                throw new XmlRpcException("Failed to create input stream: " + e.getMessage(), e);
            }
        }

        @Override
        protected void writeRequest(ReqWriter pWriter) throws IOException, XmlRpcException, SAXException {
            pWriter.write(con.getOutputStream());
        }
    }
}
