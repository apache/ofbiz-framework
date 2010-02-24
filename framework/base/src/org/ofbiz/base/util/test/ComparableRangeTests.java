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
package org.ofbiz.base.util.test;

import junit.framework.TestCase;

import org.ofbiz.base.util.ComparableRange;

public class ComparableRangeTests extends TestCase {

    public ComparableRangeTests(String name) {
        super(name);
    }

    public void testComparableRange() {
        ComparableRange<Integer> pointTest = new ComparableRange<Integer>(1, 1);
        assertTrue("isPoint", pointTest.isPoint());
        assertTrue("equality", pointTest.equals(new ComparableRange<Integer>(1, 1)));
        ComparableRange<Integer> range1 = new ComparableRange<Integer>(3, 1);
        ComparableRange<Integer> range2 = new ComparableRange<Integer>(4, 6);
        assertTrue("after range", range2.after(range1));
        assertTrue("before range", range1.before(range2));
        assertFalse("excludes value", range1.includes(0));
        assertTrue("includes value", range1.includes(1));
        assertTrue("includes value", range1.includes(2));
        assertTrue("includes value", range1.includes(3));
        assertFalse("excludes value", range1.includes(4));
        assertTrue("includes range", range1.includes(pointTest));
        assertFalse("excludes range", range1.includes(range2));
        ComparableRange<Integer> overlapTest = new ComparableRange<Integer>(2, 5);
        assertTrue("overlaps range", range1.overlaps(overlapTest));
        assertTrue("overlaps range", range2.overlaps(overlapTest));
        assertFalse("does not overlap range", range1.overlaps(range2));
        IllegalArgumentException caught = null;
        try {
            @SuppressWarnings("unused")
            ComparableRange<java.util.Date> range3 = new ComparableRange<java.util.Date>(new java.util.Date(), new java.sql.Timestamp(System.currentTimeMillis()));
        } catch (IllegalArgumentException e) {
            caught = e;
        } finally {
            assertNotNull("expected exception", caught);
        }
    }
}
