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
package org.apache.ofbiz.entityext.cache;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.DistributedCacheClear;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entityext.EntityServiceFactory;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Entity Engine Cache Services
 */
public class EntityCacheServices implements DistributedCacheClear {

    private static final String MODULE = EntityCacheServices.class.getName();

    private Delegator delegator = null;
    private LocalDispatcher dispatcher = null;
    private String userLoginId = null;

    public EntityCacheServices() { }

    @Override
    public void setDelegator(Delegator delegator, String userLoginId) {
        this.delegator = delegator;
        this.dispatcher = EntityServiceFactory.getLocalDispatcher(delegator);
        this.userLoginId = userLoginId;
    }

    /**
     * Gets auth user login.
     * @return the auth user login
     */
    public GenericValue getAuthUserLogin() {
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding the userLogin for distributed cache clear", MODULE);
        }
        return userLogin;
    }

    @Override
    public void distributedClearCacheLine(GenericValue value) {
        // Debug.logInfo("running distributedClearCacheLine for value: " + value, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning("No dispatcher is available, somehow the setDelegator (which also creates a dispatcher) was not called,"
                    + " not running distributed cache clear", MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning("The userLogin for distributed cache clear was not found with userLoginId [" + userLoginId
                    + "], not clearing remote caches.", MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync("distributedClearCacheLineByValue", UtilMisc.toMap("value", value, "userLogin", userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running the distributedClearCacheLineByValue service", MODULE);
        }
    }

    @Override
    public void distributedClearCacheLineFlexible(GenericEntity dummyPK) {
        // Debug.logInfo("running distributedClearCacheLineFlexible for dummyPK: " + dummyPK, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning("No dispatcher is available, somehow the setDelegator (which also creates a dispatcher) was not called, "
                    + "not running distributed cache clear", MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning("The userLogin for distributed cache clear was not found with userLoginId [" + userLoginId
                    + "], not clearing remote caches.", MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync("distributedClearCacheLineByDummyPK", UtilMisc.toMap("dummyPK", dummyPK, "userLogin", userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running the distributedClearCacheLineByDummyPK service", MODULE);
        }
    }

    @Override
    public void distributedClearCacheLineByCondition(String entityName, EntityCondition condition) {
        // Debug.logInfo("running distributedClearCacheLineByCondition for (name, condition): " + entityName + ", " + condition + ")", MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning("No dispatcher is available, somehow the setDelegator (which also creates a dispatcher) "
                    + "was not called, not running distributed cache clear", MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning("The userLogin for distributed cache clear was not found with userLoginId [" + userLoginId
                    + "], not clearing remote caches.", MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync("distributedClearCacheLineByCondition", UtilMisc.toMap("entityName", entityName, "condition",
                    condition, "userLogin", userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running the distributedClearCacheLineByCondition service", MODULE);
        }
    }

    @Override
    public void distributedClearCacheLine(GenericPK primaryKey) {
        // Debug.logInfo("running distributedClearCacheLine for primaryKey: " + primaryKey, MODULE);
        if (this.dispatcher == null) {
            Debug.logWarning("No dispatcher is available, somehow the setDelegator (which also creates a dispatcher) was not called, "
                    + "not running distributed cache clear", MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning("The userLogin for distributed cache clear was not found with userLoginId [" + userLoginId
                    + "], not clearing remote caches.", MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync("distributedClearCacheLineByPrimaryKey", UtilMisc.toMap("primaryKey",
                    primaryKey, "userLogin", userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running the distributedClearCacheLineByPrimaryKey service", MODULE);
        }
    }

    @Override
    public void clearAllCaches() {
        if (this.dispatcher == null) {
            Debug.logWarning("No dispatcher is available, somehow the setDelegator (which also creates a dispatcher) "
                    + "was not called, not running distributed clear all caches", MODULE);
            return;
        }

        GenericValue userLogin = getAuthUserLogin();
        if (userLogin == null) {
            Debug.logWarning("The userLogin for distributed cache clear was not found with userLoginId [" + userLoginId
                    + "], not clearing remote caches.", MODULE);
            return;
        }

        try {
            this.dispatcher.runAsync("distributedClearAllEntityCaches", UtilMisc.toMap("userLogin", userLogin), false);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running the distributedClearAllCaches service", MODULE);
        }
    }

    /**
     * Clear All Entity Caches Service
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> clearAllEntityCaches(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Boolean distributeBool = (Boolean) context.get("distribute");
        boolean distribute = false;
        if (distributeBool != null) distribute = distributeBool;

        delegator.clearAllCaches(distribute);

        return ServiceUtil.returnSuccess();
    }

    /**
     * Clear Cache Line Service: one of the following context parameters is required: value, dummyPK or primaryKey
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> clearCacheLine(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Boolean distributeBool = (Boolean) context.get("distribute");
        boolean distribute = false;
        if (distributeBool != null) distribute = distributeBool;

        if (context.containsKey("value")) {
            GenericValue value = (GenericValue) context.get("value");
            if (Debug.infoOn()) {
                Debug.logInfo("Got a clear cache line by value service call; entityName: " + value.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Got a clear cache line by value service call; value: " + value, MODULE);
            }
            delegator.clearCacheLine(value, distribute);
        } else if (context.containsKey("dummyPK")) {
            GenericEntity dummyPK = (GenericEntity) context.get("dummyPK");
            if (Debug.infoOn()) {
                Debug.logInfo("Got a clear cache line by dummyPK service call; entityName: " + dummyPK.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Got a clear cache line by dummyPK service call; dummyPK: " + dummyPK, MODULE);
            }
            delegator.clearCacheLineFlexible(dummyPK, distribute);
        } else if (context.containsKey("primaryKey")) {
            GenericPK primaryKey = (GenericPK) context.get("primaryKey");
            if (Debug.infoOn()) {
                Debug.logInfo("Got a clear cache line by primaryKey service call; entityName: " + primaryKey.getEntityName(), MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Got a clear cache line by primaryKey service call; primaryKey: " + primaryKey, MODULE);
            }
            delegator.clearCacheLine(primaryKey, distribute);
        } else if (context.containsKey("condition")) {
            String entityName = (String) context.get("entityName");
            EntityCondition condition = (EntityCondition) context.get("condition");
            if (Debug.infoOn()) {
                Debug.logInfo("Got a clear cache line by condition service call; entityName: " + entityName, MODULE);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Got a clear cache line by condition service call; condition: " + condition, MODULE);
            }
            delegator.clearCacheLineByCondition(entityName, condition, distribute);
        }
        return ServiceUtil.returnSuccess();
    }
}
