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
package org.apache.ofbiz.base.util.collections.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.GenericMap;
import org.apache.ofbiz.base.util.collections.GenericMapEntry;
import org.apache.ofbiz.base.util.collections.IteratorWrapper;

public class GenericMapTest extends GenericTestCaseBase {

    public static final String module = GenericMapTest.class.getName();

    @SuppressWarnings("serial")
    public static class TestGenericMap<K, V> extends GenericMap<K, V> {
        private static final String[] countNames = {
            "clearInternal",
            "containsKey",
            "get-true",
            "get-false",
            "isEmpty",
            "iterator-true",
            "iterator-false",
            "putInternal",
            "putAllIterator",
            "removeInternal-true",
            "removeInternal-false",
            "size",
        };
        protected final Map<String, Integer> counts = new HashMap<>();
        protected final Map<K, V> proxyMap;

        protected TestGenericMap() {
            this(null);
        }

        protected TestGenericMap(Map<K, V> srcMap) {
            for (String countName: countNames) {
                counts.put(countName, 0);
            }
            if (srcMap != null) {
                proxyMap = new HashMap<>(srcMap);
            } else {
                proxyMap = new HashMap<>();
            }
        }

        private void incrementCallCount(String name) {
            counts.put(name, counts.get(name) + 1);
        }

        public List<Integer> getCounts() {
            List<Integer> result = new ArrayList<>();
            for (String countName: countNames) {
                result.add(counts.get(countName));
            }
            return result;
        }

        @Override
        protected void clearInternal() {
            incrementCallCount("clearInternal");
            proxyMap.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            incrementCallCount("containsKey");
            return proxyMap.containsKey(key);
        }

        @Override
        protected V get(Object key, boolean noteAccess) {
            incrementCallCount("get-" + noteAccess);
            return proxyMap.get(key);
        }

        @Override
        public boolean isEmpty() {
            incrementCallCount("isEmpty");
            return proxyMap.isEmpty();
        }

        @Override
        protected Iterator<Map.Entry<K, V>> iterator(final boolean noteAccess) {
            incrementCallCount("iterator-" + noteAccess);
            return new IteratorWrapper<Map.Entry<K, V>, Map.Entry<K, V>>(proxyMap.entrySet().iterator()) {
                @Override
                protected Map.Entry<K, V> convert(Map.Entry<K, V> src) {
                    return new GenericMapEntry<>(TestGenericMap.this, src.getKey(), noteAccess);
                }
                @Override
                protected void noteRemoval(Map.Entry<K, V> dest, Map.Entry<K, V> src) {
                }
            };
        }

        @Override
        public V put(K key, V value) {
            incrementCallCount("putInternal");
            if (!proxyMap.containsKey(key)) incrementModCount();
            return proxyMap.put(key, value);
        }

        @Override
        protected <KE extends K, VE extends V> void putAllIterator(Iterator<Map.Entry<KE, VE>> it) {
            incrementCallCount("putAllIterator");
            while (it.hasNext()) {
                Map.Entry<KE, VE> entry = it.next();
                proxyMap.put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        protected V removeInternal(Object key, boolean incrementModCount) {
            incrementCallCount("removeInternal-" + incrementModCount);
            if (!proxyMap.containsKey(key)) return null;
            if (incrementModCount) incrementModCount();
            return proxyMap.remove(key);
        }

        @Override
        public int size() {
            incrementCallCount("size");
            return proxyMap.size();
        }
    }

    public GenericMapTest(String name) {
        super(name);
    }

    public void testFoo() throws Exception {
        TestGenericMap<String, Integer> map = new TestGenericMap<>();
        map.put("a", 0); Debug.logInfo("put a\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertEquals("get a", Integer.valueOf(0), map.get("a"));
        map.put("b", 1); Debug.logInfo("put b\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertEquals("get b", Integer.valueOf(1), map.get("b"));
        map.put("c", 2); Debug.logInfo("put c\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertEquals("get c", Integer.valueOf(2), map.get("c"));
        map.put("d", 3); Debug.logInfo("put d\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertEquals("get d", Integer.valueOf(3), map.get("d"));
        map.put("c", 22); Debug.logInfo("put c-2\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertEquals("get c-2", Integer.valueOf(22), map.get("c"));
        map.remove("b"); Debug.logInfo("remove b\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        assertNull("null b", map.get("b"));
        map.remove("aaa"); Debug.logInfo("remove aaa\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        Debug.logInfo("map=" + map, module);
        Debug.logInfo("counts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
        // this seems to call size()
        new HashMap<>(map);
        Debug.logInfo("counts=" + map.getCounts() + ", modCount=" + map.getModCount(), module);
    }

}
