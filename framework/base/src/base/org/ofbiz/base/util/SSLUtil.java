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
import org.ofbiz.base.component.ComponentConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.*;

import javax.net.ssl.*;

import javolution.util.FastList;

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
            for (int i = 0; i < mgrs.length; i++) {
                if (mgrs[i] instanceof X509TrustManager) {
                    try {
                        ((X509TrustManager) mgrs[i]).checkClientTrusted(chain, authType);
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
        Iterator i = ComponentConfig.getAllKeystoreInfos().iterator();
        List keyMgrs = FastList.newInstance();
        
        while (i.hasNext()) {
            ComponentConfig.KeystoreInfo ksi = (ComponentConfig.KeystoreInfo) i.next();
            if (ksi.isCertStore()) {
                KeyStore ks = ksi.getKeyStore();
                if (ks != null) {
                    keyMgrs.addAll(Arrays.asList(getKeyManagers(ks, ksi.getPassword(), alias)));
                } else {
                    throw new IOException("Unable to load keystore: " + ksi.createResourceHandler().getFullLocation());
                }
            }
        }

        return (KeyManager[]) keyMgrs.toArray(new KeyManager[keyMgrs.size()]);
    }

    public static KeyManager[] getKeyManagers() throws IOException, GeneralSecurityException, GenericConfigException {
        return getKeyManagers(null);
    }

    public static TrustManager[] getTrustManagers() throws IOException, GeneralSecurityException, GenericConfigException {
        KeyStore trustStore = KeyStoreUtil.getSystemTrustStore();
        List trustMgrs = FastList.newInstance();
        trustMgrs.addAll(Arrays.asList(getTrustManagers(trustStore)));

        Iterator i = ComponentConfig.getAllKeystoreInfos().iterator();
        while (i.hasNext()) {
            ComponentConfig.KeystoreInfo ksi = (ComponentConfig.KeystoreInfo) i.next();
            if (ksi.isTrustStore()) {
                KeyStore ks = ksi.getKeyStore();
                if (ks != null) {
                    trustMgrs.addAll(Arrays.asList(getTrustManagers(ks)));
                } else {
                    throw new IOException("Unable to load keystore: " + ksi.createResourceHandler().getFullLocation());
                }
            }
        }

        return (TrustManager[]) trustMgrs.toArray(new TrustManager[trustMgrs.size()]);
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

    public static SSLSocketFactory getSSLSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        KeyManager[] km = SSLUtil.getKeyManagers(ks, password, alias);
        TrustManager[] tm = SSLUtil.getTrustManagers();

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, new SecureRandom());
        return context.getSocketFactory();
    }

    public static SSLSocketFactory getSSLSocketFactory(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        KeyManager[] km = SSLUtil.getKeyManagers(alias);
        TrustManager[] tm = SSLUtil.getTrustManagers();

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, new SecureRandom());
        return context.getSocketFactory();
    }

    public static SSLSocketFactory getSSLSocketFactory() throws IOException, GeneralSecurityException, GenericConfigException {

        return getSSLSocketFactory(null);
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(KeyStore ks, String password, String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        TrustManager[] tm = SSLUtil.getTrustManagers();
        KeyManager[] km = SSLUtil.getKeyManagers(ks, password, alias);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, null);
        return context.getServerSocketFactory();
    }

    public static SSLServerSocketFactory getSSLServerSocketFactory(String alias) throws IOException, GeneralSecurityException, GenericConfigException {
        TrustManager[] tm = SSLUtil.getTrustManagers();
        KeyManager[] km = SSLUtil.getKeyManagers(alias);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(km, tm, null);
        return context.getServerSocketFactory();
    }

    public static HostnameVerifier getHostnameVerifier(int level) {
        switch(level) {           
            case HOSTCERT_MIN_CHECK:
                return new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        Debug.log("Checking: " + hostname + " :: " + session.getPeerHost(), module);
                        javax.security.cert.X509Certificate[] peerCerts;
                        try {
                            peerCerts = session.getPeerCertificateChain();
                        } catch (SSLPeerUnverifiedException e) {
                            // cert not verified
                            Debug.logWarning(e.getMessage(), module);
                            return false;
                        }
                        for (int i = 0; i < peerCerts.length; i++) {
                            Map certMap = new HashMap();
                            String name = peerCerts[i].getSubjectDN().getName();
                            String[] sections = name.split("\\,");
                            for (int si = 0; si < sections.length; si++) {
                                String[] nv = sections[si].split("\\=");
                                for (int nvi = 0; nvi < nv.length; nvi++) {
                                    certMap.put(nv[0], nv[1]);
                                }
                            }

                            Debug.log(peerCerts[i].getSerialNumber().toString(16) + " :: " + certMap.get("CN"), module);
                            try {
                                peerCerts[i].checkValidity();
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

            if (debug) {
                System.setProperty("javax.net.debug","ssl:handshake");
            }
            loadedProps = true;
        }
    }
}
