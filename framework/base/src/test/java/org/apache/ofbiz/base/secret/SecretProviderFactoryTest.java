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
 * <p>The factory resolves a {@link SecretProvider} via {@link java.util.ServiceLoader}. In the
 * Gradle multi-module build all plugin JARs are on the test classpath, so the resolved provider
 * may be any registered implementation (e.g. AwsSecretsManagerProvider). Tests here only assert
 * behaviour that holds regardless of which provider is active.</p>
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
    public void providerNameEndsWithProvider() {
        // All SecretProvider implementations follow the convention of ending with "Provider".
        String name = SecretProviderFactory.getProviderName();
        assertTrue("Expected provider name to end with 'Provider', got: " + name,
                name.endsWith("Provider"));
    }
}
