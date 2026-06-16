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
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests {@link SecretValueResolver} using the default {@link FileBasedSecretProvider}
 * (resolved via {@link SecretProviderFactory} since no plugin-provided {@link SecretProvider}
 * is on the test classpath) and the {@code jdbc-password.h2-ofbiz} entry already present in
 * {@code framework/base/config/passwords.properties}.
 */
public class SecretValueResolverTest {

    @Test
    public void nonSecretValuesAreReturnedUnchanged() {
        assertEquals("plainvalue", SecretValueResolver.resolve("plainvalue"));
        assertEquals("ENC(AbCd123)", SecretValueResolver.resolve("ENC(AbCd123)"));
        assertEquals("", SecretValueResolver.resolve(""));
    }

    @Test
    public void nullIsReturnedUnchanged() {
        assertNull(SecretValueResolver.resolve(null));
    }

    @Test
    public void resolvesKnownKeyFromPasswordsProperties() {
        assertEquals("ofbiz", SecretValueResolver.resolve("LOOKUP(jdbc-password.h2-ofbiz)"));
        // Cached lookup should return the same value.
        assertEquals("ofbiz", SecretValueResolver.resolve("LOOKUP(jdbc-password.h2-ofbiz)"));
    }

    @Test
    public void unresolvableKeyReturnsEmptyString() {
        assertEquals("", SecretValueResolver.resolve("LOOKUP(jdbc-password.does-not-exist)"));
    }
}
