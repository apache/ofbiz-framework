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
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javolution.util.FastList;
import javolution.util.FastMap;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;

/**
 * Generalized caching utility. Provides a number of caching features:
 * <ul>
 *   <li>Limited or unlimited element capacity
 *   <li>If limited, removes elements with the LRU (Least Recently Used) algorithm
 *   <li>Keeps track of when each element was loaded into the cache
 *   <li>Using the expireTime can report whether a given element has expired
 *   <li>Counts misses and hits
 * </ul>
 *
 */
@SuppressWarnings("serial")
public class UtilCache<K, V> implements Serializable, EvictionListener<Object, CacheLine<V>> {

    public static final String module = UtilCache.class.getName();

    /** A static Map to keep track of all of the UtilCache instances. */
    private static final ConcurrentHashMap<String, UtilCache<?, ?>> utilCacheTable = new ConcurrentHashMap<String, UtilCache<?, ?>>();

    /** An index number appended to utilCacheTable names when there are conflicts. */
    private final static ConcurrentHashMap<String, AtomicInteger> defaultIndices = new ConcurrentHashMap<String, AtomicInteger>();

    /** The name of the UtilCache instance, is also the key for the instance in utilCacheTable. */
    private final String name;

    /** A count of the number of cache hits */
    protected AtomicLong hitCount = new AtomicLong(0);

    /** A count of the number of cache misses because it is not found in the cache */
    protected AtomicLong missCountNotFound = new AtomicLong(0);
    /** A count of the number of cache misses because it expired */
    protected AtomicLong missCountExpired = new AtomicLong(0);
    /** A count of the number of cache misses because it was cleared from the Soft Reference (ie garbage collection, etc) */
    protected AtomicLong missCountSoftRef = new AtomicLong(0);

    /** A count of the number of cache hits on removes */
    protected AtomicLong removeHitCount = new AtomicLong(0);
    /** A count of the number of cache misses on removes */
    protected AtomicLong removeMissCount = new AtomicLong(0);

    /** The maximum number of elements in the cache.
     * If set to 0, there will be no limit on the number of elements in the cache.
     */
    protected int sizeLimit = 0;
    protected int maxInMemory = 0;

    /** Specifies the amount of time since initial loading before an element will be reported as expired.
     * If set to 0, elements will never expire.
     */
    protected long expireTimeNanos = 0;

    /** Specifies whether or not to use soft references for this cache, defaults to false */
    protected boolean useSoftReference = false;

    /** Specifies whether or not to use file base stored for this cache, defaults to false */
    protected boolean useFileSystemStore = false;
    private String fileStore = "runtime/data/utilcache";

    /** The set of listeners to receive notifications when items are modified (either deliberately or because they were expired). */
    protected Set<CacheListener<K, V>> listeners = new CopyOnWriteArraySet<CacheListener<K, V>>();

    protected transient HTree<Object, V> fileTable = null;
    protected ConcurrentMap<Object, CacheLine<V>> memoryTable = null;

    protected JdbmRecordManager jdbmMgr;

    // weak ref on this
    private static final ConcurrentMap<String, JdbmRecordManager> fileManagers = new ConcurrentHashMap<String, JdbmRecordManager>();

