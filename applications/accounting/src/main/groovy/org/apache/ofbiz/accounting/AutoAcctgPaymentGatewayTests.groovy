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
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class AutoAcctgPaymentGatewayTests extends OFBizTestCase {
    public AutoAcctgPaymentGatewayTests(String name) {
        super(name)
    }

    void testUpdatePaymentGatewayConfig() {
        Map serviceCtx = [:]
        serviceCtx.paymentGatewayConfigId = 'SAGEPAY_CONFIG'
        serviceCtx.description = 'Test Payment Gateway Config Id'
        serviceCtx.userLogin = EntityQuery.use(delegator).from('UserLogin').where('userLoginId', 'system').cache().queryOne()
        Map serviceResult = dispatcher.runSync('updatePaymentGatewayConfig', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue paymentGatewayConfig = EntityQuery.use(delegator).from('PaymentGatewayConfig').where('paymentGatewayConfigId', 'SAGEPAY_CONFIG').queryOne()
        assert paymentGatewayConfig
        assert paymentGatewayConfig.description  == 'Test Payment Gateway Config Id'
    }
}
