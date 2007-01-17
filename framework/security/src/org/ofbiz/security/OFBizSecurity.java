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
package org.ofbiz.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * <code>OFBizSecurity</code>
 * This class has not been altered from the original source. It now just extends Security and was therefore renamed to
 * OFBizSecurity.
 */
public class OFBizSecurity extends org.ofbiz.security.Security {
    
    public static final String module = OFBizSecurity.class.getName();
    
    public static final Map simpleRoleEntity = UtilMisc.toMap(
        "ORDERMGR", UtilMisc.toMap("name", "OrderRole", "pkey", "orderId"),
        "FACILITY", UtilMisc.toMap("name", "FacilityRole", "pkey", "facilityId"),
        "MARKETING", UtilMisc.toMap("name", "MarketingCampaignRole", "pkey", "marketingCampaignId"));    

    GenericDelegator delegator = null;

    protected OFBizSecurity() {}

    protected OFBizSecurity(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    public GenericDelegator getDelegator() {
        return delegator;
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * @see org.ofbiz.security.Security#findUserLoginSecurityGroupByUserLoginId(java.lang.String)
     */   
    public Iterator findUserLoginSecurityGroupByUserLoginId(String userLoginId) {
        List collection = (List) userLoginSecurityGroupByUserLoginId.get(userLoginId);

        if (collection == null) {
            try {
                collection = delegator.findByAnd("UserLoginSecurityGroup", UtilMisc.toMap("userLoginId", userLoginId), null);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            // make an empty collection to speed up the case where a userLogin belongs to no security groups
            if (collection == null) collection = new LinkedList();
            userLoginSecurityGroupByUserLoginId.put(userLoginId, collection);
        }
        // filter each time after cache retreival, ie cache will contain entire list
        collection = EntityUtil.filterByDate(collection, true);
        return collection.iterator();
    }

    /**
     * @see org.ofbiz.security.Security#securityGroupPermissionExists(java.lang.String, java.lang.String)
     */   
    public boolean securityGroupPermissionExists(String groupId, String permission) {
        GenericValue securityGroupPermissionValue = delegator.makeValue("SecurityGroupPermission",
                UtilMisc.toMap("groupId", groupId, "permissionId", permission));
        Boolean exists = (Boolean) securityGroupPermissionCache.get(securityGroupPermissionValue);

        if (exists == null) {
            try {
                if (delegator.findByPrimaryKey(securityGroupPermissionValue.getPrimaryKey()) != null)
                    exists = Boolean.TRUE;
                else
                    exists = Boolean.FALSE;
            } catch (GenericEntityException e) {
                exists = Boolean.FALSE;
                Debug.logWarning(e, module);
            }
            securityGroupPermissionCache.put(securityGroupPermissionValue, exists);
        }
        return exists.booleanValue();
    }

    /**
     * @see org.ofbiz.security.Security#hasPermission(java.lang.String, javax.servlet.http.HttpSession)
     */    
    public boolean hasPermission(String permission, HttpSession session) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        if (userLogin == null) return false;

        return hasPermission(permission, userLogin);
    }

    /**
     * @see org.ofbiz.security.Security#hasPermission(java.lang.String, org.ofbiz.entity.GenericValue)
     */    
    public boolean hasPermission(String permission, GenericValue userLogin) {
        if (userLogin == null) return false;

        Iterator iterator = findUserLoginSecurityGroupByUserLoginId(userLogin.getString("userLoginId"));
        GenericValue userLoginSecurityGroup = null;

        while (iterator.hasNext()) {
            userLoginSecurityGroup = (GenericValue) iterator.next();
            if (securityGroupPermissionExists(userLoginSecurityGroup.getString("groupId"), permission)) return true;
        }

        return false;
    }

    /**
     * @see org.ofbiz.security.Security#hasEntityPermission(java.lang.String, java.lang.String, javax.servlet.http.HttpSession)
     */    
    public boolean hasEntityPermission(String entity, String action, HttpSession session) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        if (userLogin == null) return false;
        return hasEntityPermission(entity, action, userLogin);
    }

    /**
     * @see org.ofbiz.security.Security#hasEntityPermission(java.lang.String, java.lang.String, org.ofbiz.entity.GenericValue)
     */   
    public boolean hasEntityPermission(String entity, String action, GenericValue userLogin) {
        if (userLogin == null) return false;

        // if (Debug.infoOn()) Debug.logInfo("hasEntityPermission: entity=" + entity + ", action=" + action, module);
        Iterator iterator = findUserLoginSecurityGroupByUserLoginId(userLogin.getString("userLoginId"));
        GenericValue userLoginSecurityGroup = null;

        while (iterator.hasNext()) {
            userLoginSecurityGroup = (GenericValue) iterator.next();

            // if (Debug.infoOn()) Debug.logInfo("hasEntityPermission: userLoginSecurityGroup=" + userLoginSecurityGroup.toString(), module);

            // always try _ADMIN first so that it will cache first, keeping the cache smaller
            if (securityGroupPermissionExists(userLoginSecurityGroup.getString("groupId"), entity + "_ADMIN"))
                return true;
            if (securityGroupPermissionExists(userLoginSecurityGroup.getString("groupId"), entity + action))
                return true;
        }

        return false;
    }
    
    /**
     * @see org.ofbiz.security.Security#hasRolePermission(java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpSession)
     */
    public boolean hasRolePermission(String application, String action, String primaryKey, String role, HttpSession session) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        return hasRolePermission(application, action, primaryKey, role, userLogin);
    }
    
