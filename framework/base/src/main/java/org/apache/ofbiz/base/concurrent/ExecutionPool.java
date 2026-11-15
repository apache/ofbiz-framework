/*
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
package org.apache.ofbiz.base.concurrent;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.util.Debug;

@SourceMonitored
public final class ExecutionPool {
    private static final String MODULE = ExecutionPool.class.getName();
    public static final ExecutorService GLOBAL_BATCH = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ExecutionPoolThreadFactory(null, "OFBiz-batch"));
    public static final ForkJoinPool GLOBAL_FORK_JOIN = new ForkJoinPool();

    protected static class ExecutionPoolThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final String namePrefix;
        private volatile int count = 1;

        ExecutionPoolThreadFactory(ThreadGroup group, String namePrefix) {
            this.group = group;
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            t.setName(namePrefix + "-" + count++);
            return t;
        }
    }

    public static ScheduledExecutorService getScheduledExecutor(ThreadGroup group, String namePrefix, int threadCount, long keepAliveSeconds,
                                                                boolean preStart) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(threadCount, new ExecutionPoolThreadFactory(group, namePrefix));
        if (keepAliveSeconds > 0) {
            executor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
            executor.allowCoreThreadTimeOut(true);
        }
        if (preStart) {
            executor.prestartAllCoreThreads();
        }
        return executor;
    }

    public static <F> List<F> getAllFutures(Collection<Future<F>> futureList) {
        List<F> result = new LinkedList<>();
        for (Future<F> future: futureList) {
            try {
                result.add(future.get());
            } catch (ExecutionException | InterruptedException e) {
                Debug.logError(e, MODULE);
            }
        }
        return result;
    }
}
