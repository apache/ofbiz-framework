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

import java.util.LinkedList;
import java.util.List;

/**
 * MultivaluedMap Context
 *
 * A MapContext which handles multiple values for the same key.
 */
public class MultivaluedMapContext<K, V> extends MapContext<K, List<V>> {

    public static final String module = MultivaluedMapContext.class.getName();

    /**
     * Create a multi-value map initialized with one context
     */
    public MultivaluedMapContext() {
        push();
    }

    /**
     * Associate {@code key} with the single value {@code value}.
     * If other values are already associated with {@code key} then override them.
     *
     * @param key the key to associate {@code value} with
     * @param value the value to add to the context
     */
    public void putSingle(K key, V value) {
        List<V> box = new LinkedList<>();
        box.add(value);
        put(key, box);
    }

    /**
     * Associate {@code key} with the single value {@code value}.
     * If other values are already associated with {@code key},
     * then add {@code value} to them.
     *
     * @param key the key to associate {@code value} with
     * @param value the value to add to the context
     */
    public void add(K key, V value) {
        List<V> cur = contexts.getFirst().get(key);
        if (cur == null) {
            cur = new LinkedList<>();
            /* if this method is called after a context switch, copy the previous values
               in current context to not mask them. */
            List<V> old = get(key);
            if (old != null) {
                cur.addAll(old);
            }
        }
        cur.add(value);
        put(key, cur);
    }

    /**
     * Get the first value contained in the list of values associated with {@code key}.
     *
     * @param key a candidate key
     * @return the first value associated with {@code key} or null if no value
     * is associated with it.
     */
    public V getFirst(Object key) {
        List<V> res = get(key);
        return res == null ? null : res.get(0);
    }
}
