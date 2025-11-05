package org.apache.ofbiz.base.util.cache;

import org.apache.ofbiz.base.util.UtilObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UtilCacheTestTools {

    static <K, V> Listener<K, V> createListener(UtilCache<K, V> cache) {
        Listener<K, V> listener = new Listener<>();
        cache.addListener(listener);
        return listener;
    }

    abstract static class Change {
        private int count = 1;

        public int getCount() {
            return count;
        }

        public void incCount() {
            count += 1;
        }
    }

    protected static final class Removal<V> extends Change {
        private final V oldValue;

        protected Removal(V oldValue) {
            this.oldValue = oldValue;
        }

        public int hashCode() {
            return UtilObject.doHashCode(oldValue);
        }

        public boolean equals(Object o) {
            if (o instanceof Removal<?> other) {
                return Objects.equals(oldValue, other.oldValue);
            }
            return false;
        }
    }

    protected static final class Addition<V> extends Change {
        private final V newValue;

        protected Addition(V newValue) {
            this.newValue = newValue;
        }

        public int hashCode() {
            return UtilObject.doHashCode(newValue);
        }

        public boolean equals(Object o) {
            if (o instanceof Addition<?> other) {
                return Objects.equals(newValue, other.newValue);
            }
            return false;
        }
    }

    protected static final class Update<V> extends Change {
        private final V newValue;
        private final V oldValue;

        protected Update(V newValue, V oldValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        public int hashCode() {
            return UtilObject.doHashCode(newValue) ^ UtilObject.doHashCode(oldValue);
        }

        public boolean equals(Object o) {
            if (o instanceof Update<?> other) {
                if (!Objects.equals(newValue, other.newValue)) {
                    return false;
                }
                return Objects.equals(oldValue, other.oldValue);
            }
            return false;
        }
    }

    static final class Listener<K, V> implements CacheListener<K, V> {
        private final Map<K, Set<Change>> changeMap = new HashMap<>();

        private void add(K key, Change change) {
            Set<Change> changeSet = changeMap.computeIfAbsent(key, k -> new HashSet<>());
            for (Change checkChange : changeSet) {
                if (checkChange.equals(change)) {
                    checkChange.incCount();
                    return;
                }
            }
            changeSet.add(change);
        }

        public synchronized void noteKeyRemoval(UtilCache<K, V> cache, K key, V oldValue) {
            add(key, new Removal<>(oldValue));
        }

        public synchronized void noteKeyAddition(UtilCache<K, V> cache, K key, V newValue) {
            add(key, new Addition<>(newValue));
        }

        public synchronized void noteKeyUpdate(UtilCache<K, V> cache, K key, V newValue, V oldValue) {
            add(key, new Update<>(newValue, oldValue));
        }

        public boolean equals(Object o) {
            if (!(o instanceof Listener<?, ?> other)) {
                return false;
            }
            return changeMap.equals(other.changeMap);
        }

        public int hashCode() {
            return super.hashCode();
        }
    }
}
