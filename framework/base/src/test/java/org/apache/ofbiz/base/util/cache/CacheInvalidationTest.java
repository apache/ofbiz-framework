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

package org.apache.ofbiz.base.util.cache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClear;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class CacheInvalidationTest {

    private RecordingInvalidationBus bus;
    private final List<InvalidationMessage> received = new ArrayList<>();
    private final AtomicInteger gaps = new AtomicInteger();
    private final InvalidationHandler handler = new InvalidationHandler() {
        @Override
        public void onMessages(List<InvalidationMessage> messages) {
            received.addAll(messages);
        }

        @Override
        public void onGap() {
            gaps.incrementAndGet();
        }
    };

    @BeforeEach
    public void installBus() {
        bus = new RecordingInvalidationBus(2000);
        CacheInvalidation.install(bus);
        CacheInvalidation.addHandler(handler);
    }

    @AfterEach
    public void uninstallBus() {
        CacheInvalidation.removeHandler(handler);
        CacheInvalidation.uninstall();
    }

    @Test
    public void publishStampsThisNode() {
        CacheInvalidation.publish(List.of(new EntityClear("default", "TestingType")));
        assertEquals(1, bus.batches().size());
        assertEquals(CacheInvalidation.getNodeId(), bus.batches().get(0).nodeId());
        assertEquals(List.of(new EntityClear("default", "TestingType")), bus.messages());
    }

    @Test
    public void nothingIsPublishedWithoutABus() {
        CacheInvalidation.uninstall();
        assertFalse(CacheInvalidation.isActive());
        CacheInvalidation.publish(List.of(new EntityClear("default", "TestingType")));
        assertTrue(bus.batches().isEmpty());
    }

    @Test
    public void remoteMessagesReachTheHandlers() {
        bus.deliverFrom("other-node", new EntityClear("default", "TestingType"));
        assertEquals(List.of(new EntityClear("default", "TestingType")), received);
    }

    @Test
    public void ownMessagesAreIgnored() {
        bus.deliverFrom(CacheInvalidation.getNodeId(), new EntityClear("default", "TestingType"));
        assertTrue(received.isEmpty());
    }

    @Test
    public void invalidPayloadsAreIgnored() {
        bus.deliverPayload("{\"v\":99}");
        assertTrue(received.isEmpty());
    }

    @Test
    public void gapsReachTheHandlers() {
        bus.reportGap();
        assertEquals(1, gaps.get());
    }

    @Test
    public void onlyOneBusAtATime() {
        assertThrows(IllegalStateException.class, () -> CacheInvalidation.install(new RecordingInvalidationBus(2000)));
        assertTrue(CacheInvalidation.isActive());
    }

    @Test
    public void aFailingHandlerDoesNotStopTheOthers() {
        List<InvalidationMessage> secondReceived = new ArrayList<>();
        InvalidationHandler failing = new InvalidationHandler() {
            @Override
            public void onMessages(List<InvalidationMessage> messages) {
                throw new IllegalStateException("handler failure for the test");
            }

            @Override
            public void onGap() {
                throw new IllegalStateException("handler failure for the test");
            }
        };
        InvalidationHandler second = new InvalidationHandler() {
            @Override
            public void onMessages(List<InvalidationMessage> messages) {
                secondReceived.addAll(messages);
            }

            @Override
            public void onGap() {
            }
        };
        // the failing handler is registered before the second one
        CacheInvalidation.removeHandler(handler);
        CacheInvalidation.addHandler(failing);
        CacheInvalidation.addHandler(second);
        try {
            bus.deliverFrom("other-node", new EntityClear("default", "TestingType"));
            assertEquals(List.of(new EntityClear("default", "TestingType")), secondReceived);
        } finally {
            CacheInvalidation.removeHandler(failing);
            CacheInvalidation.removeHandler(second);
        }
    }

    @Test
    public void aFailingPublishIsLoggedAndDoesNotPropagate() {
        CacheInvalidation.uninstall();
        InvalidationBus failingBus = new InvalidationBus() {
            @Override
            public String getName() {
                return "failing";
            }

            @Override
            public void start(InvalidationReceiver receiver) {
            }

            @Override
            public void publish(String payload) {
                throw new IllegalStateException("publish failure for the test");
            }

            @Override
            public void stop() {
            }
        };
        CacheInvalidation.install(failingBus);
        try {
            assertDoesNotThrow(() -> CacheInvalidation.publish(List.of(new EntityClear("default", "TestingType"))));
        } finally {
            CacheInvalidation.uninstall();
        }
    }
}
