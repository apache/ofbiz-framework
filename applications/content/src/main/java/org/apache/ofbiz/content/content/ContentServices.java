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
package org.apache.ofbiz.content.content;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ContentServices Class
 */
public class ContentServices {

    private static final String MODULE = ContentServices.class.getName();
    private static final String RESOURCE = "ContentUiLabels";

    /**
     * findRelatedContent Finds the related
     */
    public static Map<String, Object> findRelatedContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> results = new HashMap<>();

        GenericValue currentContent = (GenericValue) context.get("currentContent");
        String fromDate = (String) context.get("fromDate");
        String thruDate = (String) context.get("thruDate");
        String toFrom = (String) context.get("toFrom");
        Locale locale = (Locale) context.get("locale");
        if (toFrom == null) {
            toFrom = "TO";
        } else {
            toFrom = toFrom.toUpperCase(Locale.getDefault());
        }

        List<String> assocTypes = UtilGenerics.cast(context.get("contentAssocTypeList"));
        List<String> targetOperations = UtilGenerics.cast(context.get("targetOperationList"));
        List<String> contentTypes = UtilGenerics.cast(context.get("contentTypeList"));
        List<GenericValue> contentList = null;

        try {
            contentList = ContentWorker.getAssociatedContent(currentContent, toFrom, assocTypes, contentTypes, fromDate, thruDate);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentAssocRetrievingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        if (UtilValidate.isEmpty(targetOperations)) {
            results.put("contentList", contentList);
            return results;
        }

        Map<String, Object> serviceInMap = new HashMap<>();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperations);
        serviceInMap.put("entityOperation", context.get("entityOperation"));

        List<GenericValue> permittedList = new LinkedList<>();
        Map<String, Object> permResults = null;
        for (GenericValue content : contentList) {
            serviceInMap.put("currentContent", content);
            try {
                permResults = dispatcher.runSync("genericContentPermission", serviceInMap);
                if (ServiceUtil.isError(permResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(permResults));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPermissionNotGranted", locale));
            }

            Boolean hasPermission = (Boolean) permResults.get("hasPermission");
            if (Boolean.TRUE.equals(hasPermission)) {
                permittedList.add(content);
            }

        }

        results.put("contentList", permittedList);
        return results;
    }

    /**
     * Deactivates any active ContentAssoc (except the current one) that is associated with the passed in template/layout contentId and mapKey.
     */
    public static Map<String, Object> deactivateAssocs(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String contentIdTo = (String) context.get("contentIdTo");
        String mapKey = (String) context.get("mapKey");
        String contentAssocTypeId = (String) context.get("contentAssocTypeId");
        String activeContentId = (String) context.get("activeContentId");
        String contentId = (String) context.get("contentId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String sequenceNum = null;
        Map<String, Object> results = new HashMap<>();

        try {
            GenericValue activeAssoc = null;
            if (fromDate != null) {
                activeAssoc = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", activeContentId, "contentIdTo", contentIdTo,
                        "fromDate", fromDate, "contentAssocTypeId", contentAssocTypeId).queryOne();
                if (activeAssoc == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentAssocNotFound",
                            UtilMisc.toMap("activeContentId", activeContentId, "contentIdTo", contentIdTo, "contentAssocTypeId", contentAssocTypeId,
                                    "fromDate", fromDate), locale));
                }
                sequenceNum = (String) activeAssoc.get("sequenceNum");
            }

