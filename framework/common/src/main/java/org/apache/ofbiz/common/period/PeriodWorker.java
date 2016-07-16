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


package org.apache.ofbiz.common.period;

import java.sql.Timestamp;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;

public final class PeriodWorker {

    public static String module = PeriodWorker.class.getName();

    private PeriodWorker() {}

    /**
     * Method to get a condition that checks that the given fieldName is in a given timePeriod.
     */
    public static EntityCondition getFilterByPeriodExpr(String fieldName, GenericValue timePeriod) {
        Timestamp fromDate;
        Timestamp thruDate;
        if (timePeriod.get("fromDate") instanceof Timestamp) {
            fromDate = timePeriod.getTimestamp("fromDate");
            thruDate = timePeriod.getTimestamp("thruDate");
        } else {
            fromDate = UtilDateTime.toTimestamp(timePeriod.getDate("fromDate"));
            thruDate = UtilDateTime.toTimestamp(timePeriod.getDate("thruDate"));
        }

        EntityConditionList<EntityExpr> betweenCondition = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(fieldName, EntityOperator.GREATER_THAN, fromDate),
                    EntityCondition.makeCondition(fieldName, EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
        return EntityCondition.makeCondition(EntityCondition.makeCondition(fieldName, EntityOperator.NOT_EQUAL, null), betweenCondition);
    }
}
