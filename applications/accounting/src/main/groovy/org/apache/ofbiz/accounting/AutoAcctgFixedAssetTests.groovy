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

package org.apache.ofbiz.accounting

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgFixedAssetTests extends OFBizTestCase {
    public AutoAcctgFixedAssetTests(String name) {
        super(name)
    }

    void testCreateFixedAssetMaint() {
        Map serviceCtx = [
                fixedAssetId: '1000',
                statusId: 'FAM_CREATED',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMaint', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetMaint = from('FixedAssetMaint')
                .where('fixedAssetId', '1000')
                .queryFirst()

        assert fixedAssetMaint
        assert fixedAssetMaint.maintHistSeqId != null
    }

    void testCreateFixedAssetMeter() {
        Map serviceCtx = [
                   fixedAssetId: '1000',
                   productMeterTypeId: 'DISTANCE',
                   readingDate: UtilDateTime.nowTimestamp(),
                   meterValue: new BigDecimal('10'),
                   userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        GenericValue fixedAssetMeter = from('FixedAssetMeter')
                       .where('fixedAssetId', '1000', 'productMeterTypeId', 'DISTANCE')
                       .queryFirst()
        assert fixedAssetMeter
        assert fixedAssetMeter.meterValue == BigDecimal.TEN
    }

    void testCancelFixedAssetStdCost() {
        Map serviceCtx = [
                        fixedAssetId: '1000',
                        fixedAssetStdCostTypeId: 'SETUP_COST',
                        fromDate: UtilDateTime.toTimestamp("11/03/2016 00:00:00"),
                        userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('cancelFixedAssetStdCost', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue fixedAssetStdCost = from('FixedAssetStdCost')
                .where('fixedAssetId', '1000', 'fixedAssetStdCostTypeId', 'SETUP_COST', 'fromDate', UtilDateTime.toTimestamp("11/03/2016 00:00:00"))
                .queryFirst()

        assert fixedAssetStdCost
        assert fixedAssetStdCost.thruDate != null
    }

}
