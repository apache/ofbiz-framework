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
package org.ofbiz.workflow.client;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
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
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfFactory;
import org.ofbiz.workflow.WfProcess;

/**
 * Workflow Services - 'Services' and 'Workers' for interaction with Workflow API
 */
public class WorkflowServices {

    public static final String module = WorkflowServices.class.getName();

    // -------------------------------------------------------------------
    // Client 'Service' Methods
    // -------------------------------------------------------------------

    /** Cancel Workflow */
    public static Map<String, Object> cancelWorkflow(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");

        // if we passed in an activity id, lets get the process id instead
        try {
            GenericValue testObject = delegator.findOne("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId), false);
            if (testObject == null) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "Not a valid workflow runtime identifier");
                return result;
            } else if (testObject.get("workEffortTypeId") != null && testObject.getString("workEffortTypeId").equals("WORK_FLOW")) {
                // we are a valid process - do nothing
            } else if (testObject.get("workEffortTypeId") != null && testObject.getString("workEffortTypeId").equals("ACTIVITY")) {
                // we are a valid activitiy; get the process id
                workEffortId = testObject.getString("workEffortParentId");
            } else {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "Not a valid workflow runtime identifier");
                return result;
            }
        } catch (GenericEntityException e) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "Problems looking up runtime object; invalid id");
            return result;
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this workflow");
            return result;
        }
        try {
            WfProcess process = WfFactory.getWfProcess(delegator, workEffortId);
            process.abort();
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Suspend activity */
    public static Map<String, Object> suspendActivity(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this activity");
            return result;
        }
        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.suspend(workEffortId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Resume activity */
    public static Map<String, Object> resumeActivity(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this activity");
            return result;
        }
        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.resume(workEffortId);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Change the state of an activity */
    public static Map<String, Object> changeActivityState(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        String newState = (String) context.get("newState");

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this activity");
            return result;
        }
        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.setState(workEffortId, newState);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Check the state of an activity */
    public static Map<String, Object> checkActivityState(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");

        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            result.put("activityState", client.getState(workEffortId));
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Get the current activity context */
    public static Map<String, Object> getActivityContext(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");

        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            result.put("activityContext", client.getContext(workEffortId));
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Appends data to the activity context */
    public static Map<String, Object> appendActivityContext(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        Map<String, Object> appendContext = UtilGenerics.checkMap(context.get("currentContext"));

        if (UtilValidate.isEmpty(appendContext)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "The passed context is empty");
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this activity");
            return result;
        }
        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.appendContext(workEffortId, appendContext);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Assign activity to a new or additional party */
    public static Map<String, Object> assignActivity(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        String roleType = (String) context.get("roleTypeId");
        boolean removeOldAssign = false;

        if (context.containsKey("removeOldAssignments")) {
            removeOldAssign = ((String) context.get("removeOldAssignments")).equals("true") ? true : false;
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this activity");
            return result;
        }
        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.assign(workEffortId, partyId, roleType, null, removeOldAssign ? false : true);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Accept an assignment and attempt to start the activity */
    public static Map<String, Object> acceptAssignment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        String roleType = (String) context.get("roleTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.acceptAndStart(workEffortId, partyId, roleType, fromDate);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;

    }

    /** Delegate an assignment */
    public static Map<String, Object> delegateAssignment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");
        String fromParty = (String) context.get("fromPartyId");
        String fromRole = (String) context.get("fromRoleTypeId");
        Timestamp fromFromDate = (Timestamp) context.get("fromFromDate");
        String toParty = (String) context.get("toPartyId");
        String toRole = (String) context.get("toRoleTypeId");
        Timestamp toFromDate = (Timestamp) context.get("toFromDate");

        // optional fromDate (default now)
        if (toFromDate == null)
            toFromDate = UtilDateTime.nowTimestamp();

        try {
            WorkflowClient client = new WorkflowClient(ctx);
            client.delegate(workEffortId, fromParty, fromRole, fromFromDate, toParty, toRole, toFromDate);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
             we.printStackTrace();
             result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
             result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Delegate, accept an assignment */
    public static Map<String, Object> delegateAcceptAssignment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");
        String fromParty = (String) context.get("fromPartyId");
        String fromRole = (String) context.get("fromRoleTypeId");
        Timestamp fromFromDate = (Timestamp) context.get("fromFromDate");
        String toParty = (String) context.get("toPartyId");
        String toRole = (String) context.get("toRoleTypeId");
        Timestamp toFromDate = (Timestamp) context.get("toFromDate");
        Boolean startObj = (Boolean) context.get("startActivity");

        // optional start activity (default false)
        boolean start = false;
        if (startObj != null)
            start = startObj.booleanValue();

        // optional fromDate (default now)
        if (toFromDate == null)
            toFromDate = UtilDateTime.nowTimestamp();

        try {
            WorkflowClient client = new WorkflowClient(ctx);
            client.delegateAndAccept(workEffortId, fromParty, fromRole, fromFromDate, toParty, toRole, toFromDate, start);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
             we.printStackTrace();
             result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
             result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Accept a role assignment and attempt to start the activity */
    public static Map<String, Object> acceptRoleAssignment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        String roleType = (String) context.get("roleTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        try {
            WorkflowClient client = new WorkflowClient(ctx);
            client.delegateAndAccept(workEffortId, "_NA_", roleType, fromDate, partyId, roleType, fromDate, true);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    /** Complete an assignment */
    public static Map<String, Object> completeAssignment(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        String roleType = (String) context.get("roleTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Map<String, Object> actResults = UtilGenerics.checkMap(context.get("result"));

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        if (!hasPermission(security, workEffortId, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to access this assignment");
            return result;
        }

        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            client.complete(workEffortId, partyId, roleType, fromDate, actResults);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            we.printStackTrace();
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        }
        return result;
    }

    public static Map<String, Object> limitInvoker(DispatchContext ctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String limitService = (String) context.get("serviceName");
        Map<String, Object> limitContext = UtilGenerics.checkMap(context.get("serviceContext"));

        try {
            WorkflowClient client = WfFactory.getClient(ctx);
            String state = client.getState(workEffortId);

            if (state.startsWith("open")) {
                dispatcher.runSync(limitService, limitContext);
            }
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        } catch (WfException we) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, we.getMessage());
        } catch (GenericServiceException se) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, se.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------
    // Service 'Worker' Methods
    // -------------------------------------------------------------------

    /**
     * Checks if a user has permission to access workflow data.
     */
    public static boolean hasPermission(Security security, String workEffortId, GenericValue userLogin) {
        if (userLogin == null || workEffortId == null) {
            Debug.logWarning("No UserLogin object or no Workeffort ID was passed.", module);
            return false;
        }
        if (security.hasPermission("WORKFLOW_MAINT", userLogin)) {
            return true;
        } else {
            String partyId = userLogin.getString("partyId");
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"),
                        EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, workEffortId),
                        EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));

            List<GenericValue> workEffortList = null;

            try {
                workEffortList = userLogin.getDelegator().findList("WorkEffortAndPartyAssign", ecl, null, null, null, false);
                //Debug.logInfo("Found " + c.size() + " records.", module);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return false;
            }
            if (workEffortList.size() == 0) {
                ecl = EntityCondition.makeCondition(
                        EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"),
                        EntityCondition.makeCondition("workEffortParentId", EntityOperator.EQUALS, workEffortId),
                        EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
                try {
                    workEffortList = userLogin.getDelegator().findList("WorkEffortAndPartyAssign", ecl, null, null, null, false);
                    //Debug.logInfo("Found " + c.size() + " records.", module);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                    return false;
                }
            }

            if (workEffortList.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the owner of the workflow.
     */
    public static GenericValue getOwner(Delegator delegator, String workEffortId) {
        try {
            GenericValue we = delegator.findOne("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId), false);

            if (we != null && we.getString("workEffortParentId") == null) {
                List<GenericValue> workEffortList = delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "roleTypeId", "WF_OWNER"));

                if (UtilValidate.isEmpty(workEffortList)) {
                    return null;
                }
                return workEffortList.get(0);
            } else {
                return getOwner(delegator, we.getString("workEffortParentId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public static Map<String, Object> testWorkflowCondition(DispatchContext dctx, Map<String, ?> context) {
        Map<String, Object> result = FastMap.newInstance();
        result.put("evaluationResult", Boolean.TRUE);
        return result;
    }

}

