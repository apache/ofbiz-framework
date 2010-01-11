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
package org.ofbiz.base.util.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LRUMap;
import org.ofbiz.base.util.collections.ReadOnlyMapEntry;

@SuppressWarnings("serial")
public class CacheLineTable<K, V> implements Serializable {

    public static final String module = CacheLineTable.class.getName();
    protected static transient jdbm.RecordManager jdbmMgr = null;

    protected transient jdbm.htree.HTree fileTable = null;
    protected Map<K, CacheLine<V>> memoryTable = null;
    protected String fileStore = null;
    protected String cacheName = null;
    protected int maxInMemory = 0;
    protected boolean isNullSet = false;
    protected CacheLine<V> nullValue = null;

    public CacheLineTable(String fileStore, String cacheName, boolean useFileSystemStore, int maxInMemory) {
        this.fileStore = fileStore;
        this.cacheName = cacheName;
        this.maxInMemory = maxInMemory;
        if (useFileSystemStore) {
            // create the manager the first time it is needed
            if (CacheLineTable.jdbmMgr == null) {
                synchronized (this) {
                    if (CacheLineTable.jdbmMgr == null) {
                        try {
                            Debug.logImportant("Creating file system cache store for cache with name: " + cacheName, module);
                            CacheLineTable.jdbmMgr = new JdbmRecordManager(fileStore);
                        } catch (IOException e) {
                            Debug.logError(e, "Error creating file system cache store for cache with name: " + cacheName, module);
                        }
                    }
                }
            }
            if (CacheLineTable.jdbmMgr != null) {
                try {
                    long recno = CacheLineTable.jdbmMgr.getNamedObject(cacheName);
                    if (recno != 0) {
                        this.fileTable = jdbm.htree.HTree.load(CacheLineTable.jdbmMgr, recno);
                    } else {
                        this.fileTable = jdbm.htree.HTree.createInstance(CacheLineTable.jdbmMgr);
                        CacheLineTable.jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                        CacheLineTable.jdbmMgr.commit();
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        this.setLru(maxInMemory);
    }

    @SuppressWarnings("unchecked")
    private CacheLine<V> getFileTable(Object key) throws IOException {
        return (CacheLine<V>) fileTable.get(key);
    }


    @SuppressWarnings("unchecked")
    private void addAllFileTableValues(List<CacheLine<V>> values) throws IOException {
        jdbm.helper.FastIterator iter = fileTable.values();
        Object value = iter.next();
        while (value != null) {
            values.add((CacheLine<V>) value);
            value = iter.next();
        }
    }

    @SuppressWarnings("unchecked")
    private void addAllFileTableKeys(Set<K> keys) throws IOException {
        jdbm.helper.FastIterator iter = fileTable.keys();
        Object key = null;
        while ((key = iter.next()) != null) {
            if (key instanceof ObjectType.NullObject) {
                keys.add(null);
            } else {
                keys.add((K) key);
            }
        }
    }

    public synchronized CacheLine<V> put(K key, CacheLine<V> value) {
        CacheLine<V> oldValue;
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to put with null key, using NullObject" + this.cacheName, module);
            if (memoryTable instanceof FastMap) {
                oldValue = isNullSet ? nullValue : null;
                isNullSet = true;
                nullValue = value;
            } else {
                oldValue = memoryTable.put(key, value);
            }
        } else {
            oldValue = memoryTable.put(key, value);
        }
        if (fileTable != null) {
            try {
                if (oldValue == null) oldValue = getFileTable(key);
                fileTable.put(key != null ? key : ObjectType.NULL, value);
                CacheLineTable.jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        return oldValue;
    }

    public synchronized CacheLine<V> get(Object key) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to get with null key, using NullObject" + this.cacheName, module);
        }
        return getNoCheck(key);
    }

    protected CacheLine<V> getNoCheck(Object key) {
        CacheLine<V> value;
        if (memoryTable instanceof FastMap) {
            if (key == null) {
                value = isNullSet ? nullValue : null;
            } else {
                value = memoryTable.get(key);
            }
        } else {
            value = memoryTable.get(key);
        }
        if (value == null) {
            if (fileTable != null) {
                try {
                    value = getFileTable(key != null ? key : ObjectType.NULL);
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return value;
    }

    public synchronized CacheLine<V> remove(Object key) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to remove with null key, using NullObject" + this.cacheName, module);
        }
        CacheLine<V> value = this.getNoCheck(key);
        if (fileTable != null) {
            try {
                fileTable.remove(key != null ? key : ObjectType.NULL);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        if (key == null) {
            if (memoryTable instanceof FastMap) {
                isNullSet = false;
                nullValue = null;
            } else {
                memoryTable.remove(key);
            }
        } else {
            memoryTable.remove(key);
        }
        return value;
    }

    public synchronized Collection<? extends CacheLine<V>> values() {
        List<CacheLine<V>> values = FastList.newInstance();

        if (fileTable != null) {
            try {
                addAllFileTableValues(values);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            if (isNullSet) values.add(nullValue);
            values.addAll(memoryTable.values());
        }

        return values;
    }

    public synchronized Iterator<Map.Entry<K, ? extends CacheLine<V>>> iterator() {
        // this is a list, instead of a set, as the fileTable or
        // memoryTable has already deduped keys for us, and this ends up
        // being faster, as the hashCode/equals calls don't need to happen
        List<Map.Entry<K, ? extends CacheLine<V>>> list = FastList.newInstance();
        if (fileTable != null) {
            try {
                jdbm.helper.FastIterator iter = fileTable.keys();
                Object key = iter.next();
                while (key != null) {
                    CacheLine<V> value = UtilGenerics.cast(fileTable.get(key));
                    if (key instanceof ObjectType.NullObject) {
                        key = null;
                    }
                    list.add(new ReadOnlyMapEntry<K, CacheLine<V>>(UtilGenerics.<K>cast(key), value));
                    key = iter.next();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            list.addAll(memoryTable.entrySet());
            if (isNullSet) {
                list.add(new ReadOnlyMapEntry<K, CacheLine<V>>(null, nullValue));
            }
        }
        return list.iterator();
    }

    /**
     *
     * @return An unmodifiable Set for the keys for this cache; to remove while iterating call the remove method on this class.
     */
    public synchronized Set<? extends K> keySet() {
        // note that this must be a HashSet and not a FastSet in order to have a null value
        Set<K> keys = new HashSet<K>();

        if (fileTable != null) {
            try {
                addAllFileTableKeys(keys);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            keys.addAll(memoryTable.keySet());
            if (isNullSet) keys.add(null);
        }

        return Collections.unmodifiableSet(keys);
    }

    public synchronized void clear() {
        if (UtilValidate.isNotEmpty(fileTable)) {
            try {
                // remove this table
                long recid = fileTable.getRecid();
                CacheLineTable.jdbmMgr.delete(recid);
                CacheLineTable.jdbmMgr.commit();
                this.fileTable = null;

                // create a new table
                this.fileTable = jdbm.htree.HTree.createInstance(CacheLineTable.jdbmMgr);
                CacheLineTable.jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                CacheLineTable.jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        memoryTable.clear();
        isNullSet = false;
        nullValue = null;
    }

    public synchronized boolean isEmpty() {
        if (fileTable != null) {
            try {
                return fileTable.keys().next() == null;
            } catch (IOException e) {
                Debug.logError(e, module);
                return false;
            }
        } else {
            if (isNullSet) {
                return false;
            } else {
                return memoryTable.isEmpty();
            }
        }
    }

    public synchronized int size() {
        if (fileTable != null) {
            return this.keySet().size();
        } else {
            if (isNullSet) {
                return memoryTable.size() + 1;
            } else {
                return memoryTable.size();
            }
        }
    }

    public synchronized void setLru(int newSize) {
        this.maxInMemory = newSize;

        Map<K, CacheLine<V>> oldmap = null;
        if (this.memoryTable != null) {
            // using linked map to preserve the order when using LRU (FastMap is a linked map)
            oldmap = FastMap.newInstance();
            oldmap.putAll(this.memoryTable);
        }

        if (newSize > 0) {
            this.memoryTable = Collections.synchronizedMap(new LRUMap<K, CacheLine<V>>(newSize));
            if (isNullSet) {
                this.memoryTable.put(null, nullValue);
                isNullSet = false;
                nullValue = null;
            }
        } else {
            this.memoryTable = FastMap.newInstance();
        }

        if (oldmap != null) {
            this.memoryTable.putAll(oldmap);
        }
    }

    public synchronized K getKeyFromMemory(int index) {
        int currentIdx = 0;
        if (isNullSet) {
            if (currentIdx == index) {
                return null;
            }
            currentIdx++;
        }
        for (K key: memoryTable.keySet()) {
            if (currentIdx == index) {
                return key;
            }
            currentIdx++;
        }
        return null;
    }
}
