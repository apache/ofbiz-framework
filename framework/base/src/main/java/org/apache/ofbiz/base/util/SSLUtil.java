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
package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
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
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.config.GenericConfigException;

/**
 * KeyStoreUtil - Utilities for setting up SSL connections with specific client certificates
 *
 */
public final class SSLUtil {

    private static final String MODULE = SSLUtil.class.getName();

    private static final int HOSTCERT_NO_CHECK = 0;
    private static final int HOSTCERT_MIN_CHECK = 1;
    private static final int HOSTCERT_NORMAL_CHECK = 2;

    private static boolean loadedProps = false;

    private SSLUtil() { }

    static {
        SSLUtil.loadJsseProperties();
    }

    private static class TrustAnyManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String string) throws CertificateException {
            Debug.logImportant("Trusting (un-trusted) client certificate chain:", MODULE);
            for (X509Certificate cert: certs) {
                Debug.logImportant("---- " + cert.getSubjectX500Principal().getName() + " valid: " + cert.getNotAfter(), MODULE);

            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String string) throws CertificateException {
            Debug.logImportant("Trusting (un-trusted) server certificate chain:", MODULE);
            for (X509Certificate cert: certs) {
                Debug.logImportant("---- " + cert.getSubjectX500Principal().getName() + " valid: " + cert.getNotAfter(), MODULE);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    public static int getHostCertNoCheck() {
        return HOSTCERT_NO_CHECK;
    }

    public static int getHostCertMinCheck() {
        return HOSTCERT_MIN_CHECK;
    }

    public static int getHostCertNormalCheck() {
        return HOSTCERT_NORMAL_CHECK;
    }

    public static boolean isClientTrusted(X509Certificate[] chain, String authType) {
        TrustManager[] mgrs = new TrustManager[0];
        try {
            mgrs = SSLUtil.getTrustManagers();
        } catch (IOException | GeneralSecurityException | GenericConfigException e) {
            Debug.logError(e, MODULE);
        }

        for (TrustManager mgr : mgrs) {
            if (mgr instanceof X509TrustManager) {
                try {
                    ((X509TrustManager) mgr).checkClientTrusted(chain, authType);
                    return true;
                } catch (CertificateException e) {
                    Debug.logError(e, MODULE);
                }
            }
        }
        return false;
    }

    public static KeyManager[] getKeyManagers(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        List<KeyManager> keyMgrs = new LinkedList<>();
        for (ComponentConfig.KeystoreInfo ksi: ComponentConfig.getAllKeystoreInfos()) {
            if (ksi.isCertStore()) {
                KeyStore ks = ksi.getKeyStore();
                if (ks != null) {
                    List<KeyManager> newKeyManagers = Arrays.asList(getKeyManagers(ks, ksi.getPassword(), alias));
                    keyMgrs.addAll(newKeyManagers);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Loaded another cert store, adding [" + newKeyManagers.size()
                                + "] KeyManagers for alias [" + alias + "] and keystore: " + ksi.createResourceHandler()
                                        .getFullLocation(), MODULE);
                    }
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
            Debug.logWarning("System truststore not found!", MODULE);
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

        return new TrustManager[] {tm };
    }

    public static TrustManager[] getTrustAnyManagers() {
        return new TrustManager[] {new TrustAnyManager() };
    }

    public static KeyManager[] getKeyManagers(KeyStore ks, String password, String alias) throws GeneralSecurityException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
        factory.init(ks, password.toCharArray());
        KeyManager[] keyManagers = factory.getKeyManagers();
        if (alias != null) {
            for (int i = 0; i < keyManagers.length; i++) {
                if (keyManagers[i] instanceof X509KeyManager) {
                    keyManagers[i] = new AliasKeyManager((X509KeyManager) keyManagers[i], alias);
                }
            }
        }
        return keyManagers;
    }

    public static TrustManager[] getTrustManagers(KeyStore ks) {
        return new TrustManager[] {new MultiTrustManager(ks) };
    }

    public static SSLSocketFactory getSSLSocketFactory(KeyStore ks, String password, String alias)
            throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(ks, password, alias, false).getSocketFactory();
    }

    public static SSLContext getSSLContext(KeyStore ks, String password, String alias, boolean trustAny)
            throws IOException, GeneralSecurityException, GenericConfigException {
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

    public static SSLSocketFactory getSSLSocketFactory(String alias, boolean trustAny)
            throws IOException, GeneralSecurityException, GenericConfigException {
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

    public static SSLServerSocketFactory getSSLServerSocketFactory(KeyStore ks, String password, String alias)
            throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(ks, password, alias, false).getServerSocketFactory();
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(String alias)
            throws IOException, GeneralSecurityException, GenericConfigException {
        return getSSLContext(alias, false).getServerSocketFactory();
    }

    public static HostnameVerifier getHostnameVerifier(int level) {
        switch (level) {
        case HOSTCERT_MIN_CHECK:
            return (hostname, session) -> {
                Certificate[] peerCerts;
                try {
                    peerCerts = session.getPeerCertificates();
                } catch (SSLPeerUnverifiedException e) {
                    // cert not verified
                    Debug.logWarning(e.getMessage(), MODULE);
                    return false;
                }
                for (Certificate peerCert : peerCerts) {
                    try {
                        Principal x500s = session.getPeerPrincipal();
                        Map<String, String> subjectMap = KeyStoreUtil.getX500Map(x500s);
                        if (Debug.infoOn()) {
                            byte[] encodedCert = peerCert.getEncoded();
                            Debug.logInfo(new BigInteger(encodedCert).toString(16)
                                    + " :: " + subjectMap.get("CN"), MODULE);
                        }
                        peerCert.verify(peerCert.getPublicKey());
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        // certificate not valid
                        Debug.logWarning("Certificate is not valid!", MODULE);
                        return false;
                    }
                }
                return true;
            };
        case HOSTCERT_NO_CHECK:
            return (hostname, session) -> true;
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
            if (protocol != null && !"NONE".equals(protocol)) {
                System.setProperty("java.protocol.handler.pkgs", protocol);
            }
            if (proxyHost != null && !"NONE".equals(proxyHost)) {
                System.setProperty("https.proxyHost", proxyHost);
            }
            if (proxyPort != null && !"NONE".equals(proxyPort)) {
                System.setProperty("https.proxyPort", proxyPort);
            }
            if (cypher != null && !"NONE".equals(cypher)) {
                System.setProperty("https.cipherSuites", cypher);
            }

            if (debug) {
                System.setProperty("javax.net.debug", "ssl:handshake");
            }
            loadedProps = true;
        }
    }
}
