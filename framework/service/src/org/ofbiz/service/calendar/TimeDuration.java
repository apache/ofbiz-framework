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
package org.ofbiz.service.calendar;

import java.io.Serializable;
import java.util.Calendar;

import org.ofbiz.entity.GenericValue;

/** A representation of a period of time. */
@SuppressWarnings("serial")
public class TimeDuration implements Serializable {
    public static final TimeDuration ZeroTimeDuration = new NullDuration();

    protected int millis = 0;
    protected int seconds = 0;
    protected int minutes = 0;
    protected int hours = 0;
    protected int days = 0;
    protected int months = 0;
    protected int years = 0;

    protected TimeDuration() {}

    public TimeDuration(int millis, int seconds, int minutes, int hours, int days, int months, int years) {
        this.millis = millis;
        this.seconds = seconds;
        this.minutes = minutes;
        this.hours = hours;
        this.days = days;
        this.months = months;
        this.years = years;
    }

    public int millis() {
        return this.millis;
    }

    public int seconds() {
        return this.seconds;
    }

    public int minutes() {
        return this.minutes;
    }

    public int hours() {
        return this.hours;
    }

    public int days() {
        return this.days;
    }

    public int months() {
        return this.months;
    }

    public int years() {
        return this.years;
    }

    /** Add this time duration to a Calendar instance. Returns the original
     * Calendar instance.
     * @param cal
     * @return The <code>cal</code> argument
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

    /** Subtract this time duration to a Calendar instance. Returns the original
     * Calendar instance.
     * @param cal
     * @return The <code>cal</code> argument
     */
    public Calendar subtractFromCalendar(Calendar cal) {
        cal.add(Calendar.MILLISECOND, -this.millis);
        cal.add(Calendar.SECOND, -this.seconds);
        cal.add(Calendar.MINUTE, -this.minutes);
        cal.add(Calendar.HOUR, -this.hours);
        cal.add(Calendar.DAY_OF_MONTH, -this.days);
        cal.add(Calendar.MONTH, -this.months);
        cal.add(Calendar.YEAR, -this.years);
        return cal;
    }

    /** Get a <code>TimeDuration</code> instance based on duration fields
     * in a GenericValue. Returns ZeroTimeDuration if there is no
     * information in the duration fields or if the value argument is null.
     * <p>The GenericValue <b>must</b> contain the following <code>numeric</code>
     * fields:<ul><li>durationMillis</li><li>durationSeconds</li><li>durationMinutes</li>
     * <li>durationHours</li><li>durationDays</li><li>durationMonths</li>
     * <li>durationYears</li></ul></p>
     * @param value
     * @return A TimeDuration instance
     */
    public static TimeDuration getTimeDuration(GenericValue value) {
        if (value != null) {
            int millis = safeLongToInt(value.getLong("durationMillis"));
            int secs = safeLongToInt(value.getLong("durationSeconds"));
            int mins = safeLongToInt(value.getLong("durationMinutes"));
            int hrs = safeLongToInt(value.getLong("durationHours"));
            int days = safeLongToInt(value.getLong("durationDays"));
            int mos = safeLongToInt(value.getLong("durationMonths"));
            int yrs = safeLongToInt(value.getLong("durationYears"));
            if (millis != 0 || secs != 0 || mins != 0 || hrs != 0 || days != 0 || mos != 0 || yrs != 0) {
                return new TimeDuration(millis, secs, mins, hrs, days, mos, yrs);
            }
        }
        return ZeroTimeDuration;
    }

    protected static int safeLongToInt(Long longObj) {
        return longObj == null ? 0 : longObj.intValue();
    }
    
    protected static class NullDuration extends TimeDuration {
        public Calendar addToCalendar(Calendar cal) {
            return cal;
        }
        public Calendar subtractFromCalendar(Calendar cal) {
            return cal;
        }
    }
}
