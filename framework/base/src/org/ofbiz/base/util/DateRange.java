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
import java.sql.Timestamp;
import java.util.Date;

/** An immutable range of dates. */
@SuppressWarnings("serial")
public class DateRange implements Serializable {
    /** A <code>Date</code> instance initialized to the earliest possible date.*/
    public static final Date MIN_DATE = UtilDateTime.getEarliestDate();
    /** A <code>Date</code> instance initialized to the latest possible date.*/
    public static final Date MAX_DATE = UtilDateTime.getLatestDate();
    /** A <code>DateRange</code> instance initialized to the widest possible range of dates.*/
    public static final DateRange FullRange = new DateRange();

    protected Date start = MIN_DATE;
    protected Date end = MAX_DATE;
    protected DateRange() {}

    /**
     * @param start If null, defaults to <a href="#MIN_DATE">MIN_DATE</a>
     * @param end If null, defaults to <a href="#MAX_DATE">MAX_DATE</a>
     */
    public DateRange(Date start, Date end) {
        if (start != null) {
            this.start = new Date(start.getTime());
        }
        if (end != null) {
            this.end = new Date(end.getTime());
        }
    }

    /** Returns this range's duration as a millisecond value.
     * @return Range duration in milliseconds
     */
    public long durationInMillis() {
        if (this.end.after(this.start)) {
            return this.end.getTime() - this.start.getTime();
        } else {
            return this.start.getTime() - this.end.getTime();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DateRange && ((DateRange)obj).start.equals(this.start) && ((DateRange)obj).end.equals(this.end);
    }

    @Override
    public String toString() {
        return this.start + " - " + this.end;
    }

    /** Returns the starting date of this range.
     * @return Range starting date
     */
    public Date start() {
        return (Date) this.start.clone();
    }

    /** Returns the starting date of this range as a <code>Timestamp</code> instance.
     * @return Range starting date <code>Timestamp</code>
     */
    public Timestamp startStamp() {
        return new Timestamp(this.start.getTime());
    }

    /** Returns the ending date of this range.
     * @return Range ending date
     */
    public Date end() {
        return (Date) this.end.clone();
    }

    /** Returns the ending date of this range as a <code>Timestamp</code> instance.
     * @return Range ending date <code>Timestamp</code>
     */
    public Timestamp endStamp() {
        return new Timestamp(this.end.getTime());
    }

    /** Returns <code>true</code> if the ending date is later than the starting date.
     * @return <code>true</code> if the ending date is later than the starting date
     */
    public boolean isAscending() {
        return this.end.after(this.start) && !this.end.equals(this.start);
    }

    /** Returns <code>true</code> if the starting and ending dates are equal.
     * @return <code>true</code> if the starting and ending dates are equal
     */
    public boolean isPoint() {
        return this.end.equals(this.start);
    }

    /** Returns <code>true</code> if <code>date</code> occurs within this range.
     * @param date
     * @return <code>true</code> if <code>date</code> occurs within this range
     */
    public boolean includesDate(Date date) {
        date = downcastTimestamp(date);
        if (isPoint()) {
            return date.equals(this.start);
        }
        if (isAscending()) {
            return (this.start.equals(date) || date.after(this.start)) && (this.end.equals(date) || date.before(this.end));
        } else {
            return (this.start.equals(date) || date.before(this.start)) && (this.end.equals(date) || date.after(this.end));
        }
    }

    /** Returns <code>true</code> if this range occurs before <code>date</code>.
     * @param date
     * @return <code>true</code> if this range occurs before <code>date</code>
     */
    public boolean before(Date date) {
        date = downcastTimestamp(date);
        if (isAscending() || isPoint()) {
            return this.end.before(date);
        } else {
            return this.start.before(date);
        }
    }

    /** Returns <code>true</code> if the latest date in this range
     * occurs before the earliest date in <code>range</code>.
     * @param range
     * @return <code>true</code> if the latest date in this range
     * occurs before the earliest date in <code>range</code>
     */
    public boolean before(DateRange range) {
        if (isAscending() || isPoint()) {
            if (range.isAscending()) {
                return this.end.before(range.start);
            } else {
                return this.end.before(range.end);
            }
        } else {
            if (range.isAscending()) {
                return this.start.before(range.start);
            } else {
                return this.start.before(range.end);
            }
        }
    }

    /** Returns <code>true</code> if this range occurs after <code>date</code>.
     * @param date
     * @return <code>true</code> if this range occurs after <code>date</code>
     */
    public boolean after(Date date) {
        date = downcastTimestamp(date);
        if (isAscending() || isPoint()) {
            return this.start.after(date);
        } else {
            return this.end.after(date);
        }
    }

    /** Returns <code>true</code> if the earliest date in this range
     * occurs after the latest date in <code>range</code>.
     * @param range
     * @return <code>true</code> if the earliest date in this range
     * occurs after the latest date in <code>range</code>
     */
    public boolean after(DateRange range) {
        if (isAscending() || isPoint()) {
            if (range.isAscending()) {
                return this.start.after(range.end);
            } else {
                return this.start.after(range.start);
            }
        } else {
            if (range.isAscending()) {
                return this.end.after(range.end);
            } else {
                return this.end.after(range.start);
            }
        }
    }

    /** Returns <code>true</code> if <code>range</code> intersects this range.
     * @param range
     * @return <code>true</code> if <code>range</code> intersects this range
     */
    public boolean intersectsRange(DateRange range) {
        if (isPoint() && range.isPoint() && this.start.equals(range.start)) {
            return true;
        }
        return !before(range) && !after(range);
    }

    /** Returns <code>true</code> if <code>start</code> and <code>end</code>
     * intersect this range.
     * @param start If null, defaults to <a href="#MIN_DATE">MIN_DATE</a>
     * @param end If null, defaults to <a href="#MAX_DATE">MAX_DATE</a>
     * @return <code>true</code> if <code>start</code> and <code>end</code>
     * intersect this range
     */
    public boolean intersectsRange(Date start, Date end) {
        return intersectsRange(new DateRange(start, end));
    }

    protected Date downcastTimestamp(Date date) {
        // Testing for equality between a Date instance and a Timestamp instance
        // will always return false.
        if (date instanceof Timestamp) {
            date = new Date(date.getTime());
        }
        return date;
    }
}
