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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Worker methods for Contact Mechanisms
 */
public class ContactMechWorker {

    private static final String MODULE = ContactMechWorker.class.getName();

    private ContactMechWorker() { }

    /**
     * Check the contactMechTypeId value on toAnalyzeFields map and if is a PostalAddress, a TelecomNumber or FtpAddress
     * add the GenericValue related to elementMap
     * @param delegator
     * @param elementMap
     */
    private static void insertRelatedContactElement(Delegator delegator, Map<String, Object> elementMap, Map<String, Object> fields) {
        String contactMechTypeId = (String) fields.get("contactMechTypeId");
        String entityName = null;
        String prefix = null;
        switch (contactMechTypeId) {
        case "POSTAL_ADDRESS":
            entityName = "PostalAddress";
            prefix = "pa";
            break;
        case "TELECOM_NUMBER":
            entityName = "TelecomNumber";
            prefix = "tn";
            break;
        case "FTP_ADDRESS":
            entityName = "FtpAddress";
            prefix = "fa";
            break;
        }
        if (entityName != null) {
            GenericValue element = delegator.makeValue(entityName);
            element.setAllFields(fields, false, prefix, null);
            element.set("contactMechId", fields.get("contactMechId"));
            elementMap.put(ModelUtil.lowerFirstChar(entityName), element);
        }
    }

