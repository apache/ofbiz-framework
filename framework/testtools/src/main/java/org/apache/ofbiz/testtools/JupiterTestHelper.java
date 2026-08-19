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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Bare-call helpers ported from OFBizTestCase/EntityTestCase for JUnit 5 Jupiter test classes.
 * Implement this on a {@literal @}JunitJupiterTest test class (Java or
 * Groovy) to get getUserLogin(), from()/select(), getDelegator()/getDispatcher(), and
 * logInfo/logError/logWarning with no field, no method parameter, and no inheritance required.
 * Reads JupiterTestExtension.CURRENT_DELEGATOR/CURRENT_DISPATCHER directly - legal because this
 * interface shares JupiterTestExtension's package (package-private access).
 *
 * <p>Every delegator/dispatcher-backed method here - getUserLogin(), from(), select(),
 * getDelegator(), getDispatcher() - reads CURRENT_DELEGATOR/CURRENT_DISPATCHER, which are only
 * armed on the thread that {@code JupiterTestExtension.JupiterClassRunner.run()} executes on.
 * (logInfo/logError/logWarning read no ThreadLocal at all; they just delegate to
 * {@code Debug.log*} with {@code getClass().getName()}.) Relying on these ThreadLocals is safe
 * under the current, single-threaded, synchronous execution model (the default JUnit Platform
 * mode, and the only one {@code JupiterClassRunner}/{@code TestRunContainer} support). Under that
 * model, a class run
 * outside the container still reaches these methods only via a ThreadLocal that reads back
 * {@code null} - unless {@code JupiterTestExtension}'s {@code evaluateExecutionCondition()} gets a
 * chance to disable it first, reporting a skip instead of letting a null ThreadLocal read through
 * into a confusing NPE. That still happens for a class using bare
 * {@code @ExtendWith(JupiterTestExtension.class)} instead of {@literal @}JunitJupiterTest
 * (no tag, so plain {@code gradlew test} still discovers and then runtime-skips it), and for an
 * IDE-native run that bypasses Gradle's test task entirely (Gradle's excludeTags filter never gets
 * a chance to apply). A {@literal @}JunitJupiterTest class run via plain {@code gradlew test}
 * doesn't even get that far: build.gradle's excludeTags filter excludes it from discovery by its
 * tag before evaluateExecutionCondition() would ever run. That protection does not extend to
 * JUnit 5 parallel execution, which remains unsupported: a test class that opts into it runs on a
 * worker thread where the ThreadLocal is unset even when genuinely inside the container, and these
 * methods will again silently operate on/return {@code null}.
 */
public interface JupiterTestHelper {

    /**
     * Gets delegator. Ported from EntityTestCase's getDelegator() for API symmetry - from()/
     * select()/getUserLogin() above already cover the common cases, so this is only needed when a
     * test calls something that takes a raw Delegator directly (e.g. delegator.makeValue(...)).
     * @return the delegator
     */
    default Delegator getDelegator() {
        return JupiterTestExtension.CURRENT_DELEGATOR.get();
    }

    /**
     * Gets dispatcher, with no field or method parameter declaration needed in the implementing
     * class - call it directly wherever a test needs to invoke a service (e.g.
     * getDispatcher().runSync(...)). JupiterTestExtension's field-injection and
     * ParameterResolver-based method-parameter styles remain available as alternatives (see its
     * javadoc), but are only necessary for classes that don't implement JupiterTestHelper, or for
     * static {@literal @}BeforeAll/{@literal @}AfterAll methods where there is no test instance to
     * call this default method on.
     * @return the dispatcher
     */
    default LocalDispatcher getDispatcher() {
        return JupiterTestExtension.CURRENT_DISPATCHER.get();
    }

    /**
     * Returns this run's caller-supplied test parameters, merged for the test method currently
     * executing on this thread - the exact Map a runTestSuite REST/service call's testParams
     * argument carried in, or an empty map for a plain gradlew test/testIntegration run (which
     * never supplies one) or an API-triggered run with no overrides. Never null, so every call site
     * can write testParams.someKey ?: someDefault (Groovy) without a separate null check on the map
     * itself. Each test decides its own per-field defaulting this way, in the test method itself,
     * rather than through a second lookup layer - see JupiterTestExtension's CURRENT_TEST_PARAMS
     * javadoc for why an intermediate properties-file fallback tier was tried and dropped. Named to
     * match the wire-level runTestSuite service attribute (testParams), not testParameters - one
     * name for the concept everywhere.
     *
     * <p>The caller's testParams map can carry both flat/common keys and nested per-test-method
     * override objects, keyed by the exact test method name (e.g.
     * {@code {"exampleTypeId": "CONTRIVED", "shouldUpdateExample": {"exampleTypeId": "INSPIRED"}}}).
     * This method resolves that down to one flat map for the currently-running method: start from
     * every top-level entry whose value is not itself a Map (nested objects are namespace
     * containers, never a real field value in their own right, so none of them - not just the
     * current method's own - belong in this common base); if the raw map has an entry for the
     * current method name whose value is a Map, merge it on top (its keys win on conflict). A
     * missing or malformed (non-Map-valued) namespaced entry simply falls back to the common base.
     * For a parameterized test method, the current-method name is decorated (e.g.
     * "methodName[exampleTypeId=CONTRIVED]") and so will never match a plain method-name key -
     * namespacing only cleanly targets plain, non-parameterized @Test methods.
     * @return the current run's merged test parameters, never null
     */
    default Map<String, Object> getTestParams() {
        Map<String, Object> params = JupiterTestExtension.CURRENT_TEST_PARAMS.get();
        if (params == null) {
            return Map.of();
        }
        Map<String, Object> commonBase = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                commonBase.put(entry.getKey(), entry.getValue());
            }
        }
        String currentMethodName = JupiterTestExtension.CURRENT_TEST_METHOD_NAME.get();
        if (currentMethodName != null && params.get(currentMethodName) instanceof Map<?, ?> override) {
            for (Map.Entry<?, ?> entry : override.entrySet()) {
                commonBase.put((String) entry.getKey(), entry.getValue());
            }
        }
        return commonBase;
    }

    /**
     * Gets user login.
     * @param userLoginId the user login id
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    default GenericValue getUserLogin(String userLoginId) throws GenericEntityException {
        return EntityTestCase.getUserLogin(getDelegator(), userLoginId);
    }

    /**
     * Gets user login.
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    default GenericValue getUserLogin() throws GenericEntityException {
        return getUserLogin("system");
    }

    /**
     * From entity query.
     * @param entityName the entity name
     * @return the entity query
     */
    default EntityQuery from(String entityName) {
        return EntityTestCase.from(getDelegator(), entityName);
    }

    /**
     * From entity query.
     * @param dynamicViewEntity the dynamic view entity
     * @return the entity query
     */
    default EntityQuery from(DynamicViewEntity dynamicViewEntity) {
        return EntityTestCase.from(getDelegator(), dynamicViewEntity);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    default EntityQuery select(String... fields) {
        return EntityTestCase.select(getDelegator(), fields);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    default EntityQuery select(Set<String> fields) {
        return EntityTestCase.select(getDelegator(), fields);
    }

    /**
     * Log info.
     * @param msg the msg
     */
    default void logInfo(String msg) {
        Debug.logInfo(msg, getClass().getName());
    }

    /**
     * Log error.
     * @param msg the msg
     */
    default void logError(String msg) {
        Debug.logError(msg, getClass().getName());
    }

    /**
     * Log warning.
     * @param msg the msg
     */
    default void logWarning(String msg) {
        Debug.logWarning(msg, getClass().getName());
    }
}
