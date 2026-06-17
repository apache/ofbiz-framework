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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests {@link SecretProviderFactory} static accessors.
 *
 * <p>In the test environment no custom {@link SecretProvider} plugin is on the
 * {@link java.util.ServiceLoader} classpath, so the factory always selects
 * {@link FileBasedSecretProvider}.</p>
 */
public class SecretProviderFactoryTest {

    @Test
    public void instanceIsNotNull() {
        assertNotNull(SecretProviderFactory.getInstance());
    }

    @Test
    public void instanceIsSingleton() {
        assertSame(SecretProviderFactory.getInstance(), SecretProviderFactory.getInstance());
    }

    @Test
    public void providerNameIsNotNullOrEmpty() {
        String name = SecretProviderFactory.getProviderName();
        assertNotNull(name);
        assertFalse("Provider name must not be empty", name.isEmpty());
    }

    @Test
    public void providerNameIndicatesFileBasedWhenNoPluginPresent() {
        // In the test environment no vault plugin is on the ServiceLoader classpath,
        // so the factory falls back to FileBasedSecretProvider.
        String name = SecretProviderFactory.getProviderName();
        assertTrue("Expected 'FileBasedSecretProvider' in provider name when no plugin is configured, got: " + name,
                name.contains("FileBasedSecretProvider"));
    }
}
