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

import java.util.Arrays;
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Recurrence Rule Object
 */
public class RecurrenceRule {

    public static final String module = RecurrenceRule.class.getName();

    // **********************
    // * byXXX constants
    // **********************
    public static final int MIN_SEC = 0;
    public static final int MAX_SEC = 59;
    public static final int MIN_MIN = 0;
    public static final int MAX_MIN = 59;
    public static final int MIN_HR = 0;
    public static final int MAX_HR = 23;
    public static final int MIN_MTH_DAY = -31;
    public static final int MAX_MTH_DAY = 31;
    public static final int MIN_YEAR_DAY = -366;
    public static final int MAX_YEAR_DAY = 366;
    public static final int MIN_WEEK_NO = -53;
    public static final int MAX_WEEK_NO = 53;
    public static final int MIN_MTH = 1;
    public static final int MAX_MTH = 12;

    // **********************
    // * Frequency constants
    // **********************
    /** Frequency SECONDLY */
    public static final int SECONDLY = 1;

    /** Frequency MINUTELY */
    public static final int MINUTELY = 2;

    /** Frequency HOURLY */
    public static final int HOURLY = 3;

    /** Frequency DAILY */
    public static final int DAILY = 4;

    /** Frequency WEEKLY */
    public static final int WEEKLY = 5;

    /** Frequency MONTHLY */
    public static final int MONTHLY = 6;

    /** Frequency YEARLY */
    public static final int YEARLY = 7;

    // **********************
    // * GenericValue object
    // **********************
    protected GenericValue rule;

    // **********************
    // * Parsed byXXX lists
    // **********************
    protected List<String> bySecondList;
    protected List<String> byMinuteList;
    protected List<String> byHourList;
    protected List<String> byDayList;
    protected List<String> byMonthDayList;
    protected List<String> byYearDayList;
    protected List<String> byWeekNoList;
    protected List<String> byMonthList;
    protected List<String> bySetPosList;

    /**
     * Creates a new RecurrenceRule object from a RecurrenceInfo entity.
     *@param rule GenericValue object defining this rule.
     */
    public RecurrenceRule(GenericValue rule) throws RecurrenceRuleException {
        this.rule = rule;
        if (!rule.getEntityName().equals("RecurrenceRule"))
            throw new RecurrenceRuleException("Invalid RecurrenceRule Value object.");
        init();
    }

    /**
     * Initializes the rules for this RecurrenceInfo object.
     *@throws RecurrenceRuleException
     */
    public void init() throws RecurrenceRuleException {
        // Check the validity of the rule
        String freq = rule.getString("frequency");

        if (!checkFreq(freq))
            throw new RecurrenceRuleException("Recurrence FREQUENCY is a required parameter.");
        if (rule.getLong("intervalNumber").longValue() < 1)
            throw new RecurrenceRuleException("Recurrence INTERVAL must be a positive integer.");

        // Initialize the byXXX lists
        bySecondList = StringUtil.split(rule.getString("bySecondList"), ",");
        byMinuteList = StringUtil.split(rule.getString("byMinuteList"), ",");
        byHourList = StringUtil.split(rule.getString("byHourList"), ",");
        byDayList = StringUtil.split(rule.getString("byDayList"), ",");
        byMonthDayList = StringUtil.split(rule.getString("byMonthDayList"), ",");
        byYearDayList = StringUtil.split(rule.getString("byYearDayList"), ",");
        byWeekNoList = StringUtil.split(rule.getString("byWeekNoList"), ",");
        byMonthList = StringUtil.split(rule.getString("byMonthList"), ",");
        bySetPosList = StringUtil.split(rule.getString("bySetPosList"), ",");
    }

