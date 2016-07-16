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

package org.apache.ofbiz.service.test;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.ofbiz.service.xmlrpc.XmlRpcClient;

/**
 * AbstractXmlRpcTestCase
 */
public class AbstractXmlRpcTestCase extends TestCase {

    public static final String module = AbstractXmlRpcTestCase.class.getName();

    protected String keyStoreComponent;
    protected String keyStoreName;
    protected String keyAlias;

    public AbstractXmlRpcTestCase(String name, String keyStoreComponent, String keyStoreName, String keyAlias) {
        super(name);
        this.keyStoreComponent = keyStoreComponent;
        this.keyStoreName = keyStoreName;
        this.keyAlias = keyAlias;
    }

    public AbstractXmlRpcTestCase(String name) {
        super(name);
        this.keyStoreComponent = null;
        this.keyStoreName = null;
        this.keyAlias = null;
    }


    public org.apache.xmlrpc.client.XmlRpcClient getRpcClient(String url) throws MalformedURLException {
        return getRpcClient(url, null, null);
    }

    public org.apache.xmlrpc.client.XmlRpcClient getRpcClient(String url, String login, String password) throws MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url));
        if (login != null) {
            config.setBasicUserName(login);
        }
        if (password != null) {
            config.setBasicPassword(password);
        }

        if (keyStoreComponent != null && keyStoreName != null && keyAlias != null) {
            return new XmlRpcClient(config, keyStoreComponent, keyStoreName, keyAlias);
        } else {
            return new XmlRpcClient(config);
        }
    }
}
