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
package org.apache.ofbiz.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.ofbiz.base.util.GeneralException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SecurityUtilTest {

    private static final List<String> FRAMEWORK_ADMIN_PERMISSIONS = Arrays.asList(
            "SECURITY", "COMMON", "ENTITY_DATA");

    private Path tempHome;
    private Path tempExternal;
    private String previousOfbizHome;

    @Before
    public void setUpTempDirs() throws Exception {
        tempHome = Files.createTempDirectory("ofbiz-home-test");
        tempExternal = Files.createTempDirectory("ofbiz-ext-test");
        previousOfbizHome = System.getProperty("ofbiz.home");
        System.setProperty("ofbiz.home", tempHome.toString());
    }

    @After
    public void tearDownTempDirs() throws Exception {
        if (previousOfbizHome != null) {
            System.setProperty("ofbiz.home", previousOfbizHome);
        } else {
            System.clearProperty("ofbiz.home");
        }
        deleteDirRecursively(tempHome);
        deleteDirRecursively(tempExternal);
    }

    private static void deleteDirRecursively(Path dir) throws Exception {
        if (dir != null && Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void checkOfbizFileAllowListAcceptsFileInAllowedDir() throws Exception {
        Path runtimeDir = Files.createDirectory(tempHome.resolve("runtime"));
        Path file = Files.createTempFile(runtimeDir, "upload", ".dat");
        SecurityUtil.checkOfbizFileAllowList(file.toFile());
    }

    @Test
    public void checkOfbizFileAllowListRejectsFileOutsideAllowedDir() throws Exception {
        Path forbiddenDir = Files.createDirectory(tempHome.resolve("forbidden"));
        Path file = Files.createTempFile(forbiddenDir, "upload", ".dat");
        try {
            SecurityUtil.checkOfbizFileAllowList(file.toFile());
            fail("Expected GeneralException for file outside allowed dirs");
        } catch (GeneralException e) {
            assertTrue(e.getMessage().contains("not within an allowed directory"));
        }
    }

    @Test
    public void checkOfbizFileAllowListAcceptsFileUnderSymlinkedAllowedDir() throws Exception {
        // Simulates an EFS/Docker volume mount: runtime/ is a symlink to an external directory.
        // Prior to the fix, the home-boundary canonical check incorrectly rejected this because
        // the file's canonical path diverged from canonicalHome.
        Path runtimeLink = tempHome.resolve("runtime");
        Files.createSymbolicLink(runtimeLink, tempExternal);
        Path file = Files.createTempFile(tempExternal, "upload", ".dat");
        SecurityUtil.checkOfbizFileAllowList(file.toFile());
    }

    @Test
    public void checkOfbizFileAllowListRejectsPathTraversal() throws Exception {
        Path runtimeDir = Files.createDirectory(tempHome.resolve("runtime"));
        // Construct a path that tries to escape via ".." — getCanonicalPath resolves this.
        File traversalFile = new File(runtimeDir.toFile(), "../../etc/passwd");
        try {
            SecurityUtil.checkOfbizFileAllowList(traversalFile);
            fail("Expected GeneralException for path traversal attempt");
        } catch (GeneralException e) {
            assertTrue(e.getMessage().contains("not within an allowed directory"));
        }
    }


    @Test
    public void basicAdminPermissionTesting() {
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "SECURITY_CREATE1"));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "COMMON_CREATE "));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "COMMON_ADMIN"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "ENTITY_MAINT"));
    }

    @Test
    public void multiLevelAdminPermissionTesting() {
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "SECURITY_PWD_UPDATE"));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "ENTITY_DATA_UPDATE"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "ENTITY_MAINT"));
    }

    @Test
    public void multiLevelBadHierarchyPermissionTesting() {
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "SPECIFIC_MULTI_LEVEL_SECURITY_VIEW"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                FRAMEWORK_ADMIN_PERMISSIONS, "WEBTOOLS_ENTITY_DATA_ADMIN"));
    }

}
