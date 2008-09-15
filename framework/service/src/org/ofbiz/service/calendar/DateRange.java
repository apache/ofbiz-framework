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
import java.util.Date;

/** An immutable range of dates.
 */
@SuppressWarnings("serial")
public class DateRange implements Serializable {
    public static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    public static final Date MAX_DATE = new Date(Long.MAX_VALUE);
    public static final DateRange FullRange = new DateRange();

    protected Date start = MIN_DATE;
    protected Date end = MAX_DATE;

    protected DateRange() {}

    public DateRange(Date start, Date end) {
        if (start != null) {
            this.start = start;
        }
        if (end != null) {
            this.end = end;
        }
    }

    public long durationInMillis() {
        if (this.end.after(this.start)) {
            return this.end.getTime() - this.start.getTime();
        } else {
            return this.start.getTime() - this.end.getTime();
        }
    }

    public boolean equals(Object obj) {
        return obj instanceof DateRange && ((DateRange)obj).start.equals(this.start) && ((DateRange)obj).end.equals(this.end);
    }

    public String toString() {
        return super.toString() + ", start = " + this.start + ", end = " + this.end;
    }

    public Date start() {
        return (Date) this.start.clone();
    }

    public Date end() {
        return (Date) this.end.clone();
    }

    public boolean isAscending() {
        return this.end.after(this.start) && !this.end.equals(this.start);
    }

    public boolean isPoint() {
        return this.end.equals(this.start);
    }

    public boolean includesDate(Date date) {
        if (isPoint()) {
            return date.equals(this.start);
        }
        if (isAscending()) {
            return (this.start.equals(date) || date.after(this.start)) && (this.end.equals(date) || date.before(this.end));
        } else {
            return (this.start.equals(date) || date.before(this.start)) && (this.end.equals(date) || date.after(this.end));
        }
    }

    public boolean intersectsRange(DateRange dateRange) {
        return intersectsRange(dateRange.start, dateRange.end);
    }

    public boolean intersectsRange(Date start, Date end) {
        if (start == null) {
            throw new IllegalArgumentException("start argument cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("end argument cannot be null");
        }
        if (isPoint()) {
            return end.equals(start) && this.start.equals(start);
        }
        if (isAscending()) {
            if (start.after(end)) {
                return false;
            }
            return (this.end.equals(start) || start.before(this.end)) && (this.start.equals(end) || end.after(this.start));
        } else {
            if (end.after(start)) {
                return false;
            }
            return (this.end.equals(start) || start.after(this.end)) && (this.start.equals(end) || end.before(this.start));
        }
    }
}
