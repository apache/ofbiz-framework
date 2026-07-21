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
 * test-cases into Jupiter test classes run through JupiterTestSuite. JUnit 3's EntityTestCase gets
 * these through post-construction setDelegator()/setDispatcher() calls; a Groovy subclass then just
 * writes "delegator"/"dispatcher" bare, with no per-method wiring. Jupiter test classes that
 * {@code @ExtendWith(JupiterTestExtension.class)} get the same ergonomics two ways:
 *
 * <ul>
 * <li>declare a "delegator"/"dispatcher" field (any visibility - a bare Groovy property declaration
 * is enough) and it is set once per test instance via postProcessTestInstance(), same idea as
 * EntityTestCase's setters, no method parameter needed;</li>
 * <li>or declare a Delegator/LocalDispatcher method parameter directly, resolved via
 * resolveParameter() below - useful when a field would be ambiguous, e.g. mixed with
 * {@literal @}ParameterizedTest CSV arguments.</li>
 * </ul>
 *
 * Both are wired from the same suite-scoped values, so either style (or both, on different methods
 * of the same class) can be used interchangeably.
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
