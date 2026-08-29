/*
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
 */
package org.apache.ofbiz.widget.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.ofbiz.base.util.UtilProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public final class WidgetSecureLocationTests {

    @AfterEach
    public void resetAllowFilePaths() {
        UtilProperties.setPropertyValueInMemory("security", "allowFilePaths", "");
    }

    @Test
    public void allowsComponentLocation() {
        assertEquals("component://common/widget/CommonScreens.xml",
                WidgetSecureLocation.sanitize("component://common/widget/CommonScreens.xml"));
    }

    @Test
    public void rejectsTraversalInComponentLocation() {
        assertNull(WidgetSecureLocation.sanitize("component://common/widget/../../../etc/passwd"));
    }

    @Test
    public void rejectsEmptyOrNullLocation() {
        assertNull(WidgetSecureLocation.sanitize(null));
        assertNull(WidgetSecureLocation.sanitize(""));
    }

    @Test
    public void rejectsNonComponentUrl() {
        assertNull(WidgetSecureLocation.sanitize("http://evil.example/widget.xml"));
    }

    // The single-slash `file:` scheme is the exact carrier used in the reported
    // pre-auth RCE chain (file:/dev/fd/N, file:/proc/thread-self/fd/N, ...).
    // It must be rejected at the protocol layer, independent of any configured
    // allowFilePaths pattern, and independent of scheme case.
    @Test
    public void rejectsFileSchemeDescriptorPaths() {
        assertNull(WidgetSecureLocation.sanitize("file:/dev/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("file:/proc/self/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("file:/proc/thread-self/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("file:/proc/%73elf/fd/292"));
    }

    @Test
    public void rejectsFileSchemeRegardlessOfCase() {
        // Java's URL/FlexibleLocation scheme resolution is case-insensitive, so the
        // literal-lowercase check alone (as first proposed in PR #1586) can be bypassed.
        assertNull(WidgetSecureLocation.sanitize("File:/dev/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("FILE:/dev/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("FiLe:/dev/fd/292"));
    }

    @Test
    public void rejectsFileSchemeEvenWhenAllowFilePathsIsPermissive() {
        // Defense-in-depth: an administrator-configured allowFilePaths regex that is
        // too broad must not resurrect the file: scheme bypass.
        UtilProperties.setPropertyValueInMemory("security", "allowFilePaths", ".*");
        assertNull(WidgetSecureLocation.sanitize("file:/dev/fd/292"));
        assertNull(WidgetSecureLocation.sanitize("FILE:/dev/fd/292"));
    }

    @Test
    public void deniesBarePathByDefault() {
        UtilProperties.setPropertyValueInMemory("security", "allowFilePaths", "");
        assertNull(WidgetSecureLocation.sanitize("/opt/ofbiz/templates/foo.ftl"));
    }

    @Test
    public void allowsBarePathOnceConfigured() {
        UtilProperties.setPropertyValueInMemory("security", "allowFilePaths", "/opt/ofbiz/templates/.*");
        assertEquals("/opt/ofbiz/templates/foo.ftl", WidgetSecureLocation.sanitize("/opt/ofbiz/templates/foo.ftl"));
    }
}
