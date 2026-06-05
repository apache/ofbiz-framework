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

class PartyTests extends OFBizTestCase {

    PartyTests(String name) {
        super(name)
    }

    void testCopyPartyContactMechs() {
        Map serviceCtx = [
                partyIdFrom: 'TestCustomer',
                partyIdTo: 'TestParty',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('copyPartyContactMechs', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> partyContactMechList = from('PartyContactMech').where('partyId', 'TestParty').queryList()
        assert partyContactMechList
    }

    void testCreatePartyGroup() {
        Map serviceCtx = [
                partyId: 'DemoGroup1',
                groupName: 'Demo Group',
                partyTypeId: 'PARTY_GROUP',
                statusId: 'PARTY_ENABLED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyGroup', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyGroup = from('PartyGroup').where('partyId', serviceResult.partyId).queryOne()

        assert partyGroup
        assert partyGroup.partyId == 'DemoGroup1'
        assert partyGroup.groupName == 'Demo Group'
    }

    void testCreatePartyPostalAddress() {
        Map serviceCtx = [
                contactMechId: 'TestPostalAddress',
                partyId: 'TestCustomer',
                toName: 'Test Address',
                address1: '2004 Factory Blvd',
                city: 'City of Industry',
                countryGeoId: 'USA',
                stateProvinceGeoId: 'CA',
                postalCode: '90000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyPostalAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue postalAddress = from('PostalAddress').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert postalAddress != null
        assert postalAddress.city == 'City of Industry'
    }

    void testCreatePartyRelationship() {
        Map serviceCtx = [
                partyIdFrom: 'TestCompany',
                partyIdTo: 'TestCustomer',
                roleTypeIdFrom: 'INTERNAL_ORGANIZATIO',
                roleTypeIdTo: 'CONTACT',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyRelationship', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRelationship = from('PartyRelationship')
            .where('partyIdFrom',
                   'TestCompany',
                   'partyIdTo',
                   'TestCustomer',
                   'roleTypeIdFrom',
                   'INTERNAL_ORGANIZATIO',
                   'roleTypeIdTo',
                   'CONTACT',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyRelationship
    }

    void testCreatePartyRelationshipAndRole() {
        Map serviceCtx = [
                partyIdFrom: 'TestCompany',
                partyIdTo: 'TestCustomer',
                roleTypeIdFrom: 'BUYER',
                roleTypeIdTo: 'ACCOUNT_LEAD',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyRelationshipAndRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole').where('partyId', 'TestCompany', 'roleTypeId', 'BUYER').queryOne()
        assert partyRole

        GenericValue partyRelationship = from('PartyRelationship')
            .where('partyIdFrom',
                   'TestCompany',
                   'partyIdTo',
                   'TestCustomer',
                   'roleTypeIdFrom',
                   'BUYER',
                   'roleTypeIdTo',
                   'ACCOUNT_LEAD',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyRelationship
    }

    void testCreatePartyRelationshipContactAccount() {
        Map serviceCtx = [
                accountPartyId: 'TestParty',
                contactPartyId: 'TestCustomer',
                comments: 'This is a test party contact account relationship',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyRelationshipContactAccount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> partyRelationshipList = from('PartyRelationship')
            .where('partyIdFrom', 'TestParty', 'partyIdTo', 'TestCustomer', 'roleTypeIdFrom', 'ACCOUNT', 'roleTypeIdTo', 'CONTACT')
            .queryList()
        assert partyRelationshipList
    }

    void testCreatePartyRole() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                roleTypeId: 'CLIENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole')
            .where('partyId', 'DemoCustomer', 'roleTypeId', 'CLIENT')
            .queryOne()

        assert partyRole
        assert partyRole.partyId == 'DemoCustomer'
        assert partyRole.roleTypeId == 'CLIENT'
    }

    void testCreatePartyTelecomNumber() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                contactMechPurposeTypeId: 'PRIMARY_PHONE',
                countryCode: '1',
                areaCode: '801',
                contactNumber: '888-8888',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyTelecomNumber', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactMechId = serviceResult.contactMechId
        assert contactMechId

        GenericValue contactMech = from('ContactMech').where('contactMechId', contactMechId).queryOne()
        assert contactMech

        GenericValue telecomNumber = from('TelecomNumber').where('contactMechId', contactMechId).queryOne()
        assert telecomNumber
        assert telecomNumber.countryCode == '1'
        assert telecomNumber.areaCode == '801'
        assert telecomNumber.contactNumber == '888-8888'

        List<GenericValue> pcmList = from('PartyContactMech')
            .where('partyId', 'DemoCustomer', 'contactMechId', contactMechId)
            .filterByDate()
            .queryList()
        assert pcmList

        List<GenericValue> pcmpList = from('PartyContactMechPurpose')
            .where('partyId', 'DemoCustomer', 'contactMechId', contactMechId)
            .filterByDate()
            .queryList()
        assert pcmpList

        GenericValue pcmp = pcmpList.first()
        assert pcmp.contactMechPurposeTypeId == 'PRIMARY_PHONE'
    }

    void testCreatePerson() {
        Map serviceCtx = [
                firstName: 'Demo',
                lastName: 'Person',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPerson', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String partyId = serviceResult.partyId

        GenericValue party = from('Party').where('partyId', partyId).queryOne()
        assert party
        assert party.partyTypeId == 'PERSON'

        GenericValue person = from('Person').where('partyId', partyId).queryOne()
        assert person
        assert person.firstName == 'Demo'
        assert person.lastName == 'Person'
    }

    void testCreatePersonAndUserLogin() {
        Map serviceCtx = [
                partyId: 'DemoPerson',
                firstName: 'Demo',
                lastName: 'Person',
                userLoginId: 'demo.person',
                currentPassword: 'ofbiz',
                currentPasswordVerify: 'ofbiz',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPersonAndUserLogin', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue newUserLogin = serviceResult.newUserLogin
        assert newUserLogin
        assert newUserLogin.partyId == 'DemoPerson'
        assert newUserLogin.userLoginId == 'demo.person'

        GenericValue person = from('Person').where('partyId', 'DemoPerson').queryOne()
        assert person
        assert person.firstName == 'Demo'
        assert person.lastName == 'Person'
    }

    void testCreateUpdatePartyRelationshipAndRoles() {
        Map serviceCtx = [
                partyId: 'TestCompany',
                partyIdFrom: 'TestCompany',
                partyIdTo: 'TestCustomer',
                roleTypeIdFrom: 'BUYER',
                roleTypeIdTo: 'ACCOUNT_LEAD',
                fromDate: java.sql.Timestamp.valueOf('2009-09-09 01:01:01'),
                partyRelationshipTypeId: 'AGENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePartyRelationshipAndRoles', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole').where('partyId', 'TestCompany', 'roleTypeId', 'BUYER').queryOne()
        assert partyRole

        GenericValue partyRelationship = from('PartyRelationship')
            .where('partyIdFrom',
                   'TestCompany',
                   'partyIdTo',
                   'TestCustomer',
                   'roleTypeIdFrom',
                   'BUYER',
                   'roleTypeIdTo',
                   'ACCOUNT_LEAD',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2009-09-09 01:01:01'))
            .queryOne()
        assert partyRelationship
        assert serviceCtx.partyRelationshipTypeId == 'AGENT'
    }

    void testCreateUpdatePersonWithCreate() {
        Map serviceCtx = [
                partyId: 'DemoPerson1',
                firstName: 'Demo',
                lastName: 'Person1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePerson', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from('Party').where('partyId', 'DemoPerson1').queryOne()
        assert party

        GenericValue person = from('Person').where('partyId', 'DemoPerson1').queryOne()
        assert person
        assert person.firstName == 'Demo'
        assert person.lastName == 'Person1'
    }

    void testCreateUpdatePersonWithUpdate() {
        Map serviceCtx = [
                partyId: 'DemoPerson1',
                firstName: 'Demo',
                lastName: 'Person2',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createUpdatePerson', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from('Party').where('partyId', serviceResult.partyId).queryOne()
        GenericValue person = from('Person').where('partyId', serviceResult.partyId).queryOne()

        assert party
        assert person
        assert person.firstName == 'Demo'
        assert person.lastName == 'Person2'
    }

    void testDeletePartyRelationship() {
        Map serviceCtx = [
                partyIdFrom: 'TestCompany',
                partyIdTo: 'TestCustomer',
                roleTypeIdFrom: '_NA_',
                roleTypeIdTo: 'CONTACT',
                fromDate: java.sql.Timestamp.valueOf('2000-01-01 00:00:00'),
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyRelationship', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRelationship = from('PartyRelationship')
            .where('partyIdFrom',
                   'TestCompany',
                   'partyIdTo',
                   'TestCustomer',
                   'roleTypeIdFrom',
                   '_NA_',
                   'roleTypeIdTo',
                   'CONTACT',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2000-01-01 00:00:00'))
            .queryOne()
        assert !partyRelationship
    }

    void testDeletePartyRole() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                roleTypeId: 'ACCOUNTANT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole').where('partyId', 'TestCustomer', 'roleTypeId', 'ACCOUNTANT').queryOne()
        assert !partyRole
    }

    void testEnsurePartyRole() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                roleTypeId: 'VENDOR',
                userLogin: userLogin
        ]

        Map serviceResult = dispatcher.runSync('ensurePartyRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'VENDOR').queryOne()
        assert partyRole

        serviceCtx.roleTypeId = 'EMPLOYEE'
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'EMPLOYEE').queryOne()
        assert !partyRole

        serviceResult = dispatcher.runSync('ensurePartyRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'EMPLOYEE').queryOne()
        assert partyRole

        Map ensurePartyRoleFromCtx = [
                partyIdFrom: 'DemoCustomer',
                roleTypeIdFrom: 'CONTRACTOR',
                userLogin: userLogin
        ]
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'CONTRACTOR').queryOne()
        assert !partyRole

        serviceResult = dispatcher.runSync('ensurePartyRoleFrom', ensurePartyRoleFromCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'CONTRACTOR').queryOne()
        assert partyRole

        Map ensurePartyRoleToCtx = [
                partyIdTo: 'DemoCustomer',
                roleTypeIdTo: 'MANUFACTURER',
                userLogin: userLogin
        ]
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'MANUFACTURER').queryOne()
        assert !partyRole

        serviceResult = dispatcher.runSync('ensurePartyRoleTo', ensurePartyRoleToCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        partyRole = from('PartyRole').where('partyId', 'DemoCustomer', 'roleTypeId', 'MANUFACTURER').queryOne()
        assert partyRole
    }

    void testFindPartiesById() {
        Map serviceCtx = [
                idToFind: 'TestCustomer',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findPartiesById', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = serviceResult.party
        assert party
        assert party.partyId == 'TestCustomer'
    }

    void testFindPartyFromEmailAddress() {
        Map serviceCtx = [
                address: 'newtest_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findPartyFromEmailAddress', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from('Party').where('partyId', serviceResult.partyId).queryOne()
        GenericValue contactMech = from('ContactMech').where('contactMechId', serviceResult.contactMechId).queryOne()
        assert party
        assert contactMech
    }

    void testFindPartyFromTelephone() {
        Map serviceCtx = [
                telno: '801555-5555',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findPartyFromTelephone', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from('Party').where('partyId', serviceResult.partyId).queryOne()
        assert party
    }

    void testFindPartyFromTelephoneComplete() {
        Map serviceCtx = [
                telno: '801555-5555',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findPartyFromTelephoneComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue party = from('Party').where('partyId', serviceResult.partyId).queryOne()
        assert party
    }

    void testFindPartyWithNoSearchParameters() {
        Map serviceCtx = [
                lookupFlag: 'Y',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        try {
            assert serviceResult.partyList != null || serviceResult.partyListSize != null
        } finally {
            if (serviceResult.partyList instanceof org.apache.ofbiz.entity.util.EntityListIterator) {
                serviceResult.partyList.close()
            }
        }
    }

    void testFindPartyWithSearchParameters() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                roleTypeId: 'CUSTOMER',
                lookupFlag: 'Y',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('findParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRoleDetailAndPartyDetail = from('PartyRoleDetailAndPartyDetail')
            .where('partyId', 'DemoCustomer', 'roleTypeId', 'CUSTOMER')
            .queryOne()

        try {
            if (partyRoleDetailAndPartyDetail) {
                assert serviceResult.partyList != null || serviceResult.partyListSize != null
            } else {
                assert serviceResult.partyList == null && serviceResult.partyListSize == null
            }
        } finally {
            if (serviceResult.partyList instanceof org.apache.ofbiz.entity.util.EntityListIterator) {
                serviceResult.partyList.close()
            }
        }
    }

    void testGetPartyMainRole() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getPartyMainRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRole = from('PartyRole').where('partyId', 'TestCustomer', 'roleTypeId', serviceResult.roleTypeId).queryOne()
        assert partyRole
    }

    void testLookupParty() {
        Map serviceCtx = [
                firstName: 'Test',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('lookupParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.lookupResult
    }

    void testQuickCreateCustomer() {
        Map serviceCtx = [
                firstName: 'Test',
                lastName: 'Customer',
                emailAddress: 'test.customer@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('quickCreateCustomer', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue person = from('Person').where('partyId', serviceResult.partyId).queryOne()
        assert person
        assert person.firstName == 'Test'
        assert person.lastName == 'Customer'

        GenericValue partyRole = from('PartyRole').where('partyId', serviceResult.partyId, 'roleTypeId', 'CUSTOMER').queryOne()
        assert partyRole
    }

    void testUpdatePartyCreditCard() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                userLogin: userLogin
        ]
        List<GenericValue> paymentMethodAndCreditCards = from('PaymentMethodAndCreditCard')
            .where('partyId', 'DemoCustomer')
            .filterByDate()
            .queryList()
        GenericValue paymentMethodAndCreditCard = paymentMethodAndCreditCards.first()

        serviceCtx.putAll(paymentMethodAndCreditCard.getAllFields())

        Map serviceResult = dispatcher.runSync('updateCreditCard', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String paymentMethodId = serviceResult.paymentMethodId
        String oldPaymentMethodId = serviceResult.oldPaymentMethodId

        GenericValue updatedPmac = from('PaymentMethodAndCreditCard')
            .where('paymentMethodId', paymentMethodId)
            .queryOne()

        assert updatedPmac
        assert paymentMethodId == oldPaymentMethodId
        assert updatedPmac.cardType == serviceCtx.cardType
        assert updatedPmac.cardNumber == serviceCtx.cardNumber

        serviceCtx.cardType = 'CCT_MASTERCARD'
        serviceCtx.cardNumber = '5500000000000004'
        Map serviceResult2 = dispatcher.runSync('updateCreditCard', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult2)

        String newPaymentMethodId = serviceResult2.paymentMethodId
        String newOldPaymentMethodId = serviceResult2.oldPaymentMethodId

        GenericValue newPmac = from('PaymentMethodAndCreditCard')
            .where('paymentMethodId', newPaymentMethodId)
            .queryOne()

        assert newPmac
        assert newPaymentMethodId != newOldPaymentMethodId
        assert newPmac.cardType == 'CCT_MASTERCARD'
        assert newPmac.cardNumber == '5500000000000004'
    }

    void testUpdatePartyRelationship() {
        Map serviceCtx = [
                partyIdFrom: 'TestCompany',
                partyIdTo: 'TestParty',
                roleTypeIdFrom: '_NA_',
                roleTypeIdTo: 'CONTACT',
                fromDate: java.sql.Timestamp.valueOf('2000-01-01 00:00:00'),
                partyRelationshipTypeId: 'AGENT',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyRelationship', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRelationship = from('PartyRelationship')
            .where('partyIdFrom',
                   'TestCompany',
                   'partyIdTo',
                   'TestParty',
                   'roleTypeIdFrom',
                   '_NA_',
                   'roleTypeIdTo',
                   'CONTACT',
                   'fromDate',
                   java.sql.Timestamp.valueOf('2000-01-01 00:00:00'))
            .queryOne()
        assert partyRelationship
        assert partyRelationship.partyRelationshipTypeId == 'AGENT'
    }

    void testUpdateUserPassword() {
        GenericValue partyUserLogin = org.apache.ofbiz.party.party.PartyWorker.findPartyLatestUserLogin('DemoCustomer', delegator)
        Map serviceCtx = dispatcher.getDispatchContext().makeValidContext('updatePassword',
            org.apache.ofbiz.service.ModelService.IN_PARAM,
            partyUserLogin)
        serviceCtx.newPassword = 'ofbiz-demo'
        serviceCtx.newPasswordVerify = 'ofbiz-demo'
        serviceCtx.userLogin = userLogin

        Map serviceResult = dispatcher.runSync('updatePassword', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyAndUserLogin = from('PartyAndUserLogin')
            .where('userLoginId', partyUserLogin.userLoginId, 'partyId', 'DemoCustomer')
            .queryOne()

        assert partyAndUserLogin
        assert partyUserLogin.currentPassword != partyAndUserLogin.currentPassword
        assert partyAndUserLogin.userLoginId == 'DemoCustomer'
    }

}
