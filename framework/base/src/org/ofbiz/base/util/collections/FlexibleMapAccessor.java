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
package org.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import javax.el.PropertyNotFoundException;

import org.ofbiz.base.lang.SourceMonitor;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.string.UelUtil;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 */
@SourceMonitor("Adam Heath")
@SuppressWarnings("serial")
public class FlexibleMapAccessor<T> implements Serializable {
    public static final String module = FlexibleMapAccessor.class.getName();
    protected static final UtilCache<String, FlexibleMapAccessor<?>> fmaCache = UtilCache.createUtilCache("flexibleMapAccessor.ExpressionCache");
    @SuppressWarnings("unchecked")
    protected static final FlexibleMapAccessor nullFma = new FlexibleMapAccessor(null);

    protected final String original;
    protected final String bracketedOriginal;
    protected final FlexibleStringExpander fse;
    protected boolean isAscending = true;

    protected FlexibleMapAccessor(String name) {
        this.original = name;
        FlexibleStringExpander fse = null;
        String bracketedOriginal = null;
        if (UtilValidate.isNotEmpty(name)) {
            if (name.charAt(0) == '-') {
                this.isAscending = false;
                name = name.substring(1);
            } else if (name.charAt(0) == '+') {
                this.isAscending = true;
                name = name.substring(1);
            }
            if (name.contains(FlexibleStringExpander.openBracket)) {
                fse = FlexibleStringExpander.getInstance(name);
            } else {
                bracketedOriginal = FlexibleStringExpander.openBracket.concat(UelUtil.prepareExpression(name).concat(FlexibleStringExpander.closeBracket));
            }
        }
        this.bracketedOriginal = bracketedOriginal;
        this.fse = fse;
        if (Debug.verboseOn()) {
            Debug.logVerbose("FlexibleMapAccessor created, original = " + this.original, module);
        }
    }

    /** Returns a FlexibleMapAccessor instance.
     * @param original The original String expression
     * @return A FlexibleMapAccessor instance
     */
    @SuppressWarnings("unchecked")
    public static <T> FlexibleMapAccessor<T> getInstance(String original) {
        if (UtilValidate.isEmpty(original) || "null".equals(original)) {
            return nullFma;
        }
        FlexibleMapAccessor fma = fmaCache.get(original);
        if (fma == null) {
            synchronized (fmaCache) {
                fmaCache.put(original, new FlexibleMapAccessor(original));
                fma = fmaCache.get(original);
            }
        }
        return fma;
    }

    public String getOriginalName() {
        return this.original;
    }

    public boolean getIsAscending() {
        return this.isAscending;
    }

    public boolean isEmpty() {
         return this.original == null;
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
    public T get(Map<String, ? extends Object> base, Locale locale) {
        if (base == null || this.isEmpty()) {
            return null;
        }
        if (!base.containsKey(UelUtil.localizedMapLocaleKey) && locale != null) {
            Map<String, Object> writableMap = UtilGenerics.cast(base);
            writableMap.put(UelUtil.localizedMapLocaleKey, locale);
        }
        Object obj = null;
        try {
            obj = UelUtil.evaluate(base, getExpression(base));
        } catch (PropertyNotFoundException e) {
            // PropertyNotFound exceptions are common, so log verbose
            if (Debug.verboseOn()) {
                Debug.logVerbose("UEL exception while getting value: " + e + ", original = " + this.original, module);
            }
        } catch (Exception e) {
            Debug.logError("UEL exception while getting value: " + e + ", original = " + this.original, module);
        }
        return UtilGenerics.<T>cast(obj);
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
        if (this.isEmpty()) {
            return;
        }
        if (base == null) {
            throw new IllegalArgumentException("Cannot put a value in a null base Map");
        }
        try {
            UelUtil.setValue(base, getExpression(base), value == null ? Object.class : value.getClass(), value);
        } catch (Exception e) {
            Debug.logError("UEL exception while setting value: " + e + ", original = " + this.original, module);
        }
    }

    /** Given the name based information in this accessor, remove the value from the passed in Map.
     * @param base the Map to remove from
     * @return the object removed
     */
    public T remove(Map<String, ? extends Object> base) {
        if (this.isEmpty()) {
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
            Debug.logError("UEL exception while removing value: " + e + ", original = " + this.original, module);
        }
        return object;
    }

    protected String getExpression(Map<String, ? extends Object> base) {
        String expression = null;
        if (this.fse != null) {
            expression = FlexibleStringExpander.openBracket.concat(UelUtil.prepareExpression(this.fse.expandString(base)).concat(FlexibleStringExpander.closeBracket));
        } else {
            expression = this.bracketedOriginal;
        }
        return expression;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return super.toString();
        }
        return this.original;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            FlexibleMapAccessor that = (FlexibleMapAccessor) obj;
            return UtilObject.equalsHelper(this.original, that.original);
        } catch (Exception e) {}
        return false;
    }

    @Override
    public int hashCode() {
        return this.original == null ? super.hashCode() : this.original.hashCode();
    }
}
