/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.product.product.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.sql.Timestamp

@JunitJupiterTest
class ImageManagementTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testAddRejectedReasonImageManagementSetsThruDate() {
        Map createProductCtx = [productTypeId: 'FINISHED_GOOD', internalName: 'Test Image Product', userLogin: userLogin]
        Map createProductResult = dispatcher.runSync('createProduct', createProductCtx)
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        // createProductContent's own contentId is a required (non-auto-generating) INOUT pk
        // attribute, so create the Content record explicitly first via the generic, genuinely
        // auto-generating createContent service, then link it.
        Map createContentCtx = [contentName: 'Test Rejected Reason Image', userLogin: userLogin]
        Map createContentResult = dispatcher.runSync('createContent', createContentCtx)
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map createProductContentCtx = [productId: productId, contentId: contentId,
                                        productContentTypeId: 'IMAGE', userLogin: userLogin]
        Map createProductContentResult = dispatcher.runSync('createProductContent', createProductContentCtx)
        assert ServiceUtil.isSuccess(createProductContentResult)

        GenericValue productContentBefore = from('ProductContent')
                .where('productId', productId, 'productContentTypeId', 'IMAGE', 'contentId', contentId)
                .queryOne()
        assert productContentBefore.thruDate == null

        Map addRejectedReasonCtx = [contentId: contentId, description: 'RETAKE_PHOTO', userLogin: userLogin]
        Map addRejectedReasonResult = dispatcher.runSync('addRejectedReasonImageManagement', addRejectedReasonCtx)
        assert ServiceUtil.isSuccess(addRejectedReasonResult)

