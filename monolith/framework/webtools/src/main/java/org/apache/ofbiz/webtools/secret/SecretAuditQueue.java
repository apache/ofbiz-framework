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
package org.apache.ofbiz.webtools.secret;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ofbiz.base.secret.SecretAuditSink;
import org.apache.ofbiz.base.secret.SecretProviderFactory;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;

/**
 * Asynchronous audit event queue for Phase 2 FETCH, CACHE_HIT, and ROTATION_POLL events.
 *
 * <p>Events arrive via the {@link SecretAuditSink} callbacks, which fire on the hot
 * secret-resolution path. They are enqueued with a non-blocking
 * {@link ArrayBlockingQueue#offer} and drained in small batches by a single background
 * writer thread every {@value #DRAIN_INTERVAL_SECONDS} seconds via
 * {@link SecretAuditLogger#logBatch}.</p>
 *
 * <p>On queue overflow, the event is dropped and a counter is incremented. This is
 * acceptable for high-frequency FETCH/CACHE_HIT rows — a steady overflow means the
 * operator should raise {@code secret.audit.queue.capacity} or disable one of the
 * high-volume flags. It is NOT acceptable for Phase 1 admin events, which use the
 * synchronous {@link SecretAuditLogger#log} path.</p>
 *
 * <p>Events are disabled by default via {@code security.properties}:
 * {@code secret.audit.log.fetch.events=false},
 * {@code secret.audit.log.cache.hits=false}. Enabling them requires a restart.</p>
 *
 * <p>Registered with {@link org.apache.ofbiz.base.secret.SecretValueResolver} by
 * {@link SecretAuditContainer} at OFBiz startup after the entity layer is available.</p>
 */
public final class SecretAuditQueue implements SecretAuditSink {

    private static final String MODULE = SecretAuditQueue.class.getName();

    private static final int QUEUE_CAPACITY = 2000;
    private static final int DRAIN_BATCH_SIZE = 50;
    private static final long DRAIN_INTERVAL_SECONDS = 2L;
    private static final long DROP_LOG_INTERVAL = 100L;

    /** Singleton — registered with SecretValueResolver by SecretAuditContainer at startup. */
    public static final SecretAuditQueue INSTANCE = new SecretAuditQueue();

    private final ArrayBlockingQueue<SecretAuditEvent> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong droppedTotal = new AtomicLong();
    private final ScheduledExecutorService writer;

    /** Delegator name used when obtaining a Delegator from DelegatorFactory during drain. */
    private volatile String delegatorName = "default";

    private SecretAuditQueue() {
        writer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecretAuditQueue-Writer");
            t.setDaemon(true);
            return t;
        });
        writer.scheduleWithFixedDelay(this::drainQueue,
                DRAIN_INTERVAL_SECONDS, DRAIN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Called by {@link SecretAuditContainer} to override the default delegator name. */
    void setDelegatorName(String name) {
        this.delegatorName = name;
    }

    /** Package-private — for unit tests only. Returns the current number of queued events. */
    int queueSize() {
        return queue.size();
    }

    /** Package-private — for unit tests only. Discards all queued events without persisting them. */
    void clearForTesting() {
        queue.clear();
    }

    /**
     * Graceful shutdown: stops the background writer and performs a final best-effort
     * drain. Called by {@link SecretAuditContainer#stop()}.
     */
    public void shutdown() {
        writer.shutdown();
        drainQueue();
    }

    @Override
    public void onCacheHit(String key) {
        offer(SecretAuditEvent.builder()
                .action("CACHE_HIT")
                .secretKeyRef(key)
                .accessMode("CACHE_HIT")
                .providerType(SecretProviderFactory.getProviderName())
                .outcome("SUCCESS")
                .build());
    }

    @Override
    public void onFetch(String key, String accessMode, String outcome, String errorCategory) {
        SecretAuditEvent.Builder b = SecretAuditEvent.builder()
                .action("FETCH")
                .secretKeyRef(key)
                .accessMode(accessMode)
                .providerType(SecretProviderFactory.getProviderName())
                .outcome(outcome);
        if (errorCategory != null) {
            b.errorCategory(errorCategory);
        }
        offer(b.build());
    }

    @Override
    public void onRotationPoll(String outcome, String errorCategory) {
        SecretAuditEvent.Builder b = SecretAuditEvent.builder()
                .action("ROTATION_POLL")
                .providerType(SecretProviderFactory.getProviderName())
                .outcome(outcome);
        if (errorCategory != null) {
            b.errorCategory(errorCategory);
        }
        offer(b.build());
    }

    private void offer(SecretAuditEvent event) {
        if (!queue.offer(event)) {
            long dropped = droppedTotal.incrementAndGet();
            if (dropped % DROP_LOG_INTERVAL == 1) {
                Debug.logWarning("SecretAuditQueue: queue full — " + dropped
                        + " event(s) dropped since JVM start (capacity=" + QUEUE_CAPACITY + ")."
                        + " Consider raising secret.audit.queue.capacity or disabling"
                        + " secret.audit.log.cache.hits/fetch.events.", MODULE);
            }
        }
    }

    private void drainQueue() {
        if (queue.isEmpty()) {
            return;
        }
        Delegator delegator = DelegatorFactory.getDelegator(delegatorName);
        if (delegator == null) {
            // Entity layer not ready yet — events remain in the queue for the next cycle.
            return;
        }
        List<SecretAuditEvent> batch = new ArrayList<>(DRAIN_BATCH_SIZE);
        queue.drainTo(batch, DRAIN_BATCH_SIZE);
        if (!batch.isEmpty()) {
            SecretAuditLogger.logBatch(delegator, batch);
        }
    }
}
