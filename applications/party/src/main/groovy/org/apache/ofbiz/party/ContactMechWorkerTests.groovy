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
package org.apache.ofbiz.party

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.party.contact.ContactMechWorker
import org.apache.ofbiz.service.testtools.OFBizTestCase

class ContactMechWorkerTests extends OFBizTestCase {
    public ContactMechWorkerTests(String name) {
        super(name)
    }

    void testPartyContactMechResolution() {
        //control for the DemoCustomer that postal, email, telecom and ftp contact are present and return correct information
        List partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, "DemoCustomer", true)
        assert partyContactMechValueMaps
        assert partyContactMechValueMaps.size() == 7
        boolean foundPostalAddress, foundTelecom, foundEmailAddress, foundFtpAddress = false
        partyContactMechValueMaps.forEach {
            Map partyContactMechValueMap ->
                switch (partyContactMechValueMap?.contactMech?.contactMechId) {
                    case '9015':
                        foundPostalAddress = true
                        assert partyContactMechValueMap.contactMech.contactMechTypeId == 'POSTAL_ADDRESS'
                        assert partyContactMechValueMap.partyContactMech
                        assert partyContactMechValueMap.contactMechType
                        assert partyContactMechValueMap.partyContactMechPurposes
                        assert partyContactMechValueMap.partyContactMechPurposes.size() == 3
                        assert partyContactMechValueMap.postalAddress
                        assert partyContactMechValueMap.postalAddress.contactMechId == '9015'
                        assert partyContactMechValueMap.postalAddress.address1 == '2004 Factory Blvd'
                        break
                    case '9027':
                        foundTelecom = true
                        assert partyContactMechValueMap.contactMech.contactMechTypeId == 'TELECOM_NUMBER'
                        assert partyContactMechValueMap.partyContactMech
                        assert partyContactMechValueMap.contactMechType
                        assert partyContactMechValueMap.partyContactMechPurposes
                        assert partyContactMechValueMap.partyContactMechPurposes.size() == 1
                        assert partyContactMechValueMap.telecomNumber
                        assert partyContactMechValueMap.telecomNumber.contactMechId == '9027'
                        assert partyContactMechValueMap.telecomNumber.contactNumber == '444-4444'
                        break
                    case '9126':
                        foundEmailAddress = true
                        assert partyContactMechValueMap.contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
                        assert partyContactMechValueMap.contactMech.infoString == 'ofbiztest@foo.com'
                        assert partyContactMechValueMap.partyContactMech
                        assert partyContactMechValueMap.contactMechType
                        assert partyContactMechValueMap.partyContactMechPurposes
                        assert partyContactMechValueMap.partyContactMechPurposes.size() == 2
                        break
                    case '9127':
                        foundFtpAddress = true
                        assert partyContactMechValueMap.contactMech.contactMechTypeId == 'FTP_ADDRESS'
                        assert partyContactMechValueMap.partyContactMech
                        assert partyContactMechValueMap.contactMechType
                        assert !partyContactMechValueMap.partyContactMechPurposes
                        assert partyContactMechValueMap.ftpAddress
                        assert partyContactMechValueMap.ftpAddress.hostname == "ftp://apacheofbiz.foo.com"
                        break
                }
        }
        assert foundPostalAddress && foundTelecom && foundEmailAddress && foundFtpAddress

