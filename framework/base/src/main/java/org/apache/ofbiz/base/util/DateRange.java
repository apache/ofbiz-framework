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
import java.sql.Timestamp;
import java.util.Date;

import org.apache.ofbiz.base.lang.ComparableRange;

/** An immutable range of dates. This class is here for backward compatibility -
 * new code should use <code>ComparableRange&lt;Date&gt;</code> instead.
 */
@SuppressWarnings("serial")
public class DateRange extends ComparableRange<Date> implements Serializable {
    /** A <code>Date</code> instance initialized to the earliest possible date.*/
    public static final Date MIN_DATE = UtilDateTime.unmodifiableDate(UtilDateTime.getEarliestDate());
    /** A <code>Date</code> instance initialized to the latest possible date.*/
    public static final Date MAX_DATE = UtilDateTime.unmodifiableDate(UtilDateTime.getLatestDate());
    /** A <code>DateRange</code> instance initialized to the widest possible range of dates.*/
    public static final DateRange FULL_RANGE = new DateRange(MIN_DATE, MAX_DATE);

    protected static Date timestampToDate(Date date) {
        // Testing for equality between a Date instance and a Timestamp instance
        // will always return false.
        if (date instanceof Timestamp) {
            return new Date(date.getTime());
        }
        return date;
    }

    /**
     * @param start If null, defaults to <a href="#MIN_DATE">MIN_DATE</a>
     * @param end If null, defaults to <a href="#MAX_DATE">MAX_DATE</a>
     */
    public DateRange(Date start, Date end) {
        super(start == null ? MIN_DATE : UtilDateTime.unmodifiableDate(timestampToDate(start)), end == null ? MAX_DATE
                : UtilDateTime.unmodifiableDate(timestampToDate(end)));
    }

    @Override
    public boolean after(Date date) {
        return super.after(timestampToDate(date));
    }

    @Override
    public boolean before(Date date) {
        return super.before(timestampToDate(date));
    }

    /** Returns this range's duration as a millisecond value.
     * @return Range duration in milliseconds
     */
    public long durationInMillis() {
        return this.getEnd().getTime() - this.getStart().getTime();
    }

    @Override
    public Date end() {
        return (Date) this.getEnd().clone();
    }

    /** Returns the ending date of this range as a <code>Timestamp</code> instance.
     * @return Range ending date <code>Timestamp</code>
     */
    public Timestamp endStamp() {
        return new Timestamp(this.getEnd().getTime());
    }

    /** Returns <code>true</code> if <code>date</code> occurs within this range.
     * @param date
     * @return <code>true</code> if <code>date</code> occurs within this range
     */
    public boolean includesDate(Date date) {
        return super.includes(timestampToDate(date));
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

    /** Returns <code>true</code> if <code>range</code> intersects this range.
     * @param range
     * @return <code>true</code> if <code>range</code> intersects this range
     */
    public boolean intersectsRange(DateRange range) {
        return (isPoint() && range.isPoint() && this.getStart().equals(range.getStart())) || (!before(range) && !after(range));
    }

    @Override
    public Date start() {
        return (Date) this.getStart().clone();
    }

    /** Returns the starting date of this range as a <code>Timestamp</code> instance.
     * @return Range starting date <code>Timestamp</code>
     */
    public Timestamp startStamp() {
        return new Timestamp(this.getStart().getTime());
    }
}
