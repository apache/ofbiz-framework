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

package org.apache.ofbiz.entity.testtools;

import java.util.Set;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityQuery;

import junit.framework.TestCase;

public class EntityTestCase extends TestCase {

    private Delegator delegator = null;

    public EntityTestCase(String name) {
        super(name);
    }

    /**
     * Sets delegator.
     * @param delegator the delegator
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        return delegator;
    }

    /**
     * Gets user login. Shared by OFBizTestCase (JUnit 3) and JupiterTestHelper (JUnit 5) so the
     * actual EntityQuery logic exists in exactly one place; each side supplies its own Delegator
     * (instance field vs. ThreadLocal) and keeps its own bare-call wrapper.
     * @param delegator the delegator
     * @param userLoginId the user login id
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    public static GenericValue getUserLogin(Delegator delegator, String userLoginId) throws GenericEntityException {
        return EntityQuery.use(delegator)
                .from("UserLogin")
                .where("userLoginId", userLoginId)
                .queryOne();
    }

    /**
     * From entity query.
     * @param delegator the delegator
     * @param entityName the entity name
     * @return the entity query
     */
    public static EntityQuery from(Delegator delegator, String entityName) {
        return EntityQuery.use(delegator).from(entityName);
    }

    /**
     * From entity query.
     * @param delegator the delegator
     * @param dynamicViewEntity the dynamic view entity
     * @return the entity query
     */
    public static EntityQuery from(Delegator delegator, DynamicViewEntity dynamicViewEntity) {
        return EntityQuery.use(delegator).from(dynamicViewEntity);
    }

    /**
     * Select entity query.
     * @param delegator the delegator
     * @param fields the fields
     * @return the entity query
     */
    public static EntityQuery select(Delegator delegator, String... fields) {
        return EntityQuery.use(delegator).select(fields);
    }

    /**
     * Select entity query.
     * @param delegator the delegator
     * @param fields the fields
     * @return the entity query
     */
    public static EntityQuery select(Delegator delegator, Set<String> fields) {
        return EntityQuery.use(delegator).select(fields);
    }
}
