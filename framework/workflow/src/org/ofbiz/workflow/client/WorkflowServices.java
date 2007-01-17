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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
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
    public static Map cancelWorkflow(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        
        // if we passed in an activity id, lets get the process id instead
        try {
            GenericValue testObject = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
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
    public static Map suspendActivity(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map resumeActivity(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map changeActivityState(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map checkActivityState(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map getActivityContext(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map appendActivityContext(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        Map appendContext = (Map) context.get("currentContext");

        if (appendContext == null || appendContext.size() == 0) {
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
    public static Map assignActivity(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map acceptAssignment(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map delegateAssignment(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map delegateAcceptAssignment(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map acceptRoleAssignment(DispatchContext ctx, Map context) {
        Map result = new HashMap();
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
    public static Map completeAssignment(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Security security = ctx.getSecurity();
        String workEffortId = (String) context.get("workEffortId");
        String partyId = (String) context.get("partyId");
        String roleType = (String) context.get("roleTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Map actResults = (Map) context.get("result");            

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

    public static Map limitInvoker(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String workEffortId = (String) context.get("workEffortId");
        String limitService = (String) context.get("serviceName");
        Map limitContext = (Map) context.get("serviceContext");

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
            List expr = new ArrayList();

            expr.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
            expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
            expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
            expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
            expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
            expr.add(new EntityExpr("workEffortId", EntityOperator.EQUALS, workEffortId));
            expr.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));

            Collection c = null;

            try {
                c = userLogin.getDelegator().findByAnd("WorkEffortAndPartyAssign", expr);
                //Debug.logInfo("Found " + c.size() + " records.", module);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return false;
            }
            if (c.size() == 0) {
                expr = new ArrayList();
                expr.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
                expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DECLINED"));
                expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_DELEGATED"));
                expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_COMPLETED"));
                expr.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CAL_CANCELLED"));
                expr.add(new EntityExpr("workEffortParentId", EntityOperator.EQUALS, workEffortId));
                expr.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
                try {
                    c = userLogin.getDelegator().findByAnd("WorkEffortAndPartyAssign", expr);
                    //Debug.logInfo("Found " + c.size() + " records.", module);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                    return false;
                }
            }

            if (c.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the owner of the workflow.
     */
    public static GenericValue getOwner(GenericDelegator delegator, String workEffortId) {
        try {
            GenericValue we = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));

            if (we != null && we.getString("workEffortParentId") == null) {
                Collection c = delegator.findByAnd("WorkEffortPartyAssignment",
                        UtilMisc.toMap("workEffortId", workEffortId, "roleTypeId", "WF_OWNER"));

                return (GenericValue) c.iterator().next();
            } else {
                return getOwner(delegator, we.getString("workEffortParentId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

}

