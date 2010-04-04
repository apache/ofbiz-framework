package org.ofbiz.base.concurrent.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;

import org.ofbiz.base.concurrent.DependencyPool;
import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.base.util.UtilMisc;

@SourceMonitored
public class DependencyPoolTests extends GenericTestCaseBase {
    public DependencyPoolTests(String name) {
        super(name);
    }

    public void testDependencyPool() throws Exception {
        // always use more threads than cpus, so that the single-cpu case can be tested
        ScheduledExecutorService executor = ExecutionPool.getNewOptimalExecutor(getName());
        DependencyPool pool = new DependencyPool(executor);
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
        private final DependencyPool pool;
        private final Integer key;
        private final String result;
        private final Collection<Integer> dependencies;
        private final Collection<TestItem> subItems;

        protected TestItem(DependencyPool pool, Integer key, String result, Collection<Integer> dependencies, Collection<TestItem> subItems) {
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
            if (!subItems.isEmpty()) {
                pool.addAll(subItems);
            }
            return result;
        }
    }
}
