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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.security.Security;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;


/** WorkEffortWorker - Work Effort worker class. */
public class WorkEffortWorker {

    public static final String module = WorkEffortWorker.class.getName();

    /** @deprecated */
    @Deprecated
    public static void getWorkEffort(PageContext pageContext, String workEffortIdAttrName, String workEffortAttrName, String partyAssignsAttrName,
        String canViewAttrName, String tryEntityAttrName, String currentStatusAttrName) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        Security security = (Security) pageContext.getRequest().getAttribute("security");
        GenericValue userLogin = (GenericValue) pageContext.getSession().getAttribute("userLogin");

        String workEffortId = pageContext.getRequest().getParameter("workEffortId");

        // if there was no parameter, check the request attribute, this may be a newly created entity
        if (workEffortId == null)
            workEffortId = (String) pageContext.getRequest().getAttribute("workEffortId");

        GenericValue workEffort = null;

        try {
            workEffort = delegator.findOne("WorkEffort", false, "workEffortId", workEffortId);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        Boolean canView = null;
        Collection<GenericValue> workEffortPartyAssignments = null;
        Boolean tryEntity = null;
        GenericValue currentStatus = null;

        if (workEffort == null) {
            tryEntity = Boolean.FALSE;
            canView = Boolean.TRUE;

            String statusId = pageContext.getRequest().getParameter("currentStatusId");

            if (UtilValidate.isNotEmpty(statusId)) {
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
            canView = (UtilValidate.isNotEmpty(workEffortPartyAssignments)) ? Boolean.TRUE : Boolean.FALSE;
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

    /** @deprecated */
    @Deprecated
    public static void getMonthWorkEffortEvents(PageContext pageContext, String attributeName) {}

    /** @deprecated */
    @Deprecated
    public static void getActivityContext(PageContext pageContext, String workEffortId) {
        getActivityContext(pageContext, workEffortId, "activityContext");
    }

    /** @deprecated */
    @Deprecated
    public static void getActivityContext(PageContext pageContext, String workEffortId, String attribute) {
        LocalDispatcher dispatcher = (LocalDispatcher) pageContext.getRequest().getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) pageContext.getSession().getAttribute("userLogin");
        Map<String, Object> svcCtx = UtilMisc.toMap("workEffortId", workEffortId, "userLogin", userLogin);
        Map<String, Object> result = null;

        try {
            result = dispatcher.runSync("wfGetActivityContext", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        if (result != null && result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))
            Debug.logError((String) result.get(ModelService.ERROR_MESSAGE), module);
        if (result != null && result.containsKey("activityContext")) {
            Object aC = UtilGenerics.checkMap(result.get("activityContext"));
            pageContext.setAttribute(attribute, aC);
        }
    }

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId) {
        return getLowestLevelWorkEfforts(delegator, workEffortId, workEffortAssocTypeId, "workEffortIdFrom", "workEffortIdTo");
    }

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId, String left, String right) {
        if (left == null) {
            left = "workEffortIdFrom";
        }
        if (right == null) {
            right = "workEffortIdTo";
        }

        List<GenericValue> workEfforts = FastList.newInstance();
        try {
            EntityConditionList<EntityExpr> exprsLevelFirst = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(left, workEffortId),
                    EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
            List<GenericValue> childWEAssocsLevelFirst = delegator.findList("WorkEffortAssoc", exprsLevelFirst, null, null, null, true);
            for (GenericValue childWEAssocLevelFirst : childWEAssocsLevelFirst) {
                EntityConditionList<EntityExpr> exprsLevelNext = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition(left, childWEAssocLevelFirst.get(right)),
                        EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
                List<GenericValue> childWEAssocsLevelNext = delegator.findList("WorkEffortAssoc", exprsLevelNext, null, null, null, true);
                while (UtilValidate.isNotEmpty(childWEAssocsLevelNext)) {
                    List<GenericValue> tempWorkEffortList = FastList.newInstance();
                    for (GenericValue childWEAssocLevelNext : childWEAssocsLevelNext) {
                        EntityConditionList<EntityExpr> exprsLevelNth = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition(left, childWEAssocLevelNext.get(right)),
                                EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
                        List<GenericValue> childWEAssocsLevelNth = delegator.findList("WorkEffortAssoc", exprsLevelNth, null, null, null, true);
                        if (UtilValidate.isNotEmpty(childWEAssocsLevelNth)) {
                            tempWorkEffortList.addAll(childWEAssocsLevelNth);
                        }
                        workEfforts.add(childWEAssocLevelNext);
                    }
                    childWEAssocsLevelNext = tempWorkEffortList;
                }
                workEfforts.add(childWEAssocLevelFirst);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return workEfforts;
    }

    public static List<GenericValue> removeDuplicateWorkEfforts(List<GenericValue> workEfforts) {
        Set<String> keys = FastSet.newInstance();
        Set<GenericValue> exclusions = FastSet.newInstance();
        for (GenericValue workEffort : workEfforts) {
            String workEffortId = workEffort.getString("workEffortId");
            if (keys.contains(workEffortId)) {
                exclusions.add(workEffort);
            } else {
                keys.add(workEffortId);
            }
        }
        workEfforts.removeAll(exclusions);
        return workEfforts;
    }
}
