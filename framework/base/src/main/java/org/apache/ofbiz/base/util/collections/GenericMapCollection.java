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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class GenericMapCollection<K, V, M extends Map<K, V>, I> implements Collection<I> {
    protected final M source;

    public GenericMapCollection(M source) {
        this.source = source;
    }

    public boolean add(I item) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends I> collection) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        source.clear();
    }

    public boolean containsAll(Collection<?> collection) {
        for (Object item: collection) {
            if (!contains(item)) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }

    public final Iterator<I> iterator() {
        return iterator(true);
    }

    protected abstract Iterator<I> iterator(boolean noteAccess);

    public boolean removeAll(Collection<?> collection) {
        int count = 0;
        for (Object item: collection) {
            if (remove(item)) count++;
        }
        return count > 0;
    }

    public boolean retainAll(Collection<?> collection) {
        int count = 0;
        Iterator<I> it = iterator(false);
        while (it.hasNext()) {
            I item = it.next();
            if (!collection.contains(item)) {
                it.remove();
                count++;
            }
        }
        return count > 0;
    }

    public int size() {
        return source.size();
    }

    public Object[] toArray() {
        List<I> list = new LinkedList<I>();
        Iterator<I> it = iterator(false);
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray();
    }

    public <T> T[] toArray(T[] array) {
        List<Object> list = new LinkedList<Object>();
        Iterator<I> it = iterator(false);
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray(array);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("[");
        Iterator<I> it = iterator(false);
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) sb.append(", ");
        }
        return sb.append("]");
    }
}

