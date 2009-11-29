/*******n***********************************************************************
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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.ofbiz.base.util.Appender;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilObject;

public abstract class GenericMap<K, V> implements Appender<StringBuilder>, Map<K, V>, Serializable {
    private static final AtomicReferenceFieldUpdater<GenericMap, Set> keySetUpdater = AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Set.class, "keySet");
    private static final AtomicReferenceFieldUpdater<GenericMap, Set> entrySetUpdater = AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Set.class, "entrySet");
    private static final AtomicReferenceFieldUpdater<GenericMap, Collection> valuesUpdater = AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Collection.class, "values");

    private volatile Set<K> keySet;
    private volatile Set<Map.Entry<K, V>> entrySet;
    private volatile Collection<V> values;

    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map)) return false;
        if (this == o) return true;
        Map map = (Map) o;
        if (size() != map.size()) return false;
        if (o instanceof GenericMap) return equalsGenericMap((GenericMap) o);
        return equalsMap(map);
    }

    protected boolean equalsGenericMap(GenericMap map) {
        Iterator<Map.Entry<K, V>> it = iterator(false);
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            K key = entry.getKey();
            V value = entry.getValue();
            if (value != null) {
                if (!value.equals(map.get(key, false))) return false;
            } else {
                Object otherValue = map.get(key, false);
                if (otherValue != null) return false;
                if (!map.containsKey(key)) return false;
            }
        }
        return true;
    }

    protected boolean equalsMap(Map map) {
        Iterator<Map.Entry<K, V>> it = iterator(false);
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            K key = entry.getKey();
            V value = entry.getValue();
            if (value != null) {
                if (!value.equals(map.get(key))) return false;
            } else {
                Object otherValue = map.get(key);
                if (otherValue != null) return false;
                if (!map.containsKey(key)) return false;
            }
        }
        return true;
    }

    public final V get(Object key) {
        return get(key, true);
    }

    protected abstract V get(Object key, boolean noteAccess);

    public final Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySetUpdater.compareAndSet(this, null, new GenericMapEntrySet<K, V, GenericMap<K, V>>(this) {
                protected boolean contains(Object key, Object value) {
                    return UtilObject.equalsHelper(get(key, false), value);
                }

                public Iterator<Map.Entry<K, V>> iterator(boolean noteAccess) {
                    return GenericMap.this.iterator(noteAccess);
                }
            });
        }
        return entrySet;
    }

    protected abstract Iterator<Map.Entry<K, V>> iterator(boolean noteAccess);

    public final Set<K> keySet() {
        if (keySet == null) {
            keySetUpdater.compareAndSet(this, null, new GenericMapKeySet<K, V, GenericMap<K, V>>(this) {
                public boolean contains(Object key) {
                    return containsKey(key);
                }

                public Iterator<K> iterator(boolean noteAccess) {
                    return new IteratorWrapper<K, Map.Entry<K, V>>(GenericMap.this.iterator(noteAccess)) {
                        protected void noteRemoval(K dest, Map.Entry<K, V> src) {
                            // No need to note the remove, the wrapped iterator does that for us
                            // evictionPolicy.remove(evictionDeque, dest);
                            // if (diskStore != null) diskStore.remove(dest);
                        }

                        protected K convert(Map.Entry<K, V> src) {
                            return src.getKey();
                        }
                    };
                }
            });
        }
        return keySet;
    }

    public final Collection<V> values() {
        if (values == null) {
            valuesUpdater.compareAndSet(this, null, new GenericMapValues<K, V, GenericMap<K, V>>(this) {
                public Iterator<V> iterator(boolean noteAccess) {
                    return new IteratorWrapper<V, Map.Entry<K, V>>(GenericMap.this.iterator(noteAccess)) {
                        protected void noteRemoval(V dest, Map.Entry<K, V> src) {
                            // No need to note the remove, the wrapped iterator does that for us
                            // evictionPolicy.remove(evictionDeque, src.getKey());
                            // if (diskStore != null) diskStore.remove(src.getKey());
                        }

                        protected V convert(Map.Entry<K, V> src) {
                            return src.getValue();
                        }
                    };
                }
            });
        }
        return values;
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        putAllInternal(map);
    }

    private <KE extends K, VE extends V> void putAllInternal(Map<KE, VE> map) {
        Iterator<Map.Entry<KE, VE>> it;
        if (map instanceof GenericMap) {
            GenericMap<KE, VE> otherMap = UtilGenerics.cast(map);
            it = otherMap.iterator(false);
        } else {
            it = map.entrySet().iterator();
        }
        putAll(it);
    }

    protected abstract <KE extends K, VE extends V> void putAll(Iterator<Map.Entry<KE, VE>> it);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        return appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("{");
        Iterator<Map.Entry<K, V>> it = iterator(false);
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) sb.append(",");
        }
        return sb.append("}");
    }
}

