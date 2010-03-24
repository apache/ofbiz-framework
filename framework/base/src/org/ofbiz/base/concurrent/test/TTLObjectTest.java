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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.concurrent.TTLObject;
import org.ofbiz.base.lang.SourceMonitor;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitor("Adam Heath")
public abstract class TTLObjectTest extends GenericTestCaseBase {
    static {
        TTLObject.setDefaultTTLForClass(TTLObjectTestTTLObject.class, 100);
    }

    protected final AtomicInteger doneCount = new AtomicInteger();
    protected final AtomicReference<Thread> lastLoadThread = new AtomicReference<Thread>();
    protected final AtomicReference<Thrower> throwException = new AtomicReference<Thrower>();

    protected final TTLObjectTestTTLObject object;
    protected String loadData;
    protected long sleepTime;
    protected ScheduledExecutorService executor;

    protected TTLObjectTest(String name, boolean foreground) {
        super(name);
        object = new TTLObjectTestTTLObject(foreground);
    }

    protected void setUp() throws InterruptedException {
        executor = ExecutionPool.getNewExactExecutor(getName());
    }

    protected void tearDown() throws InterruptedException {
        doneCount.set(0);
        lastLoadThread.set(null);
        throwException.set(null);
        List<Runnable> runnables = executor.shutdownNow();
        assertEquals("no runnables", 0, runnables.size());
    }

    protected Future<Void> schedule(Callable<Void> callable,  long millis) {
        return executor.schedule(callable, millis, TimeUnit.MILLISECONDS);
    }

    protected Future<Void> setObjectDelayed(long millis, final String value) {
        return schedule(new Callable<Void>() {
            public Void call() throws Exception {
                object.set(value);
                return null;
            }
        }, millis);
    }

    protected void assertGetObject(String label, String wantedData, int wantedDoneCount, long minTime, long maxTime) throws Exception {
        long t1 = System.nanoTime();
        assertEquals(label + ": data", wantedData, object.getObject());
        int serial = object.getSerial();
        assertEquals(label + ": doneCount", wantedDoneCount, doneCount.get());
        long t2 = System.nanoTime();
        long time = t2 - t1;
        assertNotSame(label + ": long enough(" + time + " >= " + minTime + ")", time - minTime, Math.abs(time - minTime));
        assertNotSame(label + ": quick enough(" + time + " <= " + maxTime + ")", maxTime - time, Math.abs(maxTime - time));
    }

    public interface Thrower {
        void throwException() throws InterruptedException, Exception;
    }

    protected final class TTLObjectTestTTLObject extends TTLObject<String> {
        private final boolean foreground;

        protected TTLObjectTestTTLObject(boolean foreground) {
            this.foreground = foreground;
        }

        public long getTTL() throws ConfigurationException {
            return super.getTTL();
        }

        protected boolean getForeground() {
            return foreground ? super.getForeground() : false;
        }

        protected String load(String old, int serial) throws Exception {
            lastLoadThread.set(Thread.currentThread());
            try {
                long end = System.nanoTime() + sleepTime * 1000000;
                while (System.nanoTime() <= end) {
                    Thread.sleep(10);
                    if (checkSerial(serial)) break;
                }
                Thrower thrower = throwException.get();
                throwException.compareAndSet(thrower, null);
                if (thrower != null) {
                    thrower.throwException();
                }
            } finally {
                doneCount.incrementAndGet();
            }
            return loadData;
        }

        public void set(String value) throws ObjectException {
            setObject(value);
        }
    }
}
