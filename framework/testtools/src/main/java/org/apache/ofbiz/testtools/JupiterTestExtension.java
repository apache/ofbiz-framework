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

import java.lang.reflect.Field;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * Injects the per-suite Delegator/LocalDispatcher that ModelTestSuite already builds for JUnit 3
 * test-cases into Jupiter test classes run through JupiterTestSuite.
 *
 * <p>The recommended pattern - the one both reference examples use ({@code ExampleTests} and
 * {@code ExampleJupiterTests} in {@code plugins/example/.../test}) - is to {@code implements
 * JupiterTestHelper} and call its getDelegator()/getDispatcher()/getUserLogin()/from()/select()
 * directly: no field, no method parameter, nothing to declare in the test class at all. That
 * interface's default methods read this extension's CURRENT_DELEGATOR/CURRENT_DISPATCHER
 * ThreadLocals directly (see its javadoc), which is also why it works unchanged inside
 * {@literal @}ParameterizedTest methods - getDispatcher() isn't one of the method's declared
 * parameters, so there's no interaction with {@literal @}CsvSource (or any other argument source)
 * ordering at all. See {@code shouldCreateExampleAcrossTypes} in {@code ExampleJupiterTests},
 * which combines a {@literal @}CsvSource-provided {@code String} with a getDispatcher() call.
 *
 * <p>Two lower-level mechanisms remain available for classes that can't rely on
 * {@code JupiterTestHelper}:
 *
 * <ul>
 * <li>declare a "delegator"/"dispatcher" field (any visibility - a bare Groovy property
 * declaration is enough) and it is set once per test instance via postProcessTestInstance() below,
 * the same idea as JUnit 3's EntityTestCase getting Delegator/LocalDispatcher through
 * post-construction setDelegator()/setDispatcher() calls; or</li>
 * <li>declare a Delegator/LocalDispatcher method parameter directly, resolved via
 * resolveParameter() below.</li>
 * </ul>
 *
 * Both are wired from the same suite-scoped values as JupiterTestHelper, so either can be mixed in
 * on the same class if needed. The method-parameter style is needed rather than merely optional in
 * one case: {@literal @}BeforeAll/{@literal @}AfterAll are static, so they run with no test
 * instance for postProcessTestInstance() to inject a field into, or for a default interface method
 * to be called on; a method parameter, resolved per-invocation, is the only way to reach
 * Delegator/LocalDispatcher there.
 *
 * <p>When a {@literal @}ParameterizedTest method does mix {@literal @}CsvSource-provided arguments
 * with a method parameter resolved by this extension (Delegator/LocalDispatcher) rather than
 * JupiterTestHelper, the CSV-provided parameters must come first in the method signature: JUnit 5
 * fills them left-to-right, then resolves the remaining parameters via registered
 * ParameterResolvers.
 *
 * <p>JupiterTestSuite.run() executes tests synchronously on the calling thread (the default JUnit
 * Platform execution mode, and the one TestRunContainer relies on), so a plain ThreadLocal set
 * immediately before launcher.execute() is read correctly by both hooks below.
 */
public class JupiterTestExtension implements ParameterResolver, TestInstancePostProcessor {

    static final ThreadLocal<Delegator> CURRENT_DELEGATOR = new ThreadLocal<>();
    static final ThreadLocal<LocalDispatcher> CURRENT_DISPATCHER = new ThreadLocal<>();

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
        injectField(testInstance, "delegator", Delegator.class, CURRENT_DELEGATOR.get());
        injectField(testInstance, "dispatcher", LocalDispatcher.class, CURRENT_DISPATCHER.get());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == Delegator.class || type == LocalDispatcher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == Delegator.class ? CURRENT_DELEGATOR.get() : CURRENT_DISPATCHER.get();
    }

    private static void injectField(Object testInstance, String fieldName, Class<?> fieldType, Object value) throws IllegalAccessException {
        if (value == null) {
            return;
        }
        for (Class<?> clz = testInstance.getClass(); clz != null; clz = clz.getSuperclass()) {
            Field field = declaredFieldOrNull(clz, fieldName);
            if (field != null && fieldType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                field.set(testInstance, value);
                return;
            }
        }
    }

    private static Field declaredFieldOrNull(Class<?> clz, String fieldName) {
        try {
            return clz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

}
