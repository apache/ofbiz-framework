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
import com.ibm.icu.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/** Temporal expression abstract class. */
public abstract class TemporalExpression implements Serializable, Comparable<TemporalExpression> {
    protected TemporalExpression() {}

    /** Field used to sort expressions. Expression evaluation depends
     * on correct ordering. Expressions are evaluated from lowest value
     * to highest value. */
    protected int sequence = Integer.MAX_VALUE;

    /** Field used to sort expressions. Expression evaluation depends
     * on correct ordering. Expressions are evaluated from lowest value
     * to highest value. */
    protected int subSequence = Integer.MAX_VALUE;

    /** A unique ID for this expression. This field is intended to be used by
     * persistence classes. */
    protected String id = null;

    /** Returns true if this expression includes the specified date.
     * @param cal A date to evaluate
     * @return true if this expression includes the date represented by
     * <code>cal</code>
     */
    public abstract boolean includesDate(Calendar cal);

    /** Returns a date representing the first occurrence of this expression
     * on or after a specified date. Returns <code>null</code> if there
     * is no matching date.
     * @param cal A date to evaluate
     * @return A Calendar instance representing the first matching date,
     * or <code>null</code> if no matching date is found
     */
    public abstract Calendar first(Calendar cal);

    /** Returns a date representing the next occurrence of this expression
     * after a specified date. Returns <code>null</code> if there
     * is no matching date.
     * @param cal A date to evaluate
     * @return A Calendar instance representing the first matching date,
     * or <code>null</code> if no matching date is found
     */
    public abstract Calendar next(Calendar cal);

    /** Handles a <code>TemporalExpressionVisitor</code> visit.
     * @param visitor
     */
    public abstract void accept(TemporalExpressionVisitor visitor);

    public int compareTo(TemporalExpression obj) {
        if (this.equals(obj)) {
            return 0;
        }
        if (obj.sequence < this.sequence) {
            return 1;
        } else if (obj.sequence > this.sequence) {
            return -1;
        }
        return obj.subSequence < this.subSequence ? 1 : -1;
    }

    /** Returns a range of dates matching this expression. Returns an
     * empty Set if no dates are found.
     * @param range The range of dates to evaluate
     * @param cal The starting date
     * @return A Set of matching <code>Date</code> objects
     */
    public Set<Date> getRange(org.ofbiz.base.util.DateRange range, Calendar cal) {
        Set<Date> set = new TreeSet<Date>();
        Date last = range.start();
        Calendar next = first(cal);
        while (next != null && range.includesDate(next.getTime())) {
            last = next.getTime();
            set.add(last);
            next = next(next);
            if (next != null && last.equals(next.getTime())) {
                break;
            }
        }
        return set;
    }

    /** Returns this expression's ID.
     * @return Expression ID String
     */
    public String getId() {
        return this.id;
    }

    /** Sets this expression's ID.
     * @param id Expression ID String
     */
    public void setId(String id) {
        this.id = id;
    }

    protected boolean containsExpression(TemporalExpression expression) {
        return false;
    }

    protected Calendar setStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}
