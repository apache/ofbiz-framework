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
package org.ofbiz.content;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;



/**
 * ContentManagementEvents Class
 */
public class ContentManagementEvents {

    public static final String module = ContentManagementEvents.class.getName();

    public static String updateStaticValues(HttpServletRequest request, HttpServletResponse response) {

        HttpSession session = request.getSession();
        Security security = (Security)request.getAttribute("security");
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        ServletContext servletContext = session.getServletContext();
        String webSiteId = (String) servletContext.getAttribute("webSiteId");
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map paramMap = UtilHttp.getParameterMap(request);
                //if (Debug.infoOn()) Debug.logInfo("in updateStaticValues, paramMap:" + paramMap , module);
        String parentPlaceholderId = (String)paramMap.get("ph");
        if ( UtilValidate.isEmpty(parentPlaceholderId)) {
            request.setAttribute("_ERROR_MESSAGE_", "ParentPlaceholder is empty.");
            return "error";
        }
        List allPublishPointList = null;
        List permittedPublishPointList = null;
        List valueList = null;
        try {
            allPublishPointList = ContentManagementWorker.getAllPublishPoints(delegator, webSiteId);
            permittedPublishPointList = ContentManagementWorker.getPermittedPublishPoints(delegator, allPublishPointList, userLogin, security, "_ADMIN", null, null);
            valueList = ContentManagementWorker.getStaticValues(delegator, parentPlaceholderId, permittedPublishPointList);
        } catch(GeneralException e) {
            Debug.logError(e.getMessage(), module);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
/*
        Set keySet = paramMap.keySet();
        Iterator itKeySet = keySet.iterator();
        Map contentIdLookup = new HashMap();
        while (itKeySet.hasNext()) {
            String idxAndContentId = (String)itKeySet.next();
            int pos = idxAndContentId.indexOf("_");
            if (pos > 0) {
                String idxStr = idxAndContentId.substring(0, pos);
                int idx = Integer.parseInt(idxStr);
                String contentId = idxAndContentId.substring(pos + 1);
                contentIdLookup.put(contentId, new Integer(idx));
            }
        }
*/

        Iterator it = valueList.iterator();
        int counter = 0;
        while (it.hasNext()) {
            Map map = (Map)it.next();
            String contentId = (String)map.get("contentId");
            //Integer idxObj = (Integer)contentIdLookup.get(contentId);
            //int idx = idxObj.intValue();
            Iterator itPubPt = permittedPublishPointList.iterator();
            while (itPubPt.hasNext()) {
                String [] pubArr = (String [])itPubPt.next();
                String pubContentId = pubArr[0];
                String pubValue = (String)map.get(pubContentId);
                String paramName = Integer.toString(counter)  + "_" + pubContentId;
                String paramValue = (String)paramMap.get(paramName);
                //if (Debug.infoOn()) Debug.logInfo("in updateStaticValues, contentId:" + contentId + " pubContentId:" + pubContentId + " pubValue:" + pubValue + " paramName:" + paramName + " paramValue:" + paramValue, module);
                Map serviceIn = new HashMap();
                serviceIn.put("userLogin", userLogin);
                serviceIn.put("contentIdTo", contentId);
                serviceIn.put("contentId", pubContentId);
                serviceIn.put("contentAssocTypeId", "SUBSITE");
                try {
                    if (UtilValidate.isNotEmpty(paramValue)) {
                        if (!paramValue.equals(pubValue)) {
                            if (paramValue.equalsIgnoreCase("Y")) {
                                serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
                                Map results = dispatcher.runSync("createContentAssoc", serviceIn);
                            } else if (paramValue.equalsIgnoreCase("N") && pubValue.equalsIgnoreCase("Y")) {
                                serviceIn.put("thruDate", UtilDateTime.nowTimestamp());
                                Timestamp fromDate = (Timestamp)map.get(pubContentId + "FromDate");
                                serviceIn.put("fromDate", fromDate);
                                Map results = dispatcher.runSync("updateContentAssoc", serviceIn);
                            }
                        }
                    } else if ( UtilValidate.isNotEmpty(pubValue)) {
                        if (pubValue.equalsIgnoreCase("Y")) {
                                serviceIn.put("thruDate", UtilDateTime.nowTimestamp());
                                Timestamp fromDate = (Timestamp)map.get(pubContentId + "FromDate");
                                serviceIn.put("fromDate", fromDate);
                                Map results = dispatcher.runSync("updateContentAssoc", serviceIn);
                        }
                    }
                } catch(GenericServiceException e) {
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
        ServletContext servletContext = session.getServletContext();
        String webSiteId = (String) servletContext.getAttribute("webSiteId");
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Map paramMap = UtilHttp.getParameterMap(request);
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, paramMap:" + paramMap , module);
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
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, userLoginId:" + userLoginId + " authorId:" + authorId , module);
        List roleTypeList = null;
        if (authorId != null && userLoginId != null && authorId.equals(userLoginId)) {
            roles = "OWNER";
            roleTypeList = StringUtil.split(roles, "|");
        }
        List targetOperationList = UtilMisc.toList("CONTENT_PUBLISH");
        List contentPurposeList = null; //UtilMisc.toList("ARTICLE");
        //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, roles:" + roles +" roleTypeList:" + roleTypeList , module);
        String permittedAction = (String)paramMap.get("permittedAction"); // The content to be linked to one or more sites
        String permittedOperations = (String)paramMap.get("permittedOperations"); // The content to be linked to one or more sites
        if ( UtilValidate.isEmpty(targContentId)) {
            request.setAttribute("_ERROR_MESSAGE_", "targContentId is empty.");
            return "error";
        }

        // Get all the subSites that the user is permitted to link to
        List origPublishedLinkList = null;
        try {
            // TODO: this needs to be given author userLogin
            GenericValue authorUserLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", authorId));
            origPublishedLinkList = ContentManagementWorker.getPublishedLinks(delegator, targContentId, webSiteId, userLogin, security, permittedAction, permittedOperations, roles );
        } catch(GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch(GeneralException e2) {
            request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
            return "error";
        }
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, origPublishedLinkList:" + origPublishedLinkList , module);

        // make a map of the values that are passed in using the top subSite as the key.
        // Content can only be linked to one subsite under a top site (ends with "_MASTER")
        Set keySet = paramMap.keySet();
        Iterator itKeySet = keySet.iterator();
        Map siteIdLookup = new HashMap();
        while (itKeySet.hasNext()) {
            String param = (String)itKeySet.next();
            int pos = param.indexOf("select_");
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, param:" + param + " pos:" + pos , module);
            if (pos >= 0) {
                String siteId = param.substring(7);
                String subSiteVal = (String)paramMap.get(param);
                siteIdLookup.put(siteId, subSiteVal);
            }
        }

                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, siteIdLookup:" + siteIdLookup , module);

        // Loop thru all the possible subsites
        Iterator it = origPublishedLinkList.iterator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        int counter = 0;
        String responseMessage = null;
        String errorMessage = null;
        String permissionMessage = null;
        boolean statusIdUpdated = false;
        Map results = null;
        while (it.hasNext()) {
            Object [] arr = (Object [])it.next();
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, arr:" + Arrays.asList(arr) , module);
            String contentId = (String)arr[0]; // main (2nd level) site id
            String origSubContentId = null;
            List origSubList = (List)arr[1];
            Timestamp topFromDate = (Timestamp)arr[3];
            Timestamp origFromDate = null;
            Iterator itOrigSubPt = origSubList.iterator();
            // see if a link already exists by looking for non-null fromDate
            while (itOrigSubPt.hasNext()) {
                Object [] pubArr = (Object [])itOrigSubPt.next();
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, pubArr:" + Arrays.asList(pubArr) , module);
                Timestamp fromDate = (Timestamp)pubArr[2];
                origSubContentId = null;
                if (fromDate != null) {
                    origSubContentId = (String)pubArr[0];
                    origFromDate = fromDate;
                    break;
                }
            }

            String currentSubContentId = (String)siteIdLookup.get(contentId);
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, currentSubContentId:" + currentSubContentId , module);
                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, origSubContentId:" + origSubContentId , module);
            try {
                if (UtilValidate.isNotEmpty(currentSubContentId)) {
                    if (!currentSubContentId.equals(origSubContentId)) {
                        // disable existing link
                        if (UtilValidate.isNotEmpty(origSubContentId) && origFromDate != null) {
                            List oldActiveValues = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", targContentId, "contentIdTo", origSubContentId, "contentAssocTypeId", "PUBLISH_LINK", "thruDate", null));
                            Iterator iterOldActive = oldActiveValues.iterator();
                            while (iterOldActive.hasNext()) {
                                GenericValue cAssoc = (GenericValue)iterOldActive.next();
                                cAssoc.set("thruDate", nowTimestamp);
                                cAssoc.store();
                                //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, deactivating:" + cAssoc , module);
                            }
                        }
                        // create new link
                        Map serviceIn = new HashMap();
                        serviceIn.put("userLogin", userLogin);
                        serviceIn.put("contentId", targContentId);
                        serviceIn.put("contentAssocTypeId", "PUBLISH_LINK");
                        serviceIn.put("fromDate", nowTimestamp);
                        serviceIn.put("contentIdTo", currentSubContentId);
                        serviceIn.put("roleTypeList", roleTypeList);
                        serviceIn.put("targetOperationList", targetOperationList);
                        serviceIn.put("contentPurposeList", contentPurposeList);
                        results = dispatcher.runSync("createContentAssoc", serviceIn);
                        responseMessage = (String)results.get(ModelService.RESPONSE_MESSAGE); 
                        if (UtilValidate.isNotEmpty(responseMessage)) {
                            errorMessage = (String)results.get(ModelService.ERROR_MESSAGE);
                            Debug.logError("in updatePublishLinks, serviceIn:" + serviceIn , module);
                            Debug.logError(errorMessage, module);
                            request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                            return "error";
                        }

                        serviceIn = new HashMap();
                        serviceIn.put("userLogin", userLogin);
                        serviceIn.put("contentId", targContentId);
                        serviceIn.put("contentAssocTypeId", "PUBLISH_LINK");
                        serviceIn.put("fromDate", nowTimestamp);
                        serviceIn.put("contentIdTo", contentId);
                        serviceIn.put("roleTypeList", roleTypeList);
                        serviceIn.put("targetOperationList", targetOperationList);
                        serviceIn.put("contentPurposeList", contentPurposeList);
                        //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, serviceIn(3b):" + serviceIn , module);
                        results = dispatcher.runSync("createContentAssoc", serviceIn);
                        //if (Debug.infoOn()) Debug.logInfo("in updatePublishLinks, results(3b):" + results , module);
                        if (!statusIdUpdated) {
                            try {
                                GenericValue targContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", targContentId));
                                targContent.set("statusId", "CTNT_PUBLISHED");
                                targContent.store();
                                statusIdUpdated = true;
                            } catch(GenericEntityException e) {
                                Debug.logError(e.getMessage(), module);
                                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                                return "error";
                            }
                        }
                    }
                } else if ( UtilValidate.isNotEmpty(origSubContentId)) {
                    // if no current link is passed in, look to see if there is an existing link(s) that must be disabled
                    List oldActiveValues = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", targContentId, "contentIdTo", origSubContentId, "contentAssocTypeId", "PUBLISH_LINK", "thruDate", null));
                    Iterator iterOldActive = oldActiveValues.iterator();
                    while (iterOldActive.hasNext()) {
                        GenericValue cAssoc = (GenericValue)iterOldActive.next();
                        cAssoc.set("thruDate", nowTimestamp);
                        cAssoc.store();
                    }
                    oldActiveValues = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", targContentId, "contentIdTo", contentId, "contentAssocTypeId", "PUBLISH_LINK", "thruDate", null));
                    iterOldActive = oldActiveValues.iterator();
                    while (iterOldActive.hasNext()) {
                        GenericValue cAssoc = (GenericValue)iterOldActive.next();
                        cAssoc.set("thruDate", nowTimestamp);
                        cAssoc.store();
                    }
                }
            } catch(GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
            } catch(GenericServiceException e2) {
                    Debug.logError(e2, module);
                    request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
                    return "error";
            }
        }
        return "success";
    }

}
