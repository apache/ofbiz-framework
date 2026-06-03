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

class PartyCommunicationEventTests extends OFBizTestCase {

    PartyCommunicationEventTests(String name) {
        super(name)
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

}
