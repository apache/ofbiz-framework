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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.lang.Reusable;
import javolution.context.ObjectFactory;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;


/**
 * Map Stack
 *
 */
public class MapStack<K> implements Map<K, Object>, Reusable, LocalizedMap<Object> {

    public static final String module = MapStack.class.getName();

    protected static final ObjectFactory<MapStack<?>> mapStackFactory = new ObjectFactory<MapStack<?>>() {
        @Override
        protected MapStack<?> create() {
            return new MapStack<Object>();
        }
    };

    protected static final <K> MapStack<K> getMapStack() {
        return (MapStack<K>) UtilGenerics.<K, Object>checkMap(mapStackFactory.object());
    }

    public static <K> MapStack<K> create() {
        MapStack<K> newValue = MapStack.getMapStack();
        // initialize with a single entry
        newValue.push();
        return newValue;
    }

    @SuppressWarnings("unchecked")
    public static <K> MapStack<K> create(Map<K, Object> baseMap) {
        MapStack<K> newValue = MapStack.getMapStack();
        if (baseMap instanceof MapStack) {
            newValue.stackList.addAll(((MapStack) baseMap).stackList);
        } else {
            newValue.stackList.add(0, baseMap);
        }
        return newValue;
    }

    /** Does a shallow copy of the internal stack of the passed MapStack; enables simultaneous stacks that share common parent Maps */
    public static <K> MapStack<K> create(MapStack<K> source) {
        MapStack<K> newValue = MapStack.getMapStack();
        newValue.stackList.addAll(source.stackList);
        return newValue;
    }

    protected MapStack() {
        super();
    }

    protected List<Map<K, Object>> stackList = FastList.newInstance();

    public void reset() {
        stackList = FastList.newInstance();
    }

    /** Puts a new Map on the top of the stack */
    public void push() {
        Map<K, Object> newMap = FastMap.newInstance();
        this.stackList.add(0,newMap);
    }

