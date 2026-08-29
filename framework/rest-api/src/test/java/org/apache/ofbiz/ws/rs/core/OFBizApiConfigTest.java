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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OFBizApiConfigTest {

    @Test
    void testBuildCleanPathRemovesDuplicateSlashes() throws Exception {
        OFBizApiConfig config = new OFBizApiConfig();

        Method method = OFBizApiConfig.class.getDeclaredMethod(
                "buildCleanPath",
                String[].class);
        method.setAccessible(true);

        String path = (String) method.invoke(
                config,
                (Object) new String[] {
                        "/party/",
                        "/customers/",
                        "/{partyId}/"
                });

        assertEquals("party/customers/{partyId}", path);
    }

    @Test
    void testBuildCleanPathIgnoresNullParts() throws Exception {
        OFBizApiConfig config = new OFBizApiConfig();

        Method method = OFBizApiConfig.class.getDeclaredMethod(
                "buildCleanPath",
                String[].class);
        method.setAccessible(true);

        String path = (String) method.invoke(
                config,
                (Object) new String[] {
                        null,
                        "",
                        " ",
                        "/party/"
                });

        assertEquals("party", path);
    }

    @Test
    void testBuildCleanPathReturnsEmptyStringWhenNoValidParts() throws Exception {
        OFBizApiConfig config = new OFBizApiConfig();

        Method method = OFBizApiConfig.class.getDeclaredMethod(
                "buildCleanPath",
                String[].class);
        method.setAccessible(true);

        String path = (String) method.invoke(
                config,
                (Object) new String[] {
                        "",
                        null,
                        "   "
                });

        assertEquals("", path);
    }

    @Test
    void testBuildCleanPathWithSinglePart() throws Exception {
        OFBizApiConfig config = new OFBizApiConfig();

        Method method = OFBizApiConfig.class.getDeclaredMethod(
                "buildCleanPath",
                String[].class);
        method.setAccessible(true);

        String path = (String) method.invoke(
                config,
                (Object) new String[] {
                        "/services/"
                });

        assertEquals("services", path);
    }
}
