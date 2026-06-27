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

import java.util.List;

import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * Persists {@link SecretAuditEvent} records to the {@code SecretAuditLog} entity.
 *
 * <h2>Design constraints</h2>
 * <ul>
 *   <li><strong>Never throws</strong> — audit failure must never block the caller. All
 *       exceptions are caught and logged at WARN level.</li>
 *   <li><strong>Independent transaction</strong> — each write suspends the caller's
 *       transaction so the audit row survives a caller rollback. On non-JTA containers
 *       (plain Tomcat) where {@code suspend()} is not supported, the write degrades
 *       gracefully to the caller's transaction rather than being skipped.</li>
 *   <li><strong>Test guard</strong> — skips writes when the delegator base name is
 *       {@code "test"} to avoid polluting audit rows in integration tests.</li>
 *   <li><strong>Deployment mode</strong> — read once at class load from
 *       {@code security.properties}; never re-read per event.</li>
 * </ul>
 *
 * <h2>Phase 2 note</h2>
 * <p>FETCH and CACHE_HIT events must use an async write queue (not these synchronous
 * methods) because they are on the hot path of secret resolution. See the design
 * document for the async queue architecture.</p>
 */
public final class SecretAuditLogger {

    private static final String MODULE = SecretAuditLogger.class.getName();

    /** Read once — deployment mode never changes at runtime; no per-event property lookup. */
    private static final String DEPLOYMENT_MODE =
            UtilProperties.getPropertyValue("security", "secret.audit.deployment.mode", "DIRECT");

    private SecretAuditLogger() { }

    /**
     * Persists a single audit event in its own independent transaction.
     * Use for all Phase 1 admin-action call sites.
     */
    public static void log(Delegator delegator, SecretAuditEvent event) {
        if ("test".equalsIgnoreCase(delegator.getDelegatorBaseName())) {
            return;
        }
        Transaction suspended = null;
        boolean beganNew = false;
        boolean suspendSucceeded = false;
        try {
            try {
                suspended = TransactionUtil.suspend();
                suspendSucceeded = true;
            } catch (GenericTransactionException suspendEx) {
                // Non-JTA environment (e.g. plain Tomcat): suspend() is not supported.
                // Degrade gracefully: write in the caller's transaction rather than skipping.
                Debug.logWarning("SecretAuditLogger: suspend() unsupported, writing in"
                        + " caller's transaction: " + suspendEx.getMessage(), MODULE);
            }
            beganNew = TransactionUtil.begin();
            writeEntry(delegator, event);
            TransactionUtil.commit(beganNew);
        } catch (Exception e) {
            try {
                TransactionUtil.rollback(beganNew, "SecretAuditLogger write failed", e);
            } catch (Exception rollbackEx) {
                // Nested catch required: rollback() itself can throw, breaking the "never throw" contract.
                Debug.logWarning("SecretAuditLogger: rollback failed: "
                        + rollbackEx.getMessage(), MODULE);
            }
            Debug.logWarning("SecretAuditLogger: could not persist audit event: "
                    + e.getMessage(), MODULE);
        } finally {
            if (suspendSucceeded && suspended != null) {
                try {
                    TransactionUtil.resume(suspended);
                } catch (Exception e) {
                    Debug.logWarning("SecretAuditLogger: could not resume caller transaction",
                            MODULE);
                }
            }
        }
    }

    /**
     * Persists a batch of audit events in a single independent transaction.
     *
     * <p>All events share one suspend/begin/commit/resume cycle regardless of list size,
     * avoiding N transaction context-switches for N-key bulk operations.</p>
     *
     * <p><strong>All-or-nothing semantics:</strong> if any {@code writeEntry()} call fails,
     * the entire batch rolls back. This is acceptable for Phase 1 admin-action batches
     * (CSV upload, auto-sync) where the batch is bounded and a retry is possible on the
     * next scheduled run. It is NOT acceptable for Phase 2 FETCH/CACHE_HIT events —
     * use the async write queue for those instead.</p>
     */
    public static void logBatch(Delegator delegator, List<SecretAuditEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if ("test".equalsIgnoreCase(delegator.getDelegatorBaseName())) {
            return;
        }
        Transaction suspended = null;
        boolean beganNew = false;
        boolean suspendSucceeded = false;
        try {
            try {
                suspended = TransactionUtil.suspend();
                suspendSucceeded = true;
            } catch (GenericTransactionException suspendEx) {
                Debug.logWarning("SecretAuditLogger: suspend() unsupported for batch, writing"
                        + " in caller's transaction: " + suspendEx.getMessage(), MODULE);
            }
            beganNew = TransactionUtil.begin();
            for (SecretAuditEvent event : events) {
                writeEntry(delegator, event);
            }
            TransactionUtil.commit(beganNew);
        } catch (Exception e) {
            try {
                TransactionUtil.rollback(beganNew, "SecretAuditLogger batch write failed", e);
            } catch (Exception rollbackEx) {
                Debug.logWarning("SecretAuditLogger: batch rollback failed: "
                        + rollbackEx.getMessage(), MODULE);
            }
            Debug.logWarning("SecretAuditLogger: could not persist " + events.size()
                    + " audit event(s): " + e.getMessage(), MODULE);
        } finally {
            if (suspendSucceeded && suspended != null) {
                try {
                    TransactionUtil.resume(suspended);
                } catch (Exception e) {
                    Debug.logWarning("SecretAuditLogger: could not resume caller transaction"
                            + " after batch", MODULE);
                }
            }
        }
    }

    /**
     * Writes one {@code SecretAuditLog} row using the two-step PK pattern that matches
     * {@code EntityAuditLog} ({@code GenericDelegator.java:2663,2678}):
     * {@code getNextSeqId} then {@code create} — do not use {@code createSetNextSeqId}.
     */
    private static void writeEntry(Delegator delegator, SecretAuditEvent event)
            throws GenericEntityException {
        // Prefer explicitly-passed identity; fall back to the ThreadLocal stack
        // the HTTP layer populates. getCurrentUserIdentifier() is an instance method —
        // not callable on the Delegator interface, so the cast is required.
        String resolvedUser = event.userLoginId() != null
                ? event.userLoginId()
                : ((GenericDelegator) delegator).getCurrentUserIdentifier();

        GenericValue entry = delegator.makeValue("SecretAuditLog");
        entry.set("secretAuditLogId", delegator.getNextSeqId("SecretAuditLog"));
        entry.set("auditTimestamp", UtilDateTime.nowTimestamp());
        entry.set("userLoginId", resolvedUser);
        entry.set("clientIpAddress", event.clientIpAddress());
        entry.set("secretKeyRef", event.secretKeyRef());
        entry.set("secretTarget", event.secretTarget());
        entry.set("action", event.action());
        entry.set("accessMode", event.accessMode());
        entry.set("providerType", event.providerType());
        entry.set("deploymentMode", DEPLOYMENT_MODE);
        entry.set("outcome", event.outcome());
        entry.set("errorCategory", event.errorCategory());
        entry.set("systemResourceId", event.systemResourceId());
        entry.set("systemPropertyId", event.systemPropertyId());
        delegator.create(entry);
    }
}
