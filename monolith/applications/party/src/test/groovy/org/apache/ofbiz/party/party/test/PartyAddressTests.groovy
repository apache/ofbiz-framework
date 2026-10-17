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
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class PartyAddressTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateContactMech() {
        String contactMechId = testParams.contactMechId ?: 'TestEmailConactMech'
        String contactMechTypeId = testParams.contactMechTypeId ?: 'EMAIL_ADDRESS'
        String infoString = testParams.infoString ?: 'test_email@example.com'
        Map serviceCtx = [
                contactMechId: contactMechId,
                contactMechTypeId: contactMechTypeId,
                infoString: infoString,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == infoString
    }

    @Test
    @Order(2)
    void testCreateEmailAddress() {
        String emailAddress = testParams.emailAddress ?: 'test.email123@example.com'
        Map serviceCtx = [
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == emailAddress
    }

    @Test
    @Order(3)
    void testCreatePartyContactMech() {
        String contactMechId = testParams.contactMechId ?: 'TestContactMech3'
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'PRIMARY_EMAIL'
        String contactMechTypeId = testParams.contactMechTypeId ?: 'EMAIL_ADDRESS'
        Map serviceCtx = [
                contactMechId: contactMechId,
                partyId: partyId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                contactMechTypeId: contactMechTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMech = from('PartyContactMech')
            .where('contactMechId', contactMechId, 'partyId', partyId)
            .queryFirst()
        assert partyContactMech
    }

    @Test
    // Must run after testExpirePartyContactMechPurpose: that test expires the fixture's active
    // PRIMARY_EMAIL purpose for this contactMechId, which this test needs already expired -
    // otherwise createPartyContactMechPurpose fails ("a purpose with that type already exists").
    @Order(9)
    void testCreatePartyContactMechPurpose() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechId = testParams.contactMechId ?: 'TestContactMech'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'PRIMARY_EMAIL'
        Map serviceCtx = [
                partyId: partyId,
                contactMechId: contactMechId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyContactMechPurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMechPurpose = from('PartyContactMechPurpose')
            .where('partyId',
                   partyId,
                   'contactMechId',
                   contactMechId,
                   'contactMechPurposeTypeId',
                   contactMechPurposeTypeId,
                   'fromDate',
                   java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyContactMechPurpose
    }

    @Test
    @Order(5)
    void testCreatePartyDataSource() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String dataSourceId = testParams.dataSourceId ?: 'MY_PORTAL'
        Map serviceCtx = [
                partyId: partyId,
                dataSourceId: dataSourceId,
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyDataSource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyDataSource = from('PartyDataSource')
            .where('partyId', partyId, 'dataSourceId', dataSourceId, 'fromDate', java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyDataSource
    }

    @Test
    @Order(6)
    void testCreatePartyEmailAddress() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String emailAddress = testParams.emailAddress ?: 'test.email1234@example.com'
        Map serviceCtx = [
                partyId: partyId,
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == emailAddress
    }

    @Test
    @Order(7)
    void testCreatePostalAddress() {
        String toName = testParams.toName ?: 'Test Address'
        String address1 = testParams.address1 ?: '2004 Factory Blvd'
        String city = testParams.city ?: 'City of Industry'
        String countryGeoId = testParams.countryGeoId ?: 'USA'
        String stateProvinceGeoId = testParams.stateProvinceGeoId ?: 'CA'
        String postalCode = testParams.postalCode ?: '90000'
        Map serviceCtx = [
                toName: toName,
                address1: address1,
                city: city,
                countryGeoId: countryGeoId,
                stateProvinceGeoId: stateProvinceGeoId,
                postalCode: postalCode,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == address1
        assert postalAddress.city == city
        assert postalAddress.postalCode == postalCode
    }

    @Test
    @Order(8)
    void testCreateTelecomNumber() {
        String contactMechId = testParams.contactMechId ?: 'TestTelecomNumber'
        String areaCode = testParams.areaCode ?: '801'
        String contactNumber = testParams.contactNumber ?: '1111111'
        Map serviceCtx = [
                contactMechId: contactMechId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == areaCode
        assert telecomNumber.contactNumber == contactNumber
    }

    @Test
    @Order(4)
    void testExpirePartyContactMechPurpose() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechId = testParams.contactMechId ?: 'TestContactMech'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'PRIMARY_EMAIL'
        Map serviceCtx = [
                partyId: partyId,
                contactMechId: contactMechId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                fromDate: java.sql.Timestamp.valueOf('2000-01-01 00:00:00'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('expirePartyContactMechPurpose', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyContactMechPurpose = from('PartyContactMechPurpose')
            .where('partyId',
                   partyId,
                   'contactMechId',
                   contactMechId,
                   'contactMechPurposeTypeId',
                   contactMechPurposeTypeId,
                   'fromDate',
                   java.sql.Timestamp.valueOf('2000-01-01 00:00:00'))
            .queryOne()
        assert partyContactMechPurpose
        assert partyContactMechPurpose.thruDate != null
    }

    @Test
    @Order(10)
    void testGetPartyEmail() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        Map serviceCtx = [
                partyId: partyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyEmail', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.emailAddress
        assert serviceResult.contactMechId
    }

    @Test
    @Order(11)
    void testGetPartyPostalAddress() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        Map serviceCtx = [
                partyId: partyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.address1
        assert serviceResult.countryGeoId
        assert serviceResult.contactMechId
    }

    @Test
    @Order(12)
    void testGetPartyTelephone() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        Map serviceCtx = [
                partyId: partyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyTelephone', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.contactNumber
        assert serviceResult.contactMechId
    }

    @Test
    @Order(13)
    void testUpdateContactMech() {
        String contactMechId = testParams.contactMechId ?: 'TestContactMech'
        String contactMechTypeId = testParams.contactMechTypeId ?: 'EMAIL_ADDRESS'
        String infoString = testParams.infoString ?: 'demo_email@example.com'
        Map serviceCtx = [
                contactMechId: contactMechId,
                contactMechTypeId: contactMechTypeId,
                infoString: infoString,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateContactMech', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == infoString
    }

    @Test
    @Order(14)
    void testUpdateEmailAddress() {
        String contactMechId = testParams.contactMechId ?: 'TestContactMech'
        String emailAddress = testParams.emailAddress ?: 'test.email123@example.com'
        Map serviceCtx = [
                contactMechId: contactMechId,
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == emailAddress
    }

    @Test
    @Order(15)
    void testUpdatePartyEmailAddress() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechId = testParams.contactMechId ?: 'TestContactMech'
        String emailAddress = testParams.emailAddress ?: 'test.email12345@example.com'
        Map serviceCtx = [
                partyId: partyId,
                contactMechId: contactMechId,
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert contactMech
        assert contactMech.infoString == emailAddress
    }

    @Test
    @Order(16)
    void testUpdatePartyGroup() {
        String partyId = testParams.partyId ?: 'TestGroup-1'
        String groupName = testParams.groupName ?: 'Test Party Group'
        String logoImageUrl = testParams.logoImageUrl ?: '/images/ofbiz_logo.png'
        Map serviceCtx = [
                partyId: partyId,
                groupName: groupName,
                logoImageUrl: logoImageUrl,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyGroup', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyGroup = from('PartyGroup').where('partyId', partyId).queryOne()
        assert partyGroup
        assert partyGroup.groupName == groupName
        assert partyGroup.logoImageUrl == logoImageUrl
    }

    @Test
    @Order(17)
    void testUpdatePartyPostalAddress() {
        String contactMechId = testParams.contactMechId ?: 'TestPostalAdd2'
        String partyId = testParams.partyId ?: 'TestCustomer'
        String toName = testParams.toName ?: 'Test Address'
        String address1 = testParams.address1 ?: '2004 Factory Blvd'
        String city = testParams.city ?: 'City of Industry'
        String countryGeoId = testParams.countryGeoId ?: 'USA'
        String stateProvinceGeoId = testParams.stateProvinceGeoId ?: 'CA'
        String postalCode = testParams.postalCode ?: '90000'
        Map serviceCtx = [
                contactMechId: contactMechId,
                partyId: partyId,
                toName: toName,
                address1: address1,
                city: city,
                countryGeoId: countryGeoId,
                stateProvinceGeoId: stateProvinceGeoId,
                postalCode: postalCode,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == address1
        assert postalAddress.city == city
        assert postalAddress.postalCode == postalCode
    }

    @Test
    @Order(18)
    void testUpdatePartyTelecomNumber() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechId = testParams.contactMechId ?: 'TestContactMech1'
        String areaCode = testParams.areaCode ?: '801'
        String contactNumber = testParams.contactNumber ?: '1111111'
        Map serviceCtx = [
                partyId: partyId,
                contactMechId: contactMechId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == areaCode
        assert telecomNumber.contactNumber == contactNumber
    }

    @Test
    @Order(19)
    void testUpdatePerson() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String firstName = testParams.firstName ?: 'New Test'
        String lastName = testParams.lastName ?: 'Person'
        Map serviceCtx = [
                partyId: partyId,
                firstName: firstName,
                lastName: lastName,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePerson', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue person = from(lastName).where('partyId', partyId).queryOne()
        assert person
        assert person.firstName == firstName
        assert person.lastName == lastName
    }

    @Test
    @Order(20)
    void testUpdatePostalAddress() {
        String contactMechId = testParams.contactMechId ?: 'TestPostalAdd1'
        String toName = testParams.toName ?: 'Test Address'
        String address1 = testParams.address1 ?: '2004 Factory Blvd'
        String city = testParams.city ?: 'City of Industry'
        String countryGeoId = testParams.countryGeoId ?: 'USA'
        String stateProvinceGeoId = testParams.stateProvinceGeoId ?: 'CA'
        String postalCode = testParams.postalCode ?: '90000'
        Map serviceCtx = [
                contactMechId: contactMechId,
                toName: toName,
                address1: address1,
                city: city,
                countryGeoId: countryGeoId,
                stateProvinceGeoId: stateProvinceGeoId,
                postalCode: postalCode,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == address1
        assert postalAddress.city == city
        assert postalAddress.postalCode == postalCode
        assert serviceResult.contactMechId != serviceResult.oldContactMechId
    }

    @Test
    @Order(21)
    void testUpdatePostalAddressAndPurposes() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String contactMechId = testParams.contactMechId ?: 'TestPostalAdd3'
        String toName = testParams.toName ?: 'Test Address'
        String address1 = testParams.address1 ?: '2004 Factory Blvd'
        String city = testParams.city ?: 'City of Industry'
        String countryGeoId = testParams.countryGeoId ?: 'USA'
        String stateProvinceGeoId = testParams.stateProvinceGeoId ?: 'CA'
        String postalCode = testParams.postalCode ?: '90000'
        Map serviceCtx = [
                partyId: partyId,
                contactMechId: contactMechId,
                toName: toName,
                address1: address1,
                city: city,
                countryGeoId: countryGeoId,
                stateProvinceGeoId: stateProvinceGeoId,
                postalCode: postalCode,
                fromDate: java.sql.Timestamp.valueOf('2001-05-13 00:00:00.000'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePostalAddressAndPurposes', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress
        assert postalAddress.address1 == address1
        assert postalAddress.city == city
        assert postalAddress.postalCode == postalCode
    }

    @Test
    @Order(22)
    void testUpdateTelecomNumber() {
        String contactMechId = testParams.contactMechId ?: 'TestContactMech1'
        String areaCode = testParams.areaCode ?: '801'
        String contactNumber = testParams.contactNumber ?: '1111111'
        Map serviceCtx = [
                contactMechId: contactMechId,
                areaCode: areaCode,
                contactNumber: contactNumber,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.areaCode == areaCode
        assert telecomNumber.contactNumber == contactNumber
        assert serviceResult.contactMechId != serviceResult.oldContactMechId
    }

}
