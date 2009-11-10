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
package org.ofbiz.base.conversion;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilDateTime;

import com.ibm.icu.util.Calendar;

/** Date/time Converter classes. */
public class DateTimeConverters {

    public static class CalendarToLong extends AbstractConverter<Calendar, Long> {

        public Long convert(Calendar obj) throws ConversionException {
            return obj.getTimeInMillis();
        }

        public Class<Calendar> getSourceClass() {
            return Calendar.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }

    }

    public static class CalendarToString extends AbstractConverter<Calendar, String> {

        public String convert(Calendar obj) throws ConversionException {
            Locale locale = obj.getLocale(com.ibm.icu.util.ULocale.VALID_LOCALE).toLocale();
            TimeZone timeZone = UtilDateTime.toTimeZone(obj.getTimeZone().getID());
            DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            return df.format(obj);
        }

        public Class<Calendar> getSourceClass() {
            return Calendar.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class DateToLong extends AbstractConverter<java.util.Date, Long> {

        public Long convert(java.util.Date obj) throws ConversionException {
             return obj.getTime();
        }

        public Class<java.util.Date> getSourceClass() {
            return java.util.Date.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }

    }

    public static class DateToString extends GenericLocalizedConverter<java.util.Date, String> {

        public String convert(java.util.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }

        public Class<java.util.Date> getSourceClass() {
            return java.util.Date.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class DurationToString extends AbstractConverter<TimeDuration, String> {

        public String convert(TimeDuration obj) throws ConversionException {
             return obj.toString();
        }

        public Class<TimeDuration> getSourceClass() {
            return TimeDuration.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static abstract class GenericLocalizedConverter<S, T> extends AbstractLocalizedConverter<S, T> {

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, locale, timeZone, null);
        }

    }

    public static class LongToCalendar extends AbstractLocalizedConverter<Long, Calendar> {

        public Calendar convert(Long obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return UtilDateTime.toCalendar(new java.util.Date(obj.longValue()), timeZone, locale);
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<Calendar> getTargetClass() {
            return Calendar.class;
        }

    }

    public static class NumberToDate extends AbstractConverter<Number, java.util.Date> {

        public java.util.Date convert(Number obj) throws ConversionException {
             return new java.util.Date(obj.longValue());
        }

        public Class<Number> getSourceClass() {
            return Number.class;
        }

        public Class<java.util.Date> getTargetClass() {
            return java.util.Date.class;
        }

    }

    public static class NumberToDuration extends AbstractConverter<Number, TimeDuration> {

        public TimeDuration convert(Number obj) throws ConversionException {
             return TimeDuration.fromNumber(obj);
        }

        public Class<Number> getSourceClass() {
            return Number.class;
        }

        public Class<TimeDuration> getTargetClass() {
            return TimeDuration.class;
        }

    }

    public static class NumberToSqlDate extends AbstractConverter<Number, java.sql.Date> {

        public java.sql.Date convert(Number obj) throws ConversionException {
             return new java.sql.Date(obj.longValue());
        }

        public Class<Number> getSourceClass() {
            return Number.class;
        }

        public Class<java.sql.Date> getTargetClass() {
            return java.sql.Date.class;
        }

    }

    public static class NumberToSqlTime extends AbstractConverter<Number, java.sql.Time> {

        public java.sql.Time convert(Number obj) throws ConversionException {
             return new java.sql.Time(obj.longValue());
        }

        public Class<Number> getSourceClass() {
            return Number.class;
        }

        public Class<java.sql.Time> getTargetClass() {
            return java.sql.Time.class;
        }

    }

    public static class NumberToTimestamp extends AbstractConverter<Number, java.sql.Timestamp> {

        public java.sql.Timestamp convert(Number obj) throws ConversionException {
             return new java.sql.Timestamp(obj.longValue());
        }

        public Class<Number> getSourceClass() {
            return Number.class;
        }

        public Class<java.sql.Timestamp> getTargetClass() {
            return java.sql.Timestamp.class;
        }

    }

    public static class SqlDateToString extends GenericLocalizedConverter<java.sql.Date, String> {

        public String convert(java.sql.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }

        public Class<java.sql.Date> getSourceClass() {
            return java.sql.Date.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class SqlTimeToString extends GenericLocalizedConverter<java.sql.Time, String> {

        public String convert(java.sql.Time obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }

        public Class<java.sql.Time> getSourceClass() {
            return java.sql.Time.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class StringToCalendar extends AbstractLocalizedConverter<String, Calendar> {

        public Calendar convert(String obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            if (obj.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                java.util.Date date = df.parse(obj);
                return UtilDateTime.toCalendar(date, timeZone, locale);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Calendar> getTargetClass() {
            return Calendar.class;
        }

    }

    public static class StringToDate extends GenericLocalizedConverter<String, java.util.Date> {

        public java.util.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            if (obj.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return df.parse(obj);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<java.util.Date> getTargetClass() {
            return java.util.Date.class;
        }

    }

    public static class StringToDuration extends AbstractConverter<String, TimeDuration> {

        public TimeDuration convert(String obj) throws ConversionException {
             return TimeDuration.parseDuration(obj);
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<TimeDuration> getTargetClass() {
            return TimeDuration.class;
        }

    }

    public static class StringToSqlDate extends GenericLocalizedConverter<String, java.sql.Date> {

        public java.sql.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            if (obj.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Date(df.parse(obj).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<java.sql.Date> getTargetClass() {
            return java.sql.Date.class;
        }

    }

    public static class StringToSqlTime extends GenericLocalizedConverter<String, java.sql.Time> {

        public java.sql.Time convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            if (obj.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Time(df.parse(obj).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<java.sql.Time> getTargetClass() {
            return java.sql.Time.class;
        }

    }

    public static class StringToTimestamp extends GenericLocalizedConverter<String, java.sql.Timestamp> {

        public java.sql.Timestamp convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            if (obj.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (formatString == null || formatString.length() == 0) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Timestamp(df.parse(obj).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<java.sql.Timestamp> getTargetClass() {
            return java.sql.Timestamp.class;
        }

    }

    public static class StringToTimeZone extends AbstractConverter<String, TimeZone> {

        public TimeZone convert(String obj) throws ConversionException {
            TimeZone tz = UtilDateTime.toTimeZone(obj);
            if (tz != null) {
                return tz;
            } else {
                throw new ConversionException("Could not convert " + obj + " to TimeZone: ");
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<TimeZone> getTargetClass() {
            return TimeZone.class;
        }

    }

    public static class TimeZoneToString extends AbstractConverter<TimeZone, String> {

        public String convert(TimeZone obj) throws ConversionException {
            return obj.getID();
        }

        public Class<TimeZone> getSourceClass() {
            return TimeZone.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

}
