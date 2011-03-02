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
package org.ofbiz.base.concurrent.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.ofbiz.base.concurrent.DependencyPool;
import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class DependencyPoolTests extends GenericTestCaseBase {
    public DependencyPoolTests(String name) {
        super(name);
    }

    public void testDependencyPool() throws Exception {
        // always use more threads than cpus, so that the single-cpu case can be tested
        ScheduledExecutorService executor = ExecutionPool.getNewOptimalExecutor(getName());
        DependencyPool<Integer, TestItem, String> pool = new DependencyPool<Integer, TestItem, String>(executor);
        int itemSize = 100, depMax = 5, subMax = 3;
        List<TestItem> items = new ArrayList<TestItem>(itemSize);
        List<TestItem> previousItems = new ArrayList<TestItem>(itemSize);
        for (int i = 0; i < itemSize; i++) {
            int depSize = (int) (Math.random() * Math.min(depMax, itemSize - i - 1));
            List<Integer> deps = new ArrayList<Integer>(depSize);
            for (int j = i + 1, k = 0; j < itemSize && k < depSize; j++) {
                if (Math.random() * (itemSize - j) / (depSize - k + 1) < 1) {
                    deps.add(j);
                    k++;
                }
            }
            int subSize = (int) (Math.random() * Math.min(subMax, i));
            List<TestItem> subItems = new ArrayList<TestItem>(subSize);
OUTER:
            for (int j = 0; j < previousItems.size() && subItems.size() < subSize;) {
                if (Math.random() * j < 1) {
                    TestItem previousItem = previousItems.get(j);
                    for (int k = 0; k < deps.size(); k++) {
                        if (previousItem.getDependencies().contains(deps.get(k))) {
                            j++;
                            continue OUTER;
                        }
                    }
                    subItems.add(previousItem);
                    previousItems.remove(j);
                } else {
                    j++;
                }
            }
            TestItem item = new TestItem(pool, Integer.valueOf(i), Integer.toString(i), deps, subItems);
            items.add(item);
            previousItems.add(item);
        }
        pool.addAll(items);
        pool.start();
        pool.await();
        assertEquals("result count", itemSize, pool.getResultCount());
        for (int i = 0; i < itemSize; i++) {
            TestItem item = items.get(i);
            assertEquals("item(" + i + ") result", Integer.toString(i), pool.getResult(item));
        }
        executor.shutdown();
    }

    private static class TestItem implements DependencyPool.Item<TestItem, Integer, String> {
        private final DependencyPool<Integer, TestItem, String> pool;
        private final Integer key;
        private final String result;
        private final Collection<Integer> dependencies;
        private final Collection<TestItem> subItems;

        protected TestItem(DependencyPool<Integer, TestItem, String> pool, Integer key, String result, Collection<Integer> dependencies, Collection<TestItem> subItems) {
            this.pool = pool;
            this.key = key;
            this.result = result;
            this.dependencies = dependencies;
            this.subItems = subItems;
        }

        public Integer getKey() {
            return key;
        }

        public Collection<Integer> getDependencies() {
            return dependencies;
        }

        public Collection<TestItem> getSubItems() {
            return subItems;
        }

        public String call() throws Exception {
            int sleepTime = (int) (Math.random() * 100);
            Thread.sleep(sleepTime);
            if (!getSubItems().isEmpty()) {
                pool.addAll(getSubItems());
            }
            return result;
        }
    }
}
