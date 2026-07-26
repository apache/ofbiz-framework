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
package org.apache.ofbiz.testtools;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a Jupiter test class as running only through the ofbiz --test container
 * (testIntegration), excluded from plain gradlew test. Combines
 * {@literal @}Tag(JupiterTestExtension.INTEGRATION_TAG) - read by build.gradle's
 * excludeTags filter, so gradlew test never discovers the class at all - with
 * {@literal @}ExtendWith(JupiterTestExtension.class), which remains as a runtime
 * safety net (see JupiterTestExtension's evaluateExecutionCondition()) for any class
 * that reaches JUnit Platform discovery without this annotation's tag having excluded
 * it first, e.g. a class using {@literal @}ExtendWith(JupiterTestExtension.class)
 * directly instead of this composed annotation.
 *
 * <p>This annotation does not, by itself, register a class with testIntegration -
 * that still requires a {@code <jupiter-test-suite class-name="...">} entry in the
 * component's testdef XML. A class carrying only this annotation, with no matching
 * testdef entry, runs nowhere: excluded from gradlew test by tag, and never picked up
 * by ModelTestSuite for testIntegration either.
 *
 * <p>Running {@code gradlew test --tests} against one of these classes fails the build
 * with "No tests found for given includes" rather than reporting a skip, since the tag
 * excludes it from discovery before Gradle's {@code --tests} filter ever sees it; use
 * {@code gradlew testIntegration} or {@code ofbiz --test} instead. An IDE-native run
 * that bypasses Gradle's test task entirely still reports a clean skip via the runtime
 * condition.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag(JupiterTestExtension.INTEGRATION_TAG)
@ExtendWith(JupiterTestExtension.class)
public @interface JunitJupiterTest {
}
