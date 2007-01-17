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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.WeakHashMap;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
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
public class UtilCache implements Serializable {

    public static final String module = UtilCache.class.getName();
    
    /** A static Map to keep track of all of the UtilCache instances. */
    public static Map utilCacheTable = new WeakHashMap();

    /** An index number appended to utilCacheTable names when there are conflicts. */
    protected static Map defaultIndices = FastMap.newInstance();

    /** The name of the UtilCache instance, is also the key for the instance in utilCacheTable. */
    protected String name = null;

    /** A hashtable containing a CacheLine object with a value and a loadTime for each element. */
    public CacheLineTable cacheLineTable = null;

    /** A count of the number of cache hits */
    protected long hitCount = 0;

    /** A count of the number of cache misses because it is not found in the cache */
    protected long missCountNotFound = 0;
    /** A count of the number of cache misses because it expired */
    protected long missCountExpired = 0;
    /** A count of the number of cache misses because it was cleared from the Soft Reference (ie garbage collection, etc) */
    protected long missCountSoftRef = 0;

    /** A count of the number of cache hits on removes */
    protected long removeHitCount = 0;
    /** A count of the number of cache misses on removes */
    protected long removeMissCount = 0;
    
    /** The maximum number of elements in the cache.
     * If set to 0, there will be no limit on the number of elements in the cache.
     */
    protected int maxSize = 0;
    protected int maxInMemory = 0;

    /** Specifies the amount of time since initial loading before an element will be reported as expired.
     * If set to 0, elements will never expire.
     */
    protected long expireTime = 0;

    /** Specifies whether or not to use soft references for this cache, defaults to false */
    protected boolean useSoftReference = false;

    /** Specifies whether or not to use file base stored for this cache, defautls to false */
    protected boolean useFileSystemStore = false;
    private String fileStore = "framework/data/utilcache";

    /** The set of listeners to receive notifcations when items are modidfied(either delibrately or because they were expired). */
    protected Set listeners = FastSet.newInstance();
    
    /** Constructor which specifies the cacheName as well as the maxSize, expireTime and useSoftReference.
     * The passed maxSize, expireTime and useSoftReference will be overridden by values from cache.properties if found.
     * @param maxSize The maxSize member is set to this value
     * @param expireTime The expireTime member is set to this value
     * @param cacheName The name of the cache.
     * @param useSoftReference Specifies whether or not to use soft references for this cache.
     */
    public UtilCache(String cacheName, int maxSize, int maxInMemory, long expireTime, boolean useSoftReference, boolean useFileSystemStore) {
        this.maxSize = maxSize;
        this.maxInMemory = maxInMemory;
        this.expireTime = expireTime;
        this.useSoftReference = useSoftReference;
        this.useFileSystemStore = useFileSystemStore;
        name = cacheName + this.getNextDefaultIndex(cacheName);

        setPropertiesParams(cacheName);

        utilCacheTable.put(name, this);
    }

    public UtilCache(String cacheName, int maxSize, long expireTime, boolean useSoftReference) {
        this(cacheName, maxSize, maxSize, expireTime, useSoftReference, false);
    }

    /** Constructor which specifies the cacheName as well as the maxSize and expireTime.
     * The passed maxSize and expireTime will be overridden by values from cache.properties if found.
     * @param maxSize The maxSize member is set to this value
     * @param expireTime The expireTime member is set to this value
     * @param cacheName The name of the cache.
     */
    public UtilCache(String cacheName, int maxSize, long expireTime) {
        this(cacheName, maxSize, expireTime, false);
    }

    /** Constructor which specifies the maxSize and expireTime.
     * @param maxSize The maxSize member is set to this value
     * @param expireTime The expireTime member is set to this value
     */
    public UtilCache(int maxSize, long expireTime) {
        this.useSoftReference = false;
        this.maxSize = maxSize;
        this.expireTime = expireTime;
        String name = "specified" + this.getNextDefaultIndex("specified");

        setPropertiesParams(name);

        utilCacheTable.put(name, this);
    }

    /** This constructor takes a name for the cache, puts itself in the utilCacheTable.
     * It also uses the cacheName to lookup the initialization parameters from cache.properties.
     * @param cacheName The name of the cache.
     */
    public UtilCache(String cacheName, boolean useSoftReference) {
        name = cacheName + this.getNextDefaultIndex(cacheName);
        this.useSoftReference = useSoftReference;

        setPropertiesParams("default");
        setPropertiesParams(cacheName);

        utilCacheTable.put(name, this);
    }

