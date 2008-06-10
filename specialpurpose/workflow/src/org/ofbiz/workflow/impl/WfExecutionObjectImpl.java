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
package org.ofbiz.workflow.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.workflow.AlreadySuspended;
import org.ofbiz.workflow.CannotResume;
import org.ofbiz.workflow.CannotStop;
import org.ofbiz.workflow.CannotSuspend;
import org.ofbiz.workflow.EvaluationException;
import org.ofbiz.workflow.HistoryNotAvailable;
import org.ofbiz.workflow.InvalidData;
import org.ofbiz.workflow.InvalidState;
import org.ofbiz.workflow.NotRunning;
import org.ofbiz.workflow.NotSuspended;
import org.ofbiz.workflow.TransitionCondition;
import org.ofbiz.workflow.TransitionNotAllowed;
import org.ofbiz.workflow.UpdateNotAllowed;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfExecutionObject;
import org.ofbiz.workflow.WfUtil;

/**
 * WfExecutionObjectImpl - Workflow Execution Object implementation
 */
public abstract class WfExecutionObjectImpl implements WfExecutionObject {

    public static final String module = WfExecutionObjectImpl.class.getName();
    public static final String dispatcherName = "WFDispatcher";    

    protected String packageId = null;
    protected String packageVersion = null;
    protected String processId = null;
    protected String processVersion = null;
    protected String activityId = null;
    protected String workEffortId = null;
    protected GenericDelegator delegator = null;
    protected List history = null;

    public WfExecutionObjectImpl(GenericValue valueObject, String parentId) throws WfException {
        this.packageId = valueObject.getString("packageId");
        this.packageVersion = valueObject.getString("packageVersion");
        this.processId = valueObject.getString("processId");
        this.processVersion = valueObject.getString("processVersion");
        if (valueObject.getEntityName().equals("WorkflowActivity")) {
            this.activityId = valueObject.getString("activityId");
        } else {
            this.activityId = null;
        }
        this.delegator = valueObject.getDelegator();
        createRuntime(parentId);
    }

    public WfExecutionObjectImpl(GenericDelegator delegator, String workEffortId) throws WfException {
        this.delegator = delegator;
        this.workEffortId = workEffortId;
        this.packageId = getRuntimeObject().getString("workflowPackageId");
        this.packageVersion = getRuntimeObject().getString("workflowPackageVersion");
        this.processId = getRuntimeObject().getString("workflowProcessId");
        this.processVersion = getRuntimeObject().getString("workflowProcessVersion");
        this.activityId = getRuntimeObject().getString("workflowActivityId");
        this.history = null;
        if (Debug.verboseOn()) Debug.logVerbose(" Package ID: " + packageId + " V: " + packageVersion, module);
        if (Debug.verboseOn()) Debug.logVerbose(" Process ID: " + processId + " V: " + processVersion, module);
        if (Debug.verboseOn()) Debug.logVerbose("Activity ID: " + activityId, module);
    }
    
