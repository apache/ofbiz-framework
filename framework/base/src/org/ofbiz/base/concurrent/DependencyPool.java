package org.ofbiz.base.concurrent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.ofbiz.base.lang.LockedBy;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.util.Debug;

@SourceMonitored
public class DependencyPool<K, I extends DependencyPool.Item<I, K, V>, V> {
    public static final String module = DependencyPool.class.getName();

    private final Executor executor;
    private final ConcurrentMap<K, I> allItems = new ConcurrentHashMap<K, I>();
    private final ConcurrentMap<K, Future<V>> results = new ConcurrentHashMap<K, Future<V>>();
    private final Set<K> provides = new ConcurrentSkipListSet<K>();
    private final ReentrantLock submitLock = new ReentrantLock();
    private final Condition submitCondition = submitLock.newCondition();
    private final int inflight;
    @LockedBy("submitLock")
    private final Set<I> outstanding = new HashSet<I>();
    @LockedBy("submitLock")
    private final List<I> pending = new LinkedList<I>();

    public DependencyPool(Executor executor, int inflight) {
        this.executor = executor;
        this.inflight = inflight;
    }

    public I add(I item) {
        if (allItems.putIfAbsent(item.getKey(), item) == null) {
            submitLock.lock();
            try {
                pending.add(item);
            } finally {
                submitLock.unlock();
            }
        }
        return allItems.get(item.getKey());
    }

    public void addAll(Collection<I> items) {
        for (I item: items) {
            add(item);
        }
    }

    public void start() {
        submitLock.lock();
        try {
            submitWork();
        } finally {
            submitLock.unlock();
        }
    }

    public V getResult(I item) throws InterruptedException, ExecutionException {
        Future<V> future = results.get(item.getKey());
        if (future == null) {
            return null;
        } else {
            return future.get();
        }
    }

    public int getResultCount() {
        return results.size();
    }

    public boolean await() throws InterruptedException {
        submitLock.lock();
        try {
            Debug.logInfo("a outstanding.size=" + outstanding.size(), module);
            Debug.logInfo("a pending.size=" + pending.size(), module);
            submitWork();
            while (!outstanding.isEmpty()) {
                submitCondition.await();
            }
            Debug.logInfo("b outstanding.size=" + outstanding.size(), module);
            Debug.logInfo("b pending.size=" + pending.size(), module);
            return pending.isEmpty();
        } finally {
            submitLock.unlock();
        }
    }

    @LockedBy("submitLock")
    private int submitWork() {
        Iterator<I> pendingIt = pending.iterator();
        int submittedCount = 0;
OUTER:
        while (pendingIt.hasNext()) {
            I item = pendingIt.next();
            for (K dep: item.getDependencies()) {
                if (!results.containsKey(dep) && !provides.contains(dep)) {
                    continue OUTER;
                }
            }
            submittedCount++;
            pendingIt.remove();
            outstanding.add(item);
            executor.execute(new ItemTask(item));
        }
        return submittedCount;
    }

    private class ItemTask extends FutureTask<V> {
        private final I item;

        protected ItemTask(I item) {
            super(item);
            this.item = item;
        }

        protected void done() {
            super.done();
            results.put(item.getKey(), this);
            provides.addAll(item.getProvides());
            submitLock.lock();
            try {
                outstanding.remove(item);
                if (outstanding.size() < inflight && submitWork() == 0 && outstanding.isEmpty()) {
                    submitCondition.signal();
                }
            } finally {
                submitLock.unlock();
            }
        }
    }

    public interface Item<I extends Item<I, K, V>, K, V> extends Callable<V> {
        K getKey();
        Collection<K> getDependencies();
        Collection<K> getProvides();
    }
}
