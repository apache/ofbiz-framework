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
package org.apache.ofbiz.service.calendar;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;

import com.ibm.icu.util.Calendar;

/** A collection of TemporalExpression classes.
 * <p>For the most part, these classes are immutable - with the exception
 * of the <code>id</code> field. The basic idea is to construct an expression
 * tree in memory, and then query it.</p>
 */
@SuppressWarnings("serial")
public class TemporalExpressions implements Serializable {
    public static final TemporalExpression NULL_EXPRESSION = new Null();
    // Expressions are evaluated from smallest unit of time to largest.
    // When unit of time is the same, then they are evaluated from
    // least ambiguous to most. Frequency should always be first -
    // since it is the most specific. Date range should always be last.
    // The idea is to evaluate all other expressions, then check to see
    // if the result falls within the date range.
    // Difference: adopts the sequence of its include expression
    // Intersection: aggregates member expression sequence values
    // Substitution: adopts the sequence of its include expression
    // Union: adopts the sequence of its first member expression
    public static final int SEQUENCE_DATE_RANGE = 800;
    public static final int SEQUENCE_DAY_IN_MONTH = 460;
    public static final int SEQUENCE_DOM_RANGE = 400;
    public static final int SEQUENCE_DOW_RANGE = 450;
    public static final int SEQUENCE_FREQ = 100;
    public static final int SEQUENCE_HOUR_RANGE = 300;
    public static final int SEQUENCE_MINUTE_RANGE = 200;
    public static final int SEQUENCE_MONTH_RANGE = 600;

    /** A temporal expression that represents a range of dates. */
    public static class DateRange extends TemporalExpression {
        private static final String MODULE = DateRange.class.getName();
        private final org.apache.ofbiz.base.util.DateRange range;

        public DateRange(Date date) {
            this(date, date);
        }

        public DateRange(Date start, Date end) {
            this.range = new org.apache.ofbiz.base.util.DateRange(start, end);
            this.setSequence(SEQUENCE_DATE_RANGE);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((range == null) ? 0 : range.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    return this.range.equals(((DateRange) obj).range);
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            return includesDate(cal) ? cal : null;
        }

        /** Returns the contained <code>org.apache.ofbiz.base.util.DateRange</code>.
         * @return The contained <code>org.apache.ofbiz.base.util.DateRange</code>
         */
        public org.apache.ofbiz.base.util.DateRange getDateRange() {
            return this.range;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            return this.range.includesDate(cal.getTime());
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            return this.range.includesDate(cal.getTime());
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            return includesDate(cal) ? cal : null;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.range.start() + ", end = " + this.range.end();
        }
    }

    /** A temporal expression that represents a day in the month. */
    public static class DayInMonth extends TemporalExpression {
        private static final String MODULE = DayInMonth.class.getName();
        private final int dayOfWeek;
        private final int occurrence;

        /**
         * @param dayOfWeek An integer in the range of <code>Calendar.SUNDAY</code>
         * to <code>Calendar.SATURDAY</code>
         * @param occurrence An integer in the range of -5 to 5, excluding zero
         */
        public DayInMonth(int dayOfWeek, int occurrence) {
            if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Invalid day argument");
            }
            if (occurrence < -5 || occurrence == 0 || occurrence > 5) {
                throw new IllegalArgumentException("Invalid occurrence argument");
            }
            this.dayOfWeek = dayOfWeek;
            this.occurrence = occurrence;
            int result = occurrence;
            if (result < 0) {
                // Make negative values a higher sequence
                // Example: Last Monday should come after first Monday
                result += 11;
            }
            this.setSequence(SEQUENCE_DAY_IN_MONTH + (result * 10) + dayOfWeek);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /**
         * Align day of week calendar.
         * @param cal the cal
         * @return the calendar
         */
        protected Calendar alignDayOfWeek(Calendar cal) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            if (this.occurrence > 0) {
                while (cal.get(Calendar.DAY_OF_WEEK) != this.dayOfWeek) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                cal.add(Calendar.DAY_OF_MONTH, (this.occurrence - 1) * 7);
            } else {
                cal.add(Calendar.MONTH, 1);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                while (cal.get(Calendar.DAY_OF_WEEK) != this.dayOfWeek) {
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                }
                cal.add(Calendar.DAY_OF_MONTH, (this.occurrence + 1) * 7);
            }
            return cal;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + dayOfWeek;
            result = prime * result + occurrence;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    DayInMonth that = (DayInMonth) obj;
                    return this.dayOfWeek == that.dayOfWeek && this.occurrence == that.occurrence;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            int month = cal.get(Calendar.MONTH);
            Calendar first = alignDayOfWeek((Calendar) cal.clone());
            if (first.before(cal)) {
                first.set(Calendar.DAY_OF_MONTH, 1);
                if (first.get(Calendar.MONTH) == month) {
                    first.add(Calendar.MONTH, 1);
                }
                alignDayOfWeek(first);
            }
            return first;
        }