    // Checks for a valid frequency property.
    private boolean checkFreq(String freq) {
        if (freq == null)
            return false;
        if (freq.equalsIgnoreCase("SECONDLY"))
            return true;
        if (freq.equalsIgnoreCase("MINUTELY"))
            return true;
        if (freq.equalsIgnoreCase("HOURLY"))
            return true;
        if (freq.equalsIgnoreCase("DAILY"))
            return true;
        if (freq.equalsIgnoreCase("WEEKLY"))
            return true;
        if (freq.equalsIgnoreCase("MONTHLY"))
            return true;
        if (freq.equalsIgnoreCase("YEARLY"))
            return true;
        return false;
    }

    /**
     * Gets the end time of the recurrence rule or 0 if none.
     *@return long The timestamp of the end time for this rule or 0 for none.
     */
    public long getEndTime() {
        if (rule == null) {
            Debug.logVerbose("Rule is null.", module);
            return -1;
        }
        long time = 0;
        java.sql.Timestamp stamp = null;

        stamp = rule.getTimestamp("untilDateTime");
        Debug.logVerbose("Stamp value: " + stamp, module);

        if (stamp != null) {
            long nanos = stamp.getNanos();
            time = stamp.getTime();
            time += (nanos / 1000000);
        }
        Debug.logVerbose("Returning time: " + time, module);
        return time;
    }

    /**
     * Get the number of times this recurrence will run (-1 until end time).
     *@return long The number of time this recurrence will run.
     */
    public long getCount() {
        if (rule.get("countNumber") != null)
            return rule.getLong("countNumber").longValue();
        return 0;
    }

    /**
     * Returns the frequency name of the recurrence.
     *@return String The name of this frequency.
     */
    public String getFrequencyName() {
        return rule.getString("frequency").toUpperCase();
    }

    /**
     * Returns the frequency of this recurrence.
     *@return int The reference value for the frequency
     */
    public int getFrequency() {
        String freq = rule.getString("frequency");

        if (freq == null)
            return 0;
        if (freq.equalsIgnoreCase("SECONDLY"))
            return SECONDLY;
        if (freq.equalsIgnoreCase("MINUTELY"))
            return MINUTELY;
        if (freq.equalsIgnoreCase("HOURLY"))
            return HOURLY;
        if (freq.equalsIgnoreCase("DAILY"))
            return DAILY;
        if (freq.equalsIgnoreCase("WEEKLY"))
            return WEEKLY;
        if (freq.equalsIgnoreCase("MONTHLY"))
            return MONTHLY;
        if (freq.equalsIgnoreCase("YEARLY"))
            return YEARLY;
        return 0;
    }

    /**
     * Returns the interval of the frequency.
     *@return long Interval value
     */
    public long getInterval() {
        if (rule.get("intervalNumber") == null)
            return 1;
        return rule.getLong("intervalNumber").longValue();
    }

    /**
     * Returns the interval of the frequency as an int.
     *@return The interval of this frequency as an integer.
     */
    public int getIntervalInt() {
        // if (Debug.verboseOn()) Debug.logVerbose("[RecurrenceInfo.getInterval] : " + getInterval(), module);
        return (int) getInterval();
    }

    /**
     * Returns the next recurrence of this rule.
     *@param startTime The time this recurrence first began.
     *@param fromTime The time to base the next recurrence on.
     *@param currentCount The total number of times the recurrence has run.
     *@return long The next recurrence as a long.
     */
    public long next(long startTime, long fromTime, long currentCount) {
        // Set up the values
        if (startTime == 0)
            startTime = RecurrenceUtil.now();
        if (fromTime == 0)
            fromTime = startTime;

        // Test the end time of the recurrence.
        if (getEndTime() != 0 && getEndTime() <= RecurrenceUtil.now())
            return 0;
        Debug.logVerbose("Rule NOT expired by end time.", module);

        // Test the recurrence limit.
        if (getCount() != -1 && currentCount >= getCount())
            return 0;
        Debug.logVerbose("Rule NOT expired by max count.", module);

        boolean isSeeking = true;
        long nextRuntime = 0;
        long seekTime = fromTime;
        int loopProtection = 0;
        int maxLoop = (10 * 10 * 10 * 10 * 10);

        while (isSeeking && loopProtection < maxLoop) {
            Date nextRun = getNextFreq(startTime, seekTime);
            seekTime = nextRun.getTime();
            if (validByRule(nextRun)) {
                isSeeking = false;
                nextRuntime = nextRun.getTime();
            }
            loopProtection++;
        }
        return nextRuntime;
    }

