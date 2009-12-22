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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.util.TimeDuration;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;

import com.ibm.icu.util.Calendar;

/** Date/time Converter classes. */
public class DateTimeConverters implements ConverterLoader {
    public static class CalendarToLong extends AbstractConverter<Calendar, Long> {
        public CalendarToLong() {
            super(Calendar.class, Long.class);
        }

        public Long convert(Calendar obj) throws ConversionException {
            return obj.getTimeInMillis();
        }
    }

    public static class CalendarToString extends AbstractConverter<Calendar, String> {
        public CalendarToString() {
            super(Calendar.class, String.class);
        }

        public String convert(Calendar obj) throws ConversionException {
            Locale locale = obj.getLocale(com.ibm.icu.util.ULocale.VALID_LOCALE).toLocale();
            TimeZone timeZone = UtilDateTime.toTimeZone(obj.getTimeZone().getID());
            DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            return df.format(obj);
        }
    }

    public static class DateToLong extends AbstractConverter<java.util.Date, Long> {
        public DateToLong() {
            super(java.util.Date.class, Long.class);
        }

        public Long convert(java.util.Date obj) throws ConversionException {
             return obj.getTime();
        }
    }

    public static class DateToSqlDate extends AbstractConverter<java.util.Date, java.sql.Date> {
        public DateToSqlDate() {
            super(java.util.Date.class, java.sql.Date.class);
        }

        public java.sql.Date convert(java.util.Date obj) throws ConversionException {
            return new java.sql.Date(obj.getTime());
        }
    }

    public static class DateToString extends GenericLocalizedConverter<java.util.Date, String> {
        public DateToString() {
            super(java.util.Date.class, String.class);
        }

        @Override
        public String convert(Date obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(java.util.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class DateToTimestamp extends AbstractConverter<java.util.Date, java.sql.Timestamp> {
        public DateToTimestamp() {
            super(java.util.Date.class, java.sql.Timestamp.class);
        }

        public java.sql.Timestamp convert(java.util.Date obj) throws ConversionException {
            return new java.sql.Timestamp(obj.getTime());
        }
    }

    public static class DurationToBigDecimal extends AbstractConverter<TimeDuration, java.math.BigDecimal> {
        public DurationToBigDecimal() {
            super(TimeDuration.class, java.math.BigDecimal.class);
        }

        public java.math.BigDecimal convert(TimeDuration obj) throws ConversionException {
             return new java.math.BigDecimal(TimeDuration.toLong(obj));
        }
    }

    public static class DurationToLong extends AbstractConverter<TimeDuration, Long> {
        public DurationToLong() {
            super(TimeDuration.class, Long.class);
        }

        public Long convert(TimeDuration obj) throws ConversionException {
             return TimeDuration.toLong(obj);
        }
    }

    public static class DurationToString extends AbstractConverter<TimeDuration, String> {
        public DurationToString() {
            super(TimeDuration.class, String.class);
        }

        public String convert(TimeDuration obj) throws ConversionException {
             return obj.toString();
        }
    }

    public static abstract class GenericLocalizedConverter<S, T> extends AbstractLocalizedConverter<S, T> {
        protected GenericLocalizedConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, locale, timeZone, null);
        }
    }

    public static class LongToCalendar extends AbstractLocalizedConverter<Long, Calendar> {
        public LongToCalendar() {
            super(Long.class, Calendar.class);
        }

        public Calendar convert(Long obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return UtilDateTime.toCalendar(new java.util.Date(obj.longValue()), timeZone, locale);
        }

        public Calendar convert(Long obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault());
        }
    }

    public static class NumberToDate extends AbstractConverter<Number, java.util.Date> {
        public NumberToDate() {
            super(Number.class, java.util.Date.class);
        }

        public java.util.Date convert(Number obj) throws ConversionException {
             return new java.util.Date(obj.longValue());
        }
    }

    public static class NumberToDuration extends AbstractConverter<Number, TimeDuration> {
        public NumberToDuration() {
            super(Number.class, TimeDuration.class);
        }

        public TimeDuration convert(Number obj) throws ConversionException {
             return TimeDuration.fromNumber(obj);
        }
    }

    public static class NumberToSqlDate extends AbstractConverter<Number, java.sql.Date> {
        public NumberToSqlDate() {
            super(Number.class, java.sql.Date.class);
        }

        public java.sql.Date convert(Number obj) throws ConversionException {
             return new java.sql.Date(obj.longValue());
        }
    }

    public static class NumberToSqlTime extends AbstractConverter<Number, java.sql.Time> {
        public NumberToSqlTime() {
            super(Number.class, java.sql.Time.class);
        }

        public java.sql.Time convert(Number obj) throws ConversionException {
             return new java.sql.Time(obj.longValue());
        }
    }

    public static class NumberToTimestamp extends AbstractConverter<Number, java.sql.Timestamp> {
        public NumberToTimestamp() {
            super(Number.class, java.sql.Timestamp.class);
        }

        public java.sql.Timestamp convert(Number obj) throws ConversionException {
             return new java.sql.Timestamp(obj.longValue());
        }
    }

    public static class SqlDateToDate extends AbstractConverter<java.sql.Date, java.util.Date> {
        public SqlDateToDate() {
            super(java.sql.Date.class, java.util.Date.class);
        }

