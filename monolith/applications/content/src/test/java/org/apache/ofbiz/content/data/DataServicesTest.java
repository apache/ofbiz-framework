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
package org.apache.ofbiz.content.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the LOCAL_FILE/OFBIZ_FILE allow-list checks in
 * {@link DataServices#createFileMethod} and {@link DataServices#updateFileMethod}.
 * <p>Both methods build a target {@link File} from the caller-supplied {@code objectInfo}
 * and, for CONTEXT_FILE, already confirmed the resolved path stays under the given
 * context root before writing to it. The LOCAL_FILE and OFBIZ_FILE branches did not run
 * the equivalent {@link org.apache.ofbiz.security.SecurityUtil#checkLocalFileAllowList}/
 * {@link org.apache.ofbiz.security.SecurityUtil#checkOfbizFileAllowList} check that the
 * read path ({@link DataResourceWorker#getContentFile}) already performs for the same
 * resource types, so a target outside the configured directories was accepted and written.
 * These tests confirm both write methods now reject such a target before any file is
 * created, for both resource types.
 */
class DataServicesTest {

    private Path ofbizHome;
    private Path outsideDir;
    private String previousOfbizHome;

    @BeforeEach
    void setUpTempDirs() throws IOException {
        ofbizHome = Files.createTempDirectory("ofbiz-home-test");
        outsideDir = Files.createTempDirectory("ofbiz-outside-test");
        previousOfbizHome = System.getProperty("ofbiz.home");
        System.setProperty("ofbiz.home", ofbizHome.toString());
    }

    @AfterEach
    void tearDownTempDirs() throws IOException {
        if (previousOfbizHome != null) {
            System.setProperty("ofbiz.home", previousOfbizHome);
        } else {
            System.clearProperty("ofbiz.home");
        }
        deleteDirRecursively(ofbizHome);
        deleteDirRecursively(outsideDir);
    }

    private static void deleteDirRecursively(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private static DispatchContext mockDispatchContext() {
        DispatchContext dctx = mock(DispatchContext.class);
        when(dctx.getDelegator()).thenReturn(mock(Delegator.class));
        return dctx;
    }

    @Test
    void createFileMethodRejectsLocalFileOutsideAllowedDirectory() throws IOException {
        Path targetFile = outsideDir.resolve("output.txt");
        Map<String, Object> context = new HashMap<>();
        context.put("dataResourceTypeId", "LOCAL_FILE");
        context.put("objectInfo", targetFile.toString());
        context.put("textData", "unexpected content");

        Map<String, Object> result = DataServices.createFileMethod(mockDispatchContext(), context);

        assertTrue(ServiceUtil.isError(result), "A LOCAL_FILE target outside the allowed directories must be rejected");
        assertFalse(Files.exists(targetFile), "No file must be created for a rejected target");
    }

    @Test
    void createFileMethodRejectsOfbizFileOutsideAllowedDirectory() throws IOException {
        Path targetFile = outsideDir.resolve("output.txt");
        // A relative reference that, once resolved against ofbiz.home, lands in a sibling
        // directory outside ofbiz.home entirely -- mirroring how the OFBIZ_FILE branch
        // resolves objectInfo relative to ofbiz.home in production.
        String objectInfo = "../" + outsideDir.getFileName() + "/output.txt";
        Map<String, Object> context = new HashMap<>();
        context.put("dataResourceTypeId", "OFBIZ_FILE");
        context.put("objectInfo", objectInfo);
        context.put("textData", "unexpected content");

        Map<String, Object> result = DataServices.createFileMethod(mockDispatchContext(), context);

        assertTrue(ServiceUtil.isError(result), "An OFBIZ_FILE target resolving outside ofbiz.home must be rejected");
        assertFalse(Files.exists(targetFile), "No file must be created for a rejected target");
    }

    @Test
    void updateFileMethodRejectsLocalFileOutsideAllowedDirectory() throws Exception {
        Path targetFile = outsideDir.resolve("output.txt");
        Map<String, Object> context = new HashMap<>();
        context.put("dataResourceTypeId", "LOCAL_FILE");
        context.put("objectInfo", targetFile.toString());
        context.put("textData", "overwritten content");

        Map<String, Object> result = DataServices.updateFileMethod(mockDispatchContext(), context);

        assertTrue(ServiceUtil.isError(result), "A LOCAL_FILE target outside the allowed directories must be rejected");
        assertFalse(Files.exists(targetFile), "No file must be created for a rejected target");
    }

    @Test
    void updateFileMethodRejectsOfbizFileOutsideAllowedDirectory() throws Exception {
        Path targetFile = outsideDir.resolve("output.txt");
        String objectInfo = "../" + outsideDir.getFileName() + "/output.txt";
        Map<String, Object> context = new HashMap<>();
        context.put("dataResourceTypeId", "OFBIZ_FILE");
        context.put("objectInfo", objectInfo);
        context.put("textData", "overwritten content");

        Map<String, Object> result = DataServices.updateFileMethod(mockDispatchContext(), context);

        assertTrue(ServiceUtil.isError(result), "An OFBIZ_FILE target resolving outside ofbiz.home must be rejected");
        assertFalse(Files.exists(targetFile), "No file must be created for a rejected target");
    }
}
