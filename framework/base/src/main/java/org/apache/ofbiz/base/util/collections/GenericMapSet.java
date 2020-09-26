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

import java.util.Map;
import java.util.Set;

public abstract class GenericMapSet<K, V, M extends Map<K, V>, I> extends GenericMapCollection<K, V, M, I> implements Set<I> {
    public GenericMapSet(M source) {
        super(source);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Set<?>)) {
            return false;
        }
        Set<?> other = (Set<?>) o;
        if (getSource().size() != other.size()) {
            return false;
        }
        for (I item: this) {
            if (!other.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final int hashCode() {
        int h = 0;
        for (I item: this) {
            if (item == null) {
                continue;
            }
            h += item.hashCode();
        }
        return h;
    }
}

