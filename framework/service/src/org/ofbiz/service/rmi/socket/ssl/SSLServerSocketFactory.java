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
import org.ofbiz.base.config.GenericConfigException;

/**
 * RMI SSL Server Socket Factory
 */
public class SSLServerSocketFactory implements RMIServerSocketFactory, Serializable {

    public static final String module =  SSLServerSocketFactory.class.getName();
    protected boolean clientAuth = false;
    protected String keystore = null;
    protected String ksType = null;
    protected String ksPass = null;
    protected String alias = null;

    public void setNeedClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    public void setKeyStore(String location, String type, String password) {
        this.keystore = location;
        this.ksType = type;
        this.ksPass = password;
        this.alias = alias;
    }

    public void setKeyStoreAlias(String alias) {
        this.alias = alias;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        char[] passphrase = null;
        if (ksPass != null) {
            passphrase = ksPass.toCharArray();
        }

        KeyStore ks = null;
        if (keystore != null) {
            try {
                ks = KeyStore.getInstance(ksType);
                ks.load(new FileInputStream(keystore), passphrase);
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
        } catch (GeneralSecurityException e) {
            Debug.logError(e, "Error getting javax.net.ssl.SSLServerSocketFactory instance for Service Engine RMI calls: " + e.toString(), module);
            throw new IOException(e.toString());
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting javax.net.ssl.SSLServerSocketFactory instance for Service Engine RMI calls: " + e.toString(), module);
        }

        if (factory == null) {
            throw new IOException("Unable to obtain SSLServerSocketFactory for provided KeyStore");
        }

        SSLServerSocket socket = (SSLServerSocket) factory.createServerSocket(port);
        socket.setNeedClientAuth(clientAuth);
        return socket;
    }
}
