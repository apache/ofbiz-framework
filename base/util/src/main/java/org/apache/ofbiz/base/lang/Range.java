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
public interface Range<T> {
    /** Returns <code>true</code> if the lowest value in this range
     * occurs after the greatest value in <code>range</code>.
     * @param range The range to test
     * @return <code>true</code> if the lowest value in this range
     * occurs after the greatest value in <code>range</code>
     */
    boolean after(Range<T> range);

    /** Returns <code>true</code> if this range occurs after <code>value</code>.
     * @param value The value to test
     * @return <code>true</code> if this range occurs after <code>value</code>
     */
    boolean after(T value);

    /** Returns <code>true</code> if the greatest value in this range
     * occurs before the lowest value in <code>range</code>.
     * @param range The range to test
     * @return <code>true</code> if the greatest value in this range
     * occurs before the lowest value in <code>range</code>
     */
    boolean before(Range<T> range);

    /** Returns <code>true</code> if this range occurs before <code>value</code>.
     * @param value The value to test
     * @return <code>true</code> if this range occurs before <code>value</code>
     */
    boolean before(T value);

    /** Returns the ending value of this range.
     * @return Ending value
     */
    T end();

    /** Returns <code>true</code> if this range includes <code>range</code>.
     * @param range The range to test
     * @return <code>true</code> if this range includes <code>range</code>
     */
    boolean includes(Range<T> range);

    /** Returns <code>true</code> if <code>value</code> occurs within this range.
     * @param value The value to test
     * @return <code>true</code> if <code>value</code> occurs within this range
     */
    boolean includes(T value);

    /** Returns <code>true</code> if the starting and ending values are equal.
     * @return <code>true</code> if the starting and ending values are equal
     */
    boolean isPoint();

    /** Returns <code>true</code> if this range overlaps <code>range</code>.
     * @param range The range to test
     * @return <code>true</code> if this range overlaps <code>range</code>
     */
    boolean overlaps(Range<T> range);

    /** Returns the starting value of this range.
     * @return Starting value
     */
    T start();

}
