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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.ofbiz.base.util.GeneralException;
import org.junit.Test;

/**
 * Tests {@link FileBasedSecretProvider} in isolation using the
 * {@code jdbc-password.h2-ofbiz} entry already present in
 * {@code framework/base/config/passwords.properties}.
 */
public class FileBasedSecretProviderTest {

    private final FileBasedSecretProvider provider = new FileBasedSecretProvider();

    @Test
    public void getSecretReturnsValueForKnownKey() throws GeneralException {
        assertEquals("ofbiz", provider.getSecret("jdbc-password.h2-ofbiz"));
    }

    @Test(expected = GeneralException.class)
    public void getSecretThrowsForUnknownKey() throws GeneralException {
        provider.getSecret("jdbc-password.no-such-key-xyz");
    }

    @Test
    public void getSecretExceptionMentionsKeyName() {
        try {
            provider.getSecret("missing-key-abc");
        } catch (GeneralException e) {
            assertNotNull(e.getMessage());
            assertTrue("Exception should mention the key name", e.getMessage().contains("missing-key-abc"));
            return;
        }
        // If no exception was thrown the test fails here.
        assertTrue("Expected GeneralException for missing key", false);
    }

    @Test
    public void isFallbackEnabledDefaultsToTrue() {
        assertTrue(provider.isFallbackEnabled());
    }

    @Test
    public void closeIsNoOp() {
        provider.close(); // must not throw
    }
}
