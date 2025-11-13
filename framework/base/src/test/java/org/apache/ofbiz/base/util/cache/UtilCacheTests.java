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
package org.apache.ofbiz.base.util.cache;

import static org.apache.ofbiz.base.util.cache.UtilCacheTestTools.createListener;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.cache.UtilCacheTestTools.Listener;
import org.junit.Test;

@SuppressWarnings("serial")
public class UtilCacheTests implements Serializable {

    private <K, V> UtilCache<K, V> createUtilCache(int sizeLimit, int maxInMemory, long expireIn, boolean useSoftReference) {
        return UtilCache.createUtilCache(getClass().getName(), sizeLimit, maxInMemory, expireIn, useSoftReference);
    }

    @Test
    public void testCreateUtilCache() {
        String name = getClass().getName();
        doUtilCacheCreateTest(UtilCache.createUtilCache(), null, null, null, null);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name), null, null, null, null);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, false), null, null, null, Boolean.FALSE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, true), null, null, null, Boolean.TRUE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(5, 15000), 5, null, 15000L, null);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 6, 16000), 6, null, 16000L, null);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 7, 17000, false), 7, null, 17000L, Boolean.FALSE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 8, 18000, true), 8, null, 18000L, Boolean.TRUE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 9, 5, 19000, false), 9, 5, 19000L, Boolean.FALSE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 10, 6, 20000, false), 10, 6, 20000L, Boolean.FALSE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 11, 7, 21000, false, "a", "b"), 11, 7, 21000L, Boolean.FALSE);
        doUtilCacheCreateTest(UtilCache.createUtilCache(name, 12, 8, 22000, false, "c", "d"), 12, 8, 22000L, Boolean.FALSE);
    }

    @Test
    public void testSimple() throws Exception {
        UtilCache<String, String> myCache = createUtilCache(5, 0, 0, false);
        String myCacheName = myCache.getName();
        Listener<String, String> myCacheListener = createListener(myCache);
        Listener<String, String> controlListener = new Listener<>();

        for (int i = 0; i < 2; i++) {
            assertTrue("UtilCacheTable.keySet", UtilCache.getUtilCacheTableKeySet().contains(myCacheName));
            assertSame("UtilCache.findCache", myCache, UtilCache.findCache(myCacheName));
            assertSame("UtilCache.getOrCreateUtilCache", myCache, UtilCache.getOrCreateUtilCache(myCacheName,
                    myCache.getSizeLimit(), myCache.getMaxInMemory(), myCache.getExpireTime(), myCache.getUseSoftReference()));

            doKeyNotInCacheTest(myCache, "one");
            long origByteSize = myCache.getSizeInBytes();

            controlListener.noteKeyAddition(myCache, null, "null");
            assertNull("put", myCache.put(null, "null"));
            doKeyInCacheTest(myCache, null, "null");
            long nullByteSize = myCache.getSizeInBytes();
            assertTrue(nullByteSize > origByteSize);

            controlListener.noteKeyRemoval(myCache, null, "null");
            assertEquals("remove", "null", myCache.remove(null));
            doKeyNotInCacheTest(myCache, null);

            controlListener.noteKeyAddition(myCache, "one", "uno");
            assertNull("put", myCache.put("one", "uno"));
            doKeyInCacheTest(myCache, "one", "uno");
            long unoByteSize = myCache.getSizeInBytes();
            assert(unoByteSize > origByteSize);

            controlListener.noteKeyUpdate(myCache, "one", "single", "uno");
            assertEquals("replace", "uno", myCache.put("one", "single"));
            doKeyInCacheTest(myCache, "one", "single");
            long singleByteSize = myCache.getSizeInBytes();
            assert(singleByteSize > origByteSize);
            assert(singleByteSize > unoByteSize);

            controlListener.noteKeyRemoval(myCache, "one", "single");
            assertEquals("remove", "single", myCache.remove("one"));
            doKeyNotInCacheTest(myCache, "one");
            assertEquals("byteSize", origByteSize, myCache.getSizeInBytes());

            controlListener.noteKeyAddition(myCache, "one", "uno");
            assertNull("put", myCache.put("one", "uno"));
            doKeyInCacheTest(myCache, "one", "uno");

            controlListener.noteKeyUpdate(myCache, "one", "only", "uno");
            assertEquals("replace", "uno", myCache.put("one", "only"));
            doKeyInCacheTest(myCache, "one", "only");

            controlListener.noteKeyRemoval(myCache, "one", "only");
            myCache.erase();
            doKeyNotInCacheTest(myCache, "one");
            assertEquals("byteSize", origByteSize, myCache.getSizeInBytes());

            myCache.setExpireTime(100);
            controlListener.noteKeyAddition(myCache, "one", "uno");
            assertNull("put", myCache.put("one", "uno"));
            doKeyInCacheTest(myCache, "one", "uno");

            controlListener.noteKeyRemoval(myCache, "one", "uno");
            Thread.sleep(200);
            doKeyNotInCacheTest(myCache, "one");
        }
        assertEquals("get-miss", 10, myCache.getMissCountNotFound());
        assertEquals("get-miss-total", 10, myCache.getMissCountTotal());
        assertEquals("get-hit", 12, myCache.getHitCount());
        assertEquals("remove-hit", 6, myCache.getRemoveHitCount());
        assertEquals("remove-miss", 10, myCache.getRemoveMissCount());
        myCache.removeListener(myCacheListener);
        assertEquals("listener", controlListener, myCacheListener);
        UtilCache.clearCache(myCacheName);
        UtilCache.clearCache(":::" + myCacheName);
    }

    @Test
    public void testPutIfAbsent() {
        UtilCache<String, String> myCache = createUtilCache(5, 5, 2000, false);
        Listener<String, String> myCacheListener = createListener(myCache);
        Listener<String, String> controlListener = new Listener<>();

        controlListener.noteKeyAddition(myCache, "two", "dos");
        assertNull("putIfAbsent", myCache.putIfAbsent("two", "dos"));
        doKeyInCacheTest(myCache, "two", "dos");
        assertEquals("putIfAbsent", "dos", myCache.putIfAbsent("two", "double"));
        doKeyInCacheTest(myCache, "two", "dos");
        myCache.removeListener(myCacheListener);
        assertEquals("listener", controlListener, myCacheListener);
    }

    @Test
    public void testPutIfAbsentAndGet() {
        UtilCache<String, String> myCache = createUtilCache(5, 5, 2000, false);
        Listener<String, String> myCacheListener = createListener(myCache);
        Listener<String, String> controlListener = new Listener<>();
        controlListener.noteKeyAddition(myCache, "key", "value");
        controlListener.noteKeyAddition(myCache, "anotherKey", "anotherValue");
        assertNull("no-get", myCache.get("key"));
        assertEquals("putIfAbsentAndGet", "value", myCache.putIfAbsentAndGet("key", "value"));
        doKeyInCacheTest(myCache, "key", "value");
        assertEquals("putIfAbsentAndGet", "value", myCache.putIfAbsentAndGet("key", "newValue"));
        doKeyInCacheTest(myCache, "key", "value");
        String someValue = new String("anotherValue");
        String someOtherValue = new String("anotherValue");
        assertEquals(someValue, someOtherValue);
        assertNotSame(someValue, someOtherValue);
        String cachedValue = myCache.putIfAbsentAndGet("anotherKey", someValue);
        assertSame(cachedValue, someValue);
        cachedValue = myCache.putIfAbsentAndGet("anotherKey", someOtherValue);
        assertNotSame(cachedValue, someOtherValue);
        assertSame(cachedValue, someValue);
        myCache.removeListener(myCacheListener);
        assertEquals("listener", controlListener, myCacheListener);
    }

    @Test
    public void testChangeMemSize() {
        int size = 5;
        long expireIn = 2000;
        UtilCache<String, Serializable> myCache = createUtilCache(size, size, expireIn, false);
        Map<String, Serializable> controlMap = new HashMap<>();
        doAllKeysTest(size, myCache, controlMap);
        myCache.setMaxInMemory(2);
        assertEquals("cache.size", 2, myCache.size());
        controlMap.keySet().retainAll(myCache.getCacheLineKeys());
        assertEquals("map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue("map-values", myCache.values().containsAll(controlMap.values()));
        myCache.setMaxInMemory(0);
        assertEquals("map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue("map-values", myCache.values().containsAll(controlMap.values()));
        for (int i = size * 2; i < size * 3; i++) {
            String s = Integer.toString(i);
            doSingleKeyTest(s, myCache, i - size * 2 + 3, controlMap);
        }
        myCache.setMaxInMemory(0);
        assertEquals("map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue("map-values", myCache.values().containsAll(controlMap.values()));
        myCache.setMaxInMemory(size);
        for (int i = 0; i < size * 2; i++) {
            controlMap.remove(Integer.toString(i));
        }
        // Can't compare the contents of these collections, as setting LRU after not
        // having one, means the items that get evicted are essentially random.
        assertEquals("map-keys", controlMap.size(), myCache.getCacheLineKeys().size());
        assertEquals("map-values", controlMap.size(), myCache.values().size());
    }

    @Test
    public void testExpire() throws Exception {
        int size = 5;
        long expireIn = 2000;
        UtilCache<String, Serializable> myCache = createUtilCache(size, 5, expireIn, false);
        Map<String, Serializable> controlMap = new HashMap<>();
        doAllKeysTest(size, myCache, controlMap);
        Thread.sleep(expireIn + 500);
        controlMap.clear();
        for (int i = 0; i < size; i++) {
            String s = Integer.toString(i);
            assertNull("no-key(" + s + ")", myCache.get(s));
        }
        assertEquals("map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue("map-values", myCache.values().containsAll(controlMap.values()));
        doAllKeysTest(5, myCache, controlMap);
        assertEquals("map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue("map-values", myCache.values().containsAll(controlMap.values()));
    }

    static <K, V> void doUtilCacheCreateTest(UtilCache<K, V> myCache, Integer sizeLimit, Integer maxInMemory,
                                             Long expireTime, Boolean useSoftReference) {
        if (sizeLimit != null) {
            assertEquals(myCache.getName() + ":sizeLimit", sizeLimit.intValue(), myCache.getSizeLimit());
        }
        if (maxInMemory != null) {
            assertEquals(myCache.getName() + ":maxInMemory", maxInMemory.intValue(), myCache.getMaxInMemory());
        }
        if (expireTime != null) {
            assertEquals(myCache.getName() + ":expireTime", expireTime.longValue(), myCache.getExpireTime());
        }
        if (useSoftReference != null) {
            assertEquals(myCache.getName() + ":useSoftReference", useSoftReference,
                    myCache.getUseSoftReference());
        }
        assertTrue("initial empty", myCache.isEmpty());
        assertEquals("empty keys", Collections.emptySet(), myCache.getCacheLineKeys());
        assertEquals("empty values", Collections.emptyList(), myCache.values());
        assertSame("find cache", myCache, UtilCache.findCache(myCache.getName()));
        assertNotSame("new cache", myCache, UtilCache.createUtilCache());
    }

    public static void doSingleKeyTest(String val, UtilCache<String, Serializable> myCache, int size,
                                       Map<String, Serializable> controlMap) {
        String label = val;
        String myKey = val;
        String myValue = val;
        String temp = ":" + val;

        assertNull(label + ":get-empty", myCache.get(myKey));
        assertFalse(label + ":containsKey-empty", myCache.containsKey(myKey));
        Serializable oldValue = myCache.put(myKey, temp);
        assertTrue(label + ":containsKey-class", myCache.containsKey(myKey));
        assertEquals(label + ":get-class", temp, myCache.get(myKey));
        assertNull(label + ":oldValue-class", oldValue);
        assertEquals(label + ":size-class", size, myCache.size());
        oldValue = myCache.put(myKey, myValue);
        assertTrue(label + ":containsKey-value", myCache.containsKey(myKey));
        assertEquals(label + ":get-value", myValue, myCache.get(myKey));
        assertEquals(label + ":oldValue-value", temp, oldValue);
        assertEquals(label + ":size-value", size, myCache.size());
        controlMap.put(myKey, myValue);
        assertEquals(label + ":map-keys", controlMap.keySet(), myCache.getCacheLineKeys());
        assertTrue(label + ":map-values", myCache.values().containsAll(controlMap.values()));
    }

    static <K, V> void doKeyInCacheTest(UtilCache<K, V> myCache, K myKey, V myValue) {
        assertFalse("is-empty", myCache.isEmpty());
        assertEquals("size", 1, myCache.size());
        assertTrue("found", myCache.containsKey(myKey));
        assertTrue("validKey", UtilCache.validKey(myCache.getName(), myKey));
        assertFalse("validKey", UtilCache.validKey(":::" + myCache.getName(), myKey));
        assertEquals("get", myValue, myCache.get(myKey));
        assertEquals("keys", new HashSet<>(UtilMisc.toList(myKey)), myCache.getCacheLineKeys());
        assertEquals("values", UtilMisc.toList(myValue), myCache.values());
    }

    static <K, V> void doKeyNotInCacheTest(UtilCache<K, V> myCache, K myKey) {
        assertFalse("not-found", myCache.containsKey(myKey));
        assertFalse("validKey", UtilCache.validKey(myCache.getName(), myKey));
        assertNull("no-get", myCache.get(myKey));
        assertNull("remove", myCache.remove(myKey));
        assertTrue("is-empty", myCache.isEmpty());
        assertEquals("size", 0, myCache.size());
        assertEquals("keys", Collections.emptySet(), myCache.getCacheLineKeys());
        assertEquals("values", Collections.emptyList(), myCache.values());
    }

    static void doAllKeysTest(int size, UtilCache<String, Serializable> cache, Map<String, Serializable> map) {
        for (int i = 0; i < size; i++) {
            String s = Integer.toString(i);
            doSingleKeyTest(s, cache, i + 1, map);
        }
    }

}
