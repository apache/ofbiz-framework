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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

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
public class UtilCache<K, V> implements Serializable {

    public static final String module = UtilCache.class.getName();

    /** A static Map to keep track of all of the UtilCache instances. */
    private static final ConcurrentHashMap<String, UtilCache<?, ?>> utilCacheTable = new ConcurrentHashMap<String, UtilCache<?, ?>>();

    /** An index number appended to utilCacheTable names when there are conflicts. */
    private final static ConcurrentHashMap<String, AtomicInteger> defaultIndices = new ConcurrentHashMap<String, AtomicInteger>();

    /** The name of the UtilCache instance, is also the key for the instance in utilCacheTable. */
    private final String name;

    /** A hashtable containing a CacheLine object with a value and a loadTime for each element. */
    private final CacheLineTable<K, V> cacheLineTable;

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
    protected long expireTime = 0;

    /** Specifies whether or not to use soft references for this cache, defaults to false */
    protected boolean useSoftReference = false;

    /** Specifies whether or not to use file base stored for this cache, defautls to false */
    protected boolean useFileSystemStore = false;
    private String fileStore = "runtime/data/utilcache";

    /** The set of listeners to receive notifcations when items are modidfied(either delibrately or because they were expired). */
    protected Set<CacheListener<K, V>> listeners = new CopyOnWriteArraySet<CacheListener<K, V>>();

    /** Constructor which specifies the cacheName as well as the sizeLimit, expireTime and useSoftReference.
     * The passed sizeLimit, expireTime and useSoftReference will be overridden by values from cache.properties if found.
     * @param sizeLimit The sizeLimit member is set to this value
     * @param expireTime The expireTime member is set to this value
     * @param cacheName The name of the cache.
     * @param useSoftReference Specifies whether or not to use soft references for this cache.
     */
    private UtilCache(String cacheName, int sizeLimit, int maxInMemory, long expireTime, boolean useSoftReference, boolean useFileSystemStore, String propName, String... propNames) {
        this.name = cacheName;
        this.sizeLimit = sizeLimit;
        this.maxInMemory = maxInMemory;
        this.expireTime = expireTime;
        this.useSoftReference = useSoftReference;
        this.useFileSystemStore = useFileSystemStore;
        setPropertiesParams(propName);
        setPropertiesParams(propNames);
        int maxMemSize = this.maxInMemory;
        if (maxMemSize == 0) maxMemSize = sizeLimit;
        this.cacheLineTable = new CacheLineTable<K, V>(this.fileStore, this.name, this.useFileSystemStore, maxMemSize);
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
        String value = null;
        for (String propName: propNames) {
            try {
                value = res.getString(propName + '.' + parameter);
            } catch (MissingResourceException e) {}
        }
        // don't need this, just return null
        //if (value == null) {
        //    throw new MissingResourceException("Can't find resource for bundle", res.getClass().getName(), Arrays.asList(propNames) + "." + parameter);
        //}
        return value;
    }

    protected void setPropertiesParams(String cacheName) {
        setPropertiesParams(new String[] {cacheName});
    }

