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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

import com.ibm.icu.util.Calendar;

/** TemporalExpression UI artifacts worker. */
public class ExpressionUiHelper {

    /** An array of valid DayInMonth occurrence values. */
    private static final int Occurrence[] = {1, 2, 3, 4, 5, -1, -2, -3, -4 -5};

    /** Returns a List of valid DayInMonth occurrence int values.
     * @return returns a List of valid DayInMonth occurrence int values
     */
    public static List<?> getOccurrenceList() {
        return Arrays.asList(Occurrence);
    }

    /** Returns a List of Maps containing day of the week values.
     * @param locale
     * @return List of Maps. Each Map has a
     * <code>description</code> entry and a <code>value</code> entry.
     */
    public static List<Map<String, Object>> getDayValueList(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", locale);
        List<Map<String, Object>> result = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            result.add(UtilMisc.toMap("description", (Object)dateFormat.format(tempCal.getTime()), "value", tempCal.get(Calendar.DAY_OF_WEEK)));
            tempCal.roll(Calendar.DAY_OF_WEEK, 1);
        }
        return result;
    }

    /** Returns the first day of the week for the specified locale.
     * @param locale
     * @return The first day of the week for the specified locale
     */
    public static int getFirstDayOfWeek(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        return tempCal.getFirstDayOfWeek();
    }

    /** Returns the last day of the week for the specified locale.
     * @param locale
     * @return The last day of the week for the specified locale
     */
    public static int getLastDayOfWeek(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        tempCal.roll(Calendar.DAY_OF_WEEK, -1);
        return tempCal.get(Calendar.DAY_OF_WEEK);
    }

    /** Returns a List of Maps containing month values.
     * @param locale
     * @return List of Maps. Each Map has a
     * <code>description</code> entry and a <code>value</code> entry.
     */
    public static List<Map<String, Object>> getMonthValueList(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        tempCal.set(Calendar.MONTH, Calendar.JANUARY);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM", locale);
        List<Map<String, Object>> result = new ArrayList<>(13);
        for (int i = Calendar.JANUARY; i <= tempCal.getActualMaximum(Calendar.MONTH); i++) {
            result.add(UtilMisc.toMap("description", (Object)dateFormat.format(tempCal.getTime()), "value", i));
            tempCal.roll(Calendar.MONTH, 1);
        }
        return result;
    }

    /** Returns a List of Maps containing valid Frequency values.
     * @param uiLabelMap CommonUiLabels label Map
     * @return List of Maps. Each Map has a
     * <code>description</code> entry and a <code>value</code> entry.
     */
    public static List<Map<String, Object>> getFrequencyValueList(Map<String, Object> uiLabelMap) {
        List<Map<String, Object>> result = new ArrayList<>(6);
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonSecond"), "value", Calendar.SECOND));
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonMinute"), "value", Calendar.MINUTE));
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonHour"), "value", Calendar.HOUR_OF_DAY));
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonDay"), "value", Calendar.DAY_OF_MONTH));
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonMonth"), "value", Calendar.MONTH));
        result.add(UtilMisc.toMap("description", uiLabelMap.get("CommonYear"), "value", Calendar.YEAR));
        return result;
    }

    /** Returns a List of Maps containing valid temporal expression types.
     * @param uiLabelMap TemporalExpressionUiLabels label Map
     * @return List of Maps. Each Map has a
     * <code>description</code> entry and a <code>value</code> entry.
     */
    public static List<Map<String, Object>> getExpressionTypeList(Map<String, Object> uiLabelMap) {
        int listSize = TemporalExpressionWorker.getExpressionTypeList().length;
        List<Map<String, Object>> result = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            String exprType = TemporalExpressionWorker.getExpressionTypeList()[i];
            result.add(UtilMisc.toMap("description", uiLabelMap.get("TemporalExpression_" + exprType), "value", exprType));
        }
        return result;
    }

    /** Returns a List of candidate inclusion tempExprId Strings.
     * @param delegator
     * @param tempExprId The ID of the temporal expression needing candidates
     * for inclusion
     * @return Set of candidate tempExprId Strings
     */
    public static Set<String> getCandidateIncludeIds(Delegator delegator, String tempExprId) throws GenericEntityException {
        List<GenericValue> findList = EntityQuery.use(delegator)
                                                 .from("TemporalExpressionAssoc")
                                                 .where("fromTempExprId", tempExprId)
                                                 .cache(true)
                                                 .queryList();
        Set<String> excludedIds = new HashSet<>();
        for (GenericValue value : findList) {
            excludedIds.add(value.getString("toTempExprId"));
        }
        excludedIds.add(tempExprId);
        findList = EntityQuery.use(delegator).from("TemporalExpression").cache(true).queryList();
        Set<String> candidateIds = new HashSet<>();
        for (GenericValue value : findList) {
            candidateIds.add(value.getString("tempExprId"));
        }
        candidateIds.removeAll(excludedIds);
        return candidateIds;
    }

}
