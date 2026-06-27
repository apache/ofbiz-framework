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
package org.apache.ofbiz.base.secret;

/**
 * Receives secret access events from {@link SecretValueResolver} and
 * {@link SecretProviderFactory} for asynchronous persistence.
 *
 * <p><strong>Implementations MUST be non-blocking.</strong> These callbacks fire
 * on the hot path of secret resolution (JDBC password lookups, property reads).
 * Any synchronous I/O here causes measurable throughput regression. Use a bounded
 * in-memory queue and drain it on a background thread.</p>
 *
 * <p>Registered once at application startup via
 * {@link SecretValueResolver#setAuditSink(SecretAuditSink)}. Until a sink is
 * registered, all events are silently ignored.</p>
 *
 * <p>Events are disabled by default via {@code security.properties} to avoid
 * audit overhead before an operator explicitly opts in:
 * {@code secret.audit.log.cache.hits=false},
 * {@code secret.audit.log.fetch.events=false}.</p>
 */
public interface SecretAuditSink {

    /**
     * Called on every CACHE_HIT in {@link SecretValueResolver#resolveKey(String)}.
     * May fire thousands of times per second — implementations must use a non-blocking
     * queue offer and silently drop on overflow.
     *
     * @param key the logical secret key, e.g. {@code jdbc-password.default}
     */
    void onCacheHit(String key);

    /**
     * Called when a secret is fetched from the active provider (cache miss).
     *
     * @param key           the logical secret key
     * @param accessMode    {@code PROVIDER_CALL} or {@code FALLBACK_FILE}
     * @param outcome       {@code SUCCESS}, {@code FAILURE}, or {@code NOT_FOUND}
     * @param errorCategory null unless outcome is {@code FAILURE};
     *                      one of {@code AUTH_FAILED}, {@code NETWORK_TIMEOUT},
     *                      {@code PROVIDER_ERROR}, {@code INVALID_CONFIG}
     */
    void onFetch(String key, String accessMode, String outcome, String errorCategory);

    /**
     * Called on each scheduled rotation-poll cache invalidation.
     *
     * @param outcome       {@code SUCCESS} or {@code FAILURE}
     * @param errorCategory null unless outcome is {@code FAILURE}
     */
    void onRotationPoll(String outcome, String errorCategory);
}
