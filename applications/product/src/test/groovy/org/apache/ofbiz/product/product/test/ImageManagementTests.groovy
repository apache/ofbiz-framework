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
        Map createProductCtx = [productTypeId: 'FINISHED_GOOD', internalName: 'Test Single Approver Product', userLogin: userLogin]
        Map createProductResult = dispatcher.runSync('createProduct', createProductCtx)
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map createContentCtx = [contentName: 'Test Single Approver Image', statusId: 'IM_PENDING', userLogin: userLogin]
        Map createContentResult = dispatcher.runSync('createContent', createContentCtx)
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map createProductContentCtx = [productId: productId, contentId: contentId,
                                        productContentTypeId: 'IMAGE', userLogin: userLogin]
        Map createProductContentResult = dispatcher.runSync('createProductContent', createProductContentCtx)
        assert ServiceUtil.isSuccess(createProductContentResult)

        Map createContentApprovalCtx = [contentId: contentId, partyId: userLogin.partyId, roleTypeId: 'IMAGEAPPROVER',
                                         approvalStatusId: 'IM_PENDING', userLogin: userLogin]
        Map createContentApprovalResult = dispatcher.runSync('createContentApproval', createContentApprovalCtx)
        assert ServiceUtil.isSuccess(createContentApprovalResult)

        GenericValue contentBefore = from('Content')
                .where('contentId', contentId)
                .queryOne()
        assert contentBefore.statusId == 'IM_PENDING'

        Map updateStatusCtx = [productId: productId, contentId: contentId, checkStatusId: 'IM_APPROVED', userLogin: userLogin]
        Map updateStatusResult = dispatcher.runSync('updateStatusImageManagement', updateStatusCtx)
        assert ServiceUtil.isSuccess(updateStatusResult)

        GenericValue contentAfter = from('Content')
                .where('contentId', contentId)
                .queryOne()
        assert contentAfter.statusId == 'IM_APPROVED'

        GenericValue productContentAfter = from('ProductContent')
                .where('productId', productId, 'contentId', contentId, 'productContentTypeId', 'IMAGE')
                .queryOne()
        assert productContentAfter.purchaseFromDate != null
    }

    @Test
    @Order(3)
    void testSetImageDetailUpdatesDataResourceIsPublic() {
        Map createDataResourceCtx = [dataResourceName: 'Test Set Image Detail DataResource', isPublic: 'N', userLogin: userLogin]
        Map createDataResourceResult = dispatcher.runSync('createDataResource', createDataResourceCtx)
        assert ServiceUtil.isSuccess(createDataResourceResult)
        String dataResourceId = createDataResourceResult.dataResourceId

        Map createProductCtx = [productTypeId: 'FINISHED_GOOD', internalName: 'Test Set Image Detail Product', userLogin: userLogin]
        Map createProductResult = dispatcher.runSync('createProduct', createProductCtx)
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        // statusId IM_APPROVED and dataResourceId set up front so setImageDetail()'s
        // if (content.statusId == 'IM_APPROVED') branch runs and finds a DataResource to update.
        Map createContentCtx = [contentName: 'Test Set Image Detail Content', statusId: 'IM_APPROVED',
                                 dataResourceId: dataResourceId, userLogin: userLogin]
        Map createContentResult = dispatcher.runSync('createContent', createContentCtx)
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map createProductContentCtx = [productId: productId, contentId: contentId,
                                        productContentTypeId: 'IMAGE', userLogin: userLogin]
        Map createProductContentResult = dispatcher.runSync('createProductContent', createProductContentCtx)
        assert ServiceUtil.isSuccess(createProductContentResult)

        // fromDate is part of ProductContent's PK and a required IN attribute of setImageDetail()
        // (auto-attributes include="pk" mode="IN" optional="false") - must be the actual stored value.
        GenericValue productContent = from('ProductContent')
                .where('productId', productId, 'contentId', contentId, 'productContentTypeId', 'IMAGE')
                .queryOne()
        Timestamp fromDate = productContent.fromDate

        GenericValue dataResourceBefore = from('DataResource').where('dataResourceId', dataResourceId).queryOne()
        assert dataResourceBefore.isPublic == 'N'

        Map setImageDetailCtx = [productId: productId, contentId: contentId, productContentTypeId: 'IMAGE',
                                  fromDate: fromDate, description: 'Test image description',
                                  drIsPublic: 'Y', userLogin: userLogin]
        Map setImageDetailResult = dispatcher.runSync('setImageDetail', setImageDetailCtx)
        assert ServiceUtil.isSuccess(setImageDetailResult)

        GenericValue dataResourceAfter = from('DataResource').where('dataResourceId', dataResourceId).queryOne()
        assert dataResourceAfter.isPublic == 'Y'
    }

    @Test
    @Order(4)
    void testUpdateStatusImageManagementRejectedPath() {
        Map createProductCtx = [productTypeId: 'FINISHED_GOOD', internalName: 'Test Rejected Image Product', userLogin: userLogin]
        Map createProductResult = dispatcher.runSync('createProduct', createProductCtx)
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map createContentCtx = [contentName: 'Test Rejected Image', statusId: 'IM_PENDING', userLogin: userLogin]
        Map createContentResult = dispatcher.runSync('createContent', createContentCtx)
        assert ServiceUtil.isSuccess(createContentResult)
        String contentId = createContentResult.contentId

        Map createProductContentCtx = [productId: productId, contentId: contentId,
                                        productContentTypeId: 'IMAGE', userLogin: userLogin]
        Map createProductContentResult = dispatcher.runSync('createProductContent', createProductContentCtx)
        assert ServiceUtil.isSuccess(createProductContentResult)

        Map createContentApprovalCtx = [contentId: contentId, partyId: userLogin.partyId, roleTypeId: 'IMAGEAPPROVER',
                                         approvalStatusId: 'IM_PENDING', userLogin: userLogin]
        Map createContentApprovalResult = dispatcher.runSync('createContentApproval', createContentApprovalCtx)
        assert ServiceUtil.isSuccess(createContentApprovalResult)

        GenericValue contentBefore = from('Content')
                .where('contentId', contentId)
                .queryOne()
        assert contentBefore.statusId == 'IM_PENDING'

        Map updateStatusCtx = [productId: productId, contentId: contentId, checkStatusId: 'IM_REJECTED', userLogin: userLogin]
        Map updateStatusResult = dispatcher.runSync('updateStatusImageManagement', updateStatusCtx)
        assert ServiceUtil.isSuccess(updateStatusResult)

        GenericValue contentAfter = from('Content')
                .where('contentId', contentId)
                .queryOne()
        assert contentAfter.statusId == 'IM_REJECTED'
        assert contentAfter.createdByUserLogin == userLogin.userLoginId

        GenericValue checkRejectAfter = from('ContentApproval')
                .where('contentId', contentId, 'partyId', userLogin.partyId, 'roleTypeId', 'IMAGEAPPROVER')
                .queryOne()
        assert checkRejectAfter.approvalStatusId == 'IM_REJECTED'
    }

}
