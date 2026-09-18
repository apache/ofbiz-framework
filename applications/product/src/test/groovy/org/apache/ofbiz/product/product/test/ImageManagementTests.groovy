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

}
