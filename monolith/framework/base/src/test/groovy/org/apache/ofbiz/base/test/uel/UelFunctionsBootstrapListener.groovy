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

import org.apache.ofbiz.base.util.Debug
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Registered via META-INF/services (see the resource file alongside this class) so JUnit
 * Platform's LauncherFactory discovers and invokes launcherSessionOpened() exactly once, before
 * any test in the JVM is discovered or executed - regardless of which test class Gradle would
 * otherwise run first. This is the structural fix for a class-execution-order dependency that
 * UelTestSupport.ensureUelFunctionsLoaded()'s per-class @BeforeAll wiring alone can't guarantee
 * once dozens of unrelated tests elsewhere in the repo can also touch UelFunctions first in a
 * full, unfiltered gradlew test run.
 *
 * <p>UelTestSupport.ensureUelFunctionsLoaded() is also called directly from each of the 4 UEL
 * test classes' own {@literal @}BeforeAll. Both call sites are intentionally present: this
 * listener is the fast/global path for a normal full-suite run, while the per-class
 * {@literal @}BeforeAll is what retries the bootstrap - and correctly attributes a failure to the
 * right test class - if this listener's own attempt failed.
 */
class UelFunctionsBootstrapListener implements LauncherSessionListener {

    private static final String MODULE = UelFunctionsBootstrapListener.getName()

    @Override
    void launcherSessionOpened(LauncherSession session) {
        try {
            UelTestSupport.ensureUelFunctionsLoaded()
        } catch (Exception e) {
            // Deliberately swallowed here: an exception from this session-wide hook would abort
            // every test in the JVM, not just the ones that need UEL functions. Leaving
            // COMPONENTS_LOADED unset means the affected UEL test classes' own @BeforeAll will
            // retry and surface this same failure, scoped correctly to just those classes.
            Debug.logError(e, 'UEL function bootstrap failed during session startup; ' +
                    'UEL test classes will retry and report the failure', MODULE)
        }
    }

}
