/*
 * $Id: SSLServerSocketFactory.java 6301 2005-12-12 12:46:44Z jonesde $
 *
 * Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
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
