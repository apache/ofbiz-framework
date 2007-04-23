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

import org.apache.commons.codec.binary.Base64;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;

import javax.security.auth.x500.X500Principal;

/**
 * KeyStoreUtil - Utilities for getting KeyManagers and TrustManagers
 *
 */
public class KeyStoreUtil {

    public static final String module = KeyStoreUtil.class.getName();

    public static void storeComponentKeyStore(String componentName, String keyStoreName, KeyStore store) throws IOException, GenericConfigException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        ComponentConfig.KeystoreInfo ks = ComponentConfig.getKeystoreInfo(componentName, keyStoreName);
        File file = new File(ks.createResourceHandler().getFullLocation());
        FileOutputStream out = new FileOutputStream(file);
        store.store(out, ks.getPassword().toCharArray());
    }

    public static KeyStore getComponentKeyStore(String componentName, String keyStoreName) throws IOException, GeneralSecurityException, GenericConfigException {
        ComponentConfig.KeystoreInfo ks = ComponentConfig.getKeystoreInfo(componentName, keyStoreName);
        return getStore(ks.createResourceHandler().getURL(), ks.getType(), ks.getPassword());
    }

    public static KeyStore getStore(URL url, String password) throws IOException, GeneralSecurityException {
        return getStore(url, password, "jks");
    }

    public static KeyStore getStore(URL url, String password, String type) throws IOException, GeneralSecurityException {
        if (type == null) {
            throw new IOException("Invalid keystore type; null");
        }
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(url.openStream(), password.toCharArray());
        return ks;
    }

    public static KeyStore getSystemTrustStore() throws IOException, GeneralSecurityException {
        String fileName = System.getProperty("javax.net.ssl.trustStore");
        String password = System.getProperty("javax.net.ssl.trustStorePassword");
        if (fileName != null && password != null) {
            File file = new File(fileName);
            if (file.exists() && file.canRead()) {
                KeyStore ks = KeyStore.getInstance("jks");
                ks.load(new FileInputStream(file), password.toCharArray());
                return ks;
            }
        }
        return null;
    }

    public static X509Certificate readCertificate(byte[] certChain) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bais = new ByteArrayInputStream(certChain);
        return (X509Certificate) cf.generateCertificate(bais);
    }

    public static Map getCertX500Map(X509Certificate cert) {
        X500Principal x500 = cert.getSubjectX500Principal();
        Map x500Map = FastMap.newInstance();

        String[] x500Opts = x500.getName().split("\\,");
        for (int x = 0; x < x500Opts.length; x++) {
            String[] nv = x500Opts[x].split("\\=");
            x500Map.put(nv[0], nv[1]);
        }

        return x500Map;
    }
    
    public static void importPKCS8CertChain(KeyStore ks, String alias, byte[] keyBytes, String keyPass, byte[] certChain) throws InvalidKeySpecException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        // load the private key
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey pk = kf.generatePrivate(keysp);

        // load the cert chain
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bais = new ByteArrayInputStream(certChain);

        Collection certCol = cf.generateCertificates(bais);
        Certificate[] certs = new Certificate[certCol.toArray().length];
        if (certCol.size() == 1) {
            Debug.log("Single certificate; no chain", module);
            bais = new ByteArrayInputStream(certChain);
            Certificate cert = cf.generateCertificate(bais);
            certs[0] = cert;
        } else {
            Debug.log("Certificate chain length : " + certCol.size(), module);
            certs = (Certificate[]) certCol.toArray();
        }

        ks.setKeyEntry(alias, pk, keyPass.toCharArray(), certs);
    }

    public static String certToString(Certificate cert) throws CertificateEncodingException {
        byte[] certBuf = cert.getEncoded();
        StringBuffer buf = new StringBuffer();
        buf.append("-----BEGIN CERTIFICATE-----\n");
        buf.append(new String(Base64.encodeBase64Chunked(certBuf)));
        buf.append("\n-----END CERTIFICATE-----\n");
        return buf.toString();
    }

    public static Certificate pemToCert(String certString) throws IOException, CertificateException {
        return pemToCert(new StringReader(certString));    
    }

    public static Certificate pemToCert(File certFile) throws IOException, CertificateException {
        return pemToCert(new FileInputStream(certFile));
    }

    public static Certificate pemToCert(InputStream is) throws IOException, CertificateException {
        return pemToCert(new InputStreamReader(is));
    }

    public static Certificate pemToCert(Reader r) throws IOException, CertificateException {
        String header = "-----BEGIN CERTIFICATE-----";
        String footer = "-----END CERTIFICATE-----";

        BufferedReader reader = new BufferedReader(r);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        String line;

        // ignore up to the header
        while ((line = reader.readLine()) != null && !line.equals(header)) {
        }

        // no header found
        if (line == null) {
            throw new IOException("Error reading certificate, missing BEGIN boundary");
        }

        // in between the header and footer is the actual certificate
        while ((line = reader.readLine()) != null && !line.equals(footer)) {
            line = line.replaceAll("\\s", "");
            ps.print(line);
        }

        // no footer found
        if (line == null) {
            throw new IOException("Error reading certificate, missing END boundary");
        }
        ps.close();

        // decode the buffer to a X509Certificate

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = Base64.decodeBase64(baos.toByteArray());
        return cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public static String pemToPkHex(String certString) throws IOException, CertificateException {
        Certificate cert = pemToCert(certString);
        return StringUtil.toHexString(cert.getPublicKey().getEncoded());
    }
}
