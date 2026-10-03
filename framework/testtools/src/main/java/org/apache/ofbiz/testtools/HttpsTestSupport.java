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
package org.apache.ofbiz.testtools;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.KeyStoreUtil;

/**
 * Helper for tests that call the HTTPS port of the OFBiz instance they run in.
 */
public final class HttpsTestSupport {

    private static final String SERVER_KEYSTORE = "framework/base/config/ofbizssl.jks";
    private static final String SERVER_KEYSTORE_PASSWORD = "changeit";

    private HttpsTestSupport() { }

    /**
     * Makes the client accept the certificate of the local OFBiz server, which is self-signed and not issued
     * for localhost, and nothing else: the server certificate is still validated, against the keystore the
     * server uses, and only the host name localhost is accepted.
     * @param client the client to set up
     * @throws IOException if the server keystore cannot be read
     * @throws GeneralSecurityException if the server keystore cannot be loaded
     */
    public static void trustLocalServer(HttpClient client) throws IOException, GeneralSecurityException {
        File keystoreFile = new File(System.getProperty("ofbiz.home"), SERVER_KEYSTORE);
        KeyStore serverKeyStore = KeyStoreUtil.getStore(keystoreFile.toURI().toURL(), SERVER_KEYSTORE_PASSWORD);
        client.setTrustStore(serverKeyStore);
        client.setHostnameVerifier((hostname, session) -> "localhost".equals(hostname));
    }
}
