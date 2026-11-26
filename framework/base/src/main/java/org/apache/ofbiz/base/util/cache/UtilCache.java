/*
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
package org.apache.ofbiz.base.util.cache;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilObject;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheKey;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.ClearAll;

/**
 * Generalized caching utility, the API every OFBiz cache goes through.
 * <p>The entries live in a {@link CacheStore} created by the active {@link CacheProvider} (Caffeine by default).
 * A cache's settings come from the code that creates it, overridden by the {@code <cache>} declarations of the
 * {@code ofbiz-component.xml} files (see {@link #declare(List)}). Changing a setting rebuilds the store and keeps
 * the entries.</p>
 * <p>A cache is {@link CachePolicy#LOCAL local} unless a {@code <cache>} declaration gives it another policy: with an
 * invalidation bus installed (see {@link CacheInvalidation}) only caches declared {@code policy="invalidate"} publish
 * their removals to the other servers and apply theirs. Declare it for caches holding data read from the database;
 * the entity engine's caches are distributed by its own {@code DistributedCacheClear}, whatever their policy.</p>
 */
public final class UtilCache<K, V> {

    private static final String MODULE = UtilCache.class.getName();

    /** A static Map to keep track of all of the UtilCache instances. */
    private static final ConcurrentHashMap<String, UtilCache<?, ?>> UTIL_CACHE_TABLE = new ConcurrentHashMap<>();

    /** An index number appended to utilCacheTable names when there are conflicts. */
    private static final ConcurrentHashMap<String, AtomicInteger> DEFAULT_INDICES = new ConcurrentHashMap<>();

    /**
     * Serializes the creation in {@link #getOrCreateUtilCache}, so that only one thread builds a missing cache and
     * takes its name index. A plain monitor rather than {@code computeIfAbsent}: building a store can log, and logging
     * can create other caches, which a monitor allows on the same thread but a map's compute does not.
     */
    private static final Object CREATE_LOCK = new Object();

    /** Past this many live tombstones a cache drops them and ignores puts of every key for the tombstone time. */
    private static final int MAX_TOMBSTONES = 10_000;

    /** The most undeclared cache names listed in the grouped warning. */
    private static final int MAX_LISTED_UNDECLARED = 50;

    /** The base names of the caches already warned about as undeclared, so each name warns once per JVM. */
    private static final Set<String> WARNED_UNDECLARED = ConcurrentHashMap.newKeySet();

    /** The {@code <cache>} declarations of all components, in load order. */
    private static volatile List<CacheDeclaration> declarations = List.of();

    private final String name;
    private final CacheSettings codeSettings;
    /** Whether removals are published to other servers; false for caches whose owner publishes them (entity caches). */
    private final boolean publishRemovals;
    /** Moves on with every invalidation received for this cache, so an in-flight load can tell it may be stale. */
    private final AtomicLong remoteInvalidationVersion = new AtomicLong();
    /** Keys invalidated by another server, with the {@link System#nanoTime()} until which puts of them are ignored. */
    private final ConcurrentHashMap<Object, Long> tombstones = new ConcurrentHashMap<>();
    /**
     * The {@link System#nanoTime()} until which puts of every key are ignored, after a clear received from another
     * server or a tombstone overflow; {@code null} when there has been none.
     */
    private volatile Long clearedUntil;
    /** The {@link System#nanoTime()} of the last pruning of expired tombstones. */
    private volatile long lastTombstonePrune = System.nanoTime();
    private final AtomicLong removeHitCount = new AtomicLong();
    private final AtomicLong removeMissCount = new AtomicLong();
    private final Set<CacheListener<K, V>> listeners = new CopyOnWriteArraySet<>();
    /**
     * Writers to the store (put, putIfAbsent, get with a loader, remove, clear) hold the read lock; reconfigure holds
     * the write lock so no write lands in the old store after its entries were copied. Plain reads take no lock.
     */
    private final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    /** Holds keys as given ({@code null} as {@link ObjectType#NULL}) and values with {@code null} as {@link NullValue}. */
    private volatile CacheStore<Object, Object> store;
    private volatile Stats stats = Stats.EMPTY;

    /** Marks a cached {@code null} value: callers cache {@code null} to remember that a lookup found nothing. */
    private enum NullValue { INSTANCE }

    /**
     * The statistics carried over from replaced stores and the baseline of the current store's statistics at the
     * last clear, kept in one object so a reader never sees one updated without the other.
     */
    private record Stats(CacheStoreStats carried, CacheStoreStats baseline) {
        private static final Stats EMPTY = new Stats(CacheStoreStats.ZERO, CacheStoreStats.ZERO);
    }

