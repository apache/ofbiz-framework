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
import java.util.ConcurrentModificationException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.ofbiz.base.lang.Appender;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilObject;

@SuppressWarnings("serial")
public abstract class GenericMap<K, V> implements Appender<StringBuilder>, Map<K, V>, Serializable {
    private static final AtomicReferenceFieldUpdater<GenericMap<?, ?>, Set<?>> keySetUpdater = UtilGenerics.cast(AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Set.class, "keySet"));
    private static final AtomicReferenceFieldUpdater<GenericMap<?, ?>, Set<?>> entrySetUpdater = UtilGenerics.cast(AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Set.class, "entrySet"));
    private static final AtomicReferenceFieldUpdater<GenericMap<?, ?>, Collection<?>> valuesUpdater = UtilGenerics.cast(AtomicReferenceFieldUpdater.newUpdater(GenericMap.class, Collection.class, "values"));
    private static final AtomicIntegerFieldUpdater<GenericMap<?, ?>> modCountUpdater = UtilGenerics.cast(AtomicIntegerFieldUpdater.newUpdater(GenericMap.class, "modCount"));

    private volatile Set<K> keySet;
    private volatile Set<Map.Entry<K, V>> entrySet;
    private volatile Collection<V> values;
    private volatile int modCount;

    public int getModCount() {
        return modCount;
    }

    protected void incrementModCount() {
        modCountUpdater.getAndIncrement(this);
    }

    public final void clear() {
        if (isEmpty()) return;
        incrementModCount();
        clearInternal();
    }

    protected abstract void clearInternal();

    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map<?, ?>)) return false;
        if (this == o) return true;
        Map<?, ?> map = (Map<?, ?>) o;
        if (size() != map.size()) return false;
        if (o instanceof GenericMap<?, ?>) return equalsGenericMap((GenericMap<?, ?>) o);
        return equalsMap(map);
    }

    protected boolean equalsGenericMap(GenericMap<?, ?> map) {
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

    protected boolean equalsMap(Map<?, ?> map) {
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

    protected abstract class GenericMapIterator<DEST> extends IteratorWrapper<DEST, Map.Entry<K, V>> {
        private final int currentModCount = getModCount();

        protected GenericMapIterator(boolean noteAccess) {
            super(iterator(noteAccess));
        }

        protected boolean isValid(Map.Entry<K, V> src) {
            if (currentModCount != getModCount()) throw new ConcurrentModificationException();
            return true;
        }
    }

    public final Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySetUpdater.compareAndSet(this, null, new GenericMapEntrySet<K, V, GenericMap<K, V>>(this) {
                @Override
                protected boolean contains(Object key, Object value) {
                    return UtilObject.equalsHelper(get(key, false), value);
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator(boolean noteAccess) {
                    return new GenericMapIterator<Map.Entry<K, V>>(noteAccess) {
                        @Override
                        protected void noteRemoval(Map.Entry<K, V> dest, Map.Entry<K, V> src) {
                            // No need to note the remove, the wrapped iterator does that for us
                            // evictionPolicy.remove(evictionDeque, dest);
                            // if (diskStore != null) diskStore.remove(dest);
                        }

                        @Override
                        protected Map.Entry<K, V> convert(Map.Entry<K, V> src) {
                            return src;
                        }
                    };
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

                @Override
                public Iterator<K> iterator(boolean noteAccess) {
                    return new GenericMapIterator<K>(noteAccess) {
                        @Override
                        protected void noteRemoval(K dest, Map.Entry<K, V> src) {
                            // No need to note the remove, the wrapped iterator does that for us
                            // evictionPolicy.remove(evictionDeque, dest);
                            // if (diskStore != null) diskStore.remove(dest);
                        }

                        @Override
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
                @Override
                public Iterator<V> iterator(boolean noteAccess) {
                    return new GenericMapIterator<V>(noteAccess) {
                        @Override
                        protected void noteRemoval(V dest, Map.Entry<K, V> src) {
                            // No need to note the remove, the wrapped iterator does that for us
                            // evictionPolicy.remove(evictionDeque, src.getKey());
                            // if (diskStore != null) diskStore.remove(src.getKey());
                        }

                        @Override
                        protected V convert(Map.Entry<K, V> src) {
                            return src.getValue();
                        }
                    };
                }
            });
        }
        return values;
    }

    public final V remove(Object key) {
        return removeInternal(key, true);
    }

    protected abstract V removeInternal(Object key, boolean incrementModCount);

    public final void putAll(Map<? extends K, ? extends V> map) {
        putAllInternal(map);
    }

    private <KE extends K, VE extends V> void putAllInternal(Map<KE, VE> map) {
        if (map.isEmpty()) return;
        incrementModCount();
        Iterator<Map.Entry<KE, VE>> it;
        if (map instanceof GenericMap<?, ?>) {
            GenericMap<KE, VE> otherMap = UtilGenerics.cast(map);
            it = otherMap.iterator(false);
        } else {
            it = map.entrySet().iterator();
        }
        putAllIterator(it);
    }

    protected abstract <KE extends K, VE extends V> void putAllIterator(Iterator<Map.Entry<KE, VE>> it);

    @Override
    public String toString() {
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