        //Restart a search at now, the email 9126 need to have only one purpose
        partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, "DemoCustomer", false, "EMAIL_ADDRESS")
        partyContactMechValueMaps.forEach {
            Map partyContactMechValueMap ->
                switch (partyContactMechValueMap?.contactMech?.contactMechId) {
                    case '9126':
                        assert partyContactMechValueMap.partyContactMechPurposes.size() == 1
                        break
                }
        }

        //Restart a search at 05/13/2001 10:00:00.000, the email 9126 need to have two purposes
        partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, "DemoCustomer", UtilDateTime.toTimestamp("05/13/2001 10:00:00.000"), "EMAIL_ADDRESS")
        partyContactMechValueMaps.forEach {
            Map partyContactMechValueMap ->
                switch (partyContactMechValueMap?.contactMech?.contactMechId) {
                    case '9126':
                        assert partyContactMechValueMap.partyContactMechPurposes.size() == 2
                        break
                }
        }
    }

    void testOrderContactMechResolution() {
        List orderContactMechValueMaps = ContactMechWorker.getOrderContactMechValueMaps(delegator, "Demo1002")
        assert orderContactMechValueMaps
        assert orderContactMechValueMaps.size() == 3

        boolean foundBillingAddress, foundShippingAddress, foundOrderEmail = false
        orderContactMechValueMaps.forEach {
            Map orderContactMechValueMap ->
                switch (orderContactMechValueMap.contactMech?.contactMechId) {
                    case '9015':
                        assert orderContactMechValueMap.contactMech.contactMechTypeId == 'POSTAL_ADDRESS'
                        assert orderContactMechValueMap.contactMechType
                        assert orderContactMechValueMap.contactMechPurposeType
                        assert orderContactMechValueMap.orderContactMech
                        assert orderContactMechValueMap.postalAddress
                        assert orderContactMechValueMap.postalAddress.contactMechId == '9015'
                        assert orderContactMechValueMap.postalAddress.address1 == '2004 Factory Blvd'
                        foundBillingAddress = foundBillingAddress?: orderContactMechValueMap.contactMechPurposeType.contactMechPurposeTypeId == 'BILLING_LOCATION'
                        foundShippingAddress = foundShippingAddress?: orderContactMechValueMap.contactMechPurposeType.contactMechPurposeTypeId == 'SHIPPING_LOCATION'
                        break
                    case '9026':
                        assert orderContactMechValueMap.contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
                        assert orderContactMechValueMap.contactMech.infoString == 'ofbiztest@example.com'
                        assert orderContactMechValueMap.orderContactMech
                        assert orderContactMechValueMap.contactMechType
                        foundOrderEmail = orderContactMechValueMap.contactMechPurposeType.contactMechPurposeTypeId == 'ORDER_EMAIL'
                        break
                    default:
                        assert false
                        break
                }
        }
        assert foundBillingAddress && foundShippingAddress && foundOrderEmail
    }

    void testWorkEffortContactMechResolution() {
        List workEffortContactMechValueMaps = ContactMechWorker.getWorkEffortContactMechValueMaps(delegator, "TEST_CM_WORKER")
        assert workEffortContactMechValueMaps
        assert workEffortContactMechValueMaps.size() == 3

        boolean foundPostalAddress, foundPhone, foundEmail = false
        workEffortContactMechValueMaps.forEach {
            Map workEffortContactMechValueMap ->
                switch (workEffortContactMechValueMap.contactMech?.contactMechId) {
                    case '9015':
                        assert workEffortContactMechValueMap.contactMech.contactMechTypeId == 'POSTAL_ADDRESS'
                        assert workEffortContactMechValueMap.contactMechType
                        assert workEffortContactMechValueMap.workEffortContactMech
                        assert workEffortContactMechValueMap.postalAddress
                        assert workEffortContactMechValueMap.postalAddress.contactMechId == '9015'
                        assert workEffortContactMechValueMap.postalAddress.address1 == '2004 Factory Blvd'
                        foundPostalAddress = true
                        break
                    case '9126':
                        assert workEffortContactMechValueMap.contactMech.contactMechTypeId == 'EMAIL_ADDRESS'
                        assert workEffortContactMechValueMap.contactMech.infoString == 'ofbiztest@foo.com'
                        assert workEffortContactMechValueMap.workEffortContactMech
                        assert workEffortContactMechValueMap.contactMechType
                        foundEmail = true
                        break
                    case '9125':
                        assert workEffortContactMechValueMap.contactMech.contactMechTypeId == 'TELECOM_NUMBER'
                        assert workEffortContactMechValueMap.workEffortContactMech
                        assert workEffortContactMechValueMap.contactMechType
                        assert workEffortContactMechValueMap.telecomNumber.contactNumber == '555-5555'
                        foundPhone = true
                        break
                    default:
                        assert false
                        break
                }
        }
        assert foundPostalAddress && foundEmail && foundPhone
    }
}
