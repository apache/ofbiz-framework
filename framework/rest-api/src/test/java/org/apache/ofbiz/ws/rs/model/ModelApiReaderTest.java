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
package org.apache.ofbiz.ws.rs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration-style tests for {@link ModelApiReader}.
 *
 * <p>These tests deliberately parse real, temporary {@code *.rest.xml} files
 * rather than mocking {@code UtilXml}. The behavior under test is the
 * mapping between XML attribute names and {@link ModelApi} /
 * {@link ModelResource} / {@link ModelOperation} fields, which can only be
 * verified meaningfully against a real XML document.</p>
 */
class ModelApiReaderTest {

    @TempDir
    private File tempDir;

    private File writeXml(String content) throws IOException {
        File file = new File(tempDir, "test.rest.xml");
        Files.writeString(file.toPath(), content);
        return file;
    }

    @Test
    void testGetModelApiParsesTopLevelAttributes() throws IOException {
        File file = writeXml("""
                <api displayName="My API" name="myApi" description="desc" path="/api" publish="true"/>
                """);

        ModelApi api = ModelApiReader.getModelApi(file);

        assertEquals("My API", api.getDisplayName());
        assertEquals("myApi", api.getName());
        assertEquals("desc", api.getDescription());
        assertEquals("/api", api.getPath());
        assertTrue(api.isPublish());
    }

    @Test
    void testGetModelApiMissingPublishAttributeDefaultsToFalse() throws IOException {
        // No "publish" attribute at all -> UtilXml.checkEmpty(...) yields "",
        // and Boolean.parseBoolean("") is false. This locks in that implicit
        // default so a change in checkEmpty/parseBoolean behavior is caught.
        File file = writeXml("<api name=\"myApi\"/>");

        ModelApi api = ModelApiReader.getModelApi(file);

        assertFalse(api.isPublish());
    }

    @Test
    void testGetModelAPiParsesSingleResourceAttributes() throws IOException {
        File file = writeXml("""
                <api name="myApi">
                <resource name="users" description="user resource" displayName="Users"
                path="/users" publish="true" auth="true"/>
                </api>
                """);

        ModelApi api = ModelApiReader.getModelApi(file);

        List<ModelResource> resources = api.getResources();
        assertEquals(1, resources.size());

        ModelResource users = resources.get(0);
        assertEquals("users", users.getName());
        assertEquals("user resource", users.getDescription());
        assertEquals("Users", users.getDisplayName());
        assertEquals("/users", users.getPath());
        assertTrue(users.isPublish());
        assertTrue(users.isAuth());
    }

    @Test
    void testGetModelApiParsesNestedResourcesRecursively() throws IOException {
        File file = writeXml("""
                <api name="myApi">
                <resource name="users" path="/users">
                <resource name="orders" path="/orders">
                <resource name="items" path="/items"/>
                </resource>
                </resource>
                </api>
                """);

        ModelApi api = ModelApiReader.getModelApi(file);

        ModelResource users = api.getResources().get(0);
        assertEquals("users", users.getName());
        assertEquals(1, users.getSubResources().size());

        ModelResource orders = users.getSubResources().get(0);
        assertEquals("orders", orders.getName());
        assertEquals(1, orders.getSubResources().size());

        ModelResource items = orders.getSubResources().get(0);
        assertEquals("items", items.getName());
        assertTrue(items.getSubResources().isEmpty());
    }

    @Test
    void testGetModelApiParsesOperationsOnAResource() throws IOException {
        File file = writeXml("""
                <api name="myApi">
                <resource name="orders" path="/orders">
                <operation path="/{id}" verb="GET" produces="application/json"
                consumes="application/json" description="Get an order" auth="true">
                <service name="getOrder"/>
                </operation>
                </resource>
                </api>
                """);

        ModelApi api = ModelApiReader.getModelApi(file);
        ModelResource orders = api.getResources().get(0);

        List<ModelOperation> operations = orders.getOperations();
        assertEquals(1, operations.size());

        ModelOperation getOrder = operations.get(0);
        assertEquals("/{id}", getOrder.getPath());
        assertEquals("GET", getOrder.getVerb());
        assertEquals("application/json", getOrder.getProduces());
        assertEquals("application/json", getOrder.getConsumes());
        assertEquals("Get an order", getOrder.getDescription());
        assertEquals("getOrder", getOrder.getService());
        assertTrue(getOrder.isAuth());
    }

    @Test
    void testGetModelApiThrowsRuntimeExceptionForMalformedXml() throws IOException {
        File file = writeXml("<api name=\"broken\"><unclosed>");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ModelApiReader.getModelApi(file));
        assertTrue(ex.getMessage().contains("Failed to parse REST API definition"));
    }

    @Test
    void getModelApiThrowsForNonExistentFile() {
        File file = new File(tempDir, "does-not-exist.rest.xml");

        assertThrows(RuntimeException.class, () -> ModelApiReader.getModelApi(file));
    }
}
