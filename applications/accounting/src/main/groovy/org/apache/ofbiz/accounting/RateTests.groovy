/*
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
        assert partyRate;
        assert partyRate.thruDate
    }
}