    /**
     * Gets the current recurrence (current for the checkTime) of this rule and returns it if it is valid.
     * If the current recurrence is not valid, doesn't try to find a valid one, instead returns 0.
     *@param startTime The time this recurrence first began.
     *@param checkTime The time to base the current recurrence on.
     *@param currentCount The total number of times the recurrence has run.
     *@return long The current recurrence as long if valid. If next recurrence is not valid, returns 0.
     */
    public long validCurrent(long startTime, long checkTime, long currentCount) {
        if (startTime == 0) {
            startTime = RecurrenceUtil.now();
        }
        if (checkTime == 0) {
            checkTime = startTime;
        }

        // Test the end time of the recurrence.
        if (getEndTime() != 0 && getEndTime() <= RecurrenceUtil.now()) {
            return 0;
        }

        // Test the recurrence limit.
        if (getCount() != -1 && currentCount >= getCount()) {
            return 0;
        }

        // Get the next frequency from checkTime
        Date nextRun = getNextFreq(startTime, checkTime);
        Calendar cal = Calendar.getInstance();
        Calendar checkTimeCal = Calendar.getInstance();
        cal.setTime(nextRun);
        checkTimeCal.setTime(new Date(checkTime));

        // Get previous frequency and update its values from checkTime
        switch (getFrequency()) {
        case YEARLY:
            cal.add(Calendar.YEAR, -getIntervalInt());
            if (cal.get(Calendar.YEAR) != checkTimeCal.get(Calendar.YEAR)) {
                return 0;
            }

        case MONTHLY:
            if (MONTHLY == getFrequency()) {
                cal.add(Calendar.MONTH, -getIntervalInt());
                if (cal.get(Calendar.MONTH) != checkTimeCal.get(Calendar.MONTH)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.MONTH, checkTimeCal.get(Calendar.MONTH));
            }

        case WEEKLY:
            if (WEEKLY == getFrequency()) {
                cal.add(Calendar.WEEK_OF_YEAR, -getIntervalInt());
                if (cal.get(Calendar.WEEK_OF_YEAR) != checkTimeCal.get(Calendar.WEEK_OF_YEAR)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.WEEK_OF_YEAR, checkTimeCal.get(Calendar.WEEK_OF_YEAR));
            }

        case DAILY:
            if (DAILY == getFrequency()) {
                cal.add(Calendar.DAY_OF_MONTH, -getIntervalInt());
                if (cal.get(Calendar.DAY_OF_MONTH) != checkTimeCal.get(Calendar.DAY_OF_MONTH)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.DAY_OF_MONTH, checkTimeCal.get(Calendar.DAY_OF_MONTH));
            }

        case HOURLY:
            if (HOURLY == getFrequency()) {
                cal.add(Calendar.HOUR_OF_DAY, -getIntervalInt());
                if (cal.get(Calendar.HOUR_OF_DAY) != checkTimeCal.get(Calendar.HOUR_OF_DAY)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, checkTimeCal.get(Calendar.HOUR_OF_DAY));
            }

        case MINUTELY:
            if (MINUTELY == getFrequency()) {
                cal.add(Calendar.MINUTE, -getIntervalInt());
                if (cal.get(Calendar.MINUTE) != checkTimeCal.get(Calendar.MINUTE)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.MINUTE, checkTimeCal.get(Calendar.MINUTE));
            }

        case SECONDLY:
            if (SECONDLY == getFrequency()) {
                cal.add(Calendar.SECOND, -getIntervalInt());
                if (cal.get(Calendar.SECOND) != checkTimeCal.get(Calendar.SECOND)) {
                    return 0;
                }
            } else {
                cal.set(Calendar.SECOND, checkTimeCal.get(Calendar.SECOND));
            }
        }

        // Check for validity of the current frequency.
        if (validByRule(cal.getTime())) {
             return cal.getTime().getTime();
        }

        return 0;
    }

