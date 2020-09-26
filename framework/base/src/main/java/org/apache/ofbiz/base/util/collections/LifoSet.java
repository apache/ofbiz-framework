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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * LifoSet - Set interface wrapper around a LinkedList
 *
 */
@SuppressWarnings("serial")
public class LifoSet<V> extends AbstractSet<V> implements Serializable {

    // This set's back LinkedList
    private LinkedList<V> backedList = new LinkedList<>();
    private int maxCapacity = 10;

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     */
    public LifoSet() { }

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     * @param capacity the collection whose elements are to be placed into this set.
     */
    public LifoSet(int capacity) {
        maxCapacity = capacity;
    }

    /**
     * Sets the max capacity for this LifoSet
     * @param capacity Max Size (as integer)
     */
    public void setCapactity(int capacity) {
        this.maxCapacity = capacity;
    }

    /**
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        return backedList.size();
    }

    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(V obj) {
        int index = backedList.indexOf(obj);

        if (index == -1) {
            backedList.addFirst(obj);
            while (size() > maxCapacity) {
                backedList.removeLast();
            }
        } else {
            backedList.remove(index);
            backedList.addFirst(obj);
        }
        return true;
    }

    /**
     * @see java.util.Collection#iterator()
     */
    @Override
    public Iterator<V> iterator() {
        return backedList.iterator();
    }

    // Stack Implementation (implements all Stack methods as per the java.util.Stack object

    /**
     * @see java.util.Stack#empty()
     * @return true if and only if this stack contains no items; false otherwise
     */
    public boolean empty() {
        return this.size() == 0;
    }

    /**
     * @see java.util.Stack#push(java.lang.Object)
     * @param item The item to be pushed onto this stack
     */
    public void push(V item) {
        this.add(item);
    }

    /**
     * @see java.util.Stack#pop()
     * @return The object at the top of this stack
     * @throws EmptyStackException If this stack is empty
     */
    public V pop() throws EmptyStackException {
        if (this.size() > 0) {
            return backedList.removeFirst();
        }
        throw new EmptyStackException();
    }

    /**
     * @see java.util.Stack#peek()
     * @return The object at the top of this stack
     * @throws EmptyStackException If this stack is empty
     */
    public V peek() throws EmptyStackException {
        if (this.size() > 0) {
            return backedList.getFirst();
        }
        throw new EmptyStackException();
    }

    /**
     * @see java.util.Stack#search(java.lang.Object)
     * @param item The desired object
     * @return The 1-based position from the top of the stack where the object is located;
     * the return value -1  indicates that the object is not on the stack
     */
    public int search(Object item) {
        int index = backedList.indexOf(item);
        if (index > -1) {
            return index + 1; // this method is 1 based (per java.util.Stack)
        }
        return -1;
    }
}