    /** This constructor takes a name for the cache, puts itself in the utilCacheTable.
     * It also uses the cacheName to lookup the initialization parameters from cache.properties.
     * @param cacheName The name of the cache.
     */
    public UtilCache(String cacheName) {
        name = cacheName + this.getNextDefaultIndex(cacheName);

        setPropertiesParams("default");
        setPropertiesParams(cacheName);

        utilCacheTable.put(name, this);
    }

    /** Default constructor, all members stay at default values as defined in cache.properties, or the defaults in this file if cache.properties is not found, or there are no 'default' entries in it. */
    public UtilCache() {
        setPropertiesParams("default");

        name = "default" + this.getNextDefaultIndex("default");
        utilCacheTable.put(name, this);
    }

    protected String getNextDefaultIndex(String cacheName) {
        Integer curInd = (Integer) UtilCache.defaultIndices.get(cacheName);

        if (curInd == null) {
            UtilCache.defaultIndices.put(cacheName, new Integer(1));
            return "";
        } else {
            UtilCache.defaultIndices.put(cacheName, new Integer(curInd.intValue() + 1));
            return Integer.toString(curInd.intValue() + 1);
        }
    }

    public static String getPropertyParam(ResourceBundle res, String[] propNames, String parameter) {
        String value = null;
        for (int i = 0; i < propNames.length && value == null; i++ ) {
            try {
                value = res.getString(propNames[i] + '.' + parameter);
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
                    Integer intValue = new Integer(value);
                    if (intValue != null) {
                        this.maxSize = intValue.intValue();
                    }
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting maxSize value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "maxInMemory");
                if (UtilValidate.isNotEmpty(value)) {
                    Integer intValue = new Integer(value);
                    if (intValue != null) {
                        this.maxInMemory = intValue.intValue();
                    }
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting maxInMemory value from cache.properties file for propNames: " + propNames, module);
            }
            try {
                String value = getPropertyParam(res, propNames, "expireTime");
                if (UtilValidate.isNotEmpty(value)) {
                    Long longValue = new Long(value);
                    if (longValue != null) {
                        this.expireTime = longValue.longValue();
                    }
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
            } catch (Exception e) {
                Debug.logWarning(e, "Error getting cache.file.store value from cache.properties file for propNames: " + propNames, module);
            }
        }

        int maxMemSize = this.maxInMemory;
        if (maxMemSize == 0) maxMemSize = maxSize;
        this.cacheLineTable = new CacheLineTable(this.fileStore, this.name, this.useFileSystemStore, maxMemSize);
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @param value The value of the element
     */
    public synchronized Object put(Object key, Object value) {
        return put(key, value, expireTime);
    }

    /** Puts or loads the passed element into the cache
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @param value The value of the element
     * @param expireTime how long to keep this key in the cache
     */
    public synchronized Object put(Object key, Object value, long expireTime) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In UtilCache tried to put with null key, using NullObject for cache " + this.getName(), module);
            key = ObjectType.NULL;
        }
        CacheLine oldCacheLine;
        if (expireTime > 0) {
            oldCacheLine = (CacheLine) cacheLineTable.put(key, new CacheLine(value, useSoftReference, System.currentTimeMillis(), expireTime));
        } else {
            oldCacheLine = (CacheLine) cacheLineTable.put(key, new CacheLine(value, useSoftReference, expireTime));
        }

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
    public Object get(Object key) {
        CacheLine line = getInternal(key, true);
        if (line == null) {
            return null;
        } else {
            return line.getValue();
        }
    }

    protected CacheLine getInternalNoCheck(Object key) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In UtilCache tried to get with null key, using NullObject for cache " + this.getName(), module);
            key = ObjectType.NULL;
        }
        CacheLine line = (CacheLine) cacheLineTable.get(key);
        return line;
    }
    
    protected CacheLine getInternal(Object key, boolean countGet) {
        CacheLine line = getInternalNoCheck(key);
        if (line == null) {
            if (countGet) missCountNotFound++;
        } else if (line.softReferenceCleared()) {
            removeInternal(key, false);
            if (countGet) missCountSoftRef++;
            line = null;
        } else if (this.hasExpired(line)) {
            // note that print.info in debug.properties cannot be checked through UtilProperties here, it would cause infinite recursion...
            // if (Debug.infoOn()) Debug.logInfo("Element has expired with key " + key, module);
            removeInternal(key, false);
            if (countGet) missCountExpired++;
            line = null;
        } else {
            if (countGet) hitCount++;
        }
        return line;
    }

    public List values() {
        if (cacheLineTable == null) {
            return null;
        }
        
        List valuesList = FastList.newInstance();
        Iterator i = cacheLineTable.keySet().iterator();
        while (i.hasNext()) {
            Object key = i.next();
            valuesList.add(this.get(key));
        }

        return valuesList;
    }

    public long getSizeInBytes() {
        long totalSize = 0;
        Iterator i = cacheLineTable.values().iterator();
        while (i.hasNext()) {
            totalSize += ((CacheLine) i.next()).getSizeInBytes();
        }
        return totalSize;
    }

    /** Removes an element from the cache according to the specified key
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return The value of the removed element specified by the key
     */
    public synchronized Object remove(Object key) {
        return this.removeInternal(key, true);
    }
    
    /** This is used for internal remove calls because we only want to count external calls */
    protected synchronized Object removeInternal(Object key, boolean countRemove) {        
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In UtilCache tried to remove with null key, using NullObject for cache " + this.getName(), module);
            key = ObjectType.NULL;
        }
        CacheLine line = (CacheLine) cacheLineTable.remove(key);
        if (line != null) {
            noteRemoval(key, line.getValue());
            if (countRemove) this.removeHitCount++;
            return line.getValue();
        } else {
            if (countRemove) this.removeMissCount++;
            return null;
        }
    }

    /** Removes all elements from this cache */
    public synchronized void clear() {
        Iterator it = cacheLineTable.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            CacheLine line = getInternalNoCheck(key);
            noteRemoval(key, line == null ? null : line.getValue());
        }
        cacheLineTable.clear();
        clearCounters();
    }

    /** Removes all elements from this cache */
    public static void clearAllCaches() {
        Iterator entries = utilCacheTable.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            UtilCache utilCache = (UtilCache) entry.getValue();
            utilCache.clear();
        }
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
        return this.hitCount;
    }

    /** Returns the number of cache misses from entries that are not found in the cache
     * @return The number of cache misses
     */
    public long getMissCountNotFound() {
        return this.missCountNotFound;
    }

    /** Returns the number of cache misses from entries that are expired
     * @return The number of cache misses
     */
    public long getMissCountExpired() {
        return this.missCountExpired;
    }

    /** Returns the number of cache misses from entries that are have had the soft reference cleared out (by garbage collector and such)
     * @return The number of cache misses
     */
    public long getMissCountSoftRef() {
        return this.missCountSoftRef;
    }

    /** Returns the number of cache misses caused by any reason
     * @return The number of cache misses
     */
    public long getMissCountTotal() {
        return this.missCountSoftRef + this.missCountNotFound + this.missCountExpired;
    }
    
    public long getRemoveHitCount() {
        return this.removeHitCount;
    }
    
    public long getRemoveMissCount() {
        return this.removeMissCount;
    }

    /** Clears the hit and miss counters
     */
    public void clearCounters() {
        this.hitCount = 0;
        this.missCountNotFound = 0;
        this.missCountExpired = 0;
        this.missCountSoftRef = 0;
        this.removeHitCount = 0;
        this.removeMissCount = 0;
    }

    /** Sets the maximum number of elements in the cache.
     * If 0, there is no maximum.
     * @param maxSize The maximum number of elements in the cache
     */
    public void setMaxSize(int maxSize) {
        cacheLineTable.setLru(maxSize);
        this.maxSize = maxSize;
    }

    /** Returns the current maximum number of elements in the cache
     * @return The maximum number of elements in the cache
     */
    public long getMaxSize() {
        return maxSize;
    }

    /** Sets the expire time for the cache elements.
     * If 0, elements never expire.
     * @param expireTime The expire time for the cache elements
     */
    public void setExpireTime(long expireTime) {
        // if expire time was <= 0 and is now greater, fill expire table now
        if (this.expireTime <= 0 && expireTime > 0) {
            long currentTime = System.currentTimeMillis();
            Iterator values = cacheLineTable.values().iterator();
            while (values.hasNext()) {
                CacheLine line = (CacheLine) values.next();
                line.loadTime = currentTime;
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
            Iterator values = cacheLineTable.values().iterator();
            while (values.hasNext()) {
                CacheLine line = (CacheLine) values.next();
                line.setUseSoftReference(useSoftReference);
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
    public long size() {
        return cacheLineTable.size();
    }

    /** Returns a boolean specifying whether or not an element with the specified key is in the cache.
     * If the requested element hasExpired, it is removed before it is looked up which causes the function to return false.
     * @param key The key for the element, used to reference it in the hastables and LRU linked list
     * @return True is the cache contains an element corresponding to the specified key, otherwise false
     */
    public boolean containsKey(Object key) {
        CacheLine line = getInternal(key, false);
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
    public Set getCacheLineKeys() {
        return cacheLineTable.keySet();
    }

    public Collection getCacheLineValues() {
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
        CacheLine line = getInternalNoCheck(key);
        return hasExpired(line);
    }

    protected boolean hasExpired(CacheLine line) {
        if (line == null) return false;

        // check this BEFORE checking to see if expireTime <= 0, ie if time expiration is enabled
        // check to see if we are using softReference first, slight performance increase
        if (line.softReferenceCleared()) return true;
        
        // check if expireTime <= 0, ie if time expiration is not enabled
        if (line.expireTime <= 0) return false;

        // check if the time was saved for this; if the time was not saved, but expire time is > 0, then we don't know when it was saved so expire it to be safe
        if (line.loadTime <= 0) return true;
        
        if ((line.loadTime + line.expireTime) < System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    }

    /** Clears all expired cache entries; also clear any cache entries where the SoftReference in the CacheLine object has been cleared by the gc */
    public void clearExpired() {
        Iterator keys = cacheLineTable.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            if (hasExpired(key)) {
                removeInternal(key, false);
            }
        }
    }

    /** Send a key addition event to all registered listeners */
    protected void noteAddition(Object key, Object newValue) {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                CacheListener listener = (CacheListener) it.next();
                listener.noteKeyAddition(this, key, newValue);
            }
        }
    }

    /** Send a key removal event to all registered listeners */
    protected void noteRemoval(Object key, Object oldValue) {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                CacheListener listener = (CacheListener) it.next();
                listener.noteKeyRemoval(this, key, oldValue);
            }
        }
    }

    /** Send a key update event to all registered listeners */
    protected void noteUpdate(Object key, Object newValue, Object oldValue) {
        synchronized (listeners) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                CacheListener listener = (CacheListener) it.next();
                listener.noteKeyUpdate(this, key, newValue, oldValue);
            }
        }
    }

    /** Adds an event listener for key removals */
    public void addListener(CacheListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /** Removes an event listener for key removals */
    public void removeListener(CacheListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /** Clears all expired cache entries from all caches */
    public static void clearExpiredFromAllCaches() {
        Iterator entries = utilCacheTable.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            UtilCache utilCache = (UtilCache) entry.getValue();
            utilCache.clearExpired();
        }
    }
    
    /** Checks for a non-expired key in a specific cache */
    public static boolean validKey(String cacheName, Object key) {
        UtilCache cache = (UtilCache) utilCacheTable.get(cacheName);
        if (cache != null) {
            if (cache.containsKey(key))
                return true;
        }
        return false;
    }
    
    public static void clearCachesThatStartWith(String startsWith) {
        synchronized (utilCacheTable) {
            Iterator it = utilCacheTable.entrySet().iterator();
            while (it.hasNext()) {    
                Map.Entry entry = (Map.Entry) it.next();    
                String name = (String) entry.getKey();    
                if (name.startsWith(startsWith)) {    
                    UtilCache cache = (UtilCache) entry.getValue();    
                    cache.clear();    
                }
            }
        }
    }

    public static void clearCache(String cacheName) {
        synchronized (UtilCache.utilCacheTable) {
            UtilCache cache = (UtilCache) UtilCache.utilCacheTable.get(cacheName);
            if (cache == null) return;
            cache.clear();
        }
    }
}