    /**
     * Tests the date to see if it falls within the rules
     *@param startDate date object to test
     *@return True if the date is within the rules
     */
    public boolean isValid(Date startDate, Date date) {
        return isValid(startDate.getTime(), date.getTime());
    }

    /**
     * Tests the date to see if it falls within the rules
     *@param startTime date object to test
     *@return True if the date is within the rules
     */
    public boolean isValid(long startTime, long dateTime) {
        long testTime = startTime;

        if (testTime == dateTime)
            return true;
        while (testTime < dateTime) {
            testTime = next(startTime, testTime, 1);
            if (testTime == dateTime)
                return true;
        }
        return false;
    }

    /**
     * Removes this rule from the persistant store.
     *@throws RecurrenceRuleException
     */
    public void remove() throws RecurrenceRuleException {
        try {
            rule.remove();
        } catch (GenericEntityException e) {
            throw new RecurrenceRuleException(e.getMessage(), e);
        }
    }

    // Gets the next frequency/interval recurrence from specified time
    private Date getNextFreq(long startTime, long fromTime) {
        // Build a Calendar object
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date(startTime));

        long nextStartTime = startTime;

        while (nextStartTime < fromTime) {
            // if (Debug.verboseOn()) Debug.logVerbose("[RecurrenceInfo.getNextFreq] : Updating time - " + getFrequency(), module);
            switch (getFrequency()) {
            case SECONDLY:
                cal.add(Calendar.SECOND, getIntervalInt());
                break;

            case MINUTELY:
                cal.add(Calendar.MINUTE, getIntervalInt());
                break;

            case HOURLY:
                cal.add(Calendar.HOUR_OF_DAY, getIntervalInt());
                break;

            case DAILY:
                cal.add(Calendar.DAY_OF_MONTH, getIntervalInt());
                break;

            case WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, getIntervalInt());
                break;

            case MONTHLY:
                cal.add(Calendar.MONTH, getIntervalInt());
                break;

            case YEARLY:
                cal.add(Calendar.YEAR, getIntervalInt());
                break;

            default:
                return null; // should never happen
            }
            nextStartTime = cal.getTime().getTime();
        }
        return new Date(nextStartTime);
    }

    // Checks to see if a date is valid by the byXXX rules
    private boolean validByRule(Date date) {
        // Build a Calendar object
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);

        // Test each byXXX rule.
        if (UtilValidate.isNotEmpty(bySecondList)) {
            if (!bySecondList.contains(String.valueOf(cal.get(Calendar.SECOND))))
                return false;
        }
        if (UtilValidate.isNotEmpty(byMinuteList)) {
            if (!byMinuteList.contains(String.valueOf(cal.get(Calendar.MINUTE))))
                return false;
        }
        if (UtilValidate.isNotEmpty(byHourList)) {
            if (!byHourList.contains(String.valueOf(cal.get(Calendar.HOUR_OF_DAY))))
                return false;
        }
        if (UtilValidate.isNotEmpty(byDayList)) {
            Iterator<String> iter = byDayList.iterator();
            boolean foundDay = false;

            while (iter.hasNext() && !foundDay) {
                String dayRule = iter.next();
                String dayString = getDailyString(dayRule);

                if (cal.get(Calendar.DAY_OF_WEEK) == getCalendarDay(dayString)) {
                    if ((hasNumber(dayRule)) && (getFrequency() == MONTHLY || getFrequency() == YEARLY)) {
                        int modifier = getDailyNumber(dayRule);

                        if (modifier == 0)
                            foundDay = true;

                        if (getFrequency() == MONTHLY) {
                            // figure if we are the nth xDAY if this month
                            int currentPos = cal.get(Calendar.WEEK_OF_MONTH);
                            int dayPosCalc = cal.get(Calendar.DAY_OF_MONTH) - ((currentPos - 1) * 7);

                            if (dayPosCalc < 1)
                                currentPos--;
                            if (modifier > 0) {
                                if (currentPos == modifier) {
                                    foundDay = true;
                                }
                            } else if (modifier < 0) {
                                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                                int firstDay = dayPosCalc > 0 ? dayPosCalc : dayPosCalc + 7;
                                int totalDay = ((maxDay - firstDay) / 7) + 1;
                                int thisDiff = (currentPos - totalDay) - 1;

                                if (thisDiff == modifier) {
                                    foundDay = true;
                                }
                            }
                        } else if (getFrequency() == YEARLY) {
                            // figure if we are the nth xDAY if this year
                            int currentPos = cal.get(Calendar.WEEK_OF_YEAR);
                            int dayPosCalc = cal.get(Calendar.DAY_OF_YEAR) - ((currentPos - 1) * 7);

                            if (dayPosCalc < 1) {
                                currentPos--;
                            }
                            if (modifier > 0) {
                                if (currentPos == modifier) {
                                    foundDay = true;
                                }
                            } else if (modifier < 0) {
                                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                                int firstDay = dayPosCalc > 0 ? dayPosCalc : dayPosCalc + 7;
                                int totalDay = ((maxDay - firstDay) / 7) + 1;
                                int thisDiff = (currentPos - totalDay) - 1;

                                if (thisDiff == modifier) {
                                    foundDay = true;
                                }
                            }
                        }
                    } else {
                        // we are a DOW only rule
                        foundDay = true;
                    }
                }
            }
            if (!foundDay) {
                return false;
            }
        }
        if (UtilValidate.isNotEmpty(byMonthDayList)) {
            Iterator<String> iter = byMonthDayList.iterator();
            boolean foundDay = false;

            while (iter.hasNext() && !foundDay) {
                int day = 0;
                String dayStr = iter.next();

                try {
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, "Error parsing day string " + dayStr + ": " + nfe.toString(), module);
                }
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);

                if (day > 0 && day == currentDay) {
                    foundDay = true;
                }
                if (day < 0 && day == ((currentDay - maxDay) - 1)) {
                    foundDay = true;
                }
            }
            if (!foundDay) {
                return false;
            }
        }
        if (UtilValidate.isNotEmpty(byYearDayList)) {
            Iterator<String> iter = byYearDayList.iterator();
            boolean foundDay = false;

            while (iter.hasNext() && !foundDay) {
                int day = 0;
                String dayStr = iter.next();

                try {
                    day = Integer.parseInt(dayStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, "Error parsing day string " + dayStr + ": " + nfe.toString(), module);
                }
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                int currentDay = cal.get(Calendar.DAY_OF_YEAR);

                if (day > 0 && day == currentDay)
                    foundDay = true;
                if (day < 0 && day == ((currentDay - maxDay) - 1))
                    foundDay = true;
            }
            if (!foundDay)
                return false;
        }
        if (UtilValidate.isNotEmpty(byWeekNoList)) {
            Iterator<String> iter = byWeekNoList.iterator();
            boolean foundWeek = false;

            while (iter.hasNext() && !foundWeek) {
                int week = 0;
                String weekStr = iter.next();

                try {
                    week = Integer.parseInt(weekStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, "Error parsing week string " + weekStr + ": " + nfe.toString(), module);
                }
                int maxWeek = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
                int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);

                if (week > 0 && week == currentWeek)
                    foundWeek = true;
                if (week < 0 && week == ((currentWeek - maxWeek) - 1))
                    foundWeek = true;
            }
            if (!foundWeek)
                return false;
        }
        if (UtilValidate.isNotEmpty(byMonthList)) {
            Iterator<String> iter = byMonthList.iterator();
            boolean foundMonth = false;

            while (iter.hasNext() && !foundMonth) {
                int month = 0;
                String monthStr = iter.next();

                try {
                    month = Integer.parseInt(monthStr);
                } catch (NumberFormatException nfe) {
                    Debug.logError(nfe, "Error parsing month string " + monthStr + ": " + nfe.toString(), module);
                }
                if (month == cal.get(Calendar.MONTH)) {
                    foundMonth = true;
                }
            }
            if (!foundMonth)
                return false;
        }

        return true;
    }

    // Tests a string for the contents of a number at the beginning
    private boolean hasNumber(String str) {
        String list[] = {"+", "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        List<String> numberList = Arrays.asList(list);
        String firstChar = str.substring(0, 1);

        if (numberList.contains(firstChar))
            return true;
        return false;
    }

    // Gets the numeric value of the number at the beginning of the string
    private int getDailyNumber(String str) {
        int number = 0;
        StringBuilder numberBuf = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            String thisChar = str.substring(i, i);

            if (hasNumber(thisChar))
                numberBuf.append(thisChar);
        }
        String numberStr = numberBuf.toString();

        if (numberStr.length() > 0 && (numberStr.length() > 1 ||
                (numberStr.charAt(0) != '+' && numberStr.charAt(0) != '-'))) {
            try {
                number = Integer.parseInt(numberStr);
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Error parsing daily number string " + numberStr + ": " + nfe.toString(), module);
            }
        }
        return number;
    }

    // Gets the string part of the combined number+string
    private String getDailyString(String str) {
        StringBuilder sBuf = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            String thisChar = str.substring(i, i+1);

            if (!hasNumber(thisChar)) {
                sBuf.append(thisChar);
            }
        }
        return sBuf.toString();
    }

    // Returns the Calendar day of the rule day string
    private int getCalendarDay(String day) {
        if (day != null) day = day.trim();
        if (day.equalsIgnoreCase("MO"))
            return Calendar.MONDAY;
        if (day.equalsIgnoreCase("TU"))
            return Calendar.TUESDAY;
        if (day.equalsIgnoreCase("WE"))
            return Calendar.WEDNESDAY;
        if (day.equalsIgnoreCase("TH"))
            return Calendar.THURSDAY;
        if (day.equalsIgnoreCase("FR"))
            return Calendar.FRIDAY;
        if (day.equalsIgnoreCase("SA"))
            return Calendar.SATURDAY;
        if (day.equalsIgnoreCase("SU"))
            return Calendar.SUNDAY;
        return 0;
    }

    public String primaryKey() {
        return rule.getString("recurrenceRuleId");
    }

    public static RecurrenceRule makeRule(Delegator delegator, int frequency, int interval, int count)
            throws RecurrenceRuleException {
        return makeRule(delegator, frequency, interval, count, 0);
    }

    public static RecurrenceRule makeRule(Delegator delegator, int frequency, int interval, long endTime)
            throws RecurrenceRuleException {
        return makeRule(delegator, frequency, interval, -1, endTime);
    }

    public static RecurrenceRule makeRule(Delegator delegator, int frequency, int interval, int count, long endTime)
            throws RecurrenceRuleException {
        String freq[] = {"", "SECONDLY", "MINUTELY", "HOURLY", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"};

        if (frequency < 1 || frequency > 7)
            throw new RecurrenceRuleException("Invalid frequency");
        if (interval < 0)
            throw new RecurrenceRuleException("Invalid interval");

        String freqStr = freq[frequency];

        try {
            GenericValue value = delegator.makeValue("RecurrenceRule");

            value.set("frequency", freqStr);
            value.set("intervalNumber", Long.valueOf(interval));
            value.set("countNumber", Long.valueOf(count));
            if (endTime > 0) {
                value.set("untilDateTime", new java.sql.Timestamp(endTime));
            }
            delegator.createSetNextSeqId(value);
            RecurrenceRule newRule = new RecurrenceRule(value);
            return newRule;
        } catch (GenericEntityException ee) {
            throw new RecurrenceRuleException(ee.getMessage(), ee);
        } catch (RecurrenceRuleException re) {
            throw re;
        }
    }
}
