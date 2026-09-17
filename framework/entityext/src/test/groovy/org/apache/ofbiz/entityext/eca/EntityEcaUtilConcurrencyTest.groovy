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
package org.apache.ofbiz.entityext.eca

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger as Log4jCoreLogger
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.ofbiz.base.util.cache.UtilCache
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for a check-then-act race on EntityEcaUtil#getEntityEcaCache(): concurrent
 * cache misses for the same reader name used to each rebuild independently instead of one
 * winning and the rest reusing it. Clears the real "main" reader's cache entry, then hammers
 * getEntityEcaCache("main") from many threads at once, counting resource-load log lines to
 * detect more than one rebuild happening.
 */
class EntityEcaUtilConcurrencyTest {

    private static final String READER_NAME = 'main'
    private static final int RACER_THREADS = 24
    private static final int ITERATIONS = 10

    private static final class CountingAppender extends AbstractAppender {

        private final AtomicInteger count = new AtomicInteger(0)

        CountingAppender() {
            super('EntityEcaUtilConcurrencyTest-CountingAppender', null, null, false, Property.EMPTY_ARRAY)
        }

        @Override
        void append(LogEvent event) {
            if (event.message.formattedMessage.contains('Entity ECA definitions from')) {
                count.incrementAndGet()
            }
        }

        void reset() {
            count.set(0)
        }

        int getMatchCount() {
            count.get()
        }

    }

    @Test
    // SEPARATE_THREAD so a genuine hang fails at the deadline instead of blocking the build.
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void concurrentCacheMissesDoNotEachRebuildIndependently() {
        Log4jCoreLogger logger = (Log4jCoreLogger) LogManager.getLogger(EntityEcaUtil.name)
        CountingAppender appender = new CountingAppender()
        appender.start()
        logger.addAppender(appender)
        try {
            // Warm-up rebuild: initializes EntityEcaUtil and measures how many resource-load log
            // lines one full rebuild produces here (varies with which plugins are active).
            EntityEcaUtil.getEntityEcaCache(READER_NAME)
            int resourcesPerRebuild = appender.matchCount
            Assertions.assertTrue(resourcesPerRebuild > 0,
                    "Expected at least one entity ECA resource registered for reader \"${READER_NAME}\" in this environment")

            UtilCache<String, ?> ecaReaderCache = UtilCache.findCache('entity.EcaReaders')
            Assertions.assertNotNull(ecaReaderCache, 'entity.EcaReaders cache must already exist (created by EntityEcaUtil\'s static initializer)')

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                ecaReaderCache.remove(READER_NAME)
                appender.reset()

                CountDownLatch startLatch = new CountDownLatch(1)
                AtomicReference<Throwable> failure = new AtomicReference<>()
                ThreadFactory daemonThreadFactory = { Runnable r ->
                    Thread t = Executors.defaultThreadFactory().newThread(r)
                    t.daemon = true
                    t
                } as ThreadFactory
                ExecutorService racers = Executors.newFixedThreadPool(RACER_THREADS, daemonThreadFactory)
                try {
                    List<Future<Map>> futures = (1..RACER_THREADS).collect {
                        racers.submit({
                            startLatch.await()
                            return EntityEcaUtil.getEntityEcaCache(READER_NAME)
                        } as Callable<Map>)
                    }

                    startLatch.countDown()
                    List<Map> results = futures.collect {
                        try {
                            it.get(30, TimeUnit.SECONDS)
                        // Catching Throwable deliberately: any failure surfaced from a racer must fail the test.
                        } catch (Throwable t) { // codenarc-disable CatchThrowable
                            failure.compareAndSet(null, t)
                            null
                        }
                    }

                    if (failure.get() != null) {
                        Assertions.fail("getEntityEcaCache() threw under concurrent load: ${failure.get()}", failure.get())
                    }

                    // Every racer must end up with the exact same published cache instance.
                    Map first = results[0]
                    Assertions.assertNotNull(first, "getEntityEcaCache(\"${READER_NAME}\") returned null")
                    results.each {
                        Assertions.assertSame(first, it,
                                "All concurrent callers of getEntityEcaCache(\"${READER_NAME}\") must receive the same published cache instance")
                    }
                } finally {
                    racers.shutdownNow()
                    racers.awaitTermination(5, TimeUnit.SECONDS)
                }

                int rebuilds = appender.matchCount / resourcesPerRebuild
                Assertions.assertEquals(0, appender.matchCount % resourcesPerRebuild,
                        "Expected a whole number of full rebuilds (multiples of ${resourcesPerRebuild} resources), " +
                                "got ${appender.matchCount} log lines")
                Assertions.assertEquals(1, rebuilds,
                        "Expected exactly 1 rebuild of the \"${READER_NAME}\" entity ECA cache across ${RACER_THREADS} " +
                                "concurrent racers on iteration ${iteration}, but ${rebuilds} independent rebuilds happened " +
                                '(redundant concurrent readConfig() calls - the check-then-act race)')
            }
        } finally {
            logger.removeAppender(appender)
            appender.stop()
        }
    }

}
