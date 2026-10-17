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
package org.apache.ofbiz.product.product.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

@JunitJupiterTest
class ProductTests implements JupiterTestHelper {

    @Test
    void testUpdateProductCategory() {
        String categoryName = testParams.categoryName ?: 'Updated Test Product Category'
        String longDescription = testParams.longDescription ?: 'Updated Long Test Product Category Description'
        String productCategoryId = testParams.productCategoryId ?: 'CATALOG1_BEST_SELL'
        String productCategoryTypeId = testParams.productCategoryTypeId ?: 'BEST_SELL_CATEGORY'
        Map serviceCtx = [
                categoryName: categoryName,
                longDescription: longDescription,
                productCategoryId: productCategoryId,
                productCategoryTypeId: productCategoryTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateProductCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue prodCategory = from('ProductCategory').where('productCategoryId', productCategoryId).queryOne()
        if (prodCategory) { // fails in framework integration tests only, data is in ecommerce
            assert prodCategory.categoryName == categoryName
            assert prodCategory.productCategoryTypeId == productCategoryTypeId
        }
    }

}
