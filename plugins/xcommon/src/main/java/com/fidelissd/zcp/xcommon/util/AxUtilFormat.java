/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Forrest Rae <forrest.rae@fidelissd.com>, January, 2017                       *
 *  *****************************************************************************************
 */

package com.fidelissd.zcp.xcommon.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.ibm.icu.text.RuleBasedNumberFormat;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;

/** Extended formatting related Utility functions Created by mande on 1/14/2017. */
public class AxUtilFormat {

  private static final String module = AxUtilFormat.class.getName();

  public static final String APP_CREATED_DATE_TIME_FORMAT = "EEEEEE, MMM d yyyy 'at' h:mm a";
  public static final String APP_CREATED_DATE_TIME_FORMAT_WITH_TIME_ZONE =
      "EEEEEE, MMM d yyyy 'at' h:mm a z";
  public static final String APP_SHORT_DATE_FORMAT = "MMM dd, yyyy";
  public static final String APP_LONG_DATE_TIME_FORMAT = "EEEEEE, MMM d yyyy h:mm a";
  public static final String APP_MEDIUM_DATE_TIME_FORMAT = "EEEEEE, MMM d h:mm a";
  public static final String APP_DAYNAME_MONTH_DAY_FORMAT = "EEEEEE, MMM d";
  public static final String APP_TIME_FORMAT = "h:mm a";
  public static final String HOUR_MIN_TIME_FORMAT = "HH:mm";
  public static final String APP_US_DATE_FORMAT = "MM/dd/yyyy";
  public static final String DOJO_DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

  /**
   * Returns date opbject for input hours and mins example 09:30, 06:00
   *
   * @param timeInHoursAndMins in hh:mm format
   */
  public static Date parseHoursAndMinsAsDate(String timeInHoursAndMins) {
    Date parsedDate = null;
    SimpleDateFormat sdf = new SimpleDateFormat(HOUR_MIN_TIME_FORMAT);
    try {
      parsedDate = sdf.parse(timeInHoursAndMins);
    } catch (ParseException e) {
      Debug.logError(e, module);
    }

    return parsedDate;
  }

  /**
   * Includes date, time and year.
   *
   * @param dateTimeToFormat e.g. Saturday, Jan 14 2017 2:16 PM
   */
  public static String formatDateLong(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(
        new Date(dateTimeToFormat.getTime()), APP_LONG_DATE_TIME_FORMAT);
  }

  /**
   * Includes date and time, no year.
   *
   * @param dateTimeToFormat e.g Saturday, Jan 14 2:16 PM
   */
  public static String formatDateMedium(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(
        new Date(dateTimeToFormat.getTime()), APP_MEDIUM_DATE_TIME_FORMAT);
  }

  /**
   * Includes date, month and year
   *
   * @param dateTimeToFormat e.g. Jan 14, 2022
   */
  public static String formatDateShort(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(new Date(dateTimeToFormat.getTime()),
        APP_SHORT_DATE_FORMAT);
  }

  /**
   * Includes date only.
   *
   * @param dateTimeToFormat e.g. Saturday, Jan 14
   */
  public static String formatDate(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(new Date(dateTimeToFormat.getTime()),
        APP_DAYNAME_MONTH_DAY_FORMAT);
  }

  /**
   * Includes time only.
   *
   * @param dateTimeToFormat e.g. 2:16 PM
   */
  public static String formatTime(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(new Date(dateTimeToFormat.getTime()), APP_TIME_FORMAT);
  }

  /**
   * Formats date with 'at' included. Useful for displaying created timestamps.
   *
   * @param dateTimeToFormat e.g. Saturday, Jan 14 at 2:16 PM
   */
  public static String formatCreatedTimestamp(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(
        new Date(dateTimeToFormat.getTime()), APP_CREATED_DATE_TIME_FORMAT);
  }

  /**
   * Formats date with 'at' included with timezone. Useful for displaying created timestamps.
   *
   * @param dateTimeToFormat e.g. Saturday, Jan 14 at 2:16 PM PST
   */
  public static String formatCreatedTimestampWithTimezone(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(
        new Date(dateTimeToFormat.getTime()), APP_CREATED_DATE_TIME_FORMAT_WITH_TIME_ZONE);
  }

  /**
   * Formats date to standard US format MM/dd/yyyy
   *
   * @param dateTimeToFormat e.g. 04/01/2017
   */
  public static String formatToUsDate(Timestamp dateTimeToFormat) {
    return UtilDateTime.toDateString(new Date(dateTimeToFormat.getTime()), APP_US_DATE_FORMAT);
  }

  public static String formatToUsDate(String dateTimeToFormat) throws ParseException {
    return formatToUsDate(dateTimeToFormat, DOJO_DEFAULT_DATE_FORMAT);
  }

  public static String formatToUsDate(String dateTimeToFormat, String dateFormat)
      throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    Timestamp dateTimeToFormatTimestamp = UtilDateTime.toTimestamp(sdf.parse(dateTimeToFormat));
    if (UtilValidate.isNotEmpty(dateTimeToFormatTimestamp)) {
      return UtilDateTime.toDateString(
          new Date(dateTimeToFormatTimestamp.getTime()), APP_US_DATE_FORMAT);
    }

