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
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

/** A collection of TemporalExpression classes.
 * <p>For the most part, these classes are immutable - with the exception
 * of the <code>id</code> field. The basic idea is to construct an expression
 * tree in memory, and then query it.</p>
 */
@SuppressWarnings("serial")
public class TemporalExpressions implements Serializable {
    public static final String module = TemporalExpressions.class.getName();
    public static final TemporalExpression NullExpression = new Null();

    /** This class represents a null expression. */
    public static class Null extends TemporalExpression {
        @Override
        public Calendar first(Calendar cal) {
            return null;
        }
        @Override
        public boolean includesDate(Calendar cal) {
            return false;
        }
        @Override
        public Calendar next(Calendar cal) {
            return null;
        }
        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }
    }

    /** This class represents a mathematical union of all of its
     * member expressions. */
    public static class Union extends TemporalExpression {
        protected final Set<TemporalExpression> expressionSet;

        public Union(Set<TemporalExpression> expressionSet) {
            if (expressionSet == null) {
                throw new IllegalArgumentException("expressionSet argument cannot be null");
            }
            this.expressionSet = expressionSet;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            if (this.expressionSet.size() > 0) {
                TemporalExpression that = this.expressionSet.iterator().next();
                if (this.compareTo(that) > 0) {
                    this.sequence = that.sequence;
                    this.subSequence = that.subSequence;
                }
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                return this.expressionSet.equals(((Union) obj).expressionSet);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", size = " + this.expressionSet.size();
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
        public Calendar first(Calendar cal) {
            for (TemporalExpression expression : this.expressionSet) {
                Calendar first = expression.first(cal);
                if (first != null && includesDate(first)) {
                    return first;
                }
            }
            return null;
        }

        @Override
        public Calendar next(Calendar cal) {
            for (TemporalExpression expression : this.expressionSet) {
                Calendar next = expression.next(cal);
                if (next != null && includesDate(next)) {
                    return next;
                }
            }
            return null;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public Set<Date> getRange(org.ofbiz.base.util.DateRange range, Calendar cal) {
            Set<Date> rawSet = new TreeSet<Date>();
            Set<Date> finalSet = new TreeSet<Date>();
            for (TemporalExpression expression : this.expressionSet) {
                rawSet.addAll(expression.getRange(range, cal));
            }
            Calendar checkCal = (Calendar) cal.clone();
            for (Date date : rawSet) {
                checkCal.setTime(date);
                if (includesDate(checkCal)) {
                    finalSet.add(date);
                }
            }
            return finalSet;
        }

        /** Returns the member expression <code>Set</code>. The
         * returned set is unmodifiable.
         * @return The member expression <code>Set</code>
         */
        public Set<TemporalExpression> getExpressionSet() {
            return Collections.unmodifiableSet(this.expressionSet);
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
    }

    /** This class represents a mathematical intersection of all of its
     * member expressions. */
    public static class Intersection extends TemporalExpression {
        protected final Set<TemporalExpression> expressionSet;

        public Intersection(Set<TemporalExpression> expressionSet) {
            if (expressionSet == null) {
                throw new IllegalArgumentException("expressionSet argument cannot be null");
            }
            this.expressionSet = expressionSet;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            if (this.expressionSet.size() > 0) {
                TemporalExpression that = this.expressionSet.iterator().next();
                if (this.compareTo(that) > 0) {
                    this.sequence = that.sequence;
                    this.subSequence = that.subSequence;
                }
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                return this.expressionSet.equals(((Intersection) obj).expressionSet);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", size = " + this.expressionSet.size();
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
            } else {
                return null;
            }
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = (Calendar) cal.clone();
            for (TemporalExpression expression : this.expressionSet) {
                next = expression.next(next);
                if (next == null) {
                    return null;
                }
            }
            if (includesDate(next)) {
                return next;
            } else {
                return null;
            }
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public Set<Date> getRange(org.ofbiz.base.util.DateRange range, Calendar cal) {
            Set<Date> finalSet = new TreeSet<Date>();
            Set<Date> rawSet = new TreeSet<Date>();
            Date last = range.start();
            Calendar next = first(cal);
            while (next != null && range.includesDate(next.getTime())) {
                last = next.getTime();
                rawSet.add(last);
                next = next(next);
                if (next != null && last.equals(next.getTime())) {
                    break;
                }
            }
            Calendar checkCal = (Calendar) cal.clone();
            for (Date date : rawSet) {
                checkCal.setTime(date);
                if (includesDate(checkCal)) {
                    finalSet.add(date);
                }
            }
            return finalSet;
        }

        /** Returns the member expression <code>Set</code>. The
         * returned set is unmodifiable.
         * @return The member expression <code>Set</code>
         */
        public Set<TemporalExpression> getExpressionSet() {
            return Collections.unmodifiableSet(this.expressionSet);
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
    }

    /** This class represents a difference of two temporal expressions. */
    public static class Difference extends TemporalExpression {
        protected final TemporalExpression included;
        protected final TemporalExpression excluded;

        public Difference(TemporalExpression included, TemporalExpression excluded) {
            if (included == null) {
                throw new IllegalArgumentException("included argument cannot be null");
            }
            this.included = included;
            this.excluded = excluded;
            if (containsExpression(this)) {
                throw new IllegalArgumentException("recursive expression");
            }
            if (this.compareTo(included) > 0) {
                this.sequence = included.sequence;
                this.subSequence = included.subSequence;
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                Difference that = (Difference) obj;
                return this.included.equals(that.included) && this.excluded.equals(that.excluded);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", included = " + this.included + ", excluded = " + this.excluded;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            return this.included.includesDate(cal) && !this.excluded.includesDate(cal);
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = this.included.first(cal);
            while (first != null && this.excluded.includesDate(first)) {
                first = this.included.next(first);
            }
            return first;
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = this.included.next(cal);
            while (next != null && this.excluded.includesDate(next)) {
                next = this.included.next(next);
            }
            return next;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public Set<Date> getRange(org.ofbiz.base.util.DateRange range, Calendar cal) {
            Set<Date> finalSet = new TreeSet<Date>();
            Set<Date> rawSet = this.included.getRange(range, cal);
            Calendar checkCal = (Calendar) cal.clone();
            for (Date date : rawSet) {
                checkCal.setTime(date);
                if (!this.excluded.includesDate(checkCal)) {
                    finalSet.add(date);
                }
            }
            return finalSet;
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
        protected boolean containsExpression(TemporalExpression expression) {
            return this.included.containsExpression(expression) || this.excluded.containsExpression(expression);
        }
    }

    /** A temporal expression that represents a range of dates. */
    public static class DateRange extends TemporalExpression {
        protected final org.ofbiz.base.util.DateRange range;

        public DateRange(Date start, Date end) {
            this.sequence = 1000;
            this.range = new org.ofbiz.base.util.DateRange(start, end);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                return this.range.equals(((DateRange) obj).range);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.range.start() + ", end = " + this.range.end();
        }

        @Override
        public boolean includesDate(Calendar cal) {
            return this.range.includesDate(cal.getTime());
        }

        @Override
        public Calendar first(Calendar cal) {
            return includesDate(cal) ? cal : null;
        }

        @Override
        public Calendar next(Calendar cal) {
            return includesDate(cal) ? cal : null;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /** Returns the contained <code>org.ofbiz.base.util.DateRange</code>.
         * @return The contained <code>org.ofbiz.base.util.DateRange</code>
         */
        public org.ofbiz.base.util.DateRange getDateRange() {
            return this.range;
        }
    }

    /** A temporal expression that represents a time of day range. */
    public static class TimeOfDayRange extends TemporalExpression {
        protected final String startStr;
        protected final String endStr;
        protected final int interval;
        protected final int count;
        protected final int startSecs;
        protected final int startMins;
        protected final int startHrs;
        protected final int endSecs;
        protected final int endMins;
        protected final int endHrs;

        /**
         * @param start A time String in the form of hh:mm:ss (24 hr clock)
         * @param end A time String in the form of hh:mm:ss (24 hr clock)
         * @param interval The range interval - must be one of <code>
         * Calendar.SECOND Calendar.MINUTE Calendar.HOUR_OF_DAY</code>
         * @param count The interval count - must be greater than zero
         */
        public TimeOfDayRange(String start, String end, int interval, int count) {
            if (UtilValidate.isEmpty(start)) {
                throw new IllegalArgumentException("start argument cannot be null or empty");
            }
            if (UtilValidate.isEmpty(end)) {
                throw new IllegalArgumentException("end argument cannot be null or empty");
            }
            if (interval != Calendar.SECOND && interval != Calendar.MINUTE && interval != Calendar.HOUR_OF_DAY) {
                throw new IllegalArgumentException("invalid interval argument");
            }
            if (count < 1) {
                throw new IllegalArgumentException("invalid count argument");
            }
            this.startStr = start;
            this.endStr = end;
            this.interval = interval;
            this.count = count;
            String strArray[] = this.startStr.split(":");
            if (strArray.length == 0 || strArray.length > 3) {
                throw new IllegalArgumentException("Invalid start time argument");
            }
            this.startHrs = Integer.valueOf(strArray[0]);
            this.startMins = strArray.length > 1 ? Integer.valueOf(strArray[1]) : 0;
            this.startSecs = strArray.length > 2 ? Integer.valueOf(strArray[2]) : 0;
            if (this.startHrs > 23 || this.startMins > 59 || this.startSecs > 59) {
                throw new IllegalArgumentException("Invalid start time argument");
            }
            strArray = this.endStr.split(":");
            if (strArray.length == 0 || strArray.length > 3) {
                throw new IllegalArgumentException("Invalid end time argument");
            }
            this.endHrs = Integer.valueOf(strArray[0]);
            this.endMins = strArray.length > 1 ? Integer.valueOf(strArray[1]) : 0;
            this.endSecs = strArray.length > 2 ? Integer.valueOf(strArray[2]) : 0;
            if (this.endHrs > 23 || this.endMins > 59 || this.endSecs > 59) {
                throw new IllegalArgumentException("Invalid end time argument");
            }
            this.sequence = 600;
            this.subSequence = (this.startHrs * 4000) + (this.startMins * 60) + this.startSecs;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                TimeOfDayRange that = (TimeOfDayRange) obj;
                return this.startStr.equals(that.startStr) && this.endStr.equals(that.endStr);
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.startStr + ", end = " + this.endStr
            + ", interval = " + this.interval + ", count = " + this.count;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            long millis = cal.getTimeInMillis();
            Calendar startCal = setStart(cal);
            Calendar endCal = setEnd(startCal);
            if (endCal.before(startCal)) {
                endCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            long startMillis = startCal.getTimeInMillis();
            long endMillis = endCal.getTimeInMillis();
            return millis >= startMillis && millis <= endMillis;
        }

        @Override
        public Calendar first(Calendar cal) {
            if (includesDate(cal)) {
                return cal;
            }
            return next(cal);
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = (Calendar) cal.clone();
            next.add(this.interval, this.count);
            if (!includesDate(next)) {
                Calendar last = next;
                next = setStart(next);
                if (next.before(last)) {
                    next.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
            return next;
        }

        public int getCount() {
            return this.count;
        }

        public int getEndHours() {
            return this.endHrs;
        }

        public int getEndMins() {
            return this.endMins;
        }

        public int getEndSecs() {
            return this.endSecs;
        }

        public int getInterval() {
            return this.interval;
        }

        public int getStartHours() {
            return this.startHrs;
        }

        public int getStartMins() {
            return this.startMins;
        }

        public int getStartSecs() {
            return this.startSecs;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        protected Calendar setCalendar(Calendar cal, int hrs, int mins, int secs) {
            Calendar newCal = (Calendar) cal.clone();
            newCal.set(Calendar.MILLISECOND, 0);
            newCal.set(Calendar.SECOND, secs);
            newCal.set(Calendar.MINUTE, mins);
            newCal.set(Calendar.HOUR_OF_DAY, hrs);
            return newCal;
        }

        protected Calendar setStart(Calendar cal) {
            return setCalendar(cal, this.startHrs, this.startMins, this.startSecs);
        }

        protected Calendar setEnd(Calendar cal) {
            return setCalendar(cal, this.endHrs, this.endMins, this.endSecs);
        }
    }

    /** A temporal expression that represents a day of week range. */
    public static class DayOfWeekRange extends TemporalExpression {
        protected final int start;
        protected final int end;

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
            this.sequence = 500;
            this.subSequence = start;
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                DayOfWeekRange that = (DayOfWeekRange) obj;
                return this.start == that.start && this.end == that.end;
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
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
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            while (!includesDate(first)) {
                first.add(Calendar.DAY_OF_MONTH, 1);
            }
            return setStartOfDay(first);
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = (Calendar) cal.clone();
            next.add(Calendar.DAY_OF_MONTH, 1);
            while (!includesDate(next)) {
                next.add(Calendar.DAY_OF_MONTH, 1);
            }
            return setStartOfDay(next);
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /** Returns the starting day of this range.
         * @return The starting day of this range
         */
        public int getStartDay() {
            return this.start;
        }

        /** Returns the ending day of this range.
         * @return The ending day of this range
         */
        public int getEndDay() {
            return this.end;
        }
    }

    /** A temporal expression that represents a month range. */
    public static class MonthRange extends TemporalExpression {
        protected final int start;
        protected final int end;

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
            this.sequence = 200;
            this.subSequence = start;
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                MonthRange that = (MonthRange) obj;
                return this.start == that.start && this.end == that.end;
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
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
        public Calendar first(Calendar cal) {
            Calendar first = (Calendar) cal.clone();
            first.set(Calendar.DAY_OF_MONTH, 1);
            while (!includesDate(first)) {
                first.add(Calendar.MONTH, 1);
            }
            return first;
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = (Calendar) cal.clone();
            next.set(Calendar.DAY_OF_MONTH, 1);
            next.add(Calendar.MONTH, 1);
            while (!includesDate(next)) {
                next.add(Calendar.MONTH, 1);
            }
            return next;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /** Returns the starting month of this range.
         * @return The starting month of this range
         */
        public int getStartMonth() {
            return this.start;
        }

        /** Returns the ending month of this range.
         * @return The ending month of this range
         */
        public int getEndMonth() {
            return this.end;
        }
    }

    /** A temporal expression that represents a day of month range. */
    public static class DayOfMonthRange extends TemporalExpression {
        protected final int start;
        protected final int end;

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
            this.sequence = 300;
            this.subSequence = start;
            this.start = start;
            this.end = end;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                DayOfMonthRange that = (DayOfMonthRange) obj;
                return this.start == that.start && this.end == that.end;
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", end = " + this.end;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            int dom = cal.get(Calendar.DAY_OF_MONTH);
            return dom >= this.start && dom <= this.end;
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = setStartOfDay((Calendar) cal.clone());
            while (!includesDate(first)) {
                first.add(Calendar.DAY_OF_MONTH, 1);
            }
            return first;
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = setStartOfDay((Calendar) cal.clone());
            next.add(Calendar.DAY_OF_MONTH, 1);
            while (!includesDate(next)) {
                next.add(Calendar.DAY_OF_MONTH, 1);
            }
            return next;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /** Returns the starting day of this range.
         * @return The starting day of this range
         */
        public int getStartDay() {
            return this.start;
        }

        /** Returns the ending day of this range.
         * @return The ending day of this range
         */
        public int getEndDay() {
            return this.end;
        }
    }

    /** A temporal expression that represents a day in the month. */
    public static class DayInMonth extends TemporalExpression {
        protected final int dayOfWeek;
        protected final int occurrence;

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
            this.sequence = 400;
            this.subSequence = dayOfWeek;
            this.dayOfWeek = dayOfWeek;
            this.occurrence = occurrence;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                DayInMonth that = (DayInMonth) obj;
                return this.dayOfWeek == that.dayOfWeek && this.occurrence == that.occurrence;
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", dayOfWeek = " + this.dayOfWeek + ", occurrence = " + this.occurrence;
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
        public Calendar first(Calendar cal) {
            int month = cal.get(Calendar.MONTH);
            Calendar first = setStartOfDay(alignDayOfWeek((Calendar) cal.clone()));
            if (first.before(cal)) {
                first.set(Calendar.DAY_OF_MONTH, 1);
                if (first.get(Calendar.MONTH) == month) {
                    first.add(Calendar.MONTH, 1);
                }
                alignDayOfWeek(first);
            }
            return first;
        }

        @Override
        public Calendar next(Calendar cal) {
            int month = cal.get(Calendar.MONTH);
            Calendar next = setStartOfDay(alignDayOfWeek((Calendar) cal.clone()));
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
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
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
    }

    /** A temporal expression that represents a frequency. */
    public static class Frequency extends TemporalExpression {
        protected final Date start;
        protected final int freqType;
        protected final int freqCount;

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
                this.start = start;
            } else {
                this.start = new Date();
            }
            this.sequence = 100;
            this.subSequence = freqType;
            this.freqType = freqType;
            this.freqCount = freqCount;
            if (Debug.verboseOn()) {
                Debug.logVerbose("Created " + this, module);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            try {
                Frequency that = (Frequency) obj;
                return this.start.equals(that.start) && this.freqType == that.freqType && this.freqCount == that.freqCount;
            } catch (Exception e) {}
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + ", start = " + this.start + ", freqType = " + this.freqType + ", freqCount = " + this.freqCount;
        }

        @Override
        public boolean includesDate(Calendar cal) {
            Calendar next = first(cal);
            return next.equals(cal);
        }

        @Override
        public Calendar first(Calendar cal) {
            Calendar first = prepareCal(cal);
            while (first.before(cal)) {
                first.add(this.freqType, this.freqCount);
            }
            return first;
        }

        @Override
        public Calendar next(Calendar cal) {
            Calendar next = first(cal);
            if (next.equals(cal)) {
                next.add(this.freqType, this.freqCount);
            }
            return next;
        }

        @Override
        public void accept(TemporalExpressionVisitor visitor) {
            visitor.visit(this);
        }

        /** Returns the start date of this expression.
         * @return The start date of this expression
         */
        public Date getStartDate() {
            return (Date) this.start.clone();
        }

        /** Returns the frequency type of this expression.
         * @return The frequency type of this expression
         */
        public int getFreqType() {
            return this.freqType;
        }

        /** Returns the frequency count of this expression.
         * @return The frequency count of this expression
         */
        public int getFreqCount() {
            return this.freqCount;
        }

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
            float units = deltaMillis / divisor;
            units = (units / this.freqCount) * this.freqCount;
            skip.add(this.freqType, (int)units);
            while (skip.after(cal)) {
                skip.add(this.freqType, -this.freqCount);
            }
            return skip;
        }
    }
}
