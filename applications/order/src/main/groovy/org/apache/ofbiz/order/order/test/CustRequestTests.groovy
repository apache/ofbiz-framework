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
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil

@SuppressWarnings(['LineLength', 'UnnecessaryObjectReferences', 'UnnecessaryGString', 'PublicMethodsBeforeNonPublicMethods', 'ClassSize', 'MethodCount', 'ConsecutiveBlankLines', 'BlockEndsWithBlankLine', 'ClassEndsWithBlankLine'])
class CustRequestTests extends OFBizTestCase {

    CustRequestTests(String name) {
        super(name)
    }

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

    void testUpdateCustRequest() {
        Map serviceCtx = [
                custRequestId: '9000',
                custRequestName: 'Updated Test Request',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('updateCustRequest', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue custRequest = from('CustRequest').where('custRequestId', '9000').queryOne()
        assert custRequest
        assert custRequest.custRequestName == 'Updated Test Request'
    }

    void testCreateCustRequestItem() {
        Map serviceCtx = [
                custRequestId: '9000',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.custRequestId
    }

    void testCreateCustRequestItemNote() {
        Map serviceCtx = [
                custRequestId: '9000',
                custRequestItemSeqId: '00001',
                note: 'Test',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestItemNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.noteId
    }

    void testCreateCustRequestNote() {
        Map serviceCtx = [
                custRequestId: '9000',
                noteInfo: 'Test',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestNote', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
        assert serviceResult.fromPartyId == 'DemoCustomer'
    }

    void testCreateCustRequestParty() {
        Map serviceCtx = [
                custRequestId: '9000',
                partyId: 'Company',
                roleTypeId: 'OWNER',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestParty', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
    }

    void testCreateCustRequestStatus() {
        Map serviceCtx = [
                custRequestId: '9000',
                custRequestItemSeqId: '00001',
                statusId: 'CRQ_ACCEPTED',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.custRequestStatusId
    }

    void testSetCustRequestStatus() {
        Map serviceCtx = [
                custRequestId: '9000',
                statusId: 'CRQ_ACCEPTED',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('setCustRequestStatus', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.oldStatusId
    }

    void testGetCustRequestsByRole() {
        Map serviceCtx = [
                roleTypeId: 'OWNER',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('getCustRequestsByRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
    }

    void testCreateCustRequestContent() {
        Map serviceCtx = [
                custRequestId: '9000',
                contentId: '100-ALT',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
    }

    void testCreateCustRequestAttribute() {
        Map serviceCtx = [
                attrName: 'Test Name',
                attrValue: 'Test Value',
                custRequestId: '9000',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('createCustRequestAttribute', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
    }

    void testCopyCustRequestItem() {
        Map serviceCtx = [
                custRequestId: '9000',
                custRequestItemSeqId: '00001',
                userLogin: userLogin,
        ]
        Map serviceResult = dispatcher.runSync('copyCustRequestItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult
    }
}
