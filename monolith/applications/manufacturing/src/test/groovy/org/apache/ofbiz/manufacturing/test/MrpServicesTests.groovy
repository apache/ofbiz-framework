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
package org.apache.ofbiz.manufacturing.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.manufacturing.mrp.MrpServices
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper

import java.sql.Timestamp
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class MrpServicesTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testRequiredMrpProposedOrderIsNotLateForSlightlyEarlierTimestamp() {
        Timestamp runStartedAt = Timestamp.valueOf('2026-07-22 11:30:00.100')
        Timestamp requirementStartDate = Timestamp.valueOf('2026-07-22 11:30:00.095')

        assert !MrpServices.isProposedOrderLate(requirementStartDate, runStartedAt, mrpEvent(mrpEventTypeId: 'REQUIRED_MRP'))
    }

    @Test
    @Order(2)
    void testNonRequiredMrpProposedOrderRemainsLateWhenStartDatePrecedesRun() {
        Timestamp runStartedAt = Timestamp.valueOf('2026-07-22 11:30:00.100')
        Timestamp requirementStartDate = Timestamp.valueOf('2026-07-22 11:29:59.000')

        assert MrpServices.isProposedOrderLate(requirementStartDate, runStartedAt, mrpEvent(mrpEventTypeId: 'SALES_ORDER_SHIP'))
    }

    private static GenericValue mrpEvent(Map fields) {
        new StubGenericValue(fields)
    }

    private static class StubGenericValue extends GenericValue {

        private final Map fields

        StubGenericValue(Map fields) {
            this.fields = fields
        }

        @Override
        Object get(String name) {
            fields[name]
        }

        @Override
        Object get(Object key) {
            fields[key]
        }

        @Override
        String getString(String name) {
            Object value = fields[name]
            value == null ? null : value.toString()
        }

        Object getProperty(String name) {
            if (fields.containsKey(name)) {
                return fields[name]
            }
            super.getProperty(name)
        }

    }

}
