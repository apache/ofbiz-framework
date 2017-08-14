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

package org.apache.ofbiz.service.xmlrpc;

import java.security.KeyStore;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.KeyStoreUtil;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;

/**
 * XmlRpcClient
 */
public class XmlRpcClient extends org.apache.xmlrpc.client.XmlRpcClient {

    public static final String module = XmlRpcClient.class.getName();

    protected String keyStoreComponent;
    protected String keyStoreName;
    protected String keyAlias;

    public XmlRpcClient(XmlRpcClientConfig config, String keyStoreComponent, String keyStoreName, String keyAlias) {
        this(config);
        this.keyStoreComponent = keyStoreComponent;
        this.keyStoreName = keyStoreName;
        this.keyAlias = keyAlias;
        this.setTransportFactory(this.getClientTransportFactory());
    }

    public XmlRpcClient(XmlRpcClientConfig config) {
        super();
        this.setConfig(config);
    }

    public XmlRpcTransportFactory getClientTransportFactory() {
        if (keyStoreComponent == null || keyStoreName == null || keyAlias == null) {
            return this.getTransportFactory();
        }

        ComponentConfig.KeystoreInfo ks = ComponentConfig.getKeystoreInfo(keyStoreComponent, keyStoreName);
        KeyStore keyStore = null;
        try {
            keyStore = KeyStoreUtil.getStore(ks.createResourceHandler().getURL(), ks.getPassword(), ks.getType());
        } catch (Exception e) {
            Debug.logError(e, "Unable to load keystore: " + keyStoreName, module);
        }

        return new AliasSupportedTransportFactory(this, keyStore, ks.getPassword(), keyAlias);
    }
}
