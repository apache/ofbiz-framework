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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
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
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ContentServices Class
 */
public class ContentServices {

    public static final String module = ContentServices.class.getName();
    public static final String resource = "ContentUiLabels";

    /**
     * findRelatedContent Finds the related
     */
    public static Map<String, Object> findRelatedContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> results = new HashMap<String, Object>();

        GenericValue currentContent = (GenericValue) context.get("currentContent");
        String fromDate = (String) context.get("fromDate");
        String thruDate = (String) context.get("thruDate");
        String toFrom = (String) context.get("toFrom");
        Locale locale = (Locale) context.get("locale");
        if (toFrom == null) {
            toFrom = "TO";
        } else {
            toFrom = toFrom.toUpperCase();
        }

        List<String> assocTypes = UtilGenerics.checkList(context.get("contentAssocTypeList"));
        List<String> targetOperations = UtilGenerics.checkList(context.get("targetOperationList"));
        List<String> contentTypes = UtilGenerics.checkList(context.get("contentTypeList"));
        List<GenericValue> contentList = null;
        
        try {
            contentList = ContentWorker.getAssociatedContent(currentContent, toFrom, assocTypes, contentTypes, fromDate, thruDate);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocRetrievingError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        if (UtilValidate.isEmpty(targetOperations)) {
            results.put("contentList", contentList);
            return results;
        }

        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperations);
        serviceInMap.put("entityOperation", context.get("entityOperation"));

        List<GenericValue> permittedList = new LinkedList<GenericValue>();
        Map<String, Object> permResults = null;
        for (GenericValue content : contentList) {
            serviceInMap.put("currentContent", content);
            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPermissionNotGranted", locale));
            }

