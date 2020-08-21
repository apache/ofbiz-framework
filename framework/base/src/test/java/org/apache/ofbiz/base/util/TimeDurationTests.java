/*
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
 */
package org.apache.ofbiz.base.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

public class TimeDurationTests {
    private static final Calendar ZERO = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    static {
        ZERO.clear();
        ZERO.setTimeInMillis(0);
    }

    private static <T extends Comparable<T>> int doCompare(T comparable, T other) {
        return comparable.compareTo(other);
    }

    private static void assertDurationFields(String label, DateTuple d, String string, TimeDuration duration,
            boolean isNegative, boolean isZero) {
        assertEquals(label + ".years()", d.years, duration.years());
        assertEquals(label + ".months()", d.months, duration.months());
        assertEquals(label + ".days()", d.days, duration.days());
        assertEquals(label + ".hours()", d.hours, duration.hours());
        assertEquals(label + ".minutes()", d.minutes, duration.minutes());
        assertEquals(label + ".seconds()", d.seconds, duration.seconds());
        assertEquals(label + ".milliseconds()", d.milliseconds, duration.milliseconds());
        assertEquals(label + ".isNegative()", isNegative, duration.isNegative());
        assertEquals(label + ".toString()", string, duration.toString());
        assertEquals(label + ".equals(from/to long)", duration, TimeDuration.fromLong(TimeDuration.toLong(duration)));
        assertEquals(label + ".equals(from/to number)", duration,
                TimeDuration.fromNumber(TimeDuration.toLong(duration)));
        assertEquals(label + ".isZero", isZero, duration.isZero());
        if (isZero) {
            assertEquals(label + ".compareTo(zero) == 0", 0, doCompare(TimeDuration.ZERO_TIME_DURATION, duration));
            assertEquals(label + ".compareTo(zero) == 0", 0, doCompare(duration, TimeDuration.ZERO_TIME_DURATION));
        } else {
            assertNotSame(label + ".compareTo(zero) != 0", 0, doCompare(TimeDuration.ZERO_TIME_DURATION, duration));
            assertNotSame(label + ".compareTo(zero) != 0", 0, doCompare(duration, TimeDuration.ZERO_TIME_DURATION));
        }
    }

    private static final class DateTuple {
        private final int years;
        private final int months;
        private final int days;
        private final int hours;
        private final int minutes;
        private final int seconds;
        private final int milliseconds;

        private DateTuple(int years, int months, int days, int hours, int minutes, int seconds, int milliseconds) {
            this.years = years;
            this.months = months;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.milliseconds = milliseconds;
        }

        static DateTuple of(int years, int months, int days, int hours, int minutes, int seconds, int milliseconds) {
            return new DateTuple(years, months, days, hours, minutes, seconds, milliseconds);
        }
    }

    private static TimeDuration assertDurationLoop(String label, Calendar right, DateTuple d, TimeDuration lastString,
            boolean isNegative) {
        StringBuilder sb = new StringBuilder();
        sb.append(d.years != 0 ? d.years : "");
        sb.append(':').append(d.months != 0 ? d.months : "");
        sb.append(':').append(d.days != 0 ? d.days : "");
        sb.append(':').append(d.hours != 0 ? d.hours : "");
        sb.append(':').append(d.minutes != 0 ? d.minutes : "");
        sb.append(':').append(d.seconds != 0 ? d.seconds : "");
        sb.append(':').append(d.milliseconds != 0 ? d.milliseconds : "");
        String durationString = d.years + ":" + d.months + ":" + d.days + ":"
                + d.hours + ":" + d.minutes + ":" + d.seconds + ":" + d.milliseconds;
        TimeDuration stringDuration = TimeDuration.parseDuration(sb.toString());
        right.setTimeInMillis(0);
        if (d.years != 0) {
            right.set(Calendar.YEAR, 1970 + Math.abs(d.years));
        }
        if (d.months != 0) {
            right.set(Calendar.MONTH, Math.abs(d.months));
        }
        right.set(Calendar.DAY_OF_MONTH, Math.abs(d.days) + 1);
        if (d.hours != 0) {
            right.set(Calendar.HOUR, Math.abs(d.hours));
        }
        if (d.minutes != 0) {
            right.set(Calendar.MINUTE, Math.abs(d.minutes));
        }
        if (d.seconds != 0) {
            right.set(Calendar.SECOND, Math.abs(d.seconds));
        }
        if (d.milliseconds != 0) {
            right.set(Calendar.MILLISECOND, Math.abs(d.milliseconds));
        }
        TimeDuration calDuration = isNegative ? new TimeDuration(right, ZERO) : new TimeDuration(ZERO, right);
        assertDurationFields(label + "(parseString[0])", d, durationString,
                TimeDuration.parseDuration(durationString), isNegative, false);
        assertDurationFields(label + "(parseString)", d, durationString, stringDuration, isNegative, false);
        assertDurationFields(label + "(cal)", d, durationString, calDuration, isNegative, false);
        Calendar added = calDuration.addToCalendar((Calendar) ZERO.clone());
        TimeDuration addDuration = new TimeDuration(ZERO, added);
        assertDurationFields(label + "(cal[add])", d, durationString, addDuration, isNegative, false);
        assertEquals(label + ".compareTo(string, cal)", 0, doCompare(stringDuration, calDuration));
        assertEquals(label + ".compareTo(string, string)", 0, doCompare(stringDuration, stringDuration));
        assertEquals(label + ".compareTo(cal, cal)", 0, doCompare(calDuration, calDuration));
        assertEquals(label + ".compareTo(cal, string)", 0, doCompare(calDuration, stringDuration));
        assertEquals(label + ".equals(cal, cal)", calDuration, calDuration);
        assertEquals(label + ".equals(cal, string)", calDuration, stringDuration);
        assertEquals(label + ".equals(string, cal)", stringDuration, calDuration);
        assertEquals(label + ".equals(string, string)", stringDuration, stringDuration);
        if (lastString != null) {
            assertFalse(label + ".not-equals(string, lastString)", stringDuration.equals(lastString));
        }
        return stringDuration;
    }

