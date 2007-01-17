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

package org.ofbiz.party.contact;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;


/**
 * Services for Contact Mechanism maintenance
 */
public class ContactMechServices {

    public static final String module = ContactMechServices.class.getName();
    public static final String resource = "PartyUiLabels";

    /**
     * Creates a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createContactMech(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");


        if (result.size() > 0)
            return result;

        String contactMechTypeId = (String) context.get("contactMechTypeId");

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue tempContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", newCmId, "contactMechTypeId", contactMechTypeId));
        toBeStored.add(tempContactMech);

        if (!partyId.equals("_NA_")) {
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", newCmId,
                    "fromDate", now, "roleTypeId", context.get("roleTypeId"), "allowSolicitation", context.get("allowSolicitation"), "extension", context.get("extension"))));
        }

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.service_createContactMech_not_be_used_for_POSTAL_ADDRESS", locale);
            return ServiceUtil.returnError(errMsg);
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.service_createContactMech_not_be_used_for_TELECOM_NUMBER", locale);
            return ServiceUtil.returnError(errMsg);
        } else {
            tempContactMech.set("infoString", context.get("infoString"));
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("contactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updateContactMech(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech = null;
        GenericValue partyContactMech = null;

        try {
            contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
        }

        if (!partyId.equals("_NA_")) {
            // try to find a PartyContactMech with a valid date range
            try {
                List partyContactMechs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId), UtilMisc.toList("fromDate")), true);
                partyContactMech = EntityUtil.getFirst(partyContactMechs);
                if (partyContactMech == null) {
                    errMsg = UtilProperties.getMessage(resource,"contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale);
                    return ServiceUtil.returnError(errMsg);
                } else {
                    toBeStored.add(partyContactMech);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_find_specified_contact_info_read", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String contactMechTypeId = contactMech.getString("contactMechTypeId");

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);
        GenericValue relatedEntityToSet = null;

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.service_updateContactMech_not_be_used_for_POSTAL_ADDRESS", locale);
            return ServiceUtil.returnError(errMsg);
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.service_updateContactMech_not_be_used_for_TELECOM_NUMBER", locale);
            return ServiceUtil.returnError(errMsg);
        } else {
            newContactMech.set("infoString", context.get("infoString"));
        }

        newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
        newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));

        if (!newContactMech.equals(contactMech)) isModified = true;
        if (!newPartyContactMech.equals(partyContactMech)) isModified = true;

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            if (relatedEntityToSet != null) toBeStored.add(relatedEntityToSet);

            newContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("fromDate", now);
            newPartyContactMech.set("thruDate", null);

            try {
                Iterator partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose"));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create((GenericValue) partyContactMechPurposes.next());

                    tempVal.set("contactMechId", newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_read", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }

            partyContactMech.set("thruDate", now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_write", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            String sucMsg = UtilProperties.getMessage(resource,"contactmechservices.no_changes_made_not_updating", locale);
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, sucMsg);
            return result;
        }

        result.put("newContactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Deletes a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_DELETE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map deleteContactMech(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_DELETE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        // never delete a contact mechanism, just put a to date on the link to the party
        String contactMechId = (String) context.get("contactMechId");
        GenericValue partyContactMech = null;

        try {
            // try to find a PartyContactMech with a valid date range
            List partyContactMechs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId), UtilMisc.toList("fromDate")), true);

            partyContactMech = EntityUtil.getFirst(partyContactMechs);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_contact_info_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (partyContactMech == null) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_contact_info_no_contact_found", locale);
            return ServiceUtil.returnError(errMsg);
        }

        partyContactMech.set("thruDate", UtilDateTime.nowTimestamp());
        try {
            partyContactMech.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_contact_info_write", locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a PostalAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPostalAddress(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        String contactMechTypeId = "POSTAL_ADDRESS";

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue tempContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", newCmId, "contactMechTypeId", contactMechTypeId));
        toBeStored.add(tempContactMech);

        // don't create a PartyContactMech if there is no party; we define no party as sending _NA_ as partyId
        if (!partyId.equals("_NA_")) {
            toBeStored.add(delegator.makeValue("PartyContactMech",
                    UtilMisc.toMap("partyId", partyId, "contactMechId", newCmId,
                        "fromDate", now, "roleTypeId", context.get("roleTypeId"), "allowSolicitation",
                        context.get("allowSolicitation"), "extension", context.get("extension"))));
        }

        GenericValue newAddr = delegator.makeValue("PostalAddress", null);

        newAddr.set("contactMechId", newCmId);
        newAddr.set("toName", context.get("toName"));
        newAddr.set("attnName", context.get("attnName"));
        newAddr.set("address1", context.get("address1"));
        newAddr.set("address2", context.get("address2"));
        newAddr.set("directions", context.get("directions"));
        newAddr.set("city", context.get("city"));
        newAddr.set("postalCode", context.get("postalCode"));
        newAddr.set("postalCodeExt", context.get("postalCodeExt"));
        newAddr.set("stateProvinceGeoId", context.get("stateProvinceGeoId"));
        newAddr.set("countryGeoId", context.get("countryGeoId"));
        newAddr.set("postalCodeGeoId", context.get("postalCodeGeoId"));
        toBeStored.add(newAddr);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("contactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a PostalAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updatePostalAddress(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");


        if (result.size() > 0) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech = null;
        GenericValue partyContactMech = null;

        try {
            contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
        }

        if (!partyId.equals("_NA_")) {
            // try to find a PartyContactMech with a valid date range
            try {
                List partyContactMechs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId), UtilMisc.toList("fromDate")), true);
                partyContactMech = EntityUtil.getFirst(partyContactMechs);
                if (partyContactMech == null) {
                    errMsg = UtilProperties.getMessage(resource,"contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale);
                    return ServiceUtil.returnError(errMsg);
                } else {
                    toBeStored.add(partyContactMech);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_find_specified_contact_info_read", locale);
            return ServiceUtil.returnError(errMsg);
        }

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = null;
        if (partyContactMech != null)
            newPartyContactMech = GenericValue.create(partyContactMech);
        GenericValue relatedEntityToSet = null;

        if ("POSTAL_ADDRESS".equals(contactMech.getString("contactMechTypeId"))) {
            GenericValue addr = null;

            try {
                addr = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", contactMechId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                addr = null;
            }
            relatedEntityToSet = GenericValue.create(addr);
            relatedEntityToSet.set("toName", context.get("toName"));
            relatedEntityToSet.set("attnName", context.get("attnName"));
            relatedEntityToSet.set("address1", context.get("address1"));
            relatedEntityToSet.set("address2", context.get("address2"));
            relatedEntityToSet.set("directions", context.get("directions"));
            relatedEntityToSet.set("city", context.get("city"));
            relatedEntityToSet.set("postalCode", context.get("postalCode"));
            relatedEntityToSet.set("postalCodeExt", context.get("postalCodeExt"));
            relatedEntityToSet.set("stateProvinceGeoId", context.get("stateProvinceGeoId"));
            relatedEntityToSet.set("countryGeoId", context.get("countryGeoId"));
            relatedEntityToSet.set("postalCodeGeoId", context.get("postalCodeGeoId"));
            if (addr == null || !relatedEntityToSet.equals(addr)) {
                isModified = true;
            }
            relatedEntityToSet.set("contactMechId", newCmId);
        } else {
            Map messageMap = UtilMisc.toMap("contactMechTypeId", contactMech.getString("contactMechTypeId"));
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_update_contact_as_POSTAL_ADDRESS_specified", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (newPartyContactMech != null) {
            newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
            newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));
        }

        if (!newContactMech.equals(contactMech)) isModified = true;
        if (newPartyContactMech != null && !newPartyContactMech.equals(partyContactMech)) isModified = true;

        toBeStored.add(newContactMech);
        if (newPartyContactMech != null)
            toBeStored.add(newPartyContactMech);

        if (isModified) {
            if (relatedEntityToSet != null) toBeStored.add(relatedEntityToSet);

            newContactMech.set("contactMechId", newCmId);
            if (newPartyContactMech != null) {
                newPartyContactMech.set("contactMechId", newCmId);
                newPartyContactMech.set("fromDate", now);
                newPartyContactMech.set("thruDate", null);

                try {
                    Iterator partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose"));

                    while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                        GenericValue tempVal = GenericValue.create((GenericValue) partyContactMechPurposes.next());

                        tempVal.set("contactMechId", newCmId);
                        toBeStored.add(tempVal);
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.toString(), module);
                    Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                    errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_read", messageMap, locale);
                    return ServiceUtil.returnError(errMsg);
                }

                partyContactMech.set("thruDate", now);
            }

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_write", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            String sucMsg = UtilProperties.getMessage(resource,"contactmechservices.no_changes_made_not_updating", locale);
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, sucMsg);
            return result;
        }

        result.put("newContactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a TelecomNumber
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createTelecomNumber(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        String contactMechTypeId = "TELECOM_NUMBER";

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue tempContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", newCmId, "contactMechTypeId", contactMechTypeId));
        toBeStored.add(tempContactMech);

        toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", newCmId,
                    "fromDate", now, "roleTypeId", context.get("roleTypeId"), "allowSolicitation", context.get("allowSolicitation"), "extension", context.get("extension"))));

        toBeStored.add(delegator.makeValue("TelecomNumber", UtilMisc.toMap("contactMechId", newCmId,
                    "countryCode", context.get("countryCode"), "areaCode", context.get("areaCode"), "contactNumber", context.get("contactNumber"))));

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_create_contact_info_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("contactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a TelecomNumber
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updateTelecomNumber(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_id_generation_failure", locale);
            return ServiceUtil.returnError(errMsg);
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech = null;
        GenericValue partyContactMech = null;

        try {
            contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
            // try to find a PartyContactMech with a valid date range
            List partyContactMechs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId), UtilMisc.toList("fromDate")), true);

            partyContactMech = EntityUtil.getFirst(partyContactMechs);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
            partyContactMech = null;
        }
        if (contactMech == null) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_find_specified_contact_info_read", locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (partyContactMech == null) {
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale);
            return ServiceUtil.returnError(errMsg);
        }
        toBeStored.add(partyContactMech);

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);
        GenericValue relatedEntityToSet = null;

        if ("TELECOM_NUMBER".equals(contactMech.getString("contactMechTypeId"))) {
            GenericValue telNum = null;

            try {
                telNum = delegator.findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId", contactMechId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                telNum = null;
            }
            relatedEntityToSet = GenericValue.create(telNum);
            relatedEntityToSet.set("countryCode", context.get("countryCode"));
            relatedEntityToSet.set("areaCode", context.get("areaCode"));
            relatedEntityToSet.set("contactNumber", context.get("contactNumber"));

            if (telNum == null || !relatedEntityToSet.equals(telNum)) {
                isModified = true;
            }
            relatedEntityToSet.set("contactMechId", newCmId);
            newPartyContactMech.set("extension", context.get("extension"));
        } else {
            Map messageMap = UtilMisc.toMap("contactMechTypeId", contactMech.getString("contactMechTypeId"));
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_update_contact_as_TELECOM_NUMBER_specified", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
        newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));

        if (!newContactMech.equals(contactMech)) isModified = true;
        if (!newPartyContactMech.equals(partyContactMech)) isModified = true;

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            if (relatedEntityToSet != null) toBeStored.add(relatedEntityToSet);

            newContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("fromDate", now);
            newPartyContactMech.set("thruDate", null);

            try {
                Iterator partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose"));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create((GenericValue) partyContactMechPurposes.next());

                    tempVal.set("contactMechId", newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_read", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }

            partyContactMech.set("thruDate", now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_change_contact_info_write", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            String sucMsg = UtilProperties.getMessage(resource,"contactmechservices.no_changes_made_not_updating", locale);
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, sucMsg);
            return result;
        }

        result.put("newContactMechId", newCmId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a EmailAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createEmailAddress(DispatchContext ctx, Map context) {
        Map newContext = new HashMap(context);

        newContext.put("infoString", newContext.get("emailAddress"));
        newContext.remove("emailAddress");
        newContext.put("contactMechTypeId", "EMAIL_ADDRESS");

        return createContactMech(ctx, newContext);
    }

    /**
     * Updates a EmailAddress
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map updateEmailAddress(DispatchContext ctx, Map context) {
        Map newContext = new HashMap(context);

        newContext.put("infoString", newContext.get("emailAddress"));
        newContext.remove("emailAddress");
        return updateContactMech(ctx, newContext);
    }

    // ============================================================================
    // ============================================================================

    /**
     * Creates a PartyContactMechPurpose
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyContactMechPurpose(DispatchContext ctx, Map context) {
        //Debug.logInfo(new Exception(), "In createPartyContactMechPurpose context: " + context, module);
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0) {
            return result;
        }

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");

        GenericValue tempVal = null;
        try {
            Map pcmpFindMap = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId);
            //Debug.logInfo("pcmpFindMap = " + pcmpFindMap, module);
            List allPCMPs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose", pcmpFindMap), true);

            tempVal = EntityUtil.getFirst(allPCMPs);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            tempVal = null;
        }

        Timestamp fromDate = UtilDateTime.nowTimestamp();

        if (tempVal != null) {
            // exists already with valid date, show warning
            errMsg = UtilProperties.getMessage(resource, "contactmechservices.could_not_create_new_purpose_already_exists", locale);
            errMsg += ": " + tempVal.getPrimaryKey().toString();
            return ServiceUtil.returnError(errMsg);
        } else {
            // no entry with a valid date range exists, create new with open thruDate
            GenericValue newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                    UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId,
                        "fromDate", fromDate));

            try {
                delegator.create(newPartyContactMechPurpose);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_add_purpose_write", messageMap, locale);
                return ServiceUtil.returnError(errMsg);
            }
        }

        result.put("fromDate", fromDate);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Deletes the PartyContactMechPurpose corresponding to the parameters in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_DELETE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map deletePartyContactMechPurpose(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_DELETE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        GenericValue pcmp = null;

        try {
            pcmp = delegator.findByPrimaryKey("PartyContactMechPurpose", UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", fromDate));
            if (pcmp == null) {
                errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_purpose_from_contact_mechanism_not_found", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_purpose_from_contact_mechanism_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        pcmp.set("thruDate", UtilDateTime.nowTimestamp());
        try {
            pcmp.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"contactmechservices.could_not_delete_purpose_from_contact_mechanism_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
    
    /**
     * Just wraps the ContactMechWorker method of the same name.
     * 
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map getPartyContactMechValueMaps(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String)context.get("partyId");
        if (UtilValidate.isEmpty(partyId) ) {
            if (userLogin != null) {
                partyId = userLogin.getString("partyId");   
            } else {
                return ServiceUtil.returnError("Both 'partyId' and 'userLogin' are empty.");
            }
        }
        Boolean bShowOld = (Boolean)context.get("showOld");
        boolean showOld = (bShowOld != null && bShowOld.booleanValue()) ? true : false;
        String contactMechTypeId = (String)context.get("contactMechTypeId");
        List valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, showOld, contactMechTypeId);
        result.put("valueMaps", valueMaps );
        return result;
    }

    /**
     * Copies all contact mechs from one party to another. Does not delete or overwrite any contact mechs.
     */
    public static Map copyPartyContactMechs(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");

        try {
            // grab all of the non-expired contact mechs using this party worker method
            List valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyIdFrom, false);

            // loop through results 
            for (Iterator iter = valueMaps.iterator(); iter.hasNext(); ) {
                Map thisMap = (Map) iter.next();
                GenericValue contactMech = (GenericValue) thisMap.get("contactMech");
                GenericValue partyContactMech = (GenericValue) thisMap.get("partyContactMech");
                List partyContactMechPurposes = (List) thisMap.get("partyContactMechPurposes");

                // get the contactMechId
                String contactMechId = contactMech.getString("contactMechId");

                // create a new party contact mech for the partyIdTo
                Map serviceResults = dispatcher.runSync("createPartyContactMech", UtilMisc.toMap("partyId", partyIdTo, "userLogin", userLogin,
                            "contactMechId", contactMechId, "fromDate", UtilDateTime.nowTimestamp(), 
                            "allowSolicitation", partyContactMech.getString("allowSolicitation"), "extension", partyContactMech.getString("extension")));
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }

                // loop through purposes and copy each as a new purpose for the partyIdTo
                for (Iterator piter = partyContactMechPurposes.iterator(); piter.hasNext(); ) {
                    GenericValue purpose = (GenericValue) piter.next();
                    Map input = UtilMisc.toMap("partyId", partyIdTo, "contactMechId", contactMechId, "userLogin", userLogin);
                    input.put("contactMechPurposeTypeId", purpose.getString("contactMechPurposeTypeId"));
                    serviceResults = dispatcher.runSync("createPartyContactMechPurpose", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return serviceResults;
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError("Failed to copy contact mechs. Error: " + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
