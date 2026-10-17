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
package org.apache.ofbiz.base.test.uel

import org.apache.ofbiz.base.component.ComponentConfig
import org.apache.ofbiz.base.container.ComponentContainerTestSupport

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UelFunctions.FUNCTION_MAPPER (framework/base/.../util/string/UelFunctions.java) is a static
 * field built exactly once, the first time UelFunctions is touched by any code in the JVM, by
 * scanning ComponentConfig for every component's &lt;uel-mapping&gt; declarations. ComponentConfig
 * is normally populated by ComponentContainer during full OFBiz boot - never triggered by plain
 * gradlew test. ensureUelFunctionsLoaded() runs that one piece standalone (metadata/XML scanning
 * only, no database or web container involved) so UEL-function-based unit tests in this package
 * don't need the ofbiz --test container.
 *
 * <p>Idempotent and safe to call from every UEL test class's own {@literal @}BeforeAll,
 * regardless of which class Gradle happens to run first in this JVM - only the first call
 * actually does anything.
 */
class UelTestSupport {

    private static final AtomicBoolean COMPONENTS_LOADED = new AtomicBoolean(false)

    static void ensureUelFunctionsLoaded() {
        if (!COMPONENTS_LOADED.get()) {
            if (!ComponentConfig.componentExists('base')) {
                Path ofbizHome = Paths.get('').toAbsolutePath().normalize()
                ComponentContainerTestSupport.bootstrapComponents('uel-test-bootstrap', ofbizHome)
                if (!ComponentConfig.componentExists('base')) {
                    throw new IllegalStateException("UEL bootstrap loaded no components from ${ofbizHome} - " +
                            'is the JVM working directory the OFBiz project root?')
                }
            }
            COMPONENTS_LOADED.set(true)
        }
    }

}