    /**
     * @see org.ofbiz.security.Security#hasRolePermission(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.ofbiz.entity.GenericValue)
     */
    public boolean hasRolePermission(String application, String action, String primaryKey, String role, GenericValue userLogin) {
        List roles = null;
        if (role != null && !role.equals(""))
            roles = UtilMisc.toList(role);
        return hasRolePermission(application, action, primaryKey, roles, userLogin);
    }    
                
    /**
     * @see org.ofbiz.security.Security#hasRolePermission(java.lang.String, java.lang.String, java.lang.String, java.util.List, javax.servlet.http.HttpSession)
     */
    public boolean hasRolePermission(String application, String action, String primaryKey, List roles, HttpSession session) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        return hasRolePermission(application, action, primaryKey, roles, userLogin);
    }    
    
    /**
     * @see org.ofbiz.security.Security#hasRolePermission(java.lang.String, java.lang.String, java.lang.String, java.util.List, org.ofbiz.entity.GenericValue)
     */
    public boolean hasRolePermission(String application, String action, String primaryKey, List roles, GenericValue userLogin) {
        String entityName = null;
        EntityCondition condition = null;
        
        if (userLogin == null)
            return false;
            
        // quick test for special cases where were just want to check the permission (find screens)              
        if (primaryKey.equals("") && roles == null) {
            if (hasEntityPermission(application, action, userLogin)) return true;
            if (hasEntityPermission(application + "_ROLE", action, userLogin)) return true;
        }            
        
        Map simpleRoleMap = (Map) OFBizSecurity.simpleRoleEntity.get(application);
        if (simpleRoleMap != null && roles != null) {
            entityName = (String) simpleRoleMap.get("name");
            String pkey = (String) simpleRoleMap.get("pkey");
            if (pkey != null) {
                List expressions = new ArrayList();
                Iterator i = roles.iterator();
                while (i.hasNext()) {
                    String role = (String) i.next();
                    expressions.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, role));                    
                }
                EntityConditionList exprList = new EntityConditionList(expressions, EntityOperator.OR);
                EntityExpr keyExpr = new EntityExpr(pkey, EntityOperator.EQUALS, primaryKey);
                EntityExpr partyExpr = new EntityExpr("partyId", EntityOperator.EQUALS, userLogin.getString("partyId"));
                List joinList = UtilMisc.toList(exprList, keyExpr, partyExpr);
                condition = new EntityConditionList(joinList, EntityOperator.AND);                
            }
            
        }
        
        return hasRolePermission(application, action, entityName, condition, userLogin);                      
    }
    
    /**
     * Like hasEntityPermission above, this checks the specified action, as well as for "_ADMIN" to allow for simplified
     * general administration permission, but also checks action_ROLE and validates the user is a member for the
     * application.
     *
     * @param application The name of the application corresponding to the desired permission.
     * @param action The action on the application corresponding to the desired permission.
     * @param entityName The name of the role entity to use for validation.
     * @param condition EntityCondition used to query the entityName.
     * @param userLogin The userLogin object for user to check against.
     * @return Returns true if the currently logged in userLogin has the specified permission, otherwise returns false.
     */
    public boolean hasRolePermission(String application, String action, String entityName, EntityCondition condition, GenericValue userLogin) {
        if (userLogin == null) return false;
        
        // first check the standard permission
        if (hasEntityPermission(application, action, userLogin)) return true;
        
        // make sure we have what's needed for role security
        if (entityName == null || condition == null) return false;
        
        // now check the user for the role permission
        if (hasEntityPermission(application + "_ROLE", action, userLogin)) {
            // we have the permission now, we check to make sure we are allowed access
            List roleTest = null;
            try {
                //Debug.logInfo("Doing Role Security Check on [" + entityName + "]" + "using [" + condition + "]", module);
                roleTest = delegator.findByCondition(entityName, condition, null, null);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems doing role security lookup on entity [" + entityName + "] using [" + condition + "]", module);
                return false;
            }
            
            // if we pass all tests
            //Debug.logInfo("Found (" + (roleTest == null ? 0 : roleTest.size()) + ") matches :: " + roleTest, module);
            if (roleTest != null && roleTest.size() > 0) return true;
        }
        
        return false;
    }
            
}
