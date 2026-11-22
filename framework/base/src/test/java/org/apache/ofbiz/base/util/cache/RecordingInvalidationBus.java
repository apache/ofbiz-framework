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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** An in-JVM bus for tests: records what is published and delivers messages as if another server sent them. */
public final class RecordingInvalidationBus implements InvalidationBus {

    private final long tombstoneMillis;
    private final List<String> published = new CopyOnWriteArrayList<>();
    private volatile InvalidationReceiver receiver;

    public RecordingInvalidationBus(long tombstoneMillis) {
        this.tombstoneMillis = tombstoneMillis;
    }

    @Override
    public String getName() {
        return "recording";
    }

    @Override
    public void start(InvalidationReceiver newReceiver) {
        this.receiver = newReceiver;
    }

    @Override
    public void publish(String payload) {
        published.add(payload);
    }

    @Override
    public void stop() {
        receiver = null;
    }

    @Override
    public long tombstoneMillis() {
        return tombstoneMillis;
    }

    /** Returns the published batches, decoded. */
    public List<InvalidationBatch> batches() {
        List<InvalidationBatch> batches = new ArrayList<>();
        for (String payload : published) {
            batches.add(InvalidationCodec.decode(payload));
        }
        return batches;
    }

    /** Returns every published message, in order. */
    public List<InvalidationMessage> messages() {
        List<InvalidationMessage> messages = new ArrayList<>();
        for (InvalidationBatch batch : batches()) {
            messages.addAll(batch.messages());
        }
        return messages;
    }

    /** Delivers messages as if the server {@code nodeId} had published them. */
    public void deliverFrom(String nodeId, InvalidationMessage... messages) {
        receiver.onPayload(InvalidationCodec.encode(new InvalidationBatch(nodeId, List.of(messages))));
    }

    /** Delivers a raw payload. */
    public void deliverPayload(String payload) {
        receiver.onPayload(payload);
    }

    /** Reports that messages may have been missed. */
    public void reportGap() {
        receiver.onGap();
    }

    /** Forgets what was published. */
    public void reset() {
        published.clear();
    }
}
