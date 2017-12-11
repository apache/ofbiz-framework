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

import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;

/**
 * Map Stack
 *
 */
public class MapStack<K> extends MapContext<K, Object> {

    public static final String module = MapStack.class.getName();

    public static <K> MapStack<K> create() {
        MapStack<K> newValue = new MapStack<>();
        // initialize with a single entry
        newValue.push();
        return newValue;
    }

    @SuppressWarnings("unchecked")
    public static <K> MapStack<K> create(Map<K, Object> baseMap) {
        MapStack<K> newValue = new MapStack<>();
        if (baseMap instanceof MapStack) {
            newValue.stackList.addAll(((MapStack<K>) baseMap).stackList);
        } else {
            newValue.stackList.add(0, baseMap);
        }
        return newValue;
    }

    /** Does a shallow copy of the internal stack of the passed MapStack; enables simultaneous stacks that share common parent Maps */
    public static <K> MapStack<K> create(MapStack<K> source) {
        MapStack<K> newValue = new MapStack<>();
        newValue.stackList.addAll(source.stackList);
        return newValue;
    }

    protected MapStack() {
        super();
    }

    /**
     * Creates a MapStack object that has the same Map objects on its stack;
     * meant to be used to enable a
     * situation where a parent and child context are operating simultaneously
     * using two different MapStack objects, but sharing the Maps in common
     */
    @Override
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
    @Override
    public MapStack<K> standAloneChildStack() {
        MapStack<K> standAloneChild = MapStack.create(this);
        standAloneChild.push();
        return standAloneChild;
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public Object get(Object key) {
        if ("context".equals(key)) {
            return this;
        }

        return super.get(key);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.base.util.collections.LocalizedMap#get(java.lang.String, java.util.Locale)
     */
    @Override
    public Object get(String name, Locale locale) {
        if ("context".equals(name)) {
            return this;
        }

        return super.get(name, locale);
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public Object put(K key, Object value) {
        if ("context".equals(key)) {
            if (value == null || this != value) {
                Debug.logWarning("Putting a value in a MapStack with key [context] that is not this MapStack, will be hidden by the current MapStack self-reference: " + value, module);
            }
        }

        return super.put(key, value);
    }
}
