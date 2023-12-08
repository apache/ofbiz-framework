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
package org.apache.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.el.PropertyNotFoundException;

import org.apache.ofbiz.base.lang.IsEmpty;
import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.base.util.string.UelUtil;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 */
@SourceMonitored
@SuppressWarnings("serial")
public final class FlexibleMapAccessor<T> implements Serializable, IsEmpty {
    private static final String MODULE = FlexibleMapAccessor.class.getName();
    private static final UtilCache<String, FlexibleMapAccessor<Object>> FMA_CACHE =
            UtilCache.createUtilCache("flexibleMapAccessor.ExpressionCache");
    private static final FlexibleMapAccessor<?> NULL_FMA = new FlexibleMapAccessor<>("");

    private final boolean isEmpty;
    private final String original;
    private final String bracketedOriginal;
    private final FlexibleStringExpander fse;
    private final boolean isAscending;

    private FlexibleMapAccessor(String name) {
        this.original = name;
        this.isEmpty = name.isEmpty();
        FlexibleStringExpander fse = null;
        String bracketedOriginal = null;
        boolean isAscending = true;
        if (UtilValidate.isNotEmpty(name)) {
            if (name.charAt(0) == '-') {
                isAscending = false;
                name = name.substring(1);
            } else if (name.charAt(0) == '+') {
                isAscending = true;
                name = name.substring(1);
            }
            if (name.contains(FlexibleStringExpander.OPEN_BRACKET)) {
                fse = FlexibleStringExpander.getInstance(name);
            } else {
                bracketedOriginal = FlexibleStringExpander.OPEN_BRACKET.concat(UelUtil.prepareExpression(name)
                        .concat(FlexibleStringExpander.CLOSE_BRACKET));
            }
        }
        this.bracketedOriginal = bracketedOriginal;
        this.isAscending = isAscending;
        this.fse = fse;
        if (Debug.verboseOn()) {
            Debug.logVerbose("FlexibleMapAccessor created, original = " + this.original, MODULE);
        }
    }

    /** Returns a FlexibleMapAccessor instance.
     * @param original The original String expression
     * @return A FlexibleMapAccessor instance
     */
    public static <T> FlexibleMapAccessor<T> getInstance(String original) {
        if (UtilValidate.isEmpty(original) || "null".equals(original)) {
            return UtilGenerics.cast(NULL_FMA);
        }
        FlexibleMapAccessor<T> fma = UtilGenerics.cast(FMA_CACHE.get(original));
        if (fma == null) {
            FMA_CACHE.putIfAbsent(original, new FlexibleMapAccessor<>(original));
            fma = UtilGenerics.cast(FMA_CACHE.get(original));
        }
        return fma;
    }

    /**
     * Returns <code>true</code> if this <code>FlexibleMapAccessor</code> contains a nested expression.
     * @return <code>true</code> if this <code>FlexibleMapAccessor</code> contains a nested expression
     */
    public boolean containsNestedExpression() {
        return fse != null;
    }

    public String getOriginalName() {
        return this.original;
    }

    public boolean getIsAscending() {
        return this.isAscending;
    }

    @Override
    public boolean isEmpty() {
        return this.isEmpty;
    }

    /** Given the name based information in this accessor, get the value from the passed in Map.
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     * @param base
     * @return the found value
     */
    public T get(Map<String, ? extends Object> base) {
        return get(base, null);
    }

    /** Given the name based information in this accessor, get the value from the passed in Map.
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     *  Note that the localization functionality is only used when the lowest level sub-map implements the LocalizedMap interface
     * @param base Map to get value from
     * @param locale Optional locale parameter, if null will see if the base Map contains a "locale" key
     * @return the found value
     */
    @SuppressWarnings("unchecked")
    public T get(Map<String, ? extends Object> base, Locale locale) {
        if (base == null || this.isEmpty) {
            return null;
        }
        if (locale != null && !base.containsKey(UelUtil.getLocalizedMapLocaleKey())) {
            // This method is a hot spot, so placing the cast here instead of in another class.
            Map<String, Object> writableMap = (Map<String, Object>) base;
            writableMap.put(UelUtil.getLocalizedMapLocaleKey(), locale);
        }
        Object obj = null;
        try {
            obj = UelUtil.evaluate(base, getExpression(base));
        } catch (PropertyNotFoundException e) {
            // PropertyNotFound exceptions are common, so log verbose.
            if (Debug.verboseOn()) {
                Debug.logVerbose("UEL exception while getting value: " + e + ", original = " + this.original, MODULE);
            }
        } catch (Exception e) {
            Debug.logError("UEL exception while getting value: " + e + ", original = " + this.original, MODULE);
        }
        // This method is a hot spot, so placing the cast here instead of in another class.
        return (T) obj;
    }

    /** Given the name based information in this accessor, put the value in the passed in Map.
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that point instead of set at the point.
     * @param base
     * @param value
     */
    public void put(Map<String, Object> base, T value) {
        if (this.isEmpty) {
            return;
        }
        if (base == null) {
            throw new IllegalArgumentException("Cannot put a value in a null base Map");
        }
        try {
            UelUtil.setValue(base, getExpression(base), value == null ? Object.class : value.getClass(), value);
        } catch (Exception e) {
            Debug.logError("UEL exception while setting value: " + e + ", original = " + this.original, MODULE);
        }
    }

    /** Given the name based information in this accessor, remove the value from the passed in Map.
     * @param base the Map to remove from
     * @return the object removed
     */
    public T remove(Map<String, ? extends Object> base) {
        if (this.isEmpty) {
            return null;
        }
        T object = get(base);
        if (object == null) {
            return null;
        }
        try {
            Map<String, Object> writableMap = UtilGenerics.cast(base);
            UelUtil.removeValue(writableMap, getExpression(base));
        } catch (Exception e) {
            Debug.logError("UEL exception while removing value: " + e + ", original = " + this.original, MODULE);
        }
        return object;
    }

    private String getExpression(Map<String, ? extends Object> base) {
        String expression = null;
        if (this.fse != null) {
            expression = FlexibleStringExpander.OPEN_BRACKET.concat(UelUtil.prepareExpression(this.fse.expandString(base))
                    .concat(FlexibleStringExpander.CLOSE_BRACKET));
        } else {
            expression = this.bracketedOriginal;
        }
        return expression;
    }

    @Override
    public String toString() {
        return this.original;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlexibleMapAccessor) {
            FlexibleMapAccessor<?> that = (FlexibleMapAccessor<?>) obj;
            return Objects.equals(this.original, that.original);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.original.hashCode();
    }
}
