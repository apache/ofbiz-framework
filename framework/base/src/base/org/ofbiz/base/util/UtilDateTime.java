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
package org.ofbiz.base.util;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for handling java.util.Date, the java.sql data/time classes and related
 */
public class UtilDateTime {
    public static final String[] months = {// // to be translated over CommonMonthName, see example in accounting
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November",
        "December"
    };

    public static final String[] days = {// to be translated over CommonDayName, see example in accounting
        "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday", "Sunday"
    };

    public static final String[][] timevals = {
        {"1000", "millisecond"},
        {"60", "second"},
        {"60", "minute"},
        {"24", "hour"},
        {"168", "week"}
    };

    public static final DecimalFormat df = new DecimalFormat("0.00;-0.00");

    public static double getInterval(Date from, Date thru) {
        return thru != null ? thru.getTime() - from.getTime() : 0;
    }

    public static double getInterval(Timestamp from, Timestamp thru) {
        return thru != null ? thru.getTime() - from.getTime() + (thru.getNanos() - from.getNanos()) / 1000000 : 0;
    }

    public static String formatInterval(Date from, Date thru, int count, Locale locale) {
        return formatInterval(getInterval(from, thru), count, locale);
    }

    public static String formatInterval(Date from, Date thru, Locale locale) {
        return formatInterval(from, thru, 2, locale);
    }

    public static String formatInterval(Timestamp from, Timestamp thru, int count, Locale locale) {
        return formatInterval(getInterval(from, thru), count, locale);
    }

    public static String formatInterval(Timestamp from, Timestamp thru, Locale locale) {
        return formatInterval(from, thru, 2, locale);
    }

    public static String formatInterval(long interval, int count, Locale locale) {
        return formatInterval((double) interval, count, locale);
    }

    public static String formatInterval(long interval, Locale locale) {
        return formatInterval(interval, 2, locale);
    }

    public static String formatInterval(double interval, Locale locale) {
        return formatInterval(interval, 2, locale);
    }

    public static String formatInterval(double interval, int count, Locale locale) {
        ArrayList parts = new ArrayList(timevals.length);
        for (int i = 0; i < timevals.length; i++) {
            int value = Integer.parseInt(timevals[i][0]);
            double remainder = interval % value;
            interval = interval / value;
            parts.add(new Double(remainder));
        }

        Map uiDateTimeMap = UtilProperties.getResourceBundleMap("DateTimeLabels", locale);

        StringBuffer sb = new StringBuffer();
        for (int i = parts.size() - 1; i >= 0 && count > 0; i--) {
            if (sb.length() > 0) sb.append(", ");
            Double D = (Double) parts.get(i);
            double d = D.doubleValue();
            if (d < 1) continue;
            count--;
            sb.append(count == 0 ? df.format(d) : Integer.toString(D.intValue()));
            sb.append(' ');
            String label;
            if (D.intValue() == 1) {
                label = (String) uiDateTimeMap.get(timevals[i][1] + ".singular");
            } else {
                label = (String) uiDateTimeMap.get(timevals[i][1] + ".plural");
            }
            sb.append(label);
        }
        return sb.toString();
    }

    /**
     * Return a Timestamp for right now
     *
     * @return Timestamp for right now
     */
    public static java.sql.Timestamp nowTimestamp() {
        return getTimestamp(System.currentTimeMillis());
    }

    public static java.sql.Timestamp getTimestamp(long time) {
        return new java.sql.Timestamp(time);
    }

    /**
     * Returns currentTimeMillis as String
     * 
     * @return String(currentTimeMillis)
     */
    public static String nowAsString() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Return a string formatted as yyyyMMddHHmmss
     *
     * @return String formatted for right now
     */
    public static String nowDateString() {
        return nowDateString("yyyyMMddHHmmss");
    }

    /**
     * Return a string formatted as format
     *
     * @return String formatted for right now
     */
    public static String nowDateString(String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }

    /**
     * Return a Date for right now
     *
     * @return Date for right now
     */
    public static java.util.Date nowDate() {
        return new java.util.Date();
    }

    public static java.sql.Timestamp getDayStart(java.sql.Timestamp stamp) {
        return getDayStart(stamp, 0);
    }

    public static java.sql.Timestamp getDayStart(java.sql.Timestamp stamp, int daysLater) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static java.sql.Timestamp getNextDayStart(java.sql.Timestamp stamp) {
        return getDayStart(stamp, 1);
    }

    public static java.sql.Timestamp getDayEnd(java.sql.Timestamp stamp) {
        return getDayEnd(stamp, 0);
    }

    public static java.sql.Timestamp getDayEnd(java.sql.Timestamp stamp, int daysLater) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(999999999);
        return retStamp;
    }

    /**
     * Return the date for the first day of the year
     *
     * @param stamp
     * @return java.sql.Timestamp
     */
    public static java.sql.Timestamp getYearStart(java.sql.Timestamp stamp) {
        return getYearStart(stamp, 0, 0, 0);
    }

    public static java.sql.Timestamp getYearStart(java.sql.Timestamp stamp, int daysLater) {
        return getYearStart(stamp, daysLater, 0, 0);
    }

    public static java.sql.Timestamp getYearStart(java.sql.Timestamp stamp, int daysLater, int yearsLater) {
        return getYearStart(stamp, daysLater, 0, yearsLater);
    }
    public static java.sql.Timestamp getYearStart(java.sql.Timestamp stamp, int daysLater, int monthsLater, int yearsLater) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
        tempCal.add(Calendar.YEAR, yearsLater);
        tempCal.add(Calendar.MONTH, monthsLater);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(0);
        return retStamp;
    }
    public static java.sql.Timestamp getYearStart(java.sql.Timestamp stamp, Number daysLater, Number monthsLater, Number yearsLater) {
        return getYearStart(stamp, (daysLater == null ? 0 : daysLater.intValue()), 
                (monthsLater == null ? 0 : monthsLater.intValue()), (yearsLater == null ? 0 : yearsLater.intValue()));
    }

    /**
     * Return the date for the first day of the month
     *
     * @param stamp
     * @return java.sql.Timestamp
     */
    public static java.sql.Timestamp getMonthStart(java.sql.Timestamp stamp) {
        return getMonthStart(stamp, 0, 0);
    }

    public static java.sql.Timestamp getMonthStart(java.sql.Timestamp stamp, int daysLater) {
        return getMonthStart(stamp, daysLater, 0);
    }

    public static java.sql.Timestamp getMonthStart(java.sql.Timestamp stamp, int daysLater, int monthsLater) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), 1, 0, 0, 0);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        tempCal.add(Calendar.MONTH, monthsLater);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(0);
        return retStamp;
    }

    /**
     * Return the date for the first day of the week
     *
     * @param stamp
     * @return java.sql.Timestamp
     */
    public static java.sql.Timestamp getWeekStart(java.sql.Timestamp stamp) {
        return getWeekStart(stamp, 0, 0);
    }

    public static java.sql.Timestamp getWeekStart(java.sql.Timestamp stamp, int daysLater) {
        return getWeekStart(stamp, daysLater, 0);
    }

    public static java.sql.Timestamp getWeekStart(java.sql.Timestamp stamp, int daysLater, int weeksLater) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        tempCal.add(Calendar.WEEK_OF_MONTH, weeksLater);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static java.sql.Timestamp getWeekEnd(java.sql.Timestamp stamp) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(new java.util.Date(stamp.getTime()));
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        tempCal.add(Calendar.WEEK_OF_MONTH, 1);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        tempCal.add(Calendar.SECOND, -1);
        java.sql.Timestamp retStamp = new java.sql.Timestamp(tempCal.getTime().getTime());
        retStamp.setNanos(0);
        return retStamp;
    }
    
    public static java.util.Calendar toCalendar(java.sql.Timestamp stamp) {
        Calendar cal = Calendar.getInstance();
        if (stamp != null) {
            cal.setTimeInMillis(stamp.getTime());
        }
        return cal;
    }

    /**
     * Converts a date String into a java.sql.Date
     *
     * @param date The date String: MM/DD/YYYY
     * @return A java.sql.Date made from the date String
     */
    public static java.sql.Date toSqlDate(String date) {
        java.util.Date newDate = toDate(date, "00:00:00");

        if (newDate != null) {
            return new java.sql.Date(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a java.sql.Date from separate Strings for month, day, year
     *
     * @param monthStr The month String
     * @param dayStr   The day String
     * @param yearStr  The year String
     * @return A java.sql.Date made from separate Strings for month, day, year
     */
    public static java.sql.Date toSqlDate(String monthStr, String dayStr, String yearStr) {
        java.util.Date newDate = toDate(monthStr, dayStr, yearStr, "0", "0", "0");

        if (newDate != null) {
            return new java.sql.Date(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a java.sql.Date from separate ints for month, day, year
     *
     * @param month The month int
     * @param day   The day int
     * @param year  The year int
     * @return A java.sql.Date made from separate ints for month, day, year
     */
    public static java.sql.Date toSqlDate(int month, int day, int year) {
        java.util.Date newDate = toDate(month, day, year, 0, 0, 0);

        if (newDate != null) {
            return new java.sql.Date(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Converts a time String into a java.sql.Time
     *
     * @param time The time String: either HH:MM or HH:MM:SS
     * @return A java.sql.Time made from the time String
     */
    public static java.sql.Time toSqlTime(String time) {
        java.util.Date newDate = toDate("1/1/1970", time);

        if (newDate != null) {
            return new java.sql.Time(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a java.sql.Time from separate Strings for hour, minute, and second.
     *
     * @param hourStr   The hour String
     * @param minuteStr The minute String
     * @param secondStr The second String
     * @return A java.sql.Time made from separate Strings for hour, minute, and second.
     */
    public static java.sql.Time toSqlTime(String hourStr, String minuteStr, String secondStr) {
        java.util.Date newDate = toDate("0", "0", "0", hourStr, minuteStr, secondStr);

        if (newDate != null) {
            return new java.sql.Time(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a java.sql.Time from separate ints for hour, minute, and second.
     *
     * @param hour   The hour int
     * @param minute The minute int
     * @param second The second int
     * @return A java.sql.Time made from separate ints for hour, minute, and second.
     */
    public static java.sql.Time toSqlTime(int hour, int minute, int second) {
        java.util.Date newDate = toDate(0, 0, 0, hour, minute, second);

        if (newDate != null) {
            return new java.sql.Time(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Converts a date and time String into a Timestamp
     *
     * @param dateTime A combined data and time string in the format "MM/DD/YYYY HH:MM:SS", the seconds are optional
     * @return The corresponding Timestamp
     */
    public static java.sql.Timestamp toTimestamp(String dateTime) {
        java.util.Date newDate = toDate(dateTime);

        if (newDate != null) {
            return new java.sql.Timestamp(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Converts a date String and a time String into a Timestamp
     *
     * @param date The date String: MM/DD/YYYY
     * @param time The time String: either HH:MM or HH:MM:SS
     * @return A Timestamp made from the date and time Strings
     */
    public static java.sql.Timestamp toTimestamp(String date, String time) {
        java.util.Date newDate = toDate(date, time);

        if (newDate != null) {
            return new java.sql.Timestamp(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a Timestamp from separate Strings for month, day, year, hour, minute, and second.
     *
     * @param monthStr  The month String
     * @param dayStr    The day String
     * @param yearStr   The year String
     * @param hourStr   The hour String
     * @param minuteStr The minute String
     * @param secondStr The second String
     * @return A Timestamp made from separate Strings for month, day, year, hour, minute, and second.
     */
    public static java.sql.Timestamp toTimestamp(String monthStr, String dayStr, String yearStr, String hourStr,
            String minuteStr, String secondStr) {
        java.util.Date newDate = toDate(monthStr, dayStr, yearStr, hourStr, minuteStr, secondStr);

        if (newDate != null) {
            return new java.sql.Timestamp(newDate.getTime());
        } else {
            return null;
        }
    }

    /**
     * Makes a Timestamp from separate ints for month, day, year, hour, minute, and second.
     *
     * @param month  The month int
     * @param day    The day int
     * @param year   The year int
     * @param hour   The hour int
     * @param minute The minute int
     * @param second The second int
     * @return A Timestamp made from separate ints for month, day, year, hour, minute, and second.
     */
    public static java.sql.Timestamp toTimestamp(int month, int day, int year, int hour, int minute, int second) {
        java.util.Date newDate = toDate(month, day, year, hour, minute, second);

        if (newDate != null) {
            return new java.sql.Timestamp(newDate.getTime());
        } else {
            return null;
        }
    }

    public static java.sql.Timestamp toTimestamp(Date date) {
        if (date == null) return null;
        return new Timestamp(date.getTime());
    }

    /**
     * Converts a date and time String into a Date
     *
     * @param dateTime A combined data and time string in the format "MM/DD/YYYY HH:MM:SS", the seconds are optional
     * @return The corresponding Date
     */
    public static java.util.Date toDate(String dateTime) {
        if (dateTime == null) {
            return null;
        }
        // dateTime must have one space between the date and time...
        String date = dateTime.substring(0, dateTime.indexOf(" "));
        String time = dateTime.substring(dateTime.indexOf(" ") + 1);

        return toDate(date, time);
    }

    /**
     * Converts a date String and a time String into a Date
     *
     * @param date The date String: MM/DD/YYYY
     * @param time The time String: either HH:MM or HH:MM:SS
     * @return A Date made from the date and time Strings
     */
    public static java.util.Date toDate(String date, String time) {
        if (date == null || time == null) return null;
        String month;
        String day;
        String year;
        String hour;
        String minute;
        String second;

        int dateSlash1 = date.indexOf("/");
        int dateSlash2 = date.lastIndexOf("/");

        if (dateSlash1 <= 0 || dateSlash1 == dateSlash2) return null;
        int timeColon1 = time.indexOf(":");
        int timeColon2 = time.lastIndexOf(":");

        if (timeColon1 <= 0) return null;
        month = date.substring(0, dateSlash1);
        day = date.substring(dateSlash1 + 1, dateSlash2);
        year = date.substring(dateSlash2 + 1);
        hour = time.substring(0, timeColon1);

        if (timeColon1 == timeColon2) {
            minute = time.substring(timeColon1 + 1);
            second = "0";
        } else {
            minute = time.substring(timeColon1 + 1, timeColon2);
            second = time.substring(timeColon2 + 1);
        }

        return toDate(month, day, year, hour, minute, second);
    }

    /**
     * Makes a Date from separate Strings for month, day, year, hour, minute, and second.
     *
     * @param monthStr  The month String
     * @param dayStr    The day String
     * @param yearStr   The year String
     * @param hourStr   The hour String
     * @param minuteStr The minute String
     * @param secondStr The second String
     * @return A Date made from separate Strings for month, day, year, hour, minute, and second.
     */
    public static java.util.Date toDate(String monthStr, String dayStr, String yearStr, String hourStr,
            String minuteStr, String secondStr) {
        int month, day, year, hour, minute, second;

        try {
            month = Integer.parseInt(monthStr);
            day = Integer.parseInt(dayStr);
            year = Integer.parseInt(yearStr);
            hour = Integer.parseInt(hourStr);
            minute = Integer.parseInt(minuteStr);
            second = Integer.parseInt(secondStr);
        } catch (Exception e) {
            return null;
        }
        return toDate(month, day, year, hour, minute, second);
    }

    /**
     * Makes a Date from separate ints for month, day, year, hour, minute, and second.
     *
     * @param month  The month int
     * @param day    The day int
     * @param year   The year int
     * @param hour   The hour int
     * @param minute The minute int
     * @param second The second int
     * @return A Date made from separate ints for month, day, year, hour, minute, and second.
     */
    public static java.util.Date toDate(int month, int day, int year, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.set(year, month - 1, day, hour, minute, second);
        } catch (Exception e) {
            return null;
        }
        return new java.util.Date(calendar.getTime().getTime());
    }

    /**
     * Makes a date String in the given from a Date
     *
     * @param date The Date
     * @return A date String in the given format
     */
    public static String toDateString(java.util.Date date, String format) {
        if (date == null) return "";
        SimpleDateFormat dateFormat = null;
        if (format != null) {
            dateFormat = new SimpleDateFormat(format);
        } else {
            dateFormat = new SimpleDateFormat();
        }

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        return dateFormat.format(date);
    }
    
    /**
     * Makes a date String in the format MM/DD/YYYY from a Date
     *
     * @param date The Date
     * @return A date String in the format MM/DD/YYYY
     */
    public static String toDateString(java.util.Date date) {
        return toDateString(date, "MM/dd/yyyy");
    }

    /**
     * Makes a time String in the format HH:MM:SS from a Date. If the seconds are 0, then the output is in HH:MM.
     *
     * @param date The Date
     * @return A time String in the format HH:MM:SS or HH:MM
     */
    public static String toTimeString(java.util.Date date) {
        if (date == null) return "";
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        return (toTimeString(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
    }

    /**
     * Makes a time String in the format HH:MM:SS from a separate ints for hour, minute, and second. If the seconds are 0, then the output is in HH:MM.
     *
     * @param hour   The hour int
     * @param minute The minute int
     * @param second The second int
     * @return A time String in the format HH:MM:SS or HH:MM
     */
    public static String toTimeString(int hour, int minute, int second) {
        String hourStr;
        String minuteStr;
        String secondStr;

        if (hour < 10) {
            hourStr = "0" + hour;
        } else {
            hourStr = "" + hour;
        }
        if (minute < 10) {
            minuteStr = "0" + minute;
        } else {
            minuteStr = "" + minute;
        }
        if (second < 10) {
            secondStr = "0" + second;
        } else {
            secondStr = "" + second;
        }
        if (second == 0) {
            return hourStr + ":" + minuteStr;
        } else {
            return hourStr + ":" + minuteStr + ":" + secondStr;
        }
    }

    /**
     * Makes a combined data and time string in the format "MM/DD/YYYY HH:MM:SS" from a Date. If the seconds are 0 they are left off.
     *
     * @param date The Date
     * @return A combined data and time string in the format "MM/DD/YYYY HH:MM:SS" where the seconds are left off if they are 0.
     */
    public static String toDateTimeString(java.util.Date date) {
        if (date == null) return "";
        String dateString = toDateString(date);
        String timeString = toTimeString(date);

        if (dateString != null && timeString != null) {
            return dateString + " " + timeString;
        } else {
            return "";
        }
    }

    /**
     * Makes a Timestamp for the beginning of the month
     *
     * @return A Timestamp of the beginning of the month
     */
    public static java.sql.Timestamp monthBegin() {
        Calendar mth = Calendar.getInstance();

        mth.set(Calendar.DAY_OF_MONTH, 1);
        mth.set(Calendar.HOUR_OF_DAY, 0);
        mth.set(Calendar.MINUTE, 0);
        mth.set(Calendar.SECOND, 0);
        mth.set(Calendar.MILLISECOND, 0);
        mth.set(Calendar.AM_PM, Calendar.AM);
        return new java.sql.Timestamp(mth.getTime().getTime());
    }

    /**
     * returns a week number in a year for a Timestamp input
     *
     * @param input Timestamp date
     * @return A int containing the week number
     */
    public static int weekNumber(Timestamp input) {
        Calendar calendar = Calendar.getInstance();
        return weekNumber(input, calendar.getFirstDayOfWeek());
    }
    
    public static int weekNumber(Timestamp input, int startOfWeek) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(startOfWeek);
        
        if(startOfWeek == Calendar.MONDAY) {
           calendar.setMinimalDaysInFirstWeek(4);
        } else if(startOfWeek == Calendar.SUNDAY) { 
           calendar.setMinimalDaysInFirstWeek(3);
        }
        
        calendar.setTime(new java.util.Date(input.getTime()));
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }
}
