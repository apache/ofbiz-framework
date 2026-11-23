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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.ofbiz.base.util.Debug;

/**
 * The cache invalidation bus of this server, if one is installed. Without a bus nothing is published and every cache
 * behaves as local, so a single server is unchanged. With a bus, the caches declared {@code policy="invalidate"} and the
 * entity caches of delegators using {@code EntityCacheInvalidator} are kept in step; other caches stay local.
 */
public final class CacheInvalidation {

    private static final String MODULE = CacheInvalidation.class.getName();
    private static final String NODE_ID = UUID.randomUUID().toString();
    private static final List<InvalidationHandler> HANDLERS = new CopyOnWriteArrayList<>();
    private static final InvalidationReceiver RECEIVER = new InvalidationReceiver() {
        @Override
        public void onPayload(String payload) {
            receive(payload);
        }

        @Override
        public void onGap() {
            gap();
        }
    };

    private static volatile InvalidationBus bus;
    /**
     * The tombstone length of the installed bus, set before the bus starts so a message received while it starts gets
     * its tombstone too.
     */
    private static volatile long currentTombstoneMillis;

    private CacheInvalidation() { }

    /** The id this server stamps on what it publishes, to ignore its own messages. */
    public static String getNodeId() {
        return NODE_ID;
    }

    /**
     * Starts and installs a bus. Called by the provider plugin when it starts.
     * @throws IllegalStateException if a bus is already installed
     */
    public static synchronized void install(InvalidationBus newBus) {
        Objects.requireNonNull(newBus, "newBus");
        if (bus != null) {
            throw new IllegalStateException("A cache invalidation bus is already installed: " + bus.getName());
        }
        currentTombstoneMillis = newBus.tombstoneMillis();
        try {
            newBus.start(RECEIVER);
        } catch (RuntimeException e) {
            currentTombstoneMillis = 0;
            throw e;
        }
        bus = newBus;
        Debug.logInfo("Cache invalidation bus [" + newBus.getName() + "] installed, node id " + NODE_ID, MODULE);
        UtilCache.warnUndeclaredCaches();
    }

    /** Stops and removes the installed bus, if any. */
    public static synchronized void uninstall() {
        InvalidationBus current = bus;
        if (current != null) {
            bus = null;
            current.stop();
            currentTombstoneMillis = 0;
        }
    }

    /** Whether a bus is installed. */
    public static boolean isActive() {
        return bus != null;
    }

    /** The tombstone length of the installed bus, or 0 without one. */
    static long tombstoneMillis() {
        return currentTombstoneMillis;
    }

    /**
     * Publishes invalidations now, if a bus is installed. A failure is logged and not thrown, since the change itself has
     * happened: the other servers keep the stale entries until the next invalidation of the same data reaches them, a
     * bus gap clears their caches, or the entries expire. Entity caches usually have no expiry, so a failed publish can
     * leave them stale for a long time.
     */
    public static void publish(List<InvalidationMessage> messages) {
        InvalidationBus current = bus;
        if (current == null || messages.isEmpty()) {
            return;
        }
        try {
            current.publish(InvalidationCodec.encode(new InvalidationBatch(NODE_ID, messages)));
        } catch (RuntimeException e) {
            Debug.logError(e, "Could not publish " + messages.size() + " cache invalidation(s) on bus ["
                    + current.getName() + "]", MODULE);
        }
    }

    /** Adds a handler for received invalidations. */
    public static void addHandler(InvalidationHandler handler) {
        HANDLERS.add(Objects.requireNonNull(handler, "handler"));
    }

    /** Removes a handler. */
    public static void removeHandler(InvalidationHandler handler) {
        HANDLERS.remove(handler);
    }

    private static void receive(String payload) {
        InvalidationBatch batch;
        try {
            batch = InvalidationCodec.decode(payload);
        } catch (IllegalArgumentException e) {
            Debug.logWarning("Ignoring an invalid cache invalidation payload: " + e.getMessage(), MODULE);
            return;
        }
        if (NODE_ID.equals(batch.nodeId())) {
            return;
        }
        // The handlers run first: the entity handler bumps entity invalidation versions, which must happen before the caches are
        // cleared, or a load in flight passes its invalidation version check and re-caches a stale row.
        for (InvalidationHandler handler : HANDLERS) {
            try {
                handler.onMessages(batch.messages());
            } catch (RuntimeException e) {
                Debug.logError(e, "A cache invalidation handler failed", MODULE);
            }
        }
        try {
            UtilCache.applyRemote(batch.messages());
        } catch (RuntimeException e) {
            Debug.logError(e, "Could not apply received cache invalidations", MODULE);
        }
    }

    private static void gap() {
        Debug.logWarning("The cache invalidation bus may have missed messages; clearing every cache that is not local",
                MODULE);
        // The handlers run first, for the same reason as in receive: invalidation versions move before the caches are cleared.
        for (InvalidationHandler handler : HANDLERS) {
            try {
                handler.onGap();
            } catch (RuntimeException e) {
                Debug.logError(e, "A cache invalidation handler failed", MODULE);
            }
        }
        try {
            UtilCache.clearAllNonLocalCaches();
        } catch (RuntimeException e) {
            Debug.logError(e, "Could not clear the caches after a bus gap", MODULE);
        }
    }
}
