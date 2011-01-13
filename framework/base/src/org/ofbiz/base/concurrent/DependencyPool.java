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
package org.ofbiz.base.concurrent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.ofbiz.base.lang.LockedBy;
import org.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public class DependencyPool<K, I extends DependencyPool.Item<I, K, V>, V> {
    private final Executor executor;
    private final ConcurrentMap<K, I> allItems = new ConcurrentHashMap<K, I>();
    private final ConcurrentMap<K, Future<V>> results = new ConcurrentHashMap<K, Future<V>>();
    private final ReentrantLock submitLock = new ReentrantLock();
    private final Condition submitCondition = submitLock.newCondition();
    @LockedBy("submitLock")
    private final Set<I> outstanding = new HashSet<I>();
    @LockedBy("submitLock")
    private final List<I> pending = new LinkedList<I>();

    public DependencyPool(Executor executor) {
        this.executor = executor;
    }

    public void add(I item) {
        if (allItems.putIfAbsent(item.getKey(), item) == null) {
            submitLock.lock();
            try {
                pending.add(item);
            } finally {
                submitLock.unlock();
            }
        }
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
            submitWork();
            while (!outstanding.isEmpty()) {
                submitCondition.await();
            }
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
                if (!results.containsKey(dep)) {
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

        @Override
        protected void done() {
            super.done();
            results.put(item.getKey(), this);
            submitLock.lock();
            try {
                outstanding.remove(item);
                if (submitWork() == 0 && outstanding.isEmpty()) {
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
    }
}
