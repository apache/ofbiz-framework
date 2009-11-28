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
package org.ofbiz.content.content;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * ContentServices Class
 */
public class ContentServices {

    public static final String module = ContentServices.class.getName();

    /**
     * findRelatedContent Finds the related
     */
    public static Map<String, Object> findRelatedContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map results = FastMap.newInstance();

        GenericValue currentContent = (GenericValue) context.get("currentContent");
        String fromDate = (String) context.get("fromDate");
        String thruDate = (String) context.get("thruDate");
        String toFrom = (String) context.get("toFrom");
        if (toFrom == null) {
            toFrom = "TO";
        } else {
            toFrom = toFrom.toUpperCase();
        }

        List assocTypes = (List) context.get("contentAssocTypeList");
        List targetOperations = (List) context.get("targetOperationList");
        List contentList = null;
        List contentTypes = (List) context.get("contentTypeList");

        try {
            contentList = ContentWorker.getAssociatedContent(currentContent, toFrom, assocTypes, contentTypes, fromDate, thruDate);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error getting associated content: " + e.toString());
        }

        if (targetOperations == null || targetOperations.isEmpty()) {
            results.put("contentList", contentList);
            return results;
        }

        Map serviceInMap = FastMap.newInstance();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperations);
        serviceInMap.put("entityOperation", context.get("entityOperation"));

        List permittedList = FastList.newInstance();
        Iterator it = contentList.iterator();
        Map permResults = null;
        while (it.hasNext()) {
            GenericValue content = (GenericValue) it.next();
            serviceInMap.put("currentContent", content);
            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
                return ServiceUtil.returnError("Problem checking permissions");
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
        Map results = FastMap.newInstance();
        List parentList = FastList.newInstance();
        results.put("parentList", parentList);
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String)context.get("contentId");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        String direction = (String)context.get("direction");
        if (UtilValidate.isEmpty(direction)) direction="To";
        Map traversMap = FastMap.newInstance();
            traversMap.put("contentId", contentId);
            traversMap.put("direction", direction);
            traversMap.put("contentAssocTypeId", contentAssocTypeId);
            try {
                Map thisResults = dispatcher.runSync("traverseContent", traversMap);
            String errorMsg = ServiceUtil.getErrorMessage(thisResults);
            if (UtilValidate.isNotEmpty(errorMsg)) {
                Debug.logError("Problem in traverseContent. " + errorMsg, module);
                return ServiceUtil.returnError(errorMsg);
            }
            Map nodeMap = (Map)thisResults.get("nodeMap");
            walkParentTree(nodeMap, parentList);
            } catch (GenericServiceException e) {
            return ServiceUtil.returnFailure(e.getMessage());
            }
        return results;
    }

    private static void walkParentTree(Map nodeMap, List parentList) {
        List kids = (List)nodeMap.get("kids");
        if (UtilValidate.isEmpty(kids)) {
            parentList.add(nodeMap.get("contentId"));
        } else {
            Iterator iter = kids.iterator();
            while (iter.hasNext()) {
                Map node = (Map) iter.next();
                walkParentTree(node, parentList);
            }
        }
    }
    /**
     * This is a generic service for traversing a Content tree, typical of a blog response tree. It calls the ContentWorker.traverse method.
     */
    public static Map<String, Object> traverseContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map results = FastMap.newInstance();

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
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
        } catch (GenericEntityException e) {
            System.out.println("Entity Error:" + e.getMessage());
            return ServiceUtil.returnError("Error in retrieving Content. " + e.getMessage());
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

        Map whenMap = FastMap.newInstance();
        whenMap.put("followWhen", context.get("followWhen"));
        whenMap.put("pickWhen", context.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", context.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", context.get("returnAfterPickWhen"));

        String startContentAssocTypeId = (String) context.get("contentAssocTypeId");
        if (startContentAssocTypeId != null) {
            startContentAssocTypeId = "PUBLISH";
        }

        Map nodeMap = FastMap.newInstance();
        List pickList = FastList.newInstance();
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
        /*
        context.put("entityOperation", "_CREATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList); // for checking permissions
        //context.put("skipPermissionCheck", null);
        */

        Map result = createContentMethod(dctx, context);
        return result;
    }

    /**
     * Create a Content method. The work is done in this separate method so that complex services that need this functionality do not need to incur the
     * reflection performance penalty.
     */
    public static Map<String, Object> createContentMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_CREATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");
        if (Debug.infoOn()) Debug.logInfo("in createContentMethod, targetOperationList: " + targetOperationList, null);

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Map result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String) context.get("contentId");
        //String contentTypeId = (String) context.get("contentTypeId");

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
                List statusItems = delegator.findByAnd("StatusItem",UtilMisc.toMap("statusTypeId", "CONTENT_STATUS"), UtilMisc.toList("sequenceId"));
                if (!UtilValidate.isEmpty(statusItems)) {
                    content.put("statusId",  ((GenericValue) statusItems.get(0)).getString("statusId"));
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

        Map permResults = ContentWorker.callContentPermissionCheckResult(delegator, dispatcher, context);
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
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map result = null;
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
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_CREATE");
        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map result = FastMap.newInstance();

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
            return ServiceUtil.returnError("Not 2 out of ContentId/To/From");
        }

        if (UtilValidate.isNotEmpty(contentIdFrom)) {
            if (UtilValidate.isEmpty(contentIdTo))
                contentIdTo = contentId;
        }
        if (UtilValidate.isNotEmpty(contentIdTo)) {
            if (UtilValidate.isEmpty(contentIdFrom))
                contentIdFrom = contentId;
        }

        /*
        String deactivateExisting = (String) context.get("deactivateExisting");
        if (deactivateExisting != null && deactivateExisting.equalsIgnoreCase("true")) {
            Map andMap = FastMap.newInstance();
            andMap.put("contentIdTo", contentIdTo);
            andMap.put("contentAssocTypeId", context.get("contentAssocTypeId"));

            String mapKey = (String) context.get("mapKey");
            if (UtilValidate.isNotEmpty(mapKey)) {
                andMap.put("mapKey", mapKey);
            }
            if (Debug.infoOn()) Debug.logInfo("DEACTIVATING CONTENTASSOC andMap: " + andMap, null);

            List assocList = delegator.findByAnd("ContentAssoc", andMap);
            Iterator iter = assocList.iterator();
            while (iter.hasNext()) {
                GenericValue val = (GenericValue) iter.next();
                if (Debug.infoOn()) Debug.logInfo("DEACTIVATING CONTENTASSOC val: " + val, null);

                val.set("thruDate", UtilDateTime.nowTimestamp());
                val.store();
            }
        }
        */

        GenericValue contentAssoc = delegator.makeValue("ContentAssoc", FastMap.newInstance());
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

        Map serviceInMap = FastMap.newInstance();
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

        Map permResults = null;
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
        //Debug.logInfo("result:" + result, module);
        //Debug.logInfo("contentAssoc:" + contentAssoc, module);

        return result;
    }

    /**
     * A service wrapper for the updateContentMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateContent(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("entityOperation", "_UPDATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map result = updateContentMethod(dctx, context);
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
        Map result = FastMap.newInstance();

        context.put("entityOperation", "_UPDATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        GenericValue content = null;
        //Locale locale = (Locale) context.get("locale");
        String contentId = (String) context.get("contentId");
        try {
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("content.update.read_failure" + e.getMessage());
        }
        context.put("currentContent", content);

        Map permResults = ContentWorker.callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String lastModifiedByUserLogin = userLoginId;
            Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

            // update status first to see if allowed
            if (UtilValidate.isNotEmpty(context.get("statusId"))) {
                Map statusInMap = UtilMisc.toMap("contentId", context.get("contentId"), "statusId", context.get("statusId"),"userLogin", userLogin);
                try {
                   dispatcher.runSync("setContentStatus", statusInMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem updating content Status", "ContentServices");
                    return ServiceUtil.returnError("Problem updating content Status: " + e);
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
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map result = updateContentAssocMethod(dctx, context);
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
        Map result = FastMap.newInstance();

        context.put("entityOperation", "_UPDATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
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
            contentAssoc = delegator.findByPrimaryKey("ContentAssoc", UtilMisc.toMap("contentId", contentId, "contentIdTo", contentIdTo, "contentAssocTypeId", contentAssocTypeId, "fromDate", fromDate));
        } catch (GenericEntityException e) {
            System.out.println("Entity Error:" + e.getMessage());
            return ServiceUtil.returnError("Error in retrieving Content. " + e.getMessage());
        }
        if (contentAssoc == null) {
            return ServiceUtil.returnError("Error in updating ContentAssoc. Entity is null.");
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
        Map serviceInMap = FastMap.newInstance();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperationList);
        serviceInMap.put("contentPurposeList", contentPurposeList);
        serviceInMap.put("entityOperation", context.get("entityOperation"));
        serviceInMap.put("contentIdTo", contentIdTo);
        serviceInMap.put("contentIdFrom", contentIdFrom);

        Map permResults = null;
        try {
            permResults = dispatcher.runSync("checkAssocPermission", serviceInMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem checking permissions", "ContentServices");
            return ServiceUtil.returnError("Problem checking association permissions");
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
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);
        context.put("skipPermissionCheck", null);

        Map result = deactivateContentAssocMethod(dctx, context);
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
        Map result = FastMap.newInstance();

        context.put("entityOperation", "_UPDATE");
        List targetOperationList = ContentWorker.prepTargetOperationList(context, "_UPDATE");

        List contentPurposeList = ContentWorker.prepContentPurposeList(context);
        context.put("targetOperationList", targetOperationList);
        context.put("contentPurposeList", contentPurposeList);

        GenericValue pk = delegator.makeValue("ContentAssoc");
        pk.setAllFields(context, false, null, Boolean.TRUE);
        pk.setAllFields(context, false, "ca", Boolean.TRUE);
        //String contentIdFrom = (String) context.get("contentId");
        //String contentIdTo = (String) context.get("contentIdTo");
        //String contentId = (String) context.get("contentId");
        //String contentAssocTypeId = (String) context.get("contentAssocTypeId");
        //Timestamp fromDate = (Timestamp) context.get("fromDate");

        GenericValue contentAssoc = null;
        try {
            //contentAssoc = delegator.findByPrimaryKey("ContentAssoc", UtilMisc.toMap("contentId", contentId, "contentIdTo", contentIdTo, "contentAssocTypeId", contentAssocTypeId, "fromDate", fromDate));
            contentAssoc = delegator.findByPrimaryKey("ContentAssoc", pk);
        } catch (GenericEntityException e) {
            System.out.println("Entity Error:" + e.getMessage());
            return ServiceUtil.returnError("Error in retrieving Content. " + e.getMessage());
        }

        if (contentAssoc == null) {
            return ServiceUtil.returnError("Error in deactivating ContentAssoc. Entity is null.");
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        contentAssoc.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
        contentAssoc.put("lastModifiedDate", lastModifiedDate);
        contentAssoc.put("thruDate", UtilDateTime.nowTimestamp());

        String permissionStatus = null;
        Map serviceInMap = FastMap.newInstance();
        serviceInMap.put("userLogin", context.get("userLogin"));
        serviceInMap.put("targetOperationList", targetOperationList);
        serviceInMap.put("contentPurposeList", contentPurposeList);
        serviceInMap.put("entityOperation", context.get("entityOperation"));
        serviceInMap.put("contentIdTo", contentAssoc.get("contentIdTo"));
        serviceInMap.put("contentIdFrom", contentAssoc.get("contentId"));

        Map permResults = null;
        try {
            permResults = dispatcher.runSync("checkAssocPermission", serviceInMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem checking permissions", "ContentServices");
            return ServiceUtil.returnError("Problem checking association permissions");
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
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String sequenceNum = null;
        Map results = FastMap.newInstance();

        try {
            GenericValue activeAssoc = null;
            if (fromDate != null) {
                activeAssoc = delegator.findByPrimaryKey("ContentAssoc", UtilMisc.toMap("contentId", activeContentId, "contentIdTo", contentIdTo, "fromDate", fromDate, "contentAssocTypeId", contentAssocTypeId));
                if (activeAssoc == null) {
                    return ServiceUtil.returnError("No association found for contentId=" + activeContentId + " and contentIdTo=" + contentIdTo
                            + " and contentAssocTypeId=" + contentAssocTypeId + " and fromDate=" + fromDate);
                }
                sequenceNum = (String) activeAssoc.get("sequenceNum");
            }

            List exprList = FastList.newInstance();
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

            EntityConditionList assocExprList = EntityCondition.makeCondition(exprList, EntityOperator.AND);
            List relatedAssocs = delegator.findList("ContentAssoc", assocExprList, null, UtilMisc.toList("fromDate"), null, false);
            //if (Debug.infoOn()) Debug.logInfo("in deactivateAssocs, relatedAssocs:" + relatedAssocs, module);
            List filteredAssocs = EntityUtil.filterByDate(relatedAssocs);
            //if (Debug.infoOn()) Debug.logInfo("in deactivateAssocs, filteredAssocs:" + filteredAssocs, module);

            Iterator it = filteredAssocs.iterator();
            while (it.hasNext()) {
                GenericValue val = (GenericValue) it.next();
                val.set("thruDate", nowTimestamp);
                val.store();
                //if (Debug.infoOn()) Debug.logInfo("in deactivateAssocs, val:" + val, module);
            }
            results.put("deactivatedList", filteredAssocs);
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
        Map results = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map templateContext = (Map) context.get("templateContext");
        String contentId = (String) context.get("contentId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (templateContext != null && UtilValidate.isEmpty(contentId)) {
            contentId = (String) templateContext.get("contentId");
        }
        String mapKey = (String) context.get("mapKey");
        if (templateContext != null && UtilValidate.isEmpty(mapKey)) {
            mapKey = (String) templateContext.get("mapKey");
        }
        //String subContentId = (String) context.get("subContentId");
        //if (templateContext != null && UtilValidate.isEmpty(subContentId)) {
            //subContentId = (String) templateContext.get("subContentId");
        //}
        String mimeTypeId = (String) context.get("mimeTypeId");
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get("mimeTypeId");
        }
        Locale locale = (Locale) context.get("locale");
        if (templateContext != null && locale == null) {
            locale = (Locale) templateContext.get("locale");
        }
        GenericValue subContentDataResourceView = (GenericValue) context.get("subContentDataResourceView");
        if (subContentDataResourceView != null && subContentDataResourceView == null) {
            subContentDataResourceView = (GenericValue) templateContext.get("subContentDataResourceView");
        }

        Writer out = (Writer) context.get("outWriter");
        Writer outWriter = new StringWriter();

        if (templateContext == null) {
            templateContext = FastMap.newInstance();
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
        Map results = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Writer out = (Writer) context.get("outWriter");

        Map templateContext = (Map) context.get("templateContext");
        //GenericValue userLogin = (GenericValue)context.get("userLogin");
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
            templateContext = FastMap.newInstance();
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
        Map results = FastMap.newInstance();
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

        Map mapIn = FastMap.newInstance();
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
                GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                String contentStatusId = (String) content.get("statusId");
                String contentPrivilegeEnumId = (String) content.get("privilegeEnumId");

                if (Debug.infoOn()) Debug.logInfo("in publishContent, statusId:" + statusId + " contentStatusId:" + contentStatusId + " privilegeEnumId:" + privilegeEnumId + " contentPrivilegeEnumId:" + contentPrivilegeEnumId, module);
                // Don't do anything if link was already there
                if (!isPublished) {
                    //Map thisResults = dispatcher.runSync("deactivateAssocs", mapIn);
                    //String errorMsg = ServiceUtil.getErrorMessage(thisResults);
                    //if (UtilValidate.isNotEmpty(errorMsg)) {
                    //Debug.logError("Problem running deactivateAssocs. " + errorMsg, "ContentServices");
                    //return ServiceUtil.returnError(errorMsg);
                    //}
                    content.put("privilegeEnumId", privilegeEnumId);
                    content.put("statusId", statusId);
                    content.store();

                    mapIn = FastMap.newInstance();
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
                    Map thisResults = dispatcher.runSync("deactivateAssocs", mapIn);
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

        Map result = FastMap.newInstance();
        GenericValue content = (GenericValue)context.get("content");
        GenericValue userLogin = (GenericValue)context.get("userLogin");

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

        Map result = FastMap.newInstance();
        Map mapIn = (Map)context.get("mapIn");
        String prefix = (String)context.get("prefix");
        Map mapOut = FastMap.newInstance();
        result.put("mapOut", mapOut);
        if (mapIn != null) {
            Set entrySet = mapIn.entrySet();
            Iterator iter = entrySet.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                String key = (String)entry.getKey();
                if (key.startsWith(prefix)) {
                    String keyBase = key.substring(prefix.length());
                    Object value = entry.getValue();
                    mapOut.put(key, value);
                }
            }
        }
        return result;
    }

    public static Map<String, Object> splitString(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map result = FastMap.newInstance();
        List outputList = FastList.newInstance();
        String delimiter = UtilFormatOut.checkEmpty((String)context.get("delimiter"), "|");
        String inputString = (String)context.get("inputString");
        if (UtilValidate.isNotEmpty(inputString)) {
            outputList = StringUtil.split(inputString, delimiter);
        }
        result.put("outputList", outputList);
        return result;
    }

    public static Map<String, Object> joinString(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map result = FastMap.newInstance();
        String outputString = null;
        String delimiter = UtilFormatOut.checkEmpty((String)context.get("delimiter"), "|");
        List inputList = (List)context.get("inputList");
        if (inputList != null) {
            outputString = StringUtil.join(inputList, delimiter);
        }
        result.put("outputString", outputString);
        return result;
    }

    public static Map<String, Object> urlEncodeArgs(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{

        Map result = FastMap.newInstance();
        Map mapFiltered = FastMap.newInstance();
        Map mapIn = (Map)context.get("mapIn");
        if (mapIn != null) {
            Set entrySet = mapIn.entrySet();
            Iterator iter = entrySet.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                String key = (String)entry.getKey();
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
