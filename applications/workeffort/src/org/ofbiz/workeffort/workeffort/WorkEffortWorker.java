/*
 * $Id: WorkEffortWorker.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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
            tryEntity = new Boolean(false);
            canView = new Boolean(true);

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
            canView = (workEffortPartyAssignments != null && workEffortPartyAssignments.size() > 0) ? new Boolean(true) : new Boolean(false);
            if (!canView.booleanValue() && security.hasEntityPermission("WORKEFFORTMGR", "_VIEW", pageContext.getSession())) {
                canView = new Boolean(true);
            }

            tryEntity = new Boolean(true);

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
            tryEntity = new Boolean(false);
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
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
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
