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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;

/**
 * Services for Party/Person/Group maintenance
 */
public class PartyServices {

    public static final String module = PartyServices.class.getName();
    public static final String resource = "PartyUiLabels";

    /**
     * Deletes a Party.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map deleteParty(DispatchContext ctx, Map context) {

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
    public static Map createPerson(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        List toBeStored = new LinkedList();
        Locale locale = (Locale) context.get("locale");
        // in most cases userLogin will be null, but get anyway so we can keep track of that info if it is available
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");

        // if specified partyId starts with a number, return an error
        if (partyId != null && partyId.length() > 0 && Character.isDigit(partyId.charAt(0))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "party.id_is_digit", locale));
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (partyId == null || partyId.length() == 0) {
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
            Map newPartyMap = UtilMisc.toMap("partyId", partyId, "partyTypeId", "PERSON", "createdDate", now, "lastModifiedDate", now, "statusId", statusId);
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
     * <b>security check</b>: userLogin must have permission PARTYMGR_STS_UPDATE and the status change must be defined in StatusValidChange.
     */
    public static Map setPartyStatus(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String partyId = (String) context.get("partyId");
        String statusId = (String) context.get("statusId");
        Timestamp statusDate = (Timestamp) context.get("statusDate");
        if (statusDate == null) statusDate = UtilDateTime.nowTimestamp();

        // userLogin must have PARTYMGR_STS_UPDATE. Also, we aren't letting userLogin with same partyId change his own status.
        if (!security.hasEntityPermission("PARTYMGR", "_STS_UPDATE", userLogin)) {
            String errorMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.no_permission_to_operation", locale) + ".";
            Debug.logWarning(errorMsg, module);
            return ServiceUtil.returnError(errorMsg);
        }
        try {
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));

            // check that status is defined as a valid change
            GenericValue statusValidChange = delegator.findByPrimaryKey("StatusValidChange", UtilMisc.toMap("statusId", party.getString("statusId"), "statusIdTo", statusId));
            if (statusValidChange == null) {
                String errorMsg = "Cannot change party status from " + party.getString("statusId") + " to " + statusId;
                Debug.logWarning(errorMsg, module);
                return ServiceUtil.returnError(errorMsg);
            }
            
            // record the oldStatusId and change the party status
            String oldStatusId = party.getString("statusId");
            party.set("statusId", statusId);
            party.store();

