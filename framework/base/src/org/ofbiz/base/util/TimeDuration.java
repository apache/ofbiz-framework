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

import java.io.Serializable;
import com.ibm.icu.util.Calendar;

/** A representation of a period of time. */
@SuppressWarnings("serial")
public class TimeDuration implements Serializable {
    /** A <code>TimeDuration</code> instance that represents a zero time duration. */
    public static final TimeDuration ZeroTimeDuration = new NullDuration();

    protected int millis = 0;
    protected int seconds = 0;
    protected int minutes = 0;
    protected int hours = 0;
    protected int days = 0;
    protected int months = 0;
    protected int years = 0;
    protected TimeDuration() {}

    /**
     * @param years The number of years in this duration
     * @param months The number of months in this duration
     * @param days The number of days in this duration
     * @param hours The number of hours in this duration
     * @param minutes The number of minutes in this duration
     * @param seconds The number of years in this duration
     * @param millis The number of milliseconds in this duration
     */
    public TimeDuration(int years, int months, int days, int hours, int minutes, int seconds, int millis) {
        this.millis = millis;
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.days = days;
        this.months = months;
        this.years = years;
        if (years < 0 || months < 0  || days < 0 || hours < 0 || minutes < 0 || seconds < 0 || millis < 0) {
            makeNegative();
        }
    }

