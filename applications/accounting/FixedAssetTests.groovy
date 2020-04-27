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
package org.apache.ofbiz.accounting;

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class FixedAssetTests extends OFBizTestCase {
    public FixedAssetTests(String name) {
        super(name)
    }
    void testUpdateFixedAssetMeter() {
        Map serviceCtx = [
                fixedAssetId           : '1000',
                productMeterTypeId     : 'ODOMETER',
                readingDate            : UtilDateTime.toTimestamp("24/12/2019 00:00:00"),
                userLogin              : userLogin
        ]
        Map serviceResult = dispatcher.runSync('updateFixedAssetMeter', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue FixedAssetMeter = EntityQuery.use(delegator).from('FixedAssetMeter')
                .where('fixedAssetId', '1000', 'productMeterTypeId', 'ODOMETER')
                .filterByDate()
                .queryOne();
        assert FixedAssetMeter
    }
}