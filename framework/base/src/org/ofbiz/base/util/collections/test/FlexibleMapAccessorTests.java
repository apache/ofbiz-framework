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
package org.ofbiz.base.util.collections.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;

@SourceMonitored
public class FlexibleMapAccessorTests extends GenericTestCaseBase {
    private static final Locale localeToTest = new Locale("en", "US");
    private static FlexibleMapAccessor<?> fmaEmpty = FlexibleMapAccessor.getInstance("");
    private static FlexibleMapAccessor<?> fmaNull = FlexibleMapAccessor.getInstance(null);

    public FlexibleMapAccessorTests(String name) {
        super(name);
    }

    private static <T> void fmaTest(String label, String getText, String fseText, T var, String value) {
        fmaTest(label, getText, getText, fseText, null, var, value);
    }

    private static <T> void fmaTest(String label, String getText, String putText, String fseText, Locale locale, T var, String value) {
        Map<String, Object> testMap = new HashMap<String, Object>();
        FlexibleMapAccessor<T> fmaGet = FlexibleMapAccessor.getInstance(getText);
        assertEquals(label + ":get-original-name", getText, fmaGet.getOriginalName());
        assertEquals(label + ":get-isEmpty", false, fmaGet.isEmpty());
        assertEquals(label + ":get-instance-equals", fmaGet, FlexibleMapAccessor.getInstance(getText));
        assertEquals(label + ":toString", getText, fmaGet.toString());
        assertNotEquals(label + ":get-not-equals-empty", fmaEmpty, fmaGet);
        assertNotEquals(label + ":get-not-equals-null", fmaNull, fmaGet);
        assertNotEquals(label + ":empty-not-equals-get", fmaGet, fmaEmpty);
        assertNotEquals(label + ":null-not-equals-get", fmaGet, fmaNull);
        assertNotEquals(label + ":get-not-equals-other", fmaGet, FlexibleMapAccessorTests.class);
        assertEquals(label + ":get-toString", getText, fmaGet.toString());
        FlexibleMapAccessor<T> fmaGetAscending = FlexibleMapAccessor.getInstance("+" + getText);
        assertEquals(label + ":get-ascending-toString", "+" + getText, fmaGetAscending.toString());
        assertTrue(label + ":get-ascending-isAscending", fmaGetAscending.getIsAscending());
        FlexibleMapAccessor<T> fmaGetDescending = FlexibleMapAccessor.getInstance("-" + getText);
        assertEquals(label + ":get-descending-toString", "-" + getText, fmaGetDescending.toString());
        assertFalse(label + ":get-decending-isAscending", fmaGetDescending.getIsAscending());
        FlexibleMapAccessor<T> fmaPut = FlexibleMapAccessor.getInstance(putText);
        assertEquals(label + ":put-toString", putText, fmaPut.toString());
        assertEquals(label + ":put-original-name", putText, fmaPut.getOriginalName());
        assertEquals(label + ":put-isEmpty", false, fmaPut.isEmpty());
        assertEquals(label + ":put-instance-equals", fmaPut, FlexibleMapAccessor.getInstance(putText));
        assertNotEquals(label + ":put-not-equals-other", fmaPut, FlexibleMapAccessorTests.class);

        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(fseText);
        if (locale == null) {
            assertNull(label + ":get-initial", fmaGet.get(testMap));
            fmaPut.put(testMap, var);
            assertFalse(label + ":testMap-not-empty", testMap.isEmpty());
            assertEquals(label + ":get", var, fmaGet.get(testMap));
            assertEquals(label, value, fse.expandString(testMap));
            assertEquals(label + ":remove", var, fmaGet.remove(testMap));
            assertNull(label + ":remove-not-exist", fmaGet.remove(testMap));
        } else {
            fmaPut.put(testMap, var);
            assertFalse(label + ":testMap-not-empty", testMap.isEmpty());
            assertEquals(label + ":get", value, fmaGet.get(testMap, locale));
            // BUG: fmaGet modifies testMap, even tho it shouldn't
            assertEquals(label + ":get", value, fmaGet.get(testMap, null));
            assertEquals(label, value, fse.expandString(testMap, locale));
        }

        testMap.clear();
        fmaPut.put(testMap, null);
        assertFalse(label + ":testMap-not-empty-put-null", testMap.isEmpty());
        if (locale == null) {
            assertNull(label + ":get-put-null", fmaGet.get(testMap));
        }
        testMap.clear();
        Exception caught = null;
        try {
            fmaPut.put(null, var);
        } catch (Exception e) {
            caught = e;
        } finally {
            assertNotNull(label + ":put-null-map", caught);
            assertTrue(label + ":put-null-map-isEmpty", testMap.isEmpty());
        }
        Set<FlexibleMapAccessor<?>> set = new HashSet<FlexibleMapAccessor<?>>();
        assertFalse(label + ":not-in-set", set.contains(fmaGet));
        set.add(fmaGet);
        assertTrue(label + ":in-set", set.contains(fmaGet));
    }

    private static void fmaEmptyTest(String label, String text) {
        FlexibleMapAccessor<Class<?>> fma = FlexibleMapAccessor.getInstance(text);
        assertTrue(label + ":isEmpty", fma.isEmpty());
        Map<String, Object> testMap = new HashMap<String, Object>();
        assertNull(label + ":get", fma.get(null));
        assertNull(label + ":get", fma.get(testMap));
        assertTrue(label + ":map-isEmpty-initial", testMap.isEmpty());
        fma.put(testMap, FlexibleMapAccessorTests.class);
        assertTrue(label + ":map-isEmpty-map", testMap.isEmpty());
        fma.put(null, FlexibleMapAccessorTests.class);
        assertTrue(label + ":map-isEmpty-null", testMap.isEmpty());
        assertSame(label + ":same-null", fmaNull, fma);
        assertSame(label + ":same-empty", fmaEmpty, fma);
        assertEquals(label + ":original-name", "", fma.getOriginalName());
        assertNull(label + ":remove", fma.remove(testMap));
        assertNotNull(label + ":toString", fma.toString());
    }

    // These tests rely upon FlexibleStringExpander, so they
    // should follow the FlexibleStringExpander tests.
    public void testFlexibleMapAccessor() {
        fmaEmptyTest("fmaEmpty", "");
        fmaEmptyTest("fmaNull", null);
        fmaEmptyTest("fma\"null\"", "null");
        fmaTest("UEL auto-vivify Map", "parameters.var", "Hello ${parameters.var}!", "World", "Hello World!");
        fmaTest("UEL auto-vivify List", "parameters.someList[0]", "parameters.someList[+0]", "Hello ${parameters.someList[0]}!", null, "World", "Hello World!");
        fmaTest("fse", "para${'meter'}s.var", "Hello ${parameters.var}!", "World", "Hello World!");
        fmaTest("foo", "'The total is ${total?currency(USD)}.'", "total", "The total is ${total?currency(USD)}.", localeToTest, new BigDecimal("12345678.90"), "The total is $12,345,678.90.");
        assertTrue("containsNestedExpression method returns true", FlexibleMapAccessor.getInstance("Hello ${parameters.var}!").containsNestedExpression());
        assertFalse("containsNestedExpression method returns false", FlexibleMapAccessor.getInstance("Hello World!").containsNestedExpression());
    }

    public static class ThrowException {
        public Object getValue() throws Exception {
            throw new Exception();
        }

        public void setValue(Object value) throws Exception {
            throw new Exception();
        }
    }

    @SuppressWarnings("serial")
    public static class CantRemoveMap<K, V> extends HashMap<K, V> {
        @Override
        public V get(Object key) {
            return super.get(key);
        }

        @Override
        public V put(K key, V value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
            return super.put(key, value);
        }
    }

    public void testVerbosityAndErrors() {
        boolean isVerbose = Debug.isOn(Debug.VERBOSE);
        try {
            Debug.set(Debug.VERBOSE, true);
            Map<String, Object> testMap = new CantRemoveMap<String, Object>();
            testMap.put("throwException", new ThrowException());
            assertNull("no var", FlexibleMapAccessor.getInstance("var").get(testMap));
            Object result = FlexibleMapAccessor.getInstance("throwException.value").get(testMap);
            assertNull("get null var", result);
            FlexibleMapAccessor.getInstance("throwException.value").put(testMap, this);
            FlexibleMapAccessor.getInstance("throwException").remove(testMap);
            assertNotNull("not removed", testMap.get("throwException"));
        } finally {
            Debug.set(Debug.VERBOSE, isVerbose);
        }
    }
}
