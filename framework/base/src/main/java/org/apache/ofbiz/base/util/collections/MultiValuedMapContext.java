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

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.ofbiz.base.util.UtilGenerics;

/**
 * Map Stack
 *
 */
public class MultiValuedMapContext<K, V> implements MultivaluedMap<K, V>, LocalizedMap<V> {

    public static final String module = MultiValuedMapContext.class.getName();

    public static final <K, V> MultiValuedMapContext<K, V> getMapContext() {
        return new MultiValuedMapContext<>();
    }

    public static <K, V> MultiValuedMapContext<K, V> createMapContext() {
        MultiValuedMapContext<K, V> newValue = MultiValuedMapContext.getMapContext();
        // initialize with a single entry
        newValue.push();
        return newValue;
    }

    public static <K, V> MultiValuedMapContext<K, V> createMapContext(MultivaluedMap<K, V> baseMap) {
        if (baseMap instanceof MultiValuedMapContext) {
            return createMapContext((MultiValuedMapContext<K, V>) baseMap);
        } else {
            MultiValuedMapContext<K, V> newValue = MultiValuedMapContext.getMapContext();
            newValue.maps.addFirst(baseMap);
            return newValue;
        }
    }

    /** Does a shallow copy of the internal stack of the passed MapContext; enables simultaneous stacks that share common parent Maps */
    public static <K, V> MultiValuedMapContext<K, V> createMapContext(MultiValuedMapContext<K, V> source) {
        MultiValuedMapContext<K, V> newValue = MultiValuedMapContext.getMapContext();
        newValue.maps.addAll(source.maps);
        return newValue;
    }

    protected MultiValuedMapContext() {
        super();
    }

    protected Deque<MultivaluedMap<K, V>> maps = new LinkedList<>();

    public void reset() {
        maps = new LinkedList<>();
    }

    /** Puts a new Map on the top of the stack */
    public void push() {
        MultivaluedMap<K, V> newMap = new MultivaluedHashMap<>();
        this.maps.addFirst(newMap);
    }

    /** Puts an existing Map on the top of the stack (top meaning will override lower layers on the stack) */
    public void push(MultivaluedMap<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot push null existing Map onto a MapContext");
        }
        this.maps.addFirst(existingMap);
    }

    /** Puts an existing Map on the BOTTOM of the stack (bottom meaning will be overriden by lower layers on the stack, ie everything else already there) */
    public void addToBottom(MultivaluedMap<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot add null existing Map to bottom of a MapContext");
        }
        this.maps.add(existingMap);
    }

    /** Remove and returns the Map from the top of the stack; if there is only one Map on the stack it returns null and does not remove it */
    public MultivaluedMap<K, V> pop() {
        // always leave at least one Map in the List, ie never pop off the last Map
        return (maps.size() < 1) ? null : maps.removeFirst();
    }

    /**
     * Creates a MapContext object that has the same Map objects on its stack;
     * meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapContext objects, but sharing the Maps in common
     */
    public MultiValuedMapContext<K, V> standAloneStack() {
        MultiValuedMapContext<K, V> standAlone = MultiValuedMapContext.createMapContext(this);
        return standAlone;
    }

    /**
     * Creates a MapContext object that has the same Map objects on its stack,
     * but with a new Map pushed on the top; meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapContext objects, but sharing the Maps in common
     */
    public MultiValuedMapContext<K, V> standAloneChildStack() {
        MultiValuedMapContext<K, V> standAloneChild = MultiValuedMapContext.createMapContext(this);
        standAloneChild.push();
        return standAloneChild;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return maps.stream().allMatch(MultivaluedMap::isEmpty);
    }

    @Override
    public boolean containsKey(Object key) {
        return maps.stream().anyMatch(map -> map.containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return maps.stream().anyMatch(map -> map.containsValue(value));
    }

    @Override
    public List<V> get(Object key) {
        return maps.stream()
                   .filter(m -> m.containsKey(key))
                   .flatMap(m -> m.get(key).stream())
                   .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(String name, Locale locale) {
        for (MultivaluedMap<K, V> curMap: maps) {
            // only return if the curMap contains the key, rather than checking for null; this allows a null at a lower level to override a value at a higher level
            if (curMap.containsKey(name)) {
                if (curMap instanceof LocalizedMap<?>) {
                    LocalizedMap<V> lmap = UtilGenerics.cast(curMap);
                    return lmap.get(name, locale);
                }
                return curMap.get((K) name).stream().findFirst().get();
            }
        }
        return null;
    }

    @Override
    public List<V> remove(Object key) {
        return maps.getFirst().remove(key);
    }

    @Override
    public void clear() {
        maps.getFirst().clear();
    }

    // Convert an iterator to a stream.
    private static <U> Stream<U> stream(Iterator<U> it) {
        Iterable<U> dmaps = () -> it;
        return StreamSupport.stream(dmaps.spliterator(), false);
    }

    @Override
    public Set<K> keySet() {
        // Collect in reverse order to let the first maps masks the next ones.
        return stream(maps.descendingIterator())
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public List<V> put(K key, List<V> value) {
        return maps.getFirst().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> m) {
        maps.getFirst().putAll(m);
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        // Collect in reverse order to let the first maps masks the next ones.
        return stream(maps.descendingIterator())
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void putSingle(K key, V value) {
        maps.getFirst().putSingle(key, value);
    }

    @Override
    public void add(K key, V value) {
        maps.getFirst().add(key, value);
    }

    @Override
    public V getFirst(K key) {
        return maps.stream()
                   .map(m -> m.get(key))
                   .filter(Objects::nonNull)
                   .flatMap(List::stream)
                   .findFirst()
                   .orElse(null);
    }

    @Override
    public Collection<List<V>> values() {
        return maps.stream()
                   .flatMap(m -> m.values().stream())
                   .collect(Collectors.toList());
    }
}
