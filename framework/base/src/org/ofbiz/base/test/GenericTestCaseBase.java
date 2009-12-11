package org.ofbiz.base.test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

// This class can not use any other ofbiz helper methods, because it
// may be used to test those helper methods.
public abstract class GenericTestCaseBase extends TestCase {
    protected GenericTestCaseBase(String name) {
        super(name);
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
        if (!(got instanceof Collection)) fail(msg + "expected a collection, got a " + got.getClass());
        Iterator<T> leftIt = wanted.iterator();
        Iterator rightIt = ((Collection) got).iterator();
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
        System.err.println("a");
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, Collection<T> wanted, Object got) {
        if (wanted instanceof List || wanted instanceof Set) {
            // list.equals(list) and set.equals(set), see docs for Collection.equals
            if (got instanceof Set) fail("Not a collection, is a set");
            if (got instanceof List) fail("Not a collection, is a list");
        }
        if (wanted.equals(got)) return;
        if (!(got instanceof Collection)) fail(msg + "not a collection");
        // Need to check the reverse, wanted may not implement equals,
        // which is the case for HashMap.values()
        if (got.equals(wanted)) return;
        System.err.println("b:" + wanted.getClass());
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        List<T> list = new ArrayList<T>(wanted);
        Iterator rightIt = ((Collection) got).iterator();
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
        if (!list.isEmpty()) fail(msg + "not enough items: " + list);
    }

    private static <T> void assertEqualsArrayArray(String msg, Object wanted, Object got) {
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

    public static <T> void assertEquals(Map<T, ?> wanted, Object got) {
        assertEquals(null, wanted, got);
    }

    public static <T> void assertEquals(String msg, Map<T, ?> wanted, Object got) {
        msg = msg == null ? "" : msg + ' ';
        assertNotNull(msg + "expected a value", got);
        if (!(got instanceof Map)) fail(msg + "expected a map");
        Map<?, ?> gotMap = (Map) got;
        if (!got.equals(wanted)) {
            Set<T> leftKeys = new LinkedHashSet<T>(wanted.keySet());
            HashSet<Object> rightKeys = new HashSet<Object>(gotMap.keySet());
            for(T key: leftKeys) {
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

    @SuppressWarnings("unchecked")
    public static void assertEquals(String msg, Object wanted, Object got) {
        if (wanted instanceof List) {
            assertEquals(msg, (List<?>) wanted, got);
        } else if (wanted instanceof Map) {
            assertEquals(msg, (Map<?, ?>) wanted, got);
        } else if (wanted == null) {
            TestCase.assertEquals(msg, wanted, got);
        } else if (wanted instanceof Collection) {
            System.err.println("c");
            assertEquals(msg, (Collection<?>) wanted, got);
        } else if (wanted.getClass().isArray()) {
            if (got == null) {
                TestCase.assertEquals(msg, wanted, got);
            } else if (got.getClass().isArray()) {
                assertEqualsArrayArray(msg, wanted, got);
            } else if (got instanceof List) {
                assertEqualsArrayList(msg, wanted, (List) got);
            } else {
                TestCase.assertEquals(msg, wanted, got);
            }
        } else {
            TestCase.assertEquals(msg, wanted, got);
        }
    }

    protected static <T> List<T> list(T value) {
        ArrayList<T> list = new ArrayList<T>(1);
        list.add(value);
        return list;
    }

    protected static <T> List<T> list(T... list) {
        return Arrays.asList(list);
    }

    @SuppressWarnings("unchecked")
    protected static <K, V> Map<K, V> map(Object... list) {
        assertEquals("list has even number of elements", 0, list.length % 2);
        Map<K, V> map = new LinkedHashMap<K, V>();
        for (int i = 0; i < list.length; i += 2) {
            map.put((K) list[i], (V) list[i + 1]);
        }
        return map;
    }
}
