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

/**
 * Carries cache invalidations between the servers of one OFBiz cluster. A provider plugin implements it and installs
 * it with {@link CacheInvalidation#install(InvalidationBus)} when it starts. Payloads are opaque strings; the
 * framework encodes and decodes them.
 */
public interface InvalidationBus {

    /** A short name for logs, such as {@code redis}. */
    String getName();

    /**
     * Connects the bus. Received payloads, including this server's own, go to {@code receiver}; when the bus
     * (re)connects or detects a missed message it calls {@link InvalidationReceiver#onGap()}.
     */
    void start(InvalidationReceiver receiver);

    /**
     * Sends a payload to every server of the cluster. Called on the thread that changed the data, once for each removal
     * from a distributed cache, so it must not wait for the network: hand the payload to an asynchronous client or a
     * sender thread. Failures are logged by the caller.
     */
    void publish(String payload);

    /**
     * Disconnects the bus. A bus's callbacks (this method, {@link #start(InvalidationReceiver)} and the receiver calls
     * it makes) must not call {@link CacheInvalidation#install(InvalidationBus)} or
     * {@link CacheInvalidation#uninstall()}.
     */
    void stop();

    /**
     * How long, in milliseconds, a received key invalidation keeps a plain {@code put} of that key from caching a
     * value loaded before the invalidation.
     */
    default long tombstoneMillis() {
        return 2000;
    }
}
