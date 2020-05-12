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

class ProductFeatureTypeTests extends OFBizTestCase {
    public ProductFeatureTypeTests(String name) {
        super(name)
    }

    void testCreateProductFeatureType() {
        Map serviceCtx = [:]
        serviceCtx.productFeatureTypeId = 'testProdFeat'
        serviceCtx.description = 'Test Description'
        serviceCtx.hasTable = 'N'
        serviceCtx.userLogin = userLogin
        Map result = dispatcher.runSync('createProductFeatureType', serviceCtx)
        assert ServiceUtil.isSuccess(result)

        GenericValue productFeatureType = from('ProductFeatureType').where('productFeatureTypeId', 'testProdFeat').queryOne()
        assert productFeatureType.productFeatureTypeId == 'testProdFeat'
    }
}
