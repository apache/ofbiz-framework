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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
/**
 * @deprecated
 * OrderedMap - HashMap backed by a linked list.
 *
 */
public class OrderedMap<K, V> extends HashMap<K, V> {

    private List<K> orderedKeys = FastList.newInstance();

    /**
     * @see java.util.Map#keySet()
     */   
    public Set<K> keySet() {
        return new LinkedHashSet<K>(orderedKeys);
    }

    /**
     * @return List a copy of the ordered keys list which backs this map
     */
    public List<K> getOrderedKeys() {
        List<K> keys = FastList.newInstance();
        keys.addAll(this.orderedKeys);
        return keys;
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put(K key, V value) {
        if (!orderedKeys.contains(key))
            orderedKeys.add(key);
        return super.put(key, value);
    }

    /**
     * @see java.util.Map#clear()
     */   
    public void clear() {
        super.clear();
        orderedKeys.clear();
    }
   
    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(Object key) {
        if (orderedKeys.contains(key))
            orderedKeys.remove(key);
        return super.remove(key);
    }
    
    /**
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        if (orderedKeys.isEmpty()) {
            return null;
        }
        
        List<V> values = FastList.newInstance();
        for (K key: orderedKeys) {
            values.add(this.get(key));
        }
        return values;
    }

    public int indexOf(Object key) {
        return orderedKeys.indexOf(key);
    }
}
