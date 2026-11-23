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
 */
package org.apache.ofbiz.base.util

import static org.apache.ofbiz.base.util.tool.UtilCacheTestTools.createListener

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

import org.apache.ofbiz.base.util.cache.CacheDeclaration
import org.apache.ofbiz.base.util.cache.CacheInvalidation
import org.apache.ofbiz.base.util.cache.CacheListener
import org.apache.ofbiz.base.util.cache.CachePolicy
import org.apache.ofbiz.base.util.cache.InvalidationMessage
import org.apache.ofbiz.base.util.cache.RecordingInvalidationBus
import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.base.util.tool.UtilCacheTestTools.Listener

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

// codenarc-disable JUnitLostTest,MethodCount
class UtilCacheTest  {

    @BeforeAll
    static void clearCaches() { // codenarc-disable UnusedPrivateMethod
        UtilCache.clearAllCaches()
    }

    static void populateEmptyCache(String name, UtilCache cacheToPopulate, Integer rowsToAdd, Map controlMap,
                                   Listener controlListener = null) {
        assert cacheToPopulate.isEmpty()
        assert controlMap.isEmpty()
        for (int row in 1..rowsToAdd) {
            String key = "${row}KEY_$name"
            String value = "${row}VAL_$name"
            if (controlListener) {
                controlListener.noteKeyAddition(cacheToPopulate, key, value)
            }
            cacheToPopulate.put(key, value)
            controlMap.put(key, value)
        }
        assert cacheToPopulate.size() == rowsToAdd
        assert controlMap.keySet() == cacheToPopulate.keys()
        assert cacheToPopulate.values().containsAll(controlMap.values())
    }

    static void doUtilCacheCreateTest(UtilCache myCache, Long maxSize, Long expireTime, Boolean useSoftReference) {
        if (maxSize != null) {
            assert maxSize.longValue() == myCache.maxSize
        }
        if (expireTime != null) {
            assert expireTime.longValue() == myCache.expireTime
        }
        if (useSoftReference != null) {
            assert useSoftReference == myCache.getUseSoftReference()
        }
        assert myCache.isEmpty()
        assert Collections.emptySet() == myCache.keys()
        assert Collections.emptyList() == myCache.values()
        assert myCache === UtilCache.findCache(myCache.name)
        assert myCache !== UtilCache.createUtilCache()
    }

