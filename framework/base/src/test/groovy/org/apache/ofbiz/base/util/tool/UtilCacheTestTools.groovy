package org.apache.ofbiz.base.util.tool

import org.apache.ofbiz.base.util.UtilObject
import org.apache.ofbiz.base.util.cache.CacheListener
import org.apache.ofbiz.base.util.cache.UtilCache

class UtilCacheTestTools {

    static <K, V> Listener<K, V> createListener(UtilCache<K, V> cache) {
        Listener<K, V> listener = new Listener<>()
        cache.addListener(listener)
        return listener
    }

    static class Change {

        private int count = 1

        int getCount() {
            return count
        }

        void incCount() {
            count += 1
        }

    }

    protected static final class Removal<V> extends Change {

        private final V oldValue

        int hashCode() {
            return UtilObject.doHashCode(oldValue)
        }

        boolean equals(Object o) {
            if (o instanceof Removal) {
                return Objects.equals(oldValue, (o as Removal).oldValue)
            }
            return false
        }

        private Removal(V oldValue) {
            this.oldValue = oldValue
        }

    }

    protected static final class Addition<V> extends Change {

        private final V newValue

        int hashCode() {
            return UtilObject.doHashCode(newValue)
        }

        boolean equals(Object o) {
            if (o instanceof Addition) {
                return Objects.equals(newValue, (o as Addition).newValue)
            }
            return false
        }

        private Addition(V newValue) {
            this.newValue = newValue
        }

    }

    protected static final class Update<V> extends Change {

        private final V newValue
        private final V oldValue

        int hashCode() {
            return UtilObject.doHashCode(newValue) ^ UtilObject.doHashCode(oldValue)
        }

        boolean equals(Object o) {
            if (o instanceof Update) {
                if (!Objects.equals(newValue, (o as Update).newValue)) {
                    return false
                }
                return Objects.equals(oldValue, (o as Update).oldValue)
            }
            return false
        }

        private Update(V newValue, V oldValue) {
            this.newValue = newValue
            this.oldValue = oldValue
        }

    }

    public static final class Listener<K, V> implements CacheListener<K, V> {

        private final Map<K, Set<Change>> changeMap = [:]

        // codenarc-disable UnusedMethodParameter, SynchronizedMethod
        synchronized void noteKeyRemoval(UtilCache<K, V> cache, K key, V oldValue) {
            add(key, new Removal<>(oldValue))
        }

        synchronized void noteKeyAddition(UtilCache<K, V> cache, K key, V newValue) {
            add(key, new Addition<>(newValue))
        }

        synchronized void noteKeyUpdate(UtilCache<K, V> cache, K key, V newValue, V oldValue) {
            add(key, new Update<>(newValue, oldValue))
        }
        // codenarc-enable UnusedMethodParameter, SynchronizedMethod

        boolean equals(Object o) {
            if (!(o instanceof Listener)) {
                return false
            }
            return changeMap == (o as Listener).changeMap
        }

        int hashCode() {
            return super.hashCode()
        }

        private void add(K key, Change change) {
            Set<Change> changeSet = changeMap.computeIfAbsent(key, k -> [] as Set)
            for (Change checkChange : changeSet) {
                if (checkChange == change) {
                    checkChange.incCount()
                    return
                }
            }
            changeSet.add(change)
        }

    }

}
