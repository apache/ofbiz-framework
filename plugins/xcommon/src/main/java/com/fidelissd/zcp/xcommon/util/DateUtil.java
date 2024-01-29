/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written Mandeep Sidhu <mandeep.sidhu@fidelissd.com>, September 2018
 *  *****************************************************************************************
 */

package com.fidelissd.zcp.xcommon.util;

import com.fidelissd.zcp.xcommon.collections.FastList;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

public class DateUtil {
    private static final String module = DateUtil.class.getName();
    public static final String TIMEZONE_TIME_FORMAT = "h:mm a,z";

    public static Timestamp combineDateTime(Date date, Date time)
    {
        Calendar calendarA = Calendar.getInstance();
        calendarA.setTime(date);
        Calendar calendarB = Calendar.getInstance();
        calendarB.setTime(time);

        calendarA.set(Calendar.HOUR_OF_DAY, calendarB.get(Calendar.HOUR_OF_DAY));
        calendarA.set(Calendar.MINUTE, calendarB.get(Calendar.MINUTE));
        calendarA.set(Calendar.SECOND, calendarB.get(Calendar.SECOND));
        calendarA.set(Calendar.MILLISECOND, calendarB.get(Calendar.MILLISECOND));

        Date result = calendarA.getTime();
        return UtilDateTime.toTimestamp(result);
    }

    /**
     * Add n years to date string in EEE MMM dd yyyy format.
     * @param dateStr
     * @param n
     * @return
     */
    public static String addYears(String dateStr, int n) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy");
        Timestamp ts = UtilDateTime.toTimestamp(sdf.parse(dateStr));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts.getTime());
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)+n);
        ts.setTime(calendar.getTimeInMillis());
        return UtilDateTime.toDateString(ts, "EEE MMM dd yyyy");
    }


    /**
     * Add the date string in MM/dd/yyyy format.
     *@param dateStr
     *@return
     */
    public static String dayName(String dateStr) {
        String dayName = null;
        Date mydate = null;
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        /* If setLenient() is true - It accepts all dates.
         * If setLenient() is false - It accepts only valid dates.
		 */
        dateFormat.setLenient(false);
        try {
            mydate = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            mydate = null;
        }

        if (mydate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(mydate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            dayName = getDayName(dayOfWeek);
        } else {
            dayName = null;
        }
        return dayName;
    }

    public static String getDayName(int dayofWeek) {

        String dayName = null;
        switch (dayofWeek) {
            case 1:
                dayName = "Sunday";
                break;
            case 2:
                dayName = "Monday";
                break;
            case 3:
                dayName = "Tuesday";
                break;
            case 4:
                dayName = "Wednesday";
                break;
            case 5:
                dayName = "Thursday";
                break;
            case 6:
                dayName = "Friday";
                break;
            case 7:
                dayName = "Saturday";
                break;
        }
        return dayName;
    }

    /**
     * Returns an augmented list of all available time zones with the most commonly used 6 US timezones up top of the list.
     * Returns a list of map's with format of map as
     *  {
     *      id:'America/Chicago',
     *      displayName:'Central Daylight Time',
     *      abbreviation:'CDT',
     *      gmtOffset:'GMT -6:00'
     *  }
     * @param currentTimeZone (Optional) Default timezone that needs to be used to get current time.
     * @param locale
     * @return
     */
    public static List<Map> getFormattedListOfTimezones(TimeZone currentTimeZone, Locale locale) {
        if(UtilValidate.isEmpty(currentTimeZone)){
            currentTimeZone = TimeZone.getDefault();
        }

        List<TimeZone> availableTimeZonesRaw = UtilDateTime.availableTimeZones();
        int displayStyleLong = TimeZone.LONG;
        int displayStyleShort = TimeZone.SHORT;
        List<Map> availableTimeZones = FastList.newInstance();

        //make sure most commonly used timezones of US are at the top
        List<String> usCommonTimezones = UtilMisc.toList(
                "America/Chicago",
                "America/Denver",
                "America/Phoenix",
                "America/Los_Angeles",
                "America/Anchorage",
                "Pacific/Honolulu"
        );

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        for (String usCommonTimezoneID : usCommonTimezones) {
            TimeZone availableTz = UtilDateTime.toTimeZone(usCommonTimezoneID);
            String formattedCurrentTime = UtilDateTime.timeStampToString(nowTimestamp,TIMEZONE_TIME_FORMAT, availableTz, locale);
            Map timeZoneInfo = UtilMisc.toMap(
                "id", usCommonTimezoneID,
                "displayName", availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyleLong, locale),
                "abbreviation", availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyleShort, locale),
                "gmtOffset", displayTimeZoneOffset(availableTz),
                "formattedCurrentTime", formattedCurrentTime
            );

            availableTimeZones.add(timeZoneInfo);
        }

        for (TimeZone availableTz : availableTimeZonesRaw) {
            //exclude the common timezones already extracted above
            if (!usCommonTimezones.contains(availableTz.getID())) {
                String formattedCurrentTime = UtilDateTime.timeStampToString(nowTimestamp,TIMEZONE_TIME_FORMAT, availableTz, locale);
                Map timeZoneInfo = UtilMisc.toMap(
                    "id", availableTz.getID(),
                    "displayName", availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyleLong, locale),
                    "abbreviation", availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyleShort, locale),
                    "gmtOffset", displayTimeZoneOffset(availableTz),
                    "formattedCurrentTime", formattedCurrentTime
                );

                availableTimeZones.add(timeZoneInfo);
            }
        }

        return availableTimeZones;
    }

    /**
     * Returns the GMT offset in GMT +5:00 / GMT -5:00 format
     *
     * @param tz
     * @return
     */
    public static String displayTimeZoneOffset(TimeZone tz) {
        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset()) - TimeUnit.HOURS.toMinutes(hours);
        // avoid -4:-30 issue
        minutes = Math.abs(minutes);

        String result = "";
        if (hours > 0) {
            result = String.format("( GMT +%d:%02d )", hours, minutes);
        } else {
            result = String.format("( GMT %d:%02d )", hours, minutes);
        }

        return result;
    }

    public static Timestamp convertTimeToTimezone(Timestamp inputTime, TimeZone fromTZ, TimeZone toTZ, Locale locale){
        Date date = new Date(inputTime.getTime());

        long fromTZDst = 0;
        if(fromTZ.inDaylightTime(date))
        {
            fromTZDst = fromTZ.getDSTSavings();
        }

        long fromTZOffset = fromTZ.getRawOffset() + fromTZDst;

        long toTZDst = 0;
        if(toTZ.inDaylightTime(date))
        {
            toTZDst = toTZ.getDSTSavings();
        }
        long toTZOffset = toTZ.getRawOffset() + toTZDst;

        Date convertedDate = new Date(date.getTime() + (toTZOffset - fromTZOffset));

        return UtilDateTime.toTimestamp(convertedDate);
    }
}