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

package org.apache.ofbiz.service.rmi.socket.ssl;

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

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.SSLUtil;

/**
 * RMI SSL Server Socket Factory
 */
@SuppressWarnings("serial")
public class SSLServerSocketFactory implements RMIServerSocketFactory, Serializable {

    private static final String MODULE = SSLServerSocketFactory.class.getName();
    private boolean clientAuth = false;
    private String keystore = null;
    private String ksType = null;
    private String ksPass = null;
    private String alias = null;

    /**
     * Sets need client auth.
     * @param clientAuth the client auth
     */
    public void setNeedClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    /**
     * Sets key store.
     * @param location the location
     * @param type the type
     * @param password the password
     */
    public void setKeyStore(String location, String type, String password) {
        this.keystore = location;
        this.ksType = type;
        this.ksPass = password;
    }

    /**
     * Sets key store alias.
     * @param alias the alias
     */
    public void setKeyStoreAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        char[] passphrase = null;
        if (ksPass != null) {
            passphrase = ksPass.toCharArray();
        }

        KeyStore ks = null;
        if (keystore != null) {
            try (FileInputStream fis = new FileInputStream(keystore)) {
                ks = KeyStore.getInstance(ksType);
                ks.load(fis, passphrase);
            } catch (NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException e) {
                Debug.logError(e, MODULE);
                throw new IOException(e.getMessage());
            }
        }

        if (alias == null) {
            throw new IOException("SSL certificate alias cannot be null; MUST be set for SSLServerSocketFactory!");
        }

        javax.net.ssl.SSLServerSocketFactory factory = null;
        try {
            if (ks != null) {
                factory = SSLUtil.getSSLServerSocketFactory(ks, ksPass, alias);
            } else {
                factory = SSLUtil.getSSLServerSocketFactory(alias);
            }
        } catch (GeneralSecurityException | GenericConfigException e) {
            Debug.logError(e, "Error getting javax.net.ssl.SSLServerSocketFactory instance for Service Engine RMI calls: " + e.toString(), MODULE);
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