    private UtilCache(String name, CacheSettings codeSettings, boolean publishRemovals) {
        this.name = name;
        this.codeSettings = codeSettings;
        this.publishRemovals = publishRemovals;
    }

    private static <K, V> UtilCache<K, V> newCache(String name, CacheSettings codeSettings, boolean publishRemovals) {
        UtilCache<K, V> cache = new UtilCache<>(name, codeSettings, publishRemovals);
        cache.store = cache.newStore(CacheDeclarations.resolve(declarations, codeSettings));
        return cache;
    }

    private static CacheSettings codeSettings(String name, int sizeLimit, int maxInMemory, long expireTime,
                                              boolean useSoftReference) {
        int maxSize = maxInMemory > 0 ? maxInMemory : sizeLimit;
        // local unless a <cache> declaration says otherwise: only caches holding database data need invalidation
        return new CacheSettings(name, Math.max(0, maxSize), Math.max(0, expireTime), useSoftReference,
                CachePolicy.LOCAL);
    }

    private CacheStore<Object, Object> newStore(CacheSettings settings) {
        return CacheProviders.getActiveProvider().create(settings, this::onExpired);
    }

    private void onExpired(Object key, Object value) {
        noteRemoval(toKey(key), toValue(value));
    }

    /**
     * Rebuilds the store with the settings {@code change} derives from the current ones, keeping the entries. The
     * change is applied under the write lock, so concurrent setting changes cannot overwrite each other.
     */
    private void reconfigure(UnaryOperator<CacheSettings> change) {
        storeLock.writeLock().lock();
        try {
            CacheStore<Object, Object> oldStore = store;
            CacheSettings oldSettings = oldStore.settings();
            CacheSettings settings = change.apply(oldSettings);
            if (oldSettings.equals(settings)) {
                return;
            }
            // as before, a new expire time applies to the entries that have none; other changes leave it alone
            long noExpiry = settings.expireTimeMillis() != oldSettings.expireTimeMillis() ? settings.expireTimeMillis() : 0;
            CacheStore<Object, Object> newStore = newStore(settings);
            Set<Object> oldKeys = oldStore.keys();
            for (Object key : oldKeys) {
                Object value = oldStore.peek(key);
                if (value != null) {
                    long remaining = oldStore.expiresInMillis(key);
                    newStore.put(key, value, remaining >= 0 ? Math.max(1, remaining) : noExpiry);
                }
            }
            Stats current = stats;
            stats = new Stats(current.carried().plus(oldStore.stats()).minus(current.baseline()), CacheStoreStats.ZERO);
            store = newStore;
            // explicit removals do not reach the expiry listener, so the old store's pending timers cannot report the
            // copied entries a second time, and the values are not held twice; an entry the old store expired (and
            // reported) meanwhile is dropped from the new store too, so it is reported once
            for (Object key : oldKeys) {
                if (oldStore.remove(key) == null) {
                    newStore.remove(key);
                }
            }
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    /**
     * Replaces the {@code <cache>} declarations and applies them to every existing cache.
     * Called by the component container once all components are loaded.
     * @param newDeclarations the declarations of all components, in load order
     */
    public static void declare(List<CacheDeclaration> newDeclarations) {
        declarations = List.copyOf(newDeclarations);
        for (UtilCache<?, ?> cache : UTIL_CACHE_TABLE.values()) {
            cache.applyDeclarations();
        }
    }

    private void applyDeclarations() {
        reconfigure(current -> CacheDeclarations.resolve(declarations, codeSettings));
    }

    private static String getNextDefaultIndex(String cacheName) {
        int i = DEFAULT_INDICES.computeIfAbsent(cacheName, k -> new AtomicInteger(0)).getAndIncrement();
        return i == 0 ? "" : Integer.toString(i);
    }

    private static Object fromKey(Object key) {
        return key == null ? ObjectType.NULL : key;
    }

    private K toKey(Object key) {
        return key == ObjectType.NULL ? null : UtilGenerics.<K>cast(key);
    }

    private static Object fromValue(Object value) {
        return value == null ? NullValue.INSTANCE : value;
    }

    private V toValue(Object value) {
        return value == NullValue.INSTANCE ? null : UtilGenerics.<V>cast(value);
    }

    /**
     * Gets an element from the cache.
     * @param key the key of the element
     * @return the value, or {@code null} when the key is not in the cache or is cached with a {@code null} value
     *         (use {@link #containsKey(Object)} to tell them apart)
     */
    public V get(Object key) {
        return toValue(store.get(fromKey(key)));
    }

    /**
     * Gets an element from the cache, loading it on a miss. A {@code null} result of the loader is returned and not
     * cached. The loader runs outside any lock and may use this cache; two threads missing the same key at the same
     * time may both run it. The loaded value is cached only if no invalidation for this cache arrived from another
     * server while it loaded, so a value read before such a change is not kept.
     * @param key the key of the element
     * @param loader computes the value on a miss
     * @return the cached or loaded value
     */
    public V get(K key, Function<? super K, ? extends V> loader) {
        Object storeKey = fromKey(key);
        Object cached = store.get(storeKey);
        if (cached != null) {
            return toValue(cached);
        }
        long versionBeforeLoad = remoteInvalidationVersion.get();
        V value = loader.apply(key);
        if (value == null || remoteInvalidationVersion.get() != versionBeforeLoad) {
            return value;
        }
        Object existing;
        storeLock.readLock().lock();
        try {
            if (isTombstoned(storeKey)) {
                return value;
            }
            CacheStore<Object, Object> current = store;
            existing = current.putIfAbsent(storeKey, value, current.settings().expireTimeMillis());
        } finally {
            storeLock.readLock().unlock();
        }
        if (existing != null) {
            return toValue(existing);
        }
        if (remoteInvalidationVersion.get() != versionBeforeLoad) {
            // an invalidation landed between the check above and the put: do not keep what may be stale
            storeLock.readLock().lock();
            try {
                store.remove(storeKey);
            } finally {
                storeLock.readLock().unlock();
            }
            return value;
        }
        noteAddition(key, value);
        return value;
    }

    /**
     * Puts an element in the cache with the cache's expire time. A {@code null} value is cached too, so that
     * {@link #containsKey(Object)} remembers a lookup that found nothing.
     * <p>With an invalidation bus installed, a put is silently dropped for the bus's tombstone time after another server
     * invalidated that key or cleared the whole cache, so a value computed from data read before that change is not
     * kept; the current value, if any, is returned.</p>
     */
    public V put(K key, V value) {
        return putInternal(key, value, null);
    }

    /**
     * Puts an element in the cache. A {@code null} value is cached too. Dropped like {@link #put(Object, Object)} for the
     * tombstone time after another server invalidated the key or cleared the cache.
     * @param expireTimeMillis how long to keep this element, 0 or less for no expiry
     * @return the previous value, or {@code null}
     */
    public V put(K key, V value, long expireTimeMillis) {
        return putInternal(key, value, expireTimeMillis);
    }

    /** Puts an element; a {@code null} expire time means the cache's expire time, read under the lock. */
    private V putInternal(K key, V value, Long expireTimeMillis) {
        Object storeKey = fromKey(key);
        // read before the tombstone check, so an invalidation landing between the check and the write is seen after it
        long versionBeforePut = remoteInvalidationVersion.get();
        Object oldValue;
        storeLock.readLock().lock();
        try {
            if (isTombstoned(storeKey)) {
                return toValue(store.peek(storeKey));
            }
            CacheStore<Object, Object> current = store;
            oldValue = current.put(storeKey, fromValue(value),
                    expireTimeMillis != null ? expireTimeMillis : current.settings().expireTimeMillis());
            if (invalidatedSince(versionBeforePut, storeKey)) {
                // an invalidation of this key, or a clear, landed between the check and the write: drop what was written
                current.remove(storeKey);
                return toValue(oldValue);
            }
        } finally {
            storeLock.readLock().unlock();
        }
        if (oldValue == null) {
            noteAddition(key, value);
        } else {
            noteUpdate(key, value, toValue(oldValue));
        }
        return toValue(oldValue);
    }

    /**
     * Puts an element in the cache unless the key already has one; returns the existing value or {@code null}. Dropped
     * like {@link #put(Object, Object)} for the tombstone time after another server invalidated the key or cleared the
     * cache.
     */
    public V putIfAbsent(K key, V value) {
        return putIfAbsentInternal(key, value, null);
    }

    /** Puts an element in the cache unless the key already has one; returns the existing value or {@code null}. */
    public V putIfAbsent(K key, V value, long expireTimeMillis) {
        return putIfAbsentInternal(key, value, expireTimeMillis);
    }

    /** Puts an element if absent; a {@code null} expire time means the cache's expire time, read under the lock. */
    private V putIfAbsentInternal(K key, V value, Long expireTimeMillis) {
        Object storeKey = fromKey(key);
        long versionBeforePut = remoteInvalidationVersion.get();
        Object existing;
        storeLock.readLock().lock();
        try {
            if (isTombstoned(storeKey)) {
                return toValue(store.peek(storeKey));
            }
            CacheStore<Object, Object> current = store;
            existing = current.putIfAbsent(storeKey, fromValue(value),
                    expireTimeMillis != null ? expireTimeMillis : current.settings().expireTimeMillis());
            if (existing == null && invalidatedSince(versionBeforePut, storeKey)) {
                current.remove(storeKey);
                return null;
            }
        } finally {
            storeLock.readLock().unlock();
        }
        if (existing == null) {
            noteAddition(key, value);
        }
        return toValue(existing);
    }

    /** Puts an element in the cache unless the key already has one; returns the value now in the cache. */
    public V putIfAbsentAndGet(K key, V value) {
        V cachedValue = putIfAbsent(key, value);
        return cachedValue != null ? cachedValue : value;
    }

    /**
     * Removes an element from the cache, and tells the other servers when a bus is installed.
     * @return the removed value, or {@code null}
     */
    public V remove(Object key) {
        V oldValue = removeInternal(key, true);
        if (publishes()) {
            CacheInvalidation.publish(List.of(key instanceof String stringKey ? new CacheKey(name, stringKey)
                    : new CacheClear(name)));
        }
        return oldValue;
    }

    private boolean publishes() {
        return CacheInvalidation.isActive() && publishRemovals && hasSharedName()
                && store.settings().policy() != CachePolicy.LOCAL;
    }

    /**
     * Whether this cache's name is the same on every server: the name it was created with, without the index suffix a
     * second cache of the same name gets. The suffix depends on how many caches of that name each server created and in
     * what order, so a cache with one neither publishes nor applies key and cache invalidations.
     */
    private boolean hasSharedName() {
        return name.equals(codeSettings.name());
    }

    private V removeInternal(Object key, boolean countRemove) {
        Object oldValue;
        storeLock.readLock().lock();
        try {
            oldValue = store.remove(fromKey(key));
        } finally {
            storeLock.readLock().unlock();
        }
        if (oldValue != null) {
            noteRemoval(UtilGenerics.<K>cast(key), toValue(oldValue));
            if (countRemove) {
                removeHitCount.incrementAndGet();
            }
        } else if (countRemove) {
            removeMissCount.incrementAndGet();
        }
        return toValue(oldValue);
    }

    /** Removes all elements from this cache and resets its counters, and tells the other servers. */
    public void clear() {
        clearLocal();
        if (publishes()) {
            CacheInvalidation.publish(List.of(new CacheClear(name)));
        }
    }

    private void clearLocal() {
        clearEntries(true);
    }

    /**
     * Removes all elements and notifies the listeners.
     * @param resetStats whether the hit, miss and remove counters are reset too; a clear received from another server
     *        keeps them, as it says nothing about how this server's cache is used
     */
    private void clearEntries(boolean resetStats) {
        Map<Object, Object> removed = new LinkedHashMap<>();
        storeLock.readLock().lock();
        try {
            CacheStore<Object, Object> current = store;
            for (Object key : current.keys()) {
                Object oldValue = current.remove(key);
                if (oldValue != null) {
                    removed.put(key, oldValue);
                }
            }
            if (resetStats) {
                stats = new Stats(CacheStoreStats.ZERO, current.stats());
                removeHitCount.set(0);
                removeMissCount.set(0);
            }
        } finally {
            storeLock.readLock().unlock();
        }
        removed.forEach((key, oldValue) -> noteRemoval(toKey(key), toValue(oldValue)));
    }

    /** Removes all elements from all caches, and tells the other servers to clear theirs. */
    public static void clearAllCaches() {
        for (UtilCache<?, ?> cache : UTIL_CACHE_TABLE.values()) {
            cache.clearLocal();
        }
        CacheInvalidation.publish(List.of(new ClearAll()));
    }

    public static Set<String> getUtilCacheTableKeySet() {
        return new HashSet<>(UTIL_CACHE_TABLE.keySet());
    }

    /** Returns the values in the cache, including cached {@code null} values, without counting hits. */
    public Collection<V> values() {
        CacheStore<Object, Object> current = store;
        List<V> values = new ArrayList<>();
        for (Object key : current.keys()) {
            Object value = current.peek(key);
            if (value != null) {
                values.add(toValue(value));
            }
        }
        return values;
    }

    /** Returns a snapshot of the keys in the cache; it may contain {@code null}. */
    public Set<K> keys() {
        Set<Object> storeKeys = store.keys();
        if (!storeKeys.contains(ObjectType.NULL)) {
            // the store already returns an unmodifiable snapshot, so there is nothing to translate or copy
            return UtilGenerics.cast(storeKeys);
        }
        Set<K> keys = new LinkedHashSet<>(storeKeys.size());
        for (Object storeKey : storeKeys) {
            keys.add(toKey(storeKey));
        }
        return Collections.unmodifiableSet(keys);
    }

    private long findSizeInBytes(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            if (o instanceof Serializable) {
                return UtilObject.getByteCount(o);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Unable to compute memory size for non serializable object; returning 0 byte size for object of "
                        + o.getClass(), MODULE);
            }
            return 0;
        } catch (NotSerializableException e) {
            // a serializable object holding non serializable objects, such as a map
            if (Debug.warningOn()) {
                Debug.logWarning("NotSerializableException while computing memory size; returning 0 byte size for object of "
                        + e.getMessage(), MODULE);
            }
            return 0;
        } catch (Exception e) {
            Debug.logWarning(e, "Unable to compute memory size for object of " + o.getClass(), MODULE);
            return 0;
        }
    }