    public static void assertDuration(String label, DateTuple d) {
        TimeDuration lastString = null;
        Calendar right = (Calendar) ZERO.clone();
        for (int i = 1; i < 12; i++) {
            DateTuple nd = DateTuple.of(i * d.years, i * d.months, i * d.days,
                    i * d.hours, i * d.minutes, i * d.seconds, i * d.milliseconds);
            lastString = assertDurationLoop(i + " " + label, right, nd, lastString, false);
        }
        lastString = null;
        for (int i = -2; i > -12; i--) {
            DateTuple nd = DateTuple.of(i * d.years, i * d.months, i * d.days,
                    i * d.hours, i * d.minutes, i * d.seconds, i * d.milliseconds);
            lastString = assertDurationLoop(i + " " + label, right, nd, lastString, true);
        }
    }

    @Test
    public void testDuration() throws Exception {
        Calendar now = Calendar.getInstance();
        TimeDuration zeroDuration = TimeDuration.ZERO_TIME_DURATION;
        assertFalse("zero equals null", zeroDuration.equals(null));
        Calendar newTime = (Calendar) now.clone();
        zeroDuration.addToCalendar(newTime);
        assertEquals("zero same calendar", now, newTime);
        assertDurationFields("zero(same zero calendar)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                new TimeDuration(ZERO, ZERO), false, true);
        assertDurationFields("zero(same now calendar)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                new TimeDuration(now, now), false, true);
        assertDurationFields("zero(empty parse)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                TimeDuration.parseDuration(""), false, true);
        assertDurationFields("zero(zero parse)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                TimeDuration.parseDuration("0:0:0:0:0:0:0"), false, true);
        assertDurationFields("zero(from null number)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                TimeDuration.fromNumber(null), false, true);
        assertDurationFields("zero(from null number)", DateTuple.of(0, 0, 0, 0, 0, 0, 0), "0:0:0:0:0:0:0",
                TimeDuration.fromNumber(null), false, true);
        assertDuration("millisecond", DateTuple.of(0, 0, 0, 0, 0, 0, 1));
        assertDuration("second", DateTuple.of(0, 0, 0, 0, 0, 1, 0));
        assertDuration("minute", DateTuple.of(0, 0, 0, 0, 1, 0, 0));
        assertDuration("hour", DateTuple.of(0, 0, 0, 1, 0, 0, 0));
        assertDuration("day", DateTuple.of(0, 0, 1, 0, 0, 0, 0));
        assertDuration("month", DateTuple.of(0, 1, 0, 0, 0, 0, 0));
        assertDuration("year", DateTuple.of(1, 0, 0, 0, 0, 0, 0));
        Calendar start = new com.ibm.icu.util.GregorianCalendar(1967, 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MILLISECOND, 1);
        end.add(Calendar.SECOND, 1);
        end.add(Calendar.MINUTE, 1);
        end.add(Calendar.HOUR_OF_DAY, 1);
        end.add(Calendar.DAY_OF_MONTH, 1);
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.YEAR, 1);
        assertDurationFields("pre-epoch elapsed time", DateTuple.of(1, 1, 1, 1, 1, 1, 1), "1:1:1:1:1:1:1",
                new TimeDuration(start, end), false, false);
    }
}
