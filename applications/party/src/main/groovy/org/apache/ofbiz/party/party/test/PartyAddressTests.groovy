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
package org.apache.ofbiz.party.party.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class PartyAddressTests extends OFBizTestCase {

    PartyAddressTests(String name) {
        super(name)
    }

    void testCreateContactMech() {
        Map serviceCtx = [
                contactMechId: 'TestEmailConactMech',
                contactMechTypeId: 'EMAIL_ADDRESS',
                infoString: 'test_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', 'TestEmailConactMech').queryOne()
        assert contactMech
        assert contactMech.infoString == 'test_email@example.com'
    }

    void testCreateEmailAddress() {
        Map serviceCtx = [
                emailAddress: 'test.email123@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'test.email123@example.com'
    }

    void testCreatePartyContactMech() {
        Map serviceCtx = [
                contactMechId: 'TestContactMech3',
                partyId: 'TestCustomer',
                contactMechPurposeTypeId: 'PRIMARY_EMAIL',
                contactMechTypeId: 'EMAIL_ADDRESS',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMech = from('PartyContactMech')
            .where('contactMechId', 'TestContactMech3', 'partyId', 'TestCustomer')
            .queryFirst()
        assert partyContactMech
    }

    void testCreatePartyContactMechPurpose() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                contactMechId: 'TestContactMech',
                contactMechPurposeTypeId: 'PRIMARY_EMAIL',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyContactMechPurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMechPurpose = from('PartyContactMechPurpose')
            .where('partyId',
                   'TestCustomer',
                   'contactMechId',
                   'TestContactMech',
                   'contactMechPurposeTypeId',
                   'PRIMARY_EMAIL',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyContactMechPurpose
    }

    void testCreatePartyDataSource() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                dataSourceId: 'MY_PORTAL',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyDataSource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyDataSource = from('PartyDataSource')
            .where('partyId', 'TestCustomer', 'dataSourceId', 'MY_PORTAL', 'fromDate', java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyDataSource
    }

    void testCreatePartyEmailAddress() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                emailAddress: 'test.email1234@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'test.email1234@example.com'
    }

    void testCreatePostalAddress() {
        Map serviceCtx = [
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                countryGeoId: 'USA',
                stateProvinceGeoId: 'CA',
                postalCode: '90000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '2004 Factory Blvd'
        assert postalAddress.city == 'City of Industry'
        assert postalAddress.postalCode == '90000'
    }

    void testCreateTelecomNumber() {
        Map serviceCtx = [
                contactMechId: 'TestTelecomNumber',
                areaCode: '801',
                contactNumber: '1111111',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == '801'
        assert telecomNumber.contactNumber == '1111111'
    }

    void testExpirePartyContactMechPurpose() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                contactMechId: 'TestContactMech',
                contactMechPurposeTypeId: 'PRIMARY_EMAIL',
                fromDate: java.sql.Timestamp.valueOf('2000-01-01 00:00:00'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('expirePartyContactMechPurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMechPurpose = from('PartyContactMechPurpose')
            .where('partyId',
                   'TestCustomer',
                   'contactMechId',
                   'TestContactMech',
                   'contactMechPurposeTypeId',
                   'PRIMARY_EMAIL',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2000-01-01 00:00:00'))
            .queryOne()
        assert partyContactMechPurpose
        assert partyContactMechPurpose.thruDate != null
    }

    void testGetPartyEmail() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyEmail', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.emailAddress
        assert serviceResult.contactMechId
    }

    void testGetPartyPostalAddress() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.address1
        assert serviceResult.countryGeoId
        assert serviceResult.contactMechId
    }

    void testGetPartyTelephone() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyTelephone', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.contactNumber
        assert serviceResult.contactMechId
    }

    void testUpdateContactMech() {
        Map serviceCtx = [
                contactMechId: 'TestContactMech',
                contactMechTypeId: 'EMAIL_ADDRESS',
                infoString: 'demo_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'demo_email@example.com'
    }

    void testUpdateEmailAddress() {
        Map serviceCtx = [
                contactMechId: 'TestContactMech',
                emailAddress: 'test.email123@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'test.email123@example.com'
    }

    void testUpdatePartyEmailAddress() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                contactMechId: 'TestContactMech',
                emailAddress: 'test.email12345@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == 'test.email12345@example.com'
    }

    void testUpdatePartyGroup() {
        Map serviceCtx = [
                partyId: 'TestGroup-1',
                groupName: 'Test Party Group',
                logoImageUrl: '/images/ofbiz_logo.png',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyGroup', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyGroup = from('PartyGroup').where('partyId', 'TestGroup-1').queryOne()
        assert partyGroup
        assert partyGroup.groupName == 'Test Party Group'
        assert partyGroup.logoImageUrl == '/images/ofbiz_logo.png'
    }

    void testUpdatePartyPostalAddress() {
        Map serviceCtx = [
                contactMechId: 'TestPostalAdd2',
                partyId: 'TestCustomer',
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                countryGeoId: 'USA',
                stateProvinceGeoId: 'CA',
                postalCode: '90000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '2004 Factory Blvd'
        assert postalAddress.city == 'City of Industry'
        assert postalAddress.postalCode == '90000'
    }

    void testUpdatePartyTelecomNumber() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                contactMechId: 'TestContactMech1',
                areaCode: '801',
                contactNumber: '1111111',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == '801'
        assert telecomNumber.contactNumber == '1111111'
    }

    void testUpdatePerson() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                firstName: 'New Test',
                lastName: 'Person',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePerson', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue person = from('Person').where('partyId', 'TestCustomer').queryOne()
        assert person
        assert person.firstName == 'New Test'
        assert person.lastName == 'Person'
    }

    void testUpdatePostalAddress() {
        Map serviceCtx = [
                contactMechId: 'TestPostalAdd1',
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                countryGeoId: 'USA',
                stateProvinceGeoId: 'CA',
                postalCode: '90000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '2004 Factory Blvd'
        assert postalAddress.city == 'City of Industry'
        assert postalAddress.postalCode == '90000'
        assert serviceResult.contactMechId != serviceResult.oldContactMechId
    }

    void testUpdatePostalAddressAndPurposes() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                contactMechId: 'TestPostalAdd3',
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                countryGeoId: 'USA',
                stateProvinceGeoId: 'CA',
                postalCode: '90000',
                fromDate: java.sql.Timestamp.valueOf('2001-05-13 00:00:00.000'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePostalAddressAndPurposes', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == '2004 Factory Blvd'
        assert postalAddress.city == 'City of Industry'
        assert postalAddress.postalCode == '90000'
    }

    void testUpdateTelecomNumber() {
        Map serviceCtx = [
                contactMechId: 'TestContactMech1',
                areaCode: '801',
                contactNumber: '1111111',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == '801'
        assert telecomNumber.contactNumber == '1111111'
        assert serviceResult.contactMechId != serviceResult.oldContactMechId
    }

}
