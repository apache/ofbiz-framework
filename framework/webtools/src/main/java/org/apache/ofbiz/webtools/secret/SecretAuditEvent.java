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

import java.util.Objects;

/**
 * Immutable value object describing a single secret management audit event.
 *
 * <p>Build via {@link #builder()}. Only {@code action} and {@code outcome} are
 * required; all other fields are nullable and omitted when not applicable to the
 * call site. {@code deploymentMode} is intentionally absent — it is set by
 * {@link SecretAuditLogger} from its own static final so call sites cannot pass
 * a stale or wrong value.</p>
 *
 * <p><strong>Security invariant:</strong> this record never holds a secret value,
 * vault path, or authentication credential. {@code secretKeyRef} is the logical
 * key name only (e.g. {@code jdbc-password.default}).</p>
 */
public record SecretAuditEvent(
        /** Null for scheduler/system jobs that have no login session. */
        String userLoginId,
        /** Null for non-HTTP call sites (scheduler jobs, runSync calls). */
        String clientIpAddress,
        /** Logical key name — e.g. {@code jdbc-password.default}. Never a vault path or ARN. */
        String secretKeyRef,
        /** {@code SYSTEM_PROPERTY | PASSWORDS_FILE | UNKNOWN} */
        String secretTarget,
        /** Required. One of the action values defined in the audit taxonomy (Section 4 of the design). */
        String action,
        /** {@code PROVIDER_CALL | CACHE_HIT | FALLBACK_FILE | ENV_VAR | NOT_APPLICABLE} */
        String accessMode,
        /** Active provider name at call time — from {@code SecretProviderFactory.getProviderName()}. */
        String providerType,
        /** Required. One of the outcome values defined in the audit taxonomy (Section 4 of the design). */
        String outcome,
        /** Null unless {@code outcome=FAILURE}. One of: {@code AUTH_FAILED | NETWORK_TIMEOUT | PROVIDER_ERROR | INVALID_CONFIG} */
        String errorCategory,
        /** Nullable — populated for SYNC / STORE / ROTATION_POLL events against a SystemProperty row. */
        String systemResourceId,
        /** Nullable — populated for SYNC / STORE / ROTATION_POLL events against a SystemProperty row. */
        String systemPropertyId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String userLoginId;
        private String clientIpAddress;
        private String secretKeyRef;
        private String secretTarget;
        private String action;
        private String accessMode = "NOT_APPLICABLE";
        private String providerType;
        private String outcome;
        private String errorCategory;
        private String systemResourceId;
        private String systemPropertyId;

        private Builder() { }

        public Builder userLoginId(String v) {
            userLoginId = v;
            return this;
        }
        public Builder clientIpAddress(String v) {
            clientIpAddress = v;
            return this;
        }
        public Builder secretKeyRef(String v) {
            secretKeyRef = v;
            return this;
        }
        public Builder secretTarget(String v) {
            secretTarget = v;
            return this;
        }
        public Builder action(String v) {
            action = v;
            return this;
        }
        public Builder accessMode(String v) {
            accessMode = v;
            return this;
        }
        public Builder providerType(String v) {
            providerType = v;
            return this;
        }
        public Builder outcome(String v) {
            outcome = v;
            return this;
        }
        public Builder errorCategory(String v) {
            errorCategory = v;
            return this;
        }
        public Builder systemResourceId(String v) {
            systemResourceId = v;
            return this;
        }
        public Builder systemPropertyId(String v) {
            systemPropertyId = v;
            return this;
        }

        public SecretAuditEvent build() {
            Objects.requireNonNull(action, "SecretAuditEvent.action is required");
            Objects.requireNonNull(outcome, "SecretAuditEvent.outcome is required");
            return new SecretAuditEvent(userLoginId, clientIpAddress, secretKeyRef,
                    secretTarget, action, accessMode, providerType,
                    outcome, errorCategory, systemResourceId, systemPropertyId);
        }
    }
}
