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
package org.apache.ofbiz.base.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.ibm.icu.util.Calendar;

/**
 * Utility class for handling java.util.Date, the java.sql data/time classes and related
 */
public final class UtilDateTime {

    public static final String module = UtilDateTime.class.getName();

    private static final String[][] timevals = {
        {"1000", "millisecond"},
        {"60", "second"},
        {"60", "minute"},
        {"24", "hour"},
        {"168", "week"}
    };

    private static final DecimalFormat df = new DecimalFormat("0.00;-0.00");
    /**
     * JDBC escape format for java.sql.Date conversions.
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    /**
     * JDBC escape format for java.sql.Timestamp conversions.
     */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    /**
     * JDBC escape format for java.sql.Time conversions.
     */
    private static final String TIME_FORMAT = "HH:mm:ss";

    private UtilDateTime() {}

    public static double getInterval(Date from, Date thru) {
        return thru != null ? thru.getTime() - from.getTime() : 0;
    }

    public static int getIntervalInDays(Timestamp from, Timestamp thru) {
        return thru != null ? (int) ((thru.getTime() - from.getTime()) / (24*60*60*1000)) : 0;
    }

    public static Timestamp addDaysToTimestamp(Timestamp start, int days) {
        return new Timestamp(start.getTime() + (24L*60L*60L*1000L*days));
    }

    public static Timestamp addDaysToTimestamp(Timestamp start, Double days) {
        return new Timestamp(start.getTime() + ((int) (24L*60L*60L*1000L*days)));
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
        List<Double> parts = new ArrayList<>(timevals.length);
        for (String[] timeval: timevals) {
            int value = Integer.parseInt(timeval[0]);
            double remainder = interval % value;
            interval = interval / value;
            parts.add(remainder);
        }

        Map<String, Object> uiDateTimeMap = UtilProperties.getResourceBundleMap("DateTimeLabels", locale);

        StringBuilder sb = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0 && count > 0; i--) {
            Double D = parts.get(i);
            double d = D.doubleValue();
            if (d < 1) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            count--;
            sb.append(count == 0 ? df.format(d) : Integer.toString(D.intValue()));
            sb.append(' ');
            Object label;
            if (D.intValue() == 1) {
                label = uiDateTimeMap.get(timevals[i][1] + ".singular");
            } else {
                label = uiDateTimeMap.get(timevals[i][1] + ".plural");
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

    /**
     * Convert a millisecond value to a Timestamp.
     * @param time millsecond value
     * @return Timestamp
     */
    public static java.sql.Timestamp getTimestamp(long time) {
        return new java.sql.Timestamp(time);
    }

    /**
     * Convert a millisecond value to a Timestamp.
     * @param milliSecs millsecond value
     * @return Timestamp
     */
    public static Timestamp getTimestamp(String milliSecs) throws NumberFormatException {
        return new Timestamp(Long.parseLong(milliSecs));
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
        return getDayStart(stamp, daysLater, TimeZone.getDefault(), Locale.getDefault());
    }

    public static java.sql.Timestamp getNextDayStart(java.sql.Timestamp stamp) {
        return getDayStart(stamp, 1);
    }

    public static java.sql.Timestamp getDayEnd(java.sql.Timestamp stamp) {
        return getDayEnd(stamp, Long.valueOf(0));
    }

    public static java.sql.Timestamp getDayEnd(java.sql.Timestamp stamp, Long daysLater) {
        return getDayEnd(stamp, daysLater, TimeZone.getDefault(), Locale.getDefault());
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
        return getYearStart(stamp, daysLater, monthsLater, yearsLater, TimeZone.getDefault(), Locale.getDefault());
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
        return getMonthStart(stamp, daysLater, monthsLater, TimeZone.getDefault(), Locale.getDefault());
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
        return getWeekStart(stamp, daysLater, weeksLater, TimeZone.getDefault(), Locale.getDefault());
    }

    public static java.sql.Timestamp getWeekEnd(java.sql.Timestamp stamp) {
        return getWeekEnd(stamp, TimeZone.getDefault(), Locale.getDefault());
    }

    public static Calendar toCalendar(java.sql.Timestamp stamp) {
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
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
        }
        return null;
    }

    public static java.sql.Timestamp toTimestamp(Date date) {
        if (date == null) {
            return null;
        }
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
        if (date == null || time == null) {
            return null;
        }
        String month;
        String day;
        String year;
        String hour;
        String minute;
        String second;

        int dateSlash1 = date.indexOf("/");
        int dateSlash2 = date.lastIndexOf("/");

        if (dateSlash1 <= 0 || dateSlash1 == dateSlash2) {
            return null;
        }
        int timeColon1 = time.indexOf(":");
        int timeColon2 = time.lastIndexOf(":");

        if (timeColon1 <= 0) {
            return null;
        }
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
            Debug.logError(e, module);
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
            calendar.set(Calendar.MILLISECOND, 0);
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
        if (date == null) {
            return "";
        }
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
        if (date == null) {
            return "";
        }
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
        }
        return hourStr + ":" + minuteStr + ":" + secondStr;
    }

