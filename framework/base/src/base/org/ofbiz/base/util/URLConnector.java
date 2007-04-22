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

import org.ofbiz.base.config.GenericConfigException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;

import javax.net.ssl.*;

/**
 * URLConnector.java
 * 
 */
public class URLConnector {
    
    public static final String module = URLConnector.class.getName();

    private URLConnection connection = null;
    private URL url = null;
    private String clientCertAlias = null;
    private boolean timedOut = false;
    private int hostCertLevel = 2;

    protected URLConnector() {}
    protected URLConnector(URL url, String clientCertAlias, int hostCertLevel) {
        this.clientCertAlias = clientCertAlias;
        this.url = url;
        this.hostCertLevel = hostCertLevel;
    }
    
    protected synchronized URLConnection openConnection(int timeout) throws IOException {       
        Thread t = new Thread(new URLConnectorThread());
        t.start();
              
        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            if (connection == null) {
                timedOut = true;
            } else {
                close(connection);
            }
            throw new IOException("Connection never established");
        }

        if (connection != null) {
            return connection;
        } else {
            timedOut = true;
            throw new IOException("Connection timed out");
        }
    }
    
    public static URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, 30000);
    }
    
    public static URLConnection openConnection(URL url, int timeout) throws IOException {
        return openConnection(url, timeout, null, SSLUtil.HOSTCERT_NORMAL_CHECK);
    }
    
    public static URLConnection openConnection(URL url, String clientCertAlias) throws IOException {
        return openConnection(url, 30000, clientCertAlias, SSLUtil.HOSTCERT_NORMAL_CHECK);
    }
      
    public static URLConnection openConnection(URL url, int timeout, String clientCertAlias, int hostCertLevel) throws IOException {
        URLConnector uc = new URLConnector(url, clientCertAlias, hostCertLevel);
        return uc.openConnection(timeout);
    }    

    // special thread to open the connection
    private class URLConnectorThread implements Runnable {
        public void run() {
            URLConnection con = null;
            try {
                con = url.openConnection();
                if ("HTTPS".equalsIgnoreCase(url.getProtocol())) {
                    HttpsURLConnection scon = (HttpsURLConnection) con;
                    try {
                        scon.setSSLSocketFactory(SSLUtil.getSSLSocketFactory(clientCertAlias));
                        HostnameVerifier hv = SSLUtil.getHostnameVerifier(hostCertLevel);
                        if (hv != null) {
                            scon.setHostnameVerifier(hv);
                        }
                    } catch (GeneralSecurityException e) {
                        Debug.logError(e, module);
                    } catch (GenericConfigException e) {
                        Debug.logError(e, module);
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }

            synchronized (URLConnector.this) {
                if (timedOut && con != null) {
                    close(con);
                } else {
                    connection = con;
                    URLConnector.this.notify();
                }
            }
        }
    }
    
    // closes the HttpURLConnection does nothing to others
    private static void close(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).disconnect();
        }
    }
}