    /** Puts an existing Map on the top of the stack (top meaning will override lower layers on the stack) */
    public void push(Map<K, Object> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot push null existing Map onto a MapStack");
        }
        this.stackList.add(0, existingMap);
    }

    /** Puts an existing Map on the BOTTOM of the stack (bottom meaning will be overriden by lower layers on the stack, ie everything else already there) */
    public void addToBottom(Map<K, Object> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot add null existing Map to bottom of a MapStack");
        }
        this.stackList.add(existingMap);
    }

    /** Remove and returns the Map from the top of the stack; if there is only one Map on the stack it returns null and does not remove it */
    public Map<K, Object> pop() {
        // always leave at least one Map in the List, ie never pop off the last Map
        if (this.stackList.size() > 1) {
            return stackList.remove(0);
        } else {
            return null;
        }
    }

    /**
     * Creates a MapStack object that has the same Map objects on its stack;
     * meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapStack objects, but sharing the Maps in common
     */
    public MapStack<K> standAloneStack() {
        MapStack<K> standAlone = MapStack.create(this);
        return standAlone;
    }

    /**
     * Creates a MapStack object that has the same Map objects on its stack,
     * but with a new Map pushed on the top; meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapStack objects, but sharing the Maps in common
     */
    public MapStack<K> standAloneChildStack() {
        MapStack<K> standAloneChild = MapStack.create(this);
        standAloneChild.push();
        return standAloneChild;
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        // a little bit tricky; to represent the apparent size we need to aggregate all keys and get a count of unique keys
        // this is a bit of a slow way, but gets the best number possible
        Set<K> keys = this.keySet();
        return keys.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        // walk the stackList and if any is not empty, return false; otherwise return true
        for (Map<K, Object> curMap: this.stackList) {
            if (!curMap.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        for (Map<K, Object> curMap: this.stackList) {
            if (curMap.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, consider it an option, otherwise ignore
        Set<K> resultKeySet = FastSet.newInstance();
        for (Map<K, Object> curMap: this.stackList) {
            for (Map.Entry<K, Object> curEntry: curMap.entrySet()) {
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    if (value == null) {
                        if (curEntry.getValue() == null) {
                            return true;
                        }
                    } else {
                        if (value.equals(curEntry.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        if ("context".equals(key)) {
            return this;
        }

        // walk the stackList and for the first place it is found return true; otherwise refurn false
        for (Map<K, Object> curMap: this.stackList) {
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(key)) {
                return curMap.get(key);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.ofbiz.base.util.collections.LocalizedMap#get(java.lang.String, java.util.Locale)
     */
    public Object get(String name, Locale locale) {
        if ("context".equals(name)) {
            return this;
        }

        // walk the stackList and for the first place it is found return true; otherwise refurn false
        for (Map<K, Object> curMap: this.stackList) {
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(name)) {
                if (curMap instanceof LocalizedMap) {
                    LocalizedMap<Object> lmap = UtilGenerics.cast(curMap);
                    return lmap.get(name, locale);
                } else {
                    return curMap.get(name);
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(K key, Object value) {
        if ("context".equals(key)) {
            if (value == null || this != value) {
                Debug.logWarning("WARNING: Putting a value in a MapStack with key [context] that is not this MapStack, will be hidden by the current MapStack self-reference: " + value, module);
            }
        }

        // all write operations are local: only put in the Map on the top of the stack
        Map<K, Object> currentMap = this.stackList.get(0);
        return currentMap.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object key) {
        // all write operations are local: only remove from the Map on the top of the stack
        Map<K, Object> currentMap = this.stackList.get(0);
        return currentMap.remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends Object> arg0) {
        // all write operations are local: only put in the Map on the top of the stack
        Map<K, Object> currentMap = this.stackList.get(0);
        currentMap.putAll(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        // all write operations are local: only clear the Map on the top of the stack
        this.stackList.get(0).clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        // walk the stackList and aggregate all keys
        Set<K> resultSet = FastSet.newInstance();
        for (Map<K, Object> curMap: this.stackList) {
            resultSet.addAll(curMap.keySet());
        }
        return Collections.unmodifiableSet(resultSet);
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<Object> values() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set<K> resultKeySet = FastSet.newInstance();
        List<Object> resultValues = FastList.newInstance();
        for (Map<K, Object> curMap: this.stackList) {
            for (Map.Entry<K, Object> curEntry: curMap.entrySet()) {
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    resultValues.add(curEntry.getValue());
                }
            }
        }
        return Collections.unmodifiableCollection(resultValues);
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set<Map.Entry<K, Object>> entrySet() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set<K> resultKeySet = FastSet.newInstance();
        Set<Map.Entry<K, Object>> resultEntrySet = FastSet.newInstance();
        for (Map<K, Object> curMap: this.stackList) {
            for (Map.Entry<K, Object> curEntry: curMap.entrySet()) {
                if (!resultKeySet.contains(curEntry.getKey())) {
                    resultKeySet.add(curEntry.getKey());
                    resultEntrySet.add(curEntry);
                }
            }
        }
        return Collections.unmodifiableSet(resultEntrySet);
    }

    @Override
    public String toString() {
        StringBuilder fullMapString = new StringBuilder();
        int curLevel = 0;
        for (Map<K, Object> curMap: this.stackList) {
            fullMapString.append("============================== Start stack level " + curLevel + "\n");
            for (Map.Entry<K, Object> curEntry: curMap.entrySet()) {

                fullMapString.append("==>[");
                fullMapString.append(curEntry.getKey());
                fullMapString.append("]:");
                // skip the instances of MapStack to avoid infinite loop
                if (curEntry.getValue() instanceof MapStack) {
                    fullMapString.append("<Instance of MapStack, not printing to avoid infinite recursion>");
                } else {
                    fullMapString.append(curEntry.getValue());
                }
                fullMapString.append("\n");
            }
            fullMapString.append("============================== End stack level " + curLevel + "\n");
            curLevel++;
        }
        return fullMapString.toString();
    }
}
