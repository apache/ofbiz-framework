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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LifoSet - Set interface wrapper around a LinkedList
 *
 */
@SuppressWarnings("serial")
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private int maxSize;

    public LRUMap() {
        this(10);
    }

    public LRUMap(int size) {
        this(size, 16);
    }

    public LRUMap(int size, int initialCapacity) {
        this(size, initialCapacity, (float) .75);
    }

    public LRUMap(int size, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
        maxSize = size;
    }

    /**
     * Sets the max capacity for this LRUMap
     * @param size Max Size (as integer)
     */
    public void setMaxSize(int size) {
        this.maxSize = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > maxSize;
    }
}
