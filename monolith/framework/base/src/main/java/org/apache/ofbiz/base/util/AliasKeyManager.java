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

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

/**
 * AliasKeyManager - KeyManager used to specify a certificate alias
 *
 */
public class AliasKeyManager implements X509KeyManager {
    private static final String MODULE = X509KeyManager.class.getName();

    private X509KeyManager keyManager = null;
    private String alias = null;

    protected AliasKeyManager() { }

    public AliasKeyManager(X509KeyManager keyManager, String alias) {
        this.keyManager = keyManager;
        this.alias = alias;
    }

    // this is where the customization comes in
    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        for (String keyType: keyTypes) {
            String[] aliases = keyManager.getClientAliases(keyType, null); // ignoring the issuers
            if (aliases != null && aliases.length > 0) {
                for (String alias: aliases) {
                    if (this.alias.equals(alias)) {
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("chooseClientAlias for keyType [" + keyType + "] got alias " + this.alias, MODULE);
                        }
                        return this.alias;
                    }
                }
            }
        }
        return null;
    }

    // these just pass through the keyManager
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    /**
     * Choose server alias string.
     * @param keyType the key type
     * @param socket the socket
     * @param issuers the issuers
     * @return the string
     */
    public String chooseServerAlias(String keyType, Socket socket, Principal... issuers) {
        return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        X509Certificate[] certArray = keyManager.getCertificateChain(alias);
        if (Debug.verboseOn()) {
            Debug.logVerbose("getCertificateChain for alias [" + alias + "] got " + certArray.length + " results", MODULE);
        }
        return certArray;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        PrivateKey pk = keyManager.getPrivateKey(alias);
        if (Debug.verboseOn()) {
            Debug.logVerbose("getPrivateKey for alias [" + alias + "] got " + (pk == null ? "[Not Found!]" : "[alg:" + pk.getAlgorithm()
                    + ";format:" + pk.getFormat() + "]"), MODULE);
        }
        return pk;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return keyManager.getServerAliases(keyType, issuers);
    }
}
