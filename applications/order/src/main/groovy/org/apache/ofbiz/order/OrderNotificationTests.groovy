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
package org.apache.ofbiz.order

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class OrderNotificationTests extends OFBizTestCase {
    public OrderNotificationTests(String name) {
        super(name)
    }
    
    
    void testSendOrderConfirmation() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            sendTo: 'test_email@example.com',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('sendOrderConfirmation', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.emailType.equals("PRDS_ODR_CONFIRM")
    }
    void testSendOrderChangeNotification() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            sendTo: 'test_email@example.com',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('sendOrderChangeNotification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.emailType.equals("PRDS_ODR_CHANGE")
    }
    void testSendOrderBackorderNotification() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            sendTo: 'test_email@example.com',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('sendOrderBackorderNotification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.emailType.equals("PRDS_ODR_BACKORDER")
    }
    void testsendOrderPayRetryNotification() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            sendTo: 'test_email@example.com',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('sendOrderPayRetryNotification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.emailType.equals("PRDS_ODR_PAYRETRY")
    }
    void testsendOrderCompleteNotification() {
        Map serviceCtx = [
            orderId: 'TEST_DEMO10090',
            sendTo: 'test_email@example.com',
            userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('sendOrderCompleteNotification', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.emailType.equals("PRDS_ODR_COMPLETE")
    }
}
