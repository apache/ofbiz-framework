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
package org.apache.ofbiz.service.eca

import org.apache.ofbiz.base.config.GenericConfigException
import org.apache.ofbiz.base.config.ResourceHandler
import org.apache.ofbiz.base.util.UtilXml
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.w3c.dom.Document

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for the ServiceEcaUtil cache race documented in
 * plugins/supporting-docs/2026-08-28-testintegration-runtime-and-eca-race-fix.md
 * and fixed by https://github.com/apache/ofbiz-framework/pull/1805.
 *
 * Before that fix, ServiceEcaUtil#mergeEcaDefinitions() mutated a plain
 * HashMap/LinkedList cache with no synchronization at all between writers
 * (readConfig()/reloadConfig()/addEcaDefinitions(), racing each other every
 * time several ServiceDispatchers are constructed at once) and readers
 * (evalRules(), which runs on essentially every service call and
 * deliberately stays lock-free). A reader iterating a rule list while a
 * writer concurrently did remove()-then-add() on that same LinkedList could
 * observe a corrupted node link and throw a NullPointerException out of
 * LinkedList$ListItr.next() - exactly the crash seen in 8 unrelated
 * testIntegration suites once the artificial ECA startup delay was cut.
 *
 * This drives addEcaDefinitions() (writers) and evalRules() (a reader)
 * against the very same service/event concurrently, using duplicate-equal
 * rules so every write does the racy remove-then-add. Against the pre-fix
 * code this reliably throws; against the fix (CONFIG_LOCK serializing
 * writers, plus ConcurrentHashMap/CopyOnWriteArrayList so the lock-free
 * reader tolerates a concurrent writer) it must not.
 */
class ServiceEcaUtilConcurrencyTest {

    private static final int WRITER_THREADS = 8
    private static final int READER_THREADS = 4
    private static final int MERGES_PER_WRITER = 3000

    @Test
    // SEPARATE_THREAD so a genuine hang (e.g. a corrupted, cyclic rule list spinning
    // evalRules()/mergeEcaDefinitions() forever instead of throwing) still fails this test at
    // the deadline, rather than blocking until someone kills the build.
    //
    // 120s gives generous headroom over CI: all writers serialize on CONFIG_LOCK, so the
    // 24 000 addEcaDefinitions() calls are effectively single-threaded XML-parsing work that
    // took ~12s on an idle 10-core dev machine - comfortably longer on a shared/throttled
    // GitHub Actions runner. This bound exists to catch a genuine hang, not to pace the workload.
    @Timeout(value = 120, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void evalRulesToleratesConcurrentAddEcaDefinitions() {
        String serviceName = "raceRegressionTestService${UUID.randomUUID()}"
        String eventName = 'raceRegressionTestEvent'
        String ruleXml = "<service-eca><eca service=\"${serviceName}\" event=\"${eventName}\"/></service-eca>"

        // Overrides of a Java interface's accessor methods; they can't be turned into Groovy
        // properties without breaking the @Override contract they satisfy.
        ResourceHandler handler = new ResourceHandler() {

            @Override
            String getLoaderName() { return 'main' } // codenarc-disable GetterMethodCouldBeProperty

            @Override
            String getLocation() { return 'ServiceEcaUtilConcurrencyTest-in-memory-secas.xml' } // codenarc-disable GetterMethodCouldBeProperty

            @Override
            Document getDocument() { return UtilXml.readXmlDocument(ruleXml, false) }

            @Override
            InputStream getStream() { return null }

            @Override
            URL getURL() throws GenericConfigException {
                // No backing resource for this in-memory handler; addEcaDefinitions() catches this
                // and falls back to getLocation(), same as it does for any resource without a URL.
                throw new GenericConfigException('no URL for in-memory test resource')
            }

            @Override
            boolean isFileResource() { return false }

            @Override
            String getFullLocation() { return getLocation() } // codenarc-disable GetterMethodCouldBeProperty

        }

        CountDownLatch startLatch = new CountDownLatch(1)
        AtomicBoolean stopReaders = new AtomicBoolean(false)
        AtomicReference<Throwable> readerFailure = new AtomicReference<>()
        AtomicReference<Throwable> writerFailure = new AtomicReference<>()

        List<Thread> readers = (1..READER_THREADS).collect {
            Thread reader = new Thread({
                Map<String, Object> context = [:]
                Map<String, Object> result = [:]
                startLatch.await()
                while (!stopReaders.get()) {
                    try {
                        ServiceEcaUtil.evalRules(serviceName, null, eventName, null, context, result, false, false)
                    // Catching Throwable deliberately: a corrupted list under this exact race can throw
                    // an Error (e.g. StackOverflowError from a cyclic node) as easily as an Exception.
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

        // Daemon threads: if a corrupted collection ever makes a writer spin forever instead of
        // throwing, shutdownNow() below can't force a tight, non-interruptible loop to stop, so
        // non-daemon pool threads would otherwise keep the whole test JVM alive past the timeout.
        ThreadFactory daemonThreadFactory = { Runnable r ->
            Thread t = Executors.defaultThreadFactory().newThread(r)
            t.daemon = true
            t
        } as ThreadFactory
        ExecutorService writers = Executors.newFixedThreadPool(WRITER_THREADS, daemonThreadFactory)
        try {
            List<Future<?>> futures = (1..WRITER_THREADS).collect {
                writers.submit({
                    startLatch.await()
                    for (int j = 0; j < MERGES_PER_WRITER; j++) {
                        try {
                            ServiceEcaUtil.addEcaDefinitions(handler)
                        // See the matching note on the reader's catch above.
                        } catch (Throwable t) { // codenarc-disable CatchThrowable
                            writerFailure.compareAndSet(null, t)
                            return
                        }
                    }
                } as Runnable)
            }

            startLatch.countDown()
            // See the @Timeout comment above for why 90s: this bound only needs to be well clear
            // of the observed ~12s local runtime, with room for slower/shared CI hardware.
            futures.each { it.get(90, TimeUnit.SECONDS) }
        } finally {
            writers.shutdownNow()
            writers.awaitTermination(5, TimeUnit.SECONDS)
            stopReaders.set(true)
            readers.each { it.join(5000) }
        }

        if (writerFailure.get() != null) {
            Assertions.fail("addEcaDefinitions() threw under concurrent load: ${writerFailure.get()}", writerFailure.get())
        }
        if (readerFailure.get() != null) {
            Assertions.fail("evalRules() threw while racing concurrent addEcaDefinitions(): ${readerFailure.get()}", readerFailure.get())
        }

        // All WRITER_THREADS * MERGES_PER_WRITER merges loaded content-equal rules for the same
        // service/event, so they should have collapsed via the remove-old/add-new dedup path down
        // to exactly one rule - not accumulated duplicates and not lost entirely.
        List<ServiceEcaRule> finalRules = ServiceEcaUtil.getServiceEventRules(serviceName, eventName)
        Assertions.assertEquals(1, finalRules?.size(),
                "Expected exactly one deduplicated rule for ${serviceName}:${eventName}, got: ${finalRules}")
    }

}
