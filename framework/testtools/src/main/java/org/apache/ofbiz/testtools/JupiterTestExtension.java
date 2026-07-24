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
import org.junit.jupiter.api.extension.ParameterResolutionException;
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
 *
 * <p><b>Not per-test isolation.</b> JUnit 5 creates a fresh test instance per {@literal @}Test
 * method by default, which can suggest each method also gets a fresh Delegator/LocalDispatcher -
 * it doesn't. The Delegator/LocalDispatcher injected here are the single instances
 * ModelTestSuite.prepareTest() builds once for the whole {@code <test-suite>}, shared across every
 * test method and every Jupiter/JUnit 3 class in that suite, exactly as JUnit 3 test-cases already
 * share them today. TestRunContainer rolls back all accumulated mutations once, after the entire
 * suite finishes - not per test method - so a test can observe data created by an earlier test in
 * the same suite, and ordering between test-cases in the suite's testdef XML can matter.
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
        Object value = type == Delegator.class ? CURRENT_DELEGATOR.get() : CURRENT_DISPATCHER.get();
        if (value == null) {
            throw new ParameterResolutionException(unavailableMessage(type.getSimpleName(),
                    "parameter '" + parameterContext.getParameter().getName() + "'"));
        }
        return value;
    }

    private static void injectField(Object testInstance, String fieldName, Class<?> fieldType, Object value) throws IllegalAccessException {
        Field field = null;
        for (Class<?> clz = testInstance.getClass(); clz != null; clz = clz.getSuperclass()) {
            Field candidate = declaredFieldOrNull(clz, fieldName);
            if (candidate != null && fieldType.isAssignableFrom(candidate.getType())) {
                field = candidate;
                break;
            }
        }
        if (field == null) {
            Field mismatch = findAnyFieldOfType(testInstance.getClass(), fieldType);
            if (mismatch != null) {
                throw new IllegalStateException("Field '" + mismatch.getName() + "' in " + testInstance.getClass().getName()
                        + " is of type " + fieldType.getSimpleName() + ", but field injection only recognizes a field "
                        + "named exactly '" + fieldName + "'. Rename it to '" + fieldName + "', or implement "
                        + "JupiterTestHelper instead (type-based, no field name required).");
            }
            return;
        }
        if (value == null) {
            throw new IllegalStateException(unavailableMessage(fieldType.getSimpleName(),
                    "field '" + fieldName + "' of " + testInstance.getClass().getName()));
        }
        field.setAccessible(true);
        field.set(testInstance, value);
    }

    /**
     * Backstop for Concern 3 (name-literal field injection is otherwise silent on a typo): finds any field of the
     * right type regardless of name, so injectField() can fail loudly with the actual field name and the required
     * one, instead of leaving a misnamed field null with no indication injection was ever attempted.
     */
    private static Field findAnyFieldOfType(Class<?> testClass, Class<?> fieldType) {
        for (Class<?> clz = testClass; clz != null; clz = clz.getSuperclass()) {
            for (Field field : clz.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Both injection points (field and parameter) reach here only when the caller has explicitly asked for a
     * Delegator/LocalDispatcher - by declaring the field or parameter - so a null ThreadLocal value here is always a
     * misconfiguration, not a legitimate "test doesn't need it" case. Failing fast at the injection site turns what
     * would otherwise be a mystery NPE deep in test logic into an error that points at the actual cause.
     */
    private static String unavailableMessage(String typeName, String target) {
        return "No " + typeName + " available to inject into " + target + ". JupiterTestExtension's ThreadLocal "
                + "bridge is only populated on the thread that calls JupiterTestSuite.run(), and only for classes "
                + "run through the ofbiz --test container (jupiter-test-suite in a testdef XML). This is null "
                + "because either this class ran outside that container (e.g. plain gradlew test), or JUnit 5 "
                + "parallel execution is enabled for it - both unsupported for delegator/dispatcher injection.";
    }

    private static Field declaredFieldOrNull(Class<?> clz, String fieldName) {
        try {
            return clz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

}
