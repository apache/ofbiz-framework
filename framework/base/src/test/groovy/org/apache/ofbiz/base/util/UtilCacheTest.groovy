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
import org.apache.ofbiz.base.util.cache.CacheListener
import org.apache.ofbiz.base.util.cache.CachePolicy
import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.base.util.tool.UtilCacheTestTools.Listener

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

// codenarc-disable JUnitLostTest
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
    void testDeclareReconfiguresExistingCache() {
        String name = 'testDeclareReconfigures'
        UtilCache<String, String> myCache = UtilCache.createUtilCache(name, 0, 0, 0, false)
        myCache.put('key', 'value')
        try {
            UtilCache.declare([new CacheDeclaration(name, false, 3L, 50000L, null, CachePolicy.LOCAL)])
            assert myCache.maxSize == 3
            assert myCache.expireTime == 50000
            assert myCache.policy == CachePolicy.LOCAL
            assert myCache.get('key') == 'value'
        } finally {
            UtilCache.declare([])
        }
        assert myCache.maxSize == 0
        assert myCache.expireTime == 0
        assert myCache.policy == CachePolicy.INVALIDATE
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

}
