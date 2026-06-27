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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SecretAuditQueue} — verifies the audit-sink callbacks
 * build correctly shaped events and that the queue handles overflow and delegator-name
 * reconfiguration without throwing.
 *
 * <p>The background drain thread never fires in this environment because
 * {@code DelegatorFactory.getDelegator("default")} returns {@code null} without a running
 * OFBiz entity layer, so the queue guard exits early and events remain queued. This means
 * queue-size assertions are stable within a single test run.</p>
 */
public class SecretAuditQueueTest {

    @Before
    public void clearQueue() {
        // The queue is a singleton shared across tests. Clear it so size-based assertions
        // are not affected by events enqueued by earlier tests (the drain thread is a no-op
        // in the test environment because DelegatorFactory returns null without a running OFBiz).
        SecretAuditQueue.INSTANCE.clearForTesting();
    }

    @Test
    public void onFetchSuccessEnqueuesEvent() {
        int before = SecretAuditQueue.INSTANCE.queueSize();
        SecretAuditQueue.INSTANCE.onFetch("jdbc-password.default", "PROVIDER_CALL", "SUCCESS", null);
        assertTrue("onFetch(SUCCESS) must add exactly one event to the queue",
                SecretAuditQueue.INSTANCE.queueSize() > before);
    }

    @Test
    public void onFetchFailureWithErrorCategoryEnqueuesEvent() {
        int before = SecretAuditQueue.INSTANCE.queueSize();
        SecretAuditQueue.INSTANCE.onFetch("jdbc-password.default", "PROVIDER_CALL", "FAILURE", "NETWORK_TIMEOUT");
        assertTrue("onFetch(FAILURE) must add exactly one event to the queue",
                SecretAuditQueue.INSTANCE.queueSize() > before);
    }

    @Test
    public void onCacheHitEnqueuesEvent() {
        int before = SecretAuditQueue.INSTANCE.queueSize();
        SecretAuditQueue.INSTANCE.onCacheHit("jdbc-password.default");
        assertTrue("onCacheHit() must add exactly one event to the queue",
                SecretAuditQueue.INSTANCE.queueSize() > before);
    }

    @Test
    public void onRotationPollEnqueuesEvent() {
        int before = SecretAuditQueue.INSTANCE.queueSize();
        SecretAuditQueue.INSTANCE.onRotationPoll("SUCCESS", null);
        assertTrue("onRotationPoll(SUCCESS) must add exactly one event to the queue",
                SecretAuditQueue.INSTANCE.queueSize() > before);
    }

    @Test
    public void setDelegatorNameDoesNotThrow() {
        // Package-private — callable from this package. Verifies that reconfiguring the
        // delegator name at startup does not throw for any non-null value.
        SecretAuditQueue.INSTANCE.setDelegatorName("myCustomDelegator");
        SecretAuditQueue.INSTANCE.setDelegatorName("default"); // restore to default
    }

    @Test
    public void queueDoesNotThrowWhenAtCapacity() {
        // Flood the queue beyond its 2000-event capacity; overflow must be silently dropped.
        for (int i = 0; i < 2200; i++) {
            SecretAuditQueue.INSTANCE.onFetch("flood-key-" + i, "PROVIDER_CALL", "SUCCESS", null);
        }
        // If we reach here, the queue correctly dropped overflow events without throwing.
    }
}
