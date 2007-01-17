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

package org.ofbiz.party.party;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for Party Role maintenance
 */
public class PartyRoleServices {
    
    public static final String module = PartyRoleServices.class.getName();
    public static final String resource = "PartyUiLabels";

    /** 
     * Creates a PartyRole
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyRole(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // check permission PARTYMGR_ROLE_CREATE or use ServiceUtil.getPartyIdCheckSecurity to check permission
        String partyId = null;
        if (security.hasEntityPermission("PARTYMGR", "_ROLE_CREATE", userLogin)) {
            partyId = (String) context.get("partyId");
        } else {
            partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_CREATE");
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        GenericValue partyRole = delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", context.get("roleTypeId")));

        try {
            if (delegator.findByPrimaryKey(partyRole.getPrimaryKey()) != null) {
                errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_create_party_role_exists", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_create_party_role_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            partyRole.create();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_create_party_role_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
//            return ServiceUtil.returnError("Could create party role (write failure): " + e.getMessage());
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** 
     * Deletes a PartyRole
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map deletePartyRole(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // check permission PARTYMGR_ROLE_DELETE or use ServiceUtil.getPartyIdCheckSecurity to check permission
        String partyId = null;
        if (security.hasEntityPermission("PARTYMGR", "_ROLE_DELETE", userLogin)) {
            partyId = (String) context.get("partyId");
        } else {
            partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_CREATE");
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        GenericValue partyRole = null;

        try {
            partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", context.get("roleTypeId")));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_delete_party_role_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (partyRole == null) {
            errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_delete_party_role_not_found", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            partyRole.remove();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyroleservices.could_not_delete_party_role_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
