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

class PartyMiscTests extends OFBizTestCase {

    PartyMiscTests(String name) {
        super(name)
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

    void testRemoveAddressMatchMap() {
        // Create the record first so this test is independent of seed data and execution order
        dispatcher.runSync('createAddressMatchMap', [mapKey: 'TESTKEY-1', mapValue: 'Test Value 1', userLogin: userLogin])

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

}
