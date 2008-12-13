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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.string.UelUtil;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 */
@SuppressWarnings("serial")
public class FlexibleMapAccessor<T> implements Serializable {
    public static final String module = FlexibleMapAccessor.class.getName();
    protected static final String openBracket = "${";
    protected static final String closeBracket = "}";
    protected static final UtilCache<String, FlexibleMapAccessor<?>> fmaCache = new UtilCache<String, FlexibleMapAccessor<?>>("flexibleMapAccessor.ExpressionCache");
    @SuppressWarnings("unchecked")
    protected static final FlexibleMapAccessor nullFma = new FlexibleMapAccessor(null);

    protected final String original;
    protected final String bracketedOriginal;
    protected final ExpressionNode node;
    protected boolean isAscending = true;

    public FlexibleMapAccessor(String name) {
        // TODO: Change this to protected
        this.original = name;
        if (name != null && name.length() > 0) {
            if (name.charAt(0) == '-') {
                this.isAscending = false;
                name = name.substring(1);
            } else if (name.charAt(0) == '+') {
                this.isAscending = true;
                name = name.substring(1);
            }
        }
        String bracketedOriginal = openBracket + name + closeBracket;
        // JUEL library doesn't like "+" in list indexes
        bracketedOriginal = bracketedOriginal.replace("[+", "[");
        this.bracketedOriginal = bracketedOriginal;
        this.node = parseExpression(name);
//        Debug.logInfo("node = " + this.node, module);
    }
    
    /** Returns a FlexibleMapAccessor instance.
     * @param original The original String expression
     * @return A FlexibleMapAccessor instance
     */
    @SuppressWarnings("unchecked")
    public static <T> FlexibleMapAccessor<T> getInstance(String original) {
        if (original == null || original.length() == 0) {
            return nullFma;
        }
        FlexibleMapAccessor fma = fmaCache.get(original);
        if (fma == null) {
            synchronized (fmaCache) {
                fma = fmaCache.get(original);
                if (fma == null) {
                    fma = new FlexibleMapAccessor(original);
                    fmaCache.put(original, fma);
                }
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
         return this.original == null || this.original.length() == 0;
    }

    /** Given the name based information in this accessor, get the value from the passed in Map. 
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     * @param base
     * @return the found value
     */
    public T get(Map<String, ? extends Object> base) {
        if (this.isEmpty()) {
            return null;
        }
        Object obj = null;
        try {
            obj = UelUtil.evaluate(base, this.bracketedOriginal);
        } catch (Exception e) {
            Debug.logVerbose("Error evaluating expression: " + e, module);
        }
        return UtilGenerics.<T>cast(obj);
    }

    /** Given the name based information in this accessor, get the value from the passed in Map. 
     *  Supports LocalizedMaps by getting a String or Locale object from the base Map with the key "locale", or by explicit locale parameter.
     *  Note that the localization functionality is only used when the lowest level sub-map implements the LocalizedMap interface
     * @param base Map to get value from
     * @param locale Optional locale parameter, if null will see if the base Map contains a "locale" key
     * @return the found value
     */
    public T get(Map<String, ? extends Object> base, Locale locale) {
        if (this.isEmpty()) {
            return null;
        }
        Object obj = this.node.get(base, UtilGenerics.<Map<String, Object>>cast(base), locale);
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
        this.node.put(base, base, value);
    }
    
    /** Given the name based information in this accessor, remove the value from the passed in Map.
     * @param base the Map to remove from
     * @return the object removed
     */
    public T remove(Map<String, ? extends Object> base) {
        if (this.isEmpty()) {
            return null;
        }
        Debug.logInfo("Removing element = " + this.original, module);
        return UtilGenerics.<T>cast(this.node.remove(base, UtilGenerics.<Map<String, Object>>cast(base)));
    }
    
    public String toString() {
        if (this.isEmpty()) {
            return super.toString();
        }
        return this.original;
    }
    
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            FlexibleMapAccessor that = (FlexibleMapAccessor) obj;
            if (this.original == null && that.original == null) {
                return true;
            }
            return this.original.equals(that.original);
        } catch (Exception e) {}
        return false;
    }
    