    /** Returns the serialized size of all values, for the webtools cache pages. */
    public long getSizeInBytes() {
        long totalSize = 0;
        for (V value : values()) {
            totalSize += findSizeInBytes(value);
        }
        return totalSize;
    }

    /**
     * Describes each element for the webtools cache pages: {@code elementKey}, {@code keyNum} (its position in
     * {@link #keys()}), {@code lineSize} and, for an element that expires, {@code expireTimeMillis} left.
     */
    public List<Map<String, Object>> getLineInfos() {
        CacheStore<Object, Object> current = store;
        List<Map<String, Object>> lineInfos = new ArrayList<>();
        int keyNum = 0;
        for (Object storeKey : current.keys()) {
            Object value = current.peek(storeKey);
            if (value != null) {
                Map<String, Object> lineInfo = new HashMap<>();
                lineInfo.put("elementKey", toKey(storeKey));
                long expiresIn = current.expiresInMillis(storeKey);
                if (expiresIn >= 0) {
                    lineInfo.put("expireTimeMillis", expiresIn);
                }
                lineInfo.put("lineSize", findSizeInBytes(toValue(value)));
                lineInfo.put("keyNum", keyNum);
                lineInfos.add(lineInfo);
            }
            keyNum++;
        }
        return lineInfos;
    }

