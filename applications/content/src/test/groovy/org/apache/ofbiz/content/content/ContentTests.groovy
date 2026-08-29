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

import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.ofbiz.base.util.GeneralException
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
        String dataResourceId = testParams.dataResourceId ?: 'TEST_RESOURCE'
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = dataResourceId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.resultData.dataResource.dataResourceId == dataResourceId
        assert serviceResult.resultData.dataResource.dataResourceTypeId == 'TEST_RESOURCE_TYPE'
    }

    @Test
    @Order(2)
    void testCreateDataCategory() {
        String dataCategoryId = testParams.dataCategoryId ?: 'TEST_DATA_CATEGORY_1'
        String categoryName = testParams.categoryName ?: 'Test Data Category 1'
        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = dataCategoryId
        serviceCtx.categoryName = categoryName
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('dataCategoryId', dataCategoryId)
                .queryOne()
        assert dataCategory
        assert dataCategory.categoryName == categoryName
    }

    @Test
    @Order(3)
    void testUpdateDataCategory() {
        String dataCategoryId = testParams.dataCategoryId ?: 'TEST_DATA_CATEGORY_2'
        String categoryName = testParams.categoryName ?: 'Test Data Category 2'
        String updatedCategoryName = testParams.updatedCategoryName ?: 'Test Data Category 20'

        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = dataCategoryId
        serviceCtx.categoryName = categoryName
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceCtx.dataCategoryId = dataCategoryId
        serviceCtx.categoryName = updatedCategoryName
        serviceCtx.userLogin = userLogin
        serviceResult = dispatcher.runSync('updateDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('categoryName', categoryName)
                .queryFirst()
        assert !dataCategory

        dataCategory = from('DataCategory')
                .where('categoryName', updatedCategoryName)
                .queryFirst()
        assert dataCategory
    }

    @Test
    @Order(4)
    void testDeleteDataCategory() {
        String dataCategoryId = testParams.dataCategoryId ?: 'TEST_DATA_CATEGORY_3'
        String categoryName = testParams.categoryName ?: 'Test Data Category 3'
        Map serviceCtx = [:]
        serviceCtx.dataCategoryId = dataCategoryId
        serviceCtx.categoryName = categoryName
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceCtx.dataCategoryId = dataCategoryId
        serviceCtx.userLogin = userLogin
        serviceResult = dispatcher.runSync('removeDataCategory', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataCategory = from('DataCategory')
                .where('dataCategoryId', dataCategoryId)
                .queryOne()
        assert !dataCategory
    }

    @Test
    @Order(5)
    void testCreateDataResourceRole() {
        String dataResourceId = testParams.dataResourceId ?: 'TEST_DATA_RESOURCE_1'
        String partyId = testParams.partyId ?: 'admin'
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = dataResourceId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = partyId
        serviceCtx.roleTypeId = roleTypeId
        serviceCtx.fromDate = UtilDateTime.toTimestamp('11/03/2016 00:00:00')
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', dataResourceId, 'partyId', partyId, 'roleTypeId', roleTypeId,
                'fromDate', UtilDateTime.toTimestamp('11/03/2016 00:00:00'))
                .queryOne()
        assert dataResourceRole
    }

    @Test
    @Order(6)
    void testUpdateDataResourceRole() {
        String dataResourceId = testParams.dataResourceId ?: 'TEST_DATA_RESOURCE_2'
        String partyId = testParams.partyId ?: 'admin'
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = dataResourceId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = partyId
        serviceCtx.roleTypeId = roleTypeId
        serviceCtx.fromDate = UtilDateTime.toTimestamp('11/03/2016 00:00:00')
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', dataResourceId, 'partyId', partyId)
                .queryOne()
        assert dataResourceRole
        assert !dataResourceRole.thruDate

        serviceCtx.thruDate = UtilDateTime.nowTimestamp()
        serviceResult = dispatcher.runSync('updateDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', dataResourceId, 'partyId', partyId)
                .queryOne()
        assert dataResourceRole
        assert dataResourceRole.thruDate
    }

    @Test
    @Order(7)
    void testRemoveDataResourceRole() {
        String dataResourceId = testParams.dataResourceId ?: 'TEST_DATA_RESOURCE_3'
        String partyId = testParams.partyId ?: 'admin'
        String roleTypeId = testParams.roleTypeId ?: 'OWNER'
        Map serviceCtx = [:]
        serviceCtx.dataResourceId = dataResourceId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createDataResource', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.partyId = partyId
        serviceCtx.roleTypeId = roleTypeId
        serviceCtx.fromDate = UtilDateTime.nowTimestamp()
        serviceResult = dispatcher.runSync('createDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', dataResourceId, 'partyId', partyId)
                .queryFirst()
        assert dataResourceRole

        serviceResult = dispatcher.runSync('removeDataResourceRole', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        dataResourceRole = from('DataResourceRole')
                .where('dataResourceId', dataResourceId, 'partyId', partyId)
                .queryFirst()
        assert !dataResourceRole
    }

    @Test
    @Order(8)
    void testGetContent() {
        String contentId = testParams.contentId ?: 'TEST_CONTENT4'
        Map serviceCtx = [:]
        serviceCtx.contentId = contentId
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('getContent', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.view
    }

    // Regression coverage for the fix to renderContentAsText's service dispatch: Content.serviceName is a
    // legacy, client-writable field (createContent/updateContent both let a plain content editor set it) that
    // used to be looked up and run directly with the live HTTP request parameters as its input. Only a
    // CustomMethod explicitly typed CONTENT_RENDER may be run there now.

    // These three build their own Content/CustomMethod fixture rows inline (rather than relying on the
    // testdef data-load test-case) since that data-load and this Jupiter suite are proven, in this class,
    // not to share read-your-writes visibility of a row committed in between the two.

    @Test
    @Order(9)
    void testRenderContentAsTextIgnoresServiceName() {
        String contentId = testParams.contentId ?: 'TEST_CNT_SVCNAME'
        String dataResourceId = testParams.dataResourceId ?: 'TEST_DR_SVCNAME'
        delegator.create('DataResource', [dataResourceId: dataResourceId, dataResourceTypeId: 'ELECTRONIC_TEXT'])
        delegator.create('ElectronicText', [dataResourceId: dataResourceId, textData: 'Test text for service name ignore check'])
        delegator.create('Content', [contentId: contentId, contentTypeId: 'TEST_CONTENT_TYPE',
                dataResourceId: dataResourceId, serviceName: 'aServiceThatDefinitelyDoesNotExist'])
        GenericValue content = from('Content').where('contentId', contentId).queryOne()
        assert content
        assert content.serviceName

        StringWriter out = new StringWriter()
        Map<String, Object> templateContext = [userLogin: userLogin, requestParameters: [:]]
        // must render the underlying data straight through, not attempt to resolve/run the bogus service name
        ContentWorker.renderContentAsText(dispatcher, content, out, templateContext, Locale.US, 'text/plain', false, null)
        assert out.toString().contains('Test text for service name ignore check')
    }

    @Test
    @Order(10)
    void testRenderContentAsTextRunsTypedCustomMethod() {
        String contentId = testParams.contentId ?: 'TEST_CNT_CM_RENDER'
        String customMethodId = testParams.customMethodId ?: 'TEST_CM_RENDER'
        String dataResourceId = testParams.dataResourceId ?: 'TEST_DR_CM_RENDER'
        delegator.create('DataResource', [dataResourceId: dataResourceId, dataResourceTypeId: 'ELECTRONIC_TEXT'])
        delegator.create('ElectronicText', [dataResourceId: dataResourceId, textData: 'Test text for typed custom method check'])
        // CONTENT_RENDER is shipped seed data (framework/common/data/CommonTypeData.xml)
        delegator.create('CustomMethod', [customMethodId: customMethodId, customMethodTypeId: 'CONTENT_RENDER',
                customMethodName: 'getDataResource'])
        delegator.create('Content', [contentId: contentId, contentTypeId: 'TEST_CONTENT_TYPE',
                dataResourceId: dataResourceId, customMethodId: customMethodId])
        GenericValue content = from('Content').where('contentId', contentId).queryOne()
        assert content

        StringWriter out = new StringWriter()
        Map<String, Object> templateContext = [userLogin: userLogin,
                                                 requestParameters: [dataResourceId: dataResourceId]]
        ContentWorker.renderContentAsText(dispatcher, content, out, templateContext, Locale.US, 'text/plain', false, null)
        // the invoked service's OUT parameters were merged into templateContext
        assert templateContext.resultData
        assert out.toString().contains('Test text for typed custom method check')
    }

    @Test
    @Order(11)
    void testRenderContentAsTextRejectsCustomMethodOfWrongType() {
        String contentId = testParams.contentId ?: 'TEST_CNT_CM_WRONG'
        String customMethodId = testParams.customMethodId ?: 'TEST_CM_WRONG_TYPE'
        // TELECOM_GATEWAY is shipped seed data (framework/common/data/CommonTypeData.xml), unrelated to
        // content rendering - any type other than CONTENT_RENDER proves the point
        delegator.create('CustomMethod', [customMethodId: customMethodId, customMethodTypeId: 'TELECOM_GATEWAY',
                customMethodName: 'getDataResource'])
        delegator.create('Content', [contentId: contentId, contentTypeId: 'TEST_CONTENT_TYPE',
                dataResourceId: 'TEST_CONTENT_TEXT1', customMethodId: customMethodId])
        GenericValue content = from('Content').where('contentId', contentId).queryOne()
        assert content

        StringWriter out = new StringWriter()
        Map<String, Object> templateContext = [userLogin: userLogin, requestParameters: [:]]
        GeneralException exception = assertThrows(GeneralException) {
            ContentWorker.renderContentAsText(dispatcher, content, out, templateContext, Locale.US, 'text/plain', false, null)
        }
        assert exception.message.contains('not a content rendering method')
    }

}
