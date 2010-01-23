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

/** An immutable range of values. */
public class ComparableRange<T> implements Range<T> {

    @SuppressWarnings("unchecked")
    protected static <T> Comparable<T> cast(T value) {
        return (Comparable<T>) value;
    }

    protected final T start;
    protected final T end;
    protected final boolean isPoint;

    @SuppressWarnings("unchecked")
    public ComparableRange(Comparable<T> start, Comparable<T> end) {
        if (end.compareTo((T) start) >= 0) {
            this.start = (T) start;
            this.end = (T) end;
        } else {
            this.start = (T) end;
            this.end = (T) start;
        }
        this.isPoint = start.equals(end);
    }

    @Override
    public boolean after(Range<T> range) {
        return cast(this.start).compareTo(range.end()) > 0;
    }

    @Override
    public boolean after(T value) {
        return cast(this.start).compareTo(value) > 0;
    }

    @Override
    public boolean before(Range<T> range) {
        return cast(this.end).compareTo(range.start()) < 0;
    }

    @Override
    public boolean before(T value) {
        return cast(this.end).compareTo(value) < 0;
    }

    @Override
    public T end() {
        return this.end;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        try {
            ComparableRange<T> that = (ComparableRange<T>) obj;
            return this.start.equals(that.start) && this.end.equals(that.end); 
        } catch (ClassCastException e) {}
        return false;
    }

    @Override
    public boolean includes(Range<T> range) {
        return this.includes(range.start()) && this.includes(range.end());
    }

    @Override
    public boolean includes(T value) {
        if (this.isPoint) {
            return value.equals(this.start);
        }
        return (cast(value).compareTo(this.start) >= 0 && cast(value).compareTo(this.end) <= 0);
    }

    @Override
    public boolean isPoint() {
        return this.isPoint;
    }

    @Override
    public boolean overlaps(Range<T> range) {
        return range.includes(this.start) || range.includes(this.end) || this.includes(range);
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
