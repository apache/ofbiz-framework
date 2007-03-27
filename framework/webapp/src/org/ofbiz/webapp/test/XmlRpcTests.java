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

package org.ofbiz.webapp.test;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.LocalDispatcher;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClient;

import java.util.Map;
import java.net.URL;

import junit.framework.TestCase;

/**
 * XmlRpcTests
 */
public class XmlRpcTests extends TestCase {

    public static final String module = XmlRpcTests.class.getName();
    protected LocalDispatcher dispatcher = null;

    public XmlRpcTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        GenericDelegator delegator = GenericDelegator.getGenericDelegator("test");
        dispatcher = GenericDispatcher.getLocalDispatcher("test-dispatcher", delegator);
    }

    protected void tearDown() throws Exception {
    }

    public void testXmlRpcRequest() throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://localhost:8080/webtools/control/xmlrpc"));
        config.setBasicUserName("admin");
        config.setBasicPassword("ofbiz");

        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);

        Object[] params = new Object[] { new Double(55.00), "message from xml-rpc client" };
        Map result = (Map) client.execute("testScv", params);
        assertEquals("XML-RPC Service result success", ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE));        
    }
}
