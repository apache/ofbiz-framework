/*
 * $Id: LifoSet.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.EmptyStackException;

/**
 * LifoSet - Set interface wrapper around a LinkedList
 *
 * @author     <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.1
 */
public class LifoSet extends AbstractSet implements Serializable {

    // This set's back LinkedList
    private LinkedList backedList = new LinkedList();
    private int maxCapacity = 10;

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     */
    public LifoSet() {}

    /**
     * Constructs a set containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
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
    public int size() {
        return backedList.size();
    }

    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object obj) {
        int index = backedList.indexOf(obj);

        if (index == -1) {
            backedList.addFirst(obj);
            while (size() > maxCapacity)
                backedList.removeLast();
        } else {
            backedList.remove(index);
            backedList.addFirst(obj);
        }
        return true;
    }

    /**
     * @see java.util.Collection#iterator()         
     */  
    public Iterator iterator() {
        return backedList.iterator();
    }

    // Stack Implementation (implements all Stack methods as per the java.util.Stack object

    /**
     * @see java.util.Stack#empty()
     *
     * @return true if and only if this stack contains no items; false otherwise
     */
    public boolean empty() {
        if (this.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * @see java.util.Stack#push(java.lang.Object)
     *
     * @param item The item to be pushed onto this stack
     */
    public void push(Object item) {
        this.add(item);
    }

    /**
     * @see java.util.Stack#pop()
     *
     * @return The object at the top of this stack
     * @throws EmptyStackException If this stack is empty
     */
    public Object pop() throws EmptyStackException {
        if (this.size() > 0) {
            return backedList.removeFirst();
        }
        throw new EmptyStackException();
    }

    /**
     * @see java.util.Stack#peek()
     *
     * @return The object at the top of this stack
     * @throws EmptyStackException If this stack is empty
     */
    public Object peek() throws EmptyStackException {
        if (this.size() > 0) {
            return backedList.getFirst();
        }
        throw new EmptyStackException();
    }

    /**
     * @see java.util.Stack#search(java.lang.Object)
     *
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

