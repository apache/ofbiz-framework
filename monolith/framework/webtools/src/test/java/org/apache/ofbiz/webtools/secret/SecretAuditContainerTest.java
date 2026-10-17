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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.secret.SecretValueResolver;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link SecretAuditContainer} — verifies that {@code start()} wires
 * {@link SecretAuditQueue#INSTANCE} into {@link SecretValueResolver} and that {@code init()}
 * handles absent container configuration gracefully (falls back to delegator name "default").
 *
 * <p>These tests do not exercise the drain thread or database writes; they only verify the
 * registration/deregistration contract and the delegator-name fallback path.</p>
 */
public class SecretAuditContainerTest {

    @After
    public void clearAuditSink() {
        SecretValueResolver.setAuditSink(null);
    }

    @Test
    public void startRegistersSecretAuditQueueAsSink() throws ContainerException {
        SecretValueResolver.setAuditSink(null);
        SecretAuditContainer container = new SecretAuditContainer();
        container.init(List.of(), "secret-audit-container", null);

        container.start();

        assertSame("start() must register SecretAuditQueue.INSTANCE as the active audit sink",
                SecretAuditQueue.INSTANCE, SecretValueResolver.getAuditSink());
    }

    @Test
    public void startSetsNonNullAuditSink() throws ContainerException {
        SecretValueResolver.setAuditSink(null);
        SecretAuditContainer container = new SecretAuditContainer();
        container.init(List.of(), "secret-audit-container", null);

        container.start();

        assertNotNull("start() must leave a non-null audit sink registered with SecretValueResolver",
                SecretValueResolver.getAuditSink());
    }

    @Test
    public void initWithUnknownContainerNameFallsBackToDefault() throws ContainerException {
        // ContainerConfig.getConfiguration() returns null for an unregistered name (test environment).
        // The container must not throw and must still register the sink using the "default" delegator.
        SecretValueResolver.setAuditSink(null);
        SecretAuditContainer container = new SecretAuditContainer();
        container.init(List.of(), "non-existent-container", null);

        container.start();

        assertNotNull("start() must succeed even when ContainerConfig returns null (uses 'default' delegator fallback)",
                SecretValueResolver.getAuditSink());
    }

    @Test
    public void auditSinkIsNullBeforeStart() throws ContainerException {
        SecretValueResolver.setAuditSink(null);
        // Merely calling init() must not register the sink — that belongs to start().
        SecretAuditContainer container = new SecretAuditContainer();
        container.init(List.of(), "secret-audit-container", null);

        assertNull("init() alone must not register the audit sink — that is start()'s responsibility",
                SecretValueResolver.getAuditSink());
    }
}
