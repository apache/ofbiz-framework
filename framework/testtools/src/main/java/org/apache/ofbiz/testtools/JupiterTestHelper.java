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

import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Bare-call helpers ported from OFBizTestCase/EntityTestCase for JUnit 5 Jupiter test classes.
 * Implement this on a {@literal @}ExtendWith(JupiterTestExtension.class) test class (Java or
 * Groovy) to get getUserLogin(), from()/select(), getDelegator()/getDispatcher(), and
 * logInfo/logError/logWarning with no field, no method parameter, and no inheritance required.
 * Reads JupiterTestExtension.CURRENT_DELEGATOR/CURRENT_DISPATCHER directly - legal because this
 * interface shares JupiterTestExtension's package (package-private access).
 *
 * <p>Every delegator/dispatcher-backed method here - getUserLogin(), from(), select(),
 * getDelegator(), getDispatcher() - reads CURRENT_DELEGATOR/CURRENT_DISPATCHER, which are only
 * armed on the thread that {@code JupiterTestSuite.run()} executes on. (logInfo/logError/
 * logWarning read no ThreadLocal at all; they just delegate to {@code Debug.log*} with
 * {@code getClass().getName()}.) Relying on these ThreadLocals is safe under the current,
 * single-threaded, synchronous execution model (the default JUnit Platform mode, and the only one
 * {@code JupiterTestSuite}/{@code TestRunContainer} support), but if a test class opts into JUnit
 * 5 parallel execution, these methods will run on a worker thread where the ThreadLocal is unset,
 * and will silently operate on/return {@code null} - producing a confusing NPE far from this root
 * cause.
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
     * Gets user login.
     * @param userLoginId the user login id
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    default GenericValue getUserLogin(String userLoginId) throws GenericEntityException {
        return EntityQuery.use(JupiterTestExtension.CURRENT_DELEGATOR.get())
                .from("UserLogin")
                .where("userLoginId", userLoginId)
                .queryOne();
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
        return EntityQuery.use(JupiterTestExtension.CURRENT_DELEGATOR.get()).from(entityName);
    }

    /**
     * From entity query.
     * @param dynamicViewEntity the dynamic view entity
     * @return the entity query
     */
    default EntityQuery from(DynamicViewEntity dynamicViewEntity) {
        return EntityQuery.use(JupiterTestExtension.CURRENT_DELEGATOR.get()).from(dynamicViewEntity);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    default EntityQuery select(String... fields) {
        return EntityQuery.use(JupiterTestExtension.CURRENT_DELEGATOR.get()).select(fields);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    default EntityQuery select(Set<String> fields) {
        return EntityQuery.use(JupiterTestExtension.CURRENT_DELEGATOR.get()).select(fields);
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