        /** Returns the day of week in this expression.
         * @return The day of week in this expression
         */
        public int getDayOfWeek() {
            return this.dayOfWeek;
        }

        /** Returns the occurrence in this expression.
         * @return The occurrence in this expression
         */
        public int getOccurrence() {
            return this.occurrence;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            if (cal.get(Calendar.DAY_OF_WEEK) != this.dayOfWeek) {
                return false;
            }
            int month = cal.get(Calendar.MONTH);
            int dom = cal.get(Calendar.DAY_OF_MONTH);
            Calendar next = (Calendar) cal.clone();
            alignDayOfWeek(next);
            return dom == next.get(Calendar.DAY_OF_MONTH) && next.get(Calendar.MONTH) == month;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.DAY_OF_MONTH, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.DAY_OF_MONTH, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            int month = cal.get(Calendar.MONTH);
            Calendar next = alignDayOfWeek((Calendar) cal.clone());
            if (next.before(cal) || next.equals(cal)) {
                next.set(Calendar.DAY_OF_MONTH, 1);
                if (next.get(Calendar.MONTH) == month) {
                    next.add(Calendar.MONTH, 1);
                }
                alignDayOfWeek(next);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", dayOfWeek = " + this.dayOfWeek + ", occurrence = " + this.occurrence;
        }
    }

    /** A temporal expression that represents a day of month range. */
    public static class DayOfMonthRange extends TemporalExpression {
        private static final String MODULE = DayOfMonthRange.class.getName();
        private final int end;
        private final int start;

        public DayOfMonthRange(int dom) {
            this(dom, dom);
        }

        /**
         * @param start An integer in the range of 1 to 31
         * @param end An integer in the range of 1 to 31
         */
        public DayOfMonthRange(int start, int end) {
            if (start < 1 || start > end) {
                throw new IllegalArgumentException("Invalid start argument");
            }
            if (end < 1 || end > 31) {
                throw new IllegalArgumentException("Invalid end argument");
            }
            this.setSequence(SEQUENCE_DOM_RANGE + start);
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    DayOfMonthRange that = (DayOfMonthRange) obj;
                    return this.start == that.start && this.end == that.end;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            while (!includesDate(first)) {
                first.add(Calendar.DAY_OF_MONTH, 1);
            }
            return first;
        }

        /** Returns the ending day of this range.
         * @return The ending day of this range
         */
        public int getEndDay() {
            return this.end;
        }

        /** Returns the starting day of this range.
         * @return The starting day of this range
         */
        public int getStartDay() {
            return this.start;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int dom = cal.get(Calendar.DAY_OF_MONTH);
            return dom >= this.start && dom <= this.end;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.DAY_OF_MONTH, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.DAY_OF_MONTH, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            next.add(Calendar.DAY_OF_MONTH, 1);
            while (!includesDate(next)) {
                next.add(Calendar.DAY_OF_MONTH, 1);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }
    }