    // creates the stored runtime workeffort data.
    private void createRuntime(String parentId) throws WfException {
        GenericValue valueObject = getDefinitionObject();
        GenericValue dataObject = null;

        workEffortId = getDelegator().getNextSeqId("WorkEffort");
        Map dataMap = new HashMap();
        String weType = activityId != null ? "ACTIVITY" : "WORK_FLOW";

        dataMap.put("workEffortId", workEffortId);
        dataMap.put("workEffortTypeId", weType);
        dataMap.put("workEffortParentId", parentId);
        dataMap.put("workflowPackageId", packageId);
        dataMap.put("workflowPackageVersion", packageVersion);
        dataMap.put("workflowProcessId", processId);
        dataMap.put("workflowProcessVersion", processVersion);
        dataMap.put("workEffortName", valueObject.getString("objectName"));
        dataMap.put("description", valueObject.getString("description"));
        dataMap.put("createdDate", new Timestamp((new Date()).getTime()));
        dataMap.put("estimatedStartDate", dataMap.get("createdDate"));
        dataMap.put("lastModifiedDate", dataMap.get("createdDate"));
        dataMap.put("priority", valueObject.getLong("objectPriority"));
        dataMap.put("currentStatusId", WfUtil.getOFBStatus("open.not_running.not_started"));
        if (activityId != null)
            dataMap.put("workflowActivityId", activityId);
        if (activityId != null && parentId != null) {
            GenericValue parentWorkEffort = getWorkEffort(parentId);
            if (parentWorkEffort != null && parentWorkEffort.get("sourceReferenceId") != null)
                dataMap.put("sourceReferenceId", parentWorkEffort.getString("sourceReferenceId"));
        }

        try {            
            dataObject = getDelegator().makeValue("WorkEffort", dataMap);            
            getDelegator().create(dataObject);
            
            String objectId = activityId != null ? activityId : processId;
            if (Debug.verboseOn()) Debug.logVerbose("Created new runtime object [" + objectId + "] (Workeffort: " + runtimeKey() + ")", module);
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }
    
    protected void parseDescriptions(Map parseContext) throws WfException {
        GenericValue runtime = getRuntimeObject();
        String name = runtime.getString("workEffortName");
        String desc = runtime.getString("description");
        String nameExp = FlexibleStringExpander.expandString(name, parseContext);
        String descExp = FlexibleStringExpander.expandString(desc, parseContext);
        
        boolean changed = false;
        if (nameExp != null && !nameExp.equals(name)) {
            changed = true;
            runtime.set("workEffortName", nameExp);
        }
        if (descExp != null && !descExp.equals(desc)) {
            changed = true;
            runtime.set("description", descExp);
        }
        
        if (changed) {
            try {
                runtime.store();
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#name()
     */   
    public String name() throws WfException {
        return getRuntimeObject().getString("workEffortName");
    }
   
    /**
     * @see org.ofbiz.workflow.WfExecutionObject#setName(java.lang.String)
     */
    public void setName(String newValue) throws WfException {
        GenericValue dataObject = getRuntimeObject();

        try {
            dataObject.set("workEffortName", newValue);
            dataObject.store();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }
   
    /**
     * @see org.ofbiz.workflow.WfExecutionObject#setPriority(long)
     */
    public void setPriority(long newValue) throws WfException {
        GenericValue dataObject = getRuntimeObject();

        try {
            dataObject.set("priority", new Long(newValue));
            dataObject.store();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#priority()
     */
    public long priority() throws WfException {
        if (getRuntimeObject().get("priority") != null)
            return getRuntimeObject().getLong("priority").longValue();
        return 0; // change to default priority value
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#state()
     */
    public String state() throws WfException {
        GenericValue statusObj = null;
        String stateStr = null;

        try {
            statusObj = getRuntimeObject().getRelatedOne("CurrentStatusItem");
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (statusObj != null)
            stateStr = statusObj.getString("statusCode");

        if (stateStr == null)
            throw new WfException("Stored state is not a valid type.");
            
        if (Debug.verboseOn()) Debug.logVerbose("Current state: " + stateStr, module);            
        return stateStr;
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#validStates()
     */
    public List validStates() throws WfException {
        String statesArr[] = {"open.running", "open.not_running.not_started", "open.not_running.suspended",
                "closed.completed", "closed.terminated", "closed.aborted"};
        ArrayList possibleStates = new ArrayList(Arrays.asList(statesArr));
        String currentState = state();

        if (currentState.startsWith("closed"))
            return new ArrayList();
        if (!currentState.startsWith("open"))
            throw new WfException("Currently in an unknown state.");
        if (currentState.equals("open.running")) {
            possibleStates.remove("open.running");
            possibleStates.remove("open.not_running.not_started");
            return possibleStates;
        }
        if (currentState.equals("open.not_running.not_started")) {
            possibleStates.remove("open.not_running.not_started");
            possibleStates.remove("open.not_running.suspended");
            possibleStates.remove("closed.completed");
            possibleStates.remove("closed.terminated");            
            return possibleStates;
        }
        if (currentState.equals("open.not_running.suspended")) {
            possibleStates.remove("open.not_running.suspended");
            possibleStates.remove("open.not_running.not_started");
            possibleStates.remove("closed.complete");
            possibleStates.remove("closed.terminated");            
            return possibleStates;
        }
        return new ArrayList();
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#howManyHistory()
     */
    public int howManyHistory() throws WfException, HistoryNotAvailable {
        if (history.size() < 1)
            throw new HistoryNotAvailable();
        return history.size();
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#abort()
     */
    public void abort() throws WfException, CannotStop, NotRunning {
        Debug.logInfo("Aborting current state : " + state(), module);
        String stateStr = "closed.aborted";
        
        if (!state().startsWith("open")) {
            throw new NotRunning();
        }
        
        if (!validStates().contains(stateStr)) {
            throw new CannotStop();
        }
                
        changeState(stateStr);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#whileOpenType()
     */
    public List whileOpenType() throws WfException {
        String[] list = {"running", "not_running"};

        return Arrays.asList(list);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#whyNotRunningType()
     */
    public List whyNotRunningType() throws WfException {
        String[] list = {"not_started", "suspended"};

        return Arrays.asList(list);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#runtimeKey()
     */
    public String runtimeKey() throws WfException {
        return getRuntimeObject().getString("workEffortId");
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#key()
     */
    public String key() throws WfException {
        if (activityId != null)
            return activityId;
        else
            return processId;
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#isMemberOfHistory(org.ofbiz.workflow.WfExecutionObject)
     */
    public boolean isMemberOfHistory(WfExecutionObject member) throws WfException {
        return false;
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#setProcessContext(java.util.Map)
     */
    public void setProcessContext(Map newValue) throws WfException, InvalidData, UpdateNotAllowed {            
        setSerializedData(newValue);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#setProcessContext(java.lang.String)
     */
    public void setProcessContext(String contextKey) throws WfException, InvalidData, UpdateNotAllowed {            
        GenericValue dataObject = getRuntimeObject();

        try {
            dataObject.set("runtimeDataId", contextKey);
            dataObject.store();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#contextKey()
     */
    public String contextKey() throws WfException {
        if (getRuntimeObject().get("runtimeDataId") == null)
            return null;
        else
            return getRuntimeObject().getString("runtimeDataId");
    }
 
    /**
     * @see org.ofbiz.workflow.WfExecutionObject#processContext()
     */
    public Map processContext() throws WfException {
        return getContext();
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#workflowStateType()
     */
    public List workflowStateType() throws WfException {
        String[] list = {"open", "closed"};
        return Arrays.asList(list);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#terminate()
     */
    public void terminate() throws WfException, CannotStop, NotRunning {
        String stateStr = "closed.terminated";

        if (!state().equals("open.running"))
            throw new NotRunning();
        if (!validStates().contains(stateStr))
            throw new CannotStop();
        changeState(stateStr);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#setDescription(java.lang.String)
     */
    public void setDescription(String newValue) throws WfException {
        GenericValue valueObject = getDefinitionObject();

        try {
            valueObject.set("description", newValue);
            valueObject.store();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#description()
     */
    public String description() throws WfException {
        return getDefinitionObject().getString("description");
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#lastStateTime()
     */
    public Timestamp lastStateTime() throws WfException {
        GenericValue dataObject = getRuntimeObject();

        if (dataObject == null || dataObject.get("lastStatusUpdate") == null)
            throw new WfException("No runtime object or status has never been set.");
        return dataObject.getTimestamp("lastStatusUpdate");
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#getSequenceHistory(int)
     */
    public List getSequenceHistory(int maxNumber) throws WfException,
            HistoryNotAvailable {
        return history;
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#getIteratorHistory(java.lang.String, java.util.Map)
     */
    public Iterator getIteratorHistory(String query,
        Map namesInQuery) throws WfException, HistoryNotAvailable {
        return history.iterator();
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#resume()
     */
    public void resume() throws WfException, CannotResume, NotRunning, NotSuspended {
        if (!state().equals("open.not_running.suspended")) {
            if (state().equals("open.not_running.not_started")) {
                throw new NotRunning();
            } else if (state().startsWith("closed")) {
                throw new CannotResume();
            } else {
                throw new NotSuspended();
            }
        } else {
            changeState("open.running");
        }                                       
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#howClosedType()
     */
    public List howClosedType() throws WfException {
        String[] list = {"completed", "terminated", "aborted"};

        return Arrays.asList(list);
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#changeState(java.lang.String)
     */
    public void changeState(String newState) throws WfException, InvalidState, TransitionNotAllowed {            
        // Test is transaction is allowed???
        GenericValue dataObject = getRuntimeObject();

        if (validStates().contains(newState)) {
            try {
                long now = (new Date()).getTime();

                dataObject.set("currentStatusId", WfUtil.getOFBStatus(newState));
                dataObject.set("lastStatusUpdate", new Timestamp(now));
                dataObject.store();
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }
        } else {
            throw new InvalidState();
        }
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#suspend()
     */
    public void suspend() throws WfException, CannotSuspend, NotRunning, AlreadySuspended {            
        changeState("open.not_running.suspended");
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#getDelegator()
     */
    public GenericDelegator getDelegator() throws WfException {
        return delegator;
    }

    /**
     * @see org.ofbiz.workflow.WfExecutionObject#getDefinitionObject()
     */
    public GenericValue getDefinitionObject() throws WfException {
        String entityName = activityId != null ? "WorkflowActivity" : "WorkflowProcess";
        GenericValue value = null;
        Map fields = UtilMisc.toMap("packageId", packageId, "packageVersion", packageVersion, "processId", processId,
                "processVersion", processVersion);

        if (activityId != null)
            fields.put("activityId", activityId);
        try {
            value = getDelegator().findByPrimaryKey(entityName, fields);
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        return value;
    }

    public GenericValue getRuntimeObject() throws WfException {
        GenericValue value = null;

        try {
            value = getDelegator().findByPrimaryKey("WorkEffort",
                        UtilMisc.toMap("workEffortId", workEffortId));
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        return value;
    }

    /**
     * Getter for this type of execution object.
     * @return String
     */
    public abstract String executionObjectType();

    /**
     * Updates the runtime data entity
     * @param field The field name of the entity (resultDataId,contextDataId)
     * @param value The value to serialize and set
     * @throws WfException
     */
    protected void setSerializedData(Map value) throws WfException, InvalidData {
        GenericValue runtimeData = null;
        GenericValue dataObject = getRuntimeObject();

        try {
            if (dataObject.get("runtimeDataId") == null) {
                String seqId = getDelegator().getNextSeqId("RuntimeData");

                runtimeData = getDelegator().makeValue("RuntimeData",
                            UtilMisc.toMap("runtimeDataId", seqId));
                getDelegator().create(runtimeData);
                dataObject.set("runtimeDataId", seqId);
                dataObject.store();
            } else {
                runtimeData = dataObject.getRelatedOne("RuntimeData");
            }
            // String serialized = XmlSerializer.serialize(value);
            // System.out.println(serialized);

            runtimeData.set("runtimeInfo", XmlSerializer.serialize(value));
            runtimeData.store();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        } catch (SerializeException e) {
            throw new InvalidData(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new InvalidData(e.getMessage(), e);
        } catch (IOException e) {
            throw new InvalidData(e.getMessage(), e);
        }
    }

    /**
     * Get an instance of the local dispatcher
     * @return LocalDispatcher instance for use with this workflow
     * @throws WfException
     */
    protected LocalDispatcher getDispatcher() throws WfException {
        return GenericDispatcher.getLocalDispatcher(dispatcherName, getDelegator());
    }

    private Map getContext() throws WfException {
        GenericValue dataObject = getRuntimeObject();
        String contextXML = null;
        Map context = null;

        if (dataObject.get("runtimeDataId") == null)
            return context;
        try {
            GenericValue runtimeData = dataObject.getRelatedOne("RuntimeData");

            contextXML = runtimeData.getString("runtimeInfo");
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        // De-serialize the context
        if (contextXML != null) {
            try {
                context = (Map) XmlSerializer.deserialize(contextXML, getDelegator());
            } catch (SerializeException e) {
                throw new WfException(e.getMessage(), e);
            } catch (IOException e) {
                throw new WfException(e.getMessage(), e);
            } catch (Exception e) {
                throw new WfException(e.getMessage(), e);
            }
        }
        return context;
    }
    
    private GenericValue getWorkEffort(String workEffortId) throws WfException {
        GenericValue we = null;
        try {
            we = getDelegator().findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
        } catch (GenericEntityException e) {
            throw new WfException("Problem getting WorkEffort entity (" + workEffortId + ")", e);
        }
        return we;
    }
            
    /**
     * Evaluate a condition expression using an implementation of TransitionCondition
     * @param className The class name of the TransitionCondition implementation
     * @param expression The expression to evaluate
     * @return The result of the evaluation (True/False)
     * @throws WfException
     */    
    protected boolean evalConditionClass(String className, String expression, Map context, Map attrs) throws WfException {
        // attempt to load and instance of the class
        Object conditionObject = null;
        try {
            conditionObject = ObjectType.getInstance(className);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Cannot load class " + className, module);
            return false;           
        } catch (InstantiationException e) {
            Debug.logError(e, "Cannot get instance of class " + className, module);
            return false;            
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Cannot access class " + className, module);
            return false;            
        }
                                
        // make sure we implement the TransitionCondition interface
        if (!ObjectType.instanceOf(conditionObject, "org.ofbiz.workflow.TransitionCondition")) {
            Debug.logError("Class " + className + " is not an instance of TransitionCondition", module);
            return false;
        }
        
        // cast to the interface
        TransitionCondition cond = (TransitionCondition) conditionObject;
        
        // trim up the expression if it isn't empty
        if (expression != null)
            expression = expression.trim();
        
        // get a DispatchContext object to pass over to the eval
        DispatchContext dctx = this.getDispatcher().getDispatchContext();
        
        // evaluate the condition
        Boolean evaluation = null;  
        try {               
            evaluation = cond.evaluateCondition(context, attrs, expression, dctx);
        } catch (EvaluationException e) {
            throw new WfException("Problems evaluating condition", e);
        }
        
        return evaluation.booleanValue();                            
    }      

    /**
     * Evaluate a condition expression using BeanShell
     * @param expression The expression to evaluate
     * @param context The context to use in evaluation
     * @return The result of the evaluation (True/False)
     * @throws WfException
     */
    protected boolean evalBshCondition(String expression, Map context) throws WfException {
        if (expression == null || expression.length() == 0) {
            Debug.logVerbose("Null or empty expression, returning true.", module);
            return true;
        }
        
        Object o = null;
        try {
            o = BshUtil.eval(expression.trim(), context);
        } catch (bsh.EvalError e) {
            throw new WfException("Bsh evaluation error.", e);
        }

        if (o == null)
            return false;
        else if (o instanceof Number)
            return (((Number) o).doubleValue() == 0) ? false : true;
        else
            return (!o.toString().equalsIgnoreCase("true")) ? false : true;
    }
}

