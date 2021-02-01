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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ofbiz.base.util.UtilGenerics;

/**
 * Map Context
 *
 * Provide a combined view for a collection of maps which are organized in a deque.
 * All write operations affect only the head of the deque.
 */
public class MapContext<K, V> implements Map<K, V>, LocalizedMap<V> {

    private Deque<Map<K, V>> contexts = new LinkedList<>();

    /**
     * Gets contexts.
     * @return the contexts
     */
    public Deque<Map<K, V>> getContexts() {
        return contexts;
    }

    /** Puts a new Map on the top of the stack */
    public void push() {
        contexts.addFirst(new HashMap<>());
    }

    /** Puts an existing Map on the top of the stack (top meaning will override lower layers on the stack) */
    public void push(Map<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot push null existing Map onto a MapContext");
        }
        contexts.addFirst(existingMap);
    }

    /** Puts an existing Map on the BOTTOM of the stack (bottom meaning will be overriden by lower layers on the stack,
     * ie everything else already there) */
    public void addToBottom(Map<K, V> existingMap) {
        if (existingMap == null) {
            throw new IllegalArgumentException("Error: cannot add null existing Map to bottom of a MapContext");
        }
        contexts.addLast(existingMap);
    }

    /** Remove and returns the Map from the top of the stack; if there is only one Map on the stack it returns null and does not remove it */
    public Map<K, V> pop() {
        // always leave at least one Map in the List, ie never pop off the last Map
        return contexts.size() > 1 ? contexts.removeFirst() : null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return contexts.stream()
                .flatMap(ctx -> ctx.keySet().stream())
                .distinct()
                .mapToInt(k -> 1)
                .sum();
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return contexts.stream().allMatch(Map::isEmpty);
    }

    // Return a sequential stream of actual entries.
    private Stream<Map.Entry<K, V>> entryStream() {
        Set<K> seenKeys = new HashSet<>();
        return contexts.stream()
                .flatMap(ctx -> ctx.entrySet().stream())
                .sequential()
                .filter(e -> seenKeys.add(e.getKey()));
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return contexts.stream().anyMatch(ctx -> ctx.containsKey(key));
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return entryStream().anyMatch(e -> Objects.equals(value, e.getValue()));
    }

    private V withContextContainingKey(Object key, Function<Map<K, V>, V> f) {
        for (Map<K, V> ctx: contexts) {
            /* Use `containsKey` rather than checking for null.
               This allows a null value at the head of the deque to override the followings. */
            if (ctx.containsKey(key)) {
                return f.apply(ctx);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public V get(Object key) {
        return withContextContainingKey(key, ctx -> ctx.get(key));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.base.util.collections.LocalizedMap#get(java.lang.String, java.util.Locale)
     */
    @Override
    public V get(String name, Locale locale) {
        return withContextContainingKey(name, ctx -> {
            if (ctx instanceof LocalizedMap<?>) {
                LocalizedMap<V> lmap = UtilGenerics.cast(ctx);
                return lmap.get(name, locale);
            }
            return ctx.get(name);
        });
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value) {
        return contexts.getFirst().put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public V remove(Object key) {
        return contexts.getFirst().remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> arg0) {
        contexts.getFirst().putAll(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        contexts.getFirst().clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<K> keySet() {
        return contexts.stream()
                .flatMap(ctx -> ctx.keySet().stream())
                .collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    @Override
    public Collection<V> values() {
        return entryStream()
                .map(Map.Entry::getValue)
                .collect(collectingAndThen(toList(), Collections::unmodifiableCollection));
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return entryStream()
                // Use LinkedHashSet for users relying on the insertion order of the inner maps.
                .collect(collectingAndThen(toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    @Override
    public String toString() {
        StringBuilder fullMapString = new StringBuilder();
        int curLevel = 0;
        for (Map<K, V> ctx: contexts) {
            fullMapString.append("============================== Start stack level " + curLevel + "\n");
            for (Map.Entry<K, V> curEntry: ctx.entrySet()) {

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
}