        public java.util.Date convert(java.sql.Date obj) throws ConversionException {
            return new java.util.Date(obj.getTime());
        }
    }

    public static class SqlDateToString extends GenericLocalizedConverter<java.sql.Date, String> {
        public SqlDateToString() {
            super(java.sql.Date.class, String.class);
        }

        @Override
        public String convert(java.sql.Date obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(java.sql.Date obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class SqlDateToTimestamp extends AbstractConverter<java.sql.Date, java.sql.Timestamp> {
        public SqlDateToTimestamp() {
            super(java.sql.Date.class, java.sql.Timestamp.class);
        }

        public java.sql.Timestamp convert(java.sql.Date obj) throws ConversionException {
            return new java.sql.Timestamp(obj.getTime());
       }
    }

    public static class SqlTimeToString extends GenericLocalizedConverter<java.sql.Time, String> {
        public SqlTimeToString() {
            super(java.sql.Time.class, String.class);
        }

        public String convert(java.sql.Time obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            return df.format(obj);
        }
    }

    public static class StringToCalendar extends AbstractLocalizedConverter<String, Calendar> {
        public Calendar convert(String obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return convert(obj, Locale.getDefault(), TimeZone.getDefault(), null);
        }

        public StringToCalendar() {
            super(String.class, Calendar.class);
        }

        public Calendar convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                java.util.Date date = df.parse(trimStr);
                return UtilDateTime.toCalendar(date, timeZone, locale);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToDate extends GenericLocalizedConverter<String, java.util.Date> {
        public StringToDate() {
            super(String.class, java.util.Date.class);
        }

        public java.util.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return df.parse(trimStr);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToDuration extends AbstractConverter<String, TimeDuration> {
        public StringToDuration() {
            super(String.class, TimeDuration.class);
        }

        public TimeDuration convert(String obj) throws ConversionException {
             return TimeDuration.parseDuration(obj);
        }
    }

    public static class StringToSqlDate extends GenericLocalizedConverter<String, java.sql.Date> {
        public StringToSqlDate() {
            super(String.class, java.sql.Date.class);
        }

        public java.sql.Date convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Date(df.parse(trimStr).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToSqlTime extends GenericLocalizedConverter<String, java.sql.Time> {
        public StringToSqlTime() {
            super(String.class, java.sql.Time.class);
        }

        public java.sql.Time convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String trimStr = obj.trim();
            if (trimStr.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Time(df.parse(trimStr).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToTimestamp extends GenericLocalizedConverter<String, java.sql.Timestamp> {
        public StringToTimestamp() {
            super(String.class, java.sql.Timestamp.class);
        }

        public java.sql.Timestamp convert(String obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            String str = obj.trim();
            if (str.length() == 0) {
                return null;
            }
            DateFormat df = null;
            if (UtilValidate.isEmpty(formatString)) {
                // These hacks are a bad idea, but they are included
                // for backward compatibility.
                if (str.length() > 0 && !str.contains(":")) {
                    str = str + " 00:00:00.00";
                }
                // hack to mimic Timestamp.valueOf() method
                if (str.length() > 0 && !str.contains(".")) {
                    str = str + ".0";
                } else {
                    // DateFormat has a funny way of parsing milliseconds:
                    // 00:00:00.2 parses to 00:00:00.002
                    // so we'll add zeros to the end to get 00:00:00.200
                    String[] timeSplit = str.split("[.]");
                    if (timeSplit.length > 1 && timeSplit[1].length() < 3) {
                        str = str + "000".substring(timeSplit[1].length());
                    }
                }
                df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, locale);
            } else {
                df = UtilDateTime.toDateTimeFormat(formatString, timeZone, locale);
            }
            try {
                return new java.sql.Timestamp(df.parse(str).getTime());
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToTimeZone extends AbstractConverter<String, TimeZone> {
        public StringToTimeZone() {
            super(String.class, TimeZone.class);
        }

        public TimeZone convert(String obj) throws ConversionException {
            TimeZone tz = UtilDateTime.toTimeZone(obj);
            if (tz != null) {
                return tz;
            } else {
                throw new ConversionException("Could not convert " + obj + " to TimeZone: ");
            }
        }
    }

    public static class TimestampToDate extends AbstractConverter<java.sql.Timestamp, java.util.Date> {
        public TimestampToDate() {
            super(java.sql.Timestamp.class, java.util.Date.class);
        }

        public java.util.Date convert(java.sql.Timestamp obj) throws ConversionException {
            return new java.sql.Timestamp(obj.getTime());
        }
    }

    public static class TimestampToSqlDate extends AbstractConverter<java.sql.Timestamp, java.sql.Date> {
        public TimestampToSqlDate() {
            super(java.sql.Timestamp.class, java.sql.Date.class);
        }

        public java.sql.Date convert(java.sql.Timestamp obj) throws ConversionException {
            return new java.sql.Date(obj.getTime());
        }
    }

    public static class TimeZoneToString extends AbstractConverter<TimeZone, String> {
        public TimeZoneToString() {
            super(TimeZone.class, String.class);
        }

        public String convert(TimeZone obj) throws ConversionException {
            return obj.getID();
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(DateTimeConverters.class);
    }
}
