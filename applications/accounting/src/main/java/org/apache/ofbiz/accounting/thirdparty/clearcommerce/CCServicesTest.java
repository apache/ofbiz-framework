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

package org.apache.ofbiz.accounting.thirdparty.clearcommerce;

import java.math.BigDecimal;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class CCServicesTest extends OFBizTestCase {

    public static final String module = CCServicesTest.class.getName();

    // test data
    protected GenericValue emailAddr = null;
    protected String orderId = null;
    protected GenericValue creditCard = null;
    protected GenericValue billingAddress = null;
    protected GenericValue shippingAddress = null;
    protected Map<String, Object> pbOrder = null;
    protected BigDecimal creditAmount = null;
    protected String configFile = null;

    public CCServicesTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        // populate test data
        configFile = "paymentTest.properties";
        creditAmount = new BigDecimal("234.00");
        emailAddr = delegator.makeValue("ContactMech", UtilMisc.toMap("infoString","test@hansbakker.com"));
        orderId = "testOrder1000";
        creditCard = delegator.makeValue("CreditCard", UtilMisc.toMap("cardType","CCT_VISA",
                "expireDate","12/2008",  // mm/yyyy, gets converted to mm/yy
                "cardNumber","4111111111111111"));
        billingAddress = delegator.makeValue("PostalAddress", UtilMisc.toMap("toName","The customer Name",
                "address1","The customer billingAddress1",
                "address2","The customer billingAddress2",
                "city","The customer city",
                "stateProvinceGeoId", "NLD"));
        shippingAddress = delegator.makeValue("PostalAddress", UtilMisc.toMap("toName","The customer Name",
                "address1","The customer shippingStreet1",
                "address2","The customer shippingStreet2",
                "city","The customer city",
                "stateProvinceGeoId", "NLD",
                "postalCode","12345"));
        pbOrder = UtilMisc.<String, Object>toMap("OrderFrequencyCycle", "M",
                "OrderFrequencyInterval", "3",
                "TotalNumberPayments", "4");
    }

    /*
     * Check the authorisation
     */
    public void testAuth() throws Exception{
        Debug.logInfo("=====[testAuth] starting....", module);
        try {
            Map<String, Object> serviceInput = UtilMisc.<String, Object>toMap("paymentConfig", configFile,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "billingAddress", billingAddress,
                    "shippingAddress", shippingAddress,
                    "orderId", orderId
           );
            serviceInput.put("processAmount", new BigDecimal("200.00"));

            // run the service (make sure in payment
            Map<String, Object> result = dispatcher.runSync("clearCommerceCCAuth",serviceInput);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testCCAuth] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("authResult")).equals(new Boolean(false))) {          // returnCode ok?
                Debug.logInfo("[testAuth] Error Messages from ClearCommerce: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            }

        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }

    }
    /*
     * Check the credit action: to deduct a certain amount of a credit card.
     */
    public void testCredit() throws Exception{
        Debug.logInfo("=====[testCCredit] starting....", module);
        try {
            Map<String, Object> serviceMap = UtilMisc.<String, Object>toMap("paymentConfig", configFile,
                    "orderId", orderId,
                    "creditAmount", creditAmount,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "creditAmount", new BigDecimal("200.00"));
            // run the service
            Map<String, Object> result = dispatcher.runSync("clearCommerceCCCredit",serviceMap);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testCCCredit] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

            if (((Boolean) result.get("creditResult")).equals(new Boolean(false))) {          // returnCode ok?
                Debug.logInfo("[testCCCredit] Error Messages from ClearCommerce: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            }
        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }

    }
    /*
     * Test Purchase subscription
     */
    public void testPurchaseSubscription() throws Exception {
        Debug.logInfo("=====[testPurchaseSubscription] starting....", module);
        try {
            Map<String, Object> serviceMap = UtilMisc.<String, Object>toMap("paymentConfig", configFile,
                    "orderId", orderId,
                    "creditAmount", creditAmount,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "pbOrder", pbOrder);  // if supplied, the crediting is for a subscription and credit by period is managed by ClearCommerce
                       
            serviceMap.put("creditAmount", new BigDecimal("200.00"));

            // run the service
            Map<String, Object> result = dispatcher.runSync("clearCommerceCCCredit", serviceMap);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testPurchaseDescription] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);  // service completed ok?
            if (((Boolean) result.get("creditResult")).equals(new Boolean(false))) {          // returnCode ok?
                Debug.logInfo("[testPurchaseSubscription] Error Messages from ClearCommerce: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            }
        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    /*
     * Test Free subscription
     */
    public void testFreeSubscription() throws Exception {

            // not communicate with CC.
    }
    /*
     * Test cancel subscription
     */
    public void testCancelSubscription() throws Exception {
       /* from the API doc:
       After the Engine receives and processes an internally-managed periodic billing order, the
order cannot be modified. An order can only be cancelled. If, for example, the credit card
associated with a recurring order expires and a payment is rejected, the order must be
cancelled. If the order is to be resumed, a new recurring order must be submitted.
--> Orders are cancelled by using the Store Administrator Tool.

    So cannot by program.
       */
    }
    /*
     * Test Query subscription transaction status
     */
    public void testCCReport() throws Exception{
        Debug.logInfo("=====[testReport] starting....", module);
        try {

            Map<String, Object> serviceMap = UtilMisc.<String, Object>toMap("orderId", "4488668f-2db0-3002-002b-0003ba1d84d5",
                    "paymentConfig", configFile);

            // run the service
            Map<String, Object> result = dispatcher.runSync("clearCommerceCCReport",serviceMap);

            // verify the results
            String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
            Debug.logInfo("[testPurchaseDescription] responseMessage: " + responseMessage, module);
            TestCase.assertEquals("Reporting service", ModelService.RESPOND_SUCCESS, responseMessage);  // service completed ok?
            if (((Boolean) result.get("creditResult")).equals(new Boolean(false))) {          // returnCode ok?
                Debug.logInfo("[testReport] Error Messages from ClearCommerce: " + result.get("internalRespMsgs"), module);
                TestCase.fail("Returned messages:" + result.get("internalRespMsgs"));
            }
        } catch (GenericServiceException ex) {
            TestCase.fail(ex.getMessage());
        }
    }
}
