package org.ofbiz.base.util.collections.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.collections.GenericMap;
import org.ofbiz.base.util.collections.GenericMapEntry;
import org.ofbiz.base.util.collections.IteratorWrapper;
import org.ofbiz.base.test.GenericTestCaseBase;

public class GenericMapTest extends GenericTestCaseBase {
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
        protected final Map<String, Integer> counts = new HashMap<String, Integer>();
        protected final Map<K, V> proxyMap;

        protected TestGenericMap() {
            this(null);
        }

        protected TestGenericMap(Map<K, V> srcMap) {
            for (String countName: countNames) {
                counts.put(countName, 0);
            }
            if (srcMap != null) {
                proxyMap = new HashMap<K, V>(srcMap);
            } else {
                proxyMap = new HashMap<K, V>();
            }
        }

        private void incrementCallCount(String name) {
            counts.put(name, counts.get(name) + 1);
        }

        public List<Integer> getCounts() {
            List<Integer> result = new ArrayList<Integer>();
            for (String countName: countNames) {
                result.add(counts.get(countName));
            }
            return result;
        }

        protected void clearInternal() {
            incrementCallCount("clearInternal");
            proxyMap.clear();
        }

        public boolean containsKey(Object key) {
            incrementCallCount("containsKey");
            return proxyMap.containsKey(key);
        }

        protected V get(Object key, boolean noteAccess) {
            incrementCallCount("get-" + noteAccess);
            return proxyMap.get(key);
        }

        public boolean isEmpty() {
            incrementCallCount("isEmpty");
            return proxyMap.isEmpty();
        }

        protected Iterator<Map.Entry<K, V>> iterator(final boolean noteAccess) {
            incrementCallCount("iterator-" + noteAccess);
            //return new IteratorWrapper<Map.Entry<K, V>, Map.Entry<K, V>>(noteAccess, proxyMap.entrySet().iterator()) {
            return new IteratorWrapper<Map.Entry<K, V>, Map.Entry<K, V>>(proxyMap.entrySet().iterator()) {
                protected Map.Entry<K, V> convert(Map.Entry<K, V> src) {
                    return new GenericMapEntry<K, V>(TestGenericMap.this, src.getKey(), noteAccess);
                }
                protected void noteRemoval(Map.Entry<K, V> dest, Map.Entry<K, V> src) {
                }
            };
        }

        public V put(K key, V value) {
            incrementCallCount("putInternal");
            if (!proxyMap.containsKey(key)) incrementModCount();
            return proxyMap.put(key, value);
        }

        protected <KE extends K, VE extends V> void putAllIterator(Iterator<Map.Entry<KE, VE>> it) {
            incrementCallCount("putAllIterator");
            while (it.hasNext()) {
                Map.Entry<KE, VE> entry = it.next();
                proxyMap.put(entry.getKey(), entry.getValue());
            }
        }

        protected V removeInternal(Object key, boolean incrementModCount) {
            incrementCallCount("removeInternal-" + incrementModCount);
            if (!proxyMap.containsKey(key)) return null;
            if (incrementModCount) incrementModCount();
            return proxyMap.remove(key);
        }

        public int size() {
            incrementCallCount("size");
            return proxyMap.size();
        }
    }

    public GenericMapTest(String name) {
        super(name);
    }

    public void testFoo() throws Exception {
        TestGenericMap<String, Integer> map = new TestGenericMap<String, Integer>();
        map.put("a", 0); System.err.println("put a\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertEquals("get a", Integer.valueOf(0), map.get("a"));
        map.put("b", 1); System.err.println("put b\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertEquals("get b", Integer.valueOf(1), map.get("b"));
        map.put("c", 2); System.err.println("put c\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertEquals("get c", Integer.valueOf(2), map.get("c"));
        map.put("d", 3); System.err.println("put d\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertEquals("get d", Integer.valueOf(3), map.get("d"));
        map.put("c", 22); System.err.println("put c-2\t\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertEquals("get c-2", Integer.valueOf(22), map.get("c"));
        map.remove("b"); System.err.println("remove b\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        assertNull("null b", map.get("b"));
        map.remove("aaa"); System.err.println("remove aaa\tcounts=" + map.getCounts() + ", modCount=" + map.getModCount());
        System.err.println("map=" + map);
        System.err.println("counts=" + map.getCounts() + ", modCount=" + map.getModCount());
        // this seems to call size()
        new HashMap<String, Integer>(map);
        System.err.println("counts=" + map.getCounts() + ", modCount=" + map.getModCount());
    }

}
