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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.util.GeneralException;
import org.junit.Test;

/**
 * Tests {@link FallbackSecretProvider} retry and fallback behavior.
 *
 * <p>Note: {@link #allRetriesFailedDelegatesToFallback()} exercises the full retry loop and
 * takes ~1.5 s due to exponential backoff (500 ms + 1000 ms with default security.properties).
 * This is intentional — it validates the real production timing.</p>
 */
public class FallbackSecretProviderTest {

    @Test
    public void primarySuccessDoesNotCallFallback() throws GeneralException {
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        SecretProvider primary = fixedProvider("primary-value", true);
        SecretProvider fallback = new SecretProvider() {
            @Override
            public String getSecret(String key) {
                fallbackCalled.set(true);
                return "fallback-value";
            }
        };
        assertEquals("primary-value", new FallbackSecretProvider(primary, fallback).getSecret("k"));
        assertFalse("Fallback must not be called when primary succeeds", fallbackCalled.get());
    }

    @Test(expected = GeneralException.class)
    public void fallbackDisabledRethrowsImmediately() throws GeneralException {
        SecretProvider primary = throwingProvider(new GeneralException("vault-error"), false);
        SecretProvider fallback = fixedProvider("fallback-value", true);
        new FallbackSecretProvider(primary, fallback).getSecret("k");
    }

    @Test
    public void fallbackDisabledDoesNotConsultFallback() throws GeneralException {
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        SecretProvider primary = throwingProvider(new GeneralException("vault-error"), false);
        SecretProvider fallback = new SecretProvider() {
            @Override
            public String getSecret(String key) {
                fallbackCalled.set(true);
                return "fallback-value";
            }
        };
        try {
            new FallbackSecretProvider(primary, fallback).getSecret("k");
        } catch (GeneralException ignored) { }
        assertFalse("Fallback must never be called when isFallbackEnabled=false", fallbackCalled.get());
    }

    @Test
    public void allRetriesFailedDelegatesToFallback() throws GeneralException {
        SecretProvider primary = throwingProvider(new GeneralException("vault-unavailable"), true);
        SecretProvider fallback = fixedProvider("fallback-value", true);
        assertEquals("fallback-value", new FallbackSecretProvider(primary, fallback).getSecret("k"));
    }

    @Test
    public void primaryIsCalledMoreThanOnceBeforeFallback() throws GeneralException {
        AtomicInteger callCount = new AtomicInteger(0);
        SecretProvider primary = new SecretProvider() {
            @Override
            public String getSecret(String key) throws GeneralException {
                callCount.incrementAndGet();
                throw new GeneralException("fail");
            }
            @Override
            public boolean isFallbackEnabled() {
                return true;
            }
        };
        SecretProvider fallback = fixedProvider("fallback-value", true);
        new FallbackSecretProvider(primary, fallback).getSecret("k");
        // With default retry count of 2, primary must be called 3 times (initial + 2 retries).
        assertTrue("Primary must be called more than once before fallback",
                callCount.get() > 1);
    }

    @Test
    public void closeDelegatesToFallbackEvenIfPrimaryThrows() {
        AtomicBoolean fallbackClosed = new AtomicBoolean(false);
        SecretProvider primary = new SecretProvider() {
            @Override
            public String getSecret(String key) {
                return "";
            }
            @Override
            public void close() {
                throw new RuntimeException("primary-close-error");
            }
        };
        SecretProvider fallback = new SecretProvider() {
            @Override
            public String getSecret(String key) {
                return "";
            }
            @Override
            public void close() {
                fallbackClosed.set(true);
            }
        };
        try {
            new FallbackSecretProvider(primary, fallback).close();
        } catch (RuntimeException ignored) { }
        assertTrue("fallback.close() must always be called (try-finally)", fallbackClosed.get());
    }

    // -- helpers --

    private static SecretProvider fixedProvider(String value, boolean fallbackEnabled) {
        return new SecretProvider() {
            @Override
            public String getSecret(String key) {
                return value;
            }
            @Override
            public boolean isFallbackEnabled() {
                return fallbackEnabled;
            }
        };
    }

    private static SecretProvider throwingProvider(GeneralException ex, boolean fallbackEnabled) {
        return new SecretProvider() {
            @Override
            public String getSecret(String key) throws GeneralException {
                throw ex;
            }
            @Override
            public boolean isFallbackEnabled() {
                return fallbackEnabled;
            }
        };
    }
}
