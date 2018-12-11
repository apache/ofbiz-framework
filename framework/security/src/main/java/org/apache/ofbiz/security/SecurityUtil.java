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
package org.apache.ofbiz.security;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

/**
 * A <code>Security</code> util.
 */
public final class SecurityUtil {

    public static final String module = SecurityUtil.class.getName();
    private static final List<String> adminPermissions = UtilMisc.toList(
            "IMPERSONATE_ADMIN",
            "ARTIFACT_INFO_VIEW",
            "SERVICE_MAINT",
            "ENTITY_MAINT",
            "UTIL_CACHE_VIEW",
            "UTIL_DEBUG_VIEW");

    /**
     * Return true if given userLogin possess at least one of the adminPermission
     *
     * @param delegator
     * @param userLoginId
     * @return
     */
    public static boolean hasUserLoginAdminPermission(Delegator delegator, String userLoginId) {
        if (UtilValidate.isEmpty(userLoginId)) return false;
        try {
            return EntityQuery.use(delegator)
                    .from("UserLoginAndPermission")
                    .where(EntityCondition.makeCondition(
                            EntityCondition.makeCondition("userLoginId", userLoginId),
                            EntityCondition.makeCondition("permissionId", EntityOperator.IN, adminPermissions)))
                    .filterByDate("fromDate", "thruDate", "permissionFromDate", "permissionThruDate")
                    .queryCount() != 0;
        } catch (GenericEntityException e) {
            Debug.logError("Failed to resolve user permissions", module);
        }
        return false;
    }

    /**
     * Return the list of missing permission, if toUserLoginId has more permission thant userLoginId, emptyList either.
     *
     * @param delegator
     * @param userLoginId
     * @param toUserLoginId
     * @return
     */
    public static List<String> hasUserLoginMorePermissionThan(Delegator delegator, String userLoginId, String toUserLoginId) {
        ArrayList<String> returnList = new ArrayList<>();
        if (UtilValidate.isEmpty(userLoginId) || UtilValidate.isEmpty(toUserLoginId)) return returnList;
        List<String> userLoginPermissionIds;
        List<String> toUserLoginPermissionIds;
        try {
            userLoginPermissionIds = EntityUtil.getFieldListFromEntityList(
                    EntityQuery.use(delegator)
                            .from("UserLoginAndPermission")
                            .where("userLoginId", userLoginId)
                            .filterByDate("fromDate", "thruDate", "permissionFromDate", "permissionThruDate")
                            .queryList(), "permissionId", true);
            toUserLoginPermissionIds = EntityUtil.getFieldListFromEntityList(
                    EntityQuery.use(delegator)
                            .from("UserLoginAndPermission")
                            .where("userLoginId", toUserLoginId)
                            .filterByDate("fromDate", "thruDate", "permissionFromDate", "permissionThruDate")
                            .queryList(), "permissionId", true);
        } catch (GenericEntityException e) {
            Debug.logError("Failed to resolve user permissions", module);
            return returnList;
        }

        if (UtilValidate.isEmpty(userLoginPermissionIds)) return toUserLoginPermissionIds;
        if (UtilValidate.isEmpty(toUserLoginPermissionIds)) return returnList;

        //Resolve all ADMIN permissions associated with the origin user
        List<String> adminPermissions = userLoginPermissionIds.stream()
                .filter(perm -> perm.endsWith("_ADMIN"))
                .map(perm -> StringUtil.replaceString(perm, "_ADMIN", ""))
                .collect(Collectors.toList());

        // if toUserLoginPermissionIds contains at least one permission that is not in admin permission or userLoginPermissionIds
        // return the list of missing permission
        return toUserLoginPermissionIds.stream()
                .filter(perm ->
                        !userLoginPermissionIds.contains(perm)
                                && !adminPermissions.contains(perm.substring(0, perm.lastIndexOf("_"))))
                .collect(Collectors.toList());
    }
}