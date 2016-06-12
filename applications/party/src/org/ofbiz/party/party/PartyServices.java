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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
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
    public static final String resource = "PartyUiLabels";
    public static final String resourceError = "PartyErrorUiLabels";

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

        return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                "partyservices.cannot_delete_party_not_implemented", locale));
    }

    /**
     * Creates a Person.
     * If no partyId is specified a numeric partyId is retrieved from the Party sequence.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> createPerson(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> toBeStored = new LinkedList<GenericValue>();
        Locale locale = (Locale) context.get("locale");
        // in most cases userLogin will be null, but get anyway so we can keep track of that info if it is available
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String description = (String) context.get("description");

        // if specified partyId starts with a number, return an error
        if (UtilValidate.isNotEmpty(partyId) && partyId.matches("\\d+")) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "party.id_is_digit", locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "party.id_generation_failure", locale));
            }
        }

        // check to see if party object exists, if so make sure it is PERSON type party
        GenericValue party = null;

        try {
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (party != null) {
            if (!"PERSON".equals(party.getString("partyTypeId"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "person.create.party_exists_not_person_type", locale)); 
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
            person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (person != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.create.person_exists", locale)); 
        }

        person = delegator.makeValue("Person", UtilMisc.toMap("partyId", partyId));
        person.setNonPKFields(context);
        toBeStored.add(person);

        try {
            delegator.storeAll(toBeStored);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.create.db_error", new Object[] { e.getMessage() }, locale)); 
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
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

            String oldStatusId = party.getString("statusId");
            if (!statusId.equals(oldStatusId)) {

                if (oldStatusId == null) { // old records
                    party.set("statusId", statusId);
                    oldStatusId = party.getString("statusId");
                } else {

                // check that status is defined as a valid change
                GenericValue statusValidChange = EntityQuery.use(delegator).from("StatusValidChange").where("statusId", party.getString("statusId"), "statusIdTo", statusId).queryOne();
                if (statusValidChange == null) {
                    String errorMsg = "Cannot change party status from " + party.getString("statusId") + " to " + statusId;
                    Debug.logWarning(errorMsg, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                            "PartyStatusCannotBeChanged", 
                            UtilMisc.toMap("partyFromStatusId", party.getString("statusId"), 
                            "partyToStatusId", statusId), locale)); 
                }

                party.set("statusId", statusId);
                }
                party.store();

                // record this status change in PartyStatus table
                GenericValue partyStatus = delegator.makeValue("PartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", statusId, "statusDate", statusDate));
                partyStatus.create();

                // disable all userlogins for this user when the new status is disabled
                if (("PARTY_DISABLED").equals(statusId)) {
                    List <GenericValue> userLogins = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryList();
                    for (GenericValue userLogin : userLogins) {
                        if (!"N".equals(userLogin.getString("enabled"))) {
                            userLogin.set("enabled", "N");
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.update.write_failure", new Object[] { e.getMessage() }, locale));
        }
    }

    /**
     * Updates a Person.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> updatePerson(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, 
                    "serviceUtil.party_id_missing", locale));
        }

        GenericValue person = null;
        GenericValue party = null;

        try {
            person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.update.read_failure", new Object[] { e.getMessage() }, locale));
        }

        if (person == null || party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.update.not_found", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "person.update.write_failure", new Object[] { e.getMessage() }, locale));
        }

        if (UtilValidate.isNotEmpty(context.get("statusId")) && !context.get("statusId").equals(oldStatusId)) {
            try {
                dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", context.get("statusId"), "userLogin", context.get("userLogin")));
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "person.update.write_failure", new Object[] { e.getMessage() }, locale));
            }
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        result.put(ModelService.SUCCESS_MESSAGE, 
                UtilProperties.getMessage(resourceError, "person.update.success", locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "partyservices.could_not_create_party_group_generation_failure", locale));
            }
        } else {
            // if specified partyId starts with a number, return an error
            if (partyId.matches("\\d+")) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "partyservices.could_not_create_party_ID_digit", locale));
            }
        }

        try {
            // check to see if party object exists, if so make sure it is PARTY_GROUP type party
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            GenericValue partyGroupPartyType = EntityQuery.use(delegator).from("PartyType").where("partyTypeId", "PARTY_GROUP").cache().queryOne();

            if (partyGroupPartyType == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "partyservices.partyservices.party_type_not_found_in_database_cannot_create_party_group", locale));
            }

            if (party != null) {
                GenericValue partyType = party.getRelatedOne("PartyType", true);

                if (!EntityTypeUtil.isType(partyType, partyGroupPartyType)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                            "partyservices.partyservices.cannot_create_party_group_already_exists_not_PARTY_GROUP_type", locale));
                }
            } else {
                // create a party if one doesn't already exist
                String partyTypeId = "PARTY_GROUP";

                if (UtilValidate.isNotEmpty(context.get("partyTypeId"))) {
                    GenericValue desiredPartyType = EntityQuery.use(delegator).from("PartyType").where("partyTypeId", context.get("partyTypeId")).cache().queryOne();
                    if (desiredPartyType != null && EntityTypeUtil.isType(desiredPartyType, partyGroupPartyType)) {
                        partyTypeId = desiredPartyType.getString("partyTypeId");
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                                "PartyPartyTypeIdNotFound", UtilMisc.toMap("partyTypeId", context.get("partyTypeId")), locale));
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

            GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
            if (partyGroup != null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "partyservices.cannot_create_party_group_already_exists", locale));
            }

            partyGroup = delegator.makeValue("PartyGroup", UtilMisc.toMap("partyId", partyId));
            partyGroup.setNonPKFields(context);
            partyGroup.create();

        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.data_source_error_adding_party_group", 
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String partyId = getPartyId(context);
        if (UtilValidate.isEmpty(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, 
                    "serviceUtil.party_id_missing", locale));
        }

        GenericValue partyGroup = null;
        GenericValue party = null;

        try {
            partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_party_information_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }

        if (partyGroup == null || party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_party_information_not_found", locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_party_information_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }

        if (UtilValidate.isNotEmpty(context.get("statusId")) && !context.get("statusId").equals(oldStatusId)) {
            try {
                dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", context.get("statusId"), "userLogin", context.get("userLogin")));
            } catch (GenericServiceException e) {
                Debug.logWarning(e.getMessage(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "person.update.write_failure", new Object[] { e.getMessage() }, locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = getPartyId(context);

        // if specified partyId starts with a number, return an error
        if (UtilValidate.isNotEmpty(partyId) && partyId.matches("\\d+")) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.cannot_create_affiliate_digit", locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (UtilValidate.isEmpty(partyId)) {
            try {
                partyId = delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "partyservices.cannot_create_affiliate_generation_failure", locale));
            }
        }

        // check to see if party object exists, if so make sure it is AFFILIATE type party
        GenericValue party = null;

        try {
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.cannot_create_affiliate_no_party_entity", locale));
        }

        GenericValue affiliate = null;

        try {
            affiliate = EntityQuery.use(delegator).from("Affiliate").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        if (affiliate != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.cannot_create_affiliate_ID_already_exists", locale));
        }

        affiliate = delegator.makeValue("Affiliate", UtilMisc.toMap("partyId", partyId));
        affiliate.setNonPKFields(context);
        affiliate.set("dateTimeCreated", now, false);

        try {
            delegator.create(affiliate);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_add_affiliate_info_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
            return ServiceUtil.returnError(UtilProperties.getMessage(ServiceUtil.resource, 
                    "serviceUtil.party_id_missing", locale));
        }

        GenericValue affiliate = null;

        try {
            affiliate = EntityQuery.use(delegator).from("Affiliate").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_affiliate_information_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }

        if (affiliate == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_affiliate_information_not_found", locale));
        }

        affiliate.setNonPKFields(context);

        try {
            affiliate.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "partyservices.could_not_update_affiliate_information_write",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteString = (String) context.get("note");
        String partyId = (String) context.get("partyId");
        String noteId = (String) context.get("noteId");
        String noteName = (String) context.get("noteName");
        Locale locale = (Locale) context.get("locale");
        //Map noteCtx = UtilMisc.toMap("note", noteString, "userLogin", userLogin);

        //Make sure the note Id actually exists if one is passed to avoid a foreign key error below
        if (noteId != null) {
            try {
                GenericValue value = EntityQuery.use(delegator).from("NoteData").where("noteId", noteId).queryOne();
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
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "PartyNoteCreationError", UtilMisc.toMap("errorString", e.getMessage()), locale));
            }

            if (noteRes.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
                return noteRes;

            noteId = (String) noteRes.get("noteId");

            if (UtilValidate.isEmpty(noteId)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "partyservices.problem_creating_note_no_noteId_returned", locale));
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
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, UtilProperties.getMessage(resourceError,
                    "partyservices.problem_associating_note_with_party", 
                    UtilMisc.toMap("errMessage", ee.getMessage()), locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<Map<String,GenericValue>>();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");

        if (email.length() == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.required_parameter_email_cannot_be_empty", locale));
        }

        try {
            List<GenericValue> c = EntityQuery.use(delegator).from("PartyAndContactMech")
                    .where(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.EQUALS, EntityFunction.UPPER(email.toUpperCase())))
                    .orderBy("infoString")
                    .filterByDate()
                    .queryList();

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (parties.size() > 0)
            result.put("parties", parties);
        return result;
    }

    public static Map<String, Object> getPartyFromEmail(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<Map<String,GenericValue>>();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");

        if (email.length() == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.required_parameter_email_cannot_be_empty", locale));
        }

        try {
            List<GenericValue> c = EntityQuery.use(delegator).from("PartyAndContactMech")
                    .where(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.LIKE, EntityFunction.UPPER(("%" + email.toUpperCase()) + "%")))
                    .orderBy("infoString")
                    .filterByDate()
                    .queryList();

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                for (GenericValue pacm: c) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<Map<String,GenericValue>>();
        String userLoginId = (String) context.get("userLoginId");
        Locale locale = (Locale) context.get("locale");

        if (userLoginId.length() == 0)
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotGetUserLoginFromParty", locale));

        try {
            Collection<GenericValue> ulc = EntityQuery.use(delegator).from("PartyAndUserLogin")
                    .where(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("userLoginId"), EntityOperator.LIKE, EntityFunction.UPPER("%" + userLoginId.toUpperCase() + "%")))
                    .orderBy("userLoginId")
                    .queryList();

            if (Debug.verboseOn()) Debug.logVerbose("Collection: " + ulc, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromUserLogin number found: " + ulc.size(), module);
            if (ulc != null) {
                for (GenericValue ul: ulc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", ul.get("partyId"), "partyTypeId", ul.get("partyTypeId")));

                    parties.add(UtilMisc.<String, GenericValue>toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read", 
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (UtilValidate.isNotEmpty(parties)) {
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<Map<String,GenericValue>>();
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
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.both_names_cannot_be_empty", locale));
        }

        try {
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + firstName.toUpperCase() + "%")),
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + lastName.toUpperCase() + "%")));
            Collection<GenericValue> pc = EntityQuery.use(delegator).from("Person").where(ecl).orderBy("lastName", "firstName", "partyId").queryList();

            if (Debug.infoOn()) Debug.logInfo("PartyFromPerson number found: " + pc.size(), module);
            if (pc != null) {
                for (GenericValue person: pc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", person.get("partyId"), "partyTypeId", "PERSON"));

                    parties.add(UtilMisc.<String, GenericValue>toMap("person", person, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
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
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Collection<Map<String, GenericValue>> parties = new LinkedList<Map<String,GenericValue>>();
        String groupName = (String) context.get("groupName");
        Locale locale = (Locale) context.get("locale");

        if (groupName.length() == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotGetPartyFromPartyGroup", locale));
        }

        try {
            Collection<GenericValue> pc = EntityQuery.use(delegator).from("PartyGroup")
                    .where(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("groupName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + groupName.toUpperCase() + "%")))
                    .orderBy("groupName", "partyId")
                    .queryList();

            if (Debug.infoOn()) Debug.logInfo("PartyFromGroup number found: " + pc.size(), module);
            if (pc != null) {
                for (GenericValue group: pc) {
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", group.get("partyId"), "partyTypeId", "PARTY_GROUP"));

                    parties.add(UtilMisc.<String, GenericValue>toMap("partyGroup", group, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (parties.size() > 0) {
            result.put("parties", parties);
        }
        return result;
    }

    /**
     * Get the party object(s) from party externalId.
     * @param dctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> getPartyFromExternalId(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> parties = new ArrayList<>();
        String externalId = (String) context.get("externalId");
        Locale locale = (Locale) context.get("locale");

        try {
        	parties = EntityQuery.use(delegator).from("Party")
                    .where(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("externalId"), EntityOperator.EQUALS, EntityFunction.UPPER(externalId)))
                    .orderBy("externalId", "partyId")
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        result.put("parties", parties);
        return result;
    }

    public static Map<String, Object> getPerson(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        GenericValue person = null;

        try {
            person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).cache().queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "partyservices.cannot_get_party_entities_read",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (person != null) {
            result.put("lookupPerson", person);
        }
        return result;
    }

    public static Map<String, Object> createRoleType(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue roleType = null;

        try {
            roleType = delegator.makeValue("RoleType");
            roleType.setPKFields(context);
            roleType.setNonPKFields(context);
            roleType = delegator.create(roleType);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotCreateRoleTypeEntity",
                    UtilMisc.toMap("errMessage", e.getMessage()), locale));
        }
        if (roleType != null) {
            result.put("roleType", roleType);
        }
        return result;
    }

    public static Map<String, Object> createPartyDataSource(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        
        // input data
        String partyId = (String) context.get("partyId");
        String dataSourceId = (String) context.get("dataSourceId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) fromDate = UtilDateTime.nowTimestamp();

        try {
            // validate the existance of party and dataSource
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            GenericValue dataSource = EntityQuery.use(delegator).from("DataSource").where("dataSourceId", dataSourceId).queryOne();
            if (party == null || dataSource == null) {
                List<String> errorList = UtilMisc.toList(UtilProperties.getMessage(resource, 
                        "PartyCannotCreatePartyDataSource", locale));
                if (party == null) {
                    errorList.add(UtilProperties.getMessage(resource, 
                            "PartyNoPartyFoundWithPartyId", locale) + partyId);
                }
                if (dataSource == null) {
                    errorList.add(UtilProperties.getMessage(resource, 
                            "PartyNoPartyWithDataSourceId",
                            UtilMisc.toMap("dataSourceId", dataSourceId), locale));
                }
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

    @Deprecated // migration from ftl to widget in process.
    public static Map<String, Object> findParty(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String extInfo = (String) context.get("extInfo");

        // get the role types
        try {
            List<GenericValue> roleTypes = EntityQuery.use(delegator).from("RoleType").orderBy("description").queryList();
            result.put("roleTypes", roleTypes);
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up RoleTypes: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyLookupRoleTypeError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        // current role type
        String roleTypeId;
        try {
            roleTypeId = (String) context.get("roleTypeId");
            if (UtilValidate.isNotEmpty(roleTypeId)) {
                GenericValue currentRole = EntityQuery.use(delegator).from("RoleType").where("roleTypeId", roleTypeId).cache().queryOne();
                result.put("currentRole", currentRole);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current RoleType: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyLookupRoleTypeError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        //get party types
        try {
            List<GenericValue> partyTypes = EntityQuery.use(delegator).from("PartyType").orderBy("description").queryList();
            result.put("partyTypes", partyTypes);
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up PartyTypes: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyLookupPartyTypeError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        // current party type
        String partyTypeId;
        try {
            partyTypeId = (String) context.get("partyTypeId");
            if (UtilValidate.isNotEmpty(partyTypeId)) {
                GenericValue currentPartyType = EntityQuery.use(delegator).from("PartyType").where("partyTypeId", partyTypeId).cache().queryOne();
                result.put("currentPartyType", currentPartyType);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current PartyType: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyLookupPartyTypeError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        // current state
        String stateProvinceGeoId;
        try {
            stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
            if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
                GenericValue currentStateGeo = EntityQuery.use(delegator).from("Geo").where("geoId", stateProvinceGeoId).cache().queryOne();
                result.put("currentStateGeo", currentStateGeo);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up current stateProvinceGeo: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyLookupStateProvinceGeoError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
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
            dynamicView.addAlias("PT", "createdDate");
            dynamicView.addAlias("PT", "lastModifiedDate");
            dynamicView.addRelation("one-nofk", "", "PartyType", ModelKeyMap.makeKeyMapList("partyTypeId"));
            dynamicView.addRelation("many", "", "UserLogin", ModelKeyMap.makeKeyMapList("partyId"));

            // define the main condition & expression list
            List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
            EntityCondition mainCond = null;

            List<String> orderBy = new LinkedList<String>();
            List<String> fieldsToSelect = new LinkedList<String>();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add("partyId");
            fieldsToSelect.add("statusId");
            fieldsToSelect.add("partyTypeId");
            fieldsToSelect.add("createdDate");
            fieldsToSelect.add("lastModifiedDate");

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

            String sortField = (String) context.get("sortField");
            if(UtilValidate.isNotEmpty(sortField)){
                orderBy.add(sortField);
            }
            
            // do the lookup
            if (mainCond != null || "Y".equals(showAll)) {
                try {
                    // get the indexes for the partial list
                    lowIndex = viewIndex * viewSize + 1;
                    highIndex = (viewIndex + 1) * viewSize;

                    // set distinct on so we only get one row per order
                    // using list iterator
                    EntityListIterator pli = EntityQuery.use(delegator).select(UtilMisc.toSet(fieldsToSelect))
                            .from(dynamicView)
                            .where(mainCond)
                            .orderBy(orderBy)
                            .cursorScrollInsensitive()
                            .fetchSize(highIndex)
                            .distinct()
                            .queryIterator();

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
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "PartyLookupPartyError",
                            UtilMisc.toMap("errMessage", e.toString()), locale));
                }
            } else {
                partyListSize = 0;
            }
        }

        if (partyList == null) partyList = new LinkedList<GenericValue>();
        result.put("partyList", partyList);
        result.put("partyListSize", Integer.valueOf(partyListSize));
        result.put("paramList", paramList);
        result.put("highIndex", Integer.valueOf(highIndex));
        result.put("lowIndex", Integer.valueOf(lowIndex));

        return result;
    }

    public static Map<String, Object> performFindParty(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String extInfo = (String) context.get("extInfo");
        EntityCondition extCond = (EntityCondition) context.get("extCond");
        EntityListIterator listIt = null;

        // get the lookup flag
        String noConditionFind = (String) context.get("noConditionFind");

        // create the dynamic view entity
        DynamicViewEntity dynamicView = new DynamicViewEntity();

        // default view settings
        dynamicView.addMemberEntity("PT", "Party");
        dynamicView.addAlias("PT", "partyId");
        dynamicView.addAlias("PT", "statusId");
        dynamicView.addAlias("PT", "partyTypeId");
        dynamicView.addAlias("PT", "externalId");
        dynamicView.addAlias("PT", "createdDate");
        dynamicView.addAlias("PT", "lastModifiedDate");
        dynamicView.addRelation("one-nofk", "", "PartyType", ModelKeyMap.makeKeyMapList("partyTypeId"));
        dynamicView.addRelation("many", "", "UserLogin", ModelKeyMap.makeKeyMapList("partyId"));

        // define the main condition & expression list
        List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
        EntityCondition mainCond = null;

        List<String> orderBy = new ArrayList<String>();
        String sortField = (String) context.get("sortField");
        if(UtilValidate.isNotEmpty(sortField)){
            orderBy.add(sortField);
        }
        List<String> fieldsToSelect = new ArrayList<String>();
        // fields we need to select; will be used to set distinct
        fieldsToSelect.add("partyId");
        fieldsToSelect.add("statusId");
        fieldsToSelect.add("partyTypeId");
        fieldsToSelect.add("externalId");
        fieldsToSelect.add("createdDate");
        fieldsToSelect.add("lastModifiedDate");

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
        String partyTypeId = (String) context.get("partyTypeId");
        String roleTypeId = (String) context.get("roleTypeId");
        String statusId = (String) context.get("statusId");
        String userLoginId = (String) context.get("userLoginId");
        String externalId = (String) context.get("externalId");
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        String groupName = (String) context.get("groupName");

        // check for a partyId
        if (UtilValidate.isNotEmpty(partyId)) {
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyId"), EntityOperator.LIKE, EntityFunction.UPPER("%"+partyId+"%")));
        }

        // now the statusId - send ANY for all statuses; leave null for just enabled; or pass a specific status
        if (UtilValidate.isNotEmpty(statusId)) {
            andExprs.add(EntityCondition.makeCondition("statusId", statusId));
        } else {
            // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
            andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", GenericEntity.NULL_FIELD), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
        }
        // check for partyTypeId
        if (UtilValidate.isNotEmpty(partyTypeId)) {
            andExprs.add(EntityCondition.makeCondition("partyTypeId", partyTypeId));
        }

        if (UtilValidate.isNotEmpty(externalId)) {
            andExprs.add(EntityCondition.makeCondition("externalId", externalId));
        }
        // ----
        // UserLogin Fields
        // ----

        // filter on user login
        if (UtilValidate.isNotEmpty(userLoginId)) {

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
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+firstName+"%")));
        }

        // filter on lastName
        if (UtilValidate.isNotEmpty(lastName)) {
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+lastName+"%")));
        }

        // ----
        // RoleType Fields
        // ----

        // filter on role member
        if (UtilValidate.isNotEmpty(roleTypeId)) {

            // add role to view
            dynamicView.addMemberEntity("PR", "PartyRole");
            dynamicView.addAlias("PR", "roleTypeId");
            dynamicView.addViewLink("PT", "PR", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

            // add the expr
            andExprs.add(EntityCondition.makeCondition("roleTypeId", roleTypeId));
            fieldsToSelect.add("roleTypeId");
        }

        // ----
        // PartyIdentification Fields
        // ----

        String idValue = (String) context.get("idValue");
        String partyIdentificationTypeId = (String) context.get("partyIdentificationTypeId");
        if ("I".equals(extInfo) ||
                UtilValidate.isNotEmpty(idValue) ||
                UtilValidate.isNotEmpty(partyIdentificationTypeId)) {

            // add role to view
            dynamicView.addMemberEntity("PAI", "PartyIdentification");
            dynamicView.addAlias("PAI", "idValue");
            dynamicView.addAlias("PAI", "partyIdentificationTypeId");
            dynamicView.addViewLink("PT", "PAI", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
            fieldsToSelect.add("idValue");
            fieldsToSelect.add("partyIdentificationTypeId");
            if (UtilValidate.isNotEmpty(idValue)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.LIKE, EntityFunction.UPPER("%".concat(idValue).concat("%"))));
            }
            if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
                andExprs.add(EntityCondition.makeCondition("partyIdentificationTypeId", partyIdentificationTypeId));
            }
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
            dynamicView.addAlias("II", "inventoryItemId");
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryItemId"), EntityOperator.LIKE, EntityFunction.UPPER("%" + inventoryItemId + "%")));
            fieldsToSelect.add("inventoryItemId");
        }
        if (UtilValidate.isNotEmpty(serialNumber)) {
            dynamicView.addAlias("II", "serialNumber");
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("serialNumber"), EntityOperator.LIKE, EntityFunction.UPPER("%" + serialNumber + "%")));
            fieldsToSelect.add("serialNumber");
        }
        if (UtilValidate.isNotEmpty(softIdentifier)) {
            dynamicView.addAlias("II", "softIdentifier");
            // add the expr
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("softIdentifier"), EntityOperator.LIKE, EntityFunction.UPPER("%" + softIdentifier + "%")));
            fieldsToSelect.add("softIdentifier");
        }

        // ----
        // PostalAddress fields
        // ----
        String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
        if ( "P".equals(extInfo) ||
                UtilValidate.isNotEmpty(context.get("address1"))|| UtilValidate.isNotEmpty(context.get("address2"))||
                UtilValidate.isNotEmpty(context.get("city"))|| UtilValidate.isNotEmpty(context.get("postalCode"))||
                UtilValidate.isNotEmpty(context.get("countryGeoId"))|| (UtilValidate.isNotEmpty(stateProvinceGeoId))) {
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
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address1"), EntityOperator.LIKE, EntityFunction.UPPER("%" + address1 + "%")));
            }

            // filter on address2
            String address2 = (String) context.get("address2");
            if (UtilValidate.isNotEmpty(address2)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("address2"), EntityOperator.LIKE, EntityFunction.UPPER("%" + address2 + "%")));
            }

            // filter on city
            String city = (String) context.get("city");
            if (UtilValidate.isNotEmpty(city)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("city"), EntityOperator.LIKE, EntityFunction.UPPER("%" + city + "%")));
            }

            // filter on state geo
            if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
                andExprs.add(EntityCondition.makeCondition("stateProvinceGeoId", stateProvinceGeoId));
            }

            // filter on postal code
            String postalCode = (String) context.get("postalCode");
            if (UtilValidate.isNotEmpty(postalCode)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("postalCode"), EntityOperator.LIKE, EntityFunction.UPPER("%" + postalCode + "%")));
            }

            fieldsToSelect.add("postalCode");
            fieldsToSelect.add("city");
            fieldsToSelect.add("stateProvinceGeoId");
        }

        // ----
        // Generic CM Fields
        // ----
        if ("O".equals(extInfo) || UtilValidate.isNotEmpty(context.get("infoString"))) {
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
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityOperator.LIKE, EntityFunction.UPPER("%"+infoString+"%")));
                fieldsToSelect.add("infoString");
            }
        }

        // ----
        // TelecomNumber Fields
        // ----
        if ("T".equals(extInfo)
                || UtilValidate.isNotEmpty(context.get("countryCode"))
                || UtilValidate.isNotEmpty(context.get("areaCode"))
                || UtilValidate.isNotEmpty(context.get("contactNumber"))) {
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
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("countryCode"), EntityOperator.EQUALS, EntityFunction.UPPER(countryCode)));
            }

            // filter on areaCode
            String areaCode = (String) context.get("areaCode");
            if (UtilValidate.isNotEmpty(areaCode)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("areaCode"), EntityOperator.EQUALS, EntityFunction.UPPER(areaCode)));
            }

            // filter on contact number
            String contactNumber = (String) context.get("contactNumber");
            if (UtilValidate.isNotEmpty(contactNumber)) {
                andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("contactNumber"), EntityOperator.EQUALS, EntityFunction.UPPER(contactNumber)));
            }
            fieldsToSelect.add("contactNumber");
            fieldsToSelect.add("areaCode");
        }
        // ---- End of Dynamic View Creation

        // build the main condition, add the extend condition is it present
        if (UtilValidate.isNotEmpty(extCond)) andExprs.add(extCond);
        if (UtilValidate.isNotEmpty(andExprs)) mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
        if (Debug.infoOn()) Debug.logInfo("In findParty mainCond=" + mainCond, module);

        // do the lookup
        if (UtilValidate.isNotEmpty(noConditionFind) &&
                ("Y".equals(noConditionFind) || andExprs.size()>1)) { //exclude on condition the status expr
            try {
                // set distinct on so we only get one row per party
                // using list iterator
                listIt = EntityQuery.use(delegator).select(UtilMisc.toSet(fieldsToSelect))
                        .from(dynamicView)
                        .where(mainCond)
                        .orderBy(orderBy)
                        .cursorScrollInsensitive()
                        .distinct()
                        .queryIterator();
            } catch (GenericEntityException e) {
                String errMsg = "Failure in party find operation, rolling back transaction: " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "PartyLookupPartyError",
                        UtilMisc.toMap("errMessage", e.toString()), locale));
            }
        }
        result.put("listIt", listIt);
        return result;
    }

    /**
     * Changes the association of contact mechs, purposes, notes, orders and attributes from
     * one party to another for the purpose of merging records together. Flags the from party
     * as disabled so it no longer appears in a search.
     *
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     */
    public static Map<String, Object> linkParty(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator _delegator = dctx.getDelegator();
        Delegator delegator = _delegator.cloneDelegator();
        Locale locale = (Locale) context.get("locale");
        delegator.setEntityEcaHandler(null);

        String partyIdTo = (String) context.get("partyIdTo");
        String partyId = (String) context.get("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();

        if (partyIdTo.equals(partyId)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotLinkPartyToItSelf", locale));
        }

        // get the from/to party records
        GenericValue partyTo;
        try {
            partyTo = EntityQuery.use(delegator).from("Party").where("partyId", partyIdTo).queryOne();
        } catch (GenericEntityException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (partyTo == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyPartyToDoesNotExists", locale));
        }
        if ("PARTY_DISABLED".equals(partyTo.get("statusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyCannotMergeDisabledParty", locale));
        }

        GenericValue party;
        try {
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logInfo(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (party == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "PartyPartyFromDoesNotExists", locale));
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
            rolesToMove = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: rolesToMove) {
            attr.set("partyId", partyIdTo);
            try {
                if (EntityQuery.use(delegator).from("PartyRole").where(attr.getPrimaryKey()).queryOne() == null) {
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

        // update the Product Store Role records
        try {
            delegator.storeByCondition("ProductStoreRole", UtilMisc.<String, Object>toMap("partyId", partyIdTo, "thruDate", now),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        //  update the Communication Event Role records
        try {
            delegator.storeByCondition("CommunicationEventRole", UtilMisc.toMap("partyId", partyIdTo),
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

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
            attrsToMove = EntityQuery.use(delegator).from("PartyAttribute").where("partyId", partyId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        for (GenericValue attr: attrsToMove) {
            attr.set("partyId", partyIdTo);
            try {
                if (EntityQuery.use(delegator).from("PartyAttribute").where(attr.getPrimaryKey()).queryOne() == null) {
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
        Locale locale = (Locale) context.get("locale");
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
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                            "PartyImportInvalidCsvFile", locale));
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
                    Debug.logInfo("Creating map entry: " + addrMap, module);
                    try {
                        delegator.create(addrMap);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "PartyImportNoRecordsFoundInFile", locale));
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
     * @param ctx the dispatch context
     * @param context use to search with partyId or goodIdentification.idValue
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
            return ServiceUtil.returnError(e.getMessage());
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

    public static Map<String, Object> importParty(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        String encoding = System.getProperty("file.encoding");
        String csvString = Charset.forName(encoding).decode(fileBytes).toString();
        final BufferedReader csvReader = new BufferedReader(new StringReader(csvString));
        CSVFormat fmt = CSVFormat.DEFAULT.withHeader();
        List<String> errMsgs = new LinkedList<String>();
        List<String> newErrMsgs = new LinkedList<String>();
        String lastPartyId = null;        // last partyId read from the csv file
        String currentPartyId = null;     // current partyId from the csv file
        String newPartyId = null;        // new to create/update partyId in the system
        String newCompanyPartyId = null;
        int partiesCreated = 0;
        Map<String, Object> result = null;
        String newContactMechId = null;
        String currentContactMechTypeId = null;

        String lastAddress1 = null;
        String lastAddress2 = null;
        String lastCity = null;
        String lastCountryGeoId = null;

        String lastEmailAddress = null;

        String lastCountryCode = null;
        String lastAreaCode = null;
        String lastContactNumber = null;
        
        String lastContactMechPurposeTypeId = null;
        String currentContactMechPurposeTypeId = null;
        
        Boolean addParty = false; // when modify party, contact mech not added again
        
        if (fileBytes == null) {
            return ServiceUtil.returnError("Uploaded file data not found");
        }
        
        try {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
                if (UtilValidate.isNotEmpty(rec.get("partyId"))) {
                    currentPartyId =  rec.get("partyId");
                }
                if (lastPartyId == null || !currentPartyId.equals(lastPartyId)) {
                    newPartyId = null;
                    currentContactMechPurposeTypeId = null;
                    lastAddress1 = null;
                    lastAddress2 = null;
                    lastCity = null;
                    lastCountryGeoId = null;

                    lastEmailAddress = null;

                    lastCountryCode = null;
                    lastAreaCode = null;
                    lastContactNumber = null;
                    
                    // party validation
                    List <GenericValue> currencyCheck = EntityQuery.use(delegator).from("Uom")
                            .where("abbreviation", rec.get("preferredCurrencyUomId"), "uomTypeId", "CURRENCY_MEASURE")
                            .queryList();
                    if (UtilValidate.isNotEmpty(rec.get("preferredCurrencyUomId")) && currencyCheck.size() == 0) {
                        newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyId: " + currentPartyId + "Currency code not found for: " + rec.get("preferredCurrencyUomId"));
                    }

                    if (UtilValidate.isEmpty(rec.get("roleTypeId"))) {
                        newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory roletype is missing, possible values: CUSTOMER, SUPPLIER, EMPLOYEE and more....");
                    } else if (EntityQuery.use(delegator).from("RoleType").where("roleTypeId", rec.get("roleTypeId")).queryOne() == null) {
                        newErrMsgs.add("Line number " + rec.getRecordNumber() + ": RoletypeId is not valid: " + rec.get("roleTypeId") );
                    }

                    if (UtilValidate.isNotEmpty(rec.get("contactMechTypeId")) &&
                            EntityQuery.use(delegator).from("ContactMechType").where("contactMechTypeId", rec.get("contactMechTypeId")).cache().queryOne() == null) {
                        newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyId: " + currentPartyId + " contactMechTypeId code not found for: " + rec.get("contactMechTypeId"));
                    }
                    
                    if (UtilValidate.isNotEmpty(rec.get("contactMechPurposeTypeId")) &&
                            EntityQuery.use(delegator).from("ContactMechPurposeType").where("contactMechPurposeTypeId", rec.get("contactMechPurposeTypeId")).cache().queryOne() == null) {
                        newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyId: " + currentPartyId + "contactMechPurposeTypeId code not found for: " + rec.get("contactMechPurposeTypeId"));
                    }
                    
                    if (UtilValidate.isNotEmpty(rec.get("contactMechTypeId")) && "POSTAL_ADDRESS".equals(rec.get("contactMechTypeId"))) {
                        if (UtilValidate.isEmpty(rec.get("countryGeoId"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyId: " + currentPartyId + "Country code missing");
                        } else {
                            List <GenericValue> countryCheck = EntityQuery.use(delegator).from("Geo")
                                    .where("geoTypeId", "COUNTRY", "abbreviation", rec.get("countryGeoId"))
                                    .queryList();
                            if (countryCheck.size() == 0) {
                                newErrMsgs.add("Line number " + rec.getRecordNumber() + " partyId: " + currentPartyId + " Invalid Country code: " + rec.get("countryGeoId"));
                            }
                        }

                        if (UtilValidate.isEmpty(rec.get("city"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + " partyId: " + currentPartyId + "City name is missing");
                        } 

                        if (UtilValidate.isNotEmpty(rec.get("stateProvinceGeoId"))) {
                            List <GenericValue> stateCheck = EntityQuery.use(delegator).from("Geo")
                                    .where("geoTypeId", "STATE", "abbreviation", rec.get("stateProvinceGeoId"))
                                    .queryList();
                            if (stateCheck.size() == 0) {
                                newErrMsgs.add("Line number " + rec.getRecordNumber() + " partyId: " + currentPartyId + " Invalid stateProvinceGeoId code: " + rec.get("countryGeoId"));
                            }
                        }
                    }

                    if (UtilValidate.isNotEmpty(rec.get("contactMechTypeId")) && "TELECOM_NUMBER".equals(rec.get("contactMechTypeId"))) {
                        if (UtilValidate.isEmpty(rec.get("telAreaCode")) && UtilValidate.isEmpty(rec.get("telAreaCode"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + " partyId: " + currentPartyId + " telephone number missing");
                        }
                    }
          
                    if (UtilValidate.isNotEmpty(rec.get("contactMechTypeId")) && "EMAIL_ADDRESS".equals(rec.get("contactMechTypeId"))) {
                        if (UtilValidate.isEmpty(rec.get("emailAddress"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + " partyId: " + currentPartyId + " email address missing");
                        }
                    }
          
                    if (errMsgs.size() == 0) {
                        List <GenericValue> partyCheck = EntityQuery.use(delegator).from("PartyIdentification")
                                .where("partyIdentificationTypeId", "PARTY_IMPORT", "idValue", rec.get("partyId"))
                                .queryList();
                        addParty = partyCheck.size() == 0;
                        if (!addParty) { // update party
                            newPartyId = EntityUtil.getFirst(partyCheck).getString("partyId");
                            
                            if (UtilValidate.isNotEmpty(rec.get("groupName"))) {
                                Map<String, Object> partyGroup = UtilMisc.toMap(
                                        "partyId", newPartyId,
                                        "preferredCurrencyUomId", rec.get("preferredCurrencyUomId"),
                                        "groupName", rec.get("groupName"),
                                        "userLogin", userLogin
                                        );                   
                                result = dispatcher.runSync("updatePartyGroup", partyGroup);
                            } else { // person
                                Map<String, Object> person = UtilMisc.toMap(
                                        "partyId", newPartyId,
                                        "firstName", rec.get("firstName"),
                                        "middleName", rec.get("midleName"),
                                        "lastName", rec.get("lastName"),
                                        "preferredCurrencyUomId", rec.get("preferredCurrencyUomId"),
                                        "userLogin", userLogin
                                        );                   
                                result = dispatcher.runSync("updatePerson", person);
                            }
                            
                        } else { // create new party
                            if (UtilValidate.isNotEmpty(rec.get("groupName"))) {
                                Map<String, Object> partyGroup = UtilMisc.toMap(
                                        "preferredCurrencyUomId", rec.get("preferredCurrencyUomId"),
                                        "groupName", rec.get("groupName"),
                                        "userLogin", userLogin,
                                        "statusId", "PARTY_ENABLED"
                                        );                   
                                result = dispatcher.runSync("createPartyGroup", partyGroup);
                            } else { // person
                                Map<String, Object> person = UtilMisc.toMap(
                                        "firstName", rec.get("firstName"),
                                        "middleName", rec.get("middleName"),
                                        "lastName", rec.get("lastName"),
                                        "preferredCurrencyUomId", rec.get("preferredCurrencyUomId"),
                                        "statusId", "PARTY_ENABLED",
                                        "userLogin", userLogin
                                        );                   
                                result = dispatcher.runSync("createPerson", person);
                            }
                            newPartyId = (String) result.get("partyId");

                            Map<String, Object> partyIdentification = UtilMisc.toMap(
                                "partyId", newPartyId,
                                "partyIdentificationTypeId", "PARTY_IMPORT", 
                                "idValue", rec.get("partyId"),
                                "userLogin", userLogin
                                );

                            result = dispatcher.runSync("createPartyIdentification", partyIdentification);

                            Map<String, Object> partyRole = UtilMisc.toMap(
                                    "partyId", newPartyId,
                                    "roleTypeId", rec.get("roleTypeId"), 
                                    "userLogin", userLogin
                                    );
                            dispatcher.runSync("createPartyRole", partyRole);

                            if (UtilValidate.isNotEmpty(rec.get("companyPartyId"))) {
                                List <GenericValue> companyCheck = EntityQuery.use(delegator).from("PartyIdentification")
                                        .where("partyIdentificationTypeId", "PARTY_IMPORT", "idValue", rec.get("partyId"))
                                        .queryList();
                                if (companyCheck.size() == 0) { // update party group
                                    // company does not exist so create
                                    Map<String, Object> companyPartyGroup = UtilMisc.toMap(
                                        "partyId", newCompanyPartyId, 
                                        "statusId", "PARTY_ENABLED",
                                        "userLogin", userLogin
                                        );                   
                                    result = dispatcher.runSync("createPartyGroup", companyPartyGroup);
                                    newCompanyPartyId = (String) result.get("partyId");
                                } else {
                                    newCompanyPartyId = EntityUtil.getFirst(companyCheck).getString("partyId");
                                }

                                Map<String, Object> companyRole = UtilMisc.toMap(
                                        "partyId", newCompanyPartyId,
                                        "roleTypeId", "ACCOUNT", 
                                        "userLogin", userLogin
                                        );
                                dispatcher.runSync("createPartyRole", companyRole);
                                
                                // company exist, so create link
                                Map<String, Object> partyRelationship = UtilMisc.toMap(
                                    "partyIdTo", newPartyId,
                                    "partyIdFrom", newCompanyPartyId,
                                    "roleTypeIdFrom", "ACCOUNT",
                                    "partyRelationshipTypeId", "EMPLOYMENT",
                                    "userLogin", userLogin
                                    );                   
                                result = dispatcher.runSync("createPartyRelationship", partyRelationship);
                            }
                        }
                        Debug.logInfo(" =========================================================party created id: " + newPartyId, module);
                        partiesCreated++;
                    } else {
                        errMsgs.addAll(newErrMsgs);
                        newErrMsgs = new LinkedList<String>();
                    }
                }
                
                currentContactMechTypeId = rec.get("contactMechTypeId");
                currentContactMechPurposeTypeId = rec.get("contactMechPurposeTypeId"); 
                // party correctly created (not updated) and contactMechtype provided?
                if (newPartyId != null && addParty && UtilValidate.isNotEmpty(currentContactMechTypeId)) {
                                        
                    // fill maps and check changes
                    Map<String, Object> emailAddress = UtilMisc.toMap(
                            "contactMechTypeId", "EMAIL_ADDRESS", 
                            "userLogin", userLogin
                            );
                    Boolean emailAddressChanged = false;
                    if ("EMAIL_ADDRESS".equals(currentContactMechTypeId)) {
                        emailAddress.put("infoString", rec.get("emailAddress"));
                        emailAddressChanged = lastEmailAddress == null || !lastEmailAddress.equals(rec.get("emailAddress"));
                        lastEmailAddress = rec.get("emailAddress");
                    }
                    
                    Map<String, Object> postalAddress = UtilMisc.toMap("userLogin", (Object) userLogin); // casting is here necessary for some compiler versions

                    Boolean postalAddressChanged = false;
                    if ("POSTAL_ADDRESS".equals(currentContactMechTypeId)) {
                        postalAddress.put("address1", rec.get("address1"));
                        postalAddress.put("address2", rec.get("address2"));
                           postalAddress.put("city", rec.get("city"));
                        postalAddress.put("stateProvinceGeoId", rec.get("stateProvinceGeoId"));
                        postalAddress.put("countryGeoId", rec.get("countryGeoId"));
                        postalAddress.put("postalCode", rec.get("postalCode"));
                        postalAddressChanged =
                                lastAddress1 == null || !lastAddress1.equals(postalAddress.get("address1")) ||
                                lastAddress2 == null || !lastAddress2.equals(postalAddress.get("address2")) ||
                                lastCity == null || !lastCity.equals(postalAddress.get("city")) ||
                                lastCountryGeoId == null || !lastCountryGeoId.equals(postalAddress.get("countryGeoId"));
                        lastAddress1 = (String) postalAddress.get("address1");
                        lastAddress2 = (String) postalAddress.get("address2");
                        lastCity = (String) postalAddress.get("city");
                        lastCountryGeoId = (String) postalAddress.get("countryGeoId");
                    }                            
                            
                    Map<String, Object> telecomNumber = UtilMisc.toMap("userLogin", (Object) userLogin); // casting is here necessary for some compiler versions

                    Boolean telecomNumberChanged = false;
                    if ("TELECOM_NUMBER".equals(currentContactMechTypeId)) {
                        telecomNumber.put("countryCode", rec.get("telCountryCode"));
                        telecomNumber.put("areaCode", rec.get("telAreaCode"));
                        telecomNumber.put("contactNumber", rec.get("telContactNumber"));
                        telecomNumberChanged = 
                                lastCountryCode == null || !lastCountryCode.equals(telecomNumber.get("countryCode")) ||
                                lastAreaCode == null || !lastAreaCode.equals(telecomNumber.get("areaCode")) ||
                                lastContactNumber == null || !lastContactNumber.equals(telecomNumber.get("contactNumber"));
                        lastCountryCode = (String) telecomNumber.get("countryCode");
                        lastAreaCode = (String) telecomNumber.get("areaCode");
                        lastContactNumber = (String) telecomNumber.get("contactNumber");
                    }
                    
                    Map<String, Object> partyContactMechPurpose = UtilMisc.toMap("partyId", newPartyId, "userLogin", userLogin);
                    Boolean partyContactMechPurposeChanged = false;
                    currentContactMechPurposeTypeId = rec.get("contactMechPurposeTypeId"); 
                    if (currentContactMechPurposeTypeId != null && ("TELECOM_NUMBER".equals(currentContactMechTypeId) || "POSTAL_ADDRESS".equals(currentContactMechTypeId) ||"EMAIL_ADDRESS".equals(currentContactMechTypeId))) {
                        partyContactMechPurpose.put("contactMechPurposeTypeId", currentContactMechPurposeTypeId);
                        partyContactMechPurposeChanged = (lastContactMechPurposeTypeId == null || !lastContactMechPurposeTypeId.equals(currentContactMechPurposeTypeId)) && !telecomNumberChanged && !postalAddressChanged && !emailAddressChanged;
                        Debug.logInfo("===================================last:" + lastContactMechPurposeTypeId + " current: " + currentContactMechPurposeTypeId + " t :" + telecomNumberChanged + " p: " + postalAddressChanged + " e: " + emailAddressChanged + " result: " + partyContactMechPurposeChanged, module); 
                    }
                    lastContactMechPurposeTypeId = currentContactMechPurposeTypeId;
                    
                    // update 
                    if (errMsgs.size() == 0) {

                        if (postalAddressChanged) {
                            result = dispatcher.runSync("createPostalAddress", postalAddress);
                               newContactMechId = (String) result.get("contactMechId");
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId = "GENERAL_LOCATION";
                            }
                            dispatcher.runSync("createPartyContactMech", UtilMisc.toMap("partyId", newPartyId, "contactMechId", newContactMechId, "contactMechPurposeTypeId", currentContactMechPurposeTypeId, "userLogin", userLogin));
                        }

                        if (telecomNumberChanged) {
                            result = dispatcher.runSync("createTelecomNumber", telecomNumber);
                               newContactMechId = (String) result.get("contactMechId");
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId= "PHONE_WORK";
                            }
                            dispatcher.runSync("createPartyContactMech", UtilMisc.toMap("partyId", newPartyId, "contactMechId", newContactMechId, "contactMechPurposeTypeId", currentContactMechPurposeTypeId, "userLogin", userLogin));
                        }

                        if (emailAddressChanged) {
                            result = dispatcher.runSync("createContactMech", emailAddress);
                               newContactMechId = (String) result.get("contactMechId");
                            if (currentContactMechPurposeTypeId == null) {
                                currentContactMechPurposeTypeId = "PRIMARY_EMAIL";
                            }
                            dispatcher.runSync("createPartyContactMech", UtilMisc.toMap("partyId", newPartyId, "contactMechId", newContactMechId, "contactMechPurposeTypeId", currentContactMechPurposeTypeId, "userLogin", userLogin));
                        }
                        
                        if (partyContactMechPurposeChanged) {
                            partyContactMechPurpose.put("contactMechId", newContactMechId);
                            result = dispatcher.runSync("createPartyContactMechPurpose", partyContactMechPurpose);
                        }
                        lastPartyId = currentPartyId;
                        errMsgs.addAll(newErrMsgs);
                        newErrMsgs = new LinkedList<String>();
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (errMsgs.size() > 0) {
            return ServiceUtil.returnError(errMsgs);
        }

        result = ServiceUtil.returnSuccess(partiesCreated + " new parties created");
        return result;
    }
}
