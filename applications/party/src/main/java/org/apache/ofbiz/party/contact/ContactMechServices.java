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

package org.apache.ofbiz.party.contact;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;


/**
 * Services for Contact Mechanism maintenance
 */
public class ContactMechServices {

    public static final String module = ContactMechServices.class.getName();
    public static final String resource = "PartyUiLabels";
    public static final String resourceError = "PartyErrorUiLabels";

    /**
     * Creates a ContactMech
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createContactMech(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");

        if (result.size() > 0) {
            return result;
        }

        String contactMechTypeId = (String) context.get("contactMechTypeId");

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_id_generation_failure", locale));
        }

        GenericValue tempContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", newCmId, "contactMechTypeId", contactMechTypeId));
        toBeStored.add(tempContactMech);

        if (!"_NA_".equals(partyId)) {
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("partyId", partyId, "contactMechId", newCmId,
                    "fromDate", now, "roleTypeId", context.get("roleTypeId"), "allowSolicitation", context.get("allowSolicitation"), "extension", context.get("extension"))));
        }

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.service_createContactMech_not_be_used_for_POSTAL_ADDRESS", locale));
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.service_createContactMech_not_be_used_for_TELECOM_NUMBER", locale));
        } else {
            tempContactMech.set("infoString", context.get("infoString"));
        }

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
    public static Map<String, Object> updateContactMech(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");

        if (result.size() > 0) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_change_contact_info_id_generation_failure", locale));
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech;
        GenericValue partyContactMech = null;

        try {
            contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
        }

        if (!"_NA_".equals(partyId)) {
            // try to find a PartyContactMech with a valid date range
            try {
                partyContactMech = EntityQuery.use(delegator).from("PartyContactMech")
                        .where("partyId", partyId, "contactMechId", contactMechId)
                        .orderBy("fromDate")
                        .filterByDate()
                        .queryFirst();
                if (partyContactMech == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                            "contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale));
                }
                toBeStored.add(partyContactMech);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_find_specified_contact_info_read", locale));
        }

        String contactMechTypeId = contactMech.getString("contactMechTypeId");

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.service_updateContactMech_not_be_used_for_POSTAL_ADDRESS", locale));
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.service_updateContactMech_not_be_used_for_TELECOM_NUMBER", locale));
        } else {
            newContactMech.set("infoString", context.get("infoString"));
        }

        newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
        newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (!newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            newContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("fromDate", now);
            newPartyContactMech.set("thruDate", null);

            try {
                Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose", null, null, false));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                    tempVal.set("contactMechId", newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "contactmechservices.could_not_change_contact_info_read",
                        UtilMisc.toMap("errMessage", e.getMessage()), locale));
            }

            partyContactMech.set("thruDate", now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "contactmechservices.could_not_change_contact_info_write",
                        UtilMisc.toMap("errMessage", e.getMessage()), locale));
            }
        } else {
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resourceError,
                       "contactmechservices.no_changes_made_not_updating", locale));
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
    public static Map<String, Object> deleteContactMech(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_DELETE");

        if (result.size() > 0) {
            return result;
        }

        // never delete a contact mechanism, just put a to date on the link to the party
        String contactMechId = (String) context.get("contactMechId");
        GenericValue partyContactMech = null;

        try {
            // try to find a PartyContactMech with a valid date range
            partyContactMech = EntityQuery.use(delegator).from("PartyContactMech")
                    .where("partyId", partyId, "contactMechId", contactMechId)
                    .orderBy("fromDate")
                    .filterByDate()
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_delete_contact_info_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }

        if (partyContactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_delete_contact_info_no_contact_found", locale));
        }

        partyContactMech.set("thruDate", UtilDateTime.nowTimestamp());
        try {
            partyContactMech.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_delete_contact_info_write", locale));
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
    public static Map<String, Object> createPostalAddress(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");

        if (result.size() > 0) {
            return result;
        }

        String contactMechTypeId = "POSTAL_ADDRESS";

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_id_generation_failure", locale));
        }

        GenericValue tempContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", newCmId, "contactMechTypeId", contactMechTypeId));
        toBeStored.add(tempContactMech);

        // don't create a PartyContactMech if there is no party; we define no party as sending _NA_ as partyId
        if (!"_NA_".equals(partyId)) {
            toBeStored.add(delegator.makeValue("PartyContactMech",
                    UtilMisc.toMap("partyId", partyId, "contactMechId", newCmId,
                        "fromDate", now, "roleTypeId", context.get("roleTypeId"), "allowSolicitation",
                        context.get("allowSolicitation"), "extension", context.get("extension"))));
        }

        GenericValue newAddr = delegator.makeValue("PostalAddress");

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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
    public static Map<String, Object> updatePostalAddress(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");

        if (result.size() > 0) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_change_contact_info_id_generation_failure", locale));
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech;
        GenericValue partyContactMech = null;

        try {
            contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
        }

        if (!"_NA_".equals(partyId)) {
            // try to find a PartyContactMech with a valid date range
            try {
                partyContactMech = EntityQuery.use(delegator).from("PartyContactMech")
                        .where("partyId", partyId, "contactMechId", contactMechId)
                        .orderBy("fromDate")
                        .filterByDate()
                        .queryFirst();
                if (partyContactMech == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                            "contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale));
                }
                toBeStored.add(partyContactMech);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                contactMech = null;
            }
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_find_specified_contact_info_read", locale));
        }

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = null;
        if (partyContactMech != null) {
            newPartyContactMech = GenericValue.create(partyContactMech);
        }
        GenericValue relatedEntityToSet = null;

        if ("POSTAL_ADDRESS".equals(contactMech.getString("contactMechTypeId"))) {
            GenericValue addr;
            try {
                addr = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_update_contact_as_POSTAL_ADDRESS_specified",
                    UtilMisc.toMap("contactMechTypeId", contactMech.getString("contactMechTypeId")), locale));
        }

        if (newPartyContactMech != null) {
            newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
            newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));
        }

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (newPartyContactMech != null && !newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        if (newPartyContactMech != null) {
            toBeStored.add(newPartyContactMech);
        }

        if (isModified) {
            toBeStored.add(relatedEntityToSet);

            newContactMech.set("contactMechId", newCmId);
            if (newPartyContactMech != null) {
                newPartyContactMech.set("contactMechId", newCmId);
                newPartyContactMech.set("fromDate", now);
                newPartyContactMech.set("thruDate", null);

                try {
                    Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose", null, null, false));

                    while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                        GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                        tempVal.set("contactMechId", newCmId);
                        toBeStored.add(tempVal);
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                            "contactmechservices.could_not_change_contact_info_read",
                            UtilMisc.toMap("errMessage", e.getMessage()), locale));
                }

                partyContactMech.set("thruDate", now);
            }

            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "contactmechservices.could_not_change_contact_info_write",
                        UtilMisc.toMap("errMessage", e.getMessage()), locale));
            }
        } else {
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resourceError,
                    "contactmechservices.no_changes_made_not_updating", locale));
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
    public static Map<String, Object> createTelecomNumber(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");

        if (result.size() > 0) {
            return result;
        }

        String contactMechTypeId = "TELECOM_NUMBER";

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_id_generation_failure", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_create_contact_info_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
    public static Map<String, Object> updateTelecomNumber(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<>();
        boolean isModified = false;

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_UPDATE");

        if (result.size() > 0) {
            return result;
        }

        String newCmId = null;
        try {
            newCmId = delegator.getNextSeqId("ContactMech");
        } catch (IllegalArgumentException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_change_contact_info_id_generation_failure", locale));
        }

        String contactMechId = (String) context.get("contactMechId");
        GenericValue contactMech = null;
        GenericValue partyContactMech = null;

        try {
            contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
            // try to find a PartyContactMech with a valid date range
            partyContactMech = EntityQuery.use(delegator).from("PartyContactMech")
                    .where("partyId", partyId, "contactMechId", contactMechId)
                    .orderBy("fromDate")
                    .filterByDate()
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            contactMech = null;
        }
        if (contactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_find_specified_contact_info_read", locale));
        }
        if (partyContactMech == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.cannot_update_specified_contact_info_not_corresponds", locale));
        }
        toBeStored.add(partyContactMech);

        // never change a contact mech, just create a new one with the changes
        GenericValue newContactMech = GenericValue.create(contactMech);
        GenericValue newPartyContactMech = GenericValue.create(partyContactMech);
        GenericValue relatedEntityToSet = null;

        if ("TELECOM_NUMBER".equals(contactMech.getString("contactMechTypeId"))) {
            GenericValue telNum;
            try {
                telNum = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", contactMechId).queryOne();
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_update_contact_as_TELECOM_NUMBER_specified",
                    UtilMisc.toMap("contactMechTypeId", contactMech.getString("contactMechTypeId")), locale));
        }

        newPartyContactMech.set("roleTypeId", context.get("roleTypeId"));
        newPartyContactMech.set("allowSolicitation", context.get("allowSolicitation"));

        if (!newContactMech.equals(contactMech)) {
            isModified = true;
        }
        if (!newPartyContactMech.equals(partyContactMech)) {
            isModified = true;
        }

        toBeStored.add(newContactMech);
        toBeStored.add(newPartyContactMech);

        if (isModified) {
            toBeStored.add(relatedEntityToSet);

            newContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("contactMechId", newCmId);
            newPartyContactMech.set("fromDate", now);
            newPartyContactMech.set("thruDate", null);

            try {
                Iterator<GenericValue> partyContactMechPurposes = UtilMisc.toIterator(partyContactMech.getRelated("PartyContactMechPurpose", null, null, false));

                while (partyContactMechPurposes != null && partyContactMechPurposes.hasNext()) {
                    GenericValue tempVal = GenericValue.create(partyContactMechPurposes.next());

                    tempVal.set("contactMechId", newCmId);
                    toBeStored.add(tempVal);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "contactmechservices.could_not_change_contact_info_read",
                        UtilMisc.toMap("errMessage", e.getMessage()), locale));
            }

            partyContactMech.set("thruDate", now);
            try {
                delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "contactmechservices.could_not_change_contact_info_write",
                        UtilMisc.toMap("errMessage", e.getMessage()), locale));
            }
        } else {
            result.put("newContactMechId", contactMechId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resourceError,
                    "contactmechservices.no_changes_made_not_updating", locale));
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
    public static Map<String, Object> createEmailAddress(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> newContext = UtilMisc.makeMapWritable(context);

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
    public static Map<String, Object> updateEmailAddress(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> newContext = UtilMisc.makeMapWritable(context);

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
    public static Map<String, Object> createPartyContactMechPurpose(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
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
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        GenericValue tempVal;
        try {
            tempVal = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                    .where("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId)
                    .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            tempVal = null;
        }

        if (UtilValidate.isEmpty(fromDate)) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        if (tempVal != null) {
            // exists already with valid date, show warning
            errMsg = UtilProperties.getMessage(resourceError,
                       "contactmechservices.could_not_create_new_purpose_already_exists", locale);
            errMsg += ": " + tempVal.getPrimaryKey().toString();
            return ServiceUtil.returnError(errMsg);
        }
        // no entry with a valid date range exists, create new with open thruDate
        GenericValue newPartyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose",
                UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId,
                    "fromDate", fromDate));

        try {
            delegator.create(newPartyContactMechPurpose);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "contactmechservices.could_not_add_purpose_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }

        result.put("fromDate", fromDate);
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
    public static Map<String, Object> getPartyContactMechValueMaps(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String)context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(partyId)) {
            if (userLogin != null) {
                partyId = userLogin.getString("partyId");
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "PartyCannotGetPartyContactMech", locale));
            }
        }
        Boolean bShowOld = (Boolean)context.get("showOld");
        boolean showOld = (bShowOld != null && bShowOld) ? true : false;
        String contactMechTypeId = (String)context.get("contactMechTypeId");
        List<Map<String, Object>> valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, showOld, contactMechTypeId);
        result.put("valueMaps", valueMaps);
        return result;
    }

    /**
     * Copies all contact mechs from one party to another. Does not delete or overwrite any contact mechs.
     */
    public static Map<String, Object> copyPartyContactMechs(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        Locale locale = (Locale) context.get("locale");

        try {
            // grab all of the non-expired contact mechs using this party worker method
            List<Map<String, Object>> valueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyIdFrom, false);

            // loop through results
            for (Map<String, Object> thisMap: valueMaps) {
                GenericValue contactMech = (GenericValue) thisMap.get("contactMech");
                GenericValue partyContactMech = (GenericValue) thisMap.get("partyContactMech");
                List<GenericValue> partyContactMechPurposes = UtilGenerics.cast(thisMap.get("partyContactMechPurposes"));

                // get the contactMechId
                String contactMechId = contactMech.getString("contactMechId");

                // create a new party contact mech for the partyIdTo
                Map<String, Object> serviceResults = dispatcher.runSync("createPartyContactMech", UtilMisc.<String, Object>toMap("partyId", partyIdTo, "userLogin", userLogin,
                            "contactMechId", contactMechId, "contactMechTypeId", contactMech.getString("contactMechTypeId"), "fromDate", UtilDateTime.nowTimestamp(),
                            "allowSolicitation", partyContactMech.getString("allowSolicitation"), "extension", partyContactMech.getString("extension")));
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                }

                // loop through purposes and copy each as a new purpose for the partyIdTo
                for (GenericValue purpose: partyContactMechPurposes) {
                    Map<String, Object> input = UtilMisc.toMap("partyId", partyIdTo, "contactMechId", contactMechId, "userLogin", userLogin);
                    input.put("contactMechPurposeTypeId", purpose.getString("contactMechPurposeTypeId"));
                    serviceResults = dispatcher.runSync("createPartyContactMechPurpose", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotCopyPartyContactMech",
                    UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates an EmailAddressVerification
     */
    public static Map<String, Object> createEmailAddressVerification(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String emailAddress = (String) context.get("emailAddress");
        String verifyHash = null;

        String expireTime = EntityUtilProperties.getPropertyValue("security", "email_verification.expire.hours", delegator);
        Integer expTime = Integer.valueOf(expireTime);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expTime);
        Date date = calendar.getTime();
        Timestamp expireDate = UtilDateTime.toTimestamp(date);

        SecureRandom secureRandom = new SecureRandom();

        synchronized(ContactMechServices.class) {
            while (true) {
                Long random = secureRandom.nextLong();
                verifyHash = HashCrypt.digestHash("MD5", Long.toString(random).getBytes(StandardCharsets.UTF_8));
                List<GenericValue> emailAddVerifications = null;
                try {
                    emailAddVerifications = EntityQuery.use(delegator).from("EmailAddressVerification").where("verifyHash", verifyHash).queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (UtilValidate.isEmpty(emailAddVerifications)) {
                    GenericValue emailAddressVerification = delegator.makeValue("EmailAddressVerification");
                    emailAddressVerification.set("emailAddress", emailAddress);
                    emailAddressVerification.set("verifyHash", verifyHash);
                    emailAddressVerification.set("expireDate", expireDate);
                    try {
                        delegator.create(emailAddressVerification);
                    } catch (GenericEntityException e) {
                        Debug.logError(e.getMessage(),module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                    break;
                }
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("verifyHash", verifyHash);
        return result;
    }

}