    /** Elapsed time constructor. The time duration will be computed from the
     * two <code>Calendar</code> instances.
     * @param cal1
     * @param cal2
     */
    public TimeDuration(Calendar cal1, Calendar cal2) {
        this.set(cal1, cal2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        try {
            TimeDuration that = (TimeDuration) obj;
            return this.years == that.years && this.months == that.months && this.days == that.days
            && this.hours == that.hours && this.minutes == that.minutes && this.seconds == that.seconds
            && this.millis == that.millis;
        } catch (Exception e) {}
        return false;
    }

    @Override
    public String toString() {
        return this.years + ":" + this.months + ":" + this.days + ":" + this.hours + ":" + this.minutes + ":" + this.seconds + ":" + this.millis;
    }

    /** Returns the milliseconds in this time duration. */
    public int millis() {
        return this.millis;
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
        cal.add(Calendar.MILLISECOND, this.millis);
        cal.add(Calendar.SECOND, this.seconds);
        cal.add(Calendar.MINUTE, this.minutes);
        cal.add(Calendar.HOUR, this.hours);
        cal.add(Calendar.DAY_OF_MONTH, this.days);
        cal.add(Calendar.MONTH, this.months);
        cal.add(Calendar.YEAR, this.years);
        return cal;
    }

    protected void set(Calendar cal1, Calendar cal2) {
        // set up Calendar objects
        Calendar calStart = null;
        Calendar calEnd = null;
        boolean isNegative = false;
        if (cal1.before(cal2)) {
            calStart = (Calendar) cal1.clone();
            calEnd = (Calendar) cal2.clone();
        } else {
            isNegative = true;
            calStart = (Calendar) cal2.clone();
            calEnd = (Calendar) cal1.clone();
        }

        // this will be used to speed up time comparisons
        long targetMillis = calEnd.getTimeInMillis();
        long deltaMillis = targetMillis - calStart.getTimeInMillis();

        // shortcut for equal dates
        if (deltaMillis == 0) {
            return;
        }

        // compute elapsed years
        long yearMillis = 86400000 * calStart.getMinimum(Calendar.DAY_OF_YEAR);
        float units = deltaMillis / yearMillis;
        this.years = advanceCalendar(calStart, calEnd, (int) units, Calendar.YEAR);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        // compute elapsed months
        long monthMillis = 86400000 * calStart.getMinimum(Calendar.DAY_OF_MONTH);
        units = deltaMillis / monthMillis;
        this.months = advanceCalendar(calStart, calEnd, (int) units, Calendar.MONTH);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        // compute elapsed days
        units = deltaMillis / 86400000;
        this.days = advanceCalendar(calStart, calEnd, (int) units, Calendar.DAY_OF_MONTH);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        // compute elapsed hours
        units = deltaMillis / 3600000;
        this.hours = advanceCalendar(calStart, calEnd, (int) units, Calendar.HOUR);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        // compute elapsed minutes
        units = deltaMillis / 60000;
        this.minutes = advanceCalendar(calStart, calEnd, (int) units, Calendar.MINUTE);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        // compute elapsed seconds
        units = deltaMillis / 1000;
        this.seconds = advanceCalendar(calStart, calEnd, (int) units, Calendar.SECOND);
        deltaMillis = targetMillis - calStart.getTimeInMillis();

        this.millis = (int) deltaMillis;
        if (isNegative) {
            makeNegative();
        }
    }

    protected int advanceCalendar(Calendar start, Calendar end, int units, int type) {
        if (units >= 1) {
            start.add(type, units);
            while (start.after(end)) {
                start.add(type, -1);
                units--;
            }
        }
        return units;
    }

    protected void makeNegative() {
        this.millis = Math.min(this.millis, -this.millis);
        this.seconds = Math.min(this.seconds, -this.seconds);
        this.minutes = Math.min(this.minutes, -this.minutes);
        this.hours = Math.min(this.hours, -this.hours);
        this.days = Math.min(this.days, -this.days);
        this.months = Math.min(this.months, -this.months);
        this.years = Math.min(this.years, -this.years);
    }

    /** Returns a <code>TimeDuration</code> instance derived from a <code>long</code>
     * value. This method is intended to be used in tandem with the
     * <code>toLong</code> method. <p>The years and months portions of the
     * returned object are based on a Gregorian calendar. <b>Note:</b> this
     * method should not be used to calculate elapsed time - use the elapsed
     * time constructor instead.</p>
     *
     * @param millis A millisecond value
     * @return A <code>TimeDuration</code> instance
     */
    public static TimeDuration fromLong(long millis) {
        TimeDuration duration = new TimeDuration();
        if (millis == 0) {
            return duration;
        }
        boolean isNegative = false;
        if (millis < 0) {
            isNegative = true;
            millis = 0 - millis;
        }
        long units = millis / 0x757B12C00L;
        duration.years = (int) units;
        millis -= 0x757B12C00L * (long) duration.years;
        units = millis / 0x9CA41900L;
        duration.months = (int) units;
        millis -= 0x9CA41900L * (long) duration.months;
        units = millis / 86400000;
        duration.days = (int) units;
        millis -= 86400000 * (long) duration.days;
        units = millis / 3600000;
        duration.hours = (int) units;
        millis -= 3600000 * (long) duration.hours;
        units = millis / 60000;
        duration.minutes = (int) units;
        millis -= 60000 * (long) duration.minutes;
        units = millis / 1000;
        duration.seconds = (int) units;
        millis -= 1000 * (long) duration.seconds;
        duration.millis = (int) millis;
        if (isNegative) {
            duration.makeNegative();
        }
        return duration;
    }

    /** Returns a <code>long</code> value derived from a <code>TimeDuration</code>
     * instance. This method is intended to be used in tandem with the
     * <code>fromLong</code> method.
     *
     * @param duration
     * @return the number number of milliseconds in the duration
     */
    public static long toLong(TimeDuration duration) {
        return
        (0x757B12C00L * (long) duration.years) +
        (0x9CA41900L * (long) duration.months) +
        (86400000 * (long) duration.days) +
        (3600000 * (long) duration.hours) +
        (60000 * (long) duration.minutes) +
        (1000 * (long) duration.seconds) +
        duration.millis;
    }

    protected static class NullDuration extends TimeDuration {
        protected NullDuration() {}
        @Override
        public Calendar addToCalendar(Calendar cal) {
            return cal;
        }
    }
}
