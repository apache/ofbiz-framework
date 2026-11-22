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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheKey;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.ClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityKey;
import org.junit.jupiter.api.Test;

public class InvalidationCodecTest {

    private static final InvalidationBatch BATCH = new InvalidationBatch("node-a", List.of(
            new CacheKey("widget.screen.locationResource", "component://x"),
            new CacheClear("properties.UtilPropertiesBundleCache"),
            new ClearAll(),
            new EntityKey("default", "TestingType", Map.of("testingTypeId", "T-1")),
            new EntityClear("default", "TestingType"),
            new EntityClearAll("default")));

    @Test
    public void encodeThenDecodeGivesTheSameBatch() {
        assertEquals(BATCH, InvalidationCodec.decode(InvalidationCodec.encode(BATCH)));
    }

    @Test
    public void unknownVersionIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                InvalidationCodec.decode("{\"v\":2,\"node\":\"n\",\"messages\":[]}"));
    }

    @Test
    public void unknownKindIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                InvalidationCodec.decode("{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"java\"}]}"));
    }

    @Test
    public void missingFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode("{\"v\":1,\"messages\":[]}"));
        assertThrows(IllegalArgumentException.class, () ->
                InvalidationCodec.decode("{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"cacheKey\",\"cache\":\"c\"}]}"));
    }

    @Test
    public void nonTextValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(
                "{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"cacheKey\",\"cache\":\"c\",\"key\":{\"a\":1}}]}"));
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(
                "{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"entityKey\",\"delegator\":\"d\",\"entity\":\"E\",\"pk\":{\"id\":1}}]}"));
    }

    @Test
    public void emptyPrimaryKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(
                "{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"entityKey\",\"delegator\":\"d\",\"entity\":\"E\",\"pk\":{}}]}"));
    }

    @Test
    public void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode("{not json"));
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode("[]"));
    }

    @Test
    public void nullAndEmptyPayloadsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(null));
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(""));
    }

    @Test
    public void nullKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(
                "{\"v\":1,\"node\":\"n\",\"messages\":[{\"kind\":\"cacheKey\",\"cache\":\"c\",\"key\":null}]}"));
    }

    @Test
    public void trailingGarbageIsRejected() {
        String valid = InvalidationCodec.encode(BATCH);
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(valid + " {\"v\":1}"));
        assertThrows(IllegalArgumentException.class, () -> InvalidationCodec.decode(valid + "garbage"));
    }

    @Test
    public void sanitizeForLogStripsLineBreaksAndTruncates() {
        assertEquals("a b c", InvalidationCodec.sanitizeForLog("a\rb\nc"));
        assertEquals(103, InvalidationCodec.sanitizeForLog("x".repeat(500)).length());
    }
}
