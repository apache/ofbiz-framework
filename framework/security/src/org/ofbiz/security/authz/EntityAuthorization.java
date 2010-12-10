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
package org.ofbiz.security.authz;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.cache.Cache;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.authz.da.DynamicAccessFactory;
import org.ofbiz.security.authz.da.DynamicAccessHandler;

public class EntityAuthorization extends AbstractAuthorization {

    private static final String module = EntityAuthorization.class.getName();
    
    protected Delegator delegator; 
    
    @Override
    public List<String> getAutoGrantPermissions(String userId, String permission, Map<String, ? extends Object> context) {
        if (Debug.verboseOn()) Debug.logVerbose("Running getAutoGrantPermissions()", module);
        boolean checking = true;
        String checkString = permission;
        
        while (checking) {
            if (Debug.verboseOn()) Debug.logVerbose("Looking for auto-grant permissions for : " + checkString, module);
            List<String> autoGrant = getPermissionAutoGrant(checkString);
            if (UtilValidate.isNotEmpty(autoGrant)) {
                return autoGrant;
            }
            if (checkString.indexOf(":") > -1) {
                checkString = checkString.substring(0, checkString.lastIndexOf(":"));
            } else {
                checking = false;
            }
        }
        return null;
    }

    @Override
    public boolean hasDynamicPermission(String userId, String permission, Map<String, ? extends Object> context) {
        if (Debug.verboseOn()) Debug.logVerbose("Running hasDynamicPermission()", module);        
        String permissionId = permission;
        boolean checking = true;
        
        // find the dynamic access implementation
        String dynamicAccess = null;
        while (checking) {
            if (Debug.verboseOn()) Debug.logVerbose("Looking for dynamic access for permission -- " + permissionId, module);
            dynamicAccess = getPermissionDynamicAccess(permissionId);
            if (UtilValidate.isEmpty(dynamicAccess)) {
                if (permissionId.indexOf(":") > -1) {
                    permissionId = permissionId.substring(0, permissionId.lastIndexOf(":"));
                } else {
                    Debug.logVerbose("No sections left to check; no dynamic access implementation found", module);
                    checking = false;
                }
            } else {
                if (Debug.verboseOn()) Debug.logVerbose("Dynamic access implementation found : " + dynamicAccess, module);
                checking = false;
            }
        }
        
        // if one exists invoke it
        if (UtilValidate.isNotEmpty(dynamicAccess)) {
            // load the dynamic access handler and invoke it
            if (Debug.verboseOn()) Debug.logVerbose("Loading DynamicAccessHandler for -- " + dynamicAccess, module);
            DynamicAccessHandler dah = DynamicAccessFactory.getDynamicAccessHandler(delegator, dynamicAccess);
            if (dah != null) {
                if (Debug.verboseOn()) Debug.logVerbose("Calling DynamicAccessHandler : " + dah.getClass().getName(), module);
                return dah.handleDynamicAccess(dynamicAccess, userId, permission, context);
            } else {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("No DynamicAccessHandler found for pattern matching -- " + dynamicAccess, module);
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasStaticPermission(String userId, String permission, Map<String, ? extends Object> context) {
        if (Debug.verboseOn()) Debug.logVerbose("Running hasStaticPermission()", module);
        Iterator<GenericValue> iterator = getUserLoginSecurityGroupByUserLoginId(userId);
        GenericValue userLoginSecurityGroup = null;

        while (iterator.hasNext()) {
            userLoginSecurityGroup = iterator.next();
            if (securityGroupHasPermission(userLoginSecurityGroup.getString("groupId"), permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test to see if the specified user has permission
     * 
     * @param session HttpSession used to obtain the userId
     * @param permission the raw permission string
     * @param context name/value pairs used for permission lookup     
     * @return true if the user has permission
     */
    public boolean hasPermission(HttpSession session, String permission, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin != null) {
            return hasPermission(userLogin.getString("userLoginId"), permission, context);
        }
        return false;
    }
    
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }
    
    private Iterator<GenericValue> getUserLoginSecurityGroupByUserLoginId(String userId) {
        List<GenericValue> collection;

        try {
            collection = delegator.findByAnd("UserLoginSecurityGroup", UtilMisc.toMap("userLoginId", userId), null);
        } catch (GenericEntityException e) {
            // make an empty collection to speed up the case where a userLogin belongs to no security groups, only with no exception of course
            collection = FastList.newInstance();
            Debug.logWarning(e, module);
        }
        
        // filter each time after cache retrieval, i.e. cache will contain entire list
        collection = EntityUtil.filterByDate(collection, true);
        return collection.iterator();
    }
    
    private boolean securityGroupHasPermission(String groupId, String permission) {
        GenericValue securityGroupPermissionValue = delegator.makeValue("SecurityGroupPermission",
                UtilMisc.toMap("groupId", groupId, "permissionId", permission));

        try {
            return delegator.findOne(securityGroupPermissionValue.getEntityName(), securityGroupPermissionValue, false) != null;
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return false;
        }
    }    
    
    private List<String> getPermissionAutoGrant(String permission) {
        Cache cache = delegator.getCache();
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toMap("permissionId", permission));
        List<String> autoGrants = cache.get("SecurityPermissionAutoGrant", condition, "EntityAuthorization.autoGrants");
        if (autoGrants == null) {
            autoGrants = FastList.newInstance();
            
            List<GenericValue> values = null;
            try {
                values = delegator.findList("SecurityPermissionAutoGrant", condition, null, null, null, true);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            
            if (UtilValidate.isNotEmpty(values)) {
                for (GenericValue v : values) {
                    autoGrants.add(v.getString("grantPermission"));
                }
            }
            cache.put("SecurityPermissionAutoGrant", condition, "EntityAuthorization.autoGrants", autoGrants);
        }
        return autoGrants;
    }
    
    private String getPermissionDynamicAccess(String perm) {
        GenericValue permission = null;
        try {
            permission = delegator.findOne("SecurityPermission", UtilMisc.toMap("permissionId", perm), true);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (permission != null) {
            return permission.getString("dynamicAccess");
        }
        return null;
    }
}
