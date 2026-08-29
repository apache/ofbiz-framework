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
class PartyMiscTests implements JupiterTestHelper {

    @Test
    // Must run after testRemoveAddressMatchMap: this wipes the whole AddressMatchMap table
    // (delegator.removeAll), and running it first breaks testRemoveAddressMatchMap's own
    // create-then-remove of a fresh row ("Value not found, cannot remove") - confirmed by
    // isolating this from testCreateAddressMatchMap's position, which does not matter.
    @Order(15)
    void testClearAddressMatchMap() {
        Map serviceCtx = [
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('clearAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> addrs = from('AddressMatchMap').queryList()
        assert !addrs
    }

    @Test
    @Order(1)
    void testCreateAddressMatchMap() {
        String mapKey = testParams.mapKey ?: 'TEST_KEY'
        String mapValue = testParams.mapValue ?: 'TEST VALUE'
        Map serviceCtx = [
                mapKey: mapKey,
                mapValue: mapValue,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue addressMatchMap = from('AddressMatchMap').where('mapKey', mapKey, 'mapValue', mapValue).queryOne()
        assert addressMatchMap
    }

    @Test
    @Order(2)
    void testCreateAffiliate() {
        String partyId = testParams.partyId ?: 'TestCompany'
        String affiliateName = testParams.affiliateName ?: 'Test Affiliate'
        Map serviceCtx = [
                partyId: partyId,
                affiliateName: affiliateName,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createAffiliate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue affiliate = from('Affiliate').where('partyId', serviceResult.partyId).queryOne()
        assert affiliate
        assert affiliate.affiliateName == affiliateName
    }

    @Test
    @Order(3)
    void testCreateEmailAddressVerification() {
        String emailAddress = testParams.emailAddress ?: 'test_email@example.com'
        Map serviceCtx = [
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createEmailAddressVerification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue emailAddressVerification = from('EmailAddressVerification').where('emailAddress', emailAddress).queryOne()
        assert emailAddressVerification
        assert emailAddressVerification.verifyHash == serviceResult.verifyHash
    }

    @Test
    @Order(4)
    void testCreatePartyIdentifications() {
        String partyId = testParams.partyId ?: 'TestCustomer'
        String partyIdentificationTypeId = testParams.partyIdentificationTypeId ?: 'CARD_ID'
        String idValue = testParams.idValue ?: '123456789'
        Map serviceCtx = [
                partyId: partyId,
                identifications: [
                partyIdentificationTypeId: partyIdentificationTypeId,
                (partyIdentificationTypeId): idValue
            ],
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyIdentifications', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyIdentification = from('PartyIdentification')
            .where('partyId', partyId, 'partyIdentificationTypeId', partyIdentificationTypeId)
            .queryOne()
        assert partyIdentification
        assert partyIdentification.idValue == idValue
    }

    @Test
    @Order(5)
    void testCreatePartyInvitation() {
        String partyIdFrom = testParams.partyIdFrom ?: 'TestCompany'
        String partyId = testParams.partyId ?: 'TestCustomer'
        String emailAddress = testParams.emailAddress ?: 'test_email@example.com'
        Map serviceCtx = [
                partyIdFrom: partyIdFrom,
                partyId: partyId,
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', serviceResult.partyInvitationId).queryOne()
        assert partyInvitation
        assert partyInvitation.emailAddress == emailAddress
    }

    @Test
    @Order(6)
    void testCreatePartyInvitationGroupAssoc() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE'
        String partyIdTo = testParams.partyIdTo ?: 'TestCompany'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                partyIdTo: partyIdTo,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitationGroupAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationGroupAssoc = from('PartyInvitationGroupAssoc').where('partyInvitationId',
                                                                                         partyInvitationId,
                                                                                         'partyIdTo',
                                                                                         partyIdTo).queryOne()
        assert partyInvitationGroupAssoc
    }

    @Test
    @Order(7)
    void testCreatePartyInvitationRoleAssoc() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE'
        String roleTypeId = testParams.roleTypeId ?: 'COMMEVENT_ROLE'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyInvitationRoleAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationRoleAssoc = from('PartyInvitationRoleAssoc').where('partyInvitationId',
                                                                                       partyInvitationId,
                                                                                       'roleTypeId',
                                                                                       roleTypeId).queryOne()
        assert partyInvitationRoleAssoc
    }

    @Test
    @Order(8)
    void testCreatePartyNote() {
        String partyId = testParams.partyId ?: 'DemoCustomer'
        String noteName = testParams.noteName ?: 'Demo Note'
        String note = testParams.note ?: 'This is demo note to test createPartyNote service'
        Map serviceCtx = [
                partyId: partyId,
                noteName: noteName,
                note: note,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPartyNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyNote = from('PartyNote').where('partyId', partyId, 'noteId', serviceResult.noteId).queryOne()
        assert partyNote

        GenericValue noteData = from('NoteData').where('noteId', serviceResult.noteId).queryOne()
        assert noteData
        assert noteData.noteName == noteName
        assert noteData.noteInfo == note
    }

    @Test
    @Order(9)
    void testDeletePartyInvitation() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE-1'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', partyInvitationId).queryOne()
        assert !partyInvitation
    }

    @Test
    @Order(10)
    void testDeletePartyInvitationGroupAssoc() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE-2'
        String partyIdTo = testParams.partyIdTo ?: 'TestCompany'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                partyIdTo: partyIdTo,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitationGroupAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationGroupAssoc = from('PartyInvitationGroupAssoc').where('partyInvitationId',
                                                                                         partyInvitationId,
                                                                                         'partyIdTo',
                                                                                         partyIdTo).queryOne()
        assert !partyInvitationGroupAssoc
    }

    @Test
    @Order(11)
    void testDeletePartyInvitationRoleAssoc() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE-2'
        String roleTypeId = testParams.roleTypeId ?: 'COMMEVENT_ROLE'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deletePartyInvitationRoleAssoc', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitationRoleAssoc = from('PartyInvitationRoleAssoc').where('partyInvitationId',
                                                                                       partyInvitationId,
                                                                                       'roleTypeId',
                                                                                       roleTypeId).queryOne()
        assert !partyInvitationRoleAssoc
    }

    @Test
    @Order(12)
    void testRemoveAddressMatchMap() {
        String mapKey = testParams.mapKey ?: 'TESTKEY-1'
        String mapValue = testParams.mapValue ?: 'Test Value 1'
        // Create the record first so this test is independent of seed data and execution order
        dispatcher.runSync('createAddressMatchMap', [mapKey: mapKey, mapValue: mapValue, userLogin: userLogin])

        Map serviceCtx = [
                mapKey: mapKey,
                mapValue: mapValue,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeAddressMatchMap', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue addressMatchMap = from('AddressMatchMap').where('mapKey', mapKey, 'mapValue', mapValue).queryOne()
        assert !addressMatchMap
    }

    @Test
    @Order(13)
    void testUpdateAffiliate() {
        String partyId = testParams.partyId ?: 'TestGroup-1'
        String affiliateName = testParams.affiliateName ?: 'Test Affiliate'
        String siteType = testParams.siteType ?: 'Main Site'
        String siteVisitors = testParams.siteVisitors ?: '2000'
        Map serviceCtx = [
                partyId: partyId,
                affiliateName: affiliateName,
                siteType: siteType,
                siteVisitors: siteVisitors,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateAffiliate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue affiliate = from('Affiliate').where('partyId', partyId).queryOne()
        assert affiliate
        assert affiliate.affiliateName == affiliateName
        assert affiliate.siteType == siteType
        assert affiliate.siteVisitors == siteVisitors
    }

    @Test
    @Order(14)
    void testUpdatePartyInvitation() {
        String partyInvitationId = testParams.partyInvitationId ?: 'TEST_INVITE'
        String emailAddress = testParams.emailAddress ?: 'test_email@example.com'
        Map serviceCtx = [
                partyInvitationId: partyInvitationId,
                emailAddress: emailAddress,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updatePartyInvitation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyInvitation = from('PartyInvitation').where('partyInvitationId', partyInvitationId).queryOne()
        assert partyInvitation
        assert partyInvitation.emailAddress == emailAddress
    }

}
