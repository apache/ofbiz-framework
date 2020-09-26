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
package org.apache.ofbiz.base.test;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.TestCase;

// This class can not use any other ofbiz helper methods, because it
// may be used to test those helper methods.
public abstract class GenericTestCaseBase extends TestCase {
    protected GenericTestCaseBase(String name) {
        super(name);
    }

    public static void useAllMemory() throws Exception {
        List<long[]> dummy = new LinkedList<>();
        try {
            do {
                dummy.add(new long[1048576]);
            } while (true);
        } catch (OutOfMemoryError e) {
            System.gc();
            Thread.sleep(100);
        }
    }

    public static void assertStaticHelperClass(Class<?> clz) throws Exception {
        Constructor<?>[] constructors = clz.getDeclaredConstructors();
        assertEquals(clz.getName() + " constructor count", 1, constructors.length);
        assertEquals(clz.getName() + " private declared constructor", 1 << Constructor.DECLARED, constructors[0].getModifiers()
                & ~(1 << Constructor.PUBLIC) & (1 << Constructor.DECLARED));
        constructors[0].setAccessible(true);
        constructors[0].newInstance();
    }

    public static void assertComparison(String label, int wanted, int result) {
        if (wanted == 0) {
            assertEquals(label, wanted, result);
        } else {
            assertEquals(label, wanted, result / Math.abs(result));
        }
    }

    public static <V, E extends Exception> void assertFuture(String label, Future<V> future, V wanted, boolean interruptable, Class<E> thrownClass,
                                                             String thrownMessage) {
        try {
            assertEquals(label + ": future return", wanted, future.get());
        } catch (InterruptedException e) {
            assertTrue(label + ": expected interruption", interruptable);
        } catch (ExecutionException e) {
            assertNotNull(label + ": expecting an exception", thrownClass);
            Throwable caught = e.getCause();
            assertNotNull(label + ": captured exception", caught);
            assertEquals(label + ": correct thrown class", thrownClass, caught.getClass());
            if (thrownMessage != null) {
                assertEquals(label + ": exception message", thrownMessage, caught.getMessage());
            }
        }
    }

    public static void assertNotEquals(Object wanted, Object got) {
        assertNotEquals(null, wanted, got);
    }

    public static void assertNotEquals(String msg, Object wanted, Object got) {
        if (wanted == null) {
            if (got != null) {
                return;
            }
            failEquals(msg, wanted, got);
        } else if (wanted.equals(got)) {
            failEquals(msg, wanted, got);
        }
    }

    private static void failEquals(String msg, Object wanted, Object got) {
        StringBuilder sb = new StringBuilder();
        if (msg != null) {
            sb.append(msg).append(' ');
        }
        sb.append(" expected value: ").append(wanted);
        sb.append(" actual value: ").append(got);
        fail(sb.toString());
    }

    public static <T> void assertEquals(List<T> wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, List<T> wanted, Object got) {
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        if (got.getClass().isArray()) {
            assertEqualsListArray(msg, wanted, got);
            return;
        }
        if (!(got instanceof Collection<?>)) {
            fail(msg + "expected a collection, got a " + got.getClass());
        }
        Iterator<T> leftIt = wanted.iterator();
        Iterator<?> rightIt = ((Collection<?>) got).iterator();
        int i = 0;
        while (leftIt.hasNext() && rightIt.hasNext()) {
            T left = leftIt.next();
            Object right = rightIt.next();
            assertEquals(msg + "item " + i, left, right);
            i++;
        }
        assertFalse(msg + "not enough items", leftIt.hasNext());
        assertFalse(msg + "too many items", rightIt.hasNext());
    }

    public static <T> void assertEquals(Collection<T> wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, Collection<T> wanted, Object got) {
        if (wanted instanceof List<?> || wanted instanceof Set<?>) {
            // list.equals(list) and set.equals(set), see docs for Collection.equals
            if (got instanceof Set<?>) {
                fail("Not a collection, is a set");
            }
            if (got instanceof List<?>) {
                fail("Not a collection, is a list");
            }
        }
        if (wanted.equals(got)) {
            return;
        }
        if (!(got instanceof Collection<?>)) {
            fail(msg + "not a collection");
        }
        // Need to check the reverse, wanted may not implement equals,
        // which is the case for HashMap.values()
        if (got.equals(wanted)) {
            return;
        }
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        List<T> list = new ArrayList<>(wanted);
        Iterator<?> rightIt = ((Collection<?>) got).iterator();
        OUTER:
        while (rightIt.hasNext()) {
            Object right = rightIt.next();
            for (int i = 0; i < list.size(); i++) {
                T left = list.get(i);
                if (left == null) {
                    if (right == null) {
                        list.remove(i);
                        continue OUTER;
                    }
                } else if (left.equals(right)) {
                    list.remove(i);
                    continue OUTER;
                }
            }
            fail(msg + "couldn't find " + right);
        }
        if (!list.isEmpty()) {
            fail(msg + "not enough items: " + list);
        }
    }

    public static <T> void assertEquals(Set<T> wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, Set<T> wanted, Object got) {
        if (wanted.equals(got)) {
            return;
        }
        if (!(got instanceof Set<?>)) {
            fail(msg + "not a set");
        }
        // Need to check the reverse, wanted may not implement equals,
        // which is the case for HashMap.values()
        if (got.equals(wanted)) {
            return;
        }
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        Set<T> wantedSet = new HashSet<>(wanted);
        Iterator<?> rightIt = ((Set<?>) got).iterator();
        while (rightIt.hasNext()) {
            Object right = rightIt.next();
            if (wantedSet.contains(right)) {
                wantedSet.remove(right);
            } else {
                fail(msg + "couldn't find " + right);
            }
        }
        if (!wantedSet.isEmpty()) {
            fail(msg + "not enough items: " + wantedSet);
        }
    }

    private static void assertEqualsArrayArray(String msg, Object wanted, Object got) {
        int i = 0;
        while (i < Array.getLength(wanted) && i < Array.getLength(got)) {
            Object left = Array.get(wanted, i);
            Object right = Array.get(got, i);
            assertEquals(msg + "item " + i, left, right);
            i++;
        }
        assertFalse(msg + "not enough items", i < Array.getLength(wanted));
        assertFalse(msg + "too many items", i < Array.getLength(got));
    }

    private static <T> void assertEqualsArrayList(String msg, Object wanted, List<T> got) {
        Iterator<T> rightIt = got.iterator();
        int i = 0;
        while (i < Array.getLength(wanted) && rightIt.hasNext()) {
            Object left = Array.get(wanted, i);
            T right = rightIt.next();
            assertEquals(msg + "item " + i, left, right);
            i++;
        }
        assertFalse(msg + "too enough items", i < Array.getLength(wanted));
        assertFalse(msg + "not many items", rightIt.hasNext());
    }

    private static <T> void assertEqualsListArray(String msg, List<T> wanted, Object got) {
        Iterator<T> leftIt = wanted.iterator();
        int i = 0;
        while (leftIt.hasNext() && i < Array.getLength(got)) {
            T left = leftIt.next();
            Object right = Array.get(got, i);
            assertEquals(msg + "item " + i, left, right);
            i++;
        }
        assertFalse(msg + "not enough items", leftIt.hasNext());
        assertFalse(msg + "too many items", i < Array.getLength(got));
    }

    public static <V, I extends Iterable<V>> void assertEqualsIterable(String label, List<? extends V> wanted, I got) {
        assertEqualsIterable(label, wanted, 0, got, 0);
    }

    public static <V, I extends Iterable<V>> void assertEqualsIterable(String label, List<? extends V> wanted, int wantedExtra, I got, int gotExtra) {
        Iterator<? extends V> wantedIt = wanted.iterator();
        Iterator<V> gotIt = got.iterator();
        while (wantedIt.hasNext() && gotIt.hasNext()) {
            assertEquals(label + ":iterate", wantedIt.next(), gotIt.next());
        }
        while (wantedExtra > 0) {
            assertTrue(label + ":wanted-extra(" + wantedExtra + ")", wantedIt.hasNext());
            wantedExtra--;
        }
        assertFalse(label + ":wanted-done", wantedIt.hasNext());
        while (gotExtra > 0) {
            assertTrue(label + ":got-extra(" + gotExtra + ")", gotIt.hasNext());
            gotExtra--;
        }
        assertFalse(label + ":got-done", gotIt.hasNext());
    }

    public static <V, I extends Iterable<V>> void assertEqualsIterable(String label, List<? extends V> wanted, List<? extends V> wantedExtra,
                                                                       I got, List<? extends V> gotExtra) {
        assertEqualsIterable(label, wanted, wantedExtra, false, got, gotExtra, false);
    }

    public static <V, I extends Iterable<V>> void assertEqualsIterable(String label, List<? extends V> wanted, List<? extends V> wantedExtra,
                                                                       boolean removeWanted, I got, List<? extends V> gotExtra, boolean removeGot) {
        Iterator<? extends V> wantedIt = wanted.iterator();
        Iterator<V> gotIt = got.iterator();
        while (wantedIt.hasNext() && gotIt.hasNext()) {
            assertEquals(label + ":iterate", wantedIt.next(), gotIt.next());
        }
        while (!wantedExtra.isEmpty()) {
            assertTrue(label + ":wanted-extra(" + wantedExtra + ")-hasNext", wantedIt.hasNext());
            assertEquals(label + ":wanted-extra(" + wantedExtra + ")", wantedExtra.remove(0), wantedIt.next());
            if (removeWanted) {
                wantedIt.remove();
            }
        }
        assertFalse(label + ":wanted-done", wantedIt.hasNext());
        while (!gotExtra.isEmpty()) {
            assertTrue(label + ":got-extra(" + gotExtra + ")-hasNext", gotIt.hasNext());
            assertEquals(label + ":got-extra(" + gotExtra + ")", gotExtra.remove(0), gotIt.next());
            if (removeGot) {
                gotIt.remove();
            }
        }
        assertFalse(label + ":got-done", gotIt.hasNext());
    }

    public static <T> void assertEquals(Map<T, ?> wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, Map<T, ?> wanted, Object got) {
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        if (!(got instanceof Map<?, ?>)) {
            fail(msg + "expected a map");
        }
        Map<?, ?> gotMap = (Map<?, ?>) got;
        if (!got.equals(wanted)) {
            Set<T> leftKeys = new LinkedHashSet<>(wanted.keySet());
            Set<Object> rightKeys = new HashSet<>(gotMap.keySet());
            for (T key: leftKeys) {
                assertTrue(msg + "got key(" + key + ")", rightKeys.remove(key));
                assertEquals(msg + "key(" + key + ") value", wanted.get(key), gotMap.get(key));
            }
            assertTrue(msg + "extra entries", rightKeys.isEmpty());
        }
    }

    public static void assertEquals(String msg, String wanted, String got) {
        TestCase.assertEquals(msg, wanted, got);
    }

    public static void assertEquals(Object wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static void assertEquals(String msg, Object wanted, Object got) {
        if (wanted instanceof List) {
            assertEquals(msg, (List<?>) wanted, got);
        } else if (wanted instanceof Map) {
            assertEquals(msg, (Map<?, ?>) wanted, got);
        } else if (wanted == null) {
            TestCase.assertEquals(msg, wanted, got);
        } else if (wanted instanceof Set) {
            assertEquals(msg, (Set<?>) wanted, got);
        } else if (wanted instanceof Collection) {
            assertEquals(msg, (Collection<?>) wanted, got);
        } else if (wanted.getClass().isArray()) {
            if (got == null) {
                TestCase.assertEquals(msg, wanted, got);
            } else if (got.getClass().isArray()) {
                assertEqualsArrayArray(msg, wanted, got);
            } else if (got instanceof List) {
                assertEqualsArrayList(msg, wanted, (List<?>) got);
            } else {
                TestCase.assertEquals(msg, wanted, got);
            }
        } else {
            TestCase.assertEquals(msg, wanted, got);
        }
    }

    public static <T> List<T> list(T value) {
        List<T> list = new ArrayList<>(1);
        list.add(value);
        return list;
    }

    @SafeVarargs
    public static <T> List<T> list(T... list) {
        return new ArrayList<>(Arrays.asList(list));
    }

    public static <T> Set<T> set(T value) {
        Set<T> set = new HashSet<>(1);
        set.add(value);
        return set;
    }

    @SafeVarargs
    public static <T> Set<T> set(T... list) {
        return new HashSet<>(Arrays.asList(list));
    }

    public static <T> Set<T> set(Iterable<T> iterable) {
        return set(iterable.iterator());
    }

    public static <T> Set<T> set(Iterator<T> it) {
        Set<T> set = new HashSet<>();
        while (it.hasNext()) {
            T item = it.next();
            set.add(item);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... list) {
        assertEquals("list has even number of elements", 0, list.length % 2);
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < list.length; i += 2) {
            map.put((K) list[i], (V) list[i + 1]);
        }
        return map;
    }
}
