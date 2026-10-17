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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class CategoryTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testAddProductCategoryToCategory() {
        String productCategoryId = testParams.productCategoryId ?: 'TPC'
        String parentProductCategoryId = testParams.parentProductCategoryId ?: 'TPCP'
        Map serviceCtx = [
                productCategoryId: productCategoryId,
                parentProductCategoryId: parentProductCategoryId,
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('addProductCategoryToCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue prodCategory = from('ProductCategoryRollup')
                .where('productCategoryId', productCategoryId, 'parentProductCategoryId', parentProductCategoryId)
                .queryFirst()
        assert prodCategory != null
    }

    @Test
    @Order(2)
    void testGetProductCategoryAndLimitedMembers() {
        String productCategoryId = testParams.productCategoryId ?: '101'
        String prodCatalogId = testParams.prodCatalogId ?: 'DemoCatalog'
        Map serviceCtx = [
                productCategoryId: productCategoryId,
                prodCatalogId: prodCatalogId,
                defaultViewSize: 10,
                limitView: true,
                userLogin: userLogin
        ]
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getProductCategoryAndLimitedMembers', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.productCategoryMembers != null
        assert serviceResult.productCategory != null
        assert serviceResult.productCategory.productCategoryId == productCategoryId

        List<GenericValue> productCategoryMemberList = from('ProductCategoryMember').where('productCategoryId', productCategoryId).queryList()
        assert productCategoryMemberList.containsAll(serviceResult.productCategoryMembers)
    }

    @Test
    @Order(3)
    void testUpdateProductCategory() {
        String productCategoryId = testParams.productCategoryId ?: 'TPC'
        String newDescription = 'Updated Long Test Product Category Description'
        Map serviceCtx = [
                productCategoryId: productCategoryId,
                productCategoryTypeId: 'TEST_CATEGORY',
                longDescription: newDescription,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateProductCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue productCategory = from('ProductCategory').where('productCategoryId', productCategoryId).queryOne()
        assert productCategory.longDescription == newDescription
    }

    @Test
    @Order(4)
    void testUpdateProductCategoryNotFoundIsSilentNoOp() {
        Map serviceCtx = [
                productCategoryId: 'DoesNotExist12345',
                productCategoryTypeId: 'TEST_CATEGORY',
                longDescription: 'should never be applied',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateProductCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue productCategory = from('ProductCategory').where('productCategoryId', 'DoesNotExist12345').queryOne()
        assert productCategory == null
    }

    @Test
    @Order(5)
    void testUpdateContentSEOForCategoryUpdatesExistingTitle() {
        String productCategoryId = testParams.productCategoryId ?: 'TPCP'

        Map firstCallCtx = [productCategoryId: productCategoryId, title: 'First SEO Title', userLogin: userLogin]
        Map firstCallResult = dispatcher.runSync('updateContentSEOForCategory', firstCallCtx)
        assert ServiceUtil.isSuccess(firstCallResult)

        Map secondCallCtx = [productCategoryId: productCategoryId, title: 'Second SEO Title', userLogin: userLogin]
        Map secondCallResult = dispatcher.runSync('updateContentSEOForCategory', secondCallCtx)
        assert ServiceUtil.isSuccess(secondCallResult)

        GenericValue productCategoryContent = from('ProductCategoryContentAndInfo')
                .where('productCategoryId', productCategoryId, 'prodCatContentTypeId', 'PAGE_TITLE')
                .queryFirst()
        GenericValue electronicText = from('ElectronicText')
                .where('dataResourceId', productCategoryContent.dataResourceId)
                .queryOne()
        assert electronicText.textData == 'Second SEO Title'
    }

}