            // record this status change in PartyStatus table
            GenericValue partyStatus = delegator.makeValue("PartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", statusId, "statusDate", statusDate));
            partyStatus.create();

            Map results = ServiceUtil.returnSuccess();
            results.put("oldStatusId", oldStatusId);
            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
        }
    }

    /**
     * Updates a Person.
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_GRP_UPDATE permission.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map updatePerson(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_GRP_UPDATE");

        if (result.size() > 0)
            return result;

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

        person.setNonPKFields(context);
        party.setNonPKFields(context);

        try {
            person.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "person.update.write_failure", new Object[] { e.getMessage() }, locale));
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
    public static Map createPartyGroup(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        // partyId might be empty, so check it and get next seq party id if empty
        if (partyId == null || partyId.length() == 0) {
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

                if (UtilValidate.isNotEmpty(((String) context.get("partyTypeId")))) {
                    GenericValue desiredPartyType = delegator.findByPrimaryKeyCache("PartyType", UtilMisc.toMap("partyTypeId", context.get("partyTypeId")));
                    if (desiredPartyType != null && EntityTypeUtil.isType(desiredPartyType, partyGroupPartyType)) {
                        partyTypeId = desiredPartyType.getString("partyTypeId");
                    } else {
                        return ServiceUtil.returnError("The specified partyTypeId [" + context.get("partyTypeId") + "] could not be found or is not a sub-type of PARTY_GROUP");
                    }
                }

                Map newPartyMap = UtilMisc.toMap("partyId", partyId, "partyTypeId", partyTypeId, "createdDate", now, "lastModifiedDate", now);
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
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
    public static Map updatePartyGroup(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get the party Id from context if party has permission to update groups, otherwise use getPartyIdCheckSecurity
        String partyId = null;
        if (security.hasEntityPermission("PARTYMGR", "_GRP_UPDATE", userLogin)) {
            partyId = (String) context.get("partyId");
        } else {
            partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_UPDATE");
        }
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        GenericValue partyGroup = null;
        GenericValue party = null;

        try {
            partyGroup = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", partyId));
            party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        if (partyGroup == null || party == null) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_not_found", locale);
            return ServiceUtil.returnError(errMsg);
        }

        partyGroup.setNonPKFields(context);
        party.setNonPKFields(context);

        try {
            partyGroup.store();
            party.store();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_update_party_information_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
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
    public static Map createAffiliate(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp now = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (partyId == null || partyId.length() == 0) {
            partyId = userLogin.getString("partyId");
        }

        // if specified partyId starts with a number, return an error
        if (Character.isDigit(partyId.charAt(0))) {
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_create_affiliate_digit", locale);
            return ServiceUtil.returnError(errMsg);
        }

        // partyId might be empty, so check it and get next seq party id if empty
        if (partyId == null || partyId.length() == 0) {
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
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.could_not_add_affiliate_info_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        result.put("partyId", partyId);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    /**
     * Updates an Affiliate.
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_UPDATE permission.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map updateAffiliate(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_UPDATE");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (result.size() > 0)
            return result;

        GenericValue affiliate = null;

        try {
            affiliate = delegator.findByPrimaryKey("Affiliate", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
    public static Map createPartyNote(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String noteString = (String) context.get("note");
        String partyId = (String) context.get("partyId");
        String noteId = (String) context.get("noteId");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");
        //Map noteCtx = UtilMisc.toMap("note", noteString, "userLogin", userLogin);

        // if no noteId is specified, then create and associate the note with the userLogin
        if (noteId == null) {
            Map noteRes = null;
            try {
                noteRes = dispatcher.runSync("createNote", UtilMisc.toMap("partyId", userLogin.getString("partyId"), "note", noteString, "userLogin", userLogin, "locale", locale));
            } catch (GenericServiceException e) {
                Debug.logError(e, e.getMessage(), module);
                return ServiceUtil.returnError("Unable to create Note: " + e.getMessage());
            }

            if (noteRes.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
                return noteRes;

            noteId = (String) noteRes.get("noteId");

            if (noteId == null || noteId.length() == 0) {
                errMsg = UtilProperties.getMessage(resource,"partyservices.problem_creating_note_no_noteId_returned", locale);
                return ServiceUtil.returnError(errMsg);
            }
        }
        result.put("noteId", noteId);

        // Set the party info
        try {
            Map fields = UtilMisc.toMap("partyId", partyId, "noteId", noteId);
            GenericValue v = delegator.makeValue("PartyNote", fields);

            delegator.create(v);
        } catch (GenericEntityException ee) {
            Debug.logError(ee, module);
            Map messageMap = UtilMisc.toMap("errMessage", ee.getMessage());
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
    public static Map getPartyFromExactEmail(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        Collection parties = new LinkedList();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (email.length() == 0){
            errMsg = UtilProperties.getMessage(resource,"partyservices.required_parameter_email_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            List exprs = new LinkedList();

            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("infoString")), EntityOperator.EQUALS, new EntityFunction.UPPER(email.toUpperCase())));
            List c = EntityUtil.filterByDate(delegator.findByAnd("PartyAndContactMech", exprs, UtilMisc.toList("infoString")), true);

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                Iterator i = c.iterator();

                while (i.hasNext()) {
                    GenericValue pacm = (GenericValue) i.next();
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0)
            result.put("parties", parties);
        return result;
    }

    public static Map getPartyFromEmail(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        Collection parties = new LinkedList();
        String email = (String) context.get("email");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (email.length() == 0){
            errMsg = UtilProperties.getMessage(resource,"partyservices.required_parameter_email_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            List exprs = new LinkedList();

            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("infoString")), EntityOperator.LIKE, new EntityFunction.UPPER(("%" + email.toUpperCase()) + "%")));
            List c = EntityUtil.filterByDate(delegator.findByAnd("PartyAndContactMech", exprs, UtilMisc.toList("infoString")), true);

            if (Debug.verboseOn()) Debug.logVerbose("List: " + c, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromEmail number found: " + c.size(), module);
            if (c != null) {
                Iterator i = c.iterator();

                while (i.hasNext()) {
                    GenericValue pacm = (GenericValue) i.next();
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", pacm.get("partyId"), "partyTypeId", pacm.get("partyTypeId")));

                    parties.add(UtilMisc.toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
    public static Map getPartyFromUserLogin(DispatchContext dctx, Map context) {
        Debug.logWarning("Running the getPartyFromUserLogin Service...", module);
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        Collection parties = new LinkedList();
        String userLoginId = (String) context.get("userLoginId");
        Locale locale = (Locale) context.get("locale");

        if (userLoginId.length() == 0)
            return ServiceUtil.returnError("Required parameter 'userLoginId' cannot be empty.");

        try {
            List exprs = new LinkedList();

            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("userLoginId")), EntityOperator.LIKE, new EntityFunction.UPPER("%" + userLoginId.toUpperCase() + "%")));
            Collection ulc = delegator.findByAnd("PartyAndUserLogin", exprs, UtilMisc.toList("userloginId"));

            if (Debug.verboseOn()) Debug.logVerbose("Collection: " + ulc, module);
            if (Debug.infoOn()) Debug.logInfo("PartyFromUserLogin number found: " + ulc.size(), module);
            if (ulc != null) {
                Iterator i = ulc.iterator();

                while (i.hasNext()) {
                    GenericValue ul = (GenericValue) i.next();
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", ul.get("partyId"), "partyTypeId", ul.get("partyTypeId")));

                    parties.add(UtilMisc.toMap("party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
    public static Map getPartyFromPerson(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        Collection parties = new LinkedList();
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        Locale locale = (Locale) context.get("locale");

        if (firstName == null) {
            firstName = "";
        }
        if (lastName == null) {
            lastName = "";
        }
        if (firstName.length() == 0 && lastName.length() == 0){
            String errMsg = UtilProperties.getMessage(resource,"partyservices.both_names_cannot_be_empty", locale);
            return ServiceUtil.returnError(errMsg);
        }

        try {
            List exprs = new LinkedList();

            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("firstName")), EntityOperator.LIKE, new EntityFunction.UPPER("%" + firstName.toUpperCase() + "%")));
            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("lastName")), EntityOperator.LIKE, new EntityFunction.UPPER("%" + lastName.toUpperCase() + "%")));
            Collection pc = delegator.findByAnd("Person", exprs, UtilMisc.toList("lastName", "firstName", "partyId"));

            if (Debug.infoOn()) Debug.logInfo("PartyFromPerson number found: " + pc.size(), module);
            if (pc != null) {
                Iterator i = pc.iterator();

                while (i.hasNext()) {
                    GenericValue person = (GenericValue) i.next();
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", person.get("partyId"), "partyTypeId", "PERSON"));

                    parties.add(UtilMisc.toMap("person", person, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
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
    public static Map getPartyFromPartyGroup(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        Collection parties = new LinkedList();
        String groupName = (String) context.get("groupName");
        Locale locale = (Locale) context.get("locale");

        if (groupName.length() == 0) {
            return ServiceUtil.returnError("Required parameter 'groupName' cannot be empty.");
        }

        try {
            List exprs = new LinkedList();

            exprs.add(new EntityExpr(new EntityFunction.UPPER(new EntityFieldValue("groupName")), EntityOperator.LIKE, new EntityFunction.UPPER("%" + groupName.toUpperCase() + "%")));
            Collection pc = delegator.findByAnd("PartyGroup", exprs, UtilMisc.toList("groupName", "partyId"));

            if (Debug.infoOn()) Debug.logInfo("PartyFromGroup number found: " + pc.size(), module);
            if (pc != null) {
                Iterator i = pc.iterator();

                while (i.hasNext()) {
                    GenericValue group = (GenericValue) i.next();
                    GenericValue party = delegator.makeValue("Party", UtilMisc.toMap("partyId", group.get("partyId"), "partyTypeId", "PARTY_GROUP"));

                    parties.add(UtilMisc.toMap("partyGroup", group, "party", party));
                }
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage(resource,"partyservices.cannot_get_party_entities_read", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        if (parties.size() > 0) {
            result.put("parties", parties);
        }
        return result;
    }

    public static Map getPerson(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
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

    public static Map createRoleType(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue roleType = null;

        try {
            roleType = delegator.makeValue("RoleType", null);
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

    public static Map createPartyDataSource(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale"); 

        // input data
        String partyId = (String) context.get("partyId");
        String dataSourceId = (String) context.get("dataSourceId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) fromDate = UtilDateTime.nowTimestamp();

        // userLogin must have PARTYMGR_SRC_CREATE permission
        if (!security.hasEntityPermission("PARTYMGR", "_SRC_CREATE", userLogin)) {
            String errorMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.no_permission_to_operation", locale) + ".";
            return ServiceUtil.returnError(errorMsg);
        }
        try {
            // validate the existance of party and dataSource
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            GenericValue dataSource = delegator.findByPrimaryKey("DataSource", UtilMisc.toMap("dataSourceId", dataSourceId));
            if (party == null || dataSource == null) {
                List errorList = UtilMisc.toList("Cannot create PartyDataSource");
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

    public static Map findParty(DispatchContext dctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();

        String extInfo = (String) context.get("extInfo");

        // get the role types
        try {
            List roleTypes = delegator.findAll("RoleType", UtilMisc.toList("description"));
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
            if (roleTypeId != null && roleTypeId.length() > 0) {
                GenericValue currentRole = delegator.findByPrimaryKeyCache("RoleType", UtilMisc.toMap("roleTypeId", roleTypeId));
                result.put("currentRole", currentRole);
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
            if (stateProvinceGeoId != null && stateProvinceGeoId.length() > 0) {
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
        result.put("viewIndex", new Integer(viewIndex));

        int viewSize = 20;
        try {
            viewSize = Integer.parseInt((String) context.get("VIEW_SIZE"));
        } catch (Exception e) {
            viewSize = 20;
        }
        result.put("viewSize", new Integer(viewSize));

        // get the lookup flag
        String lookupFlag = (String) context.get("lookupFlag");

        // blank param list
        String paramList = "";

        List partyList = null;
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
            List andExprs = FastList.newInstance();
            EntityCondition mainCond = null;

            List orderBy = FastList.newInstance();
            List fieldsToSelect = FastList.newInstance();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add("partyId");
            fieldsToSelect.add("statusId");
            fieldsToSelect.add("partyTypeId");

            // get the params
            String partyId = (String) context.get("partyId");
            String statusId = (String) context.get("statusId");
            String userLoginId = (String) context.get("userLoginId");
            String firstName = (String) context.get("firstName");
            String lastName = (String) context.get("lastName");
            String groupName = (String) context.get("groupName");

            if (!"Y".equals(showAll)) {
                // check for a partyId
                if (partyId != null && partyId.length() > 0) {
                    paramList = paramList + "&partyId=" + partyId;
                    andExprs.add(new EntityExpr("partyId", true, EntityOperator.LIKE, "%"+partyId+"%", true));
                }

                // now the statusId - send ANY for all statuses; leave null for just enabled; or pass a specific status
                if (statusId != null) {
                    paramList = paramList + "&statusId=" + statusId;
                    if (!"ANY".equalsIgnoreCase(statusId)) {
                        andExprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, statusId));
                    }
                } else {
                    // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
                    andExprs.add(new EntityExpr(new EntityExpr("statusId", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
                }

                // ----
                // UserLogin Fields
                // ----

                // filter on user login
                if (userLoginId != null && userLoginId.length() > 0) {
                    paramList = paramList + "&userLoginId=" + userLoginId;

                    // modify the dynamic view
                    dynamicView.addMemberEntity("UL", "UserLogin");
                    dynamicView.addAlias("UL", "userLoginId");
                    dynamicView.addViewLink("PT", "UL", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    // add the expr
                    andExprs.add(new EntityExpr("userLoginId", true, EntityOperator.LIKE, "%"+userLoginId+"%", true));

                    fieldsToSelect.add("userLoginId");
                }

                // ----
                // PartyGroup Fields
                // ----

                // filter on groupName
                if (groupName != null && groupName.length() > 0) {
                    paramList = paramList + "&groupName=" + groupName;

                    // modify the dynamic view
                    dynamicView.addMemberEntity("PG", "PartyGroup");
                    dynamicView.addAlias("PG", "groupName");
                    dynamicView.addViewLink("PT", "PG", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

                    // add the expr
                    andExprs.add(new EntityExpr("groupName", true, EntityOperator.LIKE, "%"+groupName+"%", true));

                    fieldsToSelect.add("groupName");
                }

                // ----
                // Person Fields
                // ----

                // modify the dynamic view
                if ((firstName != null && firstName.length() > 0) || (lastName != null && lastName.length() > 0)) {
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
                if (firstName != null && firstName.length() > 0) {
                    paramList = paramList + "&firstName=" + firstName;
                    andExprs.add(new EntityExpr("firstName", true, EntityOperator.LIKE, "%"+firstName+"%", true));
                }

                // filter on lastName
                if (lastName != null && lastName.length() > 0) {
                    paramList = paramList + "&lastName=" + lastName;
                    andExprs.add(new EntityExpr("lastName", true, EntityOperator.LIKE, "%"+lastName+"%", true));
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
                    andExprs.add(new EntityExpr("roleTypeId", EntityOperator.EQUALS, roleTypeId));

                    fieldsToSelect.add("roleTypeId");
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
                    if (address1 != null && address1.length() > 0) {
                        paramList = paramList + "&address1=" + address1;
                        andExprs.add(new EntityExpr("address1", true, EntityOperator.LIKE, "%" + address1 + "%", true));
                    }

                    // filter on address2
                    String address2 = (String) context.get("address2");
                    if (address2 != null && address2.length() > 0) {
                        paramList = paramList + "&address2=" + address2;
                        andExprs.add(new EntityExpr("address2", true, EntityOperator.LIKE, "%" + address2 + "%", true));
                    }

                    // filter on city
                    String city = (String) context.get("city");
                    if (city != null && city.length() > 0) {
                        paramList = paramList + "&city=" + city;
                        andExprs.add(new EntityExpr("city", true, EntityOperator.EQUALS, city, true));
                    }

                    // filter on state geo
                    if (stateProvinceGeoId != null && !"ANY".equals(stateProvinceGeoId)) {
                        paramList = paramList + "&stateProvinceGeoId=" + stateProvinceGeoId;
                        andExprs.add(new EntityExpr("stateProvinceGeoId", EntityOperator.EQUALS, stateProvinceGeoId));
                    }

                    // filter on postal code
                    String postalCode = (String) context.get("postalCode");
                    if (postalCode != null && postalCode.length() > 0) {
                        paramList = paramList + "&postalCode=" + postalCode;
                        andExprs.add(new EntityExpr("postalCode", true, EntityOperator.LIKE, "%" + postalCode + "%", true));
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
                    if (infoString != null && infoString.length() > 0) {
                        paramList = paramList + "&infoString=" + infoString;
                        andExprs.add(new EntityExpr("infoString", true, EntityOperator.LIKE, "%"+infoString+"%", true));
                    }

                    fieldsToSelect.add("infoString");
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
                    if (countryCode != null && countryCode.length() > 0) {
                        paramList = paramList + "&countryCode=" + countryCode;
                        andExprs.add(new EntityExpr("countryCode", true, EntityOperator.EQUALS, countryCode, true));
                    }

                    // filter on areaCode
                    String areaCode = (String) context.get("areaCode");
                    if (areaCode != null && areaCode.length() > 0) {
                        paramList = paramList + "&areaCode=" + areaCode;
                        andExprs.add(new EntityExpr("areaCode", true, EntityOperator.EQUALS, areaCode, true));
                    }

                    // filter on contact number
                    String contactNumber = (String) context.get("contactNumber");
                    if (contactNumber != null && contactNumber.length() > 0) {
                        paramList = paramList + "&contactNumber=" + contactNumber;
                        andExprs.add(new EntityExpr("contactNumber", true, EntityOperator.EQUALS, contactNumber, true));
                    }

                    fieldsToSelect.add("contactNumber");
                    fieldsToSelect.add("areaCode");
                }

                // ---- End of Dynamic View Creation

                // build the main condition
                if (andExprs.size() > 0) mainCond = new EntityConditionList(andExprs, EntityOperator.AND);
            }

            Debug.logInfo("In findParty mainCond=" + mainCond, module);

            // do the lookup
            if (mainCond != null || "Y".equals(showAll)) {
                try {
                    // set distinct on so we only get one row per order
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
                    // using list iterator
                    EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, fieldsToSelect, orderBy, findOpts);

                    // get the indexes for the partial list
                    lowIndex = viewIndex * viewSize + 1;
                    highIndex = (viewIndex + 1) * viewSize;

                    // get the partial list for this page
                    partyList = pli.getPartialList(lowIndex, viewSize);

                    // attempt to get the full size
                    pli.last();
                    partyListSize = pli.currentIndex();
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
        result.put("partyListSize", new Integer(partyListSize));
        result.put("paramList", paramList);
        result.put("highIndex", new Integer(highIndex));
        result.put("lowIndex", new Integer(lowIndex));

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
    public static Map linkParty(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator _delegator = dctx.getDelegator();
        GenericDelegator delegator = _delegator.cloneDelegator();
        delegator.setEntityEcaHandler(null);

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyIdTo = (String) context.get("partyIdTo");
        String partyId = (String) context.get("partyId");
        Timestamp now = UtilDateTime.nowTimestamp();

        // update the contact mech records
        try {
            delegator.storeByCondition("PartyContactMech", UtilMisc.toMap("partyId", partyIdTo, "thruDate", now),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the contact mech purpose records
        try {
            delegator.storeByCondition("PartyContactMechPurpose", UtilMisc.toMap("partyId", partyIdTo, "thruDate", now),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the party notes
        try {
            delegator.storeByCondition("PartyNote", UtilMisc.toMap("partyId", partyIdTo),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the non-existing party roles
        List rolesToMove;
        try {
            rolesToMove = delegator.findByAnd("PartyRole", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Iterator rtmi = rolesToMove.iterator();
        while (rtmi.hasNext()) {
            GenericValue attr = (GenericValue) rtmi.next();
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
        try {
            delegator.removeByAnd("PartyRole", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            delegator.storeByCondition("PartyRole", UtilMisc.toMap("partyId", partyIdTo),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the order role records
        try {
            delegator.storeByCondition("OrderRole", UtilMisc.toMap("partyId", partyIdTo),
                    new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // update the non-existing attributes
        List attrsToMove;
        try {
            attrsToMove = delegator.findByAnd("PartyAttribute", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Iterator atmi = attrsToMove.iterator();
        while (atmi.hasNext()) {
            GenericValue attr = (GenericValue) atmi.next();
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

        // disable the party
        Map disableResp = null;
        try {
            disableResp = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", partyId, "statusId", "PARTY_DISABLED", "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (disableResp != null && ServiceUtil.isError(disableResp)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(disableResp));
        }

        Map resp = ServiceUtil.returnSuccess();
        resp.put("partyId", partyIdTo);
        return resp;
    }

    public static Map importAddressMatchMapCsv(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        ByteWrapper file = (ByteWrapper) context.get("uploadedFile");
        String csvFile = new String(file.getBytes());
        csvFile = csvFile.replaceAll("\\r", "");
        String[] records = csvFile.split("\\n");

        for (int i = 0; i < records.length; i++) {
            if (records[i] != null) {
                String str = records[i].trim();
                String[] map = str.split(",");
                if (map.length != 2 && map.length != 3) {
                    return ServiceUtil.returnError("Invalid format for CSV (key,value,sequence)");
                } else {
                    GenericValue addrMap = delegator.makeValue("AddressMatchMap", null);
                    addrMap.put("mapKey", map[0].trim().toUpperCase());
                    addrMap.put("mapValue", map[1].trim().toUpperCase());
                    int seq = i + 1;
                    if (map.length == 3) {
                        char[] chars = map[2].toCharArray();
                        boolean isNumber = true;
                        for (int c = 0; c < chars.length; c++) {
                            if (!Character.isDigit(chars[c])) {
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

                    addrMap.put("sequenceNum", new Long(seq));
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
}