    /** Constructor which specifies the cacheName as well as the sizeLimit, expireTime and useSoftReference.
     * The passed sizeLimit, expireTime and useSoftReference will be overridden by values from cache.properties if found.
     * @param sizeLimit The sizeLimit member is set to this value
     * @param expireTime The expireTime member is set to this value
     * @param cacheName The name of the cache.
     * @param useSoftReference Specifies whether or not to use soft references for this cache.
     */
    private UtilCache(String cacheName, int sizeLimit, int maxInMemory, long expireTimeMillis, boolean useSoftReference, boolean useFileSystemStore, String propName, String... propNames) {
        this.name = cacheName;
        this.sizeLimit = sizeLimit;
        this.maxInMemory = maxInMemory;
        this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(expireTimeMillis, TimeUnit.MILLISECONDS);
        this.useSoftReference = useSoftReference;
        this.useFileSystemStore = useFileSystemStore;
        setPropertiesParams(propName);
        setPropertiesParams(propNames);
        int maxMemSize = this.maxInMemory;
        if (maxMemSize == 0) maxMemSize = sizeLimit;
        if (maxMemSize == 0) {
            memoryTable = new ConcurrentHashMap<Object, CacheLine<V>>();
        } else {
            memoryTable = new Builder<Object, CacheLine<V>>()
            .maximumWeightedCapacity(maxMemSize)
            .listener(this)
            .build();
        }
        if (this.useFileSystemStore) {
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
    }

    private static String getNextDefaultIndex(String cacheName) {
        AtomicInteger curInd = defaultIndices.get(cacheName);
        if (curInd == null) {
            defaultIndices.putIfAbsent(cacheName, new AtomicInteger(0));
            curInd = defaultIndices.get(cacheName);
        }
        int i = curInd.getAndIncrement();
        return i == 0 ? "" : Integer.toString(i);
    }

    public static String getPropertyParam(ResourceBundle res, String[] propNames, String parameter) {
        try {
            for (String propName: propNames) {
            if(res.containsKey(propName+ '.' + parameter)) {
                try {
                return res.getString(propName + '.' + parameter);
                } catch (MissingResourceException e) {}
            }
            }
            // don't need this, just return null
            //if (value == null) {
            //    throw new MissingResourceException("Can't find resource for bundle", res.getClass().getName(), Arrays.asList(propNames) + "." + parameter);
            //}
        } catch (Exception e) {
            Debug.logWarning(e, "Error getting " + parameter + " value from cache.properties file for propNames: " + propNames, module);
        }
        return null;
    }

    protected void setPropertiesParams(String cacheName) {
        setPropertiesParams(new String[] {cacheName});
    }

    public void setPropertiesParams(String[] propNames) {
        ResourceBundle res = ResourceBundle.getBundle("cache");

        if (res != null) {
            String value = getPropertyParam(res, propNames, "maxSize");
            if (UtilValidate.isNotEmpty(value)) {
                this.sizeLimit = Integer.parseInt(value);
            }
            value = getPropertyParam(res, propNames, "maxInMemory");
            if (UtilValidate.isNotEmpty(value)) {
                this.maxInMemory = Integer.parseInt(value);
            }
            value = getPropertyParam(res, propNames, "expireTime");
            if (UtilValidate.isNotEmpty(value)) {
                this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(Long.parseLong(value), TimeUnit.MILLISECONDS);
            }
            value = getPropertyParam(res, propNames, "useSoftReference");
            if (value != null) {
                useSoftReference = "true".equals(value);
            }
            value = getPropertyParam(res, propNames, "useFileSystemStore");
            if (value != null) {
                useFileSystemStore = "true".equals(value);
            }
            value = getPropertyParam(res, new String[0], "cache.file.store");
            if (value != null) {
                fileStore = value;
            }
        }
    }

    private Object fromKey(Object key) {
        return key == null ? ObjectType.NULL : key;
    }

    @SuppressWarnings("unchecked")
    private K toKey(Object key) {
        return key == ObjectType.NULL ? null : (K) key;
    }

    private void addAllFileTableKeys(Set<Object> keys) throws IOException {
        FastIterator<Object> iter = fileTable.keys();
        Object key = null;
        while ((key = iter.next()) != null) {
            keys.add(key);
        }
    }

    public Object getCacheLineTable() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        if (fileTable != null) {
            try {
                synchronized (this) {
                    return fileTable.keys().next() == null;
                }
            } catch (IOException e) {
                Debug.logError(e, module);
                return false;
            }
        } else {
            return memoryTable.isEmpty();
        }
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hashtables and LRU linked list
     * @param value The value of the element
     */
    public V put(K key, V value) {
        return putInternal(key, value, expireTimeNanos);
    }

    public V putIfAbsent(K key, V value) {
        return putIfAbsentInternal(key, value, expireTimeNanos);
    }

    public V putIfAbsentAndGet(K key, V value) {
        V cachedValue = putIfAbsent(key, value);
        return (cachedValue != null? cachedValue: value);
    }

    CacheLine<V> createSoftRefCacheLine(final Object key, V value, long loadTimeNanos, long expireTimeNanos) {
        return tryRegister(loadTimeNanos, new SoftRefCacheLine<V>(value, loadTimeNanos, expireTimeNanos) {
            @Override
            CacheLine<V> changeLine(boolean useSoftReference, long expireTimeNanos) {
                if (useSoftReference) {
                    if (differentExpireTime(expireTimeNanos)) {
                        return this;
                    } else {
                        return createSoftRefCacheLine(key, getValue(), loadTimeNanos, expireTimeNanos);
                    }
                } else {
                    return createHardRefCacheLine(key, getValue(), loadTimeNanos, expireTimeNanos);
                }
            }

            @Override
            void remove() {
                removeInternal(key, this);
            }
        });
    }

    CacheLine<V> createHardRefCacheLine(final Object key, V value, long loadTimeNanos, long expireTimeNanos) {
        return tryRegister(loadTimeNanos, new HardRefCacheLine<V>(value, loadTimeNanos, expireTimeNanos) {
            @Override
            CacheLine<V> changeLine(boolean useSoftReference, long expireTimeNanos) {
                if (useSoftReference) {
                    return createSoftRefCacheLine(key, getValue(), loadTimeNanos, expireTimeNanos);
                } else {
                    if (differentExpireTime(expireTimeNanos)) {
                        return this;
                    } else {
                        return createHardRefCacheLine(key, getValue(), loadTimeNanos, expireTimeNanos);
                    }
                }
            }

            @Override
            void remove() {
                removeInternal(key, this);
            }
        });
    }

    private CacheLine<V> tryRegister(long loadTimeNanos, CacheLine<V> line) {
        if (loadTimeNanos > 0) {
            ExecutionPool.addPulse(line);
        }
        return line;
    }

    private CacheLine<V> createCacheLine(K key, V value, long expireTimeNanos) {
        long loadTimeNanos = expireTimeNanos > 0 ? System.nanoTime() : 0;
        if (useSoftReference) {
            return createSoftRefCacheLine(key, value, loadTimeNanos, expireTimeNanos);
        } else {
            return createHardRefCacheLine(key, value, loadTimeNanos, expireTimeNanos);
        }
    }
    private V cancel(CacheLine<V> line) {
        // FIXME: this is a race condition, the item could expire
        // between the time it is replaced, and it is cancelled
        V oldValue = line.getValue();
        ExecutionPool.removePulse(line);
        line.cancel();
        return oldValue;
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hashtables and LRU linked list
     * @param value The value of the element
     * @param expireTimeMillis how long to keep this key in the cache
     */
    public V put(K key, V value, long expireTimeMillis) {
        return putInternal(key, value, TimeUnit.NANOSECONDS.convert(expireTimeMillis, TimeUnit.MILLISECONDS));
    }

    public V putIfAbsent(K key, V value, long expireTimeMillis) {
        return putIfAbsentInternal(key, value, TimeUnit.NANOSECONDS.convert(expireTimeMillis, TimeUnit.MILLISECONDS));
    }

    V putInternal(K key, V value, long expireTimeNanos) {
        Object nulledKey = fromKey(key);
        CacheLine<V> oldCacheLine = memoryTable.put(nulledKey, createCacheLine(key, value, expireTimeNanos));
        V oldValue = oldCacheLine == null ? null : cancel(oldCacheLine);
        if (fileTable != null) {
            try {
                synchronized (this) {
                    if (oldValue == null) oldValue = fileTable.get(nulledKey);
                    fileTable.put(nulledKey, value);
                    jdbmMgr.commit();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        if (oldValue == null) {
            noteAddition(key, value);
            return null;
        } else {
            noteUpdate(key, value, oldValue);
            return oldValue;
        }
    }

    V putIfAbsentInternal(K key, V value, long expireTimeNanos) {
        Object nulledKey = fromKey(key);
        V oldValue;
        if (fileTable != null) {
            try {
                synchronized (this) {
                    oldValue = fileTable.get(nulledKey);
                    if (oldValue == null) {
                        memoryTable.put(nulledKey, createCacheLine(key, value, expireTimeNanos));
                        fileTable.put(nulledKey, value);
                        jdbmMgr.commit();
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
                oldValue = null;
            }
        } else {
            CacheLine<V> newCacheLine = createCacheLine(key, value, expireTimeNanos);
            CacheLine<V> oldCacheLine = memoryTable.putIfAbsent(nulledKey, newCacheLine);
            if (oldCacheLine == null) {
                oldValue = null;
            } else {
                oldValue = oldCacheLine.getValue();
                cancel(newCacheLine);
            }
        }
        if (oldValue == null) {
            noteAddition(key, value);
            return null;
        } else {
            return oldValue;
        }
    }

    /** Gets an element from the cache according to the specified key.
     * @param key The key for the element, used to reference it in the hashtables and LRU linked list
     * @return The value of the element specified by the key
     */
    public V get(Object key) {
        boolean countGet = true;
        Object nulledKey = fromKey(key);
        CacheLine<V> line = memoryTable.get(nulledKey);
        if (line == null) {
            if (fileTable != null) {
                V value;
                try {
                    synchronized (this) {
                        value = fileTable.get(nulledKey);
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                    value = null;
                }
                if (value == null) {
                    missCountNotFound.incrementAndGet();
                    return null;
                } else {
                    hitCount.incrementAndGet();
                }
                memoryTable.put(nulledKey, createCacheLine(UtilGenerics.<K>cast(key), value, expireTimeNanos));
                return value;
            } else {
                missCountNotFound.incrementAndGet();
            }
        } else {
            if (countGet) hitCount.incrementAndGet();
        }
        return line != null ? line.getValue() : null;
    }

    public Collection<V> values() {
        if (fileTable != null) {
            List<V> values = FastList.newInstance();
            try {
                synchronized (this) {
                    FastIterator<V> iter = fileTable.values();
                    V value = iter.next();
                    while (value != null) {
                        values.add(value);
                        value = iter.next();
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
            return values;
        } else {
            List<V> valuesList = FastList.newInstance();
            for (CacheLine<V> line: memoryTable.values()) {
                valuesList.add(line.getValue());
            }
            return valuesList;
        }
    }

    private long findSizeInBytes(Object o) {
        try {
            if (o == null) {
                if (Debug.infoOn()) Debug.logInfo("Found null object in cache: " + getName(), module);
                return 0;
            }
            if (o instanceof Serializable) {
                return UtilObject.getByteCount(o);
            } else {
                if (Debug.infoOn()) Debug.logInfo("Unable to compute memory size for non serializable object; returning 0 byte size for object of " + o.getClass(), module);
                return 0;
            }
        } catch (NotSerializableException e) {
            // this happens when we try to get the byte count for an object which itself is
            // serializable, but fails to be serialized, such as a map holding unserializable objects
            if (Debug.warningOn()) {
                Debug.logWarning("NotSerializableException while computing memory size; returning 0 byte size for object of " + e.getMessage(), module);
            }
            return 0;
        } catch (Exception e) {
            Debug.logWarning(e, "Unable to compute memory size for object of " + o.getClass(), module);
            return 0;
        }
    }

    public long getSizeInBytes() {
        long totalSize = 0;
        if (fileTable != null) {
            try {
                synchronized (this) {
                    FastIterator<V> iter = fileTable.values();
                    V value = iter.next();
                    while (value != null) {
                        totalSize += findSizeInBytes(value);
                        value = iter.next();
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
                return 0;
            }
        } else {
            for (CacheLine<V> line: memoryTable.values()) {
                totalSize += findSizeInBytes(line.getValue());
            }
        }
        return totalSize;
    }

    /** Removes an element from the cache according to the specified key
     * @param key The key for the element, used to reference it in the hashtables and LRU linked list
     * @return The value of the removed element specified by the key
     */
    public V remove(Object key) {
        return this.removeInternal(key, true);
    }

    /** This is used for internal remove calls because we only want to count external calls */
    @SuppressWarnings("unchecked")
    protected synchronized V removeInternal(Object key, boolean countRemove) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In UtilCache tried to remove with null key, using NullObject" + this.name, module);
        }
        Object nulledKey = fromKey(key);
        CacheLine<V> oldCacheLine;
        V oldValue;
        if (fileTable != null) {
            try {
                synchronized (this) {
                    try {
                        oldValue = fileTable.get(nulledKey);
                    } catch (IOException e) {
                        oldValue = null;
                        throw e;
                    }
                    fileTable.remove(nulledKey);
                    jdbmMgr.commit();
                }
            } catch (IOException e) {
                oldValue = null;
                Debug.logError(e, module);
            }
            oldCacheLine = memoryTable.remove(nulledKey);
        } else {
            oldCacheLine = memoryTable.remove(nulledKey);
            oldValue = oldCacheLine != null ? oldCacheLine.getValue() : null;
        }
        if (oldCacheLine != null) {
            cancel(oldCacheLine);
        }
        if (oldValue != null) {
            noteRemoval((K) key, oldValue);
            if (countRemove) removeHitCount.incrementAndGet();
            return oldValue;
        } else {
            if (countRemove) removeMissCount.incrementAndGet();
            return null;
        }
    }

    protected synchronized void removeInternal(Object key, CacheLine<V> existingCacheLine) {
        Object nulledKey = fromKey(key);
        cancel(existingCacheLine);
        if (!memoryTable.remove(nulledKey, existingCacheLine)) {
            return;
        }
        if (fileTable != null) {
            try {
                synchronized (this) {
                    fileTable.remove(nulledKey);
                    jdbmMgr.commit();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        noteRemoval(UtilGenerics.<K>cast(key), existingCacheLine.getValue());
    }

    /** Removes all elements from this cache */
    public synchronized void erase() {
        if (fileTable != null) {
            // FIXME: erase from memory too
            synchronized (this) {
                Set<Object> keys = new HashSet<Object>();
                try {
                    addAllFileTableKeys(keys);
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
                for (Object key: keys) {
                    try {
                        V value = fileTable.get(key);
                        noteRemoval(toKey(key), value);
                        removeHitCount.incrementAndGet();
                        fileTable.remove(key);
                        jdbmMgr.commit();
                    } catch (IOException e) {
                        Debug.logError(e, module);
                    }
                }
            }
            memoryTable.clear();
        } else {
            Iterator<Map.Entry<Object, CacheLine<V>>> it = memoryTable.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Object, CacheLine<V>> entry = it.next();
                noteRemoval(toKey(entry.getKey()), entry.getValue().getValue());
                removeHitCount.incrementAndGet();
                it.remove();
            }
        }
    }

    public void clear() {
        erase();
        clearCounters();
    }

    /** Removes all elements from this cache */
    public static void clearAllCaches() {
        // We make a copy since clear may take time
        for (UtilCache<?,?> cache : utilCacheTable.values()) {
            cache.clear();
        }
    }

    public static Set<String> getUtilCacheTableKeySet() {
        Set<String> set = new HashSet<String>(utilCacheTable.size());
        set.addAll(utilCacheTable.keySet());
        return set;
    }

    /** Getter for the name of the UtilCache instance.
     * @return The name of the instance
     */
    public String getName() {
        return this.name;
    }

    /** Returns the number of successful hits on the cache
     * @return The number of successful cache hits
     */
    public long getHitCount() {
        return this.hitCount.get();
    }

    /** Returns the number of cache misses from entries that are not found in the cache
     * @return The number of cache misses
     */
    public long getMissCountNotFound() {
        return this.missCountNotFound.get();
    }

    /** Returns the number of cache misses from entries that are expired
     * @return The number of cache misses
     */
    public long getMissCountExpired() {
        return this.missCountExpired.get();
    }

    /** Returns the number of cache misses from entries that are have had the soft reference cleared out (by garbage collector and such)
     * @return The number of cache misses
     */
    public long getMissCountSoftRef() {
        return this.missCountSoftRef.get();
    }

    /** Returns the number of cache misses caused by any reason
     * @return The number of cache misses
     */
    public long getMissCountTotal() {
        return getMissCountSoftRef() + getMissCountNotFound() + getMissCountExpired();
    }

    public long getRemoveHitCount() {
        return this.removeHitCount.get();
    }

    public long getRemoveMissCount() {
        return this.removeMissCount.get();
    }

    /** Clears the hit and miss counters
     */
    public void clearCounters() {
        this.hitCount.set(0);
        this.missCountNotFound.set(0);
        this.missCountExpired.set(0);
        this.missCountSoftRef.set(0);
        this.removeHitCount.set(0);
        this.removeMissCount.set(0);
    }

    public void setMaxInMemory(int newInMemory) {
        this.maxInMemory = newInMemory;
        Map<Object, CacheLine<V>> oldmap = this.memoryTable;

        if (newInMemory > 0) {
            if (this.memoryTable instanceof ConcurrentLinkedHashMap<?, ?>) {
                ((ConcurrentLinkedHashMap<?, ?>) this.memoryTable).setCapacity(newInMemory);
                return;
            } else {
                this.memoryTable =new Builder<Object, CacheLine<V>>()
                    .maximumWeightedCapacity(newInMemory)
                    .build();
            }
        } else {
            this.memoryTable = new ConcurrentHashMap<Object, CacheLine<V>>();
        }

        this.memoryTable.putAll(oldmap);
    }

    public int getMaxInMemory() {
        return maxInMemory;
    }

    public void setSizeLimit(int newSizeLimit) {
        this.sizeLimit = newSizeLimit;
    }

    public int getSizeLimit() {
        return sizeLimit;
    }

    /** Sets the expire time for the cache elements.
     * If 0, elements never expire.
     * @param expireTimeMillis The expire time for the cache elements
     */
    public void setExpireTime(long expireTimeMillis) {
        // if expire time was <= 0 and is now greater, fill expire table now
        if (expireTimeMillis > 0) {
            this.expireTimeNanos = TimeUnit.NANOSECONDS.convert(expireTimeMillis, TimeUnit.MILLISECONDS);
            for (Map.Entry<?, CacheLine<V>> entry: memoryTable.entrySet()) {
                entry.setValue(entry.getValue().changeLine(useSoftReference, expireTimeNanos));
            }
        } else {
            this.expireTimeNanos = 0;
            // if expire time was > 0 and is now <=, do nothing, just leave the load times in place, won't hurt anything...
        }
    }

    /** return the current expire time for the cache elements
     * @return The expire time for the cache elements
     */
    public long getExpireTime() {
        return TimeUnit.MILLISECONDS.convert(expireTimeNanos, TimeUnit.NANOSECONDS);
    }

    /** Set whether or not the cache lines should use a soft reference to the data */
    public void setUseSoftReference(boolean useSoftReference) {
        if (this.useSoftReference != useSoftReference) {
            this.useSoftReference = useSoftReference;
            for (Map.Entry<?, CacheLine<V>> entry: memoryTable.entrySet()) {
                entry.setValue(entry.getValue().changeLine(useSoftReference, expireTimeNanos));
            }
        }
    }

    /** Return whether or not the cache lines should use a soft reference to the data */
    public boolean getUseSoftReference() {
        return this.useSoftReference;
    }

    public boolean getUseFileSystemStore() {
        return this.useFileSystemStore;
    }

    /** Returns the number of elements currently in the cache
     * @return The number of elements currently in the cache
     */
    public int size() {
        if (fileTable != null) {
            int size = 0;
            try {
                synchronized (this) {
                    FastIterator<Object> iter = fileTable.keys();
                    while (iter.next() != null) {
                        size++;
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
            return size;
        } else {
            return memoryTable.size();
        }
    }

    /** Returns a boolean specifying whether or not an element with the specified key is in the cache.
     * @param key The key for the element, used to reference it in the hashtables and LRU linked list
     * @return True is the cache contains an element corresponding to the specified key, otherwise false
     */
    public boolean containsKey(Object key) {
        Object nulledKey = fromKey(key);
        CacheLine<V> line = memoryTable.get(nulledKey);
        if (line == null) {
            if (fileTable != null) {
                try {
                    synchronized (this) {
                        FastIterator<Object> iter = fileTable.keys();
                        Object checkKey = null;
                        while ((checkKey = iter.next()) != null) {
                            if (nulledKey.equals(checkKey)) {
                                return true;
                            }
                        }
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * NOTE: this returns an unmodifiable copy of the keySet, so removing from here won't have an effect,
     * and calling a remove while iterating through the set will not cause a concurrent modification exception.
     * This behavior is necessary for now for the persisted cache feature.
     */
    public Set<? extends K> getCacheLineKeys() {
        // note that this must be a HashSet and not a FastSet in order to have a null value
        Set<Object> keys;

        if (fileTable != null) {
            keys = new HashSet<Object>();
            try {
                synchronized (this) {
                    addAllFileTableKeys(keys);
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
            if (keys.remove(ObjectType.NULL)) {
                keys.add(null);
            }
        } else {
            if (memoryTable.containsKey(ObjectType.NULL)) {
                keys = new HashSet<Object>(memoryTable.keySet());
                keys.remove(ObjectType.NULL);
                keys.add(null);
            } else {
                keys = memoryTable.keySet();
            }
        }
        return Collections.unmodifiableSet(UtilGenerics.<Set<? extends K>>cast(keys));
    }

    public Collection<? extends CacheLine<V>> getCacheLineValues() {
        throw new UnsupportedOperationException();
    }

    private Map<String, Object> createLineInfo(int keyNum, K key, CacheLine<V> line) {
        Map<String, Object> lineInfo = FastMap.newInstance();
        lineInfo.put("elementKey", key);

        if (line.getLoadTimeNanos() > 0) {
            lineInfo.put("expireTimeMillis", TimeUnit.MILLISECONDS.convert(line.getExpireTimeNanos() - System.nanoTime(), TimeUnit.NANOSECONDS));
        }
        lineInfo.put("lineSize", findSizeInBytes(line.getValue()));
        lineInfo.put("keyNum", keyNum);
        return lineInfo;
    }

    private Map<String, Object> createLineInfo(int keyNum, K key, V value) {
        Map<String, Object> lineInfo = FastMap.newInstance();
        lineInfo.put("elementKey", key);
        lineInfo.put("lineSize", findSizeInBytes(value));
        lineInfo.put("keyNum", keyNum);
        return lineInfo;
    }

    public Collection<? extends Map<String, Object>> getLineInfos() {
        List<Map<String, Object>> lineInfos = FastList.newInstance();
        int keyIndex = 0;
        for (K key: getCacheLineKeys()) {
            Object nulledKey = fromKey(key);
            if (fileTable != null) {
                try {
                    synchronized (this) {
                        lineInfos.add(createLineInfo(keyIndex, key, fileTable.get(nulledKey)));
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            } else {
                CacheLine<V> line = memoryTable.get(nulledKey);
                if (line != null) {
                    lineInfos.add(createLineInfo(keyIndex, key, line));
                }
            }
            keyIndex++;
        }
        return lineInfos;
    }

    /** Send a key addition event to all registered listeners */
    protected void noteAddition(K key, V newValue) {
        for (CacheListener<K, V> listener: listeners) {
            listener.noteKeyAddition(this, key, newValue);
        }
    }

    /** Send a key removal event to all registered listeners */
    protected void noteRemoval(K key, V oldValue) {
        for (CacheListener<K, V> listener: listeners) {
            listener.noteKeyRemoval(this, key, oldValue);
        }
    }

    /** Send a key update event to all registered listeners */
    protected void noteUpdate(K key, V newValue, V oldValue) {
        for (CacheListener<K, V> listener: listeners) {
            listener.noteKeyUpdate(this, key, newValue, oldValue);
        }
    }

    /** Adds an event listener for key removals */
    public void addListener(CacheListener<K, V> listener) {
        listeners.add(listener);
    }

    /** Removes an event listener for key removals */
    public void removeListener(CacheListener<K, V> listener) {
        listeners.remove(listener);
    }

    /** Checks for a non-expired key in a specific cache */
    public static boolean validKey(String cacheName, Object key) {
        UtilCache<?, ?> cache = findCache(cacheName);
        if (cache != null) {
            if (cache.containsKey(key))
                return true;
        }
        return false;
    }

    public static void clearCachesThatStartWith(String startsWith) {
        for (Map.Entry<String, UtilCache<?, ?>> entry: utilCacheTable.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith(startsWith)) {
                UtilCache<?, ?> cache = entry.getValue();
                cache.clear();
            }
        }
    }

    public static void clearCache(String cacheName) {
        UtilCache<?, ?> cache = findCache(cacheName);
        if (cache == null) return;
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public static <K, V> UtilCache<K, V> getOrCreateUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime, boolean useSoftReference, boolean useFileSystemStore, String... names) {
        UtilCache<K, V> existingCache = (UtilCache<K, V>) utilCacheTable.get(name);
        if (existingCache != null) return existingCache;
        String cacheName = name + getNextDefaultIndex(name);
        UtilCache<K, V> newCache = new UtilCache<K, V>(cacheName, sizeLimit, maxInMemory, expireTime, useSoftReference, useFileSystemStore, name, names);
        utilCacheTable.putIfAbsent(name, newCache);
        return (UtilCache<K, V>) utilCacheTable.get(name);
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime, boolean useSoftReference, boolean useFileSystemStore, String... names) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, sizeLimit, maxInMemory, expireTime, useSoftReference, useFileSystemStore, name, names));
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime, boolean useSoftReference, boolean useFileSystemStore) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, sizeLimit, maxInMemory, expireTime, useSoftReference, useFileSystemStore, name));
    }

    public static <K,V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, long expireTime, boolean useSoftReference) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, sizeLimit, sizeLimit, expireTime, useSoftReference, false, name));
    }

    public static <K,V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, long expireTime) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, sizeLimit, sizeLimit, expireTime, false, false, name));
    }

    public static <K,V> UtilCache<K, V> createUtilCache(int sizeLimit, long expireTime) {
        String cacheName = "specified" + getNextDefaultIndex("specified");
        return storeCache(new UtilCache<K, V>(cacheName, sizeLimit, sizeLimit, expireTime, false, false, "specified"));
    }

    public static <K,V> UtilCache<K, V> createUtilCache(String name, boolean useSoftReference) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, 0, 0, 0, useSoftReference, false, "default", name));
    }

    public static <K,V> UtilCache<K, V> createUtilCache(String name) {
        String cacheName = name + getNextDefaultIndex(name);
        return storeCache(new UtilCache<K, V>(cacheName, 0, 0, 0, false, false, "default", name));
    }

    public static <K,V> UtilCache<K, V> createUtilCache() {
        String cacheName = "default" + getNextDefaultIndex("default");
        return storeCache(new UtilCache<K, V>(cacheName, 0, 0, 0, false, false, "default"));
    }

    private static <K, V> UtilCache<K, V> storeCache(UtilCache<K, V> cache) {
        utilCacheTable.put(cache.getName(), cache);
        return cache;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> UtilCache<K, V> findCache(String cacheName) {
        return (UtilCache<K, V>) UtilCache.utilCacheTable.get(cacheName);
    }

    @Override
    public void onEviction(Object key, CacheLine<V> value) {
        ExecutionPool.removePulse(value);
    }
}
