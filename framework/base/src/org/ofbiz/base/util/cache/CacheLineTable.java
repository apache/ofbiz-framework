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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.reardencommerce.kernel.collections.shared.evictable.ConcurrentLinkedHashMap;

import javolution.util.FastList;
import javolution.util.FastMap;

import jdbm.RecordManager;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ReadOnlyMapEntry;

@SuppressWarnings("serial")
public class CacheLineTable<K, V> implements Serializable {

    public static final String module = CacheLineTable.class.getName();
    // weak ref on this
    private static final ConcurrentMap<String, jdbm.RecordManager> fileManagers = new ConcurrentHashMap<String, jdbm.RecordManager>();

    protected transient RecordManager jdbmMgr = null;

    protected transient HTree<Object, CacheLine<V>> fileTable = null;
    protected Map<Object, CacheLine<V>> memoryTable = null;
    protected String fileStore = null;
    protected String cacheName = null;
    protected int maxInMemory = 0;

    public CacheLineTable(String fileStore, String cacheName, boolean useFileSystemStore, int maxInMemory) {
        this.fileStore = fileStore;
        this.cacheName = cacheName;
        this.maxInMemory = maxInMemory;
        if (useFileSystemStore) {
            // create the manager the first time it is needed
            jdbmMgr = fileManagers.get(fileStore);
            if (jdbmMgr == null) {
                Debug.logImportant("Creating file system cache store for cache with name: " + cacheName, module);
                try {
                    String ofbizHome = System.getProperty("ofbiz.home");
                    if (ofbizHome == null) {
                        Debug.logError("No ofbiz.home property set in environment", module);
                    } else {
                        jdbmMgr = new JdbmRecordManager(ofbizHome + "/" + fileStore);
                    }
                } catch (IOException e) {
                    Debug.logError(e, "Error creating file system cache store for cache with name: " + cacheName, module);
                }
                fileManagers.putIfAbsent(fileStore, jdbmMgr);
            }
            jdbmMgr = fileManagers.get(fileStore);
            if (jdbmMgr != null) {
                try {
                    this.fileTable = HTree.createInstance(jdbmMgr);
                    jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                    jdbmMgr.commit();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        if (maxInMemory > 0) {
            this.memoryTable = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, maxInMemory);
        } else {
            this.memoryTable = FastMap.newInstance();
        }
    }

    private Object fromKey(Object key) {
        return key == null ? ObjectType.NULL : key;
    }

    @SuppressWarnings("unchecked")
    private K toKey(Object key) {
        return key == ObjectType.NULL ? null : (K) key;
    }

    @SuppressWarnings("unchecked")
    private void addAllFileTableValues(Collection<CacheLine<V>> values) throws IOException {
        FastIterator<CacheLine<V>> iter = fileTable.values();
        CacheLine<V> value = iter.next();
        while (value != null) {
            values.add(value);
            value = iter.next();
        }
    }

    @SuppressWarnings("unchecked")
    private void addAllFileTableKeys(Set<Object> keys) throws IOException {
        FastIterator<Object> iter = fileTable.keys();
        Object key = null;
        while ((key = iter.next()) != null) {
            keys.add((K) key);
        }
    }

    public synchronized CacheLine<V> put(K key, CacheLine<V> value) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to put with null key, using NullObject" + this.cacheName, module);
        }
        Object nulledKey = fromKey(key);
        CacheLine<V> oldValue = memoryTable.put(nulledKey, value);
        if (fileTable != null) {
            try {
                if (oldValue == null) oldValue = fileTable.get(nulledKey);
                fileTable.put(nulledKey, value);
                jdbmMgr.commit();
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
        Object nulledKey = fromKey(key);
        CacheLine<V> value = memoryTable.get(nulledKey);
        if (value == null) {
            if (fileTable != null) {
                try {
                    value = fileTable.get(nulledKey);
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
        Object nulledKey = fromKey(key);
        CacheLine<V> value = memoryTable.remove(nulledKey);
        if (fileTable != null) {
            try {
                if (value == null) value = fileTable.get(nulledKey);
                fileTable.remove(nulledKey);
                jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        return value;
    }

    public synchronized Collection<? extends CacheLine<V>> values() {
        Collection<CacheLine<V>> values;

        if (fileTable != null) {
            values = FastList.newInstance();
            try {
                addAllFileTableValues(values);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            values = memoryTable.values();
        }

        return Collections.unmodifiableCollection(values);
    }

    private Map<String, Object> createLineInfo(int keyNum, K key, CacheLine<V> line) {
        Map<String, Object> lineInfo = FastMap.newInstance();
        lineInfo.put("elementKey", key);
        if (line.loadTime > 0) {
            lineInfo.put("expireTime", new Date(line.loadTime + line.expireTime));
        }
        lineInfo.put("lineSize", line.getSizeInBytes());
        lineInfo.put("keyNum", keyNum);
        return lineInfo;
    }

    public Collection<? extends Map<String, Object>> getLineInfos() {
        Set<? extends K> keys = keySet();
        List<Map<String, Object>> lineInfos = FastList.newInstance();
        int keyIndex = 0;
        for (K key: keySet()) {
            Object nulledKey = fromKey(key);
            CacheLine<V> line;
            if (fileTable != null) {
                try {
                    line = fileTable.get(nulledKey);
                } catch (IOException e) {
                    Debug.logError(e, module);
                    line = null;
                }
            } else {
                line = memoryTable.get(nulledKey);
            }
            if (line != null) {
                lineInfos.add(createLineInfo(keyIndex, key, line));
            }
            keyIndex++;
        }
        return lineInfos;
    }

    public synchronized Iterator<Map.Entry<K, ? extends CacheLine<V>>> iterator() {
        // this is a list, instead of a set, as the fileTable or
        // memoryTable has already deduped keys for us, and this ends up
        // being faster, as the hashCode/equals calls don't need to happen
        List<Map.Entry<K, ? extends CacheLine<V>>> list = FastList.newInstance();
        if (fileTable != null) {
            try {
                FastIterator<Object> iter = fileTable.keys();
                Object key = iter.next();
                while (key != null) {
                    CacheLine<V> value = fileTable.get(key);
                    list.add(new ReadOnlyMapEntry<K, CacheLine<V>>(UtilGenerics.<K>cast(toKey(key)), value));
                    key = iter.next();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            for (Map.Entry<Object, ? extends CacheLine<V>> entry: memoryTable.entrySet()) {
                list.add(new ReadOnlyMapEntry<K, CacheLine<V>>(UtilGenerics.<K>cast(toKey(entry.getKey())), entry.getValue()));
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
        Set<Object> keys = new HashSet<Object>();

        if (fileTable != null) {
            try {
                addAllFileTableKeys(keys);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            keys.addAll(memoryTable.keySet());
        }
        if (keys.remove(ObjectType.NULL)) {
            keys.add(null);
        }

        return Collections.unmodifiableSet((Set<? extends K>) keys);
    }

    public synchronized void clear() {
        if (UtilValidate.isNotEmpty(fileTable)) {
            try {
                // remove this table
                long recid = fileTable.getRecid();
                jdbmMgr.delete(recid);
                jdbmMgr.commit();
                this.fileTable = null;

                // create a new table
                this.fileTable = HTree.createInstance(jdbmMgr);
                jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        memoryTable.clear();
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
            return memoryTable.isEmpty();
        }
    }

    public synchronized int size() {
        if (fileTable != null) {
            return this.keySet().size();
        } else {
            return memoryTable.size();
        }
    }

    public synchronized void setLru(int newSize) {
        this.maxInMemory = newSize;

        // using linked map to preserve the order when using LRU (FastMap is a linked map)
        Map<Object, CacheLine<V>> oldmap = this.memoryTable;

        if (newSize > 0) {
            if (this.memoryTable instanceof ConcurrentLinkedHashMap) {
                Debug.logInfo("a setLru(" + newSize + ")", module);
                Debug.logInfo("before " + this.memoryTable.keySet(), module);
                ((ConcurrentLinkedHashMap) this.memoryTable).setCapacity(newSize);
                Debug.logInfo("after " + this.memoryTable.keySet(), module);
                return;
            } else {
                Debug.logInfo("b setLru(" + newSize + ")", module);
                this.memoryTable = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, newSize);
            }
        } else {
            Debug.logInfo("c setLru(" + newSize + ")", module);
            this.memoryTable = FastMap.newInstance();
        }

        this.memoryTable.putAll(oldmap);
    }

    public synchronized K getKeyFromMemory(int index) {
        int currentIdx = 0;
        for (Object key: memoryTable.keySet()) {
            if (currentIdx == index) {
                return toKey(key);
            }
            currentIdx++;
        }
        return null;
    }
}
