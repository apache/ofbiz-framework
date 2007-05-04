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

package org.ofbiz.base.util;

import javolution.util.FastList;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.List;
import java.util.Iterator;
import java.util.Enumeration;

/**
 * MultiTrustManager
 */
public class MultiTrustManager implements X509TrustManager {

    public static final String module = MultiTrustManager.class.getName();

    protected List keystores;

    public MultiTrustManager(KeyStore ks) {
        this();
        keystores.add(ks);
    }

    public MultiTrustManager() {
        keystores = FastList.newInstance();
    }

    public void add(KeyStore ks) {        
        if (ks != null) {
            keystores.add(ks);
        }
    }

    public void checkClientTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        if (!isTrusted(certs)) {
            throw new CertificateException("No trusted certificate found");
        }
    }

    public void checkServerTrusted(X509Certificate[] certs, String alg) throws CertificateException {
        if (!isTrusted(certs)) {
            throw new CertificateException("No trusted certificate found");
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        List certs = FastList.newInstance();
        Iterator i = keystores.iterator();
        while (i.hasNext()) {
            KeyStore k = (KeyStore) i.next();
            try {
                Enumeration e = k.aliases();
                while (e.hasMoreElements()) {
                    String alias = (String) e.nextElement();
                    Certificate[] cert = k.getCertificateChain(alias);
                    if (cert != null) {
                        for (int x = 0; x < cert.length; x++) {
                            if (cert[x] instanceof X509Certificate) {
                                if (Debug.verboseOn())
                                    Debug.log("Read certificate (chain) : " + ((X509Certificate) cert[x]).getSubjectX500Principal().getName(), module);
                                certs.add(cert[x]);
                            }
                        }
                    } else {
                        Certificate c = k.getCertificate(alias);
                        if (c != null && c instanceof X509Certificate) {
                            if (Debug.verboseOn())
                                Debug.log("Read certificate : " + ((X509Certificate) c).getSubjectX500Principal().getName(), module);
                            certs.add(c);
                        }
                    }
                }
            } catch (KeyStoreException e) {
                Debug.logError(e, module);
            }
        }

        return (X509Certificate[]) certs.toArray(new X509Certificate[certs.size()]);
    }

    protected boolean isTrusted(X509Certificate[] cert) {
        if (cert != null) {
            X509Certificate[] certs = this.getAcceptedIssuers();
            if (certs != null) {
                for (int i = 0; i < certs.length; i++) {
                    for (int x = 0; x < cert.length; x++) {
                        if (Debug.verboseOn())
                            Debug.log("--- Checking cert: " + certs[i].getSubjectX500Principal() + " vs " + cert[x].getSubjectX500Principal(), module);
                        if (certs[i].equals(cert[x])) {
                            if (Debug.verboseOn())
                                Debug.log("--- Found trusted cert: " + certs[i].getSerialNumber().toString(16) + " : " + certs[i].getSubjectX500Principal(), module);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