            List<EntityCondition> exprList = new LinkedList<>();
            exprList.add(EntityCondition.makeCondition("mapKey", EntityOperator.EQUALS, mapKey));
            if (sequenceNum != null) {
                exprList.add(EntityCondition.makeCondition("sequenceNum", EntityOperator.EQUALS, sequenceNum));
            }
            exprList.add(EntityCondition.makeCondition("mapKey", EntityOperator.EQUALS, mapKey));
            exprList.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            exprList.add(EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentIdTo));
            exprList.add(EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.EQUALS, contentAssocTypeId));

            if (UtilValidate.isNotEmpty(activeContentId)) {
                exprList.add(EntityCondition.makeCondition("contentId", EntityOperator.NOT_EQUAL, activeContentId));
            }
            if (UtilValidate.isNotEmpty(contentId)) {
                exprList.add(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId));
            }

            EntityConditionList<EntityCondition> assocExprList = EntityCondition.makeCondition(exprList, EntityOperator.AND);
            List<GenericValue> relatedAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                    .where(assocExprList)
                    .orderBy("fromDate").filterByDate().queryList();

            for (GenericValue val : relatedAssocs) {
                val.set("thruDate", nowTimestamp);
                val.store();
            }
            results.put("deactivatedList", relatedAssocs);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }



    public static Map<String, Object> linkContentToPubPt(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String contentId = (String) context.get("contentId");
        String contentIdTo = (String) context.get("contentIdTo");
        String contentAssocTypeId = (String) context.get("contentAssocTypeId");
        String statusId = (String) context.get("statusId");
        String privilegeEnumId = (String) context.get("privilegeEnumId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (Debug.infoOn()) {
            Debug.logInfo("in publishContent, statusId:" + statusId, MODULE);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in publishContent, userLogin:" + userLogin, MODULE);
        }

        Map<String, Object> mapIn = new HashMap<>();
        mapIn.put("contentId", contentId);
        mapIn.put("contentIdTo", contentIdTo);
        mapIn.put("contentAssocTypeId", contentAssocTypeId);
        String publish = (String) context.get("publish");

        try {
            boolean isPublished = false;
            GenericValue contentAssocViewFrom = ContentWorker.getContentAssocViewFrom(delegator, contentIdTo, contentId, contentAssocTypeId,
                    statusId, privilegeEnumId);
            if (contentAssocViewFrom != null) {
                isPublished = true;
            }
            if (Debug.infoOn()) {
                Debug.logInfo("in publishContent, contentId:" + contentId + " contentIdTo:" + contentIdTo + " contentAssocTypeId:"
                        + contentAssocTypeId + " publish:" + publish + " isPublished:" + isPublished, MODULE);
            }
            if (UtilValidate.isNotEmpty(publish) && "Y".equalsIgnoreCase(publish)) {
                GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                String contentStatusId = (String) content.get("statusId");
                String contentPrivilegeEnumId = (String) content.get("privilegeEnumId");

                if (Debug.infoOn()) {
                    Debug.logInfo("in publishContent, statusId:" + statusId + " contentStatusId:" + contentStatusId + " privilegeEnumId:"
                            + privilegeEnumId + " contentPrivilegeEnumId:" + contentPrivilegeEnumId, MODULE);
                }
                // Don't do anything if link was already there
                if (!isPublished) {
                    content.put("privilegeEnumId", privilegeEnumId);
                    content.put("statusId", statusId);
                    content.store();

                    mapIn = new HashMap<>();
                    mapIn.put("contentId", contentId);
                    mapIn.put("contentIdTo", contentIdTo);
                    mapIn.put("contentAssocTypeId", contentAssocTypeId);
                    mapIn.put("mapKey", context.get("mapKey"));
                    mapIn.put("fromDate", UtilDateTime.nowTimestamp());
                    mapIn.put("createdDate", UtilDateTime.nowTimestamp());
                    mapIn.put("lastModifiedDate", UtilDateTime.nowTimestamp());
                    mapIn.put("createdByUserLogin", userLogin.get("userLoginId"));
                    mapIn.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                    delegator.create("ContentAssoc", mapIn);
                }
            } else {
                // Only deactive if currently published
                if (isPublished) {
                    Map<String, Object> thisResults = dispatcher.runSync("deactivateAssocs", mapIn);
                    if (ServiceUtil.isError(thisResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(thisResults));
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Problem getting existing content", "ContentServices");
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }



}