    public String getName() {
        return name;
    }

    private CacheStoreStats stats() {
        Stats current = stats;
        return current.carried().plus(store.stats()).minus(current.baseline());
    }

    /** Returns the number of lookups that found an element. */
    public long getHitCount() {
        return stats().hitCount();
    }

    /** Returns the number of lookups that found no element. */
    public long getMissCountTotal() {
        return stats().missCount();
    }

    public long getRemoveHitCount() {
        return removeHitCount.get();
    }

    public long getRemoveMissCount() {
        return removeMissCount.get();
    }

    /** Returns the maximum number of elements, 0 for no limit. */
    public long getMaxSize() {
        return store.settings().maxSize();
    }

    /** Sets the maximum number of elements, 0 for no limit. */
    public void setMaxSize(long maxSize) {
        reconfigure(current -> current.withMaxSize(Math.max(0, maxSize)));
    }

    /** Returns the expire time of new elements in milliseconds, 0 for no expiry. */
    public long getExpireTime() {
        return store.settings().expireTimeMillis();
    }

    /** Sets the expire time of elements without their own expiry, 0 or less for no expiry. */
    public void setExpireTime(long expireTimeMillis) {
        reconfigure(current -> current.withExpireTimeMillis(Math.max(0, expireTimeMillis)));
    }

