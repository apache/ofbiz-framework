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
package org.apache.ofbiz.accounting.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class AutoAcctgFixedAssetTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateFixedAssetMaint() {
        String fixedAssetId = testParams.fixedAssetId ?: '1000'
        Map serviceCtx = [
                fixedAssetId: fixedAssetId,
                statusId: testParams.statusId ?: 'FAM_CREATED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMaint', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMaint = from('FixedAssetMaint')
                .where('fixedAssetId', fixedAssetId)
                .queryFirst()

        assert fixedAssetMaint
        assert fixedAssetMaint.maintHistSeqId != null
    }

    @Test
    @Order(2)
    void testCreateFixedAssetMeter() {
        String fixedAssetId = testParams.fixedAssetId ?: '1000'
        String productMeterTypeId = testParams.productMeterTypeId ?: 'DISTANCE'
        Map serviceCtx = [
                   fixedAssetId: fixedAssetId,
                   productMeterTypeId: productMeterTypeId,
                   readingDate: UtilDateTime.nowTimestamp(),
                   meterValue: new BigDecimal('10'),
                   userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                       .where('fixedAssetId', fixedAssetId, 'productMeterTypeId', productMeterTypeId)
                       .queryFirst()
        assert fixedAssetMeter
        assert fixedAssetMeter.meterValue == BigDecimal.TEN
    }

    @Test
    @Order(3)
    void testCancelFixedAssetStdCost() {
        String fixedAssetId = testParams.fixedAssetId ?: '1000'
        String fixedAssetStdCostTypeId = testParams.fixedAssetStdCostTypeId ?: 'SETUP_COST'
        Map serviceCtx = [
                        fixedAssetId: fixedAssetId,
                        fixedAssetStdCostTypeId: fixedAssetStdCostTypeId,
                        fromDate: UtilDateTime.toTimestamp('11/03/2016 00:00:00'),
                        userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('cancelFixedAssetStdCost', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetStdCost = from('FixedAssetStdCost')
                .where('fixedAssetId', fixedAssetId,
                       'fixedAssetStdCostTypeId', fixedAssetStdCostTypeId,
                       'fromDate', UtilDateTime.toTimestamp('11/03/2016 00:00:00'))
                .queryFirst()

        assert fixedAssetStdCost
        assert fixedAssetStdCost.thruDate != null
    }

}
