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
package org.apache.ofbiz.content;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * ContentManagementEvents Class
 */
public class ContentManagementEvents {

    public static final String module = ContentManagementEvents.class.getName();

    public static String updateStaticValues(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Security security = (Security)request.getAttribute("security");
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        String webSiteId = WebSiteWorker.getWebSiteId(request);
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        Map<String, Object> result = new HashMap<>();
        String parentPlaceholderId = (String)paramMap.get("ph");
        if (UtilValidate.isEmpty(parentPlaceholderId)) {
            request.setAttribute("_ERROR_MESSAGE_", "ParentPlaceholder is empty.");
            return "error";
        }
        List<GenericValue> allPublishPointList = null;
        List<String []> permittedPublishPointList = null;
        List<Map<String, Object>> valueList = null;
        try {
            allPublishPointList = ContentManagementWorker.getAllPublishPoints(delegator, webSiteId);
            permittedPublishPointList = ContentManagementWorker.getPermittedPublishPoints(delegator, allPublishPointList, userLogin, security, "_ADMIN", null, null);
            valueList = ContentManagementWorker.getStaticValues(delegator, parentPlaceholderId, permittedPublishPointList);
        } catch (GeneralException e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }

        int counter = 0;
        for (Map<String, Object> map : valueList) {
            String contentId = (String)map.get("contentId");
            for (String [] pubArr : permittedPublishPointList) {
                String pubContentId = pubArr[0];
                String pubValue = (String)map.get(pubContentId);
                String paramName = Integer.toString(counter)  + "_" + pubContentId;
                String paramValue = (String)paramMap.get(paramName);
                Map<String, Object> serviceIn = new HashMap<>();
                serviceIn.put("userLogin", userLogin);
                serviceIn.put("contentIdTo", contentId);
                serviceIn.put("contentId", pubContentId);
                serviceIn.put("contentAssocTypeId", "SUBSITE");
                try {
                    if (UtilValidate.isNotEmpty(paramValue)) {
                        if (!paramValue.equals(pubValue)) {
                            if ("Y".equalsIgnoreCase(paramValue)) {
                                serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
                                result = dispatcher.runSync("createContentAssoc", serviceIn);
                                if (ServiceUtil.isError(result)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(result);
                                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                                    Debug.logError(errorMessage, module);
                                    return "error";
                                }
                            } else if ("N".equalsIgnoreCase(paramValue) && "Y".equalsIgnoreCase(pubValue)) {
                                serviceIn.put("thruDate", UtilDateTime.nowTimestamp());
                                Timestamp fromDate = (Timestamp)map.get(pubContentId + "FromDate");
                                serviceIn.put("fromDate", fromDate);
                                result = dispatcher.runSync("updateContentAssoc", serviceIn);
                                if (ServiceUtil.isError(result)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(result);
                                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                                    Debug.logError(errorMessage, module);
                                    return "error";
                                }
                            }
                        }
                    } else if (UtilValidate.isNotEmpty(pubValue)) {
                        if ("Y".equalsIgnoreCase(pubValue)) {
                                serviceIn.put("thruDate", UtilDateTime.nowTimestamp());
                                Timestamp fromDate = (Timestamp)map.get(pubContentId + "FromDate");
                                serviceIn.put("fromDate", fromDate);
                                result = dispatcher.runSync("updateContentAssoc", serviceIn);
                                if (ServiceUtil.isError(result)) {
                                    String errorMessage = ServiceUtil.getErrorMessage(result);
                                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                                    Debug.logError(errorMessage, module);
                                    return "error";
                                }
                        }
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e.getMessage(), module);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
                }
            }
            counter++;
        }
        return "success";
    }

    public static String createStaticValue(HttpServletRequest request, HttpServletResponse response) {
        String retValue = "success";
        return retValue;
    }