        GenericValue productContentAfter = from('ProductContent')
                .where('productId', productId, 'productContentTypeId', 'IMAGE', 'contentId', contentId)
                .queryOne()
        assert productContentAfter.thruDate != null
    }

    @Test
    @Order(2)
    void testUpdateStatusImageManagementSingleApproverPath() {
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: 'system'], false)

        String internalName = testParams.internalName ?: 'Test Single Approver Product'
        String contentName = testParams.contentName ?: 'Test Single Approver Image'
        String statusId = testParams.statusId ?: 'IM_PENDING'
        // checkStatusId is deliberately NOT testParams-driven - it selects which branch of
        // updateStatusImageManagement() this test exercises (IM_APPROVED), and the assertions
        // below (purchaseFromDate getting set) only hold for that branch. See
        // testUpdateStatusImageManagementRejectedPath() for the IM_REJECTED branch's own test.
        String checkStatusId = 'IM_APPROVED'

        Map<String, Object> createProductResult = dispatcher.runSync('createProduct',
                [productTypeId: 'FINISHED_GOOD', internalName: internalName, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map<String, Object> createContentResult = dispatcher.runSync('createContent',
                [contentName: contentName, statusId: statusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map<String, Object> createProductContentResult = dispatcher.runSync('createProductContent',
                [productId: productId, contentId: contentId, productContentTypeId: 'IMAGE', userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductContentResult)

        Map<String, Object> createContentApprovalResult = dispatcher.runSync('createContentApproval',
                [contentId: contentId, partyId: userLogin.partyId, roleTypeId: 'IMAGEAPPROVER',
                 approvalStatusId: statusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentApprovalResult)

        GenericValue contentBefore = from('Content').where('contentId', contentId).queryOne()
        Assertions.assertEquals(statusId, contentBefore.statusId)

        Map<String, Object> updateStatusResult = dispatcher.runSync('updateStatusImageManagement',
                [productId: productId, contentId: contentId, checkStatusId: checkStatusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(updateStatusResult)

        GenericValue contentAfter = from('Content').where('contentId', contentId).queryOne()
        Assertions.assertEquals(checkStatusId, contentAfter.statusId)

        GenericValue productContentAfter = from('ProductContent')
                .where('productId', productId, 'contentId', contentId, 'productContentTypeId', 'IMAGE')
                .queryOne()
        assert productContentAfter.purchaseFromDate != null
    }

    @Test
    @Order(3)
    void testSetImageDetailUpdatesDataResourceIsPublic() {
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: 'system'], false)

        String internalName = testParams.internalName ?: 'Test Set Image Detail Product'
        String contentName = testParams.contentName ?: 'Test Set Image Detail Content'
        String dataResourceName = testParams.dataResourceName ?: 'Test Set Image Detail DataResource'
        String initialIsPublic = testParams.initialIsPublic ?: 'N'
        String drIsPublic = testParams.drIsPublic ?: 'Y'
        String description = testParams.description ?: 'Test image description'
        // statusId is deliberately NOT testParams-driven - setImageDetail()'s DataResource update
        // only runs when content.statusId == 'IM_APPROVED'; that's the one condition this test
        // exists to exercise, not a value a caller should vary away.
        String statusId = 'IM_APPROVED'

        Map<String, Object> createDataResourceResult = dispatcher.runSync('createDataResource',
                [dataResourceName: dataResourceName, isPublic: initialIsPublic, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createDataResourceResult)
        String dataResourceId = createDataResourceResult.dataResourceId

        Map<String, Object> createProductResult = dispatcher.runSync('createProduct',
                [productTypeId: 'FINISHED_GOOD', internalName: internalName, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map<String, Object> createContentResult = dispatcher.runSync('createContent',
                [contentName: contentName, statusId: statusId, dataResourceId: dataResourceId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map<String, Object> createProductContentResult = dispatcher.runSync('createProductContent',
                [productId: productId, contentId: contentId, productContentTypeId: 'IMAGE', userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductContentResult)

        // fromDate is part of ProductContent's PK and a required IN attribute of setImageDetail()
        // (auto-attributes include="pk" mode="IN" optional="false") - must be the actual stored value.
        GenericValue productContent = from('ProductContent')
                .where('productId', productId, 'contentId', contentId, 'productContentTypeId', 'IMAGE')
                .queryOne()
        Timestamp fromDate = productContent.fromDate

        GenericValue dataResourceBefore = from('DataResource').where('dataResourceId', dataResourceId).queryOne()
        Assertions.assertEquals(initialIsPublic, dataResourceBefore.isPublic)

        Map<String, Object> setImageDetailResult = dispatcher.runSync('setImageDetail',
                [productId: productId, contentId: contentId, productContentTypeId: 'IMAGE',
                 fromDate: fromDate, description: description, drIsPublic: drIsPublic, userLogin: userLogin])
        assert ServiceUtil.isSuccess(setImageDetailResult)

        GenericValue dataResourceAfter = from('DataResource').where('dataResourceId', dataResourceId).queryOne()
        Assertions.assertEquals(drIsPublic, dataResourceAfter.isPublic)
    }

    @Test
    @Order(4)
    void testUpdateStatusImageManagementRejectedPath() {
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: 'system'], false)

        String internalName = testParams.internalName ?: 'Test Rejected Image Product'
        String contentName = testParams.contentName ?: 'Test Rejected Image'
        String statusId = testParams.statusId ?: 'IM_PENDING'
        // checkStatusId is deliberately NOT testParams-driven - see
        // testUpdateStatusImageManagementSingleApproverPath()'s note on the same pattern.
        String checkStatusId = 'IM_REJECTED'

        Map<String, Object> createProductResult = dispatcher.runSync('createProduct',
                [productTypeId: 'FINISHED_GOOD', internalName: internalName, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map<String, Object> createContentResult = dispatcher.runSync('createContent',
                [contentName: contentName, statusId: statusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map<String, Object> createProductContentResult = dispatcher.runSync('createProductContent',
                [productId: productId, contentId: contentId, productContentTypeId: 'IMAGE', userLogin: userLogin])
        assert ServiceUtil.isSuccess(createProductContentResult)

        Map<String, Object> createContentApprovalResult = dispatcher.runSync('createContentApproval',
                [contentId: contentId, partyId: userLogin.partyId, roleTypeId: 'IMAGEAPPROVER',
                 approvalStatusId: statusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(createContentApprovalResult)

        GenericValue contentBefore = from('Content').where('contentId', contentId).queryOne()
        Assertions.assertEquals(statusId, contentBefore.statusId)

        Map<String, Object> updateStatusResult = dispatcher.runSync('updateStatusImageManagement',
                [productId: productId, contentId: contentId, checkStatusId: checkStatusId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(updateStatusResult)

        GenericValue contentAfter = from('Content').where('contentId', contentId).queryOne()
        Assertions.assertEquals(checkStatusId, contentAfter.statusId)
        Assertions.assertEquals(userLogin.userLoginId, contentAfter.createdByUserLogin)

        GenericValue checkRejectAfter = from('ContentApproval')
                .where('contentId', contentId, 'partyId', userLogin.partyId, 'roleTypeId', 'IMAGEAPPROVER')
                .queryOne()
        Assertions.assertEquals(checkStatusId, checkRejectAfter.approvalStatusId)
    }

}