    return null;
  }

  /** Formats US phone number using a Telecom number */
  public static PhoneNumber formatUsPhone(
      String countryCode, String areaCode, String contactNumber, String extension) {
    if (UtilValidate.isEmpty(areaCode) || UtilValidate.isEmpty(contactNumber)) {
      return null;
    }

    String rawString = countryCode + "-" + areaCode + "-" + contactNumber;
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    PhoneNumber number = null;
    try {
      number = phoneUtil.parse(rawString, "US");
    } catch (NumberParseException e) {
      Debug.logWarning(e, module);
      return null;
    }
    if (UtilValidate.isNotEmpty(extension)) {
      number.setExtension(extension);
    }

    return number;
  }

  /**
   * Utility method to spell out a given number. Few examples: spellOutNumber(1) -> first
   * spellOutNumber(2) -> second spellOutNumber(3) -> third etc...
   */
  public static String spellOutNumber(int number) {
    RuleBasedNumberFormat ruleBasedNumberFormat =
        new RuleBasedNumberFormat(Locale.US, RuleBasedNumberFormat.SPELLOUT);
    return ruleBasedNumberFormat.format(number, "%spellout-ordinal");
  }

  /** Formats the file size to a more human readable format e.g. 10 B/KB/MB/GB */
  public static String formatFileSize(BigDecimal fileSizeInBytes) {
    return formatFileSize(fileSizeInBytes.longValue());
  }

  /** Formats the file size to a more human readable format e.g. 10 B/KB/MB/GB */
  public static String formatFileSize(long fileSizeInBytes) {
    if (fileSizeInBytes <= 0) {
      return "0";
    }
    final String[] units = new String[] {"B", "kB", "MB", "GB", "TB"};
    int digitGroups = (int) (Math.log10(fileSizeInBytes) / Math.log10(1024));
    return new DecimalFormat("#,##0.#").format(fileSizeInBytes / Math.pow(1024, digitGroups))
        + " "
        + units[digitGroups];
  }

  public static String formatFileSizeFromBtyesToGB(long fileSizeInBytes, boolean noDecimal) {
    long kilo = 1024;
    long mega = kilo * kilo;
    long giga = mega * kilo;
    long tera = giga * kilo;

    double kb = (double) fileSizeInBytes / kilo;
    double mb = kb / kilo;
    double gb = mb / kilo;
    double tb = gb / kilo; // for future usage

    return String.format(noDecimal ? "%.0f" : "%.2f", gb);
  }

  /**
   * Returns the closes hour time in given format or default format if nothing is given.
   *
   * @param optionalFormat format for the time, defaults to HH:mm:ss
   */
  public static String getClosestTimeWindowFromNow(
      TimeZone userTimeZone, String optionalFormat, int minutesToAdd) {
    String closestTime = "";

    if (UtilValidate.isEmpty(userTimeZone)) {
      userTimeZone = TimeZone.getDefault();
    }

    Date nowDateTs = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    formatter.setTimeZone(userTimeZone);
    try {
      String nowDateStr = formatter.format(nowDateTs);
      Date nowDate = sdf.parse(nowDateStr);
      if (UtilValidate.isEmpty(optionalFormat)) {
        optionalFormat = "HH:mm:ss";
      }

      // hack to ensure 30 mins ahead of current time is given here
      Calendar nowCal = Calendar.getInstance();
      nowCal.setTime(nowDate);
      int minsPastCurrentHour = nowCal.get(Calendar.MINUTE);
      if (minsPastCurrentHour <= 30) {
        nowDate = DateUtils.setMinutes(nowDate, 30);
        if (minutesToAdd > 0) {
          nowDate = DateUtils.addMinutes(nowDate, minutesToAdd);
        }
        closestTime = UtilDateTime.toDateString(nowDate, optionalFormat);
      } else if (minsPastCurrentHour >= 30) {
        nowDate = DateUtils.setMinutes(nowDate, 31);
        Date nearestHour = DateUtils.round(nowDate, Calendar.HOUR);

        if (minutesToAdd > 0) {
          nowDate = DateUtils.addMinutes(nearestHour, minutesToAdd);
          closestTime = UtilDateTime.toDateString(nowDate, optionalFormat);
        } else if (minutesToAdd == 0) {
          closestTime = UtilDateTime.toDateString(nearestHour, optionalFormat);
        }
      }
    } catch (ParseException e) {
      Debug.logError(e, module);
      return null;
    }
    return closestTime;
  }

  public static Timestamp convertTimeToTimezone(
      Timestamp inputTime, TimeZone fromTZ, TimeZone toTZ, Locale locale) {
    Date date = new Date(inputTime.getTime());
    long fromTZDst = 0;
    if (fromTZ.inDaylightTime(date)) {
      fromTZDst = fromTZ.getDSTSavings();
    }
    long fromTZOffset = fromTZ.getRawOffset() + fromTZDst;
    long toTZDst = 0;
    if (toTZ.inDaylightTime(date)) {
      toTZDst = toTZ.getDSTSavings();
    }
    long toTZOffset = toTZ.getRawOffset() + toTZDst;
    Date convertedDate = new Date(date.getTime() + (toTZOffset - fromTZOffset));
    return UtilDateTime.toTimestamp(convertedDate);
  }

  private static final String SEPARATOR = ",";

  /**
   * Converts a list of strings to a list of comma separated values. ["Milan", "London", "New York",
   * "San Francisco"] will return "Milan,London,New York,San Francisco"
   */
  public static String toCsv(List<String> listToConvert) {
    return String.join(SEPARATOR, listToConvert);
  }
}
