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

import java.util.Map;
import java.util.Objects;

/**
 * One cache invalidation sent between servers that share a database. Messages carry keys only, never values, and
 * travel as versioned JSON (see {@link InvalidationCodec}).
 */
public sealed interface InvalidationMessage {

    /** Removes one {@code String} key from the cache of that name. */
    record CacheKey(String cacheName, String key) implements InvalidationMessage {
        public CacheKey {
            Objects.requireNonNull(cacheName, "cacheName");
            Objects.requireNonNull(key, "key");
        }
    }

    /** Removes every element of the cache of that name. */
    record CacheClear(String cacheName) implements InvalidationMessage {
        public CacheClear {
            Objects.requireNonNull(cacheName, "cacheName");
        }
    }

    /** Clears every cache that is not declared {@code local}. */
    record ClearAll() implements InvalidationMessage {
    }

    /** Removes one primary key of an entity, with the field values written as strings. */
    record EntityKey(String delegatorName, String entityName, Map<String, String> pkFields) implements InvalidationMessage {
        public EntityKey {
            Objects.requireNonNull(delegatorName, "delegatorName");
            Objects.requireNonNull(entityName, "entityName");
            pkFields = Map.copyOf(pkFields);
        }
    }

    /** Removes everything cached for an entity, for condition-based and bulk changes. */
    record EntityClear(String delegatorName, String entityName) implements InvalidationMessage {
        public EntityClear {
            Objects.requireNonNull(delegatorName, "delegatorName");
            Objects.requireNonNull(entityName, "entityName");
        }
    }

    /** Clears every entity cache of a delegator, for the delegator's clear-all. Other caches are not touched. */
    record EntityClearAll(String delegatorName) implements InvalidationMessage {
        public EntityClearAll {
            Objects.requireNonNull(delegatorName, "delegatorName");
        }
    }
}
