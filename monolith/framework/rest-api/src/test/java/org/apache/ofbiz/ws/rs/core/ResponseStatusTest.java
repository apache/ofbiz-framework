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
package org.apache.ofbiz.ws.rs.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.ofbiz.ws.rs.core.ResponseStatus.Custom;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response.Status.Family;

class ResponseStatusTest {

    @Test
    void testUnprocessableEntityHasCorrectStatusCode() {
        assertEquals(422, Custom.UNPROCESSABLE_ENTITY.getStatusCode());
    }

    @Test
    void testUnprocessableEntityHasCorrectReasonPhrase() {
        assertEquals("Unprocessable Entity", Custom.UNPROCESSABLE_ENTITY.getReasonPhrase());
    }

    @Test
    void testToStringReturnsReasonPhrase() {
        assertEquals("Unprocessable Entity", Custom.UNPROCESSABLE_ENTITY.toString());
    }

    @Test
    void testFamilyIsClientError() {
        // 422 falls in the 4xx range
        assertEquals(Family.CLIENT_ERROR, Custom.UNPROCESSABLE_ENTITY.getFamily());
    }

    @Test
    void testFromStatusCodeReturnsMatchingEnum() {
        Custom result = Custom.fromStatusCode(422);
        assertNotNull(result);
        assertSame(Custom.UNPROCESSABLE_ENTITY, result);
    }

    @Test
    void testFromStatusCodeReturnsNullForUnknownStatusCode() {
        assertNull(Custom.fromStatusCode(999));
    }

    @Test
    void testFromStatusCodeReturnsNullForStandardStatusCode() {
        assertNull(Custom.fromStatusCode(404));
    }
}
