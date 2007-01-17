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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * OrderedSet - Set interface wrapper around a LinkedList
 *
 */
public class OrderedSet extends AbstractSet {

    // This set's back LinkedList
    private List backedList = new LinkedList();

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     */
    public OrderedSet() {}

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this set.
     */
    public OrderedSet(Collection c) {
        Iterator i = c.iterator();

        while (i.hasNext())
            add(i.next());
    }

    /**
     * @see java.util.Collection#iterator()
     */  
    public Iterator iterator() {
        return backedList.iterator();
    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        return backedList.size();
    }

    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object obj) {
        int index = backedList.indexOf(obj);

        if (index == -1)
            return backedList.add(obj);
        else {
            backedList.set(index, obj);
            return false;
        }
    }
}