    public static String updatePublishLinks(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Security security = (Security)request.getAttribute("security");
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        String webSiteId = WebSiteWorker.getWebSiteId(request);
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String targContentId = (String)paramMap.get("contentId"); // The content to be linked to one or more sites
        String roles = null;
        String authorId = null;
        GenericValue authorContent = ContentManagementWorker.getAuthorContent(delegator, targContentId);
        if (authorContent != null) {
            authorId = authorContent.getString("contentId");
        } else {
            request.setAttribute("_ERROR_MESSAGE_", "authorContent is empty.");
            return "error";
        }

        // Determine if user is owner of target content
        String userLoginId = userLogin.getString("userLoginId");
        List<String> roleTypeList = null;
        if (authorId != null && userLoginId != null && authorId.equals(userLoginId)) {
            roles = "OWNER";
            roleTypeList = StringUtil.split(roles, "|");
        }
        List<String> targetOperationList = UtilMisc.<String>toList("CONTENT_PUBLISH");
        // TODO check the purpose of this list and if we want to make use of it. Else remove
        List<String> contentPurposeList = null; //UtilMisc.toList("ARTICLE");
        String permittedAction = (String)paramMap.get("permittedAction"); // The content to be linked to one or more sites
        String permittedOperations = (String)paramMap.get("permittedOperations"); // The content to be linked to one or more sites
        if (UtilValidate.isEmpty(targContentId)) {
            request.setAttribute("_ERROR_MESSAGE_", "targContentId is empty.");
            return "error";
        }

        // Get all the subSites that the user is permitted to link to
        List<Object []> origPublishedLinkList = null;
        try {
            // TODO: this needs to be given author userLogin
            EntityQuery.use(delegator).from("UserLogin").where("userLoginId", authorId).cache().queryOne();
            origPublishedLinkList = ContentManagementWorker.getPublishedLinks(delegator, targContentId, webSiteId, userLogin, security, permittedAction, permittedOperations, roles);
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch (GeneralException e2) {
            request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
            return "error";
        }

        // make a map of the values that are passed in using the top subSite as the key.
        // Content can only be linked to one subsite under a top site (ends with "_MASTER")
        Map<String, String> siteIdLookup = new HashMap<>();
        for (Entry<String, Object> entry : paramMap.entrySet()) {
            String param = entry.getKey();
            int pos = param.indexOf("select_");
            if (pos >= 0) {
                String siteId = param.substring(7);
                String subSiteVal = (String)paramMap.get(param);
                siteIdLookup.put(siteId, subSiteVal);
            }
        }

        // Loop thru all the possible subsites
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        boolean statusIdUpdated = false;
        Map<String, Object> result = new HashMap<>();
        for (Object [] arr : origPublishedLinkList) {
            String contentId = (String)arr[0]; // main (2nd level) site id
            String origSubContentId = null;
            List<Object []> origSubList = UtilGenerics.cast(arr[1]);
            Timestamp origFromDate = null;
            for (Object [] pubArr : origSubList) {
                Timestamp fromDate = (Timestamp)pubArr[2];
                if (fromDate != null) {
                    origSubContentId = (String)pubArr[0];
                    origFromDate = fromDate;
                    break;
                }
            }

            String currentSubContentId = siteIdLookup.get(contentId);
            try {
                if (UtilValidate.isNotEmpty(currentSubContentId)) {
                    if (!currentSubContentId.equals(origSubContentId)) {
                        // disable existing link
                        if (UtilValidate.isNotEmpty(origSubContentId) && origFromDate != null) {
                            List<GenericValue> oldActiveValues = EntityQuery.use(delegator).from("ContentAssoc")
                                    .where("contentId", targContentId, 
                                            "contentIdTo", origSubContentId, 
                                            "contentAssocTypeId", "PUBLISH_LINK", 
                                            "thruDate", null)
                                    .queryList();
                            for (GenericValue cAssoc : oldActiveValues) {
                                cAssoc.set("thruDate", nowTimestamp);
                                cAssoc.store();
                            }
                        }
                        // create new link
                        Map<String, Object> serviceIn = new HashMap<>();
                        serviceIn.put("userLogin", userLogin);
                        serviceIn.put("contentId", targContentId);
                        serviceIn.put("contentAssocTypeId", "PUBLISH_LINK");
                        serviceIn.put("fromDate", nowTimestamp);
                        serviceIn.put("contentIdTo", currentSubContentId);
                        serviceIn.put("roleTypeList", roleTypeList);
                        serviceIn.put("targetOperationList", targetOperationList);
                        // TODO check if this should be removed (see above)
                        serviceIn.put("contentPurposeList", contentPurposeList);
                        result = dispatcher.runSync("createContentAssoc", serviceIn);
                        if (ServiceUtil.isError(result)) {
                            String errorMessage = ServiceUtil.getErrorMessage(result);
                            request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                            Debug.logError(errorMessage, module);
                            return "error";
                        }

                        serviceIn = new HashMap<>();
                        serviceIn.put("userLogin", userLogin);
                        serviceIn.put("contentId", targContentId);
                        serviceIn.put("contentAssocTypeId", "PUBLISH_LINK");
                        serviceIn.put("fromDate", nowTimestamp);
                        serviceIn.put("contentIdTo", contentId);
                        serviceIn.put("roleTypeList", roleTypeList);
                        serviceIn.put("targetOperationList", targetOperationList);
                        // TODO check if this should be removed (see above)
                        serviceIn.put("contentPurposeList", contentPurposeList);
                        result = dispatcher.runSync("createContentAssoc", serviceIn);
                        if (ServiceUtil.isError(result)) {
                            String errorMessage = ServiceUtil.getErrorMessage(result);
                            request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                            Debug.logError(errorMessage, module);
                            return "error";
                        }
                        if (!statusIdUpdated) {
                            try {
                                GenericValue targContent = EntityQuery.use(delegator).from("Content").where("contentId", targContentId).queryOne();
                                targContent.set("statusId", "CTNT_PUBLISHED");
                                targContent.store();
                                statusIdUpdated = true;
                            } catch (GenericEntityException e) {
                                Debug.logError(e.getMessage(), module);
                                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                                return "error";
                            }
                        }
                    }
                } else if (UtilValidate.isNotEmpty(origSubContentId)) {
                    // if no current link is passed in, look to see if there is an existing link(s) that must be disabled
                    List<GenericValue> oldActiveValues = EntityQuery.use(delegator).from("ContentAssoc")
                            .where("contentId", targContentId, 
                                    "contentIdTo", origSubContentId, 
                                    "contentAssocTypeId", "PUBLISH_LINK", 
                                    "thruDate", null)
                            .queryList();
                    for (GenericValue cAssoc : oldActiveValues) {
                        cAssoc.set("thruDate", nowTimestamp);
                        cAssoc.store();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            } catch (GenericServiceException e2) {
                Debug.logError(e2, module);
                request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
                return "error";
            }
        }
        return "success";
    }

}
