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
package org.apache.ofbiz.service

import org.apache.ofbiz.base.start.Config
import org.apache.ofbiz.base.start.Start
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.Delegator
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import java.lang.reflect.Constructor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for the ServiceDispatcher#localContext race: register()/deregister() are
 * synchronized, but getLocalContext(), getLocalDispatcher() and containsContext() read the same
 * plain HashMap without locking, so a reader can transiently see a null/corrupted result for a
 * key that both was, and still is, registered.
 *
 * Log levels are disabled around the race: first-time ServiceDispatcher construction under this
 * repo's full multi-module test classpath logs heavily enough (parsing every component's service
 * config) to overwhelm the appender otherwise. Every mock LocalDispatcher shares one Delegator
 * mock so DispatchContext's own internal service-model cache actually hits after the first
 * lookup - without it, every registration re-triggers a full, expensive reactor-wide scan.
 */
class ServiceDispatcherConcurrencyTest {

    private static final int WRITER_THREADS = 8
    private static final int KEYS_PER_WRITER = 500
    private static final int READER_THREADS = 4
    // Backstop only, in case the stopReaders flag is somehow missed.
    private static final int MAX_READER_ITERATIONS = 2_000_000

    private Config originalStartConfig

    @Before
    void stubStartConfigIfMissing() {
        System.setProperty('ofbiz.home', System.getProperty('user.dir'))
        originalStartConfig = Start.getInstance().getConfig()
        // ServiceDispatcher's constructor needs a non-null Start config; stub one only if missing,
        // and always restore it below since it's a JVM-wide singleton.
        if (originalStartConfig == null) {
            Config stubConfig = Mockito.mock(Config)
            Mockito.when(stubConfig.getPortOffset()).thenReturn(0)
            Start.getInstance().setConfig(stubConfig)
        }
    }

    @After
    void restoreStartConfig() {
        Start.getInstance().setConfig(originalStartConfig)
    }

    @Test
    void getLocalContextToleratesConcurrentRegister() {
        boolean infoWasOn = Debug.get(Debug.INFO)
        boolean warningWasOn = Debug.get(Debug.WARNING)
        Debug.set(Debug.INFO, false)
        Debug.set(Debug.WARNING, false)
        try {
            ServiceDispatcher sd = newDispatcher()
            // Shared so every DispatchContext below resolves to the same cache key (see class doc).
            Delegator sharedDelegator = Mockito.mock(Delegator)
            Mockito.when(sharedDelegator.getDelegatorBaseName()).thenReturn('default')
            LocalDispatcher anchorDispatcher = Mockito.mock(LocalDispatcher)
            Mockito.when(anchorDispatcher.getDelegator()).thenReturn(sharedDelegator)
            LocalDispatcher writerDispatcher = Mockito.mock(LocalDispatcher)
            Mockito.when(writerDispatcher.getDelegator()).thenReturn(sharedDelegator)
            String anchorName = "anchorRegressionTestDispatcher${UUID.randomUUID()}"
            DispatchContext anchorContext = new DispatchContext(anchorName, this.class.classLoader, anchorDispatcher)

            // Registered once, up front, and never deregistered - anything reading this exact key
            // should always see it, for the whole life of this test.
            sd.register(anchorContext)

            CountDownLatch startLatch = new CountDownLatch(1)
            AtomicBoolean stopReaders = new AtomicBoolean(false)
            AtomicReference<Throwable> readerFailure = new AtomicReference<>()
            AtomicReference<Throwable> writerFailure = new AtomicReference<>()

            List<Thread> readers = (1..READER_THREADS).collect {
                Thread reader = new Thread({
                    startLatch.await()
                    int iterations = 0
                    while (!stopReaders.get() && iterations++ < MAX_READER_ITERATIONS) {
                        try {
                            if (sd.getLocalContext(anchorName) == null) {
                                throw new AssertionError("getLocalContext(${anchorName}) returned null while the anchor was still registered")
                            }
                            if (sd.getLocalDispatcher(anchorName) == null) {
                                throw new AssertionError("getLocalDispatcher(${anchorName}) returned null while the anchor was still registered")
                            }
                            if (!sd.containsContext(anchorName)) {
                                throw new AssertionError("containsContext(${anchorName}) returned false while the anchor was still registered")
                            }
                        // A corrupted HashMap can throw here too, not just return null.
                        } catch (Throwable t) { // codenarc-disable CatchThrowable
                            readerFailure.compareAndSet(null, t)
                            return
                        }
                    }
                } as Runnable)
                reader.daemon = true
                reader
            }
            readers.each { it.start() }

            ThreadFactory daemonThreadFactory = { Runnable r ->
                Thread t = Executors.defaultThreadFactory().newThread(r)
                t.daemon = true
                t
            } as ThreadFactory
            ExecutorService writers = Executors.newFixedThreadPool(WRITER_THREADS, daemonThreadFactory)
            try {
                List<Future<?>> futures = (1..WRITER_THREADS).collect { int writerIndex ->
                    writers.submit({
                        startLatch.await()
                        for (int j = 0; j < KEYS_PER_WRITER; j++) {
                            try {
                                String key = "raceRegressionTestDispatcher-${writerIndex}-${j}"
                                sd.register(new DispatchContext(key, this.class.classLoader, writerDispatcher))
                            } catch (Throwable t) { // codenarc-disable CatchThrowable
                                writerFailure.compareAndSet(null, t)
                                return
                            }
                        }
                    } as Runnable)
                }

                startLatch.countDown()
                futures.each { it.get(10, TimeUnit.SECONDS) }
            } finally {
                writers.shutdownNow()
                writers.awaitTermination(5, TimeUnit.SECONDS)
                stopReaders.set(true)
                readers.each { it.join(5000) }
            }

            if (writerFailure.get() != null) {
                throw new AssertionError("register() threw under concurrent load: ${writerFailure.get()}", writerFailure.get())
            }
            if (readerFailure.get() != null) {
                throw new AssertionError('getLocalContext()/getLocalDispatcher()/containsContext() failed while racing concurrent register(): '
                        + "${readerFailure.get()}", readerFailure.get())
            }

            Assert.assertTrue('Anchor context should still be registered at the end of the test', sd.containsContext(anchorName))
        } finally {
            Debug.set(Debug.INFO, infoWasOn)
            Debug.set(Debug.WARNING, warningWasOn)
        }
    }

    private static ServiceDispatcher newDispatcher() {
        Constructor<ServiceDispatcher> ctor = ServiceDispatcher.class.getDeclaredConstructor(Delegator.class)
        ctor.setAccessible(true)
        // null delegator: skips DB/JobManager setup, no entity engine or demo data needed.
        // Wrapped in an explicit Object[]: a bare null single argument to a varargs method is
        // ambiguous between "one null argument" and "a null arguments array" and Groovy's two
        // major versions resolve that ambiguity oppositely.
        return ctor.newInstance(new Object[]{null})
    }

}
