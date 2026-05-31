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
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil

@SuppressWarnings(['LineLength', 'UnnecessaryObjectReferences', 'UnnecessaryGString', 'PublicMethodsBeforeNonPublicMethods', 'ClassSize', 'MethodCount', 'ConsecutiveBlankLines', 'BlockEndsWithBlankLine', 'ClassEndsWithBlankLine'])
class PartyTests extends OFBizTestCase {

    PartyTests(String name) {
        super(name)
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

    void testCreateNewCommEvent() {
        Map createNewCommEventMap = [
                communicationEventTypeId: 'EMAIL_COMMUNICATION',
                statusId: 'COM_ENTERED',
                partyIdFrom: 'DemoCustomer',
                contactMechTypeId: 'EMAIL_ADDRESS',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEvent', createNewCommEventMap)
        assert ServiceUtil.isSuccess(serviceResult)
        String communicationEventId = serviceResult.communicationEventId

        Map updateCommEventMap = [
                communicationEventId: communicationEventId,
                communicationEventTypeId: 'AUTO_EMAIL_COMM',
                statusId: 'COM_COMPLETE',
                partyIdFrom: 'admin',
                contactMechTypeId: 'ELECTRONIC_ADDRESS',
                userLogin: userLogin
        ]
        Map serviceResult2 = dispatcher.runSync('updateCommunicationEvent', updateCommEventMap)
        assert ServiceUtil.isSuccess(serviceResult2)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.communicationEventTypeId == updateCommEventMap.communicationEventTypeId
        assert communicationEvent.statusId == updateCommEventMap.statusId
        assert communicationEvent.partyIdFrom == updateCommEventMap.partyIdFrom
        assert communicationEvent.contactMechTypeId == updateCommEventMap.contactMechTypeId
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

    void testCreateCommunicationEvent() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-3',
                communicationEventTypeId: 'EMAIL_COMMUNICATION',
                statusId: 'COM_COMPLETE',
                fromString: 'send@example.com',
                toString: 'receive@example.com',
                subject: 'Why i would use the OFBiz system',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-3').queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
        assert communicationEvent.subject == 'Why i would use the OFBiz system'
    }

    void testCreateCommunicationEventRole() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-6',
                partyId: 'TestCompany',
                roleTypeId: 'ADDRESSEE',
                statusId: 'COM_ROLE_CREATED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-6', 'roleTypeId', 'ADDRESSEE', 'partyId', 'TestCompany')
            .queryOne()

        assert communicationEventRole
        assert communicationEventRole.statusId == 'COM_ROLE_CREATED'
    }

    void testCreateCommunicationEventRoleWithoutPermission() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-6',
                partyId: 'TestCompany',
                roleTypeId: 'INTERNAL_ORGANIZATIO',
                statusId: 'COM_ROLE_CREATED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventRoleWithoutPermission', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-6', 'roleTypeId', 'INTERNAL_ORGANIZATIO', 'partyId', 'TestCompany')
            .queryOne()

