package org.apache.ofbiz.base.util

import org.apache.ofbiz.base.util.cache.UtilCache
import org.apache.ofbiz.service.testtools.OFBizTestCase

/**
 * ./gradlew test --tests '*UtilCacheTest'
 */
class UtilCacheTest extends OFBizTestCase {

    UtilCacheTest(String name) {
        super(name)
    }

    /**
     * Tests cache creation methods, with various parameters
     */
    public void testUtilCacheCreation() {
        String name = this.name
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

    static void doUtilCacheCreateTest(UtilCache myCache, Integer sizeLimit, Integer maxInMemory, Long expireTime,
                                      Boolean useSoftReference) {
        if (sizeLimit) {
            assertEquals myCache.name + ':sizeLimit', sizeLimit.intValue(), myCache.sizeLimit
        }
        if (maxInMemory) {
            assertEquals myCache.name + ':maxInMemory', maxInMemory.intValue(), myCache.maxInMemory
        }
        if (expireTime) {
            assertEquals myCache.name + ':expireTime', expireTime.longValue(), myCache.expireTime
        }
        if (useSoftReference) {
            assertEquals(myCache.name + ':useSoftReference', useSoftReference,
                    myCache.getUseSoftReference())
        }
        assert 'initial empty', myCache.isEmpty()
        assertEquals 'empty keys', Collections.emptySet(), myCache.cacheLineKeys
        assertEquals 'empty values', Collections.emptyList(), myCache.values()
        assertSame 'find cache', myCache, UtilCache.findCache(myCache.name)
        assertNotSame 'new cache', myCache, UtilCache.createUtilCache()
    }

    public void testCacheBases() {

    }

    static <K, V> void doKeyInCacheTest(UtilCache myCache, K myKey, V myValue) {
        assert 'is-empty', !myCache.isEmpty()
        assertEquals 'size', 1, myCache.size()
        assert 'found', myCache.containsKey(myKey)
        assert 'validKey', UtilCache.validKey(myCache.getName(), myKey)
        assert 'validKey', !UtilCache.validKey(':::' + myCache.getName(), myKey)
        assertEquals 'get', myValue, myCache.get(myKey)
        assertEquals 'keys', new HashSet<>(UtilMisc.toList(myKey)), myCache.getCacheLineKeys()
        assertEquals 'values', UtilMisc.toList(myValue), myCache.values()
    }

    static <Ka> void doKeyNotInCacheTest(UtilCache myCache, Ka myKey) {
        assert 'not-found', !myCache.containsKey(myKey)
        assert 'validKey', !UtilCache.validKey(myCache.getName(), myKey)
        assertNull 'no-get', myCache.get(myKey)
        assertNull 'remove', myCache.remove(myKey)
        assert 'is-empty', myCache.isEmpty()
        assertEquals 'size', 0, myCache.size()
        assertEquals 'keys', Collections.emptySet(), myCache.getCacheLineKeys()
        assertEquals 'values', Collections.emptyList(), myCache.values()
    }

}
