package org.apache.ofbiz.base.util

import static org.apache.ofbiz.base.util.tool.UtilCacheTestTools.createListener

import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.base.util.tool.UtilCacheTestTools.Listener
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.junit.Test
import org.junit.jupiter.api.BeforeAll

// codenarc-disable JUnitLostTest
class UtilCacheTest extends OFBizTestCase {

    UtilCacheTest(String name) {
        super(name)
    }

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
        assert controlMap.keySet() == cacheToPopulate.getCacheLineKeys()
        assert cacheToPopulate.values().containsAll(controlMap.values())
    }

    static void doUtilCacheCreateTest(UtilCache myCache, Integer sizeLimit, Integer maxInMemory, Long expireTime,
                                      Boolean useSoftReference) {
        if (sizeLimit) {
            assert sizeLimit.intValue() == myCache.sizeLimit
        }
        if (maxInMemory) {
            assert maxInMemory.intValue() == myCache.maxInMemory
        }
        if (expireTime) {
            assert expireTime.longValue() == myCache.expireTime
        }
        if (useSoftReference) {
            assert useSoftReference == myCache.getUseSoftReference()
        }
        assert myCache.isEmpty()
        assert Collections.emptySet() == myCache.cacheLineKeys
        assert Collections.emptyList() == myCache.values()
        assert myCache === UtilCache.findCache(myCache.name)
        assert myCache !== UtilCache.createUtilCache()
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
        doUtilCacheCreateTest(UtilCache.createUtilCache(), null, null, null, null)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name), null, null, null, null)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, false), null, null, null, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, true), null, null, null, Boolean.TRUE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(5, 15000), 5, null, 15000L, null)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 6, 16000), 6, null, 16000L, null)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 7, 17000, false), 7, null, 17000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 8, 18000, true), 8, null, 18000L, Boolean.TRUE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 9, 5, 19000, false), 9, 5, 19000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 10, 6, 20000, false), 10, 6, 20000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 11, 7, 21000, false, 'a', 'b'), 11, 7, 21000L, Boolean.FALSE)
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 12, 8, 22000, false, 'c', 'd'), 12, 8, 22000L, Boolean.FALSE)
    }
    // codenarc-enable JUnitTestMethodWithoutAssert

    @Test
    void testCacheGetterOnCreation() {
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        assert UtilCache.getUtilCacheTableKeySet().contains(name)
        assert myCache == UtilCache.findCache(name)
        assert myCache == UtilCache.getOrCreateUtilCache(
                name, myCache.sizeLimit, myCache.maxInMemory, myCache.expireTime, myCache.useSoftReference)
    }

    @Test
    void testCacheCreateEntry() {
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
        UtilCache myCache = UtilCache.createUtilCache(name, 5, 0, 0, false)
        Listener myCacheListener = createListener(myCache)
        Listener controlListener = new Listener()
        Map controlMap = [:]
        myCache.setExpireTime(100)
        populateEmptyCache(name, myCache, 5, controlMap, controlListener)
        controlMap.forEach { k, v -> controlListener.noteKeyRemoval(myCache, k, v) }
        try {
            sleep(200)
        } catch (InterruptedException e) {
            fail('Failed to pause process during tests execution')
        }
        assert myCache.getCacheLineKeys().isEmpty()
        assert myCache.values().isEmpty()
        assert myCacheListener == controlListener
    }

    @Test
    void testChangeMemorySize() {
        int size = 5
        UtilCache<String, Serializable> myCache = UtilCache.createUtilCache(name, size, size, 0, false)
        Map controlMap = [:]
        populateEmptyCache(name, myCache, 5, controlMap)

        myCache.setMaxInMemory(2)
        assert myCache.size() == 2
        controlMap.keySet().retainAll(myCache.getCacheLineKeys())
        assert controlMap.keySet() == myCache.getCacheLineKeys()
        assert myCache.values().containsAll(controlMap.values())

        myCache.setMaxInMemory(0)
        assert controlMap.keySet() == myCache.getCacheLineKeys()
        assert myCache.values().containsAll(controlMap.values())

        myCache.setMaxInMemory(size)
        assert controlMap.keySet() == myCache.getCacheLineKeys()
        assert myCache.values().containsAll(controlMap.values())
    }

    @Test
    void testPutIfAbsent() {
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

}
