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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import com.ibm.icu.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;

/** TemporalExpression UI artifacts worker. */
public class ExpressionUiHelper {

    /** An array of valid DayInMonth occurrence values. */
    public static final int Occurrence[] = {1, 2, 3, 4, 5, -1, -2, -3, -4 -5};

    /** Returns a List of valid DayInMonth occurrence int values.
     * @return
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
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(7);
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
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(13);
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
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(6);
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
        int listSize = TemporalExpressionWorker.ExpressionTypeList.length;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(listSize);
        for (int i = 0; i < listSize; i++) {
            String exprType = TemporalExpressionWorker.ExpressionTypeList[i];
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
        List<GenericValue> findList = delegator.findList("TemporalExpressionAssoc", EntityCondition.makeCondition("fromTempExprId", tempExprId), null, null, null, true);
        Set<String> excludedIds = FastSet.newInstance();
        for (GenericValue value : findList) {
            excludedIds.add(value.getString("toTempExprId"));
        }
        excludedIds.add(tempExprId);
        findList = delegator.findList("TemporalExpression", null, null, null, null, true);
        Set<String> candidateIds = FastSet.newInstance();
        for (GenericValue value : findList) {
            candidateIds.add(value.getString("tempExprId"));
        }
        candidateIds.removeAll(excludedIds);
        return candidateIds;
    }

}
