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
package org.ofbiz.service;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Service Permission Model Class
 */
public class ModelPermission implements Serializable {

    public static final String module = ModelPermission.class.getName();

    public static final int PERMISSION = 1;
    public static final int ENTITY_PERMISSION = 2;
    public static final int ROLE_MEMBER = 3;

    public ModelService serviceModel = null;
    public int permissionType = 0;
    public String nameOrRole = null;
    public String action = null;
    public String clazz = null;

    public boolean evalPermission(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (userLogin == null) {
            Debug.logInfo("Secure service requested with no userLogin object", module);
            return false;
        }
        switch (permissionType) {
            case PERMISSION:
                return evalSimplePermission(security, userLogin);
            case ENTITY_PERMISSION:
                return evalEntityPermission(security, userLogin);
            case ROLE_MEMBER:
                return evalRoleMember(userLogin);
            default:
                Debug.logWarning("Invalid permission type [" + permissionType + "] for permission named : " + nameOrRole + " on service : " + serviceModel.name, module);
                return false;
        }
    }

    private boolean evalSimplePermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null permission name passed for evaluation", module);
            return false;
        }
        return security.hasPermission(nameOrRole, userLogin);
    }

    private boolean evalEntityPermission(Security security, GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null permission name passed for evaluation", module);
            return false;
        }
        if (action == null) {
            Debug.logWarning("Null action passed for evaluation",  module);
        }
        return security.hasEntityPermission(nameOrRole, action, userLogin);
    }

    private boolean evalRoleMember(GenericValue userLogin) {
        if (nameOrRole == null) {
            Debug.logWarning("Null role type name passed for evaluation", module);
            return false;
        }
        GenericDelegator delegator = userLogin.getDelegator();
        List partyRoles = null;
        try {
            partyRoles = delegator.findByAnd("PartyRole", UtilMisc.toMap("roleTypeId", nameOrRole, "partyId", userLogin.get("partyId")));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to lookup PartyRole records", module);
        }

        if (partyRoles != null && partyRoles.size() > 0) {
            partyRoles = EntityUtil.filterByDate(partyRoles);
            if (partyRoles != null && partyRoles.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