    public boolean getUseSoftReference() {
        return store.settings().softValues();
    }

    public void setUseSoftReference(boolean useSoftReference) {
        reconfigure(current -> current.withSoftValues(useSoftReference));
    }

    /**
     * Changes several settings with a single rebuild of the store; a {@code null} argument leaves that setting as it
     * is. Used by the webtools cache edit page.
     * @param maxSize the maximum number of elements, 0 for no limit
     * @param expireTimeMillis the expire time of elements without their own expiry, 0 or less for no expiry
     * @param useSoftReference whether values are held through soft references
     */
    public void setSettings(Long maxSize, Long expireTimeMillis, Boolean useSoftReference) {
        reconfigure(current -> {
            CacheSettings settings = current;
            if (maxSize != null) {
                settings = settings.withMaxSize(Math.max(0, maxSize));
            }
            if (expireTimeMillis != null) {
                settings = settings.withExpireTimeMillis(Math.max(0, expireTimeMillis));
            }
            if (useSoftReference != null) {
                settings = settings.withSoftValues(useSoftReference);
            }
            return settings;
        });
    }

    /**
     * Returns the cache policy: {@link CachePolicy#LOCAL} unless a {@code <cache>} declaration sets another. It has no
     * effect until an invalidation bus exists.
     */
    public CachePolicy getPolicy() {
        return store.settings().policy();
    }

