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

import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilDateTime

import java.sql.Timestamp

class RateTests extends OFBizTestCase {
    public RateTests(String name) {
        super(name)
    }

    void testExpirePartyRate() {
        Timestamp fromDate = UtilDateTime.toTimestamp("07/04/2013 00:00:00")
        Map serviceCtx = [
                partyId   : "TEST_PARTY",
                rateTypeId          : 'AVERAGE_PAY_RATE',
                rateAmountFromDate   : fromDate,
                fromDate            :  fromDate,
                userLogin              : userLogin

        ]
        Map serviceResult = dispatcher.runSync('expirePartyRate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue partyRate = from("PartyRate").where("rateTypeId", "AVERAGE_PAY_RATE", "partyId", "TEST_PARTY", "fromDate", fromDate).queryOne()
        assert partyRate
        assert partyRate.thruDate
    }

    void testUpdateRateAmount() {
        Timestamp fromDate = UtilDateTime.toTimestamp("04/07/2013 00:00:00")
        Map serviceCtx = [
                periodTypeId   : "RATE_HOUR",
                rateTypeId          : 'OVERTIME',
                rateCurrencyUomId   : "USD",
                rateAmount          :  BigDecimal.valueOf(25),
                emplPositionTypeId  : "TEST_EMPLOYEE",
                fromDate            :  fromDate,
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from("RateAmount").where("rateTypeId", "OVERTIME", "workEffortId", "_NA_", "rateCurrencyUomId", "USD", "emplPositionTypeId", "TEST_EMPLOYEE", "partyId", "_NA_", "periodTypeId", "RATE_HOUR", "fromDate", fromDate).queryOne()
        assert rateAmount
        assert rateAmount.rateAmount == 25
    }

    void testGetRateAmount() {
        Map serviceCtx = [
                rateTypeId            : 'AVERAGE_PAY_RATE',
                workEffortId          : 'Test_effort',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.rateAmount == 75
    }

    void testGetRatesAmountsFromWorkEffortId() {
        Map serviceCtx = [
                periodTypeId           : 'RATE_HOUR',
                rateCurrencyUomId     : 'USD',
                rateTypeId            : 'AVERAGE_PAY_RATE',
                workEffortId          : 'Test_effort',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromWorkEffortId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList
    }

    void testGetRatesAmountsFromPartyId() {
        Map serviceCtx = [
                periodTypeId    : 'RATE_HOUR',
                rateCurrencyUomId   : 'USD',
                rateTypeId          : 'AVERAGE_PAY_RATE',
                partyId          : 'TEST_PARTY',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromPartyId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList != null
    }

    void testGetRatesAmountsFromEmplPositionTypeId() {
        Map serviceCtx = [
                periodTypeId    : 'RATE_HOUR',
                rateCurrencyUomId   : 'USD',
                rateTypeId          : 'AVERAGE_PAY_RATE',
                emplPositionTypeId          : 'TEST_EMPLOYEE',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('getRatesAmountsFromEmplPositionTypeId', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.ratesList
    }

    void testUpdatePartyRate() {
        Timestamp fromDate = UtilDateTime.toTimestamp("04/07/2013 00:00:00")
        Map serviceCtx = [
                partyId     : "TEST_PARTY",
                periodTypeId   : "RATE_MONTH",
                rateTypeId          : 'DISCOUNTED',
                rateAmount          :  BigDecimal.valueOf(75),
                fromDate            :  fromDate,
                userLogin              : userLogin

        ]
        Map serviceResult = dispatcher.runSync('updatePartyRate', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from("RateAmount").where("rateTypeId", "DISCOUNTED", "workEffortId", "_NA_", "rateCurrencyUomId", "USD", "emplPositionTypeId", "_NA_", "partyId", "TEST_PARTY", "periodTypeId", "RATE_MONTH", "fromDate", fromDate).queryOne()
        GenericValue partyRate = from("PartyRate").where("rateTypeId", "DISCOUNTED", "partyId", "TEST_PARTY", "fromDate", fromDate).queryOne()

        assert rateAmount
        assert partyRate
        assert rateAmount.rateAmount == 75
    }

    void testFilterRateAmountList() {
        List<GenericValue> amountList = from("RateAmount").where("rateTypeId", "AVERAGE_PAY_RATE", "rateCurrencyUomId", "USD").queryList()
        Map serviceCtx = [
                ratesList   : amountList,
                rateTypeId          : 'AVERAGE_PAY_RATE',
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('filterRateAmountList', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.filteredRatesList
    }

    void testExpireRateAmount() {
        Timestamp fromDate = UtilDateTime.toTimestamp("07/04/2013 00:00:00")
        Map serviceCtx = [
                emplPositionTypeId   : 'TEST_EMPLOYEE',
                rateTypeId          : 'AVERAGE_PAY_RATE',
                periodTypeId   : 'RATE_MONTH',
                fromDate            :  fromDate,
                userLogin              : userLogin

        ]
        Map serviceResult = dispatcher.runSync('expireRateAmount', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue rateAmount = from("RateAmount").where("rateTypeId", "AVERAGE_PAY_RATE", "workEffortId", "_NA_", "rateCurrencyUomId", "USD", "emplPositionTypeId", "TEST_EMPLOYEE", "partyId", "_NA_", "periodTypeId", "RATE_MONTH", "fromDate", fromDate).queryOne()
        assert rateAmount
        assert rateAmount.thruDate
    }

}