    /**
     * Makes a combined data and time string in the format "MM/DD/YYYY HH:MM:SS" from a Date. If the seconds are 0 they are left off.
     *
     * @param date The Date
     * @return A combined data and time string in the format "MM/DD/YYYY HH:MM:SS" where the seconds are left off if they are 0.
     */
    public static String toDateTimeString(java.util.Date date) {
        if (date == null) {
            return "";
        }
        String dateString = toDateString(date);
        String timeString = toTimeString(date);

        if (!dateString.isEmpty() && !timeString.isEmpty()) {
            return dateString + " " + timeString;
        }
        return "";
    }

    public static String toGmtTimestampString(Timestamp timestamp) {
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(timestamp);
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
        return weekNumber(input, TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * returns a day number in a week for a Timestamp input
     *
     * @param stamp Timestamp date
     * @return A int containing the day number (sunday = 1, saturday = 7)
     */
    public static int dayNumber(Timestamp stamp) {
        Calendar tempCal = toCalendar(stamp, TimeZone.getDefault(), Locale.getDefault());
        return tempCal.get(Calendar.DAY_OF_WEEK);
    }

    public static int weekNumber(Timestamp input, int startOfWeek) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(startOfWeek);

        if (startOfWeek == Calendar.MONDAY) {
           calendar.setMinimalDaysInFirstWeek(4);
        } else if (startOfWeek == Calendar.SUNDAY) {
           calendar.setMinimalDaysInFirstWeek(3);
        }

        calendar.setTime(new java.util.Date(input.getTime()));
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    // ----- New methods that take a timezone and locale -- //

    public static Calendar getCalendarInstance(TimeZone timeZone, Locale locale) {
        return Calendar.getInstance(com.ibm.icu.util.TimeZone.getTimeZone(timeZone.getID()), locale);
    }

    /**
     * Returns a Calendar object initialized to the specified date/time, time zone,
     * and locale.
     *
     * @param date date/time to use
     * @param timeZone
     * @param locale
     * @return Calendar object
     * @see java.util.Calendar
     */
    public static Calendar toCalendar(Date date, TimeZone timeZone, Locale locale) {
        Calendar cal = getCalendarInstance(timeZone, locale);
        if (date != null) {
            cal.setTime(date);
        }
        return cal;
    }

    /**
     * Perform date/time arithmetic on a Timestamp. This is the only accurate way to
     * perform date/time arithmetic across locales and time zones.
     *
     * @param stamp date/time to perform arithmetic on
     * @param adjType the adjustment type to perform. Use one of the java.util.Calendar fields.
     * @param adjQuantity the adjustment quantity.
     * @param timeZone
     * @param locale
     * @return adjusted Timestamp
     * @see java.util.Calendar
     */
    public static Timestamp adjustTimestamp(Timestamp stamp, int adjType, int adjQuantity, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.add(adjType, adjQuantity);
        return new Timestamp(tempCal.getTimeInMillis());
    }

    public static Timestamp adjustTimestamp(Timestamp stamp, Integer adjType, Integer adjQuantity) {
        Calendar tempCal = toCalendar(stamp);
        tempCal.add(adjType, adjQuantity);
        return new Timestamp(tempCal.getTimeInMillis());
    }

    public static Timestamp getDayStart(Timestamp stamp, TimeZone timeZone, Locale locale) {
        return getDayStart(stamp, 0, timeZone, locale);
    }

    public static Timestamp getDayStart(Timestamp stamp, int daysLater, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        Timestamp retStamp = new Timestamp(tempCal.getTimeInMillis());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static Timestamp getDayEnd(Timestamp stamp, TimeZone timeZone, Locale locale) {
        return getDayEnd(stamp, Long.valueOf(0), timeZone, locale);
    }

    public static Timestamp getDayEnd(Timestamp stamp, Long daysLater, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater.intValue());
        Timestamp retStamp = new Timestamp(tempCal.getTimeInMillis());
        retStamp.setNanos(0);
        //MSSQL datetime field has accuracy of 3 milliseconds and setting the nano seconds cause the date to be rounded to next day
        return retStamp;
    }

    public static Timestamp getWeekStart(Timestamp stamp, TimeZone timeZone, Locale locale) {
        return getWeekStart(stamp, 0, 0, timeZone, locale);
    }

    public static Timestamp getWeekStart(Timestamp stamp, int daysLater, TimeZone timeZone, Locale locale) {
        return getWeekStart(stamp, daysLater, 0, timeZone, locale);
    }

    public static Timestamp getWeekStart(Timestamp stamp, int daysLater, int weeksLater, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        tempCal.add(Calendar.WEEK_OF_MONTH, weeksLater);
        Timestamp retStamp = new Timestamp(tempCal.getTimeInMillis());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static Timestamp getWeekEnd(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Timestamp weekStart = getWeekStart(stamp, timeZone, locale);
        Calendar tempCal = toCalendar(weekStart, timeZone, locale);
        tempCal.add(Calendar.DAY_OF_MONTH, 6);
        return getDayEnd(new Timestamp(tempCal.getTimeInMillis()), timeZone, locale);
    }

    public static Timestamp getMonthStart(Timestamp stamp, TimeZone timeZone, Locale locale) {
        return getMonthStart(stamp, 0, 0, timeZone, locale);
    }

    public static Timestamp getMonthStart(Timestamp stamp, int daysLater, TimeZone timeZone, Locale locale) {
        return getMonthStart(stamp, daysLater, 0, timeZone, locale);
    }

    public static Timestamp getMonthStart(Timestamp stamp, int daysLater, int monthsLater, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), 1, 0, 0, 0);
        tempCal.add(Calendar.MONTH, monthsLater);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        Timestamp retStamp = new Timestamp(tempCal.getTimeInMillis());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static Timestamp getMonthEnd(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.getActualMaximum(Calendar.DAY_OF_MONTH), 0, 0, 0);
        return getDayEnd(new Timestamp(tempCal.getTimeInMillis()), timeZone, locale);
    }

    public static Timestamp getYearStart(Timestamp stamp, TimeZone timeZone, Locale locale) {
        return getYearStart(stamp, 0, 0, 0, timeZone, locale);
    }

    public static Timestamp getYearStart(Timestamp stamp, int daysLater, TimeZone timeZone, Locale locale) {
        return getYearStart(stamp, daysLater, 0, 0, timeZone, locale);
    }

    public static Timestamp getYearStart(Timestamp stamp, int daysLater, int yearsLater, TimeZone timeZone, Locale locale) {
        return getYearStart(stamp, daysLater, 0, yearsLater, timeZone, locale);
    }

    public static Timestamp getYearStart(Timestamp stamp, Number daysLater, Number monthsLater, Number yearsLater, TimeZone timeZone, Locale locale) {
        return getYearStart(stamp, (daysLater == null ? 0 : daysLater.intValue()),
                (monthsLater == null ? 0 : monthsLater.intValue()), (yearsLater == null ? 0 : yearsLater.intValue()), timeZone, locale);
    }

    public static Timestamp getYearStart(Timestamp stamp, int daysLater, int monthsLater, int yearsLater, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
        tempCal.add(Calendar.YEAR, yearsLater);
        tempCal.add(Calendar.MONTH, monthsLater);
        tempCal.add(Calendar.DAY_OF_MONTH, daysLater);
        Timestamp retStamp = new Timestamp(tempCal.getTimeInMillis());
        retStamp.setNanos(0);
        return retStamp;
    }

    public static Timestamp getYearEnd(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        tempCal.set(tempCal.get(Calendar.YEAR), tempCal.getActualMaximum(Calendar.MONTH) + 1, 0, 0, 0, 0);
        return getMonthEnd(new Timestamp(tempCal.getTimeInMillis()), timeZone, locale);
    }

    public static int weekNumber(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar tempCal = toCalendar(stamp, timeZone, locale);
        return tempCal.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * Returns a List of day name Strings - suitable for calendar headings.
     * @param locale
     * @return List of day name Strings
     */
    public static List<String> getDayNames(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        tempCal.set(Calendar.DAY_OF_WEEK, tempCal.getFirstDayOfWeek());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", locale);
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            resultList.add(dateFormat.format(tempCal.getTime()));
            tempCal.roll(Calendar.DAY_OF_WEEK, 1);
        }
        return resultList;
    }

    /**
     * Returns a List of month name Strings - suitable for calendar headings.
     *
     * @param locale
     * @return List of month name Strings
     */
    public static List<String> getMonthNames(Locale locale) {
        Calendar tempCal = Calendar.getInstance(locale);
        tempCal.set(Calendar.MONTH, Calendar.JANUARY);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM", locale);
        List<String> resultList = new ArrayList<>();
        for (int i = Calendar.JANUARY; i <= tempCal.getActualMaximum(Calendar.MONTH); i++) {
            resultList.add(dateFormat.format(tempCal.getTime()));
            tempCal.roll(Calendar.MONTH, 1);
        }
        return resultList;
    }

    /**
     * Returns an initialized DateFormat object.
     *
     * @param dateFormat
     *            optional format string
     * @param tz
     * @param locale
     *            can be null if dateFormat is not null
     * @return DateFormat object
     */
    public static DateFormat toDateFormat(String dateFormat, TimeZone tz, Locale locale) {
        DateFormat df = null;
        if (UtilValidate.isEmpty(dateFormat)) {
            df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        } else {
            df = new SimpleDateFormat(dateFormat, locale == null ? Locale.getDefault() : locale);
        }
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Returns an initialized DateFormat object.
     * @param dateTimeFormat optional format string
     * @param tz
     * @param locale can be null if dateTimeFormat is not null
     * @return DateFormat object
     */
    public static DateFormat toDateTimeFormat(String dateTimeFormat, TimeZone tz, Locale locale) {
        DateFormat df = null;
        if (UtilValidate.isEmpty(dateTimeFormat)) {
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        } else {
            df = new SimpleDateFormat(dateTimeFormat, locale == null ? Locale.getDefault() : locale);
        }
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Returns an initialized DateFormat object.
     * @param timeFormat optional format string
     * @param tz
     * @param locale can be null if timeFormat is not null
     * @return DateFormat object
     */
    public static DateFormat toTimeFormat(String timeFormat, TimeZone tz, Locale locale) {
        DateFormat df = null;
        if (UtilValidate.isEmpty(timeFormat)) {
            df = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
        } else {
            df = new SimpleDateFormat(timeFormat, locale == null ? Locale.getDefault() : locale);
        }
        df.setTimeZone(tz);
        return df;
    }

    /**
     * Localized String to Timestamp conversion. To be used in tandem with timeStampToString().
     */
    public static Timestamp stringToTimeStamp(String dateTimeString, TimeZone tz, Locale locale) throws ParseException {
        return stringToTimeStamp(dateTimeString, null, tz, locale);
    }

    /**
     * Localized String to Timestamp conversion. To be used in tandem with timeStampToString().
     */
    public static Timestamp stringToTimeStamp(String dateTimeString, String dateTimeFormat, TimeZone tz, Locale locale) throws ParseException {
        DateFormat dateFormat = toDateTimeFormat(dateTimeFormat, tz, locale);
        Date parsedDate = dateFormat.parse(dateTimeString);
        return new Timestamp(parsedDate.getTime());
    }

    /**
     * Localized Timestamp to String conversion. To be used in tandem with stringToTimeStamp().
     */
    public static String timeStampToString(Timestamp stamp, TimeZone tz, Locale locale) {
        return timeStampToString(stamp, null, tz, locale);
    }

    /**
     * Localized Timestamp to String conversion. To be used in tandem with stringToTimeStamp().
     */
    public static String timeStampToString(Timestamp stamp, String dateTimeFormat, TimeZone tz, Locale locale) {
        DateFormat dateFormat = toDateTimeFormat(dateTimeFormat, tz, locale);
        return dateFormat.format(stamp);
    }

    // Private lazy-initializer class
    private static class TimeZoneHolder {
        private static final List<TimeZone> availableTimeZoneList = getTimeZones();

        private static List<TimeZone> getTimeZones() {
            ArrayList<TimeZone> availableTimeZoneList = new ArrayList<>();
            List<String> idList = null;
            String tzString = UtilProperties.getPropertyValue("general", "timeZones.available");
            if (UtilValidate.isNotEmpty(tzString)) {
                idList = StringUtil.split(tzString, ",");
            } else {
                idList = Arrays.asList(TimeZone.getAvailableIDs());
            }
            for (String id : idList) {
                TimeZone curTz = TimeZone.getTimeZone(id);
                availableTimeZoneList.add(curTz);
            }
            availableTimeZoneList.trimToSize();
            return Collections.unmodifiableList(availableTimeZoneList);
        }

    }

    /** Returns a List of available TimeZone objects.
     * @see java.util.TimeZone
     */
    public static List<TimeZone> availableTimeZones() {
        return TimeZoneHolder.availableTimeZoneList;
    }

    /** Returns a TimeZone object based upon a time zone ID. Method defaults to
     * server's time zone if tzID is null or empty.
     * @see java.util.TimeZone
     */
    public static TimeZone toTimeZone(String tzId) {
        if (UtilValidate.isEmpty(tzId)) {
            return TimeZone.getDefault();
        }
        return TimeZone.getTimeZone(tzId);
    }

    /** Returns a TimeZone object based upon an hour offset from GMT.
     * @see java.util.TimeZone
     */
    public static TimeZone toTimeZone(int gmtOffset) {
        if (gmtOffset > 12 || gmtOffset < -14) {
            throw new IllegalArgumentException("Invalid GMT offset");
        }
        String tzId = gmtOffset > 0 ? "Etc/GMT+" : "Etc/GMT";
        return TimeZone.getTimeZone(tzId + gmtOffset);
    }

    public static int getSecond(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.SECOND);
    }

    public static int getMinute(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.MINUTE);
    }

    public static int getHour(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public static int getDayOfWeek(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public static int getDayOfMonth(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static int getDayOfYear(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    public static int getWeek(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static int getMonth(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.MONTH);
    }

    public static int getYear(Timestamp stamp, TimeZone timeZone, Locale locale) {
        Calendar cal = toCalendar(stamp, timeZone, locale);
        return cal.get(Calendar.YEAR);
    }

    public static Date getEarliestDate() {
        Calendar cal = getCalendarInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        cal.set(Calendar.YEAR, cal.getActualMinimum(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.getActualMinimum(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getLatestDate() {
        Calendar cal = getCalendarInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault());
        cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * Returns a copy of {@code date} that cannot be modified.
     * Attempts to modify the returned date will result in an
     * {@code UnsupportedOperationException}.
     *
     * @param date  the date to copy
     * @return an immutable copy of {@code date}.
     */
    public static Date unmodifiableDate(Date date) {
        if (date instanceof ImmutableDate) {
            return date;
        }
        return new ImmutableDate(date.getTime());
    }

    @SuppressWarnings("serial")
    private static class ImmutableDate extends Date {
        private ImmutableDate(long date) {
            super(date);
        }

        @Override
        public Object clone() {
            // No need to clone an immutable object.
            return this;
        }

        @Deprecated
        public void setYear(int year) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public void setMonth(int month) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public void setDate(int date) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public void setHours(int hours) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public void setMinutes(int minutes) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public void setSeconds(int seconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTime(long time) {
            throw new UnsupportedOperationException();
        }

    }

    public static String getDateFormat() {
        return DATE_FORMAT;
    }

    public static String getDateTimeFormat() {
        return DATE_TIME_FORMAT;
    }

    public static String getTimeFormat() {
        return TIME_FORMAT;
    }
}