    public int size() {
        return (int) store.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns whether the cache holds an element for the key, including one cached with a {@code null} value,
     * without counting a hit or a miss.
     */
    public boolean containsKey(Object key) {
        return store.peek(fromKey(key)) != null;
    }

    private void noteAddition(K key, V newValue) {
        for (CacheListener<K, V> listener : listeners) {
            listener.noteKeyAddition(this, key, newValue);
        }
    }

    private void noteRemoval(K key, V oldValue) {
        for (CacheListener<K, V> listener : listeners) {
            listener.noteKeyRemoval(this, key, oldValue);
        }
    }

    private void noteUpdate(K key, V newValue, V oldValue) {
        for (CacheListener<K, V> listener : listeners) {
            listener.noteKeyUpdate(this, key, newValue, oldValue);
        }
    }

    /**
     * Adds a listener. It is told about additions, updates, explicit removals and expiries, but not about entries
     * dropped to keep the cache within its maximum size.
     * <p>Notifications of additions, updates and explicit removals run after the operation, outside the cache's
     * lock. Expiry notifications, and those for values of a soft-reference cache cleared by the garbage collector,
     * may run on any thread using the cache (during Caffeine's maintenance, possibly while that thread holds this
     * cache's read or write lock) or on the JDK scheduler thread. Such a listener must return quickly and must not
     * write to or reconfigure this cache. For a value cleared by the garbage collector {@code oldValue} may be
     * {@code null}.</p>
     * @param listener the listener to add
     */
    public void addListener(CacheListener<K, V> listener) {
        listeners.add(listener);
    }

    public void removeListener(CacheListener<K, V> listener) {
        listeners.remove(listener);
    }

    /** Checks for a non-expired key in a specific cache. */
    public static boolean validKey(String cacheName, Object key) {
        UtilCache<?, ?> cache = findCache(cacheName);
        return cache != null && cache.containsKey(key);
    }

    /** Clears every cache whose name starts with {@code startsWith}, and tells the other servers in one batch. */
    public static void clearCachesThatStartWith(String startsWith) {
        List<InvalidationMessage> messages = new ArrayList<>();
        for (Map.Entry<String, UtilCache<?, ?>> entry : UTIL_CACHE_TABLE.entrySet()) {
            if (entry.getKey().startsWith(startsWith)) {
                UtilCache<?, ?> cache = entry.getValue();
                cache.clearLocal();
                if (cache.publishes()) {
                    messages.add(new CacheClear(cache.name));
                }
            }
        }
        CacheInvalidation.publish(messages);
    }

    public static void clearCache(String cacheName) {
        UtilCache<?, ?> cache = findCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Returns the cache registered under {@code name}, creating it if needed.
     * @param sizeLimit the maximum size when {@code maxInMemory} is 0
     * @param maxInMemory the maximum size, 0 to use {@code sizeLimit}
     */
    public static <K, V> UtilCache<K, V> getOrCreateUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime,
                                                              boolean useSoftReference) {
        return getOrCreateUtilCache(name, sizeLimit, maxInMemory, expireTime, useSoftReference, true);
    }

    /**
     * Returns the cache registered under {@code name}, creating it if needed.
     * @param sizeLimit the maximum size when {@code maxInMemory} is 0
     * @param maxInMemory the maximum size, 0 to use {@code sizeLimit}
     * @param publishRemovals whether this cache publishes its removals to other servers; pass {@code false} when the
     *        code owning the cache publishes them itself, as the entity engine does
     */
    public static <K, V> UtilCache<K, V> getOrCreateUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime,
                                                              boolean useSoftReference, boolean publishRemovals) {
        UtilCache<K, V> existingCache = findCache(name);
        if (existingCache != null) {
            return existingCache;
        }
        UtilCache<K, V> newCache;
        synchronized (CREATE_LOCK) {
            existingCache = findCache(name);
            if (existingCache != null) {
                return existingCache;
            }
            newCache = newCache(name + getNextDefaultIndex(name),
                    codeSettings(name, sizeLimit, maxInMemory, expireTime, useSoftReference), publishRemovals);
            UTIL_CACHE_TABLE.put(name, newCache);
        }
        // a declare() running while the cache was created may have missed it; this is a no-op when nothing changed
        newCache.applyDeclarations();
        newCache.warnIfUndeclared();
        return newCache;
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, int maxInMemory, long expireTime,
                                                         boolean useSoftReference) {
        return storeCache(newCache(name + getNextDefaultIndex(name),
                codeSettings(name, sizeLimit, maxInMemory, expireTime, useSoftReference), true));
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, long expireTime, boolean useSoftReference) {
        return createUtilCache(name, sizeLimit, sizeLimit, expireTime, useSoftReference);
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, int sizeLimit, long expireTime) {
        return createUtilCache(name, sizeLimit, sizeLimit, expireTime, false);
    }

