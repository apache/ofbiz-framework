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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilObject;

public abstract class GenericMapValues<K, V, M extends Map<K, V>> extends GenericMapCollection<K, V, M, V> {
    public GenericMapValues(M source) {
        super(source);
    }

    @Override
    public boolean contains(Object item) {
        Iterator<V> it = iterator(false);
        while (it.hasNext()) {
            if (UtilObject.equalsHelper(item, it.next())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Collection<?>)) {
            return false;
        }
        if (o instanceof List<?> || o instanceof Set<?>) {
            return false;
        }
        Collection<?> other = (Collection<?>) o;
        if (source.size() != other.size()) {
            return false;
        }
        Iterator<V> it = iterator(false);
        while (it.hasNext()) {
            V item = it.next();
            if (!other.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        Iterator<V> it = iterator(false);
        while (it.hasNext()) {
            V item = it.next();
            if (item == null) {
                continue;
            }
            h += item.hashCode();
        }
        return h;
    }

    @Override
    public boolean remove(Object item) {
        Iterator<V> it = iterator(false);
        while (it.hasNext()) {
            if (UtilObject.equalsHelper(item, it.next())) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}

