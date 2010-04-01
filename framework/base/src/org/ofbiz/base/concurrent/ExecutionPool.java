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

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public final class ExecutionPool {
    protected static class ExecutionPoolThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int count = 0;

        protected ExecutionPoolThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            t.setName(namePrefix + "-" + count++);
            return t;
        }
    }

    private static class ExecutionPoolFactory {
        protected static ScheduledThreadPoolExecutor getExecutor(String namePrefix, int threadCount) {
            ExecutionPoolThreadFactory threadFactory = new ExecutionPoolThreadFactory(namePrefix);
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threadCount, threadFactory);
            executor.prestartAllCoreThreads();
            return executor;
        }
    }

    public static ThreadFactory createThreadFactory(String namePrefix) {
        return new ExecutionPoolThreadFactory(namePrefix);
    }

    public static ScheduledExecutorService getExecutor(String namePrefix, int threadCount) {
        return ExecutionPoolFactory.getExecutor(namePrefix, threadCount);
    }

    public static ScheduledExecutorService getNewExactExecutor(String namePrefix) {
        return getExecutor(namePrefix, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
    }

    public static ScheduledExecutorService getNewOptimalExecutor(String namePrefix) {
        return getExecutor(namePrefix, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 2);
    }

    public static void addPulse(Pulse pulse) {
        delayQueue.put(pulse);
    }

    public static void removePulse(Pulse pulse) {
        delayQueue.remove(pulse);
    }

    public static void pulseAll(Class<? extends Pulse> match) {
        Iterator<Pulse> it = delayQueue.iterator();
        while (it.hasNext()) {
            Pulse pulse = it.next();
            if (match.isInstance(pulse)) {
                it.remove();
                pulse.run();
            }
        }
    }

    static {
        ExecutionPoolPulseWorker worker = new ExecutionPoolPulseWorker();
        int processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        for (int i = 0; i < processorCount; i++) {
            Thread t = new Thread(worker);
            t.setDaemon(true);
            t.setName("ExecutionPoolPulseWorker(" + i + ")");
            t.start();
        }
    }

    private static final DelayQueue<Pulse> delayQueue = new DelayQueue<Pulse>();

    public static class ExecutionPoolPulseWorker implements Runnable {
        public void run() {
            try {
                while (true) {
                    delayQueue.take().run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static abstract class Pulse implements Delayed, Runnable {
        protected final long expireTimeNanos;
        protected final long loadTimeNanos;

        protected Pulse(long delayNanos) {
            this(System.nanoTime(), delayNanos);
        }

        protected Pulse(long loadTimeNanos, long delayNanos) {
            this.loadTimeNanos = loadTimeNanos;
            expireTimeNanos = loadTimeNanos + delayNanos;
        }

        public long getLoadTimeNanos() {
            return loadTimeNanos;
        }

        public long getExpireTimeNanos() {
            return expireTimeNanos;
        }

        public final long getDelay(TimeUnit unit) {
            return unit.convert(expireTimeNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        public final int compareTo(Delayed other) {
            long r = (expireTimeNanos - ((Pulse) other).expireTimeNanos);
            if (r < 0) return -1;
            if (r > 0) return 1;
            return 0;
        }
    }

}
