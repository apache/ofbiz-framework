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
package org.apache.ofbiz.ws.rs.resources;

import static org.apache.ofbiz.ws.rs.resources.ApiRootResource.joinUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiRootResourceTest {

    @ParameterizedTest
    @CsvSource({
        "base, path",
        "base, /path",
        "base/, path",
        "base/, /path"
    })
    void testJoinUriBaseAndPath(String base, String path) {
        assertEquals("base/path", joinUri(base, path));
    }

    @ParameterizedTest
    @CsvSource({
        "base, /",
        "base/, /"
    })
    void testJoinUriBase(String base, String path) {
        assertEquals("base/", joinUri(base, path));
    }

    @Test
    void testJoinUriPreservePathParams() {
        assertEquals("base/path.{myParam}", joinUri("base", "/path.{myParam}"));
    }

    @Test
    void testJoinUriOneArgument() {
        assertEquals("base", joinUri("base"));
    }

    @Test
    void testJoinUriEmpty() {
        assertEquals("", joinUri());
    }

    // Current known Limitations, change Tests once these change.
    // Ensures that a change to these is a concious decision

    //No full normalization with double '/' -> //
    @Test
    void testJoinUriDoubleSlash() {
        assertEquals("base//path", joinUri("base//", "path"));
    }

    // No fault tolerance for null parts
    @Test
    void testNullPartThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> joinUri("base", null));
    }
}
