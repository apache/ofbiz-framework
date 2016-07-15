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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.UtilGenerics;


/**
 * Map Stack
 *
 */
public class MapContext<K, V> implements Map<K, V>, LocalizedMap<V> {

    public static final String module = MapContext.class.getName();

    public static final <K, V> MapContext<K, V> getMapContext() {
        return new MapContext<K, V>();
    }

    public static <K, V> MapContext<K, V> createMapContext() {
        MapContext<K, V> newValue = MapContext.getMapContext();
        // initialize with a single entry
        newValue.push();
        return newValue;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> MapContext<K, V> createMapContext(Map<K, V> baseMap) {
        MapContext<K, V> newValue = MapContext.getMapContext();
        if (baseMap instanceof MapContext) {
            newValue.stackList.addAll(((MapContext) baseMap).stackList);
        } else {
            newValue.stackList.add(0, baseMap);
        }
        return newValue;
    }

    /** Does a shallow copy of the internal stack of the passed MapContext; enables simultaneous stacks that share common parent Maps */
    public static <K, V> MapContext<K, V> createMapContext(MapContext<K, V> source) {
        MapContext<K, V> newValue = MapContext.getMapContext();
        newValue.stackList.addAll(source.stackList);
        return newValue;
    }

    protected MapContext() {
        super();
    }

    protected List<Map<K, V>> stackList = new LinkedList<Map<K, V>>();

    public void reset() {
        stackList = new LinkedList<Map<K, V>>();
    }

    /** Puts a new Map on the top of the stack */
    public void push() {
        Map<K, V> newMap = new HashMap<K, V>();
        this.stackList.add(0,newMap);
    }

    /** Puts an existing Map on the top of the stack (top meaning will override lower layers on the stack) */
    public void push(Map<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot push null existing Map onto a MapContext");
        }
        this.stackList.add(0, existingMap);
    }

    /** Puts an existing Map on the BOTTOM of the stack (bottom meaning will be overriden by lower layers on the stack, ie everything else already there) */
    public void addToBottom(Map<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot add null existing Map to bottom of a MapContext");
        }
        this.stackList.add(existingMap);
    }

    /** Remove and returns the Map from the top of the stack; if there is only one Map on the stack it returns null and does not remove it */
    public Map<K, V> pop() {
        // always leave at least one Map in the List, ie never pop off the last Map
        if (this.stackList.size() > 1) {
            return stackList.remove(0);
        } else {
            return null;
        }
    }

    /**
     * Creates a MapContext object that has the same Map objects on its stack;
     * meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapContext objects, but sharing the Maps in common
     */
    public MapContext<K, V> standAloneStack() {
        MapContext<K, V> standAlone = MapContext.createMapContext(this);
        return standAlone;
    }

    /**
     * Creates a MapContext object that has the same Map objects on its stack,
     * but with a new Map pushed on the top; meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapContext objects, but sharing the Maps in common
     */
    public MapContext<K, V> standAloneChildStack() {
        MapContext<K, V> standAloneChild = MapContext.createMapContext(this);
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
        for (Map<K, V> curMap: this.stackList) {
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
        for (Map<K, V> curMap: this.stackList) {
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
        Set<K> resultKeySet = new HashSet<K>();
        for (Map<K, V> curMap: this.stackList) {
            for (Map.Entry<K, V> curEntry: curMap.entrySet()) {
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
    public V get(Object key) {
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        for (Map<K, V> curMap: this.stackList) {
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
    public V get(String name, Locale locale) {
        // walk the stackList and for the first place it is found return true; otherwise refurn false
        for (Map<K, V> curMap: this.stackList) {
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(name)) {
                if (curMap instanceof LocalizedMap<?>) {
                    LocalizedMap<V> lmap = UtilGenerics.cast(curMap);
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
    public V put(K key, V value) {
        // all write operations are local: only put in the Map on the top of the stack
        Map<K, V> currentMap = this.stackList.get(0);
        return currentMap.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(Object key) {
        // all write operations are local: only remove from the Map on the top of the stack
        Map<K, V> currentMap = this.stackList.get(0);
        return currentMap.remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> arg0) {
        // all write operations are local: only put in the Map on the top of the stack
        Map<K, V> currentMap = this.stackList.get(0);
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
        Set<K> resultSet = new HashSet<K>();
        for (Map<K, V> curMap: this.stackList) {
            resultSet.addAll(curMap.keySet());
        }
        return Collections.unmodifiableSet(resultSet);
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set<K> resultKeySet = new HashSet<K>();
        List<V> resultValues = new LinkedList<V>();
        for (Map<K, V> curMap: this.stackList) {
            for (Map.Entry<K, V> curEntry: curMap.entrySet()) {
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
    public Set<Map.Entry<K, V>> entrySet() {
        // walk the stackList and the entries for each Map and if nothing is in for the current key, put it in
        Set<K> resultKeySet = new HashSet<K>();
        Set<Map.Entry<K, V>> resultEntrySet = new ListSet<Map.Entry<K, V>>();
        for (Map<K, V> curMap: this.stackList) {
            for (Map.Entry<K, V> curEntry: curMap.entrySet()) {
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
        for (Map<K, V> curMap: this.stackList) {
            fullMapString.append("============================== Start stack level " + curLevel + "\n");
            for (Map.Entry<K, V> curEntry: curMap.entrySet()) {

                fullMapString.append("==>[");
                fullMapString.append(curEntry.getKey());
                fullMapString.append("]:");
                // skip the instances of MapContext to avoid infinite loop
                if (curEntry.getValue() instanceof MapContext<?, ?>) {
                    fullMapString.append("<Instance of MapContext, not printing to avoid infinite recursion>");
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

    private static final class ListSet<E> extends AbstractSet<E> implements Set<E> {

        protected final List<E> listImpl;

        public ListSet() {
            this.listImpl = new ArrayList<E>();
        }

        public int size() {
            return this.listImpl.size();
        }

        public Iterator<E> iterator() {
            return this.listImpl.iterator();
        }

        public boolean add(final E obj) {
            boolean added = false;

            if (!this.listImpl.contains(obj)) {
                added = this.listImpl.add(obj);
            }

            return added;
        }

        public boolean isEmpty() {
            return this.listImpl.isEmpty();
        }

        public boolean contains(final Object obj) {
            return this.listImpl.contains(obj);
        }

        public boolean remove(final Object obj) {
            return this.listImpl.remove(obj);
        }

        public void clear() {
            this.listImpl.clear();
        }

    }

}
