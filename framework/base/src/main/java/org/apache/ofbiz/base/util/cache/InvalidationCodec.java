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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.CacheKey;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.ClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Writes and reads the wire format of {@link InvalidationBatch}: a small versioned JSON document. Reading is strict;
 * anything unexpected is rejected with an {@link IllegalArgumentException}, and nothing is ever deserialized into
 * Java objects other than strings.
 */
public final class InvalidationCodec {

    /** The wire format version this code writes and reads. */
    public static final int VERSION = 1;

    /** The longest peer-supplied string {@link #sanitizeForLog(String)} keeps. */
    private static final int MAX_LOGGED_LENGTH = 100;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private InvalidationCodec() { }

    /** Encodes a batch as JSON. */
    public static String encode(InvalidationBatch batch) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("v", VERSION);
        root.put("node", batch.nodeId());
        ArrayNode messages = root.putArray("messages");
        for (InvalidationMessage message : batch.messages()) {
            ObjectNode node = messages.addObject();
            switch (message) {
            case CacheKey m -> {
                node.put("kind", "cacheKey");
                node.put("cache", m.cacheName());
                node.put("key", m.key());
            }
            case CacheClear m -> {
                node.put("kind", "cacheClear");
                node.put("cache", m.cacheName());
            }
            case ClearAll _ -> node.put("kind", "clearAll");
            case EntityKey m -> {
                node.put("kind", "entityKey");
                node.put("delegator", m.delegatorName());
                node.put("entity", m.entityName());
                ObjectNode pk = node.putObject("pk");
                m.pkFields().forEach(pk::put);
            }
            case EntityClear m -> {
                node.put("kind", "entityClear");
                node.put("delegator", m.delegatorName());
                node.put("entity", m.entityName());
            }
            case EntityClearAll m -> {
                node.put("kind", "entityClearAll");
                node.put("delegator", m.delegatorName());
            }
            }
        }
        return root.toString();
    }

    /**
     * Decodes a batch.
     * @throws IllegalArgumentException if the payload is not a valid batch of this version
     */
    public static InvalidationBatch decode(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Empty cache invalidation payload");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed cache invalidation payload", e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("A cache invalidation payload must be a JSON object");
        }
        JsonNode version = root.get("v");
        if (version == null || !version.isInt() || version.intValue() != VERSION) {
            throw new IllegalArgumentException("Unsupported cache invalidation payload version: "
                    + sanitizeForLog(String.valueOf(version)));
        }
        String nodeId = text(root, "node");
        JsonNode messages = root.get("messages");
        if (messages == null || !messages.isArray()) {
            throw new IllegalArgumentException("A cache invalidation payload needs a messages array");
        }
        List<InvalidationMessage> result = new ArrayList<>(messages.size());
        for (JsonNode node : messages) {
            result.add(decodeMessage(node));
        }
        return new InvalidationBatch(nodeId, result);
    }

    private static InvalidationMessage decodeMessage(JsonNode node) {
        String kind = text(node, "kind");
        return switch (kind) {
        case "cacheKey" -> new CacheKey(text(node, "cache"), text(node, "key"));
        case "cacheClear" -> new CacheClear(text(node, "cache"));
        case "clearAll" -> new ClearAll();
        case "entityKey" -> new EntityKey(text(node, "delegator"), text(node, "entity"), pkFields(node.get("pk")));
        case "entityClear" -> new EntityClear(text(node, "delegator"), text(node, "entity"));
        case "entityClearAll" -> new EntityClearAll(text(node, "delegator"));
        default -> throw new IllegalArgumentException("Unknown cache invalidation message kind: "
                + sanitizeForLog(kind));
        };
    }

    /**
     * Makes a string received from another server safe to put in a log line: carriage returns and line feeds become
     * spaces and it is cut to 100 characters.
     */
    public static String sanitizeForLog(String peerValue) {
        if (peerValue == null) {
            return "null";
        }
        String value = peerValue.length() > MAX_LOGGED_LENGTH ? peerValue.substring(0, MAX_LOGGED_LENGTH) + "..."
                : peerValue;
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("Missing or non-text field [" + fieldName + "] in a cache invalidation");
        }
        return value.textValue();
    }

    private static Map<String, String> pkFields(JsonNode node) {
        if (node == null || !node.isObject() || node.isEmpty()) {
            throw new IllegalArgumentException("An entity key invalidation needs a non-empty pk object");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : node.properties()) {
            if (!field.getValue().isTextual()) {
                throw new IllegalArgumentException("Non-text value of primary key field ["
                        + sanitizeForLog(field.getKey()) + "]");
            }
            fields.put(field.getKey(), field.getValue().textValue());
        }
        return fields;
    }
}
