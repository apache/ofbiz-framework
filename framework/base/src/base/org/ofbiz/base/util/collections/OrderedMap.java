/*
 * $Id: OrderedMap.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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
