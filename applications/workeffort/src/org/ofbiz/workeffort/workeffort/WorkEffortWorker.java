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

package org.ofbiz.workeffort.workeffort;

import java.util.Collection;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * WorkEffortWorker - Worker class to reduce code in JSPs & make it more reusable
 */
public class WorkEffortWorker {
    
    public static final String module = WorkEffortWorker.class.getName();
    
    // TODO: REMOVE this method when JSPs/etc are moved to FreeMarker; this is replaced by a corresponding service
    public static void getWorkEffort(PageContext pageContext, String workEffortIdAttrName, String workEffortAttrName, String partyAssignsAttrName,
        String canViewAttrName, String tryEntityAttrName, String currentStatusAttrName) {
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
        Security security = (Security) pageContext.getRequest().getAttribute("security");
        GenericValue userLogin = (GenericValue) pageContext.getSession().getAttribute("userLogin");

        String workEffortId = pageContext.getRequest().getParameter("workEffortId");

        // if there was no parameter, check the request attribute, this may be a newly created entity
        if (workEffortId == null)
            workEffortId = (String) pageContext.getRequest().getAttribute("workEffortId");

        GenericValue workEffort = null;

        try {
            workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        Boolean canView = null;
        Collection workEffortPartyAssignments = null;
        Boolean tryEntity = null;
        GenericValue currentStatus = null;

        if (workEffort == null) {
            tryEntity = Boolean.FALSE;
            canView = Boolean.TRUE;

            String statusId = pageContext.getRequest().getParameter("currentStatusId");

            if (statusId != null && statusId.length() > 0) {
                try {
                    currentStatus = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap("statusId", statusId));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        } else {
            // get a collection of workEffortPartyAssignments, if empty then this user CANNOT view the event, unless they have permission to view all
            if (userLogin != null && userLogin.get("partyId") != null && workEffortId != null) {
                try {
                    workEffortPartyAssignments =
                            delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", workEffortId, "partyId", userLogin.get("partyId")));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
            canView = (workEffortPartyAssignments != null && workEffortPartyAssignments.size() > 0) ? Boolean.TRUE : Boolean.FALSE;
            if (!canView.booleanValue() && security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", pageContext.getSession())) {
                canView = Boolean.TRUE;
            }

            tryEntity = Boolean.TRUE;

            if (workEffort.get("currentStatusId") != null) {
                try {
                    currentStatus = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap("statusId", workEffort.get("currentStatusId")));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            }
        }

        // if there was an error message, don't get values from entity
        if (pageContext.getRequest().getAttribute("_ERROR_MESSAGE_") != null) {
            tryEntity = Boolean.FALSE;
        }

        if (workEffortId != null)
            pageContext.setAttribute(workEffortIdAttrName, workEffortId);
        if (workEffort != null)
            pageContext.setAttribute(workEffortAttrName, workEffort);
        if (canView != null)
            pageContext.setAttribute(canViewAttrName, canView);
        if (workEffortPartyAssignments != null)
            pageContext.setAttribute(partyAssignsAttrName, workEffortPartyAssignments);
        if (tryEntity != null)
            pageContext.setAttribute(tryEntityAttrName, tryEntity);
        if (currentStatus != null)
            pageContext.setAttribute(currentStatusAttrName, currentStatus);
    }

    public static void getMonthWorkEffortEvents(PageContext pageContext, String attributeName) {}

    public static void getActivityContext(PageContext pageContext, String workEffortId) {
        getActivityContext(pageContext, workEffortId, "activityContext");
    }

    public static void getActivityContext(PageContext pageContext, String workEffortId, String attribute) {
        LocalDispatcher dispatcher = (LocalDispatcher) pageContext.getRequest().getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) pageContext.getSession().getAttribute("userLogin");
        Map svcCtx = UtilMisc.toMap("workEffortId", workEffortId, "userLogin", userLogin);
        Map result = null;

        try {
            result = dispatcher.runSync("wfGetActivityContext", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        if (result != null && result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
            Debug.logError((String) result.get(ModelService.ERROR_MESSAGE), module);
        if (result != null && result.containsKey("activityContext")) {
            Map aC = (Map) result.get("activityContext");

            pageContext.setAttribute(attribute, aC);
        }
    }
}
