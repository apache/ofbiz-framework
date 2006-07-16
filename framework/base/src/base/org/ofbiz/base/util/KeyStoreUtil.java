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

import java.io.*;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;

/**
 * KeyStoreUtil - Utilities for getting KeyManagers and TrustManagers
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class KeyStoreUtil {

    public static final String module = KeyStoreUtil.class.getName();

    public static String getKeyStoreFileName() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.client.keyStore", null);
    }

    public static String getKeyStorePassword() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.client.keyStore.password", null);
    }

    public static String getKeyStoreType() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.client.keyStore.type", "jks");
    }

    public static String getTrustStoreFileName() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.trustStore", null);
    }

    public static String getTrustStorePassword() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.trustStore.password", null);
    }

    public static String getTrustStoreType() {
        return UtilProperties.getPropertyValue("jsse.properties", "ofbiz.trustStore.type", "jks");
    }

    public static KeyStore getKeyStore() throws IOException, GeneralSecurityException {
        if (getKeyStoreFileName() != null && !keyStoreExists(getKeyStoreFileName())) {
            return null;
        }
        FileInputStream fis = new FileInputStream(getKeyStoreFileName());
        KeyStore ks = KeyStore.getInstance(getKeyStoreType());
        ks.load(fis, getKeyStorePassword().toCharArray());
        fis.close();
        return ks;
    }

    public static void saveKeyStore(KeyStore ks) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        ks.store(new FileOutputStream(getKeyStoreFileName()), getKeyStorePassword().toCharArray());
    }

    public static KeyStore getTrustStore() throws IOException, GeneralSecurityException {
        if (getTrustStoreFileName() != null && !keyStoreExists(getTrustStoreFileName())) {
            return null;
        }
        FileInputStream fis = new FileInputStream(getTrustStoreFileName());
        KeyStore ks = KeyStore.getInstance(getTrustStoreType());
        ks.load(fis, getTrustStorePassword().toCharArray());
        fis.close();
        return ks;
    }

    public static void saveTrustStore(KeyStore ks) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        ks.store(new FileOutputStream(getTrustStoreFileName()), getTrustStorePassword().toCharArray());
    }

    public static boolean keyStoreExists(String fileName) {
        File keyFile = new File(fileName);
        return keyFile.exists();
    }

    public static KeyStore createKeyStore(String fileName, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore ks = null;
        ks = KeyStore.getInstance("jks");
        ks.load(null, password.toCharArray());
        ks.store(new FileOutputStream(fileName), password.toCharArray());
        ks.load(new FileInputStream(fileName), password.toCharArray());
        return ks;
    }

    public static void renameKeyStoreEntry(String fromAlias, String toAlias) throws GeneralSecurityException, IOException {
        KeyStore ks = getKeyStore();
        String pass = getKeyStorePassword();
        renameEntry(ks, pass, fromAlias, toAlias);
        saveKeyStore(ks);
    }

    private static void renameEntry(KeyStore ks, String pass, String fromAlias, String toAlias) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        if (ks.isKeyEntry(fromAlias)) {
            Key fromKey = ks.getKey(fromAlias, pass.toCharArray());
            if (fromKey instanceof PrivateKey) {
                Certificate[] certs = ks.getCertificateChain(fromAlias);
                ks.deleteEntry(fromAlias);
                ks.setKeyEntry(toAlias, fromKey, pass.toCharArray(), certs);
            }
        } else if (ks.isCertificateEntry(fromAlias)) {
            Certificate cert = ks.getCertificate(fromAlias);
            ks.deleteEntry(fromAlias);
            ks.setCertificateEntry(toAlias, cert);
        }
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

    // key pair generation methods
    public static KeyPair createDHKeyPair() throws Exception {
        AlgorithmParameterGenerator apGen = AlgorithmParameterGenerator.getInstance("DH");
        apGen.init(1024);

        AlgorithmParameters algParams = apGen.generateParameters();
        DHParameterSpec dhParamSpec = (DHParameterSpec) algParams.getParameterSpec(DHParameterSpec.class);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParamSpec);

        KeyPair keypair = keyGen.generateKeyPair();
        return keypair;
    }

    public static KeyPair getKeyPair(String alias, String password) throws Exception {
        KeyStore ks = getKeyStore();
        Key key = ks.getKey(alias, password.toCharArray());
        if (key instanceof PrivateKey) {
            Certificate cert = ks.getCertificate(alias);
            PublicKey publicKey = cert.getPublicKey();
            return new KeyPair(publicKey, (PrivateKey) key);
        } else {
            Debug.logError("Key is not an instance of PrivateKey", module);
        }
        return null;
    }

    public static void storeCertificate(String alias, Certificate cert) throws Exception {
        KeyStore ks = getKeyStore();
        ks.setCertificateEntry(alias, cert);
        ks.store(new FileOutputStream(getKeyStoreFileName()), getKeyStorePassword().toCharArray());
    }

    public static void storeKeyPair(KeyPair keyPair, String alias, String password) throws Exception {
        KeyStore ks = getKeyStore();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        // not sure what to do here. Do we need to create a cert to assoc with the private key?
        // cannot find methods for just setting the private/public key; missing something
        ks.store(new FileOutputStream(getKeyStoreFileName()), getKeyStorePassword().toCharArray());
    }

    public static String certToString(Certificate cert) throws CertificateEncodingException {
        byte[] certBuf = cert.getEncoded();
        StringBuffer buf = new StringBuffer();
        buf.append("-----BEGIN CERTIFICATE-----\n");
        buf.append(Base64.base64Encode(certBuf));
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
            continue;
        }

        // no header found
        if (line == null) {
            throw new IOException("Error reading certificate, missing BEGIN boundary");
        }

        // in between the header and footer is the actual certificate
        while ((line = reader.readLine()) != null && !line.equals(footer)) {
            ps.print(line);
        }

        // no footer found
        if (line == null) {
            throw new IOException("Error reading certificate, missing END boundary");
        }
        ps.close();

        // decode the buffer to a X509Certificate

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = Base64.base64Decode(baos.toByteArray());
        return cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public static SecretKey generateSecretKey(PrivateKey ourKey, PublicKey theirKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(ourKey);
        ka.doPhase(theirKey, true);
        return ka.generateSecret("TripleDES");
    }

    public static PublicKey readDHPublicKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        return keyFactory.generatePublic(x509KeySpec);
    }

    public static PrivateKey readDHPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        return keyFactory.generatePrivate(x509KeySpec);
    }

}
