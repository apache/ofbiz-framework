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
class ProductContentTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testUpdateContentSEOForProductUpdatesExistingText() {
        Map createProductCtx = [productTypeId: 'FINISHED_GOOD', internalName: 'Test SEO Product', userLogin: userLogin]
        Map createProductResult = dispatcher.runSync('createProduct', createProductCtx)
        assert ServiceUtil.isSuccess(createProductResult)
        String productId = createProductResult.productId

        Map firstCallCtx = [productId: productId, title: 'First Title', metaKeyword: 'First Keyword',
                             metaDescription: 'First Description', userLogin: userLogin]
        Map firstCallResult = dispatcher.runSync('updateContentSEOForProduct', firstCallCtx)
        assert ServiceUtil.isSuccess(firstCallResult)

        Map secondCallCtx = [productId: productId, title: 'Second Title', metaKeyword: 'Second Keyword',
                              metaDescription: 'Second Description', userLogin: userLogin]
        Map secondCallResult = dispatcher.runSync('updateContentSEOForProduct', secondCallCtx)
        assert ServiceUtil.isSuccess(secondCallResult)

        assert textDataFor(productId, 'PAGE_TITLE') == 'Second Title'
        assert textDataFor(productId, 'META_KEYWORD') == 'Second Keyword'
        assert textDataFor(productId, 'META_DESCRIPTION') == 'Second Description'
    }

    private String textDataFor(String productId, String productContentTypeId) {
        GenericValue productContent = from('ProductContentAndInfo')
                .where('productId', productId, 'productContentTypeId', productContentTypeId)
                .queryFirst()
        GenericValue electronicText = from('ElectronicText')
                .where('dataResourceId', productContent.dataResourceId)
                .queryOne()
        return electronicText.textData
    }

}
