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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Adapter which allows viewing a multi-value map as a single-value map.
public class MultivaluedMapContextAdapter<K, V> implements Map<K, V> {
    private MultivaluedMapContext<K, V> adaptee;

    public MultivaluedMapContextAdapter(MultivaluedMapContext<K, V> adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public int size() {
        return adaptee.size();
    }

    @Override
    public boolean isEmpty() {
        return adaptee.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return adaptee.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return adaptee.values().stream()
                .map(l -> l.get(0))
                .anyMatch(value::equals);
    }

    @Override
    public V get(Object key) {
        return adaptee.getFirst(key);
    }

    @Override
    public V put(K key, V value) {
        V prev = get(key);
        adaptee.putSingle(key, value);
        return prev;
    }

    @Override
    public V remove(Object key) {
        V prev = get(key);
        adaptee.remove(key);
        return prev;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(adaptee::putSingle);
    }

    @Override
    public void clear() {
        adaptee.clear();
    }

    @Override
    public Set<K> keySet() {
        return adaptee.keySet();
    }

    @Override
    public Collection<V> values() {
        return adaptee.values().stream()
                .map(l -> l.get(0))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return adaptee.keySet().stream()
                .collect(Collectors.toMap(k -> k, this::get))
                .entrySet();
    }
}