    public int hashCode() {
        return this.original == null ? super.hashCode() : this.original.hashCode();
    }
    
    protected static ExpressionNode parseExpression(String original) {
        if (original == null || original.length() == 0) {
            return null;
        }
        String str = original;
        // TODO: Make this smarter - expressions could contain periods
        int end = original.indexOf(".");
        if (end != -1) {
            str = original.substring(0, end);
        }
        ExpressionNode node = null;
        if (str.contains("[")) {
            node = parseBracketedExpression(str);
        } else {
            node = new MapElementNode(str);
        }
        if (end != -1) {
            node.setChild(parseExpression(original.substring(end + 1)));
        }
        return node;
    }

    protected static ExpressionNode parseBracketedExpression(String original) {
        boolean isAddAtEnd = false;
        boolean isAddAtIndex = false;
        int listIndex = -1;
        int openBrace = original.indexOf('[');
        int closeBrace = (openBrace == -1 ? -1 : original.indexOf(']', openBrace));
        if (closeBrace == -1) {
            throw new RuntimeException("Missing ] in expression: " + original);
        }
        String base = original.substring(0, openBrace);
        String property = original.substring(openBrace+1, closeBrace).trim();
        if (property.length() == 0) {
            isAddAtEnd = true;
        } else if (property.charAt(0) == '+') {
            property = property.substring(1);
            isAddAtIndex = true;
        }
        try{
            listIndex = Integer.parseInt(property);
        } catch (Exception e) {}
        if (listIndex != -1 || isAddAtEnd || isAddAtIndex) {
            return new ListNode(original, base, isAddAtEnd, isAddAtIndex, listIndex);
        } else {
            return new BracketNode(original, base, property);
        }
    }

    /** Abstract class for the expression nodes. Expression nodes are separated
     * by a period. <code>ExpressionNode</code> instances are connected as a
     * singly-linked list - going from left to right in the expression. The last
     * node has a <code>null</code> child.
     */
    protected static abstract class ExpressionNode implements Serializable {
        protected final String original;
        protected ExpressionNode child = null;
        protected ExpressionNode(String original) {
            this.original = original;
        }
        protected void setChild(ExpressionNode child) {
            this.child = child;
        }
        protected abstract Object get(Map<String, ? extends Object> context, Map<String, Object> base, Locale locale);
        protected abstract void put(Map<String, Object> context, Map<String, Object> base, Object value);
        protected abstract Object remove(Map<String, ? extends Object> context, Map<String, Object> base);
    }

    /** Implementation of a <code>Map</code> element (<code>someMap.someElement</code>). */
    protected static class MapElementNode extends ExpressionNode {
        protected MapElementNode(String original) {
            super(original);
        }
        public String toString() {
            return "MapElementNode(" + this.original + ")" + (this.child == null ? "" : "." + this.child.toString());
        }
        protected Object get(Map<String, ? extends Object> context, Map<String, Object> base, Locale locale) {
            String key = getExprString(context, this.original);
            if (this.child == null) {
                return legacyGet(base, key, locale);
            } else {
                Map<String, Object> thisElement = getMapPutElement(base, key, true);
                return this.child.get(context, thisElement, locale);
            }
        }
        protected void put(Map<String, Object> context, Map<String, Object> base, Object value) {
            String key = getExprString(context, this.original);
            if (this.child == null) {
                base.put(key, value);
            } else {
                Map<String, Object> thisElement = getMapPutElement(base, key, true);
                this.child.put(context, thisElement, value);
            }
        }
        protected Object remove(Map<String, ? extends Object> context, Map<String, Object> base) {
            String key = getExprString(context, this.original);
            if (this.child == null) {
                return base.remove(key);
            } else {
                Map<String, Object> thisElement = UtilGenerics.<Map<String, Object>>cast(base.get(key));
                if (thisElement != null) {
                    return this.child.remove(context, thisElement);
                }
            }
            return null;
        }
    }

