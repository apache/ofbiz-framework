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
package org.apache.ofbiz.accounting

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgCostTests extends OFBizTestCase {
    public AutoAcctgCostTests(String name) {
        super(name)
    }

    void testUpdateProductAverageCostOnReceiveInventory() {
        Map serviceCtx = [:]
        serviceCtx.facilityId = 'DemoFacility1'
        serviceCtx.quantityAccepted = new BigDecimal('10')
        serviceCtx.productId = 'TestProduct3'
        serviceCtx.inventoryItemId = '9999'
        serviceCtx.userLogin = userLogin
        Map result = dispatcher.runSync('updateProductAverageCostOnReceiveInventory', serviceCtx)
        assert ServiceUtil.isSuccess(result)

        GenericValue productAverageCost = from('ProductAverageCost').filterByDate()
                                .where('productId', 'TestProduct3', 'facilityId', 'DemoFacility1').queryFirst()
        assert productAverageCost
        assert productAverageCost.productAverageCostTypeId == 'SIMPLE_AVG_COST'
        assert productAverageCost.averageCost == 9
    }

    void testGetProductAverageCost() {
        GenericValue inventoryItem = from('InventoryItem')
                                            .where('inventoryItemId', '9999').queryOne()
        Map serviceCtx = [
                inventoryItem: inventoryItem,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getProductAverageCost', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        assert serviceResult.unitCost == 9
    }
}