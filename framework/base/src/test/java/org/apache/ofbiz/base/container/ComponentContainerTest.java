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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ComponentContainerTest {
    private static final Path ORDER_CONFIG = Paths.get("applications", "order", "config");
    private static final Path ACCOUNTING_CONFIG = Paths.get("applications", "accounting", "config");
    private static final Path[] CONFIGS = {ORDER_CONFIG, ACCOUNTING_CONFIG};

    private Path ofbizHome = Paths.get(ComponentContainerTest.class.getResource("testsdata").toURI())
            .toAbsolutePath().normalize();

    public ComponentContainerTest() throws URISyntaxException { }

    @Before
    public void setUp() throws IOException {
        cleanUp();
        for (Path cfg : CONFIGS) {
            Files.createDirectory(ofbizHome.resolve(cfg));
        }
    }

    @After
    public void cleanUp() throws IOException {
        for (Path cfg : CONFIGS) {
            Files.deleteIfExists(ofbizHome.resolve(cfg));
        }
    }

    @Test
    public void testCheckDependencyForComponent() throws ContainerException {
        ComponentContainer containerObj = new ComponentContainer();
        containerObj.init("component-container", ofbizHome);

        List<String> loadedComponents = ComponentConfig.components()
                .map(ComponentConfig::getGlobalName)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("order", "accounting"), loadedComponents);
    }
}
