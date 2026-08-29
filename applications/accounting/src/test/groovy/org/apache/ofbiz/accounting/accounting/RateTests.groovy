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

import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilDateTime

import java.sql.Timestamp
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class RateTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testExpirePartyRate() {
        Timestamp fromDate = UtilDateTime.toTimestamp('07/04/2013 00:00:00')
        String partyId = testParams.partyId ?: 'TEST_PARTY'
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        Map serviceCtx = [
                partyId: partyId,
                rateTypeId: rateTypeId,
                rateAmountFromDate: fromDate,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('expirePartyRate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRate = from('PartyRate').where('rateTypeId', rateTypeId, 'partyId', partyId, 'fromDate', fromDate).queryOne()
        assert partyRate
        assert partyRate.thruDate
    }

    @Test
    @Order(2)
    void testUpdateRateAmount() {
        Timestamp fromDate = UtilDateTime.toTimestamp('04/07/2013 00:00:00')
        String periodTypeId = testParams.periodTypeId ?: 'RATE_HOUR'
        String rateTypeId = testParams.rateTypeId ?: 'OVERTIME'
        String rateCurrencyUomId = testParams.rateCurrencyUomId ?: 'USD'
        String emplPositionTypeId = testParams.emplPositionTypeId ?: 'TEST_EMPLOYEE'
        Map serviceCtx = [
                periodTypeId: periodTypeId,
                rateTypeId: rateTypeId,
                rateCurrencyUomId: rateCurrencyUomId,
                rateAmount: BigDecimal.valueOf(25),
                emplPositionTypeId: emplPositionTypeId,
                fromDate: fromDate,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from('RateAmount')
                .where('rateTypeId', rateTypeId,
                       'workEffortId', '_NA_',
                       'rateCurrencyUomId', rateCurrencyUomId,
                       'emplPositionTypeId', emplPositionTypeId,
                       'partyId', '_NA_',
                       'periodTypeId', periodTypeId,
                       'fromDate', fromDate).queryOne()
        assert rateAmount
        assert rateAmount.rateAmount == 25
    }

    @Test
    @Order(3)
    void testGetRateAmount() {
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        String workEffortId = testParams.workEffortId ?: 'Test_effort'
        Map serviceCtx = [
                rateTypeId: rateTypeId,
                workEffortId: workEffortId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.rateAmount == 75
    }

    @Test
    @Order(4)
    void testGetRatesAmountsFromWorkEffortId() {
        String periodTypeId = testParams.periodTypeId ?: 'RATE_HOUR'
        String rateCurrencyUomId = testParams.rateCurrencyUomId ?: 'USD'
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        String workEffortId = testParams.workEffortId ?: 'Test_effort'
        Map serviceCtx = [
                periodTypeId: periodTypeId,
                rateCurrencyUomId: rateCurrencyUomId,
                rateTypeId: rateTypeId,
                workEffortId: workEffortId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromWorkEffortId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList
    }

    @Test
    @Order(5)
    void testGetRatesAmountsFromPartyId() {
        String periodTypeId = testParams.periodTypeId ?: 'RATE_HOUR'
        String rateCurrencyUomId = testParams.rateCurrencyUomId ?: 'USD'
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        String partyId = testParams.partyId ?: 'TEST_PARTY'
        Map serviceCtx = [
                periodTypeId: periodTypeId,
                rateCurrencyUomId: rateCurrencyUomId,
                rateTypeId: rateTypeId,
                partyId: partyId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromPartyId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList != null
    }

    @Test
    @Order(6)
    void testGetRatesAmountsFromEmplPositionTypeId() {
        String periodTypeId = testParams.periodTypeId ?: 'RATE_HOUR'
        String rateCurrencyUomId = testParams.rateCurrencyUomId ?: 'USD'
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        String emplPositionTypeId = testParams.emplPositionTypeId ?: 'TEST_EMPLOYEE'
        Map serviceCtx = [
                periodTypeId: periodTypeId,
                rateCurrencyUomId: rateCurrencyUomId,
                rateTypeId: rateTypeId,
                emplPositionTypeId: emplPositionTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromEmplPositionTypeId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList
    }

    @Test
    @Order(7)
    void testUpdatePartyRate() {
        Timestamp fromDate = UtilDateTime.toTimestamp('04/07/2013 00:00:00')
        String partyId = testParams.partyId ?: 'TEST_PARTY'
        String periodTypeId = testParams.periodTypeId ?: 'RATE_MONTH'
        String rateTypeId = testParams.rateTypeId ?: 'DISCOUNTED'
        Map serviceCtx = [
                partyId: partyId,
                periodTypeId: periodTypeId,
                rateTypeId: rateTypeId,
                rateAmount: BigDecimal.valueOf(75),
                fromDate: fromDate,
                userLogin: userLogin

        ]
        Map serviceResult = dispatcher.runSync('updatePartyRate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from('RateAmount')
                .where('rateTypeId', rateTypeId, 'workEffortId', '_NA_', 'rateCurrencyUomId', 'USD', 'emplPositionTypeId', '_NA_',
                        'partyId', partyId, 'periodTypeId', periodTypeId, 'fromDate', fromDate).queryOne()
        GenericValue partyRate = from('PartyRate').where('rateTypeId', rateTypeId, 'partyId', partyId, 'fromDate', fromDate).queryOne()

        assert rateAmount
        assert partyRate
        assert rateAmount.rateAmount == 75
    }

    @Test
    @Order(8)
    void testFilterRateAmountList() {
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        List<GenericValue> amountList = from('RateAmount').where('rateTypeId', rateTypeId, 'rateCurrencyUomId', 'USD').queryList()
        Map serviceCtx = [
                ratesList: amountList,
                rateTypeId: rateTypeId,
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('filterRateAmountList', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.filteredRatesList
    }

    @Test
    @Order(9)
    void testExpireRateAmount() {
        Timestamp fromDate = UtilDateTime.toTimestamp('07/04/2013 00:00:00')
        String emplPositionTypeId = testParams.emplPositionTypeId ?: 'TEST_EMPLOYEE'
        String rateTypeId = testParams.rateTypeId ?: 'AVERAGE_PAY_RATE'
        String periodTypeId = testParams.periodTypeId ?: 'RATE_MONTH'
        Map serviceCtx = [
                emplPositionTypeId: emplPositionTypeId,
                rateTypeId: rateTypeId,
                periodTypeId: periodTypeId,
                fromDate: fromDate,
                userLogin: userLogin

        ]
        Map serviceResult = dispatcher.runSync('expireRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from('RateAmount')
                .where('rateTypeId', rateTypeId, 'workEffortId', '_NA_', 'rateCurrencyUomId', 'USD',
                        'emplPositionTypeId', emplPositionTypeId, 'partyId', '_NA_', 'periodTypeId', periodTypeId, 'fromDate', fromDate).queryOne()
        assert rateAmount
        assert rateAmount.thruDate
    }

}
