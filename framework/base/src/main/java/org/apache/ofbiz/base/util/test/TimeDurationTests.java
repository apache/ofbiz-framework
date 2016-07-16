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
package org.apache.ofbiz.base.util.test;

//import com.ibm.icu.util.Calendar;
//import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class TimeDurationTests extends GenericTestCaseBase {
    private static final Calendar zero = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

    static {
        zero.clear();
        zero.setTimeInMillis(0);
    }

    public TimeDurationTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private static <T extends Comparable<T>> int doCompare(T comparable, T other) {
        return comparable.compareTo(other);
    }

    private static void assertDurationFields(String label, int years, int months, int days, int hours, int minutes, int seconds, int milliseconds, String string, TimeDuration duration, boolean isNegative, boolean isZero) {
        assertEquals(label + ".years()", years, duration.years());
        assertEquals(label + ".months()", months, duration.months());
        assertEquals(label + ".days()", days, duration.days());
        assertEquals(label + ".hours()", hours, duration.hours());
        assertEquals(label + ".minutes()", minutes, duration.minutes());
        assertEquals(label + ".seconds()", seconds, duration.seconds());
        assertEquals(label + ".milliseconds()", milliseconds, duration.milliseconds());
        assertEquals(label + ".isNegative()", isNegative, duration.isNegative());
        assertEquals(label + ".toString()", string, duration.toString());
        assertEquals(label + ".equals(from/to long)", duration, TimeDuration.fromLong(TimeDuration.toLong(duration)));
        assertEquals(label + ".equals(from/to number)", duration, TimeDuration.fromNumber(TimeDuration.toLong(duration)));
        assertEquals(label + ".isZero", isZero, duration.isZero());
        if (isZero) {
            assertEquals(label + ".compareTo(zero) == 0", 0, doCompare(TimeDuration.ZeroTimeDuration, duration));
            assertEquals(label + ".compareTo(zero) == 0", 0, doCompare(duration, TimeDuration.ZeroTimeDuration));
        } else {
            assertNotSame(label + ".compareTo(zero) != 0", 0, doCompare(TimeDuration.ZeroTimeDuration, duration));
            assertNotSame(label + ".compareTo(zero) != 0", 0, doCompare(duration, TimeDuration.ZeroTimeDuration));
        }
    }

    private static TimeDuration assertDurationLoop(String label, Calendar right, int years, int months, int days, int hours, int minutes, int seconds, int milliseconds, TimeDuration lastString, boolean isNegative) {
        StringBuilder sb = new StringBuilder();
        sb.append(years != 0 ? years : "");
        sb.append(':').append(months != 0 ? months : "");
        sb.append(':').append(days != 0 ? days : "");
        sb.append(':').append(hours != 0 ? hours : "");
        sb.append(':').append(minutes != 0 ? minutes : "");
        sb.append(':').append(seconds != 0 ? seconds : "");
        sb.append(':').append(milliseconds != 0 ? milliseconds : "");
        String durationString = years + ":" + months + ":" + days + ":" + hours + ":" + minutes + ":" + seconds + ":" + milliseconds;
        TimeDuration stringDuration = TimeDuration.parseDuration(sb.toString());
        right.setTimeInMillis(0);
        if (years != 0) {
            right.set(Calendar.YEAR, 1970 + Math.abs(years));
        }
        if (months != 0) {
            right.set(Calendar.MONTH, Math.abs(months));
        }
        right.set(Calendar.DAY_OF_MONTH, Math.abs(days) + 1);
        if (hours != 0) {
            right.set(Calendar.HOUR, Math.abs(hours));
        }
        if (minutes != 0) {
            right.set(Calendar.MINUTE, Math.abs(minutes));
        }
        if (seconds != 0) {
            right.set(Calendar.SECOND, Math.abs(seconds));
        }
        if (milliseconds != 0) {
            right.set(Calendar.MILLISECOND, Math.abs(milliseconds));
        }
        TimeDuration calDuration = isNegative ? new TimeDuration(right, zero) : new TimeDuration(zero, right);
        assertDurationFields(label + "(parseString[0])", years, months, days, hours, minutes, seconds, milliseconds, durationString, TimeDuration.parseDuration(durationString), isNegative, false);
        assertDurationFields(label + "(parseString)", years, months, days, hours, minutes, seconds, milliseconds, durationString, stringDuration, isNegative, false);
        assertDurationFields(label + "(cal)", years, months, days, hours, minutes, seconds, milliseconds, durationString, calDuration, isNegative, false);
        Calendar added = calDuration.addToCalendar((Calendar) zero.clone());
        TimeDuration addDuration = new TimeDuration(zero, added);
        assertDurationFields(label + "(cal[add])", years, months, days, hours, minutes, seconds, milliseconds, durationString, addDuration, isNegative, false);
        assertEquals(label + ".compareTo(string, cal)", 0, doCompare(stringDuration, calDuration));
        assertEquals(label + ".compareTo(string, string)", 0, doCompare(stringDuration, stringDuration));
        assertEquals(label + ".compareTo(cal, cal)", 0, doCompare(calDuration, calDuration));
        assertEquals(label + ".compareTo(cal, string)", 0, doCompare(calDuration, stringDuration));
        assertEquals(label + ".equals(cal, cal)", calDuration, calDuration);
        assertEquals(label + ".equals(cal, string)", calDuration, stringDuration);
        assertEquals(label + ".equals(string, cal)", stringDuration, calDuration);
        assertEquals(label + ".equals(string, string)", stringDuration, stringDuration);
        assertFalse(label + ".not-equals(string, this)", stringDuration.equals(TimeDurationTests.class));
        if (lastString != null) {
            assertFalse(label + ".not-equals(string, lastString)", stringDuration.equals(lastString));
        }
        return stringDuration;
    }

    public static void assertDuration(String label, int years, int months, int days, int hours, int minutes, int seconds, int milliseconds) {
        TimeDuration lastString = null;
        Calendar right = (Calendar) zero.clone();
        for (int i = 1; i < 12; i++) {
            lastString = assertDurationLoop(i + " " + label, right, i * years, i * months, i * days, i * hours, i * minutes, i * seconds, i * milliseconds, lastString, false);
        }
        lastString = null;
        for (int i = -2; i > -12; i--) {
            lastString = assertDurationLoop(i + " " + label, right, i * years, i * months, i * days, i * hours, i * minutes, i * seconds, i * milliseconds, lastString, true);
        }
    }

    public void testDuration() throws Exception {
        Calendar now = Calendar.getInstance();
        TimeDuration zeroDuration = TimeDuration.ZeroTimeDuration;
        assertFalse("zero equals null", zeroDuration.equals(null));
        Calendar newTime = (Calendar) now.clone();
        zeroDuration.addToCalendar(newTime);
        assertEquals("zero same calendar", now, newTime);
        assertDurationFields("zero(same zero calendar)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", new TimeDuration(zero, zero), false, true);
        assertDurationFields("zero(same now calendar)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", new TimeDuration(now, now), false, true);
        assertDurationFields("zero(empty parse)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", TimeDuration.parseDuration(""), false, true);
        assertDurationFields("zero(zero parse)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", TimeDuration.parseDuration("0:0:0:0:0:0:0"), false, true);
        assertDurationFields("zero(from null number)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", TimeDuration.fromNumber(null), false, true);
        assertDurationFields("zero(from null number)", 0, 0, 0, 0, 0, 0, 0, "0:0:0:0:0:0:0", TimeDuration.fromNumber(null), false, true);
        assertDuration("millisecond", 0, 0, 0, 0, 0, 0, 1);
        assertDuration("second", 0, 0 ,0 ,0, 0, 1, 0);
        assertDuration("minute", 0, 0, 0, 0, 1, 0, 0);
        assertDuration("hour", 0, 0, 0, 1, 0, 0, 0);
        assertDuration("day",  0, 0, 1, 0, 0, 0, 0);
        assertDuration("month", 0, 1, 0, 0, 0, 0, 0);
        assertDuration("year", 1, 0, 0, 0, 0, 0, 0);
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
        assertDurationFields("pre-epoch elapsed time", 1, 1, 1, 1, 1, 1, 1, "1:1:1:1:1:1:1", new TimeDuration(start, end), false, false);
    }
}
