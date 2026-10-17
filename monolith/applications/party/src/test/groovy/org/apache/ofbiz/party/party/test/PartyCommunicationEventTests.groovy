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
class PartyCommunicationEventTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateCommunicationEvent() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-3'
        String communicationEventTypeId = testParams.communicationEventTypeId ?: 'EMAIL_COMMUNICATION'
        String statusId = testParams.statusId ?: 'COM_COMPLETE'
        String fromString = testParams.fromString ?: 'send@example.com'
        String toString = testParams.toString ?: 'receive@example.com'
        String subject = testParams.subject ?: 'Why i would use the OFBiz system'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                communicationEventTypeId: communicationEventTypeId,
                statusId: statusId,
                fromString: fromString,
                toString: toString,
                subject: subject,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == statusId
        assert communicationEvent.subject == subject
    }

    @Test
    @Order(2)
    void testCreateCommunicationEventRole() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-6'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'ADDRESSEE'
        String statusId = testParams.statusId ?: 'COM_ROLE_CREATED'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'roleTypeId', roleTypeId, 'partyId', partyId)
            .queryOne()

        assert communicationEventRole
        assert communicationEventRole.statusId == statusId
    }

    @Test
    @Order(3)
    void testCreateCommunicationEventRoleWithoutPermission() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-6'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'INTERNAL_ORGANIZATIO'
        String statusId = testParams.statusId ?: 'COM_ROLE_CREATED'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventRoleWithoutPermission', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'roleTypeId', roleTypeId, 'partyId', partyId)
            .queryOne()

        assert communicationEventRole
        assert communicationEventRole.statusId == statusId
    }

    @Test
    @Order(4)
    void testCreateCommunicationEventWithoutPermission() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-4'
        String communicationEventTypeId = testParams.communicationEventTypeId ?: 'EMAIL_COMMUNICATION'
        String statusId = testParams.statusId ?: 'COM_COMPLETE'
        String fromString = testParams.fromString ?: 'send@example.com'
        String toString = testParams.toString ?: 'receive@example.com'
        String subject = testParams.subject ?: 'Why i would use the OFBiz system'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                communicationEventTypeId: communicationEventTypeId,
                statusId: statusId,
                fromString: fromString,
                toString: toString,
                subject: subject,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEventWithoutPermission', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == statusId
        assert communicationEvent.subject == subject
    }

    @Test
    @Order(5)
    void testCreateNewCommEvent() {
        String communicationEventTypeId = testParams.communicationEventTypeId ?: 'EMAIL_COMMUNICATION'
        String statusId = testParams.statusId ?: 'COM_ENTERED'
        String partyIdFrom = testParams.partyIdFrom ?: 'DemoCustomer'
        String contactMechTypeId = testParams.contactMechTypeId ?: 'EMAIL_ADDRESS'
        String communicationEventTypeId1 = testParams.communicationEventTypeId1 ?: 'AUTO_EMAIL_COMM'
        String statusId1 = testParams.statusId1 ?: 'COM_COMPLETE'
        String partyIdFrom1 = testParams.partyIdFrom1 ?: 'admin'
        String contactMechTypeId1 = testParams.contactMechTypeId1 ?: 'ELECTRONIC_ADDRESS'
        Map createNewCommEventMap = [
                communicationEventTypeId: communicationEventTypeId,
                statusId: statusId,
                partyIdFrom: partyIdFrom,
                contactMechTypeId: contactMechTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createCommunicationEvent', createNewCommEventMap)
        assert ServiceUtil.isSuccess(serviceResult)
        String communicationEventId = serviceResult.communicationEventId

        Map updateCommEventMap = [
                communicationEventId: communicationEventId,
                communicationEventTypeId: communicationEventTypeId1,
                statusId: statusId1,
                partyIdFrom: partyIdFrom1,
                contactMechTypeId: contactMechTypeId1,
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

    @Test
    @Order(6)
    void testDeleteCommunicationEvent() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-1'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert !communicationEvent
    }

    @Test
    @Order(7)
    void testDeleteCommunicationEventWorkEffort() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-5'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('deleteCommunicationEventWorkEffort', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert !communicationEvent

        List<GenericValue> communicationEventWorkEff = from('CommunicationEventWorkEff')
                .where('communicationEventId', communicationEventId)
                .queryList()
        assert !communicationEventWorkEff
    }

    @Test
    @Order(8)
    void testRemoveCommunicationEventRole() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-5'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'ADDRESSEE'
        String statusId = testParams.statusId ?: 'COM_ROLE_CREATED'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('removeCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'partyId', partyId, 'roleTypeId', roleTypeId)
            .queryOne()
        assert !communicationEventRole
    }

    @Test
    @Order(9)
    void testSetCommEventComplete() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-6'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommEventComplete', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == 'COM_COMPLETE'
    }

    @Test
    @Order(10)
    void testSetCommEventRoleToRead() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-7'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'ADDRESSEE'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommEventRoleToRead', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'partyId', partyId, 'roleTypeId', roleTypeId, 'statusId', 'COM_ROLE_READ')
            .queryOne()
        assert communicationEventRole
    }

    @Test
    // Must run after testUpdateCommunicationEventRole: this sets the CommunicationEventRole
    // (TestEvent-2/TestCompany/ADDRESSEE) to COM_ROLE_COMPLETED, and COMPLETED -> READ is not a
    // valid transition, which is what testUpdateCommunicationEventRole needs to do to that role.
    @Order(14)
    void testSetCommunicationEventRoleStatus() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-2'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'ADDRESSEE'
        String statusId = testParams.statusId ?: 'COM_ROLE_COMPLETED'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommunicationEventRoleStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'partyId', partyId, 'roleTypeId', roleTypeId, 'statusId', statusId)
            .queryOne()
        assert communicationEventRole
    }

    @Test
    @Order(12)
    void testSetCommunicationEventStatus() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-6'
        String statusId = testParams.statusId ?: 'COM_COMPLETE'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('setCommunicationEventStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == statusId
    }

    @Test
    @Order(13)
    void testUpdateCommunicationEvent() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-7'
        String statusId = testParams.statusId ?: 'COM_COMPLETE'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateCommunicationEvent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEvent = from('CommunicationEvent').where('communicationEventId', communicationEventId).queryOne()
        assert communicationEvent
        assert communicationEvent.statusId == statusId
    }

    @Test
    @Order(11)
    void testUpdateCommunicationEventRole() {
        String communicationEventId = testParams.communicationEventId ?: 'TestEvent-2'
        String partyId = testParams.partyId ?: 'TestCompany'
        String roleTypeId = testParams.roleTypeId ?: 'ADDRESSEE'
        String statusId = testParams.statusId ?: 'COM_ROLE_READ'
        Map serviceCtx = [
                communicationEventId: communicationEventId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                statusId: statusId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateCommunicationEventRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue communicationEventRole = from('CommunicationEventRole')
            .where('communicationEventId', communicationEventId, 'partyId', partyId, 'roleTypeId', roleTypeId)
            .queryOne()
        assert communicationEventRole
        assert communicationEventRole.statusId == statusId
    }

}
