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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * OrderedMap - HashMap backed by a linked list.
 *
 */
public class OrderedMap extends HashMap {

    private List orderedKeys = new LinkedList();

    /**
     * @see java.util.Map#keySet()
     */   
    public Set keySet() {
        return new OrderedSet(orderedKeys);
    }

    /**
     * @return List a copy of the ordered keys list which backs this map
     */
    public List getOrderedKeys() {
        return new LinkedList(this.orderedKeys);
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value) {
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
    public Object remove(Object key) {
        if (orderedKeys.contains(key))
            orderedKeys.remove(key);
        return super.remove(key);
    }
    
    /**
     * @see java.util.Map#values()
     */
    public Collection values() {
        Iterator i = orderedKeys.iterator();
        if (!i.hasNext()) {
            return null;
        }
        
        List values = new ArrayList();        
        while (i.hasNext()) {
            values.add(this.get(i.next()));
        }
        return (Collection) values;
    }

    public int indexOf(Object key) {
        return orderedKeys.indexOf(key);
    }
}
