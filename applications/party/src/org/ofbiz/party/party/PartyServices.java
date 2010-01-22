/*
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
 */

package org.ofbiz.party.party;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for Party/Person/Group maintenance
 */
public class PartyServices {

    public static final String module = PartyServices.class.getName();
    public static final String resource = "PartyErrorUiLabels";

    /**
     * Deletes a Party.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> deleteParty(DispatchContext ctx, Map<String, ? extends Object> context) {

        Locale locale = (Locale) context.get("locale");

        /*
         * pretty serious operation, would delete:
         * - Party
         * - PartyRole
         * - PartyRelationship: from and to
         * - PartyDataObject
         * - Person or PartyGroup
         * - PartyContactMech, but not ContactMech itself
         * - PartyContactMechPurpose
         * - Order?
         *
         * We may want to not allow this, but rather have some sort of delete flag for it if it's REALLY that big of a deal...
         */

        String errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_delete_party_not_implemented", locale);
        return ServiceUtil.returnError(errMsg);
    }

    /**
     * Creates a Person.
     * If no partyId is specified a numeric partyId is retrieved from the Party sequence.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPerson(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = FastList.newInstance();
        Locale locale = (Locale) context.get("locale");
        // in most cases userLogin will be null, but get anyway so we can keep track of that info if it is available
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String description = (String) context.get("description");

        // if specified partyId starts with a number, return an error
        if (UtilValidate.isNotEmpty(partyId) && Character.isDigit(partyId.charAt(0))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "party.id_is_digit", locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "party.id_generation_failure", locale));
            }
        }

        // check to see if party object exists, if so make sure it is PERSON type party
        GenericValue party = null;

        try {
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (party != null) {
            if (!"PERSON".equals(party.getString("partyTypeId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.create.party_exists_not_person_type", locale));
            }
        } else {
            // create a party if one doesn't already exist with an initial status from the input
            String statusId = (String) context.get("statusId");
            if (statusId == null) {
                statusId = "PARTY_ENABLED";
            }
            Map<String, Object> newPartyMap = UtilMisc.toMap("partyId", partyId, "partyTypeId", "PERSON", "description", description, "createdDate", now, "lastModifiedDate", now, "statusId", statusId);
            String preferredCurrencyUomId = (String) context.get("preferredCurrencyUomId");
            if (!UtilValidate.isEmpty(preferredCurrencyUomId)) {
                newPartyMap.put("preferredCurrencyUomId", preferredCurrencyUomId);
            }
            String externalId = (String) context.get("externalId");
            if (!UtilValidate.isEmpty(externalId)) {
                newPartyMap.put("externalId", externalId);
            }
            if (userLogin != null) {
                newPartyMap.put("createdByUserLogin", userLogin.get("userLoginId"));
                newPartyMap.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
            }
            party = delegator.makeValue("Party", newPartyMap);
            toBeStored.add(party);

            // create the status history
            GenericValue statusRec = delegator.makeValue("PartyStatus",
                    UtilMisc.toMap("partyId", partyId, "statusId", statusId, "statusDate", now));
            toBeStored.add(statusRec);
        }

        GenericValue person = null;

        try {
            person = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (person != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.create.person_exists", locale));
        }

        person = delegator.makeValue("Person", UtilMisc.toMap("partyId", partyId));
        person.setNonPKFields(context);
        toBeStored.add(person);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.create.db_error", new Object[] { e.getMessage() }, locale));
        }

        result.put("partyId", partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Sets a party status.
     * <b>security check</b>: the status change must be defined in StatusValidChange.
     */
    public static Map<String, Object> setPartyStatus(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String partyId = (String) context.get("partyId");
        String statusId = (String) context.get("statusId");
        Timestamp statusDate = (Timestamp) context.get("statusDate");
        if (statusDate == null) {
            statusDate = UtilDateTime.nowTimestamp();
        }

        try {
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

            if (party.get("statusId") == null) { // old records
                party.set("statusId", "PARTY_ENABLED");
            }

            String oldStatusId = party.getString("statusId");
            if (!party.getString("statusId").equals(statusId)) {

                // check that status is defined as a valid change
                GenericValue statusValidChange = delegator.findByPrimaryKey("StatusValidChange", UtilMisc.toMap("statusId", party.getString("statusId"), "statusIdTo", statusId));
                if (statusValidChange == null) {
                    String errorMsg = "Cannot change party status from " + party.getString("statusId") + " to " + statusId;
                    Debug.logWarning(errorMsg, module);
                    return ServiceUtil.returnError(errorMsg);
                }

                party.set("statusId", statusId);
                party.store();

                // record this status change in PartyStatus table
                GenericValue partyStatus = delegator.makeValue("PartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", statusId, "statusDate", statusDate));
                partyStatus.create();

                // disable all userlogins for this user when the new status is disabled
                if (("PARTY_DISABLED").equals(statusId)) {
                    List <GenericValue> userLogins = delegator.findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
                    for(GenericValue userLogin : userLogins) {
                        if (!"N".equals(userLogin.getString("enabled"))) {
                            userLogin.set("enabled", "N");
                            userLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());
                            userLogin.store();
                        }
                    }
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("oldStatusId", oldStatusId);
            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
        }
    }

    /**
     * Updates a Person.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updatePerson(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.party_id_missing", locale));
        }

        GenericValue person = null;
        GenericValue party = null;

        try {
            person = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.read_failure", new Object[] { e.getMessage() }, locale));
        }

        if (person == null || party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.not_found", locale));
        }

        // update status by separate service
        String oldStatusId = party.getString("statusId");
        if (party.get("statusId") == null) { // old records
            party.set("statusId", "PARTY_ENABLED");
        }

        person.setNonPKFields(context);
        party.setNonPKFields(context);

        party.set("statusId", oldStatusId);

        try {
            person.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
        }

        if (UtilValidate.isNotEmpty(context.get("statusId")) && !context.get("statusId").equals(oldStatusId)) {
            try {
                dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", context.get("statusId"), "userLogin", context.get("userLogin")));
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
            }
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "person.update.success", locale));
        return result;
    }

    /**
     * Creates a PartyGroup.
     * If no partyId is specified a numeric partyId is retrieved from the Party sequence.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPartyGroup(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_create_party_group_generation_failure", locale);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            // if specified partyId starts with a number, return an error
            if (Character.isDigit(partyId.charAt(0))) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_create_party_ID_digit", locale);
                return ServiceUtil.returnError(errMsg);
            }
        }

        try {
            // check to see if party object exists, if so make sure it is PARTY_GROUP type party
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            GenericValue partyGroupPartyType = delegator.findByPrimaryKeyCache("PartyType", UtilMisc.toMap("partyTypeId", "PARTY_GROUP"));

            if (partyGroupPartyType == null) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.party_type_not_found_in_database_cannot_create_party_group", locale);
                return ServiceUtil.returnError(errMsg);
            }

            if (party != null) {
                GenericValue partyType = party.getRelatedOneCache("PartyType");

                if (!EntityTypeUtil.isType(partyType, partyGroupPartyType)) {
                    errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_party_group_already_exists_not_PARTY_GROUP_type", locale);
                    return ServiceUtil.returnError(errMsg);
                }
            } else {
                // create a party if one doesn't already exist
                String partyTypeId = "PARTY_GROUP";

                if (UtilValidate.isNotEmpty(context.get("partyTypeId"))) {
                    GenericValue desiredPartyType = delegator.findByPrimaryKeyCache("PartyType", UtilMisc.toMap("partyTypeId", context.get("partyTypeId")));
                    if (desiredPartyType != null && EntityTypeUtil.isType(desiredPartyType, partyGroupPartyType)) {
                        partyTypeId = desiredPartyType.getString("partyTypeId");
                    } else {
                        return ServiceUtil.returnError("The specified partyTypeId [" + context.get("partyTypeId") + "] could not be found or is not a sub-type of PARTY_GROUP");
                    }
                }

                Map<String, Object> newPartyMap = UtilMisc.toMap("partyId", partyId, "partyTypeId", partyTypeId, "createdDate", now, "lastModifiedDate", now);
                if (userLogin != null) {
                    newPartyMap.put("createdByUserLogin", userLogin.get("userLoginId"));
                    newPartyMap.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                }

                String statusId = (String) context.get("statusId");
                party = delegator.makeValue("Party", newPartyMap);
                party.setNonPKFields(context);

                if (statusId == null) {
                    statusId = "PARTY_ENABLED";
                }
                party.set("statusId", statusId);
                party.create();

                // create the status history
                GenericValue partyStat = delegator.makeValue("PartyStatus",
                        UtilMisc.toMap("partyId", partyId, "statusId", statusId, "statusDate", now));
                partyStat.create();
            }

            GenericValue partyGroup = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", partyId));
            if (partyGroup != null) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_party_group_already_exists", locale);
                return ServiceUtil.returnError(errMsg);
            }

            partyGroup = delegator.makeValue("PartyGroup", UtilMisc.toMap("partyId", partyId));
            partyGroup.setNonPKFields(context);
            partyGroup.create();

        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.data_source_error_adding_party_group", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("partyId", partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates a PartyGroup.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updatePartyGroup(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.party_id_missing", locale));
        }

        String errMsg = null;
        GenericValue partyGroup = null;
        GenericValue party = null;

        try {
            partyGroup = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", partyId));
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (partyGroup == null || party == null) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_not_found", locale);
            return ServiceUtil.returnError(errMsg);
        }


        // update status by separate service
        String oldStatusId = party.getString("statusId");
        partyGroup.setNonPKFields(context);
        party.setNonPKFields(context);
        party.set("statusId", oldStatusId);

        try {
            partyGroup.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (UtilValidate.isNotEmpty(context.get("statusId")) && !context.get("statusId").equals(oldStatusId)) {
            try {
                dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", context.get("statusId"), "userLogin", context.get("userLogin")));
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
            }
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Create an Affiliate entity.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createAffiliate(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();

        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        String partyId = getPartyId(context);

        // if specified partyId starts with a number, return an error
        if (Character.isDigit(partyId.charAt(0))) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_affiliate_digit", locale);
            return ServiceUtil.returnError(errMsg);
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_affiliate_generation_failure", locale);
                return ServiceUtil.returnError(errMsg);
            }
        }

        // check to see if party object exists, if so make sure it is AFFILIATE type party
        GenericValue party = null;

        try {
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (party == null) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_affiliate_no_party_entity", locale);
            return ServiceUtil.returnError(errMsg);
        }

        GenericValue affiliate = null;

        try {
            affiliate = delegator.findByPrimaryKey("Affiliate", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (affiliate != null) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_affiliate_ID_already_exists", locale);
            return ServiceUtil.returnError(errMsg);
        }

        affiliate = delegator.makeValue("Affiliate", UtilMisc.toMap("partyId", partyId));
        affiliate.setNonPKFields(context);
        affiliate.set("dateTimeCreated", now, false);

        try {
            delegator.create(affiliate);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_add_affiliate_info_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("partyId", partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates an Affiliate.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updateAffiliate(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.party_id_missing", locale));
        }

        String errMsg = null;
        GenericValue affiliate = null;

        try {
            affiliate = delegator.findByPrimaryKey("Affiliate", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_affiliate_information_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (affiliate == null) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_affiliate_information_not_found", locale);
            return ServiceUtil.returnError(errMsg);
        }

        affiliate.setNonPKFields(context);

        try {
            affiliate.store();
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_affiliate_information_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Add a PartyNote.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPartyNote(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteString = (String) context.get("note");
        String partyId = (String) context.get("partyId");
        String noteId = (String) context.get("noteId");
        String noteName = (String) context.get("noteName");

        String errMsg = null;
        Locale locale = (Locale) context.get("locale");
        //Map noteCtx = UtilMisc.toMap("note", noteString, "userLogin", userLogin);

        //Make sure the note Id actually exists if one is passed to avoid a foreign key error below
        if (noteId != null) {
            try {
                GenericValue value = delegator.findByPrimaryKey("NoteData", UtilMisc.toMap("noteId", noteId));
                if (value == null) {
                    Debug.logError("ERROR: Note id does not exist for : " + noteId + ", autogenerating." , module);
                    noteId = null;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "ERROR: Note id does not exist for : " + noteId + ", autogenerating." , module);
                noteId = null;
            }
        }

        // if no noteId is specified, then create and associate the note with the userLogin
        if (noteId == null) {
            Map<String, Object> noteRes = null;
            try {
                noteRes = dispatcher.runSync("createNote", UtilMisc.toMap("partyId", userLogin.getString("partyId"),
                         "note", noteString, "userLogin", userLogin, "locale", locale, "noteName", noteName));
            } catch (GenericServiceException e) {
                Debug.logError(e, e.getMessage(), module);
                return ServiceUtil.returnError("Unable to create Note: " + e.getMessage());
            }

            if (noteRes.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
                return noteRes;

            noteId = (String) noteRes.get("noteId");

            if (UtilValidate.isEmpty(noteId)) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.problem_creating_note_no_noteId_returned", locale);
                return ServiceUtil.returnError(errMsg);
            }
        }
        result.put("noteId", noteId);

        // Set the party info
        try {
            Map<String, String> fields = UtilMisc.toMap("partyId", partyId, "noteId", noteId);
            GenericValue v = delegator.makeValue("PartyNote", fields);

            delegator.create(v);
        } catch (GenericEntityException ee) {
            Debug.logError(ee, module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", ee.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.problem_associating_note_with_party", messageMap, locale);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
        }
        return result;
    }

    /**
     * Get the party object(s) from an e-mail address
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartyFromExactEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = FastList.newInstance();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (email.length() == 0) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.required_parameter_email_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            EntityExpr ee = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.EQUALS, EntityFunction.UPPER(email.toUpperCase()));
            List<GenericValue> c = EntityUtil.filterByDate(delegator.findList("PartyAndContactMech", ee, null, UtilMisc.toList("infoString"), null, false), true);

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0)
            result.put("parties", parties);
        return result;
    }

    public static Map<String, Object> getPartyFromEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = FastList.newInstance();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (email.length() == 0) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.required_parameter_email_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            EntityExpr ee = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.LIKE, EntityFunction.UPPER(("%" + email.toUpperCase()) + "%"));
            List<GenericValue> c = EntityUtil.filterByDate(delegator.findList("PartyAndContactMech", ee, null, UtilMisc.toList("infoString"), null, false), true);

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0)
            result.put("parties", parties);
        return result;
    }

    /**
     * Get the party object(s) from a user login ID
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartyFromUserLogin(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logWarning("Running the getPartyFromUserLogin Service...", module);
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = FastList.newInstance();
        String userLoginId = (String) context.get("userLoginId");
        Locale locale = (Locale) context.get("locale");

        if (userLoginId.length() == 0)
            return ServiceUtil.returnError("Required parameter 'userLoginId' cannot be empty.");

        try {
            EntityExpr ee = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("userLoginId"), EntityOperator.LIKE, EntityFunction.UPPER("%" + userLoginId.toUpperCase() + "%"));
            Collection<GenericValue> ulc = delegator.findList("PartyAndUserLogin", ee, null, UtilMisc.toList("userLoginId"), null, false);

            if (Debug.verboseOn()) Debug.logVerbose("Collection: " + ulc, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromUserLogin number found: " + ulc.size(), module);
            if (ulc != null) {
                for (GenericValue ul: ulc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", ul.get("partyId"), "partyTypeId", ul.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0) {
            result.put("parties", parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from person information
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartyFromPerson(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = FastList.newInstance();
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        Locale locale = (Locale) context.get("locale");

        if (firstName == null) {
            firstName = "";
        }
        if (lastName == null) {
            lastName = "";
        }
        if (firstName.length() == 0 && lastName.length() == 0) {
            String errMsg = UtilProperties.getMessage(resource,"partyservices.both_names_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + firstName.toUpperCase() + "%")),
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + lastName.toUpperCase() + "%")));
            Collection<GenericValue> pc = delegator.findList("Person", ecl, null, UtilMisc.toList("lastName", "firstName", "partyId"), null, false);

            if (Debug.infoOn()) Debug.logInfo("PartyFromPerson number found: " + pc.size(), module);
            if (pc != null) {
                for (GenericValue person: pc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", person.get("partyId"), "partyTypeId", "PERSON"));

                    parties.add(UtilMisc.<String, GenericValue>toMap("person", person, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0) {
            result.put("parties", parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from party group name.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartyFromPartyGroup(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = FastList.newInstance();
        String groupName = (String) context.get("groupName");
        Locale locale = (Locale) context.get("locale");

        if (groupName.length() == 0) {
            return ServiceUtil.returnError("Required parameter 'groupName' cannot be empty.");
        }

        try {
            EntityExpr ee = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("groupName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + groupName.toUpperCase() + "%"));
            Collection<GenericValue> pc = delegator.findList("PartyGroup", ee, null, UtilMisc.toList("groupName", "partyId"), null, false);

            if (Debug.infoOn()) Debug.logInfo("PartyFromGroup number found: " + pc.size(), module);
            if (pc != null) {
                for (GenericValue group: pc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", group.get("partyId"), "partyTypeId", "PARTY_GROUP"));

                    parties.add(UtilMisc.<String, GenericValue>toMap("partyGroup", group, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0) {
            result.put("parties", parties);
        }
        return result;
    }

    public static Map<String, Object> getPerson(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        GenericValue person = null;

        try {
            person = delegator.findByPrimaryKeyCache("Person", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Cannot get person entity (read failure): " + e.getMessage());
        }
        if (person != null) {
            result.put("lookupPerson", person);
        }
        return result;
    }

    public static Map<String, Object> createRoleType(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        GenericValue roleType = null;

        try {
            roleType = delegator.makeValue("RoleType");
            roleType.setPKFields(context);
            roleType.setNonPKFields(context);
            roleType = delegator.create(roleType);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Cannot create role type entity (write failure): " + e.getMessage());
        }
        if (roleType != null) {
            result.put("roleType", roleType);
        }
        return result;
    }

    public static Map<String, Object> createPartyDataSource(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();

        // input data
        String partyId = (String) context.get("partyId");
        String dataSourceId = (String) context.get("dataSourceId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) fromDate = UtilDateTime.nowTimestamp();

        try {
            // validate the existance of party and dataSource
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            GenericValue dataSource = delegator.findByPrimaryKey("DataSource", UtilMisc.toMap("dataSourceId", dataSourceId));
            if (party == null || dataSource == null) {
                List<String> errorList = UtilMisc.toList("Cannot create PartyDataSource");
                if (party == null) errorList.add("party with ID [" + partyId + "] was not found ");
                if (dataSource == null) errorList.add("data source with ID [" + dataSourceId + "] was not found ");
                return ServiceUtil.returnError(errorList);
            }

            // create the PartyDataSource
            GenericValue partyDataSource = delegator.makeValue("PartyDataSource", UtilMisc.toMap("partyId", partyId, "dataSourceId", dataSourceId, "fromDate", fromDate));
            partyDataSource.create();

        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> findParty(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String extInfo = (String) context.get("extInfo");

        // get the role types
        try {
            List<GenericValue> roleTypes = delegator.findList("RoleType", null, null, UtilMisc.toList("description"), null, false);
            result.put("roleTypes", roleTypes);
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up RoleTypes: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // current role type
        String roleTypeId;
        try {
            roleTypeId = (String) context.get("roleTypeId");
            if (UtilValidate.isNotEmpty(roleTypeId)) {
                GenericValue currentRole = delegator.findByPrimaryKeyCache("RoleType", UtilMisc.toMap("roleTypeId", roleTypeId));
                result.put("currentRole", currentRole);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current RoleType: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        //get party types
        try {
            List<GenericValue> partyTypes = delegator.findList("PartyType", null, null, UtilMisc.toList("description"), null, false);
            result.put("partyTypes", partyTypes);
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up PartyTypes: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // current party type
        String partyTypeId;
        try {
            partyTypeId = (String) context.get("partyTypeId");
            if (UtilValidate.isNotEmpty(partyTypeId)) {
                GenericValue currentPartyType = delegator.findByPrimaryKeyCache("PartyType", UtilMisc.toMap("partyTypeId", partyTypeId));
                result.put("currentPartyType", currentPartyType);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current RoleType: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // current state
        String stateProvinceGeoId;
        try {
            stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
            if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
                GenericValue currentStateGeo = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", stateProvinceGeoId));
                result.put("currentStateGeo", currentStateGeo);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current stateProvinceGeo: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // set the page parameters
        int viewIndex = 0;
        try {
            viewIndex = Integer.parseInt((String) context.get("VIEW_INDEX"));
        } catch (Exception e) {
            viewIndex = 0;
        }
        result.put("viewIndex", Integer.valueOf(viewIndex));

        int viewSize = 20;
        try {
            viewSize = Integer.parseInt((String) context.get("VIEW_SIZE"));
        } catch (Exception e) {
            viewSize = 20;
        }
        result.put("viewSize", Integer.valueOf(viewSize));

        // get the lookup flag
        String lookupFlag = (String) context.get("lookupFlag");

        // blank param list
        String paramList = "";

        List<GenericValue> partyList = null;
        int partyListSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        if ("Y".equals(lookupFlag)) {
            String showAll = (context.get("showAll") != null ? (String) context.get("showAll") : "N");
            paramList = paramList + "&lookupFlag=" + lookupFlag + "&showAll=" + showAll + "&extInfo=" + extInfo;

            // create the dynamic view entity
            DynamicViewEntity dynamicView = new DynamicViewEntity();

            // default view settings
            dynamicView.addMemberEntity("PT", "Party");
            dynamicView.addAlias("PT", "partyId");
            dynamicView.addAlias("PT", "statusId");
            dynamicView.addAlias("PT", "partyTypeId");
            dynamicView.addRelation("one-nofk", "", "PartyType", ModelKeyMap.makeKeyMapList("partyTypeId"));
            dynamicView.addRelation("many", "", "UserLogin", ModelKeyMap.makeKeyMapList("partyId"));

            // define the main condition & expression list
            List<EntityCondition> andExprs = FastList.newInstance();
            EntityCondition mainCond = null;

            List<String> orderBy = FastList.newInstance();
            List<String> fieldsToSelect = FastList.newInstance();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add("partyId");
            fieldsToSelect.add("statusId");
            fieldsToSelect.add("partyTypeId");

            // filter on parties that have relationship with logged in user
            String partyRelationshipTypeId = (String) context.get("partyRelationshipTypeId");
            if (UtilValidate.isNotEmpty(partyRelationshipTypeId)) {
                // add relation to view
                dynamicView.addMemberEntity("PRSHP", "PartyRelationship");
                dynamicView.addAlias("PRSHP", "partyIdTo");
                dynamicView.addAlias("PRSHP", "partyRelationshipTypeId");
                dynamicView.addViewLink("PT", "PRSHP", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId", "partyIdTo"));
                List<String> ownerPartyIds = UtilGenerics.cast(context.get("ownerPartyIds"));
                EntityCondition relationshipCond = null;
                if (UtilValidate.isEmpty(ownerPartyIds)) {
                    String partyIdFrom = userLogin.getString("partyId");
                    paramList = paramList + "&partyIdFrom=" + partyIdFrom;
                    relationshipCond = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyIdFrom"), EntityOperator.EQUALS, EntityFunction.UPPER(partyIdFrom));
                } else {
                    relationshipCond = EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, ownerPartyIds);
                }
                dynamicView.addAlias("PRSHP", "partyIdFrom");
                // add the expr
                andExprs.add(EntityCondition.makeCondition(
                        relationshipCond, EntityOperator.AND,
                        EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyRelationshipTypeId"), EntityOperator.EQUALS, EntityFunction.UPPER(partyRelationshipTypeId))));
                fieldsToSelect.add("partyIdTo");
            }

            // get the params
            String partyId = (String) context.get("partyId");
            String statusId = (String) context.get("statusId");
            String userLoginId = (String) context.get("userLoginId");
            String firstName = (String) context.get("firstName");
            String lastName = (String) context.get("lastName");
            String groupName = (String) context.get("groupName");

            if (!"Y".equals(showAll)) {
                // check for a partyId
                if (UtilValidate.isNotEmpty(partyId)) {
                    paramList = paramList + "&partyId=" + partyId;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyId"), EntityOperator.LIKE, EntityFunction.UPPER("%"+partyId+"%")));
                }

                // now the statusId - send ANY for all statuses; leave null for just enabled; or pass a specific status
                if (statusId != null) {
                    paramList = paramList + "&statusId=" + statusId;
                    if (!"ANY".equalsIgnoreCase(statusId)) {
                        andExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
                    }
                } else {
                    // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
                    andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
                }
                // check for partyTypeId
                if (partyTypeId != null && !"ANY".equals(partyTypeId)) {
                    paramList = paramList + "&partyTypeId=" + partyTypeId;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyTypeId"), EntityOperator.LIKE, EntityFunction.UPPER("%"+partyTypeId+"%")));
                }

                // ----
                // UserLogin Fields
                // ----

                // filter on user login
                if (UtilValidate.isNotEmpty(userLoginId)) {
                    paramList = paramList + "&userLoginId=" + userLoginId;

                    // modify the dynamic view
                    dynamicView.addMemberEntity("UL", "UserLogin");
                    dynamicView.addAlias("UL", "userLoginId");
                    dynamicView.addViewLink("PT", "UL", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("userLoginId"), EntityOperator.LIKE, EntityFunction.UPPER("%"+userLoginId+"%")));

                    fieldsToSelect.add("userLoginId");
                }

                // ----
                // PartyGroup Fields
                // ----

                // filter on groupName
                if (UtilValidate.isNotEmpty(groupName)) {
                    paramList = paramList + "&groupName=" + groupName;

                    // modify the dynamic view
                    dynamicView.addMemberEntity("PG", "PartyGroup");
                    dynamicView.addAlias("PG", "groupName");
                    dynamicView.addViewLink("PT", "PG", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("groupName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+groupName+"%")));

                    fieldsToSelect.add("groupName");
                }

                // ----
                // Person Fields
                // ----

                // modify the dynamic view
                if (UtilValidate.isNotEmpty(firstName) || UtilValidate.isNotEmpty(lastName)) {
                    dynamicView.addMemberEntity("PE", "Person");
                    dynamicView.addAlias("PE", "firstName");
                    dynamicView.addAlias("PE", "lastName");
                    dynamicView.addViewLink("PT", "PE", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    fieldsToSelect.add("firstName");
                    fieldsToSelect.add("lastName");
                    orderBy.add("lastName");
                    orderBy.add("firstName");
                }

                // filter on firstName
                if (UtilValidate.isNotEmpty(firstName)) {
                    paramList = paramList + "&firstName=" + firstName;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+firstName+"%")));
                }

                // filter on lastName
                if (UtilValidate.isNotEmpty(lastName)) {
                    paramList = paramList + "&lastName=" + lastName;
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+lastName+"%")));
                }

                // ----
                // RoleType Fields
                // ----

                // filter on role member
                if (roleTypeId != null && !"ANY".equals(roleTypeId)) {
                    paramList = paramList + "&roleTypeId=" + roleTypeId;

                    // add role to view
                    dynamicView.addMemberEntity("PR", "PartyRole");
                    dynamicView.addAlias("PR", "roleTypeId");
                    dynamicView.addViewLink("PT", "PR", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    // add the expr
                    andExprs.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, roleTypeId));

                    fieldsToSelect.add("roleTypeId");
                }

                // ----
                // InventoryItem Fields
                // ----

                // filter on inventory item's fields
                String inventoryItemId = (String) context.get("inventoryItemId");
                String serialNumber = (String) context.get("serialNumber");
                String softIdentifier = (String) context.get("softIdentifier");
                if (UtilValidate.isNotEmpty(inventoryItemId) ||
                    UtilValidate.isNotEmpty(serialNumber) ||
                    UtilValidate.isNotEmpty(softIdentifier)) {

                    // add role to view
                    dynamicView.addMemberEntity("II", "InventoryItem");
                    dynamicView.addAlias("II", "ownerPartyId");
                    dynamicView.addViewLink("PT", "II", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId", "ownerPartyId"));
                }
                if (UtilValidate.isNotEmpty(inventoryItemId)) {
                    paramList = paramList + "&inventoryItemId=" + inventoryItemId;
                    dynamicView.addAlias("II", "inventoryItemId");
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryItemId"), EntityOperator.LIKE, EntityFunction.UPPER("%" + inventoryItemId + "%")));
                    fieldsToSelect.add("inventoryItemId");
                }
                if (UtilValidate.isNotEmpty(serialNumber)) {
                    paramList = paramList + "&serialNumber=" + serialNumber;
                    dynamicView.addAlias("II", "serialNumber");
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("serialNumber"), EntityOperator.LIKE, EntityFunction.UPPER("%" + serialNumber + "%")));
                    fieldsToSelect.add("serialNumber");
                }
                if (UtilValidate.isNotEmpty(softIdentifier)) {
                    paramList = paramList + "&softIdentifier=" + softIdentifier;
                    dynamicView.addAlias("II", "softIdentifier");
                    // add the expr
                    andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("softIdentifier"), EntityOperator.LIKE, EntityFunction.UPPER("%" + softIdentifier + "%")));
                    fieldsToSelect.add("softIdentifier");
                }

                // ----
                // PostalAddress fields
                // ----
                if ("P".equals(extInfo)) {
                    // add address to dynamic view
                    dynamicView.addMemberEntity("PC", "PartyContactMech");
                    dynamicView.addMemberEntity("PA", "PostalAddress");
                    dynamicView.addAlias("PC", "contactMechId");
                    dynamicView.addAlias("PA", "address1");
                    dynamicView.addAlias("PA", "address2");
                    dynamicView.addAlias("PA", "city");
                    dynamicView.addAlias("PA", "stateProvinceGeoId");
                    dynamicView.addAlias("PA", "countryGeoId");
                    dynamicView.addAlias("PA", "postalCode");
                    dynamicView.addViewLink("PT", "PC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
                    dynamicView.addViewLink("PC", "PA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));

                    // filter on address1
                    String address1 = (String) context.get("address1");
                    if (UtilValidate.isNotEmpty(address1)) {
                        paramList = paramList + "&address1=" + address1;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address1"), EntityOperator.LIKE, EntityFunction.UPPER("%" + address1 + "%")));
                    }

                    // filter on address2
                    String address2 = (String) context.get("address2");
                    if (UtilValidate.isNotEmpty(address2)) {
                        paramList = paramList + "&address2=" + address2;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address2"), EntityOperator.LIKE, EntityFunction.UPPER("%" + address2 + "%")));
                    }

                    // filter on city
                    String city = (String) context.get("city");
                    if (UtilValidate.isNotEmpty(city)) {
                        paramList = paramList + "&city=" + city;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("city"), EntityOperator.LIKE, EntityFunction.UPPER("%" + city + "%")));
                    }

                    // filter on state geo
                    if (stateProvinceGeoId != null && !"ANY".equals(stateProvinceGeoId)) {
                        paramList = paramList + "&stateProvinceGeoId=" + stateProvinceGeoId;
                        andExprs.add(EntityCondition.makeCondition("stateProvinceGeoId", EntityOperator.EQUALS, stateProvinceGeoId));
                    }

                    // filter on postal code
                    String postalCode = (String) context.get("postalCode");
                    if (UtilValidate.isNotEmpty(postalCode)) {
                        paramList = paramList + "&postalCode=" + postalCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("postalCode"), EntityOperator.LIKE, EntityFunction.UPPER("%" + postalCode + "%")));
                    }

                    fieldsToSelect.add("postalCode");
                    fieldsToSelect.add("city");
                    fieldsToSelect.add("stateProvinceGeoId");
                }

                // ----
                // Generic CM Fields
                // ----
                if ("O".equals(extInfo)) {
                    // add info to dynamic view
                    dynamicView.addMemberEntity("PC", "PartyContactMech");
                    dynamicView.addMemberEntity("CM", "ContactMech");
                    dynamicView.addAlias("PC", "contactMechId");
                    dynamicView.addAlias("CM", "infoString");
                    dynamicView.addViewLink("PT", "PC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
                    dynamicView.addViewLink("PC", "CM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));

                    // filter on infoString
                    String infoString = (String) context.get("infoString");
                    if (UtilValidate.isNotEmpty(infoString)) {
                        paramList = paramList + "&infoString=" + infoString;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.LIKE, EntityFunction.UPPER("%"+infoString+"%")));
                        fieldsToSelect.add("infoString");
                    }

                }

                // ----
                // TelecomNumber Fields
                // ----
                if ("T".equals(extInfo)) {
                    // add telecom to dynamic view
                    dynamicView.addMemberEntity("PC", "PartyContactMech");
                    dynamicView.addMemberEntity("TM", "TelecomNumber");
                    dynamicView.addAlias("PC", "contactMechId");
                    dynamicView.addAlias("TM", "countryCode");
                    dynamicView.addAlias("TM", "areaCode");
                    dynamicView.addAlias("TM", "contactNumber");
                    dynamicView.addViewLink("PT", "PC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
                    dynamicView.addViewLink("PC", "TM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));

                    // filter on countryCode
                    String countryCode = (String) context.get("countryCode");
                    if (UtilValidate.isNotEmpty(countryCode)) {
                        paramList = paramList + "&countryCode=" + countryCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("countryCode"), EntityOperator.EQUALS, EntityFunction.UPPER(countryCode)));
                    }

                    // filter on areaCode
                    String areaCode = (String) context.get("areaCode");
                    if (UtilValidate.isNotEmpty(areaCode)) {
                        paramList = paramList + "&areaCode=" + areaCode;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("areaCode"), EntityOperator.EQUALS, EntityFunction.UPPER(areaCode)));
                    }

                    // filter on contact number
                    String contactNumber = (String) context.get("contactNumber");
                    if (UtilValidate.isNotEmpty(contactNumber)) {
                        paramList = paramList + "&contactNumber=" + contactNumber;
                        andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("contactNumber"), EntityOperator.EQUALS, EntityFunction.UPPER(contactNumber)));
                    }

                    fieldsToSelect.add("contactNumber");
                    fieldsToSelect.add("areaCode");
                }

                // ---- End of Dynamic View Creation

                // build the main condition
                if (andExprs.size() > 0) mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
            }

            Debug.logInfo("In findParty mainCond=" + mainCond, module);

            // do the lookup
            if (mainCond != null || "Y".equals(showAll)) {
                try {
                    // get the indexes for the partial list
                    lowIndex = viewIndex * viewSize + 1;
                    highIndex = (viewIndex + 1) * viewSize;

                    // set distinct on so we only get one row per order
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, -1, highIndex, true);
                    // using list iterator
                    EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, fieldsToSelect, orderBy, findOpts);

                    // get the partial list for this page
                    partyList = pli.getPartialList(lowIndex, viewSize);

                    // attempt to get the full size
                    partyListSize = pli.getResultsSizeAfterPartialList();
                    if (highIndex > partyListSize) {
                        highIndex = partyListSize;
                    }

                    // close the list iterator
                    pli.close();
                } catch (GenericEntityException e) {
                    String errMsg = "Failure in party find operation, rolling back transaction: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            } else {
                partyListSize = 0;
            }
        }

        if (partyList == null) partyList = FastList.newInstance();
        result.put("partyList", partyList);
        result.put("partyListSize", Integer.valueOf(partyListSize));
        result.put("paramList", paramList);
        result.put("highIndex", Integer.valueOf(highIndex));
        result.put("lowIndex", Integer.valueOf(lowIndex));

        return result;
    }

    /**
     * Changes the association of contact mechs, purposes, notes, orders and attributes from
     * one party to another for the purpose of merging records together. Flags the from party
     * as disabled so it no longer appears in a search.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> linkParty(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator _delegator = dctx.getDelegator();
        Delegator delegator = _delegator.cloneDelegator();
        delegator.setEntityEcaHandler(null);

        String partyIdTo = (String) context.get("partyIdTo");
        String partyId = (String) context.get("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();

        if (partyIdTo.equals(partyId)) {
            return ServiceUtil.returnError("Cannot link the same party with itself");
        }

        // get the from/to party records
        GenericValue partyTo;
        try {
            partyTo = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyIdTo));
        } catch (GenericEntityException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (partyTo == null) {
            return ServiceUtil.returnError("Party To does not exist!");
        }
        if ("PARTY_DISABLED".equals(partyTo.get("statusId"))) {
            return ServiceUtil.returnError("Cannot merge records into a disabled party!");
        }

        GenericValue party;
        try {
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.log(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (party == null) {
            return ServiceUtil.returnError("Party FROM does not exist!");
        }

        // update the contact mech records
        try {
            delegator.storeByCondition("PartyContactMech", UtilMisc.<String, Object>toMap("partyId", partyIdTo, "thruDate", now),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the contact mech purpose records
        try {
            delegator.storeByCondition("PartyContactMechPurpose", UtilMisc.<String, Object>toMap("partyId", partyIdTo, "thruDate", now),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the party notes
        try {
            delegator.storeByCondition("PartyNote", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the inventory item(s)
        try {
            delegator.storeByCondition("InventoryItem", UtilMisc.toMap("ownerPartyId", partyIdTo),
                    EntityCondition.makeCondition("ownerPartyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the subscription
        try {
            delegator.storeByCondition("Subscription", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the userLogin records
        try {
            delegator.storeByCondition("UserLogin", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the non-existing party roles
        List<GenericValue> rolesToMove;
        try {
            rolesToMove = delegator.findByAnd("PartyRole", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: rolesToMove) {
            attr.set("partyId", partyIdTo);
            try {
                if (delegator.findByPrimaryKey("PartyRole", attr.getPrimaryKey()) == null) {
                    attr.create();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // update the order role records
        try {
            delegator.storeByCondition("OrderRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // invoice role
        try {
            delegator.storeByCondition("InvoiceRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // data resource role
        try {
            delegator.storeByCondition("DataResourceRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // content role
        try {
            delegator.storeByCondition("ContentRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the fin account
        try {
            delegator.storeByCondition("FinAccountRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // TODO: there are a number of other places which may need to be updated

        // remove all previous party roles
        try {
            delegator.removeByAnd("PartyRole", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            // if this fails no problem
        }

        // update the non-existing attributes
        List<GenericValue> attrsToMove;
        try {
            attrsToMove = delegator.findByAnd("PartyAttribute", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: attrsToMove) {
            attr.set("partyId", partyIdTo);
            try {
                if (delegator.findByPrimaryKey("PartyAttribute", attr.getPrimaryKey()) == null) {
                    attr.create();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        try {
            delegator.removeByAnd("PartyAttribute", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // create a party link attribute
        GenericValue linkAttr = delegator.makeValue("PartyAttribute");
        linkAttr.set("partyId", partyId);
        linkAttr.set("attrName", "LINKED_TO");
        linkAttr.set("attrValue", partyIdTo);
        try {
            delegator.create(linkAttr);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // disable the party
        String currentStatus = party.getString("statusId");
        if (currentStatus == null || !"PARTY_DISABLED".equals(currentStatus)) {
            party.set("statusId", "PARTY_DISABLED");

            try {
                party.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error setting disable mode on partyId: " + partyId, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put("partyId", partyIdTo);
        return resp;
    }

    public static Map<String, Object> importAddressMatchMapCsv(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        String encoding = System.getProperty("file.encoding");
        String csvFile = Charset.forName(encoding).decode(fileBytes).toString();
        csvFile = csvFile.replaceAll("\\r", "");
        String[] records = csvFile.split("\\n");

        for (int i = 0; i < records.length; i++) {
            if (records[i] != null) {
                String str = records[i].trim();
                String[] map = str.split(",");
                if (map.length != 2 && map.length != 3) {
                    return ServiceUtil.returnError("Invalid format for CSV (key,value,sequence)");
                } else {
                    GenericValue addrMap = delegator.makeValue("AddressMatchMap");
                    addrMap.put("mapKey", map[0].trim().toUpperCase());
                    addrMap.put("mapValue", map[1].trim().toUpperCase());
                    int seq = i + 1;
                    if (map.length == 3) {
                        char[] chars = map[2].toCharArray();
                        boolean isNumber = true;
                        for (char c: chars) {
                            if (!Character.isDigit(c)) {
                                isNumber = false;
                            }
                        }
                        if (isNumber) {
                            try {
                                seq = Integer.parseInt(map[2]);
                            } catch (Throwable t) {
                                Debug.logWarning(t, "Unable to parse number", module);
                            }
                        }
                    }

                    addrMap.put("sequenceNum", Long.valueOf(seq));
                    Debug.log("Creating map entry: " + addrMap, module);
                    try {
                        delegator.create(addrMap);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            } else {
                return ServiceUtil.returnError("No records found in file");
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static String getPartyId(Map<String, ? extends Object> context) {
        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                partyId = userLogin.getString("partyId");
            }
        }
        return partyId;
    }


    /**
     * Finds partyId(s) corresponding to a party reference, partyId or a GoodIdentification idValue
     * @param dctx
     * @param context
     * @param context.partyId use to search with partyId or goodIdentification.idValue
     * @return a GenericValue with a partyId and a List of complementary partyId found
     */
    public static Map<String, Object> findPartyById(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        String idToFind = (String) context.get("idToFind");
        String partyIdentificationTypeId = (String) context.get("partyIdentificationTypeId");
        String searchPartyFirstContext = (String) context.get("searchPartyFirst");
        String searchAllIdContext = (String) context.get("searchAllId");

        boolean searchPartyFirst = UtilValidate.isNotEmpty(searchPartyFirstContext) && "N".equals(searchPartyFirstContext) ? false : true;
        boolean searchAllId = UtilValidate.isNotEmpty(searchAllIdContext)&& "Y".equals(searchAllIdContext) ? true : false;

        GenericValue party = null;
        List<GenericValue> partiesFound = null;
        try {
            partiesFound = PartyWorker.findPartiesById(delegator, idToFind, partyIdentificationTypeId, searchPartyFirst, searchAllId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            ServiceUtil.returnError(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(partiesFound)) {
            // gets the first partyId of the List
            party = EntityUtil.getFirst(partiesFound);
            // remove this partyId
            partiesFound.remove(0);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("party", party);
        result.put("partiesFound", partiesFound);

        return result;
    }

}