        assert communicationEventRole
        assert communicationEventRole.statusId == 'COM_ROLE_CREATED'
    }

    void testCreateCommunicationEventWithoutPermission() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-4',
                communicationEventTypeId: 'EMAIL_COMMUNICATION',
                statusId: 'COM_COMPLETE',
                fromString: 'send@example.com',
                toString: 'receive@example.com',
                subject: 'Why i would use the OFBiz system',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventWithoutPermission', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-4').queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
        assert communicationEvent.subject == 'Why i would use the OFBiz system'
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

    void testCreatePartyIdentifications() {
        Map serviceCtx = [
                partyId: 'TestCustomer',
                identifications: [
                partyIdentificationTypeId: 'CARD_ID',
                CARD_ID: '123456789'
            ],
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyIdentifications', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyIdentification = from('PartyIdentification')
            .where('partyId', 'TestCustomer', 'partyIdentificationTypeId', 'CARD_ID')
            .queryOne()
        assert partyIdentification
        assert partyIdentification.idValue == '123456789'
    }

    void testCreatePartyNote() {
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                noteName: 'Demo Note',
                note: 'This is demo note to test createPartyNote service',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyNote = from('PartyNote').where('partyId', 'DemoCustomer', 'noteId', serviceResult.noteId).queryOne()
        assert partyNote

        GenericValue noteData = from('NoteData').where('noteId', serviceResult.noteId).queryOne()
        assert noteData
        assert noteData.noteName == 'Demo Note'
        assert noteData.noteInfo == 'This is demo note to test createPartyNote service'
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

    void testDeleteCommunicationEvent() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-1').queryOne()
        assert !communicationEvent
    }

    void testDeleteCommunicationEventWorkEffort() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-5',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteCommunicationEventWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-5').queryOne()
        assert !communicationEvent

        List<GenericValue> communicationEventWorkEff = from('CommunicationEventWorkEff').where('communicationEventId', 'TestEvent-5').queryList()
        assert !communicationEventWorkEff
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

    void testRemoveCommunicationEventRole() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-5',
                partyId: 'TestCompany',
                roleTypeId: 'ADDRESSEE',
                statusId: 'COM_ROLE_CREATED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-5', 'partyId', 'TestCompany', 'roleTypeId', 'ADDRESSEE')
            .queryOne()
        assert !communicationEventRole
    }

    void testSetCommEventComplete() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-6',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommEventComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-6').queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
    }

    void testSetCommEventRoleToRead() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-7',
                partyId: 'TestCompany',
                roleTypeId: 'ADDRESSEE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommEventRoleToRead', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-7', 'partyId', 'TestCompany', 'roleTypeId', 'ADDRESSEE', 'statusId', 'COM_ROLE_READ')
            .queryOne()
        assert communicationEventRole
    }

    void testSetCommunicationEventStatus() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-6',
                statusId: 'COM_COMPLETE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommunicationEventStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-6').queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
    }

    void testUpdateCommunicationEvent() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-7',
                statusId: 'COM_COMPLETE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', 'TestEvent-7').queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
    }

    void testUpdateCommunicationEventRole() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-2',
                partyId: 'TestCompany',
                roleTypeId: 'ADDRESSEE',
                statusId: 'COM_ROLE_READ',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-2', 'partyId', 'TestCompany', 'roleTypeId', 'ADDRESSEE')
            .queryOne()
        assert communicationEventRole
        assert communicationEventRole.statusId == 'COM_ROLE_READ'
    }

    void testSetCommunicationEventRoleStatus() {
        Map serviceCtx = [
                communicationEventId: 'TestEvent-2',
                partyId: 'TestCompany',
                roleTypeId: 'ADDRESSEE',
                statusId: 'COM_ROLE_COMPLETED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommunicationEventRoleStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', 'TestEvent-2', 'partyId', 'TestCompany', 'roleTypeId', 'ADDRESSEE', 'statusId', 'COM_ROLE_COMPLETED')
            .queryOne()
        assert communicationEventRole
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

    void testLookupParty() {
        Map serviceCtx = [
                firstName: 'Test',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('lookupParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.lookupResult
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

    void testCreateAffiliate() {
        Map serviceCtx = [
                partyId: 'TestCompany',
                affiliateName: 'Test Affiliate',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAffiliate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue affiliate = from('Affiliate').where('partyId', serviceResult.partyId).queryOne()
        assert affiliate
        assert affiliate.affiliateName == 'Test Affiliate'
    }

    void testUpdateAffiliate() {
        Map serviceCtx = [
                partyId: 'TestGroup-1',
                affiliateName: 'Test Affiliate',
                siteType: 'Main Site',
                siteVisitors: '2000',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateAffiliate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue affiliate = from('Affiliate').where('partyId', 'TestGroup-1').queryOne()
        assert affiliate
        assert affiliate.affiliateName == 'Test Affiliate'
        assert affiliate.siteType == 'Main Site'
        assert affiliate.siteVisitors == '2000'
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

    void testCreateAddressMatchMap() {
        Map serviceCtx = [
                mapKey: 'TEST_KEY',
                mapValue: 'TEST VALUE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue addressMatchMap = from('AddressMatchMap').where('mapKey', 'TEST_KEY', 'mapValue', 'TEST VALUE').queryOne()
        assert addressMatchMap
    }

    void testRemoveAddressMatchMap() {
        Map serviceCtx = [
                mapKey: 'TESTKEY-1',
                mapValue: 'Test Value 1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue addressMatchMap = from('AddressMatchMap').where('mapKey', 'TESTKEY-1', 'mapValue', 'Test Value 1').queryOne()
        assert !addressMatchMap
    }

    void testCreatePartyInvitation() {
        Map serviceCtx = [
                partyIdFrom: 'TestCompany',
                partyId: 'TestCustomer',
                emailAddress: 'test_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', serviceResult.partyInvitationId).queryOne()
        assert partyInvitation
        assert partyInvitation.emailAddress == 'test_email@example.com'
    }

    void testUpdatePartyInvitation() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE',
                emailAddress: 'test_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', 'TEST_INVITE').queryOne()
        assert partyInvitation
        assert partyInvitation.emailAddress == 'test_email@example.com'
    }

    void testDeletePartyInvitation() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE-1',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', 'TEST_INVITE-1').queryOne()
        assert !partyInvitation
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

    void testCreateEmailAddressVerification() {
        Map serviceCtx = [
                emailAddress: 'test_email@example.com',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddressVerification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue emailAddressVerification = from('EmailAddressVerification').where('emailAddress', 'test_email@example.com').queryOne()
        assert emailAddressVerification
        assert emailAddressVerification.verifyHash == serviceResult.verifyHash
    }

    void testCreatePartyInvitationRoleAssoc() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE',
                roleTypeId: 'COMMEVENT_ROLE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitationRoleAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationRoleAssoc = from('PartyInvitationRoleAssoc').where('partyInvitationId',
                                                                                       'TEST_INVITE',
                                                                                       'roleTypeId',
                                                                                       'COMMEVENT_ROLE').queryOne()
        assert partyInvitationRoleAssoc
    }

    void testDeletePartyInvitationRoleAssoc() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE-2',
                roleTypeId: 'COMMEVENT_ROLE',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitationRoleAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationRoleAssoc = from('PartyInvitationRoleAssoc').where('partyInvitationId',
                                                                                       'TEST_INVITE-2',
                                                                                       'roleTypeId',
                                                                                       'COMMEVENT_ROLE').queryOne()
        assert !partyInvitationRoleAssoc
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

    void testCreatePartyInvitationGroupAssoc() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE',
                partyIdTo: 'TestCompany',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitationGroupAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationGroupAssoc = from('PartyInvitationGroupAssoc').where('partyInvitationId',
                                                                                         'TEST_INVITE',
                                                                                         'partyIdTo',
                                                                                         'TestCompany').queryOne()
        assert partyInvitationGroupAssoc
    }

    void testDeletePartyInvitationGroupAssoc() {
        Map serviceCtx = [
                partyInvitationId: 'TEST_INVITE-2',
                partyIdTo: 'TestCompany',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitationGroupAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationGroupAssoc = from('PartyInvitationGroupAssoc').where('partyInvitationId',
                                                                                         'TEST_INVITE-2',
                                                                                         'partyIdTo',
                                                                                         'TestCompany').queryOne()
        assert !partyInvitationGroupAssoc
    }

    void testClearAddressMatchMap() {
        Map serviceCtx = [
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('clearAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> addrs = from('AddressMatchMap').queryList()
        assert !addrs
    }

}