    /** Implementation of a <code>List</code> element with a literal index
     * (<code>someList[1]</code>). */
    protected static class ListNode extends ExpressionNode {
        protected final String key;
        protected final boolean isAddAtEnd;
        protected final boolean isAddAtIndex;
        protected final int listIndex;
        protected ListNode(String original, String base, boolean isAddAtEnd, boolean isAddAtIndex, int listIndex) {
            super(original);
            this.key = base;
            this.isAddAtEnd = isAddAtEnd;
            this.isAddAtIndex = isAddAtIndex;
            this.listIndex = listIndex;
        }
        public String toString() {
            return "ListNode(" + this.original + ")" + (this.child == null ? "" : "." + this.child.toString());
        }
        protected Object get(Map<String, ? extends Object> context, Map<String, Object> base, Locale locale) {
            List<Object> list = getListElementFromMap(base, key, true);
            if (this.child == null) {
                return list.get(this.listIndex);
            }
            Map<String, Object> newBase = null;
            try {
                newBase = UtilGenerics.<Map<String, Object>>cast(list.get(this.listIndex));
            } catch (Exception e) {
                throw new RuntimeException("Variable is not a Map: " + this.original);
            }
            return this.child.get(context, newBase, locale);
        }
        protected void put(Map<String, Object> context, Map<String, Object> base, Object value) {
            String key = getExprString(context, this.key);
            List<Object> list = getListElementFromMap(base, key, true);
            base.put(key, list);
            if (this.child == null) {
                if (this.isAddAtEnd) {
                    list.add(value);
                } else {
                    if (this.isAddAtIndex) {
                        list.add(this.listIndex, value);
                    } else {
                        list.set(this.listIndex, value);
                    }
                }
                return;
            }
            Map<String, Object> newBase = null;
            try {
                newBase = UtilGenerics.<Map<String, Object>>cast(list.get(this.listIndex));
            } catch (Exception e) {
                throw new RuntimeException("Variable is not a Map: " + this.original);
            }
            this.child.put(context, newBase, value);
        }
        protected Object remove(Map<String, ? extends Object> context, Map<String, Object> base) {
            String key = getExprString(context, this.key);
            return removeList(getListElementFromMap(base, key, false), this.listIndex, context, this.child);
        }
    }

