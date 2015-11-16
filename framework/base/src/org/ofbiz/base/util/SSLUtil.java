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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;

/**
 * KeyStoreUtil - Utilities for setting up SSL connections with specific client certificates
 *
 */
public class SSLUtil {

    public static final String module = SSLUtil.class.getName();

    public static final int HOSTCERT_NO_CHECK = 0;
    public static final int HOSTCERT_MIN_CHECK = 1;
    public static final int HOSTCERT_NORMAL_CHECK = 2;

    private static boolean loadedProps = false;

    static {
        SSLUtil.loadJsseProperties();
    }

    public static boolean isClientTrusted(X509Certificate[] chain, String authType) {
        TrustManager[] mgrs = new TrustManager[0];
        try {
            mgrs = SSLUtil.getTrustManagers();
        } catch (IOException e) {
            Debug.logError(e, module);
        } catch (GeneralSecurityException e) {
            Debug.logError(e, module);
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
        }

        if (mgrs != null) {
            for (TrustManager mgr: mgrs) {
                if (mgr instanceof X509TrustManager) {
                    try {
                        ((X509TrustManager) mgr).checkClientTrusted(chain, authType);
                        return true;
                    } catch (CertificateException e) {
                        // do nothing; just loop
                    }
                }
            }
        }
        return false;
    }

    public static KeyManager[] getKeyManagers(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        List<KeyManager> keyMgrs = new LinkedList<KeyManager>();
        for (ComponentConfig.KeystoreInfo ksi: ComponentConfig.getAllKeystoreInfos()) {
            if (ksi.isCertStore()) {
                KeyStore ks = ksi.getKeyStore();
                if (ks != null) {
                    List<KeyManager> newKeyManagers = Arrays.asList(getKeyManagers(ks, ksi.getPassword(), alias));
                    keyMgrs.addAll(newKeyManagers);
                    if (Debug.verboseOn()) Debug.logVerbose("Loaded another cert store, adding [" + (newKeyManagers == null ? "0" : newKeyManagers.size()) + "] KeyManagers for alias [" + alias + "] and keystore: " + ksi.createResourceHandler().getFullLocation(), module);
                } else {
                    throw new IOException("Unable to load keystore: " + ksi.createResourceHandler().getFullLocation());
                }
            }
        }

        return keyMgrs.toArray(new KeyManager[keyMgrs.size()]);
    }

    public static KeyManager[] getKeyManagers() throws IOException, GeneralSecurityException, GenericConfigException {
        return getKeyManagers(null);
    }

    public static TrustManager[] getTrustManagers() throws IOException, GeneralSecurityException, GenericConfigException {
        MultiTrustManager tm = new MultiTrustManager();
        tm.add(KeyStoreUtil.getSystemTrustStore());
        if (tm.getNumberOfKeyStores() < 1) {
            Debug.logWarning("System truststore not found!", module);
        }

        for (ComponentConfig.KeystoreInfo ksi: ComponentConfig.getAllKeystoreInfos()) {
            if (ksi.isTrustStore()) {
                KeyStore ks = ksi.getKeyStore();
                if (ks != null) {
                    tm.add(ks);
                } else {
                    throw new IOException("Unable to load keystore: " + ksi.createResourceHandler().getFullLocation());
                }
            }
        }

        return new TrustManager[] { tm };
    }

    public static TrustManager[] getTrustAnyManagers() {
        return new TrustManager[] { new TrustAnyManager() };
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
        return new TrustManager[] { new MultiTrustManager(ks) };
    }

    public static SSLSocketFactory getSSLSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(ks, password, alias, false).getSocketFactory();
    }

    public static SSLContext getSSLContext(KeyStore ks, String password, String alias, boolean trustAny) throws IOException, GeneralSecurityException, GenericConfigException {
        KeyManager[] km = SSLUtil.getKeyManagers(ks, password, alias);
        TrustManager[] tm;
        if (trustAny) {
            tm = SSLUtil.getTrustAnyManagers();
        } else {
            tm = SSLUtil.getTrustManagers();
        }

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, new SecureRandom());
        return context;
    }

    public static SSLSocketFactory getSSLSocketFactory(String alias, boolean trustAny) throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(alias, trustAny).getSocketFactory();
    }

    public static SSLContext getSSLContext(String alias, boolean trustAny) throws IOException, GeneralSecurityException, GenericConfigException {
        KeyManager[] km = SSLUtil.getKeyManagers(alias);
        TrustManager[] tm;
        if (trustAny) {
            tm = SSLUtil.getTrustAnyManagers();
        } else {
            tm = SSLUtil.getTrustManagers();
        }

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, new SecureRandom());
        return context;
    }

    public static SSLSocketFactory getSSLSocketFactory(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLSocketFactory(alias, false);
    }

    public static SSLSocketFactory getSSLSocketFactory() throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLSocketFactory(null);
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(ks, password, alias, false).getServerSocketFactory();
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(alias, false).getServerSocketFactory();
    }

    public static HostnameVerifier getHostnameVerifier(int level) {
        switch (level) {
            case HOSTCERT_MIN_CHECK:
                return new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        javax.security.cert.X509Certificate[] peerCerts;
                        try {
                            peerCerts = session.getPeerCertificateChain();
                        } catch (SSLPeerUnverifiedException e) {
                            // cert not verified
                            Debug.logWarning(e.getMessage(), module);
                            return false;
                        }
                        for (javax.security.cert.X509Certificate peerCert: peerCerts) {
                            Principal x500s = peerCert.getSubjectDN();
                            Map<String, String> subjectMap = KeyStoreUtil.getX500Map(x500s);

                            if (Debug.infoOn())
                                Debug.logInfo(peerCert.getSerialNumber().toString(16) + " :: " + subjectMap.get("CN"), module);

                            try {
                                peerCert.checkValidity();
                            } catch (Exception e) {
                                // certificate not valid
                                Debug.logWarning("Certificate is not valid!", module);
                                return false;
                            }
                        }
                        return true;
                    }
                };
            case HOSTCERT_NO_CHECK:
                return new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
            default:
                return null;
        }
    }

    public static void loadJsseProperties() {
        loadJsseProperties(false);
    }

    public static synchronized void loadJsseProperties(boolean debug) {
        if (!loadedProps) {
            String protocol = UtilProperties.getPropertyValue("jsse", "java.protocol.handler.pkgs", "NONE");
            String proxyHost = UtilProperties.getPropertyValue("jsse", "https.proxyHost", "NONE");
            String proxyPort = UtilProperties.getPropertyValue("jsse", "https.proxyPort", "NONE");
            String cypher = UtilProperties.getPropertyValue("jsse", "https.cipherSuites", "NONE");
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

            if (debug) {
                System.setProperty("javax.net.debug","ssl:handshake");
            }
            loadedProps = true;
        }
    }

    static class TrustAnyManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] certs, String string) throws CertificateException {
            Debug.logImportant("Trusting (un-trusted) client certificate chain:", module);
            for (X509Certificate cert: certs) {
                Debug.logImportant("---- " + cert.getSubjectX500Principal().getName() + " valid: " + cert.getNotAfter(), module);

            }
        }

        public void checkServerTrusted(X509Certificate[] certs, String string) throws CertificateException {
            Debug.logImportant("Trusting (un-trusted) server certificate chain:", module);
            for (X509Certificate cert: certs) {
                Debug.logImportant("---- " + cert.getSubjectX500Principal().getName() + " valid: " + cert.getNotAfter(), module);
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
