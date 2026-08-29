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

package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * MultiTrustManager
 */
public class MultiTrustManager implements X509TrustManager {

    private static final String MODULE = MultiTrustManager.class.getName();
    private List<KeyStore> keystores;

    public MultiTrustManager(KeyStore ks) {
        this();
        keystores.add(ks);
    }

    public MultiTrustManager() {
        keystores = new LinkedList<>();
    }

    /**
     * Add.
     * @param ks the ks
     */
    public void add(KeyStore ks) {
        if (ks != null) {
            keystores.add(ks);
        }
    }

    /**
     * Gets number of key stores.
     * @return the number of key stores
     */
    int getNumberOfKeyStores() {
        return keystores.size();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        CertificateException trustFailure;
        try {
            getDelegateTrustManager().checkClientTrusted(certs, alg);
            return;
        } catch (CertificateException e) {
            trustFailure = e;
        } catch (RuntimeException e) {
            // The JDK's own X509TrustManager implementation can throw an unchecked exception
            // (e.g. when there are no trust anchors at all) instead of a CertificateException.
            trustFailure = new CertificateException(e);
        }
        if (!"true".equals(UtilProperties.getPropertyValue("certificate", "client.all-trusted", "true"))) {
            throw trustFailure;
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        CertificateException trustFailure;
        try {
            getDelegateTrustManager().checkServerTrusted(certs, alg);
            return;
        } catch (CertificateException e) {
            trustFailure = e;
        } catch (RuntimeException e) {
            // The JDK's own X509TrustManager implementation can throw an unchecked exception
            // (e.g. when there are no trust anchors at all) instead of a CertificateException.
            trustFailure = new CertificateException(e);
        }
        if (!"true".equals(UtilProperties.getPropertyValue("certificate", "server.all-trusted", "true"))) {
            throw trustFailure;
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> issuers = new LinkedList<>();
        for (KeyStore store: keystores) {
            try {
                Enumeration<String> e = store.aliases();
                while (e.hasMoreElements()) {
                    String alias = e.nextElement();
                    Certificate[] chain = store.getCertificateChain(alias);
                    if (chain != null) {
                        for (Certificate cert: chain) {
                            if (cert instanceof X509Certificate) {
                                if (Debug.verboseOn()) {
                                    Debug.logVerbose("Read certificate (chain) : " + ((X509Certificate) cert).getSubjectX500Principal().getName(),
                                            MODULE);
                                }
                                issuers.add((X509Certificate) cert);
                            }
                        }
                    } else {
                        Certificate cert = store.getCertificate(alias);
                        if (cert != null && cert instanceof X509Certificate) {
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Read certificate : " + ((X509Certificate) cert).getSubjectX500Principal().getName(), MODULE);
                            }
                            issuers.add((X509Certificate) cert);
                        }
                    }
                }
            } catch (KeyStoreException e) {
                Debug.logError(e, MODULE);
            }
        }

        return issuers.toArray(new X509Certificate[issuers.size()]);
    }

    /**
     * Builds a standard {@link X509TrustManager} backed by a {@link TrustManagerFactory}, initialized
     * with a KeyStore containing every trust anchor currently returned by {@link #getAcceptedIssuers()}
     * (i.e. the system truststore plus every configured component truststore, unchanged). Delegating to
     * this real trust manager gives genuine certificate path validation instead of a flat equality check.
     * @return the delegate trust manager
     */
    private X509TrustManager getDelegateTrustManager() throws CertificateException {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            int i = 0;
            for (X509Certificate issuer : getAcceptedIssuers()) {
                trustStore.setCertificateEntry("trust-anchor-" + i++, issuer);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    return (X509TrustManager) tm;
                }
            }
            throw new CertificateException("No X509TrustManager available from TrustManagerFactory");
        } catch (GeneralSecurityException | IOException e) {
            throw new CertificateException(e);
        }
    }
}
