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
package org.ofbiz.service.calendar;

import com.ibm.icu.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

/** TemporalExpression persistence worker. */
public class TemporalExpressionWorker {

    // Temporal expression constants
    public final static String DateRange = "DATE_RANGE";
    public final static String DayInMonth = "DAY_IN_MONTH";
    public final static String DayOfMonthRange = "DAY_OF_MONTH_RANGE";
    public final static String DayOfWeekRange = "DAY_OF_WEEK_RANGE";
    public final static String Difference = "DIFFERENCE";
    public final static String Frequency = "FREQUENCY";
    public final static String Intersection = "INTERSECTION";
    public final static String MonthRange = "MONTH_RANGE";
    public final static String TimeOfDayRange = "TIME_OF_DAY_RANGE";
    public final static String Union = "UNION";
    public final static String ExpressionTypeList[] = {DateRange, DayInMonth, DayOfMonthRange, DayOfWeekRange,
        Difference, Frequency, Intersection, MonthRange, TimeOfDayRange, Union};

    /** Get a <code>TemporalExpression</code> from persistent storage.
     * @param delegator
     * @param tempExprId
     * @return A <code>TemporalExpression</code> instance based on <code>tempExprId</code>
     * @throws GenericEntityException
     */
    public static TemporalExpression getTemporalExpression(Delegator delegator, String tempExprId) throws GenericEntityException {
        if (UtilValidate.isEmpty(tempExprId)) {
            throw new IllegalArgumentException("tempExprId argument cannot be empty");
        }
        GenericValue exprValue = delegator.findOne("TemporalExpression", UtilMisc.toMap("tempExprId", tempExprId), true);
        if (UtilValidate.isEmpty(exprValue)) {
            throw new IllegalArgumentException("tempExprId argument invalid - expression not found");
        }
        return makeTemporalExpression(delegator, exprValue);
    }

    /** Create a <code>TemporalExpression</code> instance from a TemporalExpression
     * GenericValue.<p>This method makes recursive calls, so care must be taken to
     * avoid endless loops.</p>
     * @param delegator
     * @param exprValue
     * @return A <code>TemporalExpression</code> instance based on <code>exprValue</code>
     * @throws GenericEntityException
     */
    public static TemporalExpression makeTemporalExpression(Delegator delegator, GenericValue exprValue) throws GenericEntityException {
        String tempExprId = exprValue.getString("tempExprId");
        String tempExprTypeId = exprValue.getString("tempExprTypeId");
        if (DateRange.equals(tempExprTypeId)) {
            return new TemporalExpressions.DateRange(exprValue.getTimestamp("date1"), exprValue.getTimestamp("date2"));
        } else if (DayInMonth.equals(tempExprTypeId)) {
            return new TemporalExpressions.DayInMonth(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue());
        } else if (DayOfMonthRange.equals(tempExprTypeId)) {
            return new TemporalExpressions.DayOfMonthRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue());
        } else if (DayOfWeekRange.equals(tempExprTypeId)) {
            return new TemporalExpressions.DayOfWeekRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue());
        } else if (Difference.equals(tempExprTypeId)) {
            GenericValue inclAssoc = EntityUtil.getFirst(delegator.findList("TemporalExpressionAssoc", EntityCondition.makeCondition(EntityCondition.makeCondition("fromTempExprId", tempExprId), EntityCondition.makeCondition("exprAssocType", "INCLUDE")), null, null, null, true));
            GenericValue exclAssoc = EntityUtil.getFirst(delegator.findList("TemporalExpressionAssoc", EntityCondition.makeCondition(EntityCondition.makeCondition("fromTempExprId", tempExprId), EntityCondition.makeCondition("exprAssocType", "EXCLUDE")), null, null, null, true));
            if (inclAssoc != null && exclAssoc != null) {
                return new TemporalExpressions.Difference(getTemporalExpression(delegator, inclAssoc.getString("toTempExprId")), getTemporalExpression(delegator, exclAssoc.getString("toTempExprId")));
            }
        } else if (Frequency.equals(tempExprTypeId)) {
            return new TemporalExpressions.Frequency(exprValue.getTimestamp("date1"), exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue());
        } else if (Intersection.equals(tempExprTypeId)) {
            return new TemporalExpressions.Intersection(getChildExpressions(delegator, tempExprId));
        } else if (MonthRange.equals(tempExprTypeId)) {
            return new TemporalExpressions.MonthRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue());
        } else if (TimeOfDayRange.equals(tempExprTypeId)) {
            int interval = Calendar.HOUR_OF_DAY;
            int count = 1;
            Long longObj = exprValue.getLong("integer1");
            if (longObj != null) {
                interval = longObj.intValue();
            }
            longObj = exprValue.getLong("integer2");
            if (longObj != null) {
                count = longObj.intValue();
            }
            return new TemporalExpressions.TimeOfDayRange(exprValue.getString("string1"), exprValue.getString("string2"), interval, count);
        } else if (Union.equals(tempExprTypeId)) {
            return new TemporalExpressions.Union(getChildExpressions(delegator, tempExprId));
        }
        return TemporalExpressions.NullExpression;
    }

    protected static Set<TemporalExpression> getChildExpressions(Delegator delegator, String tempExprId) throws GenericEntityException {
        List<GenericValue> valueList = delegator.findList("TemporalExpressionAssoc", EntityCondition.makeCondition("fromTempExprId", tempExprId), null, null, null, true);
        if (UtilValidate.isEmpty(valueList)) {
            throw new IllegalArgumentException("tempExprId argument invalid - no child expressions found");
        }
        Set<TemporalExpression> exprList = new TreeSet<TemporalExpression>();
        for (GenericValue value : valueList) {
            exprList.add(makeTemporalExpression(delegator, value.getRelatedOne("ToTemporalExpression")));
        }
        return exprList;
    }
}
