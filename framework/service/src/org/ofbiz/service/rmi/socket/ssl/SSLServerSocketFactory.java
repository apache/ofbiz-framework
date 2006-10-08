/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.service.rmi.socket.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLServerSocket;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilProperties;

/**
 * RMI SSL Server Socket Factory
 */
public class SSLServerSocketFactory implements RMIServerSocketFactory, Serializable {

    public static final String module =  SSLServerSocketFactory.class.getName();
    protected boolean clientAuth = false;

    public void setNeedClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        String storeType = UtilProperties.getPropertyValue("jsse.properties", "ofbiz.rmi.keyStore.type", "jks");
        String storeFile = UtilProperties.getPropertyValue("jsse.properties", "ofbiz.rmi.keyStore", null);
        String storeAlias = UtilProperties.getPropertyValue("jsse.properties", "ofbiz.rmi.keyStore.alias", null);
        String storePass = UtilProperties.getPropertyValue("jsse.properties", "ofbiz.rmi.keyStore.password", null);
        char[] passphrase = null;
        if (storePass != null) {
            passphrase = storePass.toCharArray();
        }

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(storeType);
            ks.load(new FileInputStream(storeFile), passphrase);
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, module);
            throw new IOException(e.getMessage());
        } catch (CertificateException e) {
            Debug.logError(e, module);
            throw new IOException(e.getMessage());
        } catch (KeyStoreException e) {
            Debug.logError(e, module);
            throw new IOException(e.getMessage());
        }

        if (ks == null) {
            throw new IOException("Unable to load KeyStore containing Service Engine RMI SSL certificate");
        }


        javax.net.ssl.SSLServerSocketFactory factory = null;
        try {
            factory = SSLUtil.getSSLServerSocketFactory(ks, storePass, storeAlias);
        } catch (GeneralSecurityException e) {
            Debug.logError(e, "Error getting javax.net.ssl.SSLServerSocketFactory instance for Service Engine RMI calls: " + e.toString(), module);
            throw new IOException(e.toString());
        }

        if (factory == null) {
            throw new IOException("Unable to obtain SSLServerSocketFactory for provided KeyStore");
        }

        SSLServerSocket socket = (SSLServerSocket) factory.createServerSocket(port);
        socket.setNeedClientAuth(clientAuth);
        return socket;
    }
}
