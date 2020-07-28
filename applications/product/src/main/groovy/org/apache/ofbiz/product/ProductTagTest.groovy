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
package org.apache.ofbiz.product

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.apache.ofbiz.product.product.ProductEvents

class ProductTagTest extends OFBizTestCase {
    public ProductTagTest(String name) {
        super(name)
    }

    void testProductTag() {
        /*Test Product Tag
        Step 1) Create a product tag.
        Step 2) Check a product tag is created.
        Step 3) Approve a product tag.
        Step 4) Check a product tag is approved.
        Step 5) Create multiple product tag.
        Step 6) Approve all product tags.
        Step 7) Check all product tags is approved.
        Step 8) Check all product tags is created.*/


        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()
        GenericValue systemUserLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', dispatcher)
        // Step 1
        request.setParameter('productId', 'GZ-1000');
        request.setParameter('productTags', 'test');
        ProductEvents.addProductTags(request, response)

        // Step 2
        GenericValue productKeyword = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'test', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert productKeyword != null
        assert productKeyword.statusId == 'KW_PENDING'
        // Step 3
        Map updateProductKeywordMap = [
                userLogin : systemUserLogin,
                productId : 'GZ-1000',
                keyword : 'test',
                keywordTypeId : 'KWT_TAG',
                statusId : 'KW_APPROVED',
        ]
        Map resultMap = dispatcher.runSync('updateProductKeyword', updateProductKeywordMap)
        assert ServiceUtil.isSuccess(resultMap)

        // Step 4
        GenericValue checkProductKeywordApprove = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'test', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeywordApprove != null
        assert checkProductKeywordApprove.statusId == 'KW_APPROVED'

        // Step 5
        request.setParameter('productId', 'GZ-1000');
        request.setParameter('productTags', '\'rock and roll\' t-shirt red');
        ProductEvents.addProductTags(request, response)

        // Step 6
        GenericValue checkProductKeyword1 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'rock and roll', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeyword1 != null
        assert checkProductKeyword1.statusId == 'KW_PENDING'

        GenericValue checkProductKeyword2 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 't-shirt', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeyword2 != null
        assert checkProductKeyword2.statusId == 'KW_PENDING'

        GenericValue checkProductKeyword3 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'red', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeyword3 != null
        assert checkProductKeyword3.statusId == 'KW_PENDING'

        // Step 7
        List<GenericValue> checkAllProductKeywordApproveList = from('ProductKeyword').where('productId', 'GZ-1000', 'statusId', 'KW_PENDING', 'keywordTypeId', 'KWT_TAG').queryList()
        for (GenericValue checkAllProductKeywordApprove : checkAllProductKeywordApproveList) {
            updateProductKeywordMap = [
                    userLogin : systemUserLogin,
                    productId : checkAllProductKeywordApprove.productId,
                    keyword : checkAllProductKeywordApprove.keyword,
                    keywordTypeId : checkAllProductKeywordApprove.keywordTypeId,
                    statusId : 'KW_APPROVED',
            ]
            resultMap = dispatcher.runSync('updateProductKeyword', updateProductKeywordMap)
            assert ServiceUtil.isSuccess(resultMap)
        }

        // Step 8
        GenericValue checkProductKeywordApprove1 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'rock and roll', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeywordApprove1 != null
        assert checkProductKeywordApprove1.statusId == 'KW_APPROVED'

        GenericValue checkProductKeywordApprove2 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 't-shirt', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeywordApprove2 != null
        assert checkProductKeywordApprove2.statusId == 'KW_APPROVED'

        GenericValue checkProductKeywordApprove3 = from('ProductKeyword').where('productId', 'GZ-1000', 'keyword', 'red', 'keywordTypeId', 'KWT_TAG').queryOne()
        assert checkProductKeywordApprove3 != null
        assert checkProductKeywordApprove3.statusId == 'KW_APPROVED'
    }
}