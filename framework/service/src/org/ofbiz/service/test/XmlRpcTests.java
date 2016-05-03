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

package org.ofbiz.service.test;

import java.util.Locale;
import java.util.Map;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.ofbiz.base.start.Start;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;

/**
 * XmlRpcTests
 */
public class XmlRpcTests extends AbstractXmlRpcTestCase {

    public static final String module = XmlRpcTests.class.getName();
    public static final String resource = "ServiceErrorUiLabels";
    public static String url = "http://localhost:8080/webtools/control/xmlrpc";

    public XmlRpcTests(String name) {
        super(name);
        if (Start.getInstance().getConfig().portOffset != 0) {
            Integer port = 8080 + Start.getInstance().getConfig().portOffset;
            url = url.replace("8080", port.toString());
        }
    }

    /**
     * Test Xml Rpc by java class call with a Object List
     * @throws Exception
     */
    public void testXmlRpcRequest() throws Exception {
        XmlRpcClient client = this.getRpcClient(url, "admin", "ofbiz");
        Object[] params = new Object[] { 55.00, "message from xml-rpc client" };
        Map<String, Object> result = UtilGenerics.cast(client.execute("testScv", params));
        assertEquals("XML-RPC Service result success", "service done", result.get("resp"));
    }
    
    /**
     * Service to receive information from xml-rpc call
     */
    public static Map<String, Object> testXmlRpcAdd(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Integer num1 = (Integer) context.get("num1");
        Integer num2 = (Integer) context.get("num2");
        if (UtilValidate.isEmpty(num1) || UtilValidate.isEmpty(num2)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestXmlRpcMissingParameters", locale));
        }
        Integer res = num1 + num2;
        response.put("resulting", res);
        return response;
    }

    /**
     * Service to send information to xml-rpc service
     */    
    public static Map<String, Object> testXmlRpcClientAdd(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = null;
        Integer num1 = 125;
        Integer num2 = 365;
        try {
            Map<String, Object> localMap = dctx.makeValidContext("testXmlRpcLocalEngine", "IN", context);
            localMap.put("num1", num1);
            localMap.put("num2", num2);
            result = dctx.getDispatcher().runSync("testXmlRpcLocalEngine", localMap);
        }
        catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getLocalizedMessage());
        }
        if (ServiceUtil.isError(result)) return result;
        Integer res = (Integer) result.get("resulting");
        if (res == (num1 + num2)) { 
            result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "ServiceTestXmlRpcCalculationOK", locale) + res);
        } else {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestXmlRpcCalculationKO", locale));
        }
        return result;
    }
}