    /** Polls {@code condition} until it holds or {@code timeoutMillis} pass; returns whether it held. */
    static boolean waitFor(long timeoutMillis = 5000, Closure<Boolean> condition) {
        long deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                return false
            }
            sleep(50)
        }
        return true
    }

    static <K, V> void doSingleKeyInSingleCacheTest(UtilCache myCache, K myKey, V myValue) {
        assert !myCache.isEmpty()
        assert myCache.size() == 1
        assert myCache.containsKey(myKey)
        assert UtilCache.validKey(myCache.getName(), myKey)
        assert myValue == myCache.get(myKey)
    }

    static <K> void doKeyNotInCacheTest(UtilCache myCache, K myKey) {
        assert !myCache.containsKey(myKey)
        assert !UtilCache.validKey(myCache.getName(), myKey)
        assert myCache.get(myKey) == null
        assert myCache.remove(myKey) == null
    }

    static RecordingInvalidationBus installBus(long tombstoneMillis) {
        RecordingInvalidationBus bus = new RecordingInvalidationBus(tombstoneMillis)
        CacheInvalidation.install(bus)
        return bus
    }

    /** Declares the caches {@code policy="invalidate"}; caches are local by default. Reset with {@code declare([])}. */
    static void declareInvalidate(String... names) {
        UtilCache.declare(names.collect { new CacheDeclaration(it, false, null, null, null, CachePolicy.INVALIDATE) })
    }

    // codenarc-disable JUnitTestMethodWithoutAssert
    @Test
    void testCreateUtilCache() {
        String name = 'testCreateUtilCache'
        doUtilCacheCreateTest(UtilCache.createUtilCache(), 0L, 0L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name), 0L, 0L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, false), 0L, 0L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, true), 0L, 0L, Boolean.TRUE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(5, 15000), 5L, 15000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 6, 16000), 6L, 16000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 7, 17000, false), 7L, 17000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 8, 18000, true), 8L, 18000L, Boolean.TRUE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 9, 5, 19000, false), 5L, 19000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 10, 0, 20000, false), 10L, 20000L, Boolean.FALSE)
    }
    // codenarc-enable JUnitTestMethodWithoutAssert

    @Test
    void testCacheGetterOnCreation() {
        String name = 'testCacheGetterOnCreation'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        assert UtilCache.getUtilCacheTableKeySet().contains(name)
        assert myCache == UtilCache.findCache(name)
        assert myCache == UtilCache.getOrCreateUtilCache(name, (int) myCache.maxSize, 0, myCache.expireTime,
                myCache.useSoftReference)
    }

    @Test
    void testCacheCreateEntry() {
        String name = 'testCacheCreateEntry'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String key = "KEY_$name"
        String value = "VAL_$name"

        controlListener.noteKeyAddition(myCache, key, value)
        Object objectInCache = myCache.put(key, value)
        assert objectInCache == null
        doSingleKeyInSingleCacheTest myCache, key, value
        assert myCacheListener == controlListener
    }

    @Test
    void testCacheCreateEntryWithNullKey() {
        String name = 'testCacheCreateEntryWithNullKey'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String value = "VAL_$name"

        controlListener.noteKeyAddition(myCache, null, value)
        Object objectInCache = myCache.put(null, value)
        assert objectInCache == null
        doSingleKeyInSingleCacheTest myCache, null, value
        assert myCacheListener == controlListener
    }

    @Test
    void testCacheUpdateEntry() {
        String name = 'testCacheUpdateEntry'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String key = "KEY_$name"
        String value1 = "VAL1_$name"
        String value2 = "VAL2_$name"

        controlListener.noteKeyAddition(myCache, key, value1)
        Object objectInCache = myCache.put(key, value1)
        assert objectInCache == null
        doSingleKeyInSingleCacheTest myCache, key, value1

        controlListener.noteKeyUpdate(myCache, key, value2, value1)
        objectInCache = myCache.put(key, value2)
        assert objectInCache == value1
        doSingleKeyInSingleCacheTest myCache, key, value2
        assert myCacheListener == controlListener
    }

    @Test
    void testRemoveCacheEntry() {
        String name = 'testRemoveCacheEntry'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String key = "KEY_$name"
        String value = "VAL_$name"

        controlListener.noteKeyAddition(myCache, key, value)
        Object objectInCache = myCache.put(key, value)
        assert objectInCache == null
        doSingleKeyInSingleCacheTest myCache, key, value

        controlListener.noteKeyRemoval(myCache, key, value)
        objectInCache = myCache.remove(key)
        assert objectInCache == value
        doKeyNotInCacheTest myCache, key
        assert myCacheListener == controlListener
    }

    @Test
    void testSetExpireCache() {
        String name = 'testSetExpireCache'
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        Map controlMap = [:]
        myCache.setExpireTime(1000)
        populateEmptyCache(name, myCache, 5, controlMap, controlListener)
        controlMap.forEach { k, v -> controlListener.noteKeyRemoval(myCache, k, v) }
        // reads stop returning an expired element at once, but Caffeine's timer wheel removes it and tells the
        // listeners up to about a second later
        assert waitFor { myCache.keys().isEmpty() && myCacheListener == controlListener }
        assert myCache.values().isEmpty()
    }

    @Test
    void testChangeMemorySize() {
        String name = 'testChangeMemorySize'
        int size = 5
        UtilCache<String, Serializable> myCache = UtilCache.createUtilCache(name, size, size, 0, false)
        Map controlMap = [:]
        populateEmptyCache(name, myCache, 5, controlMap)

        myCache.setMaxSize(2)
        assert myCache.size() == 2
        controlMap.keySet().retainAll(myCache.keys())
        assert controlMap.keySet() == myCache.keys()
        assert myCache.values().containsAll(controlMap.values())

        myCache.setMaxSize(0)
        assert controlMap.keySet() == myCache.keys()
        assert myCache.values().containsAll(controlMap.values())

        myCache.setMaxSize(size)
        assert controlMap.keySet() == myCache.keys()
        assert myCache.values().containsAll(controlMap.values())
    }

    @Test
    void testPutIfAbsent() {
        String name = 'testPutIfAbsent'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 1, 1, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String key = "KEY_$name"
        String value1 = "VAL1_$name"
        String value2 = "VAL2_$name"

        controlListener.noteKeyAddition(myCache, key, value1)
        Object oldObject = myCache.putIfAbsent(key, value1)
        assert oldObject == null
        doSingleKeyInSingleCacheTest(myCache, key, value1)

        oldObject = myCache.putIfAbsent(key, value2)
        assert oldObject == value1
        doSingleKeyInSingleCacheTest(myCache, key, value1)
        assert myCacheListener == controlListener
    }

    @Test
    void testPutIfAbsentAndGet() {
        String name = 'testPutIfAbsentAndGet'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 1, 1, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        String key1 = "KEY1_$name"
        String value1 = "VAL1_$name"

        Object inCache = myCache.get(key1)
        assert inCache == null
        controlListener.noteKeyAddition(myCache, key1, value1)
        inCache = myCache.putIfAbsentAndGet(key1, value1)
        assert inCache == value1
        doSingleKeyInSingleCacheTest myCache, key1, value1

        inCache = myCache.putIfAbsentAndGet(key1, 'newValue')
        assert inCache == value1
        doSingleKeyInSingleCacheTest myCache, key1, value1

        String key2 = "KEY2_$name"
        String value2 = new String('anotherValue') // codenarc-disable UnnecessaryStringInstantiation
        String value2Bis = new String('anotherValue') // codenarc-disable UnnecessaryStringInstantiation
        assert value2 == value2Bis
        assert value2 !== value2Bis

        controlListener.noteKeyAddition(myCache, key2, value2)
        inCache = myCache.putIfAbsentAndGet(key2, value2)
        assert inCache === value2
        inCache = myCache.putIfAbsentAndGet(key2, value2Bis)
        assert inCache !== value2Bis
        assert inCache === value2
        doSingleKeyInSingleCacheTest myCache, key2, value2
        assert controlListener == myCacheListener
    }

    @Test
    void testGetWithLoader() {
        String name = 'testGetWithLoader'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        int calls = 0
        controlListener.noteKeyAddition(myCache, 'key', 'loaded')
        assert myCache.get('key') { calls++; 'loaded' } == 'loaded'
        assert myCache.get('key') { calls++; 'other' } == 'loaded'
        assert myCache.get('none') { null } == null
        assert calls == 1
        assert !myCache.containsKey('none')
        assert myCacheListener == controlListener
    }

    @Test
    void testPutNullValueIsCached() {
        String name = 'testPutNullValueIsCached'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 0, false)
        myCache.put('key', 'value')
        assert myCache.put('key', null) == 'value'
        assert myCache.containsKey('key')
        assert myCache.get('key') == null
        assert myCache.size() == 1
        assert myCache.values() == [null]
        assert myCache.putIfAbsent('key', 'other') == null
        assert myCache.get('key') == null
        assert myCache.remove('key') == null
        assert !myCache.containsKey('key')
    }

    @Test
    void testOtherSettingChangesKeepEntriesWithoutExpiry() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testOtherSettingChanges', 0, 0, 60000, false)
        myCache.put('forever', 'value', 0)
        myCache.setMaxSize(10)
        myCache.setUseSoftReference(true)
        assert myCache.get('forever') == 'value'
        assert !myCache.getLineInfos().find { it.elementKey == 'forever' }.containsKey('expireTimeMillis')
    }

    @Test
    void testNewExpireTimeAppliesToEntriesWithoutExpiry() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testNewExpireTimeApplies', 0, 0, 0, false)
        myCache.put('key', 'value')
        myCache.setExpireTime(60000)
        assert myCache.getLineInfos().find { it.elementKey == 'key' }.expireTimeMillis > 0
    }

    @Test
    void testSetSettingsChangesOnlyGivenSettings() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testSetSettings', 0, 0, 0, false)
        myCache.put('key', 'value')
        myCache.setSettings(5L, null, Boolean.TRUE)
        assert myCache.getMaxSize() == 5L
        assert myCache.getExpireTime() == 0L
        assert myCache.getUseSoftReference()
        assert myCache.get('key') == 'value'
    }

    @Test
    void testGetOrCreateUtilCacheCreatesOneCacheUnderContention() {
        String name = 'testGetOrCreateContention'
        int threads = 16
        CountDownLatch start = new CountDownLatch(1)
        List<UtilCache<String, String>> results = Collections.synchronizedList([])
        List<Thread> workers = (1..threads).collect {
            Thread.start {
                start.await()
                results << UtilCache.getOrCreateUtilCache(name, 0, 0, 0, false)
            }
        }
        start.countDown()
        workers*.join()
        assert results.size() == threads
        assert results.every { it.is(results[0]) }
        // only the creating thread took a name index, so the next cache with this base name gets index 1
        assert UtilCache.createUtilCache(name).getName() == name + '1'
    }

    @Test
    void testRemovePublishesTheKeyWhenABusIsInstalled() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testRemovePublishes', 0, 0, 0, false)
        RecordingInvalidationBus bus = installBus(2000)
        try {
            declareInvalidate('testRemovePublishes')
            myCache.put('a', '1')
            myCache.remove('a')
            myCache.remove(Integer.valueOf(7))
            myCache.clear()
            assert bus.messages() == [new InvalidationMessage.CacheKey(myCache.getName(), 'a'),
                                      new InvalidationMessage.CacheClear(myCache.getName()),
                                      new InvalidationMessage.CacheClear(myCache.getName())]
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testClearCachesThatStartWithPublishesOneBatch() {
        UtilCache<String, String> first = UtilCache.createUtilCache('testPrefixBatch.first', 0, 0, 0, false)
        UtilCache<String, String> second = UtilCache.createUtilCache('testPrefixBatch.second', 0, 0, 0, false)
        UtilCache<String, String> local = UtilCache.createUtilCache('testPrefixBatch.local', 0, 0, 0, false)
        RecordingInvalidationBus bus = installBus(2000)
        try {
            declareInvalidate('testPrefixBatch.first', 'testPrefixBatch.second')
            first.put('a', '1')
            local.put('c', '3')
            UtilCache.clearCachesThatStartWith('testPrefixBatch.')
            assert first.isEmpty() && second.isEmpty() && local.isEmpty()
            assert bus.batches().size() == 1
            assert bus.messages() as Set == [new InvalidationMessage.CacheClear(first.getName()),
                                             new InvalidationMessage.CacheClear(second.getName())] as Set
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testNothingIsPublishedWithoutABus() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testNoBusNoPublish', 0, 0, 0, false)
        try {
            declareInvalidate('testNoBusNoPublish')
            assert !CacheInvalidation.isActive()
            myCache.put('a', '1')
            assert myCache.remove('a') == '1'
            myCache.clear()
            // nothing was kept back to be sent once a bus is installed
            RecordingInvalidationBus bus = installBus(2000)
            try {
                assert bus.messages().isEmpty()
            } finally {
                CacheInvalidation.uninstall()
            }
        } finally {
            UtilCache.declare([])
        }
    }

    @Test
    void testUndeclaredCachesAreLocalAndNeitherPublishNorApply() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testUndeclaredLocal', 0, 0, 0, false)
        assert myCache.policy == CachePolicy.LOCAL
        RecordingInvalidationBus bus = installBus(2000)
        try {
            myCache.put('a', '1')
            myCache.remove('a')
            myCache.clear()
            assert bus.messages().isEmpty()
            myCache.put('b', '2')
            bus.deliverFrom('other-node', new InvalidationMessage.CacheKey(myCache.getName(), 'b'))
            bus.deliverFrom('other-node', new InvalidationMessage.CacheClear(myCache.getName()))
            bus.deliverFrom('other-node', new InvalidationMessage.ClearAll())
            bus.reportGap()
            assert myCache.get('b') == '2'
        } finally {
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testLocalCachesNeitherPublishNorApply() {
        String name = 'testLocalCacheNoPublish'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 0, false)
        RecordingInvalidationBus bus = installBus(2000)
        try {
            UtilCache.declare([new CacheDeclaration(name, false, null, null, null, CachePolicy.LOCAL)])
            myCache.put('a', '1')
            myCache.remove('a')
            assert bus.messages().isEmpty()
            myCache.put('b', '2')
            bus.deliverFrom('other-node', new InvalidationMessage.CacheKey(myCache.getName(), 'b'))
            assert myCache.get('b') == '2'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testCachesCreatedWithoutPublishingDoNotPublish() {
        UtilCache<String, String> myCache = UtilCache.getOrCreateUtilCache('testNoPublishOption', 0, 0, 0, false, false)
        // declared invalidate, so only the publishRemovals argument keeps it from publishing
        declareInvalidate('testNoPublishOption')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            myCache.put('a', '1')
            myCache.remove('a')
            myCache.clear()
            assert bus.messages().isEmpty()
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testReceivedKeyRemovesAndBlocksPutsForTheTombstoneTime() {
        // a long tombstone, so a slow machine cannot see it expire before the put is checked
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testReceivedKey', 0, 0, 0, false)
        UtilCache<String, String> expiring = UtilCache.createUtilCache('testReceivedKeyExpiry', 0, 0, 0, false)
        declareInvalidate('testReceivedKey', 'testReceivedKeyExpiry')
        RecordingInvalidationBus bus = installBus(60000)
        try {
            myCache.put('a', 'old')
            bus.deliverFrom('other-node', new InvalidationMessage.CacheKey(myCache.getName(), 'a'))
            assert !myCache.containsKey('a')
            myCache.put('a', 'stale')
            assert !myCache.containsKey('a')
            myCache.putIfAbsent('a', 'stale')
            assert !myCache.containsKey('a')
            myCache.put('other', 'kept')
            assert myCache.get('other') == 'kept'
        } finally {
            CacheInvalidation.uninstall()
        }
        // a short tombstone on a separate cache for the expiry
        RecordingInvalidationBus shortBus = installBus(200)
        try {
            expiring.put('a', 'old')
            shortBus.deliverFrom('other-node', new InvalidationMessage.CacheKey(expiring.getName(), 'a'))
            assert !expiring.containsKey('a')
            assert waitFor {
                expiring.put('a', 'fresh')
                expiring.get('a') == 'fresh'
            }
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testReceivedClearBlocksPutsForTheTombstoneTime() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testReceivedClearWindow', 0, 0, 0, false)
        declareInvalidate('testReceivedClearWindow')
        RecordingInvalidationBus bus = installBus(60000)
        try {
            myCache.put('a', 'old')
            bus.deliverFrom('other-node', new InvalidationMessage.CacheClear(myCache.getName()))
            assert myCache.isEmpty()
            myCache.put('b', 'stale')
            myCache.putIfAbsent('c', 'stale')
            assert myCache.get('d') { String key -> 'loaded' } == 'loaded'
            assert myCache.isEmpty()
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testTooManyTombstonesBlockPutsOfEveryKey() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testTombstoneOverflow', 0, 0, 0, false)
        declareInvalidate('testTombstoneOverflow')
        RecordingInvalidationBus bus = installBus(60000)
        try {
            myCache.put('unrelated', 'kept')
            InvalidationMessage[] keys = (0..10_000).collect {
                new InvalidationMessage.CacheKey(myCache.getName(), "key$it".toString())
            } as InvalidationMessage[]
            bus.deliverFrom('other-node', keys)
            // the overflow drops the tombstones but treats the whole cache as just cleared, so no stale put gets in
            myCache.put('key10000', 'stale')
            myCache.put('never-invalidated', 'blocked')
            assert !myCache.containsKey('key10000')
            assert !myCache.containsKey('never-invalidated')
            // entries already cached are not removed by the overflow
            assert myCache.get('unrelated') == 'kept'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testReceivedClearKeepsTheStatistics() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testReceivedClearStats', 0, 0, 0, false)
        declareInvalidate('testReceivedClearStats')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            myCache.put('a', '1')
            myCache.put('b', '2')
            myCache.get('a')
            myCache.get('missing')
            myCache.remove('b')
            long hits = myCache.getHitCount()
            long misses = myCache.getMissCountTotal()
            long removeHits = myCache.getRemoveHitCount()
            assert hits > 0 && misses > 0 && removeHits > 0
            bus.deliverFrom('other-node', new InvalidationMessage.CacheClear(myCache.getName()))
            assert myCache.isEmpty()
            assert myCache.getHitCount() == hits
            assert myCache.getMissCountTotal() == misses
            assert myCache.getRemoveHitCount() == removeHits
            bus.reportGap()
            assert myCache.getHitCount() == hits
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testOwnMessagesAreIgnored() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testOwnMessages', 0, 0, 0, false)
        declareInvalidate('testOwnMessages')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            myCache.put('a', '1')
            bus.deliverFrom(CacheInvalidation.getNodeId(), new InvalidationMessage.CacheKey(myCache.getName(), 'a'))
            assert myCache.get('a') == '1'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testReceivedClearAllClearsEveryNonLocalCache() {
        UtilCache<String, String> first = UtilCache.createUtilCache('testClearAllFirst', 0, 0, 0, false)
        UtilCache<String, String> second = UtilCache.createUtilCache('testClearAllSecond', 0, 0, 0, false)
        UtilCache<String, String> local = UtilCache.createUtilCache('testClearAllLocal', 0, 0, 0, false)
        declareInvalidate('testClearAllFirst', 'testClearAllSecond')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            first.put('a', '1')
            second.put('b', '2')
            local.put('c', '3')
            bus.deliverFrom('other-node', new InvalidationMessage.ClearAll())
            assert first.isEmpty()
            assert second.isEmpty()
            assert local.get('c') == '3'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testClearAllCachesPublishesOneClearAll() {
        RecordingInvalidationBus bus = installBus(2000)
        try {
            UtilCache.clearAllCaches()
            assert bus.messages() == [new InvalidationMessage.ClearAll()]
        } finally {
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testGapClearsEveryNonLocalCache() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testGapClears', 0, 0, 0, false)
        declareInvalidate('testGapClears')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            myCache.put('a', '1')
            bus.reportGap()
            assert myCache.isEmpty()
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testCachesWithAnIndexSuffixNeitherPublishNorApply() {
        UtilCache<String, String> first = UtilCache.createUtilCache('testSuffixed', 0, 0, 0, false)
        UtilCache<String, String> second = UtilCache.createUtilCache('testSuffixed', 0, 0, 0, false)
        assert second.getName() != first.getName()
        declareInvalidate('testSuffixed')
        RecordingInvalidationBus bus = installBus(2000)
        try {
            second.put('a', '1')
            second.remove('a')
            second.clear()
            UtilCache.clearCachesThatStartWith(second.getName())
            assert bus.messages().isEmpty()

            second.put('b', '2')
            bus.deliverFrom('other-node', new InvalidationMessage.CacheKey(second.getName(), 'b'),
                    new InvalidationMessage.CacheClear(second.getName()))
            assert second.get('b') == '2'

            first.remove('c')
            assert bus.messages() == [new InvalidationMessage.CacheKey(first.getName(), 'c')]
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testPutRacingAReceivedInvalidationIsDroppedWithoutTombstones() {
        UtilCache<Object, String> myCache = UtilCache.createUtilCache('testPutRaceNoTombstone', 0, 0, 0, false)
        declareInvalidate('testPutRaceNoTombstone')
        // no tombstone time, so only the invalidation version can tell the put raced an invalidation
        RecordingInvalidationBus bus = installBus(0)
        try {
            // the store hashes the key while writing it: deliver the clear then, between the check and the write
            boolean delivered = false
            Object racingKey = new Object() {

                @Override
                int hashCode() {
                    if (!delivered) {
                        delivered = true
                        bus.deliverFrom('other-node', new InvalidationMessage.CacheClear(myCache.getName()))
                    }
                    return 1
                }

                @Override
                boolean equals(Object other) {
                    return this.is(other)
                }

            }
            myCache.put(racingKey, 'stale')
            assert delivered
            assert !myCache.containsKey(racingKey)
            myCache.put('later', 'fresh')
            assert myCache.get('later') == 'fresh'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testLoaderResultIsNotCachedWhenInvalidatedDuringTheLoad() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testLoaderGuard', 0, 0, 0, false)
        declareInvalidate('testLoaderGuard')
        // no tombstone time, so the received clear blocks this load only, not the next one
        RecordingInvalidationBus bus = installBus(0)
        try {
            String value = myCache.get('a') { String key ->
                bus.deliverFrom('other-node', new InvalidationMessage.CacheClear(myCache.getName()))
                'loaded'
            }
            assert value == 'loaded'
            assert !myCache.containsKey('a')
            assert myCache.get('b') { String key -> 'loaded' } == 'loaded'
            assert myCache.get('b') == 'loaded'
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testLoaderMayUseTheSameCache() {
        UtilCache<String, String> myCache = UtilCache.createUtilCache('testLoaderReentry', 0, 0, 0, false)
        myCache.put('base', 'x')
        assert myCache.get('derived') { String key -> myCache.get('base') + 'y' } == 'xy'
        assert myCache.get('derived') == 'xy'
    }

    @Test
    void testDeclareReconfiguresExistingCache() {
        String name = 'testDeclareReconfigures'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 0, false)
        myCache.put('key', 'value')
        try {
            UtilCache.declare([new CacheDeclaration(name, false, 3L, 50000L, null, CachePolicy.INVALIDATE)])
            assert myCache.maxSize == 3
            assert myCache.expireTime == 50000
            assert myCache.policy == CachePolicy.INVALIDATE
            assert myCache.get('key') == 'value'
        } finally {
            UtilCache.declare([])
        }
        assert myCache.maxSize == 0
        assert myCache.expireTime == 0
        assert myCache.policy == CachePolicy.LOCAL
        assert myCache.get('key') == 'value'
    }

    @Test
    void testReconfigureKeepsEntriesAndNotifiesExpiryOnce() {
        String name = 'testReconfigureKeepsEntries'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 2000, false)
        List<String> removals = new CopyOnWriteArrayList<>()
        myCache.addListener([
                noteKeyAddition: { cache, key, value -> },
                noteKeyUpdate: { cache, key, newValue, oldValue -> },
                noteKeyRemoval: { cache, key, oldValue -> removals.add("$key=$oldValue".toString()) },
        ] as CacheListener<String, String>)
        myCache.put('a', '1')
        myCache.put('b', '2')
        myCache.put('c', '3')

        myCache.setMaxSize(10)
        assert myCache.maxSize == 10
        assert myCache.keys() == ['a', 'b', 'c'] as Set
        assert myCache.get('b') == '2'
        assert removals.isEmpty()

        // the copied entries keep the expiry they had left and are reported once, by the new store only
        assert waitFor { removals.size() >= 3 }
        sleep(1500) // past the duplicate window: a second notification would show up by now
        assert removals.sort() == ['a=1', 'b=2', 'c=3']
        assert myCache.isEmpty()
    }

    @Test
    void testUndeclaredPublishingCacheIsWarnedAboutOnce() {
        String before = 'testWarnUndeclared.beforeBus'
        String after = 'testWarnUndeclared.afterBus'
        UtilCache.createUtilCache(before, 0, 0, 0, false)
        assert !UtilCache.wasWarnedUndeclared(before)
        assert UtilCache.warnUndeclaredCaches() == []
        installBus(2000)
        try {
            // a cache that existed when the bus was installed was reported by the grouped warning at install
            assert UtilCache.wasWarnedUndeclared(before)
            assert UtilCache.warnUndeclaredCaches() == []
            assert !UtilCache.wasWarnedUndeclared(after)
            // a cache created while the bus is installed is checked when created, and the name warns only once
            UtilCache.createUtilCache(after, 0, 0, 0, false)
            assert UtilCache.wasWarnedUndeclared(after)
            UtilCache.createUtilCache(after, 0, 0, 0, false)
            UtilCache.getOrCreateUtilCache(after + '2', 0, 0, 0, false)
            // names already warned about individually are not reported again, nor is the earlier grouped one
            assert UtilCache.warnUndeclaredCaches() == []
            assert UtilCache.wasWarnedUndeclared(after + '2')
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

    @Test
    void testDeclaredOrEntityOrBuslessCachesAreNotWarnedAbout() {
        String declared = 'testWarnDeclared.local'
        String prefixed = 'testWarnDeclared.prefixed.one'
        String entityLike = 'testWarnDeclared.entity'
        UtilCache.createUtilCache('testWarnBusless', 0, 0, 0, false)
        UtilCache.warnUndeclaredCaches()
        assert !UtilCache.wasWarnedUndeclared('testWarnBusless')
        installBus(2000)
        try {
            UtilCache.declare([new CacheDeclaration(declared, false, null, null, null, CachePolicy.LOCAL),
                               new CacheDeclaration('testWarnDeclared.prefixed.', true, null, null, null, CachePolicy.LOCAL)])
            UtilCache.createUtilCache(declared, 0, 0, 0, false)
            UtilCache.createUtilCache(prefixed, 0, 0, 0, false)
            UtilCache.getOrCreateUtilCache(entityLike, 0, 0, 0, true, false)
            UtilCache.warnUndeclaredCaches()
            assert !UtilCache.wasWarnedUndeclared(declared)
            assert !UtilCache.wasWarnedUndeclared(prefixed)
            assert !UtilCache.wasWarnedUndeclared(entityLike)
        } finally {
            UtilCache.declare([])
            CacheInvalidation.uninstall()
        }
    }

}
