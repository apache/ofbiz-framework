/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.ofbiz.product.test

import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * Facility Tests
 */
@JunitJupiterTest
class StockMovesTest implements JupiterTestHelper {

    @Test
    void testStockMoves() {
        Map fsmnCtx = [
                facilityId: 'WebStoreWarehouse',
                userLogin: userLogin
        ]
        Map respMap1 = dispatcher.runSync('findStockMovesNeeded', fsmnCtx)
        assert !respMap1.warningMessageList

        if (respMap1.stockMoveHandled) {
            fsmnCtx.stockMoveHandled = respMap1.stockMoveHandled
        }
        Map respMap2 = dispatcher.runSync('findStockMovesRecommended', fsmnCtx)
        assert !respMap2.warningMessageList

        Map ppsmCtx = [
                productId: 'GZ-2644',
                facilityId: 'WebStoreWarehouse',
                locationSeqId: 'TLTLTLUL01',
                targetLocationSeqId: 'TLTLTLLL01',
                quantityMoved: new BigDecimal('5'),
                userLogin: userLogin
        ]
        dispatcher.runSync('processPhysicalStockMove', ppsmCtx)
    }

}