    /** A temporal expression that represents a day of week range. */
    public static class DayOfWeekRange extends TemporalExpression {
        private static final String MODULE = DayOfWeekRange.class.getName();
        private final int end;
        private final int start;

        public DayOfWeekRange(int dow) {
            this(dow, dow);
        }

        /**
         * @param start An integer in the range of <code>Calendar.SUNDAY</code>
         * to <code>Calendar.SATURDAY</code>
         * @param end An integer in the range of <code>Calendar.SUNDAY</code>
         * to <code>Calendar.SATURDAY</code>
         */
        public DayOfWeekRange(int start, int end) {
            if (start < Calendar.SUNDAY || start > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Invalid start argument");
            }
            if (end < Calendar.SUNDAY || end > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Invalid end argument");
            }
            this.setSequence(SEQUENCE_DOW_RANGE + start);
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    DayOfWeekRange that = (DayOfWeekRange) obj;
                    return this.start == that.start && this.end == that.end;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            while (!includesDate(first)) {
                first.add(Calendar.DAY_OF_MONTH, 1);
            }
            return first;
        }

        /** Returns the ending day of this range.
         * @return The ending day of this range
         */
        public int getEndDay() {
            return this.end;
        }

        /** Returns the starting day of this range.
         * @return The starting day of this range
         */
        public int getStartDay() {
            return this.start;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            if (dow == this.start || dow == this.end) {
                return true;
            }
            Calendar compareCal = (Calendar) cal.clone();
            while (compareCal.get(Calendar.DAY_OF_WEEK) != this.start) {
                compareCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            while (compareCal.get(Calendar.DAY_OF_WEEK) != this.end) {
                if (compareCal.get(Calendar.DAY_OF_WEEK) == dow) {
                    return true;
                }
                compareCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return false;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.DAY_OF_MONTH, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.DAY_OF_MONTH, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            if (includesDate(next)) {
                if (context.isDayBumped()) {
                    context.setDayBumped(false);
                    return next;
                }
                next.add(Calendar.DAY_OF_MONTH, 1);
            }
            while (!includesDate(next)) {
                next.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (cal.get(Calendar.MONTH) != next.get(Calendar.MONTH)) {
                context.setMonthBumped(true);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }
    }

    /** A temporal expression that represents a difference of two temporal expressions. */
    public static class Difference extends TemporalExpression {
        private static final String MODULE = Difference.class.getName();
        private final TemporalExpression excluded;
        private final TemporalExpression included;

        public Difference(TemporalExpression included, TemporalExpression excluded) {
            if (included == null) {
                throw new IllegalArgumentException("included argument cannot be null");
            }
            this.included = included;
            this.excluded = excluded;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            this.setSequence(included.getSequence());
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        protected boolean containsExpression(TemporalExpression expression) {
            return this.included.containsExpression(expression) || this.excluded.containsExpression(expression);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((excluded == null) ? 0 : excluded.hashCode());
            result = prime * result + ((included == null) ? 0 : included.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    Difference that = (Difference) obj;
                    return this.included.equals(that.included) && this.excluded.equals(that.excluded);
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = this.included.first(cal);
            while (first != null && this.excluded.includesDate(first)) {
                first = this.included.next(first);
            }
            return first;
        }

        /** Returns the excluded expression.
         * @return The excluded <code>TemporalExpression</code>
         */
        public TemporalExpression getExcluded() {
            return this.excluded;
        }

        /** Returns the included expression.
         * @return The included <code>TemporalExpression</code>
         */
        public TemporalExpression getIncluded() {
            return this.included;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            return this.included.includesDate(cal) && !this.excluded.includesDate(cal);
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            return this.included.isSubstitutionCandidate(cal, expressionToTest) && !this.excluded.isSubstitutionCandidate(cal, expressionToTest);
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = this.included.next(cal, context);
            while (next != null && this.excluded.includesDate(next)) {
                next = this.included.next(next, context);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", included = " + this.included + ", excluded = " + this.excluded;
        }
    }

    /* A temporal expression that represents a frequency. */
    public static class Frequency extends TemporalExpression {
        private static final String MODULE = Frequency.class.getName();
        private final int freqCount;
        private final int freqType;
        private final Date start;

        /**
         * @param start Starting date, defaults to current system time
         * @param freqType One of the following integer values: <code>Calendar.SECOND
         * Calendar.MINUTE Calendar.HOUR Calendar.DAY_OF_MONTH Calendar.MONTH
         * Calendar.YEAR</code>
         * @param freqCount A positive integer
         */
        public Frequency(Date start, int freqType, int freqCount) {
            if (freqType != Calendar.SECOND && freqType != Calendar.MINUTE
                    && freqType != Calendar.HOUR && freqType != Calendar.DAY_OF_MONTH
                    && freqType != Calendar.MONTH && freqType != Calendar.YEAR) {
                throw new IllegalArgumentException("Invalid freqType argument");
            }
            if (freqCount < 1) {
                throw new IllegalArgumentException("freqCount argument must be a positive integer");
            }
            if (start != null) {
                this.start = (Date) start.clone();
            } else {
                this.start = new Date();
            }
            this.setSequence(SEQUENCE_FREQ + freqType);
            this.freqType = freqType;
            this.freqCount = freqCount;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + freqCount;
            result = prime * result + freqType;
            result = prime * result + ((start == null) ? 0 : start.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    Frequency that = (Frequency) obj;
                    return this.start.equals(that.start) && this.freqType == that.freqType && this.freqCount == that.freqCount;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = prepareCal(cal);
            while (first.before(cal)) {
                first.add(this.freqType, this.freqCount);
            }
            return first;
        }

        /** Returns the frequency count of this expression.
         * @return The frequency count of this expression
         */
        public int getFreqCount() {
            return this.freqCount;
        }

        /** Returns the frequency type of this expression.
         * @return The frequency type of this expression
         */
        public int getFreqType() {
            return this.freqType;
        }

        /** Returns the start date of this expression.
         * @return The start date of this expression
         */
        public Date getStartDate() {
            return (Date) this.start.clone();
        }

        @Override
        public boolean includesDate(Calendar cal) {
            Calendar next = first(cal);
            return next.equals(cal);
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(this.freqType, -this.freqCount);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(this.freqType, -this.freqCount);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = first(cal);
            if (next.equals(cal)) {
                next.add(this.freqType, this.freqCount);
            }
            return next;
        }

        /**
         * Prepare cal calendar.
         * @param cal the cal
         * @return the calendar
         */
        protected Calendar prepareCal(Calendar cal) {
            // Performs a "sane" skip forward in time - avoids time consuming loops
            // like incrementing every second from Jan 1 2000 until today
            Calendar skip = (Calendar) cal.clone();
            skip.setTime(this.start);
            long deltaMillis = cal.getTimeInMillis() - this.start.getTime();
            if (deltaMillis < 1000) {
                return skip;
            }
            long divisor = deltaMillis;
            if (this.freqType == Calendar.DAY_OF_MONTH) {
                divisor = 86400000;
            } else if (this.freqType == Calendar.HOUR) {
                divisor = 3600000;
            } else if (this.freqType == Calendar.MINUTE) {
                divisor = 60000;
            } else if (this.freqType == Calendar.SECOND) {
                divisor = 1000;
            } else {
                return skip;
            }
            long units = deltaMillis / divisor;
            units -= units % this.freqCount;
            skip.add(this.freqType, (int) units);
            while (skip.after(cal)) {
                skip.add(this.freqType, -this.freqCount);
            }
            return skip;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", freqType = " + this.freqType + ", freqCount = " + this.freqCount;
        }
    }


    /** A temporal expression that represents an hour range. */
    public static class HourRange extends TemporalExpression {
        private static final String MODULE = HourRange.class.getName();
        private final int end;
        private final int start;

        /**
         * @param hour An integer in the range of 0 to 23.
         */
        public HourRange(int hour) {
            this(hour, hour);
        }

        /**
         * @param start An integer in the range of 0 to 23.
         * @param end An integer in the range of 0 to 23.
         */
        public HourRange(int start, int end) {
            if (start < 0 || start > 23) {
                throw new IllegalArgumentException("Invalid start argument");
            }
            if (end < 0 || end > 23) {
                throw new IllegalArgumentException("Invalid end argument");
            }
            this.start = start;
            this.end = end;
            this.setSequence(SEQUENCE_HOUR_RANGE + start);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    HourRange that = (HourRange) obj;
                    return this.start == that.start && this.end == that.end;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            while (!includesDate(first)) {
                first.add(Calendar.HOUR_OF_DAY, 1);
            }
            return first;
        }

        /** Returns the ending hour of this range.
         * @return The ending hour of this range
         */
        public int getEndHour() {
            return this.end;
        }

        /**
         * Gets hour range as set.
         * @return the hour range as set
         */
        public Set<Integer> getHourRangeAsSet() {
            Set<Integer> rangeSet = new TreeSet<>();
            if (this.start == this.end) {
                rangeSet.add(this.start);
            } else {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, this.start);
                while (cal.get(Calendar.HOUR_OF_DAY) != this.end) {
                    rangeSet.add(cal.get(Calendar.HOUR_OF_DAY));
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                }
            }
            return rangeSet;
        }

        /** Returns the starting hour of this range.
         * @return The starting hour of this range
         */
        public int getStartHour() {
            return this.start;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour == this.start || hour == this.end) {
                return true;
            }
            Calendar compareCal = (Calendar) cal.clone();
            compareCal.set(Calendar.HOUR_OF_DAY, this.start);
            while (compareCal.get(Calendar.HOUR_OF_DAY) != this.end) {
                if (compareCal.get(Calendar.HOUR_OF_DAY) == hour) {
                    return true;
                }
                compareCal.add(Calendar.HOUR_OF_DAY, 1);
            }
            return false;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.HOUR_OF_DAY, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.HOUR_OF_DAY, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            if (includesDate(next)) {
                if (context.isHourBumped()) {
                    return next;
                }
                next.add(Calendar.HOUR_OF_DAY, 1);
            }
            while (!includesDate(next)) {
                next.add(Calendar.HOUR_OF_DAY, 1);
            }
            if (cal.get(Calendar.DAY_OF_MONTH) != next.get(Calendar.DAY_OF_MONTH)) {
                context.setDayBumped(true);
                if (cal.get(Calendar.MONTH) != next.get(Calendar.MONTH)) {
                    context.setMonthBumped(true);
                }
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }
    }

    /** A temporal expression that represents a mathematical intersection of all of its
     * member expressions. */
    public static class Intersection extends TemporalExpression {
        private static final String MODULE = Intersection.class.getName();
        private final Set<TemporalExpression> expressionSet;

        public Intersection(Set<TemporalExpression> expressionSet) {
            if (expressionSet == null) {
                throw new IllegalArgumentException("expressionSet argument cannot be null");
            }
            this.expressionSet = expressionSet;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            if (!this.expressionSet.isEmpty()) {
                // Aggregate member expression sequences in a way that will
                // ensure the proper evaluation sequence for the entire collection
                int result = 0;
                TemporalExpression[] exprArray = this.expressionSet.toArray(new TemporalExpression[this.expressionSet.size()]);
                for (int i = exprArray.length - 1; i >= 0; i--) {
                    result *= 10;
                    result += exprArray[i].getSequence();
                }
                this.setSequence(result);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        protected boolean containsExpression(TemporalExpression expression) {
            for (TemporalExpression setItem : this.expressionSet) {
                if (setItem.containsExpression(expression)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((expressionSet == null) ? 0 : expressionSet.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    return this.expressionSet.equals(((Intersection) obj).expressionSet);
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            for (TemporalExpression expression : this.expressionSet) {
                first = expression.first(first);
                if (first == null) {
                    return null;
                }
            }
            if (includesDate(first)) {
                return first;
            }
            return null;
        }

        /** Returns the member expression <code>Set</code>. The
         * returned set is unmodifiable.
         * @return The member expression <code>Set</code>
         */
        public Set<TemporalExpression> getExpressionSet() {
            return Collections.unmodifiableSet(this.expressionSet);
        }

        @Override
        public boolean includesDate(Calendar cal) {
            for (TemporalExpression expression : this.expressionSet) {
                if (!expression.includesDate(cal)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            for (TemporalExpression expression : this.expressionSet) {
                if (!expression.isSubstitutionCandidate(cal, expressionToTest)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            for (TemporalExpression expression : this.expressionSet) {
                next = expression.next(next, context);
                if (next == null) {
                    return null;
                }
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", size = " + this.expressionSet.size();
        }
    }

    /** A temporal expression that represents a minute range. */
    public static class MinuteRange extends TemporalExpression {
        private static final String MODULE = MinuteRange.class.getName();
        private final int end;
        private final int start;

        /**
         * @param minute An integer in the range of 0 to 59.
         */
        public MinuteRange(int minute) {
            this(minute, minute);
        }

        /**
         * @param start An integer in the range of 0 to 59.
         * @param end An integer in the range of 0 to 59.
         */
        public MinuteRange(int start, int end) {
            if (start < 0 || start > 59) {
                throw new IllegalArgumentException("Invalid start argument");
            }
            if (end < 0 || end > 59) {
                throw new IllegalArgumentException("Invalid end argument");
            }
            this.start = start;
            this.end = end;
            this.setSequence(SEQUENCE_MINUTE_RANGE + start);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    MinuteRange that = (MinuteRange) obj;
                    return this.start == that.start && this.end == that.end;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            while (!includesDate(first)) {
                first.add(Calendar.MINUTE, 1);
            }
            return first;
        }

        /** Returns the ending minute of this range.
         * @return The ending minute of this range
         */
        public int getEndMinute() {
            return this.end;
        }

        /**
         * Gets minute range as set.
         * @return the minute range as set
         */
        public Set<Integer> getMinuteRangeAsSet() {
            Set<Integer> rangeSet = new TreeSet<>();
            if (this.start == this.end) {
                rangeSet.add(this.start);
            } else {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, this.start);
                while (cal.get(Calendar.HOUR_OF_DAY) != this.end) {
                    rangeSet.add(cal.get(Calendar.HOUR_OF_DAY));
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                }
            }
            return rangeSet;
        }

        /** Returns the starting minute of this range.
         * @return The starting minute of this range
         */
        public int getStartMinute() {
            return this.start;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int minute = cal.get(Calendar.MINUTE);
            if (minute == this.start || minute == this.end) {
                return true;
            }
            Calendar compareCal = (Calendar) cal.clone();
            compareCal.set(Calendar.MINUTE, this.start);
            while (compareCal.get(Calendar.MINUTE) != this.end) {
                if (compareCal.get(Calendar.MINUTE) == minute) {
                    return true;
                }
                compareCal.add(Calendar.MINUTE, 1);
            }
            return false;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.MINUTE, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.MINUTE, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            if (includesDate(next)) {
                next.add(Calendar.MINUTE, 1);
            }
            while (!includesDate(next)) {
                next.add(Calendar.MINUTE, 1);
            }
            if (cal.get(Calendar.HOUR_OF_DAY) != next.get(Calendar.HOUR_OF_DAY)) {
                context.setHourBumped(true);
                if (cal.get(Calendar.DAY_OF_MONTH) != next.get(Calendar.DAY_OF_MONTH)) {
                    context.setDayBumped(true);
                    if (cal.get(Calendar.MONTH) != next.get(Calendar.MONTH)) {
                        context.setMonthBumped(true);
                    }
                }
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }
    }

    /** A temporal expression that represents a month range. */
    public static class MonthRange extends TemporalExpression {
        private static final String MODULE = MonthRange.class.getName();
        private final int end;
        private final int start;

        public MonthRange(int month) {
            this(month, month);
        }

        /**
         * @param start An integer in the range of <code>Calendar.JANUARY</code>
         * to <code>Calendar.UNDECIMBER</code>
         * @param end An integer in the range of <code>Calendar.JANUARY</code>
         * to <code>Calendar.UNDECIMBER</code>
         */
        public MonthRange(int start, int end) {
            if (start < Calendar.JANUARY || start > Calendar.UNDECIMBER) {
                throw new IllegalArgumentException("Invalid start argument");
            }
            if (end < Calendar.JANUARY || end > Calendar.UNDECIMBER) {
                throw new IllegalArgumentException("Invalid end argument");
            }
            this.setSequence(SEQUENCE_MONTH_RANGE + start);
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + end;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    MonthRange that = (MonthRange) obj;
                    return this.start == that.start && this.end == that.end;
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            first.set(Calendar.DAY_OF_MONTH, 1);
            while (!includesDate(first)) {
                first.add(Calendar.MONTH, 1);
            }
            return first;
        }

        /** Returns the ending month of this range.
         * @return The ending month of this range
         */
        public int getEndMonth() {
            return this.end;
        }

        /** Returns the starting month of this range.
         * @return The starting month of this range
         */
        public int getStartMonth() {
            return this.start;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int month = cal.get(Calendar.MONTH);
            if (month == this.start || month == this.end) {
                return true;
            }
            Calendar compareCal = (Calendar) cal.clone();
            while (compareCal.get(Calendar.MONTH) != this.start) {
                compareCal.add(Calendar.MONTH, 1);
            }
            while (compareCal.get(Calendar.MONTH) != this.end) {
                if (compareCal.get(Calendar.MONTH) == month) {
                    return true;
                }
                compareCal.add(Calendar.MONTH, 1);
            }
            return false;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            Calendar checkCal = (Calendar) cal.clone();
            checkCal.add(Calendar.MONTH, -1);
            while (!includesDate(checkCal)) {
                if (expressionToTest.includesDate(checkCal)) {
                    return true;
                }
                checkCal.add(Calendar.MONTH, -1);
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = (Calendar) cal.clone();
            next.set(Calendar.DAY_OF_MONTH, 1);
            next.add(Calendar.MONTH, 1);
            while (!includesDate(next)) {
                next.add(Calendar.MONTH, 1);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }
    }

    /** A temporal expression that represents a null expression. */
    public static class Null extends TemporalExpression {
        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }
        @Override
        public Calendar first(Calendar cal) {
            return null;
        }
        @Override
        public boolean includesDate(Calendar cal) {
            return false;
        }
        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            return false;
        }
        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            return null;
        }
    }

    /** A temporal expression that provides a substitution for an excluded temporal expression. */
    public static class Substitution extends TemporalExpression {
        private static final String MODULE = Substitution.class.getName();
        private final TemporalExpression excluded;
        private final TemporalExpression included;
        private final TemporalExpression substitute;

        public Substitution(TemporalExpression included, TemporalExpression excluded, TemporalExpression substitute) {
            if (included == null) {
                throw new IllegalArgumentException("included argument cannot be null");
            }
            if (excluded == null) {
                throw new IllegalArgumentException("excluded argument cannot be null");
            }
            if (substitute == null) {
                throw new IllegalArgumentException("substitute argument cannot be null");
            }
            this.included = included;
            this.excluded = excluded;
            this.substitute = substitute;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            this.setSequence(included.getSequence());
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        protected boolean containsExpression(TemporalExpression expression) {
            return this.included.containsExpression(expression) || this.excluded.containsExpression(expression);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((excluded == null) ? 0 : excluded.hashCode());
            result = prime * result + ((included == null) ? 0 : included.hashCode());
            result = prime * result + ((substitute == null) ? 0 : substitute.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    Substitution that = (Substitution) obj;
                    return this.included.equals(that.included) && this.excluded.equals(that.excluded) && this.substitute.equals(that.substitute);
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = this.included.first(cal);
            if (first != null && this.excluded.includesDate(first)) {
                first = this.substitute.first(first);
            }
            return first;
        }

        /** Returns the excluded expression.
         * @return The excluded <code>TemporalExpression</code>
         */
        public TemporalExpression getExcluded() {
            return this.excluded;
        }

        /** Returns the included expression.
         * @return The included <code>TemporalExpression</code>
         */
        public TemporalExpression getIncluded() {
            return this.included;
        }

        /** Returns the substitute expression.
         * @return The substitute <code>TemporalExpression</code>
         */
        public TemporalExpression getSubstitute() {
            return this.substitute;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            return this.included.includesDate(cal) || this.substitute.isSubstitutionCandidate(cal, this.excluded);
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            return this.substitute.isSubstitutionCandidate(cal, expressionToTest);
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar next = this.included.next(cal, context);
            if (next != null && this.excluded.includesDate(next)) {
                next = this.substitute.next(next, context);
            }
            return next;
        }

        @Override
        public String toString() {
            return super.toString() + ", included = " + this.included + ", excluded = " + this.excluded + ", substitute = " + this.substitute;
        }
    }

    /** A temporal expression that represents a mathematical union of all of its
     * member expressions. */
    public static class Union extends TemporalExpression {
        private static final String MODULE = Union.class.getName();
        private final Set<TemporalExpression> expressionSet;

        public Union(Set<TemporalExpression> expressionSet) {
            if (expressionSet == null) {
                throw new IllegalArgumentException("expressionSet argument cannot be null");
            }
            this.expressionSet = expressionSet;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            if (!this.expressionSet.isEmpty()) {
                TemporalExpression that = this.expressionSet.iterator().next();
                this.setSequence(that.getSequence());
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, MODULE);
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        protected boolean containsExpression(TemporalExpression expression) {
            for (TemporalExpression setItem : this.expressionSet) {
                if (setItem.containsExpression(expression)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((expressionSet == null) ? 0 : expressionSet.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj == this) {
                    return true;
                }
                try {
                    return this.expressionSet.equals(((Union) obj).expressionSet);
                } catch (ClassCastException e) {
                    Debug.logInfo(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        @Override
        public Calendar first(Calendar cal) {
            for (TemporalExpression expression : this.expressionSet) {
                Calendar first = expression.first(cal);
                if (first != null && includesDate(first)) {
                    return first;
                }
            }
            return null;
        }

        /** Returns the member expression <code>Set</code>. The
         * returned set is unmodifiable.
         * @return The member expression <code>Set</code>
         */
        public Set<TemporalExpression> getExpressionSet() {
            return Collections.unmodifiableSet(this.expressionSet);
        }

        @Override
        public boolean includesDate(Calendar cal) {
            for (TemporalExpression expression : this.expressionSet) {
                if (expression.includesDate(cal)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isSubstitutionCandidate(Calendar cal, TemporalExpression expressionToTest) {
            for (TemporalExpression expression : this.expressionSet) {
                if (expression.isSubstitutionCandidate(cal, expressionToTest)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Calendar next(Calendar cal, ExpressionContext context) {
            Calendar result = null;
            for (TemporalExpression expression : this.expressionSet) {
                Calendar next = expression.next(cal, context);
                if (next != null) {
                    if (result == null || next.before(result)) {
                        result = next;
                    }
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return super.toString() + ", size = " + this.expressionSet.size();
        }
    }
}
