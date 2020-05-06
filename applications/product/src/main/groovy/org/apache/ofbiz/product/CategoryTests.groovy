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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class CategoryTests extends OFBizTestCase {
    public CategoryTests(String name) {
        super(name)
    }

    void testAddProductCategoryToCategory() {
        Map serviceCtx = [
                productCategoryId: 'TPC',
                parentProductCategoryId: 'TPCP',
                fromDate: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('addProductCategoryToCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue prodCategory = from('ProductCategoryRollup').where('productCategoryId', 'TPC', 'parentProductCategoryId', 'TPCP').queryFirst()
        assert prodCategory != null
    }

    void testGetProductCategoryAndLimitedMembers() {
        Map serviceCtx = [
                productCategoryId: '101',
                prodCatalogId: 'DemoCatalog',
                defaultViewSize: 10,
                limitView: true,
                userLogin: userLogin
        ]
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getProductCategoryAndLimitedMembers', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.productCategoryMembers != null
        assert serviceResult.productCategory != null
        assert serviceResult.productCategory.productCategoryId == '101'

        List<GenericValue> productCategoryMemberList = from('ProductCategoryMember').where('productCategoryId', '101').queryList()
        assert productCategoryMemberList.containsAll(serviceResult.productCategoryMembers)
    }

}

