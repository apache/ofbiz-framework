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

package org.apache.ofbiz.service.testtools;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Set;

public class OFBizTestCase extends EntityTestCase {

    private LocalDispatcher dispatcher = null;
    private static final String MODULE = OFBizTestCase.class.getName();

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    public OFBizTestCase(String name) {
        super(name);
    }

    /**
     * Sets dispatcher.
     * @param dispatcher the dispatcher
     */
    public void setDispatcher(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Gets user login.
     * @param userLoginId the user login id
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    protected GenericValue getUserLogin(String userLoginId) throws GenericEntityException {
        return EntityQuery.use(getDelegator())
                .from("UserLogin")
                .where("userLoginId", userLoginId)
                .queryOne();
    }

    /**
     * Gets user login.
     * @return the user login
     * @throws GenericEntityException the generic entity exception
     */
    protected GenericValue getUserLogin() throws GenericEntityException {
        return getUserLogin("system");
    }

    /**
     * From entity query.
     * @param entityName the entity name
     * @return the entity query
     */
    protected EntityQuery from(String entityName) {
        return EntityQuery.use(getDelegator()).from(entityName);
    }

    /**
     * From entity query.
     * @param dynamicViewEntity the dynamic view entity
     * @return the entity query
     */
    protected EntityQuery from(DynamicViewEntity dynamicViewEntity) {
        return EntityQuery.use(getDelegator()).from(dynamicViewEntity);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    protected EntityQuery select(String... fields) {
        return EntityQuery.use(getDelegator()).select(fields);
    }

    /**
     * Select entity query.
     * @param fields the fields
     * @return the entity query
     */
    protected EntityQuery select(Set<String> fields) {
        return EntityQuery.use(getDelegator()).select(fields);
    }

    /**
     * Log info.
     * @param msg the msg
     */
    protected void logInfo(String msg) {
        Debug.logInfo(msg, MODULE);
    }

    /**
     * Log error.
     * @param msg the msg
     */
    protected void logError(String msg) {
        Debug.logError(msg, MODULE);
    }

    /**
     * Log warning.
     * @param msg the msg
     */
    protected void logWarning(String msg) {
        Debug.logWarning(msg, MODULE);
    }
}
