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
package org.ofbiz.base.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.*;

/**
 * KeyStoreUtil - Utilities for setting up SSL connections with specific client certificates
 *
 */
public class SSLUtil {

    public static final String module = SSLUtil.class.getName();
    private static boolean loadedProps = false;

    static {
        SSLUtil. loadJsseProperties();
    }

    public static KeyManager[] getKeyManagers(KeyStore ks, String password, String alias) throws GeneralSecurityException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
        factory.init(ks, password.toCharArray());
        KeyManager[] keyManagers = factory.getKeyManagers();
        if (alias != null) {
            for (int i = 0; i < keyManagers.length; i++) {
                if (keyManagers[i] instanceof X509KeyManager) {
                    keyManagers[i] = new AliasKeyManager((X509KeyManager)keyManagers[i], alias);
                }
            }
        }
        return keyManagers;
    }

    public static TrustManager[] getTrustManagers(KeyStore ks) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        factory.init(ks);
        return factory.getTrustManagers();
    }

    public static SSLSocketFactory getSSLSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException {
        KeyStore trustStore = KeyStoreUtil.getTrustStore();
        TrustManager[] tm = getTrustManagers(trustStore);
        KeyManager[] km = getKeyManagers(ks, password, alias);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, null);
        return context.getSocketFactory();
    }

    public static SSLSocketFactory getSSLSocketFactory(String alias) throws IOException, GeneralSecurityException {
        return getSSLSocketFactory(KeyStoreUtil.getKeyStore(), KeyStoreUtil.getKeyStorePassword(), alias);
    }

    public static SSLSocketFactory getSSLSocketFactory() throws IOException, GeneralSecurityException {
        return getSSLSocketFactory(null);
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException {
        KeyStore trustStore = KeyStoreUtil.getTrustStore();
        TrustManager[] tm = getTrustManagers(trustStore);
        KeyManager[] km = getKeyManagers(ks, password, alias);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, null);
        return context.getServerSocketFactory();
    }

    public static void loadJsseProperties() {
        loadJsseProperties(false);
    }

    public static synchronized void loadJsseProperties(boolean debug) {
        if (!loadedProps) {
            String protocol = UtilProperties.getPropertyValue("jsse.properties", "java.protocol.handler.pkgs", "NONE");
            String proxyHost = UtilProperties.getPropertyValue("jsse.properties", "https.proxyHost", "NONE");
            String proxyPort = UtilProperties.getPropertyValue("jsse.properties", "https.proxyPort", "NONE");
            String cypher = UtilProperties.getPropertyValue("jsse.properties", "https.cipherSuites", "NONE");
            if (protocol != null && !protocol.equals("NONE")) {
                System.setProperty("java.protocol.handler.pkgs", protocol);
            }
            if (proxyHost != null && !proxyHost.equals("NONE")) {
                System.setProperty("https.proxyHost", proxyHost);
            }
            if (proxyPort != null && !proxyPort.equals("NONE")) {
                System.setProperty("https.proxyPort", proxyPort);
            }
            if (cypher != null && !cypher.equals("NONE")) {
                System.setProperty("https.cipherSuites", cypher);
            }

            // set up the keystore properties
            System.setProperty("javax.net.ssl.keyStore", KeyStoreUtil.getKeyStoreFileName());
            System.setProperty("javax.net.ssl.keyStorePassword", KeyStoreUtil.getKeyStorePassword());
            System.setProperty("javax.net.ssl.trustStore", KeyStoreUtil.getTrustStoreFileName());
            System.setProperty("javax.net.ssl.trustStorePassword", KeyStoreUtil.getTrustStorePassword());
            if (debug) {
                System.setProperty("javax.net.debug","ssl:handshake");
            }
            loadedProps = true;
        }
    }
}
