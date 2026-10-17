/*
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
 */
package org.apache.ofbiz.base.lang;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.junit.jupiter.api.Test;

public class ComparableRangeTests {

    private static <L extends Comparable<L>, R extends Comparable<R>> void comparableRangeConstructorTest(L left,
            R right) {
        new ComparableRange<>(left, left);
        new ComparableRange<>(right, right);
    }

    private static <T extends Comparable<T>, B extends Comparable<B>> void comparableRangeTest(String label, B bad,
            T a, T b, T c, T d) {
        comparableRangeConstructorTest(bad, a);
        assertTrue(new ComparableRange<>(a, a).isPoint(), label + ":a-isPoint");
        assertTrue(new ComparableRange<>(b, b).isPoint(), label + ":b-isPoint");
        assertTrue(new ComparableRange<>(c, c).isPoint(), label + ":c-isPoint");
        ComparableRange<T> first = new ComparableRange<>(a, b);
        ComparableRange<T> second = new ComparableRange<>(c, d);
        ComparableRange<T> all = new ComparableRange<>(a, d);
        assertEquals(a + " - " + b, first.toString(), label + ":a-b toString");
        assertEquals(c + " - " + d, second.toString(), label + ":c-d toString");
        assertEquals(a + " - " + d, all.toString(), label + ":a-d toString");
        assertFalse(first.isPoint(), label + ":a-b isPoint");
        assertFalse(second.isPoint(), label + ":c-d isPoint");
        assertFalse(all.isPoint(), label + ":a-d isPoint");
        assertEquals(first, first, label + ":a-b == a-b");
        assertEquals(0, first.compareTo(first), label + ":a-b.compareTo(a-b)");
        assertEquals(first, new ComparableRange<>(a, b), label + ":a-b equals a-b");
        assertEquals(0, first.compareTo(new ComparableRange<>(a, b)), label + ":a-b.compareTo(new a-b)");
        assertEquals(first, new ComparableRange<>(b, a), label + ":a-b equals b-a");
        assertEquals(0, first.compareTo(new ComparableRange<>(b, a)), label + ":a-b.compareTo(new b-a)");
        assertNotEquals(first, ComparableRangeTests.class, label + ":a-b not-equal other");
        assertFalse(all.equals(null), label + ":a-d equals null");
        ClassCastException caught = null;
        try {
            UtilGenerics.<Comparable<Object>>cast(first).compareTo(ComparableRangeTests.class);
        } catch (ClassCastException e) {
            caught = e;
        } finally {
            assertNotNull(caught, label + " compareTo CCE");
        }
        assertNotEquals(new ComparableRange<>(a, a), first, label + ":a-a != a-b");
        assertThat(label + ":a-a.compareTo(a-b) < 0", 0, greaterThan(new ComparableRange<>(a, a).compareTo(first)));
        assertNotEquals(new ComparableRange<>(a, a), second, label + ":a-a != c-d");
        assertThat(label + ":a-a.compareTo(c-d) < 0", 0, greaterThan(new ComparableRange<>(a, a).compareTo(second)));
        assertNotEquals(new ComparableRange<>(a, a), all, label + ":a-a != a-d");
        assertThat(label + ":a-a.compareTo(a-d) < 0", 0, greaterThan(new ComparableRange<>(a, a).compareTo(all)));
        assertTrue(second.after(first), label + ":b-c after a-b");
        assertThat(label + ":b-c.compareTo(a-b)", 0, lessThan(second.compareTo(first)));
        assertFalse(second.after(second), label + ":c-d !after c-d");
        assertEquals(0, second.compareTo(second), label + ":c-d.compareTo(c-d)");
        assertTrue(first.before(second), label + ":a-b before c-d");
        assertThat(label + ":a-b.compareTo(c-d)", 0, greaterThan(first.compareTo(second)));
        assertFalse(first.before(first), label + ":a-b !before a-b");
        assertEquals(0, first.compareTo(first), label + ":a-b.compareTo(a-b)");
        assertTrue(all.includes(first), label + ":a-d includes a-b");
        assertTrue(first.overlaps(new ComparableRange<>(b, c)), label + ":a-b overlaps b-c");
        assertTrue(new ComparableRange<>(b, c).overlaps(second), label + ":b-c overlaps c-d");
        assertTrue(first.overlaps(all), label + ":a-b overlaps a-d");
        assertTrue(all.overlaps(first), label + ":a-d overlaps a-b");
        assertTrue(all.overlaps(new ComparableRange<>(b, c)), label + ":a-d overlaps b-c");
        assertTrue(new ComparableRange<>(b, c).overlaps(all), label + ":b-c overlaps a-d");
        assertFalse(first.overlaps(second), label + ":a-b overlaps c-d");
        assertFalse(second.overlaps(first), label + ":c-d overlaps a-b");
        assertTrue(first.includes(a), label + ":a-b includes a");
        assertTrue(first.includes(b), label + ":a-b includes b");
        assertFalse(first.includes(c), label + ":a-b includes c");
        assertFalse(new ComparableRange<>(a, a).includes(first), label + ":a includes a-b");
        assertTrue(second.after(a), label + ":c-d after a");
        assertTrue(second.after(b), label + ":c-d after b");
        assertFalse(second.after(c), label + ":c-d after c");
        assertFalse(second.after(d), label + ":c-d after d");
        assertFalse(first.before(a), label + ":a-b after a");
        assertFalse(first.before(b), label + ":a-b after b");
        assertTrue(first.before(c), label + ":a-b after c");
        assertTrue(first.before(d), label + ":a-b after d");
    }

    @Test
    public void testComparableRange() {
        comparableRangeTest("integer", 20L, 1, 2, 3, 4);
    }
}
