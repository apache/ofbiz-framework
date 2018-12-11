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

import java.io.Serializable;
import java.util.Objects;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.lang.ThreadSafe;

import com.ibm.icu.util.Calendar;

/** An immutable representation of a period of time. */
@SourceMonitored
@ThreadSafe
@SuppressWarnings("serial")
public class TimeDuration implements Serializable, Comparable<TimeDuration> {
    /** A <code>TimeDuration</code> instance that represents a zero time duration. */
    public static final TimeDuration ZeroTimeDuration = new NullDuration();

    protected final int milliseconds;
    protected final int seconds;
    protected final int minutes;
    protected final int hours;
    protected final int days;
    protected final int months;
    protected final int years;

    /**
     * @param years The number of years in this duration
     * @param months The number of months in this duration
     * @param days The number of days in this duration
     * @param hours The number of hours in this duration
     * @param minutes The number of minutes in this duration
     * @param seconds The number of years in this duration
     * @param milliseconds The number of milliseconds in this duration
     */
    public TimeDuration(int years, int months, int days, int hours, int minutes, int seconds, int milliseconds) {
        this.milliseconds = milliseconds;
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.days = days;
        this.months = months;
        this.years = years;
    }

    /** Elapsed time constructor. The time duration will be computed from the
     * two <code>Calendar</code> instances.
     * @param cal1
     * @param cal2
     */
    public TimeDuration(Calendar cal1, Calendar cal2) {
        // set up Calendar objects
        Calendar calStart;
        Calendar calEnd;
        int factor;
        if (cal1.before(cal2)) {
            factor = 1;
            calStart = (Calendar) cal1.clone();
            calEnd = (Calendar) cal2.clone();
        } else {
            factor = -1;
            calStart = (Calendar) cal2.clone();
            calEnd = (Calendar) cal1.clone();
        }

        /* Strategy: Using millisecond arithmetic alone will produce inaccurate results.
         * Using a Calendar alone will take too long. So, we use millisecond arithmetic
         * to get near the correct result, then zero in on the correct result using a
         * Calendar.
         */
        long targetMillis = calEnd.getTimeInMillis();
        long deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // shortcut for equal dates
        if (deltaMillis == 0) {
            this.years = this.months = this.days = this.hours = this.minutes = this.seconds = this.milliseconds = 0;
            return;
        }

        // compute elapsed years
        long yearMillis = 86400000 * calStart.getLeastMaximum(Calendar.DAY_OF_YEAR);
        float units = deltaMillis / yearMillis;
        this.years = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.YEAR);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // compute elapsed months
        long monthMillis = 86400000 * (calStart.getMaximum(Calendar.DAY_OF_MONTH) / 2);
        units = deltaMillis / monthMillis;
        this.months = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.MONTH);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // compute elapsed days
        units = deltaMillis / 86400000;
        this.days = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.DAY_OF_MONTH);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // compute elapsed hours
        units = deltaMillis / 3600000;
        this.hours = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.HOUR);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // compute elapsed minutes
        units = deltaMillis / 60000;
        this.minutes = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.MINUTE);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        // compute elapsed seconds
        units = deltaMillis / 1000;
        this.seconds = factor * advanceCalendar(calStart, calEnd, (int) units, Calendar.SECOND);
        deltaMillis = computeDeltaMillis(calStart.getTimeInMillis(), targetMillis);

        this.milliseconds = factor * (int) deltaMillis;
    }

    private static long computeDeltaMillis(long start, long end) {
        if (start < 0) {
            return end + (-start);
        }
        return end - start;
    }

    private static int advanceCalendar(Calendar start, Calendar end, int units, int type) {
        if (units >= 1) {
            // Bother, the below needs explanation.
            //
            // If start has a day value of 31, and you add to the month,
            // and the target month is not allowed to have 31 as the day
            // value, then the day will be changed to a value that is in
            // range.  But, when the code needs to then subtract 1 from
            // the month, because it has advanced to far, the day is *not*
            // set back to the original value of 31.
            //
            // This bug can be triggered by having a duration of -1 day,
            // then adding this duration to a calendar that represents 0
            // milliseconds, then creating a new duration by using the 2
            // Calendar constructor, with cal1 being 0, and cal2 being the
            // new calendar that you added the duration to.
            //
            // To solve this problem, we make a temporary copy of the
            // start calendar, and only modify it if we actually have to.
            Calendar tmp = (Calendar) start.clone();
            int tmpUnits = units;
            tmp.add(type, tmpUnits);
            while (tmp.after(end)) {
                tmp.add(type, -1);
                units--;
            }
            if (units != 0) {
                start.add(type, units);
            }
        }
        return units;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        try {
            TimeDuration that = (TimeDuration) obj;
            return this.years == that.years
                    && this.months == that.months
                    && this.days == that.days
                    && this.hours == that.hours
                    && this.minutes == that.minutes
                    && this.seconds == that.seconds
                    && this.milliseconds == that.milliseconds;
        } catch (Exception e) {}
        return false;
    }

    /** Returns a <code>String</code> formatted as
     * years:months:days:hours:minutes:seconds:millseconds.
     */
    @Override
    public String toString() {
        return this.years + ":" + this.months + ":" + this.days + ":" + this.hours + ":" + this.minutes + ":" + this.seconds + ":" + this.milliseconds;
    }

    @Override
    public int compareTo(TimeDuration arg0) {
        if (this == arg0) {
            return 0;
        }
        int r = this.years - arg0.years;
        if (r != 0) {
            return r;
        }
        r = this.months - arg0.months;
        if (r != 0) {
            return r;
        }
        r = this.days - arg0.days;
        if (r != 0) {
            return r;
        }
        r = this.hours - arg0.hours;
        if (r != 0) {
            return r;
        }
        r = this.minutes - arg0.minutes;
        if (r != 0) {
            return r;
        }
        r = this.seconds - arg0.seconds;
        if (r != 0) {
            return r;
        }
        return this.milliseconds - arg0.milliseconds;
    }

    /** Returns <code>true</code> if this duration is negative.
     *
     * @return <code>true</code> if this duration is negative
     */
    public boolean isNegative() {
        return years < 0 || months < 0  || days < 0 || hours < 0 || minutes < 0 || seconds < 0 || milliseconds < 0;
    }

    /** Returns <code>true</code> if this duration is zero.
     *
     * @return <code>true</code> if this duration is zero
     */
    public boolean isZero() {
        return this.milliseconds == 0 && this.seconds == 0 &&
                this.minutes == 0 && this.hours == 0 && this.days == 0 &&
                this.months == 0 && this.years == 0;
    }

    /** Returns the milliseconds in this time duration. */
    public int milliseconds() {
        return this.milliseconds;
    }

    /** Returns the seconds in this time duration. */
    public int seconds() {
        return this.seconds;
    }

    /** Returns the minutes in this time duration. */
    public int minutes() {
        return this.minutes;
    }

    /** Returns the hours in this time duration. */
    public int hours() {
        return this.hours;
    }

    /** Returns the days in this time duration. */
    public int days() {
        return this.days;
    }

    /** Returns the months in this time duration. */
    public int months() {
        return this.months;
    }

    /** Returns the years in this time duration. */
    public int years() {
        return this.years;
    }

    /** Add this time duration to a Calendar instance. Returns the original
     * Calendar instance.
     * @param cal
     * @return <code>cal</code>
     */
    public Calendar addToCalendar(Calendar cal) {
        cal.add(Calendar.MILLISECOND, this.milliseconds);
        cal.add(Calendar.SECOND, this.seconds);
        cal.add(Calendar.MINUTE, this.minutes);
        cal.add(Calendar.HOUR, this.hours);
        cal.add(Calendar.DAY_OF_MONTH, this.days);
        cal.add(Calendar.MONTH, this.months);
        cal.add(Calendar.YEAR, this.years);
        return cal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(milliseconds, seconds, minutes, hours, days, months, years);
    }

    /** Returns a <code>TimeDuration</code> instance derived from an encoded
     * <code>long</code> value. This method is intended to be used in tandem with the
     * <code>toLong</code> method. <b>Note:</b> this
     * method should not be used to calculate elapsed time - use the elapsed
     * time constructor instead.
     *
     * @param duration An encoded duration
     * @return A <code>TimeDuration</code> instance
     */
    public static TimeDuration fromLong(long duration) {
        if (duration == 0) {
            return ZeroTimeDuration;
        }
        long units = duration / 0x757B12C00L;
        int years = (int) units;
        duration -= 0x757B12C00L * years;
        units = duration / 0x9CA41900L;
        int months = (int) units;
        duration -= 0x9CA41900L * months;
        units = duration / 86400000;
        int days = (int) units;
        duration -= 86400000 * (long) days;
        units = duration / 3600000;
        int hours = (int) units;
        duration -= 3600000 * (long) hours;
        units = duration / 60000;
        int minutes = (int) units;
        duration -= 60000 * (long) minutes;
        units = duration / 1000;
        int seconds = (int) units;
        duration -= 1000 * (long) seconds;
        return new TimeDuration(years, months, days, hours, minutes, seconds, (int) duration);
    }

    /** Returns a <code>TimeDuration</code> instance derived from a <code>Number</code>
     * instance. If <code>number</code> is <code>null</code>,
     * returns a zero <code>TimeDuration</code>.<p>This is a convenience method
     * intended to be used with entity engine fields. Some duration fields are
     * stored as a <code>Long</code>, while others are stored as a
     * <code>Double</code>. This method will decode both types.</p>
     *
     * @param number A <code>Number</code> instance, can be <code>null</code>
     * @return A <code>TimeDuration</code> instance
     */
    public static TimeDuration fromNumber(Number number) {
        return number == null ? ZeroTimeDuration : fromLong(number.longValue());
    }

    public static TimeDuration parseDuration(String duration) {
        if (UtilValidate.isEmpty(duration)) {
            return ZeroTimeDuration;
        }
        boolean isZero = true;
        int[] intArray = {0, 0, 0, 0, 0, 0, 0};
        int i = intArray.length - 1;
        String[] strArray = duration.split(":", -1);
        for (int s = strArray.length - 1; s >= 0; s--) {
            if (UtilValidate.isNotEmpty(strArray[s])) {
                intArray[i] = Integer.parseInt(strArray[s].trim());
                if (intArray[i] != 0) {
                    isZero = false;
                }
            }
            i--;
        }
        if (isZero) {
            return ZeroTimeDuration;
        }
        return new TimeDuration(intArray[0], intArray[1], intArray[2],
                intArray[3], intArray[4], intArray[5], intArray[6]);
    }

    /** Returns a <code>long</code> value derived from a <code>TimeDuration</code>
     * instance. This method is intended to be used in tandem with the
     * <code>fromLong</code> method.
     *
     * @param duration
     * @return the duration encoded as a <code>long</code> value
     */
    public static long toLong(TimeDuration duration) {
        return
        (0x757B12C00L * duration.years) +
        (0x9CA41900L * duration.months) +
        (86400000 * (long) duration.days) +
        (3600000 * (long) duration.hours) +
        (60000 * (long) duration.minutes) +
        (1000 * (long) duration.seconds) +
        duration.milliseconds;
    }

    protected static class NullDuration extends TimeDuration {
        protected NullDuration() {
            super(0, 0, 0, 0, 0, 0, 0);
        }
        @Override
        public Calendar addToCalendar(Calendar cal) {
            return cal;
        }
        @Override
        public boolean isZero() {
            return true;
        }
    }
}
