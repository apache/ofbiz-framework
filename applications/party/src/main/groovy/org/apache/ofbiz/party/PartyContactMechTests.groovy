/*
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
 */
package org.apache.ofbiz.party

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.party.party.PartyWorker
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class PartyContactMechTests extends OFBizTestCase {
    public PartyContactMechTests(String name) {
        super(name)
    }

    void testUpdatePartyEmailAddress() {
        String partyId = 'DemoCustomer'
        String contactMechTypeId = 'EMAIL_ADDRESS'
        String emailAddress = 'ofbiztest@example.com'

        // first try with just updating without changing the email address
        Map serviceCtx = [
                partyId   : partyId,
                contactMechTypeId  : contactMechTypeId,
                emailAddress: emailAddress,
                contactMechId: '9026',
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        assert contactMechId.equals(serviceCtx.contactMechId)
        assert contactMech.infoString.equals(serviceCtx.emailAddress)

        // now update with changing the email address, a new record will be created in ContactMech entity this time
        serviceResult.clear()
        serviceCtx.emailAddress = 'ofbiz-test@example.com'
        serviceResult = dispatcher.runSync('updatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String newContactMechId = serviceResult.contactMechId
        assert newContactMechId

        contactMech.clear()
        contactMech = from("ContactMech")
                .where('contactMechId', newContactMechId)
                .queryOne()
        assert contactMech
        assert !contactMechId.equals(newContactMechId)
        assert contactMech.infoString.equals(serviceCtx.emailAddress)
    }

    void testUpdatePartyTelecomNumber() {
        String partyId = 'DemoCustomer'

        // first try with just updating without changing the email address
        Map serviceCtx = [
                partyId   : partyId,
                contactMechId: '9025',
                countryCode: '1',
                areaCode: '801',
                contactNumber: '555-5555',
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        assert contactMechId.equals(serviceCtx.contactMechId)

        GenericValue telecomNumber = from("TelecomNumber")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode.equals(serviceCtx.areaCode)
        assert telecomNumber.contactNumber.equals(serviceCtx.contactNumber)

        // try now with changing the telecom number, a new record will be created in ContactMech, TelecomNumber entity this time
        serviceResult.clear()
        serviceCtx.contactNumber = '555-6666'
        serviceResult = dispatcher.runSync('updatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String newContactMechId = serviceResult.contactMechId
        assert newContactMechId

        contactMech.clear()
        contactMech = from("ContactMech")
                .where('contactMechId', newContactMechId)
                .queryOne()
        assert contactMech
        assert !contactMechId.equals(newContactMechId)

        telecomNumber.clear()
        telecomNumber = from("TelecomNumber")
                .where('contactMechId', newContactMechId)
                .queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode.equals(serviceCtx.areaCode)
        assert telecomNumber.contactNumber.equals(serviceCtx.contactNumber)
    }

    void testUpdatePartyPostalAddress() {
        String partyId = 'DemoCustomer'

        // first try with just updating without changing the postal address
        GenericValue postalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator)
        Map serviceCtx = dispatcher.getDispatchContext().makeValidContext("updatePartyPostalAddress", ModelService.IN_PARAM, postalAddress)
        serviceCtx.partyId = partyId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('updatePartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId
        assert contactMechId.equals(serviceCtx.contactMechId)

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech

        postalAddress.clear()
        postalAddress = from("PostalAddress")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert postalAddress
        assert postalAddress.address1.equals(serviceCtx.address1)
        assert postalAddress.stateProvinceGeoId.equals(serviceCtx.stateProvinceGeoId)
        assert postalAddress.postalCode.equals(serviceCtx.postalCode)

        // try now with changing the postal address fields, a new record will be created in ContactMech, PostalAddress entity this time
        serviceResult.clear()
        serviceCtx.stateProvinceGeoId = 'VA'
        serviceCtx.postalCode = '20147'
        serviceResult = dispatcher.runSync('updatePartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String newContactMechId = serviceResult.contactMechId
        assert newContactMechId

        contactMech.clear()
        contactMech = from("ContactMech")
                .where('contactMechId', newContactMechId)
                .queryOne()
        assert contactMech
        assert !contactMechId.equals(newContactMechId)

        postalAddress.clear()
        postalAddress = from("PostalAddress")
                .where('contactMechId', newContactMechId)
                .queryOne()
        assert postalAddress
        assert postalAddress.stateProvinceGeoId.equals(serviceCtx.stateProvinceGeoId)
        assert postalAddress.postalCode.equals(serviceCtx.postalCode)
    }

    void testCreatePartyEmailAddress() {
        String partyId = 'DemoEmployee'
        String emailAddress = 'demo.employee@gmail.com'
        String contactMechPurposeTypeId = 'PRIMARY_EMAIL'

        Map serviceCtx = [
                partyId: partyId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                emailAddress: emailAddress,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        assert emailAddress.equals(contactMech.infoString)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().orderBy('fromDate')
                .queryFirst()
        assert partyContactMech
        assert partyId.equals(partyContactMech.partyId)

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().orderBy('fromDate')
                .queryFirst()
        assert partyContactMechPurpose
        assert partyId.equals(partyContactMechPurpose.partyId)
        assert contactMechPurposeTypeId.equals(partyContactMechPurpose.contactMechPurposeTypeId)
    }

    void testCreatePartyTelecomNumber() {
        String partyId = 'DemoEmployee'
        String areaCode = '801'
        String contactNumber = '888-8899'
        String contactMechPurposeTypeId = 'PRIMARY_PHONE'

        Map serviceCtx = [
                partyId: partyId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        GenericValue telecomNumber = from("TelecomNumber")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert telecomNumber
        assert areaCode.equals(telecomNumber.areaCode)
        assert contactNumber.equals(telecomNumber.contactNumber)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMech
        assert partyId.equals(partyContactMech.partyId)

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMechPurpose
        assert partyId.equals(partyContactMechPurpose.partyId)
        assert contactMechPurposeTypeId.equals(partyContactMechPurpose.contactMechPurposeTypeId)
    }

    void testCreateUpdatePartyTelecomNumberWithCreate() {
        String partyId = 'DemoCustomer'
        String contactMechPurposeTypeId = 'PHONE_WORK'
        String areaCode = '801'
        String contactNumber = '888-8888'
        String extension = '444'

        Map serviceCtx = [
                partyId: partyId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                extension: extension,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        GenericValue telecomNumber = from("TelecomNumber")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert telecomNumber
        assert areaCode.equals(telecomNumber.areaCode)
        assert contactNumber.equals(telecomNumber.contactNumber)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMech
        assert partyId.equals(partyContactMech.partyId)
        assert extension.equals(partyContactMech.extension)

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMechPurpose
        assert partyId.equals(partyContactMechPurpose.partyId)
        assert contactMechPurposeTypeId.equals(partyContactMechPurpose.contactMechPurposeTypeId)
    }

    void testCreateUpdatePartyTelecomNumberWithUpdate() {
        String partyId = 'DemoCustomer'
        String contactMechPurposeTypeId = 'PHONE_HOME'
        String areaCode = '802'
        String contactNumber = '555-5555'

        Map serviceCtx = [
                partyId: partyId,
                contactMechId: '9125',
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId != '9125'

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', '9125')
                .queryFirst()
        assert partyContactMechPurpose
        assert partyContactMechPurpose.thruDate

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech

        GenericValue telecomNumber = from("TelecomNumber")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert telecomNumber
        assert areaCode.equals(telecomNumber.areaCode)
        assert contactNumber.equals(telecomNumber.contactNumber)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMech

        partyContactMechPurpose.clear()
        partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMechPurpose
    }

    void testCreateUpdatePartyEmailAddressWithCreate() {
        String partyId = 'DemoCustomer'
        String contactMechPurposeTypeId = 'PRIMARY_EMAIL'
        String emailAddress = 'demo.customer@foo.com'

        Map serviceCtx = [
                partyId: partyId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                emailAddress: emailAddress,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId
        assert emailAddress.equals(serviceResult.emailAddress)

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        assert emailAddress.equals(contactMech.infoString)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMech

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMechPurpose
    }

    void testCreateUpdatePartyEmailAddressWithUpdate() {
        String partyId = 'DemoCustomer'
        String contactMechPurposeTypeId = 'PRIMARY_EMAIL'
        String emailAddress = 'demo.customer@foo.com'

        Map serviceCtx = [
                partyId: partyId,
                contactMechId: '9126',
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                emailAddress: emailAddress,
                userLogin : userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId
        assert contactMechId != '9126'
        assert emailAddress.equals(serviceResult.emailAddress)

        GenericValue partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', '9126')
                .queryFirst()
        assert partyContactMechPurpose
        assert partyContactMechPurpose.thruDate != null

        GenericValue contactMech = from("ContactMech")
                .where('contactMechId', contactMechId)
                .queryOne()
        assert contactMech
        assert emailAddress.equals(contactMech.infoString)

        GenericValue partyContactMech = from("PartyContactMech")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMech

        partyContactMechPurpose.clear()
        partyContactMechPurpose = from("PartyContactMechPurpose")
                .where('contactMechId', contactMechId)
                .filterByDate().queryFirst()
        assert partyContactMechPurpose
    }
}