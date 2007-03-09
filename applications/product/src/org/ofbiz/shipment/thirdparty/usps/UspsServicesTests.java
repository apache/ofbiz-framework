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

package org.ofbiz.shipment.thirdparty.usps;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;


/**
 * Tests for USPS Webtools API services
 *
 * These were created for simple validation only.
 */
public class UspsServicesTests extends TestCase {

    public static String module = UspsServicesTests.class.getName();

    public static final String DELEGATOR_NAME = "test";
    public static final String DISPATCHER_NAME = "test-dispatcher";

    private GenericDelegator delegator = null;
    private LocalDispatcher dispatcher = null;


    public UspsServicesTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        delegator = GenericDelegator.getGenericDelegator(DELEGATOR_NAME);
        dispatcher = GenericDispatcher.getLocalDispatcher(DISPATCHER_NAME, delegator);
    }

    protected void tearDown() throws Exception {
        dispatcher.deregister();
    }

    public void testUspsTrackConfirm() throws Exception {

        // run the service
        Map result = dispatcher.runSync("uspsTrackConfirm", UtilMisc.toMap("trackingId", "EJ958083578US"));

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsTrackConfirm] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String trackingSummary = (String) result.get("trackingSummary");
        Debug.log("[testUspsTrackConfirm] trackingSummary: " + trackingSummary, module);
        assertEquals("trackingSummary is correct",
                "Your item was delivered at 8:10 am on June 1 in Wilmington DE 19801.", trackingSummary);

        List trackingDetailList = (List) result.get("trackingDetailList");
        assertEquals("trackingDetailList has 3 elements", 3, trackingDetailList.size());

        Debug.log("[testUspsTrackConfirm] trackingDetailList[0]: " + trackingDetailList.get(0), module);
        assertEquals("trackingDetailList element 0 is correct",
                "May 30 11:07 am NOTICE LEFT WILMINGTON DE 19801.", trackingDetailList.get(0));

        Debug.log("[testUspsTrackConfirm] trackingDetailList[1]: " + trackingDetailList.get(1), module);
        assertEquals("trackingDetailList element 0 is correct",
                "May 30 10:08 am ARRIVAL AT UNIT WILMINGTON DE 19850.", trackingDetailList.get(1));

        Debug.log("[testUspsTrackConfirm] trackingDetailList[2]: " + trackingDetailList.get(2), module);
        assertEquals("trackingDetailList element 0 is correct",
                "May 29 9:55 am ACCEPT OR PICKUP EDGEWATER NJ 07020.", trackingDetailList.get(2));
    }

    public void testUspsAddressValidation() throws Exception {

        // run the service
        Map result = dispatcher.runSync("uspsAddressValidation",
                UtilMisc.toMap("address1", "6406 Ivy Lane", "city", "Greenbelt", "state", "MD"));

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsAddressValidation] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String address1 = (String) result.get("address1");
        Debug.log("[testUspsAddressValidation] address1: " + address1, module);
        assertEquals("address1 is correct", "6406 IVY LN", address1);

        String city = (String) result.get("city");
        Debug.log("[testUspsAddressValidation] city: " + city, module);
        assertEquals("city is correct", "GREENBELT", city);

        String state = (String) result.get("state");
        Debug.log("[testUspsAddressValidation] state: " + state, module);
        assertEquals("state is correct", "MD", state);

        String zip5 = (String) result.get("zip5");
        Debug.log("[testUspsAddressValidation] zip5: " + zip5, module);
        assertEquals("zip5 is correct", "20770", zip5);

        String zip4 = (String) result.get("zip4");
        Debug.log("[testUspsAddressValidation] zip4: " + zip4, module);
        assertEquals("zip4 is correct", "1440", zip4);
    }

    public void testUspsCityStateLookup() throws Exception {

        // run the service
        Map result = dispatcher.runSync("uspsCityStateLookup", UtilMisc.toMap("zip5", "90210"));

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsCityStateLookup] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String city = (String) result.get("city");
        Debug.log("[testUspsCityStateLookup] city: " + city, module);
        assertEquals("city is correct", "BEVERLY HILLS", city);

        String state = (String) result.get("state");
        Debug.log("[testUspsCityStateLookup] state: " + state, module);
        assertEquals("state is correct", "CA", state);
    }

    public void testUspsPriorityMailStandard() throws Exception {

        // run the service
        Map result = dispatcher.runSync("uspsPriorityMailStandard", UtilMisc.toMap("originZip", "4", "destinationZip", "4"));

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsPriorityMailStandard] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String days = (String) result.get("days");
        Debug.log("[testUspsPriorityMailStandard] days: " + days, module);
        assertEquals("days is correct", "1", days);
    }

    public void testUspsPackageServicesStandard() throws Exception {

        // run the service
        Map result = dispatcher.runSync("uspsPackageServicesStandard", UtilMisc.toMap("originZip", "4", "destinationZip", "4"));

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsPackageServicesStandard] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String days = (String) result.get("days");
        Debug.log("[testUspsPackageServicesStandard] days: " + days, module);
        assertEquals("days is correct", "2", days);
    }

    public void testUspsDomesticRate() throws Exception {

        // prepare the context
        Map context = new HashMap();

        context.put("service", "Priority");
        context.put("originZip", "20770");
        context.put("destinationZip", "09021");
        context.put("pounds", "5");
        context.put("ounces", "1");
        context.put("container", "None");
        context.put("size", "Regular");
        context.put("machinable", "False");

        // run the service
        Map result = dispatcher.runSync("uspsDomesticRate", context);

        // verify the results
        String responseMessage = (String) result.get(ModelService.RESPONSE_MESSAGE);
        Debug.log("[testUspsDomesticRate] responseMessage: " + responseMessage, module);
        assertEquals("Service result is success", ModelService.RESPOND_SUCCESS, responseMessage);

        String postage = (String) result.get("postage");
        Debug.log("[testUspsDomesticRate] postage: " + postage, module);
        assertEquals("postage is correct", "7.90", postage);

        String restrictionCodes = (String) result.get("restrictionCodes");
        Debug.log("[testUspsDomesticRate] restrictionCodes: " + restrictionCodes, module);
        assertEquals("restrictionCodes is correct", "B-B1-C-D-U", restrictionCodes);

        String restrictionDesc = (String) result.get("restrictionDesc");
        Debug.log("[testUspsDomesticRate] restrictionDesc: " + restrictionDesc, module);
        assertEquals("restrictionDesc is correct", "B. Form 2976-A", restrictionDesc.substring(0,14));
    }
}