    /** Implementation of a <code>Map</code> or <code>List</code> element with
     * a variable property (<code>someElement[var]</code>). If <code>var</code>
     * evaluates to an integer, then the element is treated as a <code>List</code>,
     * otherwise it is treated as a <code>Map</code>.*/
    protected static class BracketNode extends ExpressionNode {
        // .base[property]
        protected final String base;
        protected final String property;
        protected BracketNode(String original, String base, String property) {
            super(original);
            this.base = base;
            this.property = property;
        }
        public String toString() {
            return "BracketNode(" + this.original + ")" + (this.child == null ? "" : "." + this.child.toString());
        }
        protected Object get(Map<String, ? extends Object> context, Map<String, Object> base, Locale locale) {
            String key = getExprString(context, this.base);
            Object property = getProperty(context);
            Integer index = UtilMisc.toIntegerObject(property);
            if (index != null) {
                // This node is a List
                List<Object> list = getListElementFromMap(base, key, true);
                if (this.child == null) {
                    return list.get(index);
                }
                Map<String, Object> newBase = null;
                try {
                    newBase = UtilGenerics.<Map<String, Object>>cast(list.get(index));
                } catch (Exception e) {
                    throw new RuntimeException("Variable is not a Map: " + this.original);
                }
                return this.child.get(context, newBase, locale);
            } else {
                // This node is a Map
                Map<String, Object> newBase = getMapPutElement(base, key, true);
                String newKey = property.toString();
                if (this.child == null) {
                    return legacyGet(newBase, newKey, locale);
                } else {
                    Map<String, Object> thisElement = getMapPutElement(newBase, newKey, true);
                    return this.child.get(context, thisElement, locale);
                }
            }
        }
        protected void put(Map<String, Object> context, Map<String, Object> base, Object value) {
            String key = getExprString(context, this.base);
            Object property = getProperty(context);
            Integer index = UtilMisc.toIntegerObject(property);
            if (index != null) {
                // This node is a List
                List<Object> list = getListElementFromMap(base, key, true);
                base.put(key, list);
                if (this.child == null) {
                    list.set(index, value);
                    return;
                }
                Map<String, Object> newBase = null;
                try {
                    newBase = UtilGenerics.<Map<String, Object>>cast(list.get(index));
                } catch (Exception e) {
                    throw new RuntimeException("Variable is not a Map: " + this.original);
                }
                this.child.put(context, newBase, value);
            } else {
                // This node is a Map
                Map<String, Object> newBase = getMapPutElement(base, key, true);
                String newKey = property.toString();
                if (this.child == null) {
                    newBase.put(newKey, value);
                } else {
                    Map<String, Object> thisElement = getMapPutElement(newBase, newKey, true);
                    this.child.put(context, thisElement, value);
                }
            }
        }
        protected Object remove(Map<String, ? extends Object> context, Map<String, Object> base) {
            String key = getExprString(context, this.base);
            Object property = getProperty(context);
            Integer index = UtilMisc.toIntegerObject(property);
            if (index != null) {
                // This node is a List
                return removeList(getListElementFromMap(base, key, false), index, context, this.child);
            } else {
                // This node is a Map
                Map<String, Object> newBase = UtilGenerics.<Map<String, Object>>cast(base.get(key));
                if (newBase != null) {
                    if (this.child == null) {
                        String newKey = property.toString();
                        return newBase.remove(newKey);
                    } else {
                        return this.child.remove(context, newBase);
                    }
                }
                return null;
            }
        }
        protected Object getProperty(Map<String, ? extends Object> context) {
            String str = this.property;
            if (!this.property.startsWith(openBracket)) {
                str = openBracket + str + closeBracket;
            }
            Object obj = null;
            try {
                obj = UelUtil.evaluate(context, str);
            } catch (Exception e) {}
            if (obj == null) {
                throw new RuntimeException("Variable " + this.property + " not found in expression " + original);
            }
            return obj;
        }

    }

    /** Evaluates an expression, then converts the result <code>Object</code> to a <code>String</code>. */
    protected static String getExprString(Map<String, ? extends Object> context, String expression) {
        Object result = expression;
        if (expression.startsWith(openBracket)) {
            try {
                result = UelUtil.evaluate(context, expression);
            } catch (Exception e) {
                throw new RuntimeException("Error while evaluating expression " + expression + ": " + e);
            }
        }
        return result.toString();
    }

    /** Returns a <code>List</code> element contained in a <code>Map</code>. If <code>createIfMissing
     * </code> is true, then the <code>List</code> is created if not found. */
    protected static List<Object> getListElementFromMap(Map<String, ? extends Object> base, String key, boolean createIfMissing) {
        List<Object> result = null;
        try {
            result = UtilGenerics.<List<Object>>cast(base.get(key));
        } catch (Exception e) {
            throw new RuntimeException("Variable is not a List: " + key);
        }
        if (result == null && createIfMissing) {
            result = FastList.newInstance();
        }
        return result;
    }

    protected static Map<String, Object> getMapPutElement(Map<String, Object> base, String key, boolean createIfMissing) {
        Map<String, Object> result = UtilGenerics.<Map<String, Object>>cast(base.get(key));
        if (result == null && createIfMissing) {
            result = FastMap.newInstance();
            base.put(key, result);
        }
        return result;
    }

    protected static Object removeList(List<Object> list, int index, Map<String, ? extends Object> context, ExpressionNode child) {
        if (list != null) {
            if (child == null) {
                return list.remove(index);
            } else {
                Map<String, Object> newBase = null;
                try {
                    newBase = UtilGenerics.<Map<String, Object>>cast(list.get(index));
                } catch (Exception e) {}
                if (newBase != null) {
                    return child.remove(context, newBase);
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    protected static Object legacyGet(Map<String, Object> map, String key, Locale locale) {
        if (map instanceof LocalizedMap) {
            return ((LocalizedMap)map).get(key, locale);
        }
        return map.get(key);
    }
}
