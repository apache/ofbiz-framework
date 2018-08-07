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

package org.apache.ofbiz.accounting.thirdparty.securepay;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class SecurePayServiceTest extends OFBizTestCase{

    public SecurePayServiceTest(String name) {
        super(name);
    }
    
    public static final String module = SecurePayServiceTest.class.getName();

    // test data
    protected GenericValue emailAddr = null;
    protected String orderId = null;
    protected GenericValue creditCard = null;
    protected GenericValue billingAddress = null;
    protected GenericValue shippingAddress = null;
    protected BigDecimal creditAmount = null;
    protected String configFile = null;
    protected GenericValue orderPaymentPreference = null;
    protected List<Object> orderItems = null;
    protected Map<String, Object> orderItemMap = null;
    protected GenericValue billToParty = null;
    protected String paymentGatewayConfigId = null;
    protected BigDecimal refundAmount = null;
    protected GenericValue paymentGatewayResponse = null;
    protected String releaseRefNum = null;

    @Override
    protected void setUp() throws Exception {
        // populate test data
        configFile = "paymentTest.properties";
        creditAmount = new BigDecimal("234.51");
        emailAddr = delegator.makeValue("ContactMech", UtilMisc.toMap(
                "infoString","test@hansbakker.com"));
        orderId = "Demo1002";
        creditCard = delegator.makeValue("CreditCard", UtilMisc.toMap(
                "cardType","CCT_VISA",
                "expireDate","10/2011",  // mm/yyyy, gets converted to mm/yy
                "cardNumber","4444333322221111"));
        billingAddress = delegator.makeValue("PostalAddress", UtilMisc.toMap(
                "toName","The customer Name",
                "address1","The customer billingAddress1",
                "address2","The customer billingAddress2",
                "city","The customer city",
                "stateProvinceGeoId", "NLD"));
        shippingAddress = delegator.makeValue("PostalAddress", UtilMisc.toMap(
                "toName","The customer Name",
                "address1","The customer shippingStreet1",
                "address2","The customer shippingStreet2",
                "city","The customer city",
                "stateProvinceGeoId", "NLD",
                "postalCode","12345"));
        orderItemMap = UtilMisc.<String, Object>toMap(
                "orderId", "Demo1002", 
                "orderItemSeqId", "00001", 
                "orderItemTypeId", "PRODUCT_ORDER_ITEM", 
                "productId", "GZ-1000",
                "prodCatalogId", "DemoCatalog", 
                "quantity" , new BigDecimal("2.000000"), 
                "unitPrice", new BigDecimal("59.00"),
                "statusId" ,"ITEM_COMPLETED"
                );
        orderItems = UtilMisc.<Object>toList(orderItemMap);
        billToParty = delegator.makeValue("Party" , UtilMisc.toMap("partyId", "DemoCustomer"));
        paymentGatewayConfigId = "SECUREPAY_CONFIG";
        refundAmount = new BigDecimal("100.08");
        orderPaymentPreference = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap(
            "orderPaymentPreferenceId", "testOrder1000_01", 
            "orderId", "Demo1002", 
            "paymentMethodTypeId", "CREDIT_CARD", 
            "maxAmount", new BigDecimal("200.00"), 
            "statusId", "PAYMENT_AUTHORIZED"));
        
        GenericValue checkOrderPaymentPreference = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderPaymentPreferenceId", "testOrder1000_01").queryOne();
        if (UtilValidate.isEmpty(checkOrderPaymentPreference)) {
            orderPaymentPreference.create();
        }
    }

    public void testAuth() throws Exception{
        Debug.logInfo("=====[testAuth] starting....", module);
        try {
            Map<String, Object> serviceInput = UtilMisc.<String, Object>toMap(
                    "paymentConfig", configFile,
                    "billToParty", billToParty,
                    "billToEmail", emailAddr,
                    "orderPaymentPreference", orderPaymentPreference,
                    "orderItems", orderItems,
                    "creditCard", creditCard,
                    "billingAddress", billingAddress,
                    "shippingAddress", shippingAddress,
                    "orderId", orderId,
                    "currency", "AUD"
           );
            serviceInput.put("processAmount", new BigDecimal("100.08"));

            // run the service
            Map<String, Object> result = dispatcher.runSync("ofbScAuthorize",serviceInput);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testCCAuth] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("authResult")).equals(Boolean.FALSE)) {          // returnCode ok?
                Debug.logInfo("[testAuth] Error Messages from SecurePay: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            } else {
                Debug.logInfo("[testAuth] Result from SecurePay: " + result, module);
                String authRefNum = (String) result.get("authRefNum");
                BigDecimal processAmount =  (BigDecimal) result.get("processAmount");
                paymentGatewayResponse = delegator.makeValue("PaymentGatewayResponse" , UtilMisc.toMap(
                        "paymentGatewayResponseId", "testOrder1000_01",
                        "orderPaymentPreferenceId", "testOrder1000_01",
                        "amount" , processAmount,
                        "referenceNum", authRefNum,
                        "paymentMethodTypeId", "CREDIT_CARD",
                        "paymentServiceTypeEnumId", "PRDS_PAY_AUTH",
                        "currencyUomId", "AUD"
                        ));
                GenericValue checkPaymentGatewayResponse = EntityQuery.use(delegator).from("PaymentGatewayResponse").where("paymentGatewayResponseId", "testOrder1000_01").queryOne();
                if (UtilValidate.isEmpty(checkPaymentGatewayResponse)) {
                    paymentGatewayResponse.create();
                }
            }
        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testdoCapture() throws Exception {
        Debug.logInfo("=====[testdoCapture] starting....", module);
        GenericValue paymentGatewayResponse = EntityQuery.use(delegator).from("PaymentGatewayResponse").where("paymentGatewayResponseId", "testOrder1000_01").queryOne();
        try {
            Map<String, Object> serviceInput = UtilMisc.<String, Object>toMap(
                    "paymentConfig", configFile,
                    "orderPaymentPreference", orderPaymentPreference,
                    "authTrans", paymentGatewayResponse
           );
            serviceInput.put("captureAmount", refundAmount);

            // run the service
            Map<String, Object> result = dispatcher.runSync("ofbScCapture",serviceInput);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testdoCapture] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("captureResult")).equals(Boolean.FALSE)) {          // returnCode ok?
                Debug.logInfo("[testdoCapture] Error Messages from SecurePay: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            } else {
                String captureRefNum = (String) result.get("captureRefNum");
                GenericValue checkPaymentGatewayResponse = EntityQuery.use(delegator).from("PaymentGatewayResponse").where("paymentGatewayResponseId", "testOrder1000_01").queryOne();
                checkPaymentGatewayResponse.set("referenceNum", captureRefNum);
                checkPaymentGatewayResponse.store();
                Debug.logInfo("[testdoCapture] Result from SecurePay: " + result, module);
            }
            
        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testdoRefund() throws Exception {
        Debug.logInfo("=====[testdoRefund] starting....", module);
        try {
            Map<String, Object> serviceInput = UtilMisc.toMap(
                    "paymentConfig", configFile,
                    "orderPaymentPreference", orderPaymentPreference
           );
            serviceInput.put("refundAmount", refundAmount);
            // run the service
            Map<String, Object> result = dispatcher.runSync("ofbScRefund", serviceInput);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testdoRefund] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("refundResult")).equals(Boolean.FALSE)) {          // returnCode ok?
                Debug.logInfo("[testdoRefund] Error Messages from SecurePay: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            } else {
                Debug.logInfo("[testdoRefund] Result from SecurePay: " + result, module);
            }

        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    public void testdoCredit() throws Exception{
        Debug.logInfo("=====[testdoCredit] starting....", module);
        try {
            Map<String, Object> serviceInput = UtilMisc.toMap(
                    "paymentConfig", configFile,
                    "billToParty", billToParty,
                    "billToEmail", emailAddr,
                    "orderItems", orderItems,
                    "creditCard", creditCard,
                    "billingAddress", billingAddress,
                    "referenceCode", orderId,
                    "currency", "AUD"
           );
            serviceInput.put("creditAmount", creditAmount);
            // run the service
            Map<String, Object> result = dispatcher.runSync("ofbScCCCredit",serviceInput);
            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testdoCredit] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("creditResult")).equals(Boolean.FALSE)) {          // returnCode ok?
                Debug.logInfo("[testdoCredit] Error Messages from SecurePay: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            } else {
                Debug.logInfo("[testdoCredit] Result from SecurePay: " + result, module);
            }

        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
}
