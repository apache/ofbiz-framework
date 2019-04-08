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
package org.apache.ofbiz.base.lang.test;

import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.lang.ComparableRange;
import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.util.UtilGenerics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SourceMonitored
public class ComparableRangeTests extends GenericTestCaseBase {

    public ComparableRangeTests(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    private static <L extends Comparable<L>, R extends Comparable<R>> void comparableRangeConstructorTest(L left, R right) {
        new ComparableRange<L>(left, left);
        new ComparableRange<R>(right, right);
        IllegalArgumentException caught = null;
        try {
            new ComparableRange(left, right);
        } catch (IllegalArgumentException e) {
            caught = e;
        } finally {
            assertNotNull("expected exception", caught);
        }
        caught = null;
        try {
            new ComparableRange(right, left);
        } catch (IllegalArgumentException e) {
            caught = e;
        } finally {
            assertNotNull("expected exception", caught);
        }
    }

    private static <T extends Comparable<T>, B extends Comparable<B>> void comparableRangeTest(String label, B bad, T a, T b, T c, T d) {
        comparableRangeConstructorTest(bad, a);
        assertTrue(label + ":a-isPoint", new ComparableRange<T>(a, a).isPoint());
        assertTrue(label + ":b-isPoint", new ComparableRange<T>(b, b).isPoint());
        assertTrue(label + ":c-isPoint", new ComparableRange<T>(c, c).isPoint());
        ComparableRange<T> first = new ComparableRange<T>(a, b);
        ComparableRange<T> second = new ComparableRange<T>(c, d);
        ComparableRange<T> all = new ComparableRange<T>(a, d);
        assertEquals(label + ":a-b toString", a + " - " + b, first.toString());
        assertEquals(label + ":c-d toString", c + " - " + d, second.toString());
        assertEquals(label + ":a-d toString", a + " - " + d, all.toString());
        assertFalse(label + ":a-b isPoint", first.isPoint());
        assertFalse(label + ":c-d isPoint", second.isPoint());
        assertFalse(label + ":a-d isPoint", all.isPoint());
        assertEquals(label + ":a-b == a-b", first, first);
        assertEquals(label + ":a-b.compareTo(a-b)", 0, first.compareTo(first));
        assertEquals(label + ":a-b equals a-b", first, new ComparableRange<T>(a, b));
        assertEquals(label + ":a-b.compareTo(new a-b)", 0, first.compareTo(new ComparableRange<T>(a, b)));
        assertEquals(label + ":a-b equals b-a", first, new ComparableRange<T>(b, a));
        assertEquals(label + ":a-b.compareTo(new b-a)", 0, first.compareTo(new ComparableRange<T>(b, a)));
        assertNotEquals(label + ":a-b not-equal other", first, ComparableRangeTests.class);
        assertFalse(label + ":a-d equals null", all.equals(null));
        ClassCastException caught = null;
        try {
            UtilGenerics.<Comparable<Object>>cast(first).compareTo(ComparableRangeTests.class);
        } catch (ClassCastException e) {
            caught = e;
        } finally {
            assertNotNull(label + " compareTo CCE", caught);
        }
        assertNotEquals(label + ":a-a != a-b", new ComparableRange<T>(a, a), first);
        assertThat(label + ":a-a.compareTo(a-b) < 0", 0, greaterThan(new ComparableRange<T>(a, a).compareTo(first)));
        assertNotEquals(label + ":a-a != c-d", new ComparableRange<T>(a, a), second);
        assertThat(label + ":a-a.compareTo(c-d) < 0", 0, greaterThan(new ComparableRange<T>(a, a).compareTo(second)));
        assertNotEquals(label + ":a-a != a-d", new ComparableRange<T>(a, a), all);
        assertThat(label + ":a-a.compareTo(a-d) < 0", 0, greaterThan(new ComparableRange<T>(a, a).compareTo(all)));
        assertTrue(label + ":b-c after a-b", second.after(first));
        assertThat(label + ":b-c.compareTo(a-b)", 0, lessThan(second.compareTo(first)));
        assertFalse(label + ":c-d !after c-d", second.after(second));
        assertEquals(label + ":c-d.compareTo(c-d)", 0, second.compareTo(second));
        assertTrue(label + ":a-b before c-d", first.before(second));
        assertThat(label + ":a-b.compareTo(c-d)", 0, greaterThan(first.compareTo(second)));
        assertFalse(label + ":a-b !before a-b", first.before(first));
        assertEquals(label + ":a-b.compareTo(a-b)", 0, first.compareTo(first));
        assertTrue(label + ":a-d includes a-b", all.includes(first));
        assertTrue(label + ":a-b overlaps b-c", first.overlaps(new ComparableRange<T>(b, c)));
        assertTrue(label + ":b-c overlaps c-d", new ComparableRange<T>(b, c).overlaps(second));
        assertTrue(label + ":a-b overlaps a-d", first.overlaps(all));
        assertTrue(label + ":a-d overlaps a-b", all.overlaps(first));
        assertTrue(label + ":a-d overlaps b-c", all.overlaps(new ComparableRange<T>(b, c)));
        assertTrue(label + ":b-c overlaps a-d", new ComparableRange<T>(b, c).overlaps(all));
        assertFalse(label + ":a-b overlaps c-d", first.overlaps(second));
        assertFalse(label + ":c-d overlaps a-b", second.overlaps(first));
        assertTrue(label + ":a-b includes a", first.includes(a));
        assertTrue(label + ":a-b includes b", first.includes(b));
        assertFalse(label + ":a-b includes c", first.includes(c));
        assertFalse(label + ":a includes a-b", new ComparableRange<T>(a, a).includes(first));
        assertTrue(label + ":c-d after a", second.after(a));
        assertTrue(label + ":c-d after b", second.after(b));
        assertFalse(label + ":c-d after c", second.after(c));
        assertFalse(label + ":c-d after d", second.after(d));
        assertFalse(label + ":a-b after a", first.before(a));
        assertFalse(label + ":a-b after b", first.before(b));
        assertTrue(label + ":a-b after c", first.before(c));
        assertTrue(label + ":a-b after d", first.before(d));
    }

    public void testComparableRange() {
        comparableRangeTest("integer", 20L, 1, 2, 3, 4);
    }
}