    public void setPropertiesParams(String[] propNames) {
        ResourceBundle res = ResourceBundle.getBundle("cache");

        if (res != null) {
            try {
                String value = getPropertyParam(res, propNames, "maxSize");
                if (UtilValidate.isNotEmpty(value)) {
                    this.sizeLimit = Integer.parseInt(value);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting maxSize value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "maxInMemory");
                if (UtilValidate.isNotEmpty(value)) {
                    this.maxInMemory = Integer.parseInt(value);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting maxInMemory value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "expireTime");
                if (UtilValidate.isNotEmpty(value)) {
                    this.expireTime = Long.parseLong(value);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting expireTime value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "useSoftReference");
                if (value != null) {
                    useSoftReference = "true".equals(value);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting useSoftReference value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "useFileSystemStore");
                if (value != null) {
                    useFileSystemStore = "true".equals(value);
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting useFileSystemStore value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = res.getString("cache.file.store");
                if (value != null) {
                    fileStore = value;
                }
            } catch (MissingResourceException e) {
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting cache.file.store value from cache.properties file for propNames: " + propNames, module);
            }
        }
    }

    public CacheLineTable<K, V> getCacheLineTable() {
        return cacheLineTable;
    }

    public boolean isEmpty() {
        return cacheLineTable.isEmpty();
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @param value The value of the element
     */
    public V put(K key, V value) {
        return put(key, value, expireTime);
    }

    private CacheLine<V> createCacheLine(V value, long expireTime) {
        long loadTime = expireTime > 0 ? System.currentTimeMillis() : 0;
        if (useSoftReference) {
            return new SoftRefCacheLine<V>(value, loadTime, expireTime);
        } else {
            return new HardRefCacheLine<V>(value, loadTime, expireTime);
        }
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @param value The value of the element
     * @param expireTime how long to keep this key in the cache
     */
    public V put(K key, V value, long expireTime) {
        CacheLine<V> oldCacheLine = cacheLineTable.put(key, createCacheLine(value, expireTime));

        if (oldCacheLine == null) {
            noteAddition(key, value);
            return null;
        } else {
            noteUpdate(key, value, oldCacheLine.getValue());
            return oldCacheLine.getValue();
        }

    }

    /** Gets an element from the cache according to the specified key.
     * If the requested element hasExpired, it is removed before it is looked up which causes the function to return null.
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return The value of the element specified by the key
     */
    public V get(Object key) {
        CacheLine<V> line = getInternal(key, true);
        if (line == null) {
            return null;
        } else {
            return line.getValue();
        }
    }

    protected CacheLine<V> getInternalNoCheck(Object key) {
        CacheLine<V> line = cacheLineTable.get(key);
        return line;
    }

    protected CacheLine<V> getInternal(Object key, boolean countGet) {
        CacheLine<V> line = getInternalNoCheck(key);
        if (line == null) {
            if (countGet) incrementCounter(missCountNotFound);
        } else if (line.isInvalid()) {
            removeInternal(key, false);
            if (countGet) incrementCounter(missCountSoftRef);
            line = null;
        } else if (line.hasExpired()) {
            // note that print.info in debug.properties cannot be checked through UtilProperties here, it would cause infinite recursion...
            // if (Debug.infoOn()) Debug.logInfo("Element has expired with key " + key, module);
            removeInternal(key, false);
            if (countGet) incrementCounter(missCountExpired);
            line = null;
        } else {
            if (countGet) incrementCounter(hitCount);
        }
        return line;
    }

    public Collection<V> values() {
        if (cacheLineTable.isEmpty()) {
            return Collections.emptyList();
        }

        List<V> valuesList = FastList.newInstance();
        for (K key: cacheLineTable.keySet()) {
            CacheLine<V> line = this.getInternal(key, false);
            if (line == null) {
                continue;
            } else {
                valuesList.add(line.getValue());
            }
        }

        return valuesList;
    }

    public long getSizeInBytes() {
        long totalSize = 0;
        for (CacheLine<V> line: cacheLineTable.values()) {
            totalSize += line.getSizeInBytes();
        }
        return totalSize;
    }

    /** Removes an element from the cache according to the specified key
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return The value of the removed element specified by the key
     */
    public V remove(Object key) {
        return this.removeInternal(key, true);
    }

    /** This is used for internal remove calls because we only want to count external calls */
    @SuppressWarnings("unchecked")
    protected synchronized V removeInternal(Object key, boolean countRemove) {
        CacheLine<V> line = cacheLineTable.remove(key);
        if (line != null) {
            noteRemoval((K) key, line.getValue());
            if (countRemove) incrementCounter(removeHitCount);
            return line.getValue();
        } else {
            if (countRemove) incrementCounter(removeMissCount);
            return null;
        }
    }

    /** Removes all elements from this cache */
    public synchronized void clear() {
        for (K key: cacheLineTable.keySet()) {
            CacheLine<V> line = getInternalNoCheck(key);
            noteRemoval(key, line == null ? null : line.getValue());
        }
        cacheLineTable.clear();
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

    private static final void incrementCounter(AtomicLong stat) {
        long currentValue;
        do {
            currentValue = stat.get();
        } while (!stat.weakCompareAndSet(currentValue, currentValue + 1));
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

    /** Sets the maximum number of elements in the cache.
     * If 0, there is no maximum.
     * @param maxSize The maximum number of elements in the cache
     * @deprecated Use setMaxInMemory
     */
    public void setMaxSize(int maxSize) {
        setMaxInMemory(maxSize);
    }

    /** Returns the current maximum number of elements in the cache
     * @return The maximum number of elements in the cache
     * @deprecated Use getMaxInMemory
     */
    public int getMaxSize() {
        return getMaxInMemory();
    }

    public void setMaxInMemory(int newMaxInMemory) {
        cacheLineTable.setLru(newMaxInMemory);
        this.maxInMemory = newMaxInMemory;
    }

    public int getMaxInMemory() {
        return maxInMemory;
    }

    public void setSizeLimit(int newSizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public int getSizeLimit() {
        return sizeLimit;
    }

    /** Sets the expire time for the cache elements.
     * If 0, elements never expire.
     * @param expireTime The expire time for the cache elements
     */
    public void setExpireTime(long expireTime) {
        // if expire time was <= 0 and is now greater, fill expire table now
        if (this.expireTime <= 0 && expireTime > 0) {
            for (K key: getCacheLineKeys()) {
                CacheLine<V> line = getInternalNoCheck(key);
                cacheLineTable.put(key, line.changeLine(useSoftReference, expireTime));
            }
        } else if (this.expireTime <= 0 && expireTime > 0) {
            // if expire time was > 0 and is now <=, do nothing, just leave the load times in place, won't hurt anything...
        }

        this.expireTime = expireTime;
    }

    /** return the current expire time for the cache elements
     * @return The expire time for the cache elements
     */
    public long getExpireTime() {
        return expireTime;
    }

    /** Set whether or not the cache lines should use a soft reference to the data */
    public void setUseSoftReference(boolean useSoftReference) {
        if (this.useSoftReference != useSoftReference) {
            this.useSoftReference = useSoftReference;
            for (K key: cacheLineTable.keySet()) {
                CacheLine<V> line = cacheLineTable.get(key);
                cacheLineTable.put(key, line.changeLine(useSoftReference, line.expireTime));
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
        return cacheLineTable.size();
    }

    /** Returns a boolean specifying whether or not an element with the specified key is in the cache.
     * If the requested element hasExpired, it is removed before it is looked up which causes the function to return false.
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return True is the cache contains an element corresponding to the specified key, otherwise false
     */
    public boolean containsKey(Object key) {
        CacheLine<V> line = getInternal(key, false);
        if (line != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * NOTE: this returns an unmodifiable copy of the keySet, so removing from here won't have an effect,
     * and calling a remove while iterating through the set will not cause a concurrent modification exception.
     * This behavior is necessary for now for the persisted cache feature.
     */
    public Set<? extends K> getCacheLineKeys() {
        return cacheLineTable.keySet();
    }

    public Collection<? extends CacheLine<V>> getCacheLineValues() {
        return cacheLineTable.values();
    }

    /** Returns a boolean specifying whether or not the element corresponding to the key has expired.
     * Only returns true if element is in cache and has expired. Error conditions return false, if no expireTable entry, returns true.
     * Always returns false if expireTime <= 0.
     * Also, if SoftReference in the CacheLine object has been cleared by the gc return true.
     *
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return True is the element corresponding to the specified key has expired, otherwise false
     */
    public boolean hasExpired(Object key) {
        CacheLine<V> line = getInternalNoCheck(key);
        if (line == null) return false;
        return line.hasExpired();
    }

    /** Clears all expired cache entries; also clear any cache entries where the SoftReference in the CacheLine object has been cleared by the gc */
    public void clearExpired() {
        for (K key: cacheLineTable.keySet()) {
            if (hasExpired(key)) {
                removeInternal(key, false);
            }
        }
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

    /** Clears all expired cache entries from all caches */
    public static void clearExpiredFromAllCaches() {
        // We make a copy since clear may take time
        for (UtilCache<?,?> utilCache : utilCacheTable.values()) {
            utilCache.clearExpired();
        }
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
}
