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

package org.ofbiz.accounting.thirdparty.clearcommerce;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.lang.Thread;

import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

public class CCServicesTest extends TestCase {

    public static final String module = CCServicesTest.class.getName();
    public static final String DELEGATOR_NAME = "test";
    public GenericDelegator delegator = null;
    public static final String DISPATCHER_NAME = "test-dispatcher";
    public LocalDispatcher dispatcher = null;
    
    // test data
    protected GenericValue emailAddr = null;
    protected String orderId = null;
    protected GenericValue creditCard = null;
    protected GenericValue billingAddress = null;
    protected GenericValue shippingAddress = null;
    protected Map pbOrder = null;
    protected Double creditAmount = null;
    protected String configFile = null;
    
    public CCServicesTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        this.delegator = GenericDelegator.getGenericDelegator(DELEGATOR_NAME);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(DISPATCHER_NAME, delegator);

        // populate test data
        configFile = new String("paymentTest.properties");
        creditAmount = new Double(234.00);
        emailAddr = delegator.makeValue("ContactMech", UtilMisc.toMap(
                "infoString","test@hansbakker.com"));
        orderId = new String("testOrder1000");
        creditCard = delegator.makeValue("CreditCard", UtilMisc.toMap(
                "cardType","VISA",
                "expireDate","12/2008",  // mm/yyyy, gets converted to mm/yy
                "cardNumber","4111111111111111"));
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
        pbOrder = UtilMisc.toMap(
                "OrderFrequencyCycle", "M", 
                "OrderFrequencyInterval", "3", 
                "TotalNumberPayments", "4");
    }
    
    protected void tearDown() throws Exception {
        dispatcher.deregister();
    }

    /*
     * Check the authorisation
     */
    public void testAuth() throws Exception{
        Debug.logInfo("=====[testAuth] starting....", module);
        try {
            Map serviceInput = UtilMisc.toMap(
                    "paymentConfig", configFile,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "billingAddress", billingAddress,
                    "shippingAddress", shippingAddress,
                    "orderId", orderId
            );
            serviceInput.put("processAmount", new Double(200.00));
            
            // run the service (make sure in payment
            Map result = dispatcher.runSync("clearCommerceCCAuth",serviceInput);
            
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
            Map serviceMap = UtilMisc.toMap(
                    "paymentConfig", configFile,
                    "orderId", orderId,
                    "creditAmount", creditAmount,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "creditAmount", new Double(200.00)
            );
            // run the service
            Map result = dispatcher.runSync("clearCommerceCCCredit",serviceMap);
            
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
            
            Map serviceMap = UtilMisc.toMap(
                    "paymentConfig", configFile,
                    "orderId", orderId,
                    "creditAmount", creditAmount,
                    "billToEmail", emailAddr,
                    "creditCard", creditCard,
                    "pbOrder", pbOrder          // if supplied, the crediting is for a subscription and credit by period is managed by ClearCommerce                
            );
            serviceMap.put("creditAmount", new Double(200.00));

            // run the service
            Map result = dispatcher.runSync("clearCommerceCCCredit",serviceMap);
            
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
            
            Map serviceMap = UtilMisc.toMap(
                    "orderId", "4488668f-2db0-3002-002b-0003ba1d84d5",
                    "paymentConfig", configFile
            );
            
            // run the service
            Map result = dispatcher.runSync("clearCommerceCCReport",serviceMap);
            
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
