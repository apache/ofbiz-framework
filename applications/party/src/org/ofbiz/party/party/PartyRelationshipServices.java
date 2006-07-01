/*
 * $Id: PartyRelationshipServices.java 6449 2005-12-30 16:18:57Z sichen $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
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
 * Services for Party Relationship maintenance
 *
 * @author     <a href="mailto:cworley@chris-n-april.com">Christopher Worley</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class PartyRelationshipServices {

    public static final String module = PartyRelationshipServices.class.getName();
    public static final String resource = "PartyUiLabels";

    /** Creates a PartyRelationship
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyRelationship(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // check permission PARTYMGR_REL_CREATE or use ServiceUtil.getPartyIdCheckSecurity to check permission
        if (!security.hasEntityPermission("PARTYMGR", "_REL_CREATE", userLogin)) {
            // note: partyId isn't used after this, but it might in the future. for now, it is used only to check the security
            String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_CREATE");
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        String partyIdFrom = (String) context.get("partyIdFrom");
        if (partyIdFrom == null) {
            partyIdFrom = (String) userLogin.getString("partyId");
        }

        String partyIdTo = (String) context.get("partyIdTo");
        if (partyIdTo == null) {
//            Map messageMap = UtilMisc.toMap("errProductFeatures", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.cannot_create_party_relationship_partyIdTo_null", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String roleTypeIdFrom = (String) context.get("roleTypeIdFrom");
        if (roleTypeIdFrom == null) {
            roleTypeIdFrom = "_NA_";
        }

        String roleTypeIdTo = (String) context.get("roleTypeIdTo");
        if (roleTypeIdTo == null) {
            roleTypeIdTo = "_NA_";
        }

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        try {
            if (delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyIdFrom, "roleTypeId", roleTypeIdFrom)) == null) {
                errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.cannot_create_party_relationship_partyIdFrom_not_in_role", locale);
                return ServiceUtil.returnError(errMsg);
            }

            if (delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyIdTo, "roleTypeId", roleTypeIdTo)) == null) {
                errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.cannot_create_party_relationship_partyIdTo_not_in_role", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_create_party_role_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue partyRelationship = delegator.makeValue("PartyRelationship", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "roleTypeIdFrom", roleTypeIdFrom, "roleTypeIdTo", roleTypeIdTo, "fromDate", fromDate));
        partyRelationship.setNonPKFields(context);

        try {
            if (delegator.findByPrimaryKey(partyRelationship.getPrimaryKey()) != null) {
                errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_create_party_role_exists", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_create_party_role_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            partyRelationship.create();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_create_party_relationship_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Updates a PartyRelationship
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updatePartyRelationship(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // check permission PARTYMGR_REL_UPDATE or use ServiceUtil.getPartyIdCheckSecurity to check permission
        if (!security.hasEntityPermission("PARTYMGR", "_REL_UPDATE", userLogin)) {
            // note: partyId isn't used after this, but it might in the future. for now, it is used only to check the security
            String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_UPDATE");
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        String partyIdFrom = (String) context.get("partyIdFrom");

        if (partyIdFrom == null) {
            partyIdFrom = (String) userLogin.getString("partyId");
        }

        String partyIdTo = (String) context.get("partyIdTo");

        if (partyIdTo == null) {
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.cannot_create_party_relationship_partyIdTo_null", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String roleTypeIdFrom = (String) context.get("roleTypeIdFrom");

        if (roleTypeIdFrom == null) {
            roleTypeIdFrom = "_NA_";
        }

        String roleTypeIdTo = (String) context.get("roleTypeIdTo");

        if (roleTypeIdTo == null) {
            roleTypeIdTo = "_NA_";
        }

        GenericValue partyRelationship = null;
        try {
            partyRelationship = delegator.findByPrimaryKey("PartyRelationship", UtilMisc.toMap("partyIdFrom", partyIdFrom,
                            "partyIdTo", partyIdTo, "roleTypeIdFrom", roleTypeIdFrom, "roleTypeIdTo", roleTypeIdTo, "fromDate", context.get("fromDate")));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_update_party_relation_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (partyRelationship == null) {
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_update_party_relation_not_found", locale);
            return ServiceUtil.returnError(errMsg);
        }

        partyRelationship.setNonPKFields(context);

        try {
            partyRelationship.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            errMsg = UtilProperties.getMessage(resource,"partyrelationshipservices.could_not_update_party_relation_write", locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Deletes a PartyRelationship
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map deletePartyRelationship(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!security.hasEntityPermission("PARTYMGR", "_REL_DELETE", userLogin)) {
            // note: partyId isn't used after this, but it might in the future. for now, it is used only to check the security
            String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_DELETE");
        }

        if (result.size() > 0)
            return result;

        GenericValue partyRelationship = null;

        try {
            partyRelationship = delegator.findByPrimaryKey("PartyRelationship", UtilMisc.toMap("partyIdFrom", context.get("partyIdFrom"), "partyIdTo", context.get("partyIdTo"), "roleTypeIdFrom", context.get("roleTypeIdFrom"), "roleTypeIdTo", context.get("roleTypeIdTo"), "fromDate", context.get("fromDate")));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("Could not delete party relationship (read failure): " + e.getMessage());
        }

        if (partyRelationship == null) {
            return ServiceUtil.returnError("Could not delete party relationship (partyRelationship not found)");
        }

        try {
            partyRelationship.remove();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError("Could delete party role (write failure): " + e.getMessage());
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /** Creates a PartyRelationshipType
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyRelationshipType(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_CREATE");

        if (result.size() > 0)
            return result;

        GenericValue partyRelationshipType = delegator.makeValue("PartyRelationshipType", UtilMisc.toMap("partyRelationshipTypeId", context.get("partyRelationshipTypeId")));

        partyRelationshipType.set("parentTypeId", context.get("parentTypeId"), false);
        partyRelationshipType.set("hasTable", context.get("hasTable"), false);
        partyRelationshipType.set("roleTypeIdValidFrom", context.get("roleTypeIdValidFrom"), false);
        partyRelationshipType.set("roleTypeIdValidTo", context.get("roleTypeIdValidTo"), false);
        partyRelationshipType.set("description", context.get("description"), false);
        partyRelationshipType.set("partyRelationshipName", context.get("partyRelationshipName"), false);

        try {
            if (delegator.findByPrimaryKey(partyRelationshipType.getPrimaryKey()) != null) {
                return ServiceUtil.returnError("Could not create party relationship type: already exists");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("Could not create party relationship type (read failure): " + e.getMessage());
        }

        try {
            partyRelationshipType.create();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError("Could not create party relationship type (write failure): " + e.getMessage());
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

}
