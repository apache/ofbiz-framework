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

import java.util.ArrayList;
import com.ibm.icu.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.calendar.TemporalExpression;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Recurrence Info Object
 */
public class RecurrenceInfo {

    public static final String module = RecurrenceInfo.class.getName();

    protected GenericValue info;
    protected Date startDate;
    protected List<RecurrenceRule> rRulesList;
    protected List<RecurrenceRule> eRulesList;
    protected List<Date> rDateList;
    protected List<Date> eDateList;

    /** Creates new RecurrenceInfo */
    public RecurrenceInfo(GenericValue info) throws RecurrenceInfoException {
        this.info = info;
        if (!info.getEntityName().equals("RecurrenceInfo"))
            throw new RecurrenceInfoException("Invalid RecurrenceInfo Value object.");
        init();
    }

    /** Initializes the rules for this RecurrenceInfo object. */
    public void init() throws RecurrenceInfoException {

        if (info.get("startDateTime") == null)
            throw new RecurrenceInfoException("Recurrence startDateTime cannot be null.");

        // Get start date
        long startTime = info.getTimestamp("startDateTime").getTime();

        if (startTime > 0) {
            int nanos = info.getTimestamp("startDateTime").getNanos();

            startTime += (nanos / 1000000);
        } else {
            throw new RecurrenceInfoException("Recurrence startDateTime must have a value.");
        }
        startDate = new Date(startTime);

        // Get the recurrence rules objects
        try {
            rRulesList = new ArrayList<RecurrenceRule>();
            for (GenericValue value: info.getRelated("RecurrenceRule", null, null, false)) {
                rRulesList.add(new RecurrenceRule(value));
            }
        } catch (GenericEntityException gee) {
            rRulesList = null;
        } catch (RecurrenceRuleException rre) {
            throw new RecurrenceInfoException("Illegal rule format.", rre);
        }

        // Get the exception rules objects
        try {
            eRulesList = new ArrayList<RecurrenceRule>();
            for (GenericValue value: info.getRelated("ExceptionRecurrenceRule", null, null, false)) {
                eRulesList.add(new RecurrenceRule(value));
            }
        } catch (GenericEntityException gee) {
            eRulesList = null;
        } catch (RecurrenceRuleException rre) {
            throw new RecurrenceInfoException("Illegal rule format", rre);
        }

        // Get the recurrence date list
        rDateList = RecurrenceUtil.parseDateList(StringUtil.split(info.getString("recurrenceDateTimes"), ","));
        // Get the exception date list
        eDateList = RecurrenceUtil.parseDateList(StringUtil.split(info.getString("exceptionDateTimes"), ","));

        // Sort the lists.
        Collections.sort(rDateList);
        Collections.sort(eDateList);
    }

    /** Returns the primary key for this value object */
    public String getID() {
        return info.getString("recurrenceInfoId");
    }

    /** Returns the startDate Date object. */
    public Date getStartDate() {
        return this.startDate;
    }

    /** Returns the long value of the startDate. */
    public long getStartTime() {
        return this.startDate.getTime();
    }

    /** Returns a recurrence rule iterator */
    public Iterator<RecurrenceRule> getRecurrenceRuleIterator() {
        return rRulesList.iterator();
    }

    /** Returns a sorted recurrence date iterator */
    public Iterator<Date> getRecurrenceDateIterator() {
        return rDateList.iterator();
    }

    /** Returns a exception recurrence iterator */
    public Iterator<RecurrenceRule> getExceptionRuleIterator() {
        return eRulesList.iterator();
    }

    /** Returns a sorted exception date iterator */
    public Iterator<Date> getExceptionDateIterator() {
        return eDateList.iterator();
    }

    /** Returns the current count of this recurrence. */
    public long getCurrentCount() {
        if (info.get("recurrenceCount") != null)
            return info.getLong("recurrenceCount").longValue();
        return 0;
    }

    /** Increments the current count of this recurrence and updates the record. */
    public void incrementCurrentCount() throws GenericEntityException {
        incrementCurrentCount(true);
    }

    /** Increments the current count of this recurrence. */
    public void incrementCurrentCount(boolean store) throws GenericEntityException {
        if (store) {
            info.set("recurrenceCount", getCurrentCount() + 1);
            info.store();
        }
    }

    /** Removes the recurrence from persistant store. */
    public void remove() throws RecurrenceInfoException {
        List<RecurrenceRule> rulesList = new ArrayList<RecurrenceRule>();

        rulesList.addAll(rRulesList);
        rulesList.addAll(eRulesList);

        try {
            for (RecurrenceRule rule: rulesList)
                rule.remove();
            info.remove();
        } catch (RecurrenceRuleException rre) {
            throw new RecurrenceInfoException(rre.getMessage(), rre);
        } catch (GenericEntityException gee) {
            throw new RecurrenceInfoException(gee.getMessage(), gee);
        }
    }

    /** Returns the first recurrence. */
    public long first() {
        return startDate.getTime();
        // First recurrence is always the start time
    }

    /** Returns the estimated last recurrence. */
    public long last() {
        // TODO: find the last recurrence.
        return 0;
    }

    /** Returns the next recurrence from now. */
    public long next() {
        return next(RecurrenceUtil.now());
    }

    /** Returns the next recurrence from the specified time. */
    public long next(long fromTime) {
        // Check for the first recurrence (StartTime is always the first recurrence)
        if (getCurrentCount() == 0 || fromTime == 0 || fromTime == startDate.getTime()) {
            return first();
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Date List Size: " + (rDateList == null ? 0 : rDateList.size()), module);
            Debug.logVerbose("Rule List Size: " + (rRulesList == null ? 0 : rRulesList.size()), module);
        }

        // Check the rules and date list
        if (rDateList == null && rRulesList == null) {
            return 0;
        }

        long nextRuleTime = fromTime;
        boolean hasNext = true;

        // Get the next recurrence from the rule(s).
        Iterator<RecurrenceRule> rulesIterator = getRecurrenceRuleIterator();
        while (rulesIterator.hasNext()) {
            RecurrenceRule rule = rulesIterator.next();
            while (hasNext) {
                // Gets the next recurrence time from the rule.
                nextRuleTime = getNextTime(rule, nextRuleTime);
                // Tests the next recurrence against the rules.
                if (nextRuleTime == 0 || isValid(nextRuleTime)) {
                    hasNext = false;
                }
            }
        }
        return nextRuleTime;
    }

    /** Checks the current recurrence validity at the moment. */
    public boolean isValidCurrent() {
        return isValidCurrent(RecurrenceUtil.now());
    }

    /** Checks the current recurrence validity for checkTime. */
    public boolean isValidCurrent(long checkTime) {
        if (checkTime == 0 || (rDateList == null && rRulesList == null)) {
            return false;
        }

        boolean found = false;
        Iterator<RecurrenceRule> rulesIterator = getRecurrenceRuleIterator();
        while (rulesIterator.hasNext()) {
            RecurrenceRule rule = rulesIterator.next();
            long currentTime = rule.validCurrent(getStartTime(), checkTime, getCurrentCount());
            currentTime = checkDateList(rDateList, currentTime, checkTime);
            if ((currentTime > 0) && isValid(checkTime)) {
                found = true;
            } else {
                return false;
            }
        }

        return found;
    }

    private long getNextTime(RecurrenceRule rule, long fromTime) {
        long nextTime = rule.next(getStartTime(), fromTime, getCurrentCount());
        if (Debug.verboseOn()) Debug.logVerbose("Next Time Before Date Check: " + nextTime, module);
        return checkDateList(rDateList, nextTime, fromTime);
    }

    private long checkDateList(List<Date> dateList, long time, long fromTime) {
        long nextTime = time;

        if (UtilValidate.isNotEmpty(dateList)) {
            for (Date thisDate: dateList) {
                if (nextTime > 0 && thisDate.getTime() < nextTime && thisDate.getTime() > fromTime)
                    nextTime = thisDate.getTime();
                else if (nextTime == 0 && thisDate.getTime() > fromTime)
                    nextTime = thisDate.getTime();
            }
        }
        return nextTime;
    }

    private boolean isValid(long time) {
        Iterator<RecurrenceRule> exceptRulesIterator = getExceptionRuleIterator();

        while (exceptRulesIterator.hasNext()) {
            RecurrenceRule except = exceptRulesIterator.next();

            if (except.isValid(getStartTime(), time) || eDateList.contains(new Date(time)))
                return false;
        }
        return true;
    }

    public String primaryKey() {
        return info.getString("recurrenceInfoId");
    }

    public static RecurrenceInfo makeInfo(Delegator delegator, long startTime, int frequency,
            int interval, int count) throws RecurrenceInfoException {
        return makeInfo(delegator, startTime, frequency, interval, count, 0);
    }

    public static RecurrenceInfo makeInfo(Delegator delegator, long startTime, int frequency,
            int interval, long endTime) throws RecurrenceInfoException {
        return makeInfo(delegator, startTime, frequency, interval, -1, endTime);
    }

    public static RecurrenceInfo makeInfo(Delegator delegator, long startTime, int frequency,
            int interval, int count, long endTime) throws RecurrenceInfoException {
        try {
            RecurrenceRule r = RecurrenceRule.makeRule(delegator, frequency, interval, count, endTime);
            String ruleId = r.primaryKey();
            GenericValue value = delegator.makeValue("RecurrenceInfo");

            value.set("recurrenceRuleId", ruleId);
            value.set("startDateTime", new java.sql.Timestamp(startTime));
            delegator.createSetNextSeqId(value);
            RecurrenceInfo newInfo = new RecurrenceInfo(value);

            return newInfo;
        } catch (RecurrenceRuleException re) {
            throw new RecurrenceInfoException(re.getMessage(), re);
        } catch (GenericEntityException ee) {
            throw new RecurrenceInfoException(ee.getMessage(), ee);
        } catch (RecurrenceInfoException rie) {
            throw rie;
        }
    }

    /** Convert a RecurrenceInfo object to a TemporalExpression object.
     * @param info A RecurrenceInfo instance
     * @return A TemporalExpression instance
     */
    public static TemporalExpression toTemporalExpression(RecurrenceInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("info argument cannot be null");
        }
        return new RecurrenceWrapper(info);
    }

    /** Wraps a RecurrenceInfo object with a TemporalExpression object. This
     * class is intended to help with the transition from RecurrenceInfo/RecurrenceRule
     * to TemporalExpression.
     */
    @SuppressWarnings("serial")
    protected static class RecurrenceWrapper extends TemporalExpression {
        protected RecurrenceInfo info;
        protected RecurrenceWrapper() {}
        public RecurrenceWrapper(RecurrenceInfo info) {
            this.info = info;
        }
        @Override
        public Calendar first(Calendar cal) {
            long result = this.info.first();
            if (result == 0) {
                return null;
            }
            Calendar first = (Calendar) cal.clone();
            first.setTimeInMillis(result);
            return first;
        }
        @Override
        public boolean includesDate(Calendar cal) {
            return this.info.isValidCurrent(cal.getTimeInMillis());
        }
        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            long result = this.info.next(cal.getTimeInMillis());
            if (result == 0) {
                return null;
            }
            Calendar next = (Calendar) cal.clone();
            next.setTimeInMillis(result);
            return next;
        }
        @Override
        public void accept(TemporalExpressionVisitor visitor) {}
        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            return false;
        }
    }
}
