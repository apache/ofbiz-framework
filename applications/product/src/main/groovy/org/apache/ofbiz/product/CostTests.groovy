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

class CostTests extends OFBizTestCase {
    public CostTests(String name) {
        super(name)
    }

    void testCalculateProductStandardCosts() {
        String productId = 'PROD_MANUF'
        Map serviceCtx = [
                productId: productId,
                currencyUomId: 'USD',
                costComponentTypePrefix: 'EST_STD',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('calculateProductCosts', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)

        List<GenericValue> costComponents = from('CostComponent').where('productId', productId).filterByDate().queryList()
        BigDecimal costTotalAmount = BigDecimal.ZERO;

        for (GenericValue costComponent : costComponents) {
            assert costComponent.costUomId == 'USD'
            if (costComponent.costComponentTypeId == 'EST_STD_ROUTE_COST') {
                assert costComponent.cost == 10
            } else if (costComponent.costComponentTypeId == 'EST_STD_MAT_COST') {
                assert costComponent.cost == 39
            } else if (costComponent.costComponentTypeId == 'EST_STD_OTHER_COST') {
                assert costComponent.cost == 31
            } else if (costComponent.costComponentTypeId == 'EST_STD_GEN_COST') {
                assert costComponent.cost == 4
            }
            costTotalAmount += costComponent.cost
        }
        assert costTotalAmount == 84
    }

    void testGetProductCost() {
        String productId = 'PROD_MANUF'
        Map serviceCtx = [
                productId: productId,
                currencyUomId: 'USD',
                costComponentTypePrefix: 'EST_STD',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('getProductCost', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assert resultMap.productCost == 84
    }

}