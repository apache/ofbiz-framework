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
package org.apache.ofbiz.minilang.method.conditional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.minilang.MiniLangUtil;

/**
 * Implements a comparison - consisting of an l-value, an r-value, and a comparison operator.
 */
public abstract class Compare {

    private static final Map<String, Compare> INSTANCE_MAP = createInstanceMap();

    private static void assertValuesNotNull(Object lValue, Object rValue) {
        if (lValue == null) {
            throw new IllegalArgumentException("Cannot compare: l-value is null");
        }
        if (rValue == null) {
            throw new IllegalArgumentException("Cannot compare: r-value is null");
        }
    }

    private static Map<String, Compare> createInstanceMap() {
        Map<String, Compare> writableMap = new HashMap<>(10);
        writableMap.put("contains", new CompareContains());
        writableMap.put("equals", new CompareEquals());
        writableMap.put("greater", new CompareGreater());
        writableMap.put("greater-equals", new CompareGreaterEquals());
        writableMap.put("is-empty", new CompareIsEmpty());
        writableMap.put("is-not-null", new CompareIsNotNull());
        writableMap.put("is-null", new CompareIsNull());
        writableMap.put("less", new CompareLess());
        writableMap.put("less-equals", new CompareLessEquals());
        writableMap.put("not-equals", new CompareNotEquals());
        return Collections.unmodifiableMap(writableMap);
    }

    /**
     * Returns a <code>Compare</code> instance for the specified operator.
     * 
     * @param operator
     * @return A <code>Compare</code> instance for the specified operator
     */
    public static Compare getInstance(String operator) {
        Assert.notNull("operator", operator);
        return INSTANCE_MAP.get(operator);
    }

    /**
     * Returns the result of this comparison.
     *  
     * @param lValue The object being compared
     * @param rValue The object being compared to
     * @param type The Java class to be used in the comparison
     * @param format Optional format to be used in object type conversions
     * @param locale Optional locale to be used in object type conversions
     * @param timeZone Optional time zone to be used in object type conversions
     * @return The result of this comparison
     * @throws Exception
     */
    public abstract boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception;

    private static final class CompareContains extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            // The type parameter is ignored when using the contains operator, so no conversions will be performed.
            if (lValue == null || lValue == GenericEntity.NULL_FIELD) {
                return false;
            }
            if (lValue instanceof Collection) {
                Collection<Object> collection = UtilGenerics.checkCollection(lValue);
                return collection.contains(rValue);
            }
            if (lValue instanceof String && rValue instanceof String) {
                return ((String) lValue).contains((String) rValue);
            }
            throw new IllegalArgumentException("Cannot compare: l-value is not a collection");
        }
    }

    private static final class CompareEquals extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            if (convertedLvalue == null) {
                return convertedRvalue == null;
            }
            if (convertedRvalue == null) {
                return false;
            }
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) == 0;
            }
            return convertedLvalue.equals(convertedRvalue);
        }
    }

    private static final class CompareGreater extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            assertValuesNotNull(convertedLvalue, convertedRvalue);
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) > 0;
            }
            throw new IllegalArgumentException("Cannot compare: l-value is not a comparable type");
        }
    }

    private static final class CompareGreaterEquals extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            assertValuesNotNull(convertedLvalue, convertedRvalue);
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) >= 0;
            }
            throw new IllegalArgumentException("Cannot compare: l-value is not a comparable type");
        }
    }

    private static final class CompareIsEmpty extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            return ObjectType.isEmpty(convertedLvalue);
        }
    }

    private static final class CompareIsNotNull extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            return convertedLvalue != null;
        }
    }

    private static final class CompareIsNull extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            return convertedLvalue == null;
        }
    }

    private static final class CompareLess extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            assertValuesNotNull(convertedLvalue, convertedRvalue);
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) < 0;
            }
            throw new IllegalArgumentException("Cannot compare: l-value is not a comparable type");
        }
    }

    private static final class CompareLessEquals extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            assertValuesNotNull(convertedLvalue, convertedRvalue);
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) <= 0;
            }
            throw new IllegalArgumentException("Cannot compare: l-value is not a comparable type");
        }
    }

    private static final class CompareNotEquals extends Compare {

        @Override
        public boolean doCompare(Object lValue, Object rValue, Class<?> type, Locale locale, TimeZone timeZone, String format) throws Exception {
            Object convertedLvalue = MiniLangUtil.convertType(lValue, type, locale, timeZone, format);
            Object convertedRvalue = MiniLangUtil.convertType(rValue, type, locale, timeZone, format);
            if (convertedLvalue == null) {
                return convertedRvalue != null;
            }
            if (convertedRvalue == null) {
                return true;
            }
            if (convertedLvalue instanceof Comparable &&
                convertedRvalue instanceof Comparable) {
                Comparable<Object> comparable = UtilGenerics.cast(convertedLvalue);
                return comparable.compareTo(convertedRvalue) != 0;
            }
            return !convertedLvalue.equals(convertedRvalue);
        }
    }
}
