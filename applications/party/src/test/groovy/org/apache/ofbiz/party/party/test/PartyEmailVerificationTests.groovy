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

import static org.junit.jupiter.api.Assertions.assertThrows

import java.nio.charset.StandardCharsets

import jakarta.transaction.Status

import org.apache.ofbiz.base.crypto.HashCrypt
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.transaction.TransactionUtil
import org.apache.ofbiz.service.ServiceAuthException
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class PartyEmailVerificationTests implements JupiterTestHelper {

    @Test
    @Order(3)
    void testCreateEmailAddressVerification() {
        String contactMechId = createTestEmail('verify_create@example.com')
        Map serviceResult = dispatcher.runSync('createEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: contactMechId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.token

        GenericValue verification = from('EmailAddressVerification')
                .where('emailAddress', 'verify_create@example.com').queryOne()
        assert verification.partyId == 'TestParty'
        assert verification.contactMechId == contactMechId
        assert verification.verifyHash == HashCrypt.digestHash('SHA-256',
                serviceResult.token.getBytes(StandardCharsets.UTF_8))
        assert verification.expireDate.after(new java.sql.Timestamp(System.currentTimeMillis()))
    }

    @Test
    @Order(3)
    void testVerifyEmailAddress() {
        String contactMechId = createTestEmail('verify_ok@example.com')
        String token = issueToken(contactMechId)

        assert ServiceUtil.isSuccess(dispatcher.runSync('verifyEmailAddress', [token: token]))
        assert verifiedFlag(contactMechId) == 'Y'
        assert !from('EmailAddressVerification').where('emailAddress', 'verify_ok@example.com').queryOne()

        // the token works once
        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: token]))
    }

    @Test
    @Order(3)
    void testVerifyEmailAddressRejectsUnknownToken() {
        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: 'not-a-real-token']))
    }

    @Test
    @Order(3)
    void testVerifyEmailAddressRejectsExpiredToken() {
        String contactMechId = createTestEmail('verify_expired@example.com')
        String token = issueToken(contactMechId)
        GenericValue verification = from('EmailAddressVerification')
                .where('emailAddress', 'verify_expired@example.com').queryOne()
        verification.expireDate = new java.sql.Timestamp(System.currentTimeMillis() - 60000)
        verification.store()

        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: token]))
        assert verifiedFlag(contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testVerifyEmailAddressRejectsChangedAddress() {
        String contactMechId = createTestEmail('verify_before@example.com')
        String token = issueToken(contactMechId)
        GenericValue contactMech = from('ContactMech').where(contactMechId: contactMechId).queryOne()
        contactMech.infoString = 'verify_after@example.com'
        contactMech.store()

        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: token]))
        assert verifiedFlag(contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testVerifyEmailAddressOnlyAcceptsLatestToken() {
        String contactMechId = createTestEmail('verify_resend@example.com')
        String firstToken = issueToken(contactMechId)
        String secondToken = issueToken(contactMechId)

        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: firstToken]))
        assert ServiceUtil.isSuccess(dispatcher.runSync('verifyEmailAddress', [token: secondToken]))
        assert verifiedFlag(contactMechId) == 'Y'
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerification() {
        String contactMechId = createTestEmail('verify_send@example.com')
        Map result = dispatcher.runSync('sendEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: contactMechId, productStoreId: '9000', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)

        GenericValue verification = from('EmailAddressVerification')
                .where('emailAddress', 'verify_send@example.com').queryOne()
        assert verification?.contactMechId == contactMechId
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationByOwner() {
        String contactMechId = createTestEmail('verify_owner@example.com')
        Map result = dispatcher.runSync('sendEmailAddressVerification',
                [contactMechId: contactMechId, productStoreId: '9000', userLogin: getUserLogin('TestParty')])
        assert ServiceUtil.isSuccess(result)
        assert from('EmailAddressVerification').where('emailAddress', 'verify_owner@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationSkipsStoreWithoutSetting() {
        String contactMechId = createTestEmail('verify_nostore@example.com')
        Map result = dispatcher.runSync('sendEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: contactMechId, productStoreId: 'NoSuchStore', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert !from('EmailAddressVerification').where('emailAddress', 'verify_nostore@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationRejectsOtherParty() {
        Map created = dispatcher.runSync('createPartyEmailAddress',
                [partyId: 'TestCustomer', emailAddress: 'verify_other@example.com', userLogin: userLogin])
        assert ServiceUtil.isSuccess(created)
        assertThrows(ServiceAuthException) {
            dispatcher.runSync('sendEmailAddressVerification',
                    [partyId: 'TestCustomer', contactMechId: created.contactMechId, productStoreId: '9000',
                     userLogin: getUserLogin('TestParty')])
        }
        assert !from('EmailAddressVerification').where('emailAddress', 'verify_other@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testChangedEmailAddressIsNotVerified() {
        String contactMechId = createTestEmail('verify_old@example.com')
        assert ServiceUtil.isSuccess(dispatcher.runSync('verifyEmailAddress', [token: issueToken(contactMechId)]))
        assert verifiedFlag(contactMechId) == 'Y'

        Map result = dispatcher.runSync('updatePartyEmailAddress', [partyId: 'TestParty',
                contactMechId: contactMechId, emailAddress: 'verify_new@example.com', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert result.contactMechId != contactMechId
        assert verifiedFlag(result.contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationMailFailureKeepsTransaction() {
        GenericValue storeEmail = from('ProductStoreEmailSetting')
                .where(productStoreId: '9000', emailType: 'PRDS_EMAIL_VERIFY').queryOne()
        String bodyScreenLocation = storeEmail.bodyScreenLocation
        storeEmail.bodyScreenLocation = 'component://party/widget/partymgr/NoSuchScreens.xml#NoSuchScreen'
        storeEmail.store()
        String contactMechId = createTestEmail('verify_badscreen@example.com')
        boolean began = TransactionUtil.begin()
        try {
            Map result = dispatcher.runSync('sendEmailAddressVerification',
                    [partyId: 'TestParty', contactMechId: contactMechId, productStoreId: '9000', userLogin: userLogin])
            assert ServiceUtil.isSuccess(result)
            assert TransactionUtil.getStatus() != Status.STATUS_MARKED_ROLLBACK
        } finally {
            if (TransactionUtil.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                TransactionUtil.rollback(began, 'test cleanup', null)
            } else {
                TransactionUtil.commit(began)
            }
            storeEmail.bodyScreenLocation = bodyScreenLocation
            storeEmail.store()
        }
    }

    @Test
    @Order(3)
    void testCreateEmailAddressVerificationRejectsNonEmailContactMech() {
        Map telecom = dispatcher.runSync('createPartyTelecomNumber',
                [partyId: 'TestParty', contactNumber: '5550100', userLogin: userLogin])
        assert ServiceUtil.isSuccess(telecom)
        assert ServiceUtil.isError(dispatcher.runSync('createEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: telecom.contactMechId, userLogin: userLogin]))
    }

    @Test
    @Order(3)
    void testCreateEmailAddressVerificationRejectsOtherPartyContactMech() {
        Map other = dispatcher.runSync('createPartyEmailAddress',
                [partyId: 'TestCustomer', emailAddress: 'verify_foreign@example.com', userLogin: userLogin])
        assert ServiceUtil.isSuccess(other)
        assert ServiceUtil.isError(dispatcher.runSync('createEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: other.contactMechId, userLogin: userLogin]))
        assert !from('EmailAddressVerification').where('emailAddress', 'verify_foreign@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testVerifyEmailAddressRejectsExpiredContactMech() {
        String contactMechId = createTestEmail('verify_expiredmech@example.com')
        String token = issueToken(contactMechId)
        GenericValue partyContactMech = from('PartyContactMech').where(partyId: 'TestParty', contactMechId: contactMechId)
                .filterByDate().queryFirst()
        partyContactMech.thruDate = new java.sql.Timestamp(System.currentTimeMillis() - 60000)
        partyContactMech.store()

        assert ServiceUtil.isError(dispatcher.runSync('verifyEmailAddress', [token: token]))
        assert from('EmailAddressVerification').where('emailAddress', 'verify_expiredmech@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testCreatePartyEmailAddressIgnoresVerifiedInput() {
        Map result = dispatcher.runSync('createPartyEmailAddress', [partyId: 'TestParty',
                emailAddress: 'verify_forged@example.com', verified: 'Y', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert verifiedFlag(result.contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testUpdatePartyContactMechKeepsVerifiedFlag() {
        String contactMechId = createTestEmail('verify_keep@example.com')
        assert ServiceUtil.isSuccess(dispatcher.runSync('verifyEmailAddress', [token: issueToken(contactMechId)]))
        Map result = dispatcher.runSync('updatePartyContactMech', [partyId: 'TestParty', contactMechId: contactMechId,
                newContactMechId: contactMechId, contactMechTypeId: 'EMAIL_ADDRESS', comments: 'changed comment',
                verified: 'N', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert verifiedFlag(contactMechId) == 'Y'
    }

    @Test
    @Order(3)
    void testUpdatePartyContactMechCannotSetVerifiedOnUnverifiedAddress() {
        String contactMechId = createTestEmail('verify_forge_pcm@example.com')
        assert verifiedFlag(contactMechId) != 'Y'
        Map result = dispatcher.runSync('updatePartyContactMech', [partyId: 'TestParty', contactMechId: contactMechId,
                newContactMechId: contactMechId, contactMechTypeId: 'EMAIL_ADDRESS', comments: 'forged',
                verified: 'Y', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert verifiedFlag(contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testUpdatePartyEmailAddressCannotSetVerifiedOnUnverifiedAddress() {
        String contactMechId = createTestEmail('verify_forge_pea@example.com')
        Map result = dispatcher.runSync('updatePartyEmailAddress', [partyId: 'TestParty', contactMechId: contactMechId,
                emailAddress: 'verify_forge_pea@example.com', verified: 'Y', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert verifiedFlag(result.contactMechId) != 'Y'
    }

    @Test
    @Order(3)
    void testCreateEmailAddressVerificationRejectsOtherParty() {
        Map created = dispatcher.runSync('createPartyEmailAddress',
                [partyId: 'TestCustomer', emailAddress: 'verify_token_other@example.com', userLogin: userLogin])
        assert ServiceUtil.isSuccess(created)
        assertThrows(ServiceAuthException) {
            dispatcher.runSync('createEmailAddressVerification',
                    [partyId: 'TestCustomer', contactMechId: created.contactMechId, userLogin: getUserLogin('TestParty')])
        }
        assert !from('EmailAddressVerification').where('emailAddress', 'verify_token_other@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationIgnoresWebSiteOfAnotherStore() {
        String contactMechId = createTestEmail('verify_website@example.com')
        GenericValue otherSite = from('WebSite')
                .where(EntityCondition.makeCondition('productStoreId', EntityOperator.NOT_EQUAL, '9000'))
                .queryList().find { it.productStoreId }
        Map result = dispatcher.runSync('sendEmailAddressVerification', [partyId: 'TestParty',
                contactMechId: contactMechId, productStoreId: '9000',
                webSiteId: otherSite?.webSiteId ?: 'NoSuchWebSite', userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assert from('EmailAddressVerification').where('emailAddress', 'verify_website@example.com').queryOne()
    }

    @Test
    @Order(3)
    void testSendEmailAddressVerificationIsNotRepeatedRightAway() {
        String contactMechId = createTestEmail('verify_throttle@example.com')
        Map send = [partyId: 'TestParty', contactMechId: contactMechId, productStoreId: '9000', userLogin: userLogin]
        assert ServiceUtil.isSuccess(dispatcher.runSync('sendEmailAddressVerification', send))
        String firstHash = from('EmailAddressVerification')
                .where('emailAddress', 'verify_throttle@example.com').queryOne().verifyHash

        assert ServiceUtil.isSuccess(dispatcher.runSync('sendEmailAddressVerification', send))
        assert from('EmailAddressVerification')
                .where('emailAddress', 'verify_throttle@example.com').queryOne().verifyHash == firstHash
    }

    private String createTestEmail(String emailAddress) {
        Map result = dispatcher.runSync('createPartyEmailAddress',
                [partyId: 'TestParty', emailAddress: emailAddress, userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        return result.contactMechId
    }

    private String issueToken(String contactMechId) {
        Map result = dispatcher.runSync('createEmailAddressVerification',
                [partyId: 'TestParty', contactMechId: contactMechId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        return result.token
    }

    private String verifiedFlag(String contactMechId) {
        return from('PartyContactMech').where(partyId: 'TestParty', contactMechId: contactMechId)
                .filterByDate().queryFirst().verified
    }

}
