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
package org.apache.ofbiz.order.order.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.service.ServiceUtil
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class CustRequestTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateNewRequest() {
        Map serviceCtx = [
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.custRequestId

        GenericValue custRequest = from('CustRequest').where('custRequestId', serviceResult.custRequestId).queryOne()
        assert custRequest
    }

    @Test
    @Order(2)
    void testUpdateCustRequest() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestName = testParams.custRequestName ?: 'Updated Test Request'
        Map serviceCtx = [
                custRequestId: custRequestId,
                custRequestName: custRequestName,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue custRequest = from('CustRequest').where('custRequestId', custRequestId).queryOne()
        assert custRequest
        assert custRequest.custRequestName == custRequestName
    }

    @Test
    @Order(3)
    void testCreateCustRequestItem() {
        String custRequestId = testParams.custRequestId ?: '9000'
        Map serviceCtx = [
                custRequestId: custRequestId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.custRequestId
    }

    @Test
    @Order(4)
    void testCreateCustRequestItemNote() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestItemSeqId = testParams.custRequestItemSeqId ?: '00001'
        String note = testParams.note ?: 'Test'
        Map serviceCtx = [
                custRequestId: custRequestId,
                custRequestItemSeqId: custRequestItemSeqId,
                note: note,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestItemNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.noteId
    }

    @Test
    @Order(5)
    void testCreateCustRequestNote() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String noteInfo = testParams.noteInfo ?: 'Test'
        Map serviceCtx = [
                custRequestId: custRequestId,
                noteInfo: noteInfo,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.noteId
        assert serviceResult.fromPartyId == 'DemoCustomer'
    }

    @Test
    @Order(6)
    void testCreateCustRequestParty() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String partyId = testParams.partyId ?: 'Company'
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [
                custRequestId: custRequestId,
                partyId: partyId,
                roleTypeId: roleTypeId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue custRequestParty = from('CustRequestParty')
                .where('custRequestId', custRequestId, 'partyId', partyId, 'roleTypeId', roleTypeId)
                .filterByDate().queryFirst()
        assert custRequestParty
    }

    @Test
    @Order(7)
    void testCreateCustRequestStatus() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestItemSeqId = testParams.custRequestItemSeqId ?: '00001'
        String statusId = testParams.statusId ?: 'CRQ_ACCEPTED'
        Map serviceCtx = [
                custRequestId: custRequestId,
                custRequestItemSeqId: custRequestItemSeqId,
                statusId: statusId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.custRequestStatusId
    }

    @Test
    @Order(8)
    void testSetCustRequestStatus() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String statusId = testParams.statusId ?: 'CRQ_ACCEPTED'
        Map serviceCtx = [
                custRequestId: custRequestId,
                statusId: statusId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('setCustRequestStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.oldStatusId
    }

    @Test
    @Order(9)
    void testGetCustRequestsByRole() {
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [
                roleTypeId: roleTypeId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('getCustRequestsByRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.custRequestAndRoles instanceof List
    }

    @Test
    @Order(10)
    void testCreateCustRequestContent() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String contentId = testParams.contentId ?: '100-ALT'
        Map serviceCtx = [
                custRequestId: custRequestId,
                contentId: contentId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue custRequestContent = from('CustRequestContent')
                .where('custRequestId', custRequestId, 'contentId', contentId)
                .filterByDate().queryFirst()
        assert custRequestContent
    }

    @Test
    @Order(11)
    void testCreateCustRequestAttribute() {
        String attrName = testParams.attrName ?: 'Test Name'
        String attrValue = testParams.attrValue ?: 'Test Value'
        String custRequestId = testParams.custRequestId ?: '9000'
        Map serviceCtx = [
                attrName: attrName,
                attrValue: attrValue,
                custRequestId: custRequestId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestAttribute', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue custRequestAttribute = from('CustRequestAttribute')
                .where('custRequestId', custRequestId, 'attrName', attrName)
                .queryOne()
        assert custRequestAttribute
        assert custRequestAttribute.attrValue == attrValue
    }

    @Test
    @Order(12)
    void testCopyCustRequestItem() {
        String custRequestId = testParams.custRequestId ?: '9000'
        String custRequestItemSeqId = testParams.custRequestItemSeqId ?: '00001'
        Map serviceCtx = [
                custRequestId: custRequestId,
                custRequestItemSeqId: custRequestItemSeqId,
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyCustRequestItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        List<GenericValue> custRequestItems = from('CustRequestItem').where('custRequestId', custRequestId).queryList()
        assert custRequestItems.size() > 1
    }

}
