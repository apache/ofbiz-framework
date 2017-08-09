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
package org.apache.ofbiz.base.lang;

/** A range of values. */
@SourceMonitored
public class ComparableRange<T extends Comparable<T>> implements Range<T>, Comparable<ComparableRange<T>> {

    protected final T start;
    protected final T end;
    protected final boolean isPoint;

    public ComparableRange(T start, T end) {
        if (start.getClass() != end.getClass()) {
            throw new IllegalArgumentException("start Class and end Class must be the same");
        }
        if (end.compareTo(start) >= 0) {
            this.start = start;
            this.end = end;
        } else {
            this.start = end;
            this.end = start;
        }
        this.isPoint = start.equals(end);
    }

    @Override
    public boolean after(Range<T> range) {
        return this.start.compareTo(range.end()) > 0;
    }

    @Override
    public boolean after(T value) {
        return this.start.compareTo(value) > 0;
    }

    @Override
    public boolean before(Range<T> range) {
        return this.end.compareTo(range.start()) < 0;
    }

    @Override
    public boolean before(T value) {
        return this.end.compareTo(value) < 0;
    }

    @Override
    public T end() {
        return this.end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        try {
            ComparableRange<?> that = (ComparableRange<?>) obj;
            return this.start.equals(that.start()) && this.end.equals(that.end());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public int hashCode() {
        return super.hashCode();
    }

    public int compareTo(ComparableRange<T> range) {
        if (this == range) {
            return 0;
        }
        return this.start.equals(range.start()) ? this.end.compareTo(range.end()) : this.start.compareTo(range.start());
    }

    @Override
    public boolean includes(Range<T> range) {
        return this.includes(range.start()) && this.includes(range.end());
    }

    @Override
    public boolean includes(T value) {
        if (this.isPoint) {
            return value.equals(this.start());
        }
        return value.compareTo(this.start()) >= 0 && value.compareTo(this.end()) <= 0;
    }

    @Override
    public boolean isPoint() {
        return this.isPoint;
    }

    @Override
    public boolean overlaps(Range<T> range) {
        return range.includes(this.start()) || range.includes(this.end()) || this.includes(range);
    }

    @Override
    public T start() {
        return this.start;
    }

    @Override
    public String toString() {
        return this.start + " - " + this.end;
    }
}
