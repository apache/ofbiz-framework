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
package org.apache.ofbiz.ws.rs.openapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OFBizResourceScannerTest {

    private OFBizResourceScanner scanner;

    @BeforeEach
    private void setUp() {
        scanner = new OFBizResourceScanner();
    }

    /**
     * Verifies that a {@code null} input is treated as ignored,
     * consistent with {@code UtilValidate.isEmpty} returning {@code true} for null.
     */
    @Test
    void testIsIgnoredNull() {
        assertTrue(scanner.isIgnored(null));
    }

    /**
     * Verifies that an empty string is treated as ignored.
     */
    @Test
    void testIsIgnoredEmptyString() {
        assertTrue(scanner.isIgnored(""));
    }

    /**
     * Verifies that a class name not in {@code IGNORED} and not starting with
     * any ignored entry is not ignored.
     */
    @Test
    void testIsIgnoredNonMatchingClass() {
        assertFalse(scanner.isIgnored("org.apache.ofbiz.ws.rs.resources.OFBizOpenApiReader"));
    }

    /**
     * Verifies that a partial match that is not a prefix is not ignored.
     * The ignored entry must match from the start of the string.
     */
    @Test
    void testIsIgnoredPartialNonPrefixMatch() {
        assertFalse(scanner.isIgnored("com.example.OFBizServiceResource"));
    }

    /**
     * Verifies that a completely unrelated class name is not ignored.
     */
    @Test
    void testIsIgnoredUnrelatedClass() {
        assertFalse(scanner.isIgnored("com.example.SomeOtherResource"));
    }
}
