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

import org.apache.xmlrpc.client.XmlRpcClient;
import org.ofbiz.base.util.UtilGenerics;

import java.util.Map;

/**
 * XmlRpcTests
 */
public class XmlRpcTests extends AbstractXmlRpcTestCase {

    public static final String module = XmlRpcTests.class.getName();
    public static final String url = "http://localhost:8080/webtools/control/xmlrpc";

    public XmlRpcTests(String name) {
        super(name);
    }

    public void testXmlRpcRequest() throws Exception {
        XmlRpcClient client = this.getRpcClient(url, "admin", "ofbiz");
        Object[] params = new Object[] { 55.00, "message from xml-rpc client" };
        Map<String, Object> result = UtilGenerics.cast(client.execute("testScv", params));
        assertEquals("XML-RPC Service result success", "service done", result.get("resp"));
    }
}
