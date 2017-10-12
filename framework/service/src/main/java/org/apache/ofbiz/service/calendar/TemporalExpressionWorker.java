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
package org.apache.ofbiz.service.calendar;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/** TemporalExpression persistence worker. */
public final class TemporalExpressionWorker {

    public final static String module = TemporalExpressionWorker.class.getName();

    // Temporal expression type constants
    private final static String DateRange = "DATE_RANGE";
    private final static String DayInMonth = "DAY_IN_MONTH";
    private final static String DayOfMonthRange = "DAY_OF_MONTH_RANGE";
    private final static String DayOfWeekRange = "DAY_OF_WEEK_RANGE";
    private final static String Difference = "DIFFERENCE";
    private final static String Frequency = "FREQUENCY";
    private final static String HourRange = "HOUR_RANGE";
    private final static String Intersection = "INTERSECTION";
    private final static String MinuteRange = "MINUTE_RANGE";
    private final static String MonthRange = "MONTH_RANGE";
    private final static String Substitution = "SUBSTITUTION";
    private final static String Union = "UNION";
    private final static String ExpressionTypeList[] = {DateRange, DayInMonth, DayOfMonthRange, DayOfWeekRange,
        Difference, Frequency, HourRange, Intersection, MinuteRange, MonthRange, Substitution, Union};

    // Temporal expression assoc type constants
    private final static String INCLUDE = "INCLUDE";
    private final static String EXCLUDE = "EXCLUDE";
    private final static String SUBSTITUTE = "SUBSTITUTE";

    private TemporalExpressionWorker () {}

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
        GenericValue exprValue = EntityQuery.use(delegator).from("TemporalExpression").where("tempExprId", tempExprId).cache().queryOne();
        if (UtilValidate.isEmpty(exprValue)) {
            throw new IllegalArgumentException("tempExprId argument invalid - expression not found");
        }
        TemporalExpression result = makeTemporalExpression(delegator, exprValue);
        if (Debug.verboseOn()) {
            TemporalExpressionPrinter printer = new TemporalExpressionPrinter(result);
            Debug.logVerbose(printer.toString(), module);
        }
        return result;
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
            return setExpressionId(exprValue, new TemporalExpressions.DateRange(exprValue.getTimestamp("date1"), exprValue.getTimestamp("date2")));
        } else if (DayInMonth.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.DayInMonth(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (DayOfMonthRange.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.DayOfMonthRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (DayOfWeekRange.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.DayOfWeekRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (Difference.equals(tempExprTypeId)) {
            List<GenericValue> childExpressions = EntityQuery.use(delegator).from("TemporalExpressionAssoc").where("fromTempExprId", tempExprId).cache(true).queryList();
            GenericValue inclAssoc = null;
            GenericValue exclAssoc = null;
            for (GenericValue childExpression : childExpressions) {
                if (INCLUDE.equals(childExpression.get("exprAssocType"))) {
                    inclAssoc = childExpression;
                } else if (EXCLUDE.equals(childExpression.get("exprAssocType"))) {
                    exclAssoc = childExpression;
                }
            }
            if (inclAssoc != null && exclAssoc != null) {
                return setExpressionId(exprValue, new TemporalExpressions.Difference(getTemporalExpression(delegator, inclAssoc.getString("toTempExprId")), getTemporalExpression(delegator, exclAssoc.getString("toTempExprId"))));
            }
        } else if (Frequency.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.Frequency(exprValue.getTimestamp("date1"), exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (HourRange.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.HourRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (Intersection.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.Intersection(getChildExpressions(delegator, tempExprId)));
        } else if (MinuteRange.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.MinuteRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (MonthRange.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.MonthRange(exprValue.getLong("integer1").intValue(), exprValue.getLong("integer2").intValue()));
        } else if (Substitution.equals(tempExprTypeId)) {
            List<GenericValue> childExpressions = EntityQuery.use(delegator).from("TemporalExpressionAssoc").where("fromTempExprId", tempExprId).cache(true).queryList();
            GenericValue inclAssoc = null;
            GenericValue exclAssoc = null;
            GenericValue substAssoc = null;
            for (GenericValue childExpression : childExpressions) {
                if (INCLUDE.equals(childExpression.get("exprAssocType"))) {
                    inclAssoc = childExpression;
                } else if (EXCLUDE.equals(childExpression.get("exprAssocType"))) {
                    exclAssoc = childExpression;
                } else if (SUBSTITUTE.equals(childExpression.get("exprAssocType"))) {
                    substAssoc = childExpression;
                }
            }
            if (inclAssoc != null && exclAssoc != null && substAssoc != null) {
                return setExpressionId(exprValue, new TemporalExpressions.Substitution(getTemporalExpression(delegator, inclAssoc.getString("toTempExprId")), getTemporalExpression(delegator, exclAssoc.getString("toTempExprId")), getTemporalExpression(delegator, substAssoc.getString("toTempExprId"))));
            }
        } else if (Union.equals(tempExprTypeId)) {
            return setExpressionId(exprValue, new TemporalExpressions.Union(getChildExpressions(delegator, tempExprId)));
        }
        return TemporalExpressions.NullExpression;
    }

    private static Set<TemporalExpression> getChildExpressions(Delegator delegator, String tempExprId) throws GenericEntityException {
        List<GenericValue> valueList = EntityQuery.use(delegator).from("TemporalExpressionAssoc").where("fromTempExprId", tempExprId).cache(true).queryList();
        if (UtilValidate.isEmpty(valueList)) {
            throw new IllegalArgumentException("tempExprId argument invalid - no child expressions found");
        }
        Set<TemporalExpression> exprList = new TreeSet<>();
        for (GenericValue value : valueList) {
            exprList.add(makeTemporalExpression(delegator, value.getRelatedOne("ToTemporalExpression", false)));
        }
        return exprList;
    }

    private static TemporalExpression setExpressionId(GenericValue value, TemporalExpression expression) {
        expression.setId(value.getString("tempExprId"));
        return expression;
    }

    public static String[] getExpressionTypeList() {
        return ExpressionTypeList.clone();
    }
}