    public static List<Map<String, Object>> getPartyContactMechValueMaps(Delegator delegator, String partyId, boolean showOld) {
       return getPartyContactMechValueMaps(delegator, partyId, showOld, null);
    }
    public static List<Map<String, Object>> getPartyContactMechValueMaps(Delegator delegator, String partyId, boolean showOld, String contactMechTypeId) {
        Timestamp date = showOld? null: UtilDateTime.nowTimestamp();
        return getPartyContactMechValueMaps(delegator, partyId, date, contactMechTypeId);
    }
    public static List<Map<String, Object>> getPartyContactMechValueMaps(Delegator delegator, String partyId, Timestamp date, String contactMechTypeId) {
        List<Map<String, Object>> partyContactMechValueMaps = new ArrayList<>();

        List<GenericValue> allPartyContactMechs = null;
        List<EntityCondition> conditionList = UtilMisc.toList(EntityCondition.makeCondition("partyId", partyId));
        if (contactMechTypeId != null) conditionList.add(EntityCondition.makeCondition("contactMechTypeId", contactMechTypeId));

        //Resolve all
        try {
            allPartyContactMechs = EntityQuery.use(delegator)
                    .from("PartyAndContactMech")
                    .where(conditionList)
                    .filterByDate(date)
                    .cache()
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (UtilValidate.isEmpty(allPartyContactMechs)) {
            return partyContactMechValueMaps;
        }
        List<String> contactMechIds = EntityUtil.getFieldListFromEntityList(allPartyContactMechs, "contactMechId", true);
        conditionList = UtilMisc.toList(
                EntityCondition.makeCondition("partyId", partyId),
                EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds));
        List<GenericValue> allPartyContactMechPurposes = null;
        try {
            allPartyContactMechPurposes = EntityQuery.use(delegator)
                    .from("PartyContactMechPurpose")
                    .where(conditionList)
                    .filterByDate(date)
                    .cache()
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        for (GenericValue partyContactMech: allPartyContactMechs) {
            Map<String, Object> fields = partyContactMech.getAllFields();

            Map<String, Object> partyContactMechValueMap = new HashMap<>();
            partyContactMechValueMaps.add(partyContactMechValueMap);
            partyContactMechValueMap.put("contactMech", delegator.makeValidValue("ContactMech", fields));
            partyContactMechValueMap.put("contactMechType", delegator.makeValidValue("ContactMechType", fields));
            partyContactMechValueMap.put("partyContactMech", delegator.makeValidValue("PartyContactMech", fields));

            ContactMechWorker.insertRelatedContactElement(delegator, partyContactMechValueMap, fields);
            List<GenericValue> partyContactMechPurposes = EntityUtil.filterByAnd(allPartyContactMechPurposes, UtilMisc.toMap("contactMechId", partyContactMech.getString("contactMechId")));
            partyContactMechValueMap.put("partyContactMechPurposes", partyContactMechPurposes);
        }

        return partyContactMechValueMaps;
    }

    public static List<Map<String, Object>> getFacilityContactMechValueMaps(Delegator delegator, String facilityId, boolean showOld) {
       return getFacilityContactMechValueMaps(delegator, facilityId, showOld, null);
    }
    public static List<Map<String, Object>> getFacilityContactMechValueMaps(Delegator delegator, String facilityId, boolean showOld, String contactMechTypeId) {
        Timestamp date = showOld? UtilDateTime.nowTimestamp(): null;
        return getFacilityContactMechValueMaps(delegator, facilityId, date, contactMechTypeId);
    }
    public static List<Map<String, Object>> getFacilityContactMechValueMaps(Delegator delegator, String facilityId, Timestamp date, String contactMechTypeId) {
        List<Map<String, Object>> facilityContactMechValueMaps = new ArrayList<>();

        List<GenericValue> allFacilityContactMechs = null;
        List<EntityCondition> conditionList = UtilMisc.toList(EntityCondition.makeCondition("facilityId", facilityId));
        if (contactMechTypeId != null) conditionList.add(EntityCondition.makeCondition("contactMechTypeId", contactMechTypeId));

        //Resolve all
        try {
            allFacilityContactMechs = EntityQuery.use(delegator)
                    .from("FacilityAndContactMech")
                    .where(conditionList)
                    .filterByDate(date)
                    .cache()
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (UtilValidate.isEmpty(allFacilityContactMechs)) {
            return facilityContactMechValueMaps;
        }
        List<String> contactMechIds = EntityUtil.getFieldListFromEntityList(allFacilityContactMechs, "contactMechId", true);
        conditionList = UtilMisc.toList(
                EntityCondition.makeCondition("facilityId", facilityId),
                EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds));
        List<GenericValue> allFacilityContactMechPurposes = null;
        try {
            allFacilityContactMechPurposes = EntityQuery.use(delegator).from("FacilityContactMechPurpose").where(conditionList).filterByDate(date).cache().queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        for (GenericValue facilityContactMech: allFacilityContactMechs) {
            Map<String, Object> fields = facilityContactMech.getAllFields();

            Map<String, Object> facilityContactMechValueMap = new HashMap<>();
            facilityContactMechValueMaps.add(facilityContactMechValueMap);
            facilityContactMechValueMap.put("contactMech", delegator.makeValidValue("ContactMech", fields));
            facilityContactMechValueMap.put("contactMechType", delegator.makeValidValue("ContactMechType", fields));
            facilityContactMechValueMap.put("facilityContactMech", delegator.makeValidValue("FacilityContactMech", fields));

            ContactMechWorker.insertRelatedContactElement(delegator, facilityContactMechValueMap, fields);
            List<GenericValue> facilityContactMechPurposes = EntityUtil.filterByAnd(allFacilityContactMechPurposes, UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")));
            facilityContactMechValueMap.put("facilityContactMechPurposes", facilityContactMechPurposes);
        }

        return facilityContactMechValueMaps;
    }

    public static List<Map<String, GenericValue>> getOrderContactMechValueMaps(Delegator delegator, String orderId) {
        return getEntityContactMechValueMaps(delegator, "Order", orderId, null);
    }

    public static Collection<Map<String, GenericValue>> getWorkEffortContactMechValueMaps(Delegator delegator, String workEffortId) {
        Collection<Map<String, GenericValue>> workEffortContactMechValueMaps = getEntityContactMechValueMaps(delegator, "WorkEffort", workEffortId, UtilDateTime.nowTimestamp());
        return UtilValidate.isNotEmpty(workEffortContactMechValueMaps) ? workEffortContactMechValueMaps : null;
    }

    private static List<Map<String, GenericValue>> getEntityContactMechValueMaps(Delegator delegator, String entityName, String entityId, Timestamp date) {
        List<Map<String, GenericValue>> entityContactMechValueMaps = new LinkedList<>();
        String downCaseEntityName = ModelUtil.lowerFirstChar(entityName);

        List<GenericValue> allEntityContactMechs = null;
        String entityViewName = entityName + "AndContactMech";

        ModelEntity contactMechViewModel = delegator.getModelEntity(entityViewName);
        if (contactMechViewModel == null) {
            Debug.logError("Entity view " + entityViewName + " not exist, please check your call. We return empty list", MODULE);
            return entityContactMechValueMaps;
        }
        boolean contactMechPurposeTypeIdFieldPresent = contactMechViewModel.isField("contactMechPurposeTypeId");
        boolean fromDateFieldPresent = contactMechViewModel.isField("fromDate");
        try {
            EntityQuery contactMechQuery = EntityQuery.use(delegator)
                    .from(entityViewName)
                    .where(downCaseEntityName + "Id", entityId);
            if (contactMechPurposeTypeIdFieldPresent) {
                contactMechQuery.orderBy("contactMechPurposeTypeId");
            }
            if (fromDateFieldPresent) {
                contactMechQuery.filterByDate(date);
            }
            allEntityContactMechs = contactMechQuery.cache().queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (allEntityContactMechs == null) {
            return entityContactMechValueMaps;
        }

        for (GenericValue entityContactMech: allEntityContactMechs) {
            Map<String, Object> fields = entityContactMech.getAllFields();
            Map<String, GenericValue> entityContactMechValueMap = new HashMap<>();

            entityContactMechValueMaps.add(entityContactMechValueMap);
            entityContactMechValueMap.put("contactMech", delegator.makeValidValue("ContactMech", fields));
            entityContactMechValueMap.put(downCaseEntityName + "ContactMech", delegator.makeValidValue(entityName + "ContactMech", fields));
            entityContactMechValueMap.put("contactMechType", delegator.makeValidValue("ContactMechType", fields));
            if (contactMechPurposeTypeIdFieldPresent) {
                entityContactMechValueMap.put("contactMechPurposeType", delegator.makeValidValue("ContactMechPurposeType", fields));
            }
            insertRelatedContactElement(delegator, UtilGenerics.cast(entityContactMechValueMap), fields);
        }

        return entityContactMechValueMaps;
    }

    public static void getContactMechAndRelated(ServletRequest request, String partyId, Map<String, Object> target) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        boolean tryEntity = true;
        if (request.getAttribute("_ERROR_MESSAGE_") != null) {
            tryEntity = false;
        }
        if ("true".equals(request.getParameter("tryEntity"))) {
            tryEntity = true;
        }

        String donePage = request.getParameter("DONE_PAGE");
        if (donePage == null) {
            donePage = (String) request.getAttribute("DONE_PAGE");
        }
        if (UtilValidate.isEmpty(donePage)) {
            donePage = "viewprofile";
        }
        target.put("donePage", donePage);

        String contactMechTypeId = request.getParameter("preContactMechTypeId");

        if (contactMechTypeId == null) {
            contactMechTypeId = (String) request.getAttribute("preContactMechTypeId");
        }
        if (contactMechTypeId != null) {
            tryEntity = false;
        }

        String contactMechId = request.getParameter("contactMechId");

        if (request.getAttribute("contactMechId") != null) {
            contactMechId = (String) request.getAttribute("contactMechId");
        }

        GenericValue contactMech = null;

        if (contactMechId != null) {
            target.put("contactMechId", contactMechId);

            // try to find a PartyContactMech with a valid date range
            List<GenericValue> partyContactMechs = null;

            try {
                partyContactMechs = EntityQuery.use(delegator).from("PartyContactMech")
                        .where("partyId", partyId, "contactMechId", contactMechId)
                        .filterByDate()
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            GenericValue partyContactMech = EntityUtil.getFirst(partyContactMechs);

            if (partyContactMech != null) {
                target.put("partyContactMech", partyContactMech);

                Collection<GenericValue> partyContactMechPurposes = null;

                try {
                    partyContactMechPurposes = EntityUtil.filterByDate(partyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                if (UtilValidate.isNotEmpty(partyContactMechPurposes)) {
                    target.put("partyContactMechPurposes", partyContactMechPurposes);
                }
            }

            try {
                contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            if (contactMech != null) {
                target.put("contactMech", contactMech);
                contactMechTypeId = contactMech.getString("contactMechTypeId");
            }
        }

        if (contactMechTypeId != null) {
            target.put("contactMechTypeId", contactMechTypeId);

            try {
                GenericValue contactMechType = EntityQuery.use(delegator).from("ContactMechType").where("contactMechTypeId", contactMechTypeId).queryOne();

                if (contactMechType != null) {
                    target.put("contactMechType", contactMechType);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            Collection<GenericValue> purposeTypes = new LinkedList<>();
            Iterator<GenericValue> typePurposes = null;

            try {
                typePurposes = UtilMisc.toIterator(EntityQuery.use(delegator).from("ContactMechTypePurpose")
                        .where("contactMechTypeId", contactMechTypeId)
                        .queryList());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            while (typePurposes != null && typePurposes.hasNext()) {
                GenericValue contactMechTypePurpose = typePurposes.next();
                GenericValue contactMechPurposeType = null;

                try {
                    contactMechPurposeType = contactMechTypePurpose.getRelatedOne("ContactMechPurposeType", false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                if (contactMechPurposeType != null) {
                    purposeTypes.add(contactMechPurposeType);
                }
            }
            if (purposeTypes.size() > 0) {
                target.put("purposeTypes", purposeTypes);
            }
        }

        String requestName;

        if (contactMech == null) {
            // create
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                if (request.getParameter("contactMechPurposeTypeId") != null || request.getAttribute("contactMechPurposeTypeId") != null) {
                    requestName = "createPostalAddressAndPurpose";
                } else {
                    requestName = "createPostalAddress";
                }
            } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                requestName = "createTelecomNumber";
            } else if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "createEmailAddress";
            } else if ("FTP_ADDRESS".equals(contactMechTypeId)) {
                requestName = "createFtpAddress";
            } else {
                requestName = "createContactMech";
            }
        } else {
            // update
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "updatePostalAddress";
            } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                requestName = "updateTelecomNumber";
            } else if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "updateEmailAddress";
            } else if ("FTP_ADDRESS".equals(contactMechTypeId)) {
                requestName = "updateFtpAddress";
            } else {
                requestName = "updateContactMech";
            }
        }
        target.put("requestName", requestName);

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            GenericValue postalAddress = null;

            try {
                if (contactMech != null) {
                    postalAddress = contactMech.getRelatedOne("PostalAddress", false);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (postalAddress != null) {
                target.put("postalAddress", postalAddress);
            }
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            GenericValue telecomNumber = null;

            try {
                if (contactMech != null) {
                    telecomNumber = contactMech.getRelatedOne("TelecomNumber", false);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (telecomNumber != null) {
                target.put("telecomNumber", telecomNumber);
            }
        } else if ("FTP_ADDRESS".equals(contactMechTypeId)) {
            GenericValue ftpAddress = null;

            try {
                if (contactMech != null) {
                    ftpAddress = contactMech.getRelatedOne("FtpAddress", false);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (ftpAddress != null) {
                target.put("ftpAddress", ftpAddress);
            }
        }

        if ("true".equals(request.getParameter("useValues"))) {
            tryEntity = true;
        }
        target.put("tryEntity", tryEntity);

        try {
            Collection<GenericValue> contactMechTypes = EntityQuery.use(delegator).from("ContactMechType").cache(true).queryList();

            if (contactMechTypes != null) {
                target.put("contactMechTypes", contactMechTypes);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
    }

    /** Returns the first valid FacilityContactMech found based on the given facilityId and a prioritized list of purposes
     * @param delegator the delegator
     * @param facilityId the facility id
     * @param purposeTypes a List of ContactMechPurposeType ids which will be checked one at a time until a valid contact mech is found
     * @return returns the first valid FacilityContactMech found based on the given facilityId and a prioritized list of purposes
     */
    public static GenericValue getFacilityContactMechByPurpose(Delegator delegator, String facilityId, List<String> purposeTypes) {
        if (UtilValidate.isEmpty(facilityId)) {
            return null;
        }
        if (UtilValidate.isEmpty(purposeTypes)) {
            return null;
        }

        for (String purposeType: purposeTypes) {
            List<GenericValue> facilityContactMechPurposes = null;
            List<EntityCondition> conditionList = new LinkedList<>();
            conditionList.add(EntityCondition.makeCondition("facilityId", facilityId));
            conditionList.add(EntityCondition.makeCondition("contactMechPurposeTypeId", purposeType));
            EntityCondition entityCondition = EntityCondition.makeCondition(conditionList);
            try {
                facilityContactMechPurposes = EntityQuery.use(delegator).from("FacilityContactMechPurpose")
                        .where(entityCondition)
                        .orderBy("-fromDate")
                        .cache(true)
                        .filterByDate()
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
                continue;
            }
            for (GenericValue facilityContactMechPurpose: facilityContactMechPurposes) {
                String contactMechId = facilityContactMechPurpose.getString("contactMechId");
                List<GenericValue> facilityContactMechs = null;
                conditionList = new LinkedList<>();
                conditionList.add(EntityCondition.makeCondition("facilityId", facilityId));
                conditionList.add(EntityCondition.makeCondition("contactMechId", contactMechId));
                entityCondition = EntityCondition.makeCondition(conditionList);
                try {
                    facilityContactMechs = EntityQuery.use(delegator).from("FacilityContactMech")
                            .where(entityCondition)
                            .orderBy("-fromDate")
                            .cache(true)
                            .filterByDate()
                            .queryList();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                if (UtilValidate.isNotEmpty(facilityContactMechs)) {
                    return EntityUtil.getFirst(facilityContactMechs);
                }
            }

        }
        return null;
    }

    public static void getFacilityContactMechAndRelated(ServletRequest request, String facilityId, Map<String, Object> target) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        boolean tryEntity = true;
        if (request.getAttribute("_ERROR_MESSAGE") != null) {
            tryEntity = false;
        }
        if ("true".equals(request.getParameter("tryEntity"))) {
            tryEntity = true;
        }

        String donePage = request.getParameter("DONE_PAGE");
        if (donePage == null) {
            donePage = (String) request.getAttribute("DONE_PAGE");
        }
        if (UtilValidate.isEmpty(donePage)) {
            donePage = "viewprofile";
        }
        target.put("donePage", donePage);

        String contactMechTypeId = request.getParameter("preContactMechTypeId");

        if (contactMechTypeId == null) {
            contactMechTypeId = (String) request.getAttribute("preContactMechTypeId");
        }
        if (contactMechTypeId != null) {
            tryEntity = false;
        }

        String contactMechId = request.getParameter("contactMechId");

        if (request.getAttribute("contactMechId") != null) {
            contactMechId = (String) request.getAttribute("contactMechId");
        }

        GenericValue contactMech = null;

        if (contactMechId != null) {
            target.put("contactMechId", contactMechId);

            // try to find a PartyContactMech with a valid date range
            List<GenericValue> facilityContactMechs = null;

            try {
                facilityContactMechs = EntityQuery.use(delegator).from("FacilityContactMech")
                        .where("facilityId", facilityId, "contactMechId", contactMechId)
                        .filterByDate()
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            GenericValue facilityContactMech = EntityUtil.getFirst(facilityContactMechs);

            if (facilityContactMech != null) {
                target.put("facilityContactMech", facilityContactMech);

                Collection<GenericValue> facilityContactMechPurposes = null;

                try {
                    facilityContactMechPurposes = EntityUtil.filterByDate(facilityContactMech.getRelated("FacilityContactMechPurpose", null, null, false), true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                if (UtilValidate.isNotEmpty(facilityContactMechPurposes)) {
                    target.put("facilityContactMechPurposes", facilityContactMechPurposes);
                }
            }

            try {
                contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            if (contactMech != null) {
                target.put("contactMech", contactMech);
                contactMechTypeId = contactMech.getString("contactMechTypeId");
            }
        }

        if (contactMechTypeId != null) {
            target.put("contactMechTypeId", contactMechTypeId);

            try {
                GenericValue contactMechType = EntityQuery.use(delegator).from("ContactMechType").where("contactMechTypeId", contactMechTypeId).queryOne();

                if (contactMechType != null) {
                    target.put("contactMechType", contactMechType);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }

            Collection<GenericValue> purposeTypes = new LinkedList<>();
            Iterator<GenericValue> typePurposes = null;

            try {
                typePurposes = UtilMisc.toIterator(EntityQuery.use(delegator).from("ContactMechTypePurpose")
                        .where("contactMechTypeId", contactMechTypeId)
                        .queryList());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            while (typePurposes != null && typePurposes.hasNext()) {
                GenericValue contactMechTypePurpose = typePurposes.next();
                GenericValue contactMechPurposeType = null;

                try {
                    contactMechPurposeType = contactMechTypePurpose.getRelatedOne("ContactMechPurposeType", false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                if (contactMechPurposeType != null) {
                    purposeTypes.add(contactMechPurposeType);
                }
            }
            if (purposeTypes.size() > 0) {
                target.put("purposeTypes", purposeTypes);
            }
        }

        String requestName;

        if (contactMech == null) {
            // create
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                if (request.getParameter("contactMechPurposeTypeId") != null || request.getAttribute("contactMechPurposeTypeId") != null) {
                    requestName = "createPostalAddressAndPurpose";
                } else {
                    requestName = "createPostalAddress";
                }
            } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                requestName = "createTelecomNumber";
            } else if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "createEmailAddress";
            } else {
                requestName = "createContactMech";
            }
        } else {
            // update
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "updatePostalAddress";
            } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                requestName = "updateTelecomNumber";
            } else if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                requestName = "updateEmailAddress";
            } else {
                requestName = "updateContactMech";
            }
        }
        target.put("requestName", requestName);

        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            GenericValue postalAddress = null;

            try {
                if (contactMech != null) {
                    postalAddress = contactMech.getRelatedOne("PostalAddress", false);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (postalAddress != null) {
                target.put("postalAddress", postalAddress);
            }
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            GenericValue telecomNumber = null;

            try {
                if (contactMech != null) {
                    telecomNumber = contactMech.getRelatedOne("TelecomNumber", false);
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (telecomNumber != null) {
                target.put("telecomNumber", telecomNumber);
            }
        }

        if ("true".equals(request.getParameter("useValues"))) {
            tryEntity = true;
        }
        target.put("tryEntity", tryEntity);

        try {
            Collection<GenericValue> contactMechTypes = EntityQuery.use(delegator).from("ContactMechType").cache(true).queryList();

            if (contactMechTypes != null) {
                target.put("contactMechTypes", contactMechTypes);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
    }

    public static List<Map<String, Object>> getPartyPostalAddresses(ServletRequest request, String partyId, String curContactMechId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        List<Map<String, Object>> postalAddressInfos = new LinkedList<>();

        List<GenericValue> allPartyContactMechs = null;

        try {
            allPartyContactMechs = EntityQuery.use(delegator).from("PartyContactMech").where("partyId", partyId).filterByDate().queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (allPartyContactMechs == null) {
            return postalAddressInfos;
        }

        for (GenericValue partyContactMech: allPartyContactMechs) {
            GenericValue contactMech = null;

            try {
                contactMech = partyContactMech.getRelatedOne("ContactMech", false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (contactMech != null && "POSTAL_ADDRESS".equals(contactMech.getString("contactMechTypeId")) && !contactMech.getString("contactMechId").equals(curContactMechId)) {
                Map<String, Object> postalAddressInfo = new HashMap<>();

                postalAddressInfos.add(postalAddressInfo);
                postalAddressInfo.put("contactMech", contactMech);
                postalAddressInfo.put("partyContactMech", partyContactMech);

                try {
                    GenericValue postalAddress = contactMech.getRelatedOne("PostalAddress", false);
                    postalAddressInfo.put("postalAddress", postalAddress);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }

                try {
                    List<GenericValue> partyContactMechPurposes = EntityUtil.filterByDate(partyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
                    postalAddressInfo.put("partyContactMechPurposes", partyContactMechPurposes);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }

        return postalAddressInfos;
    }

    public static Map<String, Object> getCurrentPostalAddress(ServletRequest request, String partyId, String curContactMechId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, Object> results = new HashMap<>();

        if (curContactMechId != null) {
            List<GenericValue> partyContactMechs = null;

            try {
                partyContactMechs = EntityQuery.use(delegator).from("PartyContactMech")
                        .where("partyId", partyId, "contactMechId", curContactMechId)
                        .filterByDate()
                        .queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            GenericValue curPartyContactMech = EntityUtil.getFirst(partyContactMechs);
            results.put("curPartyContactMech", curPartyContactMech);

            GenericValue curContactMech = null;
            if (curPartyContactMech != null) {
                try {
                    curContactMech = curPartyContactMech.getRelatedOne("ContactMech", false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }

                Collection<GenericValue> curPartyContactMechPurposes = null;
                try {
                    curPartyContactMechPurposes = EntityUtil.filterByDate(curPartyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
                results.put("curPartyContactMechPurposes", curPartyContactMechPurposes);
            }
            results.put("curContactMech", curContactMech);

            GenericValue curPostalAddress = null;
            if (curContactMech != null) {
                try {
                    curPostalAddress = curContactMech.getRelatedOne("PostalAddress", false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            }

            results.put("curPostalAddress", curPostalAddress);
        }
        return results;
    }

    public static boolean isUspsAddress(GenericValue postalAddress) {
        if (postalAddress == null) {
            // null postal address is not a USPS address
            return false;
        }
        if (!"PostalAddress".equals(postalAddress.getEntityName())) {
            // not a postal address not a USPS address
            return false;
        }

        // get and clean the address strings
        String addr1 = postalAddress.getString("address1");
        String addr2 = postalAddress.getString("address2");

        // get the matching string from general.properties
        String matcher = EntityUtilProperties.getPropertyValue("general", "usps.address.match", postalAddress.getDelegator());
        if (UtilValidate.isNotEmpty(matcher)) {
            if (addr1 != null && addr1.toLowerCase(Locale.getDefault()).matches(matcher)) {
                return true;
            }
            if (addr2 != null && addr2.toLowerCase(Locale.getDefault()).matches(matcher)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCompanyAddress(GenericValue postalAddress, String companyPartyId) {
        if (postalAddress == null) {
            // null postal address is not an internal address
            return false;
        }
        if (!"PostalAddress".equals(postalAddress.getEntityName())) {
            // not a postal address not an internal address
            return false;
        }
        if (companyPartyId == null) {
            // no partyId not an internal address
            return false;
        }

        String state = postalAddress.getString("stateProvinceGeoId");
        String addr1 = postalAddress.getString("address1");
        String addr2 = postalAddress.getString("address2");
        if (state != null) {
            state = state.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
        } else {
            state = "";
        }
        if (addr1 != null) {
            addr1 = addr1.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
        } else {
            addr1 = "";
        }
        if (addr2 != null) {
            addr2 = addr2.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
        } else {
            addr2 = "";
        }

        // get all company addresses
        Delegator delegator = postalAddress.getDelegator();
        List<GenericValue> postalAddresses = new LinkedList<>();
        try {
            List<GenericValue> partyContactMechs = EntityQuery.use(delegator).from("PartyContactMech")
                    .where("partyId", companyPartyId)
                    .filterByDate()
                    .queryList();
            if (partyContactMechs != null) {
                for (GenericValue pcm: partyContactMechs) {
                    GenericValue addr = pcm.getRelatedOne("PostalAddress", false);
                    if (addr != null) {
                        postalAddresses.add(addr);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get party postal addresses", MODULE);
        }

        for (GenericValue addr: postalAddresses) {
            String thisAddr1 = addr.getString("address1");
            String thisAddr2 = addr.getString("address2");
            String thisState = addr.getString("stateProvinceGeoId");
            if (thisState != null) {
                thisState = thisState.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
            } else {
                thisState = "";
            }
            if (thisAddr1 != null) {
                thisAddr1 = thisAddr1.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
            } else {
                thisAddr1 = "";
            }
            if (thisAddr2 != null) {
                thisAddr2 = thisAddr2.replaceAll("\\W", "").toLowerCase(Locale.getDefault());
            } else {
                thisAddr2 = "";
            }
            if (thisAddr1.equals(addr1) && thisAddr2.equals(addr2) && thisState.equals(state)) {
                return true;
            }
        }

        return false;
    }

    public static String getContactMechAttribute(Delegator delegator, String contactMechId, String attrName) {
        GenericValue attr = null;
        try {
            attr = EntityQuery.use(delegator).from("ContactMechAttribute").where("contactMechId", contactMechId, "attrName", attrName).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (attr == null) {
            return null;
        } else {
            return attr.getString("attrValue");
        }
    }

    public static String getPostalAddressPostalCodeGeoId(GenericValue postalAddress, Delegator delegator) throws GenericEntityException {
        // if postalCodeGeoId not empty use that
        if (UtilValidate.isNotEmpty(postalAddress.getString("postalCodeGeoId"))) {
            return postalAddress.getString("postalCodeGeoId");
        }

        // no postalCodeGeoId, see if there is a Geo record matching the countryGeoId and postalCode fields
        if (UtilValidate.isNotEmpty(postalAddress.getString("countryGeoId")) && UtilValidate.isNotEmpty(postalAddress.getString("postalCode"))) {
            // first try the shortcut with the geoId convention for "{countryGeoId}-{postalCode}"
            GenericValue geo = EntityQuery.use(delegator).from("Geo").where("geoId", postalAddress.getString("countryGeoId") + "-" + postalAddress.getString("postalCode")).cache().queryOne();
            if (geo != null) {
                // save the value to the database for quicker future reference
                if (postalAddress.isMutable()) {
                    postalAddress.set("postalCodeGeoId", geo.getString("geoId"));
                    postalAddress.store();
                } else {
                    GenericValue mutablePostalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", postalAddress.getString("contactMechId")).queryOne();
                    mutablePostalAddress.set("postalCodeGeoId", geo.getString("geoId"));
                    mutablePostalAddress.store();
                }

                return geo.getString("geoId");
            }

            // no shortcut, try the longcut to see if there is something with a geoCode associated to the countryGeoId
            GenericValue geoAssocAndGeoTo = EntityQuery.use(delegator).from("GeoAssocAndGeoTo")
                    .where("geoIdFrom", postalAddress.getString("countryGeoId"), "geoCode", postalAddress.getString("postalCode"), "geoAssocTypeId", "REGIONS")
                    .cache(true)
                    .queryFirst();
            if (geoAssocAndGeoTo != null) {
                // save the value to the database for quicker future reference
                if (postalAddress.isMutable()) {
                    postalAddress.set("postalCodeGeoId", geoAssocAndGeoTo.getString("geoId"));
                    postalAddress.store();
                } else {
                    GenericValue mutablePostalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", postalAddress.getString("contactMechId")).queryOne();
                    mutablePostalAddress.set("postalCodeGeoId", geoAssocAndGeoTo.getString("geoId"));
                    mutablePostalAddress.store();
                }

                return geoAssocAndGeoTo.getString("geoId");
            }
        }

        // nothing found, return null
        return null;
    }

    /**
     * Returns a <b>PostalAddress</b> <code>GenericValue</code> as a URL encoded <code>String</code>.
     *
     * @param postalAddress A <b>PostalAddress</b> <code>GenericValue</code>.
     * @return A URL encoded <code>String</code>.
     * @throws GenericEntityException
     * @throws UnsupportedEncodingException
     */
    public static String urlEncodePostalAddress(GenericValue postalAddress) throws GenericEntityException, UnsupportedEncodingException {
        Assert.notNull("postalAddress", postalAddress);
        if (!"PostalAddress".equals(postalAddress.getEntityName())) {
            throw new IllegalArgumentException("postalAddress argument is not a PostalAddress entity");
        }
        StringBuilder sb = new StringBuilder();
        if (postalAddress.get("address1") != null) {
            sb.append(postalAddress.get("address1"));
        }
        if (postalAddress.get("address2") != null) {
            sb.append(", ").append(postalAddress.get("address2"));
        }
        if (postalAddress.get("city") != null) {
            sb.append(", ").append(postalAddress.get("city"));
        }
        if (postalAddress.get("stateProvinceGeoId") != null) {
            GenericValue geoValue = postalAddress.getRelatedOne("StateProvinceGeo", false);
            if (geoValue != null) {
                sb.append(", ").append(geoValue.get("geoName"));
            }
        } else if (postalAddress.get("countyGeoId") != null) {
            GenericValue geoValue = postalAddress.getRelatedOne("CountyGeo", false);
            if (geoValue != null) {
                sb.append(", ").append(geoValue.get("geoName"));
            }
        }
        if (postalAddress.get("postalCode") != null) {
            sb.append(", ").append(postalAddress.get("postalCode"));
        }
        if (postalAddress.get("countryGeoId") != null) {
            GenericValue geoValue = postalAddress.getRelatedOne("CountryGeo", false);
            if (geoValue != null) {
                sb.append(", ").append(geoValue.get("geoName"));
            }
        }
        String postalAddressString = sb.toString().trim();
        while (postalAddressString.contains("  ")) {
            postalAddressString = postalAddressString.replace("  ", " ");
        }
        return URLEncoder.encode(postalAddressString, "UTF-8");
    }
}