    public static <K, V> UtilCache<K, V> createUtilCache(int sizeLimit, long expireTime) {
        return createUtilCache("specified", sizeLimit, sizeLimit, expireTime, false);
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name, boolean useSoftReference) {
        return createUtilCache(name, 0, 0, 0, useSoftReference);
    }

    public static <K, V> UtilCache<K, V> createUtilCache(String name) {
        return createUtilCache(name, 0, 0, 0, false);
    }

    public static <K, V> UtilCache<K, V> createUtilCache() {
        return createUtilCache("default", 0, 0, 0, false);
    }

    private static <K, V> UtilCache<K, V> storeCache(UtilCache<K, V> cache) {
        UTIL_CACHE_TABLE.put(cache.getName(), cache);
        // a declare() running while the cache was created may have missed it; this is a no-op when nothing changed
        cache.applyDeclarations();
        cache.warnIfUndeclared();
        return cache;
    }

    /**
     * With a bus installed, logs once per cache name a warning for this cache if it publishes removals and no
     * {@code <cache>} declaration matches it, since it is then treated as local without anyone having decided so.
     */
    private void warnIfUndeclared() {
        String baseName = codeSettings.name();
        if (!CacheInvalidation.isActive() || !publishRemovals || CacheDeclarations.isDeclared(declarations, baseName)) {
            return;
        }
        if (WARNED_UNDECLARED.add(baseName)) {
            Debug.logWarning("Cache [" + baseName + "] has no <cache> declaration and is treated as local, so it is not "
                    + "cleared on the other servers; declare policy=\"invalidate\" in its component's ofbiz-component.xml "
                    + "if it holds data read from the database, or policy=\"local\" to confirm it is per-server.", MODULE);
        }
    }

    /**
     * Checks every existing cache, when a bus is installed, and logs one grouped warning for the undeclared ones that were
     * not reported before.
     * @return the sorted base names reported, empty when none
     */
    static List<String> warnUndeclaredCaches() {
        if (!CacheInvalidation.isActive()) {
            return List.of();
        }
        SortedSet<String> names = new TreeSet<>();
        for (UtilCache<?, ?> cache : UTIL_CACHE_TABLE.values()) {
            String baseName = cache.codeSettings.name();
            if (cache.publishRemovals && !CacheDeclarations.isDeclared(declarations, baseName)
                    && WARNED_UNDECLARED.add(baseName)) {
                names.add(baseName);
            }
        }
        if (names.isEmpty()) {
            return List.of();
        }
        List<String> sorted = List.copyOf(names);
        int shown = Math.min(sorted.size(), MAX_LISTED_UNDECLARED);
        String list = String.join(", ", sorted.subList(0, shown))
                + (sorted.size() > shown ? " and " + (sorted.size() - shown) + " more" : "");
        Debug.logWarning(sorted.size() + " caches have no <cache> declaration and are treated as local, so they are not "
                + "cleared on the other servers: [" + list + "]; declare policy=\"invalidate\" in the owning component's "
                + "ofbiz-component.xml for any that hold data read from the database, or policy=\"local\" to confirm "
                + "they are per-server.", MODULE);
        return sorted;
    }

