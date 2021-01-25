/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package org.apache.ofbiz.product

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil

class ProductConfigTests extends OFBizTestCase {
    public ProductConfigTests(String name) {
        super(name)
    }

    void testCreateProductConfigOption() {
        Map serviceCtx = [:]
        serviceCtx.configItemId = 'testConfigItemId'
        serviceCtx.configOptionName = 'Test Config Option'
        serviceCtx.description = 'Test product config option'
        serviceCtx.userLogin = userLogin
        Map serviceResult = dispatcher.runSync('createProductConfigOption', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.configOptionId

        GenericValue productConfigOption = from('ProductConfigOption')
                .where('configItemId', 'testConfigItemId')
                .queryFirst()
        assert productConfigOption
        assert 'Test Config Option'.equals(productConfigOption.configOptionName)
    }
}
