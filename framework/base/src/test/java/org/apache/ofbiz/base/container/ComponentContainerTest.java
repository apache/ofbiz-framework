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
package org.apache.ofbiz.base.container;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public final class ComponentContainerTest {
    private static final Path ORDER_CONFIG = Paths.get("applications", "test-order", "config");
    private static final Path ACCOUNTING_CONFIG = Paths.get("applications", "test-accounting", "config");
    private static final Path[] CONFIGS = {ORDER_CONFIG, ACCOUNTING_CONFIG};

    private Path ofbizHome = Paths.get(ComponentContainerTest.class.getResource("testsdata").toURI())
            .toAbsolutePath().normalize();

    public ComponentContainerTest() throws URISyntaxException { }

    @BeforeEach
    public void setUp() throws IOException {
        cleanUp();
        for (Path cfg : CONFIGS) {
            Files.createDirectory(ofbizHome.resolve(cfg));
        }
    }

    @AfterEach
    public void cleanUp() throws IOException {
        for (Path cfg : CONFIGS) {
            Files.deleteIfExists(ofbizHome.resolve(cfg));
        }
    }

    @Test
    public void testCheckDependencyForComponent() throws ContainerException {
        ComponentContainer containerObj = new ComponentContainer();
        containerObj.init("component-container", ofbizHome);

        // ComponentConfig's cache is a single JVM-wide static shared with the rest of the test
        // suite (e.g. the UEL bootstrap session listener also loads the real, ~90-component
        // project into it). Filter down to just this test's own fixture names so the assertion
        // stays a genuine, order-sensitive check of sortDependencies() - accounting depends on
        // order, so a correct topological sort must place test-order before test-accounting even
        // though alphabetical scan order would visit test-accounting first - regardless of
        // whatever else has been loaded into the shared cache by other tests in this JVM.
        List<String> loadedComponents = ComponentConfig.components()
                .map(ComponentConfig::getGlobalName)
                .filter(name -> name.equals("test-order") || name.equals("test-accounting"))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("test-order", "test-accounting"), loadedComponents);
    }
}