    /** Whether a warning for the undeclared cache {@code baseName} has been logged. */
    static boolean wasWarnedUndeclared(String baseName) {
        return WARNED_UNDECLARED.contains(baseName);
    }

    /** Forgets the warned names, for tests. */
    static void resetWarnedUndeclared() {
        WARNED_UNDECLARED.clear();
    }

    public static <K, V> UtilCache<K, V> findCache(String cacheName) {
        return UtilGenerics.cast(UTIL_CACHE_TABLE.get(cacheName));
    }

    /**
     * Applies invalidations received from another server; local caches, which include every cache not declared with
     * another policy, are left alone.
     */
    static void applyRemote(List<InvalidationMessage> messages) {
        for (InvalidationMessage message : messages) {
            switch (message) {
            case CacheKey m -> {
                UtilCache<?, ?> cache = findCache(m.cacheName());
                if (cache != null && cache.hasSharedName() && cache.appliesRemote()) {
                    cache.applyRemoteKey(m.key());
                }
            }
            case CacheClear m -> {
                UtilCache<?, ?> cache = findCache(m.cacheName());
                if (cache != null && cache.hasSharedName() && cache.appliesRemote()) {
                    cache.applyRemoteClear();
                }
            }
            case ClearAll _ -> clearAllNonLocalCaches();
            default -> {
                // entity messages are applied by the entity engine's handler
            }
            }
        }
    }

    /**
     * Clears every cache that is not local, after a received clear-all or a bus gap. The entity caches are cleared by the
     * entity engine's handler.
     */
    static void clearAllNonLocalCaches() {
        for (UtilCache<?, ?> cache : UTIL_CACHE_TABLE.values()) {
            if (cache.appliesRemote()) {
                cache.applyRemoteClear();
            }
        }
    }

    private boolean appliesRemote() {
        return store.settings().policy() != CachePolicy.LOCAL;
    }

    private void applyRemoteKey(String key) {
        remoteInvalidationVersion.incrementAndGet();
        long tombstoneMillis = CacheInvalidation.tombstoneMillis();
        if (tombstoneMillis > 0) {
            long now = System.nanoTime();
            long tombstoneNanos = TimeUnit.MILLISECONDS.toNanos(tombstoneMillis);
            if (now - lastTombstonePrune >= tombstoneNanos / 2) {
                lastTombstonePrune = now;
                tombstones.values().removeIf(until -> now - until >= 0);
            }
            if (tombstones.size() >= MAX_TOMBSTONES) {
                // too many keys to track one by one: treat the whole cache as just cleared instead
                tombstones.clear();
                extendClearedUntil(now + tombstoneNanos);
            } else {
                tombstones.put(key, now + tombstoneNanos);
            }
        }
        removeInternal(key, false);
    }

    private void applyRemoteClear() {
        remoteInvalidationVersion.incrementAndGet();
        long tombstoneMillis = CacheInvalidation.tombstoneMillis();
        if (tombstoneMillis > 0) {
            extendClearedUntil(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(tombstoneMillis));
        }
        clearEntries(false);
    }

    /** Moves the window in which puts of every key are ignored to end no earlier than {@code until}. */
    private void extendClearedUntil(long until) {
        Long current = clearedUntil;
        if (current == null || until - current > 0) {
            clearedUntil = until;
        }
    }

    /**
     * Whether an invalidation received since {@code versionBeforePut} covers this store key, so a value just written
     * for it may be stale. Without tombstones there is no telling which key was invalidated, so any received
     * invalidation counts.
     */
    private boolean invalidatedSince(long versionBeforePut, Object storeKey) {
        if (remoteInvalidationVersion.get() == versionBeforePut) {
            return false;
        }
        return CacheInvalidation.tombstoneMillis() <= 0 || isTombstoned(storeKey);
    }

    /**
     * Whether a put of this store key must be ignored because another server just invalidated it, or just cleared the
     * whole cache.
     */
    private boolean isTombstoned(Object storeKey) {
        Long clearUntil = clearedUntil;
        if (clearUntil != null && System.nanoTime() - clearUntil < 0) {
            return true;
        }
        if (tombstones.isEmpty()) {
            return false;
        }
        Long until = tombstones.get(storeKey);
        if (until == null) {
            return false;
        }
        if (System.nanoTime() - until < 0) {
            return true;
        }
        tombstones.remove(storeKey, until);
        return false;
    }
}
