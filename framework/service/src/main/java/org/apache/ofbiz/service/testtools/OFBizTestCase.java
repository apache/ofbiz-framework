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

    protected LocalDispatcher dispatcher = null;
    protected String MODULE = this.getClass().getName();

    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    public OFBizTestCase(String name) {
        super(name);
    }

    public void setDispatcher(LocalDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // Retrieves a particular login record.
    protected GenericValue getUserLogin(String userLoginId) throws GenericEntityException {
        return EntityQuery.use(delegator)
                .from("UserLogin")
                .where("userLoginId", userLoginId)
                .queryOne();
    }

    // Retrieves the default login record.
    protected GenericValue getUserLogin() throws GenericEntityException {
        return getUserLogin("system");
    }

    protected EntityQuery from(String entityName) {
        return EntityQuery.use(delegator).from(entityName);
    }

    protected EntityQuery from(DynamicViewEntity dynamicViewEntity) {
        return EntityQuery.use(delegator).from(dynamicViewEntity);
    }

    protected EntityQuery select(String... fields) {
        return EntityQuery.use(delegator).select(fields);
    }

    protected EntityQuery select(Set<String> fields) {
        return EntityQuery.use(delegator).select(fields);
    }

    protected void logInfo(String msg) {
        Debug.logInfo(msg, MODULE);
    }

    protected void logError(String msg) {
        Debug.logError(msg, MODULE);
    }

    protected void logWarning(String msg) {
        Debug.logWarning(msg, MODULE);
    }
}