            String permissionStatus = (String) permResults.get("permissionStatus");
            if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
                permittedList.add(content);
            }

        }

        results.put("contentList", permittedList);
        return results;
    }
    /**
     * This is a generic service for traversing a Content tree, typical of a blog response tree. It calls the ContentWorker.traverse method.
     */
    public static Map<String, Object> findContentParents(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<String, Object>();
        List<Object> parentList = new LinkedList<Object>();
        results.put("parentList", parentList);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String)context.get("contentId");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        String direction = (String)context.get("direction");
        if (UtilValidate.isEmpty(direction)) {
            direction="To";
        }
        Map<String, Object> traversMap = new HashMap<String, Object>();
        traversMap.put("contentId", contentId);
        traversMap.put("direction", direction);
        traversMap.put("contentAssocTypeId", contentAssocTypeId);
        try {
            Map<String, Object> thisResults = dispatcher.runSync("traverseContent", traversMap);
            String errorMsg = ServiceUtil.getErrorMessage(thisResults);
            if (UtilValidate.isNotEmpty(errorMsg)) {
                Debug.logError("Problem in traverseContent. " + errorMsg, module);
                return ServiceUtil.returnError(errorMsg);
            }
            Map<String, Object> nodeMap = UtilGenerics.checkMap(thisResults.get("nodeMap"));
            walkParentTree(nodeMap, parentList);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnFailure(e.getMessage());
        }
        return results;
    }

    private static void walkParentTree(Map<String, Object> nodeMap, List<Object> parentList) {
        List<Map<String, Object>> kids = UtilGenerics.checkList(nodeMap.get("kids"));
        if (UtilValidate.isEmpty(kids)) {
            parentList.add(nodeMap.get("contentId"));
        } else {
            for (Map<String, Object> node : kids) {
                walkParentTree(node, parentList);
            }
        }
    }
    /**
     * This is a generic service for traversing a Content tree, typical of a blog response tree. It calls the ContentWorker.traverse method.
     */
    public static Map<String, Object> traverseContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");

        String contentId = (String) context.get("contentId");
        String direction = (String) context.get("direction");
        if (direction != null && direction.equalsIgnoreCase("From")) {
            direction = "From";
        } else {
            direction = "To";
        }

        if (contentId == null) {
            contentId = "PUBLISH_ROOT";
        }

        GenericValue content = null;
        try {
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Error:" + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
        }

        String fromDateStr = (String) context.get("fromDateStr");
        String thruDateStr = (String) context.get("thruDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }

        Timestamp thruDate = null;
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }

        Map<String, Object> whenMap = new HashMap<String, Object>();
        whenMap.put("followWhen", context.get("followWhen"));
        whenMap.put("pickWhen", context.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", context.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", context.get("returnAfterPickWhen"));

        String startContentAssocTypeId = (String) context.get("contentAssocTypeId");
        if (startContentAssocTypeId != null) {
            startContentAssocTypeId = "PUBLISH";
        }

        Map<String, Object> nodeMap = new HashMap<String, Object>();
        List<GenericValue> pickList = new LinkedList<GenericValue>();
        ContentWorker.traverse(delegator, content, fromDate, thruDate, whenMap, 0, nodeMap, startContentAssocTypeId, pickList, direction);

        results.put("nodeMap", nodeMap);
        results.put("pickList", pickList);
        return results;
    }

    /**
     * Create a Content service. The work is done in a separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> createContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = createContentMethod(dctx, context);
        return result;
    }

    /**
     * Create a Content method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> createContentMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_CREATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");
        if (Debug.infoOn()) Debug.logInfo("in createContentMethod, targetOperationList: " + targetOperationList, null);

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String) context.get("contentId");

        if (UtilValidate.isEmpty(contentId)) {
            contentId = delegator.getNextSeqId("Content");
        }

        GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", contentId));
        content.setNonPKFields(context);

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");

        // get first statusId  for content out of the statusItem table if not provided
        if (UtilValidate.isEmpty(context.get("statusId"))) {
            try {
                GenericValue statusItem = EntityQuery.use(delegator).from("StatusItem")
                        .where("statusTypeId", "CONTENT_STATUS")
                        .orderBy("sequenceId").queryFirst();
                if (statusItem != null) {
                    content.put("statusId",  statusItem.get("statusId"));
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        content.put("createdByUserLogin", userLoginId);
        content.put("lastModifiedByUserLogin", userLoginId);
        content.put("createdDate", nowTimestamp);
        content.put("lastModifiedDate", nowTimestamp);

        context.put("currentContent", content);
        if (Debug.infoOn()) Debug.logInfo("in createContentMethod, context: " + context, null);

        Map<String, Object> permResults = ContentWorker.callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
            try {
                content.create();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            } catch (Exception e2) {
                return ServiceUtil.returnError(e2.getMessage());
            }

            result.put("contentId", contentId);
        } else {
            String errorMsg = (String) permResults.get(ModelService.ERROR_MESSAGE);
            result.put(ModelService.ERROR_MESSAGE, errorMsg);
            return ServiceUtil.returnFailure(errorMsg);
        }

        context.remove("currentContent");
        return result;
    }

    /**
     * Create a ContentAssoc service. The work is done in a separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> createContentAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_CREATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map<String, Object> result = null;
        try {
            result = createContentAssocMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e2) {
            return ServiceUtil.returnError(e2.getMessage());
        } catch (Exception e3) {
            return ServiceUtil.returnError(e3.getMessage());
        }
        return result;
    }

    /**
     * Create a ContentAssoc method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> createContentAssocMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException, GenericEntityException {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");
        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        Locale locale = (Locale) context.get("locale");

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();

        // This section guesses how contentId should be used (From or To) if
        // only a contentIdFrom o contentIdTo is passed in
        String contentIdFrom = (String) context.get("contentIdFrom");
        String contentIdTo = (String) context.get("contentIdTo");
        String contentId = (String) context.get("contentId");
        int contentIdCount = 0;
        if (UtilValidate.isNotEmpty(contentIdFrom))
            contentIdCount++;
        if (UtilValidate.isNotEmpty(contentIdTo))
            contentIdCount++;
        if (UtilValidate.isNotEmpty(contentId))
            contentIdCount++;
        if (contentIdCount < 2) {
            Debug.logError("Not 2 out of ContentId/To/From.", "ContentServices");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentCreateContentAssocMethodError", locale));
        }

        if (UtilValidate.isNotEmpty(contentIdFrom)) {
            if (UtilValidate.isEmpty(contentIdTo))
                contentIdTo = contentId;
        }
        if (UtilValidate.isNotEmpty(contentIdTo)) {
            if (UtilValidate.isEmpty(contentIdFrom))
                contentIdFrom = contentId;
        }

        GenericValue contentAssoc = delegator.makeValue("ContentAssoc");
        contentAssoc.put("contentId", contentIdFrom);
        contentAssoc.put("contentIdTo", contentIdTo);
        contentAssoc.put("contentAssocTypeId", context.get("contentAssocTypeId"));
        contentAssoc.put("contentAssocPredicateId", context.get("contentAssocPredicateIdFrom"));
        contentAssoc.put("dataSourceId", context.get("dataSourceId"));

        Timestamp fromDate = (Timestamp) context.get("fromDate");
        if (fromDate == null) {
            contentAssoc.put("fromDate", UtilDateTime.nowTimestamp());
        } else {
            contentAssoc.put("fromDate", fromDate);
        }

        Timestamp thruDate = (Timestamp) context.get("thruDate");
        if (thruDate == null) {
            contentAssoc.put("thruDate", null);
        } else {
            contentAssoc.put("thruDate", thruDate);
        }

        contentAssoc.put("sequenceNum", context.get("sequenceNum"));
        contentAssoc.put("mapKey", context.get("mapKey"));

        String upperCoordinateStr = (String) context.get("upperCoordinate");
        if (UtilValidate.isEmpty(upperCoordinateStr)) {
            contentAssoc.put("upperCoordinate", null);
        } else {
            contentAssoc.put("upperCoordinate", upperCoordinateStr);
        }

        String leftCoordinateStr = (String) context.get("leftCoordinate");
        if (UtilValidate.isEmpty(leftCoordinateStr)) {
            contentAssoc.put("leftCoordinate", null);
        } else {
            contentAssoc.put("leftCoordinate", leftCoordinateStr);
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String createdByUserLogin = userLoginId;
        String lastModifiedByUserLogin = userLoginId;

        Timestamp createdDate = UtilDateTime.nowTimestamp();
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

        contentAssoc.put("createdByUserLogin", createdByUserLogin);
        contentAssoc.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
        contentAssoc.put("createdDate", createdDate);
        contentAssoc.put("lastModifiedDate", lastModifiedDate);

        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        String permissionStatus = null;
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperationList);
        serviceInMap.put("contentPurposeList", contentPurposeList);
        serviceInMap.put("entityOperation", context.get("entityOperation"));
        serviceInMap.put("contentAssocPredicateId", context.get("contentAssocPredicateId"));
        serviceInMap.put("contentIdTo", contentIdTo);
        serviceInMap.put("contentIdFrom", contentIdFrom);
        serviceInMap.put("statusId", context.get("statusId"));
        serviceInMap.put("privilegeEnumId", context.get("privilegeEnumId"));
        serviceInMap.put("roleTypeList", context.get("roleTypeList"));
        serviceInMap.put("displayFailCond", context.get("displayFailCond"));

        Map<String, Object> permResults = null;
        permResults = dispatcher.runSync("checkAssocPermission", serviceInMap);
        permissionStatus = (String) permResults.get("permissionStatus");

        if (permissionStatus != null && permissionStatus.equals("granted")) {
            contentAssoc.create();
        } else {
            String errorMsg = (String)permResults.get(ModelService.ERROR_MESSAGE);
            result.put(ModelService.ERROR_MESSAGE, errorMsg);
            return ServiceUtil.returnFailure(errorMsg);
        }

        result.put("contentIdTo", contentIdTo);
        result.put("contentIdFrom", contentIdFrom);
        result.put("fromDate", contentAssoc.get("fromDate"));
        result.put("contentAssocTypeId", contentAssoc.get("contentAssocTypeId"));

        return result;
    }

    /**
     * A service wrapper for the updateContentMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateContent(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map<String, Object> result = updateContentMethod(dctx, context);
        return result;
    }

    /**
     * Update a Content method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty of calling a service.
     * DEJ20060610: why is this being done? It's a bad design because the service call overhead is not very big, but not calling through the service engine breaks functionality possibilities like ECAs and such
     */
    public static Map<String, Object> updateContentMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        
        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        GenericValue content = null;
        Locale locale = (Locale) context.get("locale");
        String contentId = (String) context.get("contentId");
        try {
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
        }
        context.put("currentContent", content);

        Map<String, Object> permResults = ContentWorker.callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String lastModifiedByUserLogin = userLoginId;
            Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

            // update status first to see if allowed
            if (UtilValidate.isNotEmpty(context.get("statusId"))) {
                Map<String, Object> statusInMap = UtilMisc.<String, Object>toMap("contentId", context.get("contentId"), "statusId", context.get("statusId"), "userLogin", userLogin);
                try {
                   dispatcher.runSync("setContentStatus", statusInMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem updating content Status", "ContentServices");
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentStatusUpdateError", UtilMisc.toMap("errorString", e), locale));
                }
            }

            content.setNonPKFields(context);
            content.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
            content.put("lastModifiedDate", lastModifiedDate);
            try {
                content.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errorMsg = ContentWorker.prepPermissionErrorMsg(permResults);
            return ServiceUtil.returnError(errorMsg);
        }

        return result;
    }

    /**
     * Update a ContentAssoc service. The work is done in a separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> updateContentAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map<String, Object> result = updateContentAssocMethod(dctx, context);
        return result;
    }

    /**
     * Update a ContentAssoc method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> updateContentAssocMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = new HashMap<String, Object>();

        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        // This section guesses how contentId should be used (From or To) if
        // only a contentIdFrom o contentIdTo is passed in
        String contentIdFrom = (String) context.get("contentId");
        String contentIdTo = (String) context.get("contentIdTo");
        String contentId = (String) context.get("contentId");
        String contentAssocTypeId = (String) context.get("contentAssocTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        GenericValue contentAssoc = null;
        try {
            contentAssoc = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentId, "contentIdTo", contentIdTo, "contentAssocTypeId", contentAssocTypeId, "fromDate", fromDate).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Error:" + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocRetrievingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        if (contentAssoc == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocUpdateError", locale));
        }

        contentAssoc.put("contentAssocPredicateId", context.get("contentAssocPredicateId"));
        contentAssoc.put("dataSourceId", context.get("dataSourceId"));
        contentAssoc.set("thruDate", context.get("thruDate"));
        contentAssoc.set("sequenceNum", context.get("sequenceNum"));
        contentAssoc.put("mapKey", context.get("mapKey"));

        String upperCoordinateStr = (String) context.get("upperCoordinate");
        if (UtilValidate.isEmpty(upperCoordinateStr)) {
            contentAssoc.put("upperCoordinate", null);
        } else {
            contentAssoc.setString("upperCoordinate", upperCoordinateStr);
        }

        String leftCoordinateStr = (String) context.get("leftCoordinate");
        if (UtilValidate.isEmpty(leftCoordinateStr)) {
            contentAssoc.put("leftCoordinate", null);
        } else {
            contentAssoc.setString("leftCoordinate", leftCoordinateStr);
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        contentAssoc.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
        contentAssoc.put("lastModifiedDate", lastModifiedDate);

        String permissionStatus = null;
        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperationList);
        serviceInMap.put("contentPurposeList", contentPurposeList);
        serviceInMap.put("entityOperation", context.get("entityOperation"));
        serviceInMap.put("contentIdTo", contentIdTo);
        serviceInMap.put("contentIdFrom", contentIdFrom);

        Map<String, Object> permResults = null;
        try {
            permResults = dispatcher.runSync("checkAssocPermission", serviceInMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem checking permissions", "ContentServices");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPermissionNotGranted", locale));
        }
        permissionStatus = (String) permResults.get("permissionStatus");

        if (permissionStatus != null && permissionStatus.equals("granted")) {
            try {
                contentAssoc.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errorMsg = ContentWorker.prepPermissionErrorMsg(permResults);
            return ServiceUtil.returnError(errorMsg);
        }

        return result;
    }

    /**
     * Update a ContentAssoc service. The work is done in a separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> deactivateContentAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map<String, Object> result = deactivateContentAssocMethod(dctx, context);
        return result;
    }

    /**
     * Update a ContentAssoc method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> deactivateContentAssocMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");
        context.put("entityOperation", "_UPDATE");
        List<String> targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List<String> contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        GenericValue pk = delegator.makeValue("ContentAssoc");
        pk.setAllFields(context, false, null, Boolean.TRUE);
        pk.setAllFields(context, false, "ca", Boolean.TRUE);

        GenericValue contentAssoc = null;
        try {
            contentAssoc = EntityQuery.use(delegator).from("ContentAssoc").where(pk).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Error:" + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocRetrievingError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }

        if (contentAssoc == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocDeactivatingError", locale));
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        contentAssoc.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
        contentAssoc.put("lastModifiedDate", lastModifiedDate);
        contentAssoc.put("thruDate", UtilDateTime.nowTimestamp());

        String permissionStatus = null;
        Map<String, Object> serviceInMap = new HashMap<String, Object>();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperationList);
        serviceInMap.put("contentPurposeList", contentPurposeList);
        serviceInMap.put("entityOperation", context.get("entityOperation"));
        serviceInMap.put("contentIdTo", contentAssoc.get("contentIdTo"));
        serviceInMap.put("contentIdFrom", contentAssoc.get("contentId"));

        Map<String, Object> permResults = null;
        try {
            permResults = dispatcher.runSync("checkAssocPermission", serviceInMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem checking permissions", "ContentServices");
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPermissionNotGranted", locale));
        }
        permissionStatus = (String) permResults.get("permissionStatus");

        if (permissionStatus != null && permissionStatus.equals("granted")) {
            try {
                contentAssoc.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errorMsg = ContentWorker.prepPermissionErrorMsg(permResults);
            return ServiceUtil.returnError(errorMsg);
        }

        return result;
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
        Map<String, Object> results = new HashMap<String, Object>();

        try {
            GenericValue activeAssoc = null;
            if (fromDate != null) {
                activeAssoc = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", activeContentId, "contentIdTo", contentIdTo, "fromDate", fromDate, "contentAssocTypeId", contentAssocTypeId).queryOne();
                if (activeAssoc == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentAssocNotFound", UtilMisc.toMap("activeContentId", activeContentId, "contentIdTo", contentIdTo, "contentAssocTypeId", contentAssocTypeId, "fromDate", fromDate), locale));
                }
                sequenceNum = (String) activeAssoc.get("sequenceNum");
            }

            List<EntityCondition> exprList = new LinkedList<EntityCondition>();
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

    /**
     * Get and render subcontent associated with template id and mapkey. If subContentId is supplied, that content will be rendered without searching for other
     * matching content.
     */
    public static Map<String, Object> renderSubContentAsText(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String,Object> templateContext = UtilGenerics.checkMap(context.get("templateContext"));
        String contentId = (String) context.get("contentId");

        if (templateContext != null && UtilValidate.isEmpty(contentId)) {
            contentId = (String) templateContext.get("contentId");
        }
        String mapKey = (String) context.get("mapKey");
        if (templateContext != null && UtilValidate.isEmpty(mapKey)) {
            mapKey = (String) templateContext.get("mapKey");
        }
        String mimeTypeId = (String) context.get("mimeTypeId");
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get("mimeTypeId");
        }
        Locale locale = (Locale) context.get("locale");
        if (templateContext != null && locale == null) {
            locale = (Locale) templateContext.get("locale");
        }
        GenericValue subContentDataResourceView = (GenericValue) context.get("subContentDataResourceView");
        if (templateContext != null && subContentDataResourceView == null) {
            subContentDataResourceView = (GenericValue) templateContext.get("subContentDataResourceView");
        }

        Writer out = (Writer) context.get("outWriter");
        Writer outWriter = new StringWriter();

        if (templateContext == null) {
            templateContext = new HashMap<String, Object>();
        }

        try {
            ContentWorker.renderSubContentAsText(dispatcher, delegator, contentId, outWriter, mapKey, templateContext, locale, mimeTypeId, true);
            out.write(outWriter.toString());
            results.put("textData", outWriter.toString());
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering sub-content text", module);
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error rendering sub-content text", module);
            return ServiceUtil.returnError(e.toString());
        }

        return results;

    }

    /**
     * Get and render subcontent associated with template id and mapkey. If subContentId is supplied, that content will be rendered without searching for other
     * matching content.
     */
    public static Map<String, Object> renderContentAsText(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String,Object> results = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Writer out = (Writer) context.get("outWriter");

        Map<String,Object> templateContext = UtilGenerics.checkMap(context.get("templateContext"));
        String contentId = (String) context.get("contentId");
        if (templateContext != null && UtilValidate.isEmpty(contentId)) {
            contentId = (String) templateContext.get("contentId");
        }
        String mimeTypeId = (String) context.get("mimeTypeId");
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get("mimeTypeId");
        }
        Locale locale = (Locale) context.get("locale");
        if (templateContext != null && locale == null) {
            locale = (Locale) templateContext.get("locale");
        }

        if (templateContext == null) {
            templateContext = new HashMap<String, Object>();
        }

        Writer outWriter = new StringWriter();
        GenericValue view = (GenericValue)context.get("subContentDataResourceView");
        if (view != null && view.containsKey("contentId")) {
            contentId = view.getString("contentId");
        }

        try {
            ContentWorker.renderContentAsText(dispatcher, delegator, contentId, outWriter, templateContext, locale, mimeTypeId, null, null, true);
            if (out != null) out.write(outWriter.toString());
            results.put("textData", outWriter.toString());
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering sub-content text", module);
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error rendering sub-content text", module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static Map<String, Object> linkContentToPubPt(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String contentId = (String) context.get("contentId");
        String contentIdTo = (String) context.get("contentIdTo");
        String contentAssocTypeId = (String) context.get("contentAssocTypeId");
        String statusId = (String) context.get("statusId");
        String privilegeEnumId = (String) context.get("privilegeEnumId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (Debug.infoOn()) Debug.logInfo("in publishContent, statusId:" + statusId, module);
        if (Debug.infoOn()) Debug.logInfo("in publishContent, userLogin:" + userLogin, module);

        Map<String, Object> mapIn = new HashMap<String, Object>();
        mapIn.put("contentId", contentId);
        mapIn.put("contentIdTo", contentIdTo);
        mapIn.put("contentAssocTypeId", contentAssocTypeId);
        String publish = (String) context.get("publish");

        try {
            boolean isPublished = false;
            GenericValue contentAssocViewFrom = ContentWorker.getContentAssocViewFrom(delegator, contentIdTo, contentId, contentAssocTypeId, statusId, privilegeEnumId);
            if (contentAssocViewFrom != null)
                isPublished = true;
            if (Debug.infoOn()) Debug.logInfo("in publishContent, contentId:" + contentId + " contentIdTo:" + contentIdTo + " contentAssocTypeId:" + contentAssocTypeId + " publish:" + publish + " isPublished:" + isPublished, module);
            if (UtilValidate.isNotEmpty(publish) && publish.equalsIgnoreCase("Y")) {
                GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                String contentStatusId = (String) content.get("statusId");
                String contentPrivilegeEnumId = (String) content.get("privilegeEnumId");

                if (Debug.infoOn()) Debug.logInfo("in publishContent, statusId:" + statusId + " contentStatusId:" + contentStatusId + " privilegeEnumId:" + privilegeEnumId + " contentPrivilegeEnumId:" + contentPrivilegeEnumId, module);
                // Don't do anything if link was already there
                if (!isPublished) {
                    content.put("privilegeEnumId", privilegeEnumId);
                    content.put("statusId", statusId);
                    content.store();

                    mapIn = new HashMap<String, Object>();
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
                    String errorMsg = ServiceUtil.getErrorMessage(thisResults);
                    if (UtilValidate.isNotEmpty(errorMsg)) {
                        Debug.logError("Problem running deactivateAssocs. " + errorMsg, "ContentServices");
                        return ServiceUtil.returnError(errorMsg);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting existing content", "ContentServices");
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem running deactivateAssocs", "ContentServices");
            return ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    public static Map<String, Object> publishContent(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = new HashMap<String, Object>();
        GenericValue content = (GenericValue)context.get("content");
        
        try {
            content.put("statusId", "CTNT_PUBLISHED");
            content.store();
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getPrefixedMembers(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> mapIn = UtilGenerics.checkMap(context.get("mapIn"));
        String prefix = (String)context.get("prefix");
        Map<String, Object> mapOut = new HashMap<String, Object>();
        result.put("mapOut", mapOut);
        if (mapIn != null) {
            Set<Map.Entry<String, Object>> entrySet = mapIn.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                if (key.startsWith(prefix)) {
                    Object value = entry.getValue();
                    mapOut.put(key, value);
                }
            }
        }
        return result;
    }

    public static Map<String, Object> splitString(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = new HashMap<String, Object>();
        List<String> outputList = new LinkedList<String>();
        String delimiter = UtilFormatOut.checkEmpty((String)context.get("delimiter"), "|");
        String inputString = (String)context.get("inputString");
        if (UtilValidate.isNotEmpty(inputString)) {
            outputList = StringUtil.split(inputString, delimiter);
        }
        result.put("outputList", outputList);
        return result;
    }

    public static Map<String, Object> joinString(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = new HashMap<String, Object>();
        String outputString = null;
        String delimiter = UtilFormatOut.checkEmpty((String)context.get("delimiter"), "|");
        List<String> inputList = UtilGenerics.checkList(context.get("inputList"));
        if (inputList != null) {
            outputString = StringUtil.join(inputList, delimiter);
        }
        result.put("outputString", outputString);
        return result;
    }

    public static Map<String, Object> urlEncodeArgs(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> mapFiltered = new HashMap<String, Object>();
        Map<String, Object> mapIn = UtilGenerics.checkMap(context.get("mapIn"));
        if (mapIn != null) {
            Set<Map.Entry<String, Object>> entrySet = mapIn.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    if (UtilValidate.isNotEmpty(value)) {
                        mapFiltered.put(key, value);
                    }
                } else if (value != null) {
                    mapFiltered.put(key, value);
                }
            }
            String outputString = UtilHttp.urlEncodeArgs(mapFiltered);
            result.put("outputString", outputString);
        }
        return result;
    }

}
