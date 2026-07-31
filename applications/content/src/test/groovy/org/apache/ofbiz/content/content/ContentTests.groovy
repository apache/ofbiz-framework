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
package org.apache.ofbiz.content.content

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class ContentTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testGetDataResource() {
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = 'TEST_RESOURCE'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.resultData.dataResource.dataResourceId == 'TEST_RESOURCE'
        assert serviceResult.resultData.dataResource.dataResourceTypeId == 'TEST_RESOURCE_TYPE'
    }

    @Test
    @Order(2)
    void testCreateDataCategory() {
        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = 'TEST_DATA_CATEGORY_1'
        serviceCtx.categoryName = 'Test Data Category 1'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('dataCategoryId', 'TEST_DATA_CATEGORY_1')
                .queryOne()
        assert dataCategory
        assert dataCategory.categoryName == 'Test Data Category 1'
    }

    @Test
    @Order(3)
    void testUpdateDataCategory() {
        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = 'TEST_DATA_CATEGORY_2'
        serviceCtx.categoryName = 'Test Data Category 2'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceCtx.dataCategoryId = 'TEST_DATA_CATEGORY_2'
        serviceCtx.categoryName = 'Test Data Category 20'
        serviceCtx.userLogin = userLogin
        serviceResult = dispatcher.runSync('updateDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('categoryName', 'Test Data Category 2')
                .queryFirst()
        assert !dataCategory

        dataCategory = from('DataCategory')
                .where('categoryName', 'Test Data Category 20')
                .queryFirst()
        assert dataCategory
    }

    @Test
    @Order(4)
    void testDeleteDataCategory() {
        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = 'TEST_DATA_CATEGORY_3'
        serviceCtx.categoryName = 'Test Data Category 3'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceCtx.dataCategoryId = 'TEST_DATA_CATEGORY_3'
        serviceCtx.userLogin = userLogin
        serviceResult = dispatcher.runSync('removeDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('dataCategoryId', 'TEST_DATA_CATEGORY_3')
                .queryOne()
        assert !dataCategory
    }

    @Test
    @Order(5)
    void testCreateDataResourceRole() {
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = 'TEST_DATA_RESOURCE_1'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = 'admin'
        serviceCtx.roleTypeId = 'OWNER'
        serviceCtx.fromDate = UtilDateTime.toTimestamp('11/03/2016 00:00:00')
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', 'TEST_DATA_RESOURCE_1', 'partyId', 'admin', 'roleTypeId', 'OWNER',
                'fromDate', UtilDateTime.toTimestamp('11/03/2016 00:00:00'))
                .queryOne()
        assert dataResourceRole
    }

    @Test
    @Order(6)
    void testUpdateDataResourceRole() {
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = 'TEST_DATA_RESOURCE_2'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = 'admin'
        serviceCtx.roleTypeId = 'OWNER'
        serviceCtx.fromDate = UtilDateTime.toTimestamp('11/03/2016 00:00:00')
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', 'TEST_DATA_RESOURCE_2', 'partyId', 'admin')
                .queryOne()
        assert dataResourceRole
        assert !dataResourceRole.thruDate

        serviceCtx.thruDate = UtilDateTime.nowTimestamp()
        serviceResult = dispatcher.runSync('updateDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', 'TEST_DATA_RESOURCE_2', 'partyId', 'admin')
                .queryOne()
        assert dataResourceRole
        assert dataResourceRole.thruDate
    }

    @Test
    @Order(7)
    void testRemoveDataResourceRole() {
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = 'TEST_DATA_RESOURCE_3'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = 'admin'
        serviceCtx.roleTypeId = 'OWNER'
        serviceCtx.fromDate = UtilDateTime.nowTimestamp()
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', 'TEST_DATA_RESOURCE_3', 'partyId', 'admin')
                .queryFirst()
        assert dataResourceRole

        serviceResult = dispatcher.runSync('removeDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', 'TEST_DATA_RESOURCE_3', 'partyId', 'admin')
                .queryFirst()
        assert !dataResourceRole
    }

    @Test
    @Order(8)
    void testGetContent() {
        Map serviceCtx = [:]
        serviceCtx.contentId = 'TEST_CONTENT4'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.view
    }

}
