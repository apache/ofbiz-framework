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
package org.ofbiz.scrum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
/**
 * Scrum Services
 */
public class ScrumServices {

    public static final String module = ScrumServices.class.getName();

    public static Map<String, Object> linkToProduct(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String communicationEventId = (String) context.get("communicationEventId");
        // Debug.logInfo("==== Processing Commevent: " +  communicationEventId, module);

        if (UtilValidate.isNotEmpty(communicationEventId)) {
            try {
                GenericValue communicationEvent = delegator.findOne("CommunicationEvent", UtilMisc.toMap("communicationEventId", communicationEventId), false);
                if (UtilValidate.isNotEmpty(communicationEvent)) {
                    String subject = communicationEvent.getString("subject");
                    if (UtilValidate.isNotEmpty(subject)) {
                        int pdLocation = subject.indexOf("PD#");
                        if (pdLocation > 0) {
                            // scan until the first non digit character
                            int nonDigitLocation = pdLocation + 3;
                            while (nonDigitLocation < subject.length() && Character.isDigit(subject.charAt(nonDigitLocation))) {
                                nonDigitLocation++;
                            }
                            String productId = subject.substring(pdLocation + 3, nonDigitLocation);
                            // Debug.logInfo("=======================Product id found in subject: >>" + custRequestId + "<<", module);
                            GenericValue product = delegator.findOne("Product", UtilMisc.toMap("productId", productId), false);
                            if (UtilValidate.isNotEmpty(product)) {
                                GenericValue communicationEventProductMap = delegator.findOne("CommunicationEventProduct", UtilMisc.toMap("productId", productId, "communicationEventId", communicationEventId), false);
                                if (UtilValidate.isEmpty(communicationEventProductMap)) {
                                    GenericValue communicationEventProduct = delegator.makeValue("CommunicationEventProduct", UtilMisc.toMap("productId", productId, "communicationEventId", communicationEventId));
                                    communicationEventProduct.create();
                                }
                                try {
                                    List<GenericValue> productRoleList = delegator.findByAnd("ProductRole", UtilMisc.toMap("productId",productId, "partyId", communicationEvent.getString("partyIdFrom"), "roleTypeId","PRODUCT_OWNER"), null, false);
                                    GenericValue productRoleMap = EntityUtil.getFirst(productRoleList);
                                    GenericValue userLogin = (GenericValue) context.get("userLogin");
                                    // also close the incoming communication event
                                    if (UtilValidate.isNotEmpty(productRoleMap)) {
                                        dispatcher.runSync("setCommunicationEventStatus", UtilMisc.<String, Object>toMap("communicationEventId", communicationEvent.getString("communicationEventId"), "statusId", "COM_COMPLETE", "userLogin", userLogin));
                                    }
                                } catch (GenericServiceException e1) {
                                    Debug.logError(e1, "Error calling updating commevent status", module);
                                    return ServiceUtil.returnError("Error calling updating commevent status:" + e1.toString());
                                }
                            } else {
                                Debug.logInfo("Product id " + productId + " found in subject but not in database", module);
                            }
                        }
                    }
                }

            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("find by primary key error:" + e.toString());
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            return result;
        } else {
            Map<String, Object> result = ServiceUtil.returnError("A communication event id is required");
            return result;
        }
    }

    /**
     * viewScrumRevision
     * <p>
     * Use for view Scrum Revision
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> viewScrumRevision(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String revision = (String) context.get("revision");
        String repository = (String) context.get("repository");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        StringBuilder logMessage = new StringBuilder();
        StringBuilder diffMessage = new StringBuilder();
        try {
            if (UtilValidate.isNotEmpty(repository) && UtilValidate.isNotEmpty(revision)) {
                String logline = null;
                String logCommand = "svn log -r" + revision + " " + repository;
                Process logProcess = Runtime.getRuntime().exec(logCommand);
                BufferedReader logIn = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
                while ((logline = logIn.readLine()) != null) {
                    logMessage.append(logline).append("\n");
                }
                String diffline = null;
                String diffCommand = "svn diff -r" + Integer.toString((Integer.parseInt(revision.trim()) - 1)) + ":" + revision + " " + repository;
                Process diffProcess = Runtime.getRuntime().exec(diffCommand);
                BufferedReader diffIn = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
                while ((diffline = diffIn.readLine()) != null) {
                    diffMessage.append(diffline).append("\n");
                }
            }
            result.put("revision", revision);
            result.put("repository", repository);
            result.put("logMessage", logMessage.toString());
            result.put("diffMessage", diffMessage.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    /**
     * retrieveMissingScrumRevision
     * <p>
     * Use for retrieve the missing data of the Revision
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> retrieveMissingScrumRevision(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String latestRevision = (String) context.get("latestRevision");
        String repositoryRoot = (String) context.get("repositoryRoot");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            if (UtilValidate.isNotEmpty(repositoryRoot) && UtilValidate.isNotEmpty(latestRevision)) {
                Integer revision = Integer.parseInt(latestRevision.trim());
                for (int i = 1; i <= revision; i++) {
                    String logline = null;
                    List<String> logMessageList = FastList.newInstance();
                    String logCommand = "svn log -r" + i + " " + repositoryRoot;
                    Process logProcess = Runtime.getRuntime().exec(logCommand);
                    BufferedReader logIn = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
                    while ((logline = logIn.readLine()) != null) {
                        logMessageList.add(logline.toString().trim());
                    }
                    if (UtilValidate.isNotEmpty(logMessageList)) {
                        String userInfo = logMessageList.get(1).replace(" | ", ",");
                        String taskInfo = logMessageList.get(3);
                        // get user information
                        String[] versionInfoTemp = userInfo.split(",");
                        String user = versionInfoTemp[1];
                        // get task information
                        String taskId = null;
                        char[] taskInfoList = taskInfo.toCharArray();
                        int count = 0;
                        for (int j = 0; j < taskInfoList.length; j++) {
                            if (Character.isDigit(taskInfoList[j])) {
                                count = count + 1;
                            } else {
                                count = 0;
                            }
                            if (count == 5) {
                                taskId = taskInfo.substring(j - 4, j + 1);
                            }
                        }
                        String revisionLink = repositoryRoot.substring(repositoryRoot.lastIndexOf("svn/") + 4, repositoryRoot.length()) + "&revision=" + i;
                        Debug.logInfo("Revision Link ============== >>>>>>>>>>> "+ revisionLink, module);
                        if (UtilValidate.isNotEmpty(taskId)) {
                            String version = "R" + i;
                            List <GenericValue> workeffContentList = delegator.findByAnd("WorkEffortAndContentDataResource", UtilMisc.toMap("contentName",version.trim() ,"drObjectInfo", revisionLink.trim()), null, false);
                            List<EntityCondition> exprsAnd = FastList.newInstance();
                            exprsAnd.add(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, taskId));

                            List<EntityCondition> exprsOr = FastList.newInstance();
                            exprsOr.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_ERROR"));
                            exprsOr.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_TEST"));
                            exprsOr.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_IMPL"));
                            exprsOr.add(EntityCondition.makeCondition("workEffortTypeId", EntityOperator.EQUALS, "SCRUM_TASK_INST"));
                            exprsAnd.add(EntityCondition.makeCondition(exprsOr, EntityOperator.OR));

                            List<GenericValue> workEffortList = delegator.findList("WorkEffort", EntityCondition.makeCondition(exprsAnd, EntityOperator.AND), null, null, null, false);
                            if (UtilValidate.isEmpty(workeffContentList) && UtilValidate.isNotEmpty(workEffortList)) {
                                Map<String, Object> inputMap = FastMap.newInstance();
                                inputMap.put("taskId", taskId);
                                inputMap.put("user", user);
                                inputMap.put("revisionNumber", Integer.toString(i));
                                inputMap.put("revisionLink", revisionLink);
                                inputMap.put("revisionDescription", taskInfo);
                                inputMap.put("userLogin", userLogin);
                                Debug.logInfo("inputMap ============== >>>>>>>>>>> "+ inputMap, module);
                                dispatcher.runSync("updateScrumRevision", inputMap);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException entityEx) {
            entityEx.printStackTrace();
            return ServiceUtil.returnError(entityEx.getMessage());
        } catch (GenericServiceException serviceEx) {
            serviceEx.printStackTrace();
            return ServiceUtil.returnError(serviceEx.getMessage());
        }

        return result;
    }

    /**
     * removeDuplicateScrumRevision
     * <p>
     * Use for remove duplicate scrum revision
     *
     * @param ctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service.
     */
    public static Map<String, Object> removeDuplicateScrumRevision(DispatchContext ctx, Map<String, ? extends Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String repositoryRoot = (String) context.get("repositoryRoot");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            List<EntityCondition> exprsAnd = FastList.newInstance();
            String revisionLink = repositoryRoot.substring(repositoryRoot.lastIndexOf("svn/") + 4, repositoryRoot.length()) + "&revision=";
            exprsAnd.add(EntityCondition.makeCondition("workEffortContentTypeId", EntityOperator.EQUALS, "TASK_SUB_INFO"));
            exprsAnd.add(EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "DOCUMENT"));
            exprsAnd.add(EntityCondition.makeCondition("drObjectInfo", EntityOperator.LIKE, revisionLink + "%"));
            List<GenericValue> workEffortDataResourceList = delegator.findList("WorkEffortAndContentDataResource", EntityCondition.makeCondition(exprsAnd, EntityOperator.AND), null, null, null, false);
            if (UtilValidate.isNotEmpty(workEffortDataResourceList)) {
                Debug.logInfo("Total Content Size ============== >>>>>>>>>>> "+ workEffortDataResourceList.size(), module);
                Set<String> keys = FastSet.newInstance();
                Set<GenericValue> exclusions = FastSet.newInstance();
                for (GenericValue workEffort : workEffortDataResourceList) {
                    String drObjectInfo = workEffort.getString("drObjectInfo");
                    if (keys.contains(drObjectInfo)) {
                        exclusions.add(workEffort);
                    } else {
                        keys.add(drObjectInfo);
                    }
                }
                // remove the duplicate entry
                Debug.logInfo("Remove size ============== >>>>>>>>>>> "+ exclusions.size(), module);
                if (UtilValidate.isNotEmpty(exclusions)) {
                    for (GenericValue contentResourceMap : exclusions) {
                        Debug.logInfo("Remove contentId ============== >>>>>>>>>>> "+ contentResourceMap.getString("contentId"), module);
                        GenericValue dataResourceMap = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", contentResourceMap.getString("dataResourceId")), false);
                        GenericValue contentMap = delegator.findOne("Content", UtilMisc.toMap("contentId", contentResourceMap.getString("contentId")), false);
                        contentMap.removeRelated("WorkEffortContent");
                        contentMap.removeRelated("ContentRole");
                        contentMap.remove();
                        dataResourceMap.removeRelated("DataResourceRole");
                        dataResourceMap.remove();
                    }
                }
            }
        } catch (GenericEntityException entityEx) {
            entityEx.printStackTrace();
            return ServiceUtil.returnError(entityEx.getMessage());
        }
        return result;
    }
}
