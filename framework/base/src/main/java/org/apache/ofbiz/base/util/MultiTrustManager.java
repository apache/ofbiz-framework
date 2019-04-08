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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.X509TrustManager;

/**
 * MultiTrustManager
 */
public class MultiTrustManager implements X509TrustManager {

    public static final String module = MultiTrustManager.class.getName();

    protected List<KeyStore> keystores;

    public MultiTrustManager(KeyStore ks) {
        this();
        keystores.add(ks);
    }

    public MultiTrustManager() {
        keystores = new LinkedList<KeyStore>();
    }

    public void add(KeyStore ks) {
        if (ks != null) {
            keystores.add(ks);
        }
    }

    public int getNumberOfKeyStores() {
        return keystores.size();
    }

    public void checkClientTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        if (isTrusted(certs)) {
            return;
        }
        if (!"true".equals(UtilProperties.getPropertyValue("certificate", "client.all-trusted", "true"))) {
            throw new CertificateException("No trusted certificate found");
        }
    }

    public void checkServerTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        if (isTrusted(certs)) {
            return;
        }
        if (!"true".equals(UtilProperties.getPropertyValue("certificate", "server.all-trusted", "true"))) {
            throw new CertificateException("No trusted certificate found");
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> issuers = new LinkedList<X509Certificate>();
        for (KeyStore store: keystores) {
            try {
                Enumeration<String> e = store.aliases();
                while (e.hasMoreElements()) {
                    String alias = e.nextElement();
                    Certificate[] chain = store.getCertificateChain(alias);
                    if (chain != null) {
                        for (Certificate cert: chain) {
                            if (cert instanceof X509Certificate) {
                                if (Debug.verboseOn())
                                    Debug.logInfo("Read certificate (chain) : " + ((X509Certificate) cert).getSubjectX500Principal().getName(), module);
                                issuers.add((X509Certificate) cert);
                            }
                        }
                    } else {
                        Certificate cert = store.getCertificate(alias);
                        if (cert != null && cert instanceof X509Certificate) {
                            if (Debug.verboseOn())
                                Debug.logInfo("Read certificate : " + ((X509Certificate) cert).getSubjectX500Principal().getName(), module);
                            issuers.add((X509Certificate) cert);
                        }
                    }
                }
            } catch (KeyStoreException e) {
                Debug.logError(e, module);
            }
        }

        return issuers.toArray(new X509Certificate[issuers.size()]);
    }

    protected boolean isTrusted(X509Certificate[] cert) {
        if (cert != null) {
            X509Certificate[] issuers = this.getAcceptedIssuers();
            if (issuers != null) {
                for (X509Certificate issuer: issuers) {
                    for (X509Certificate c: cert) {
                        if (Debug.verboseOn())
                            Debug.logInfo("--- Checking cert: " + issuer.getSubjectX500Principal() + " vs " + c.getSubjectX500Principal(), module);
                        if (issuer.equals(c)) {
                            if (Debug.verboseOn())
                                Debug.logInfo("--- Found trusted cert: " + issuer.getSerialNumber().toString(16) + " : " + issuer.getSubjectX500Principal(), module);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
