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

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class InventoryTests extends OFBizTestCase {
    public InventoryTests(String name) {
        super(name)
    }

    void testGetInventoryAvailableByFacility() {
        Map serviceCtx = [
                productId: 'GZ-2644',
                facilityId: 'WebStoreWarehouse',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getInventoryAvailableByFacility', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.quantityOnHandTotal.compareTo(new BigDecimal('509')) == 0
        assert serviceResult.availableToPromiseTotal.compareTo(new BigDecimal('509')) == 0
    }

    // Test Physical Inventory Adjustment
    void testCreatePhysicalInventoryAndVariance() {
        Map serviceCtx = [
                inventoryItemId: '9024',
                varianceReasonId: 'VAR_LOST',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createPhysicalInventoryAndVariance', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.physicalInventoryId
    }
}