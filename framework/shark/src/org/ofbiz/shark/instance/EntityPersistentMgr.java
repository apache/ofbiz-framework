/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.instance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.internal.instancepersistence.*;
import org.enhydra.shark.api.internal.working.CallbackUtilities;

/**
 * Shark Persistance Manager Implementation
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class EntityPersistentMgr implements PersistentManagerInterface {

    public static final String module = EntityPersistentMgr.class.getName();

    protected CallbackUtilities callBackUtil = null;

    public void configure(CallbackUtilities callBackUtil) throws RootException {
        this.callBackUtil = callBackUtil;
    }

    public void shutdownDatabase() throws PersistenceException {
    }

    // store methods
    public void persist(ProcessMgrPersistenceInterface processMgr, SharkTransaction trans) throws PersistenceException {
        try {
            ((ProcessMgr) processMgr).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(ProcessPersistenceInterface process, SharkTransaction trans) throws PersistenceException {
        try {
            ((Process) process).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persistExternalRequester(String processId, Object req, SharkTransaction trans) throws PersistenceException {
        ProcessPersistenceInterface proc = this.restoreProcess(processId, trans);
        proc.setExternalRequester(req);
        this.persist(proc, trans);
    }

    public void persist(ActivityPersistenceInterface activity, SharkTransaction trans) throws PersistenceException {
        try {
            ((Activity) activity).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(ResourcePersistenceInterface resource, SharkTransaction trans) throws PersistenceException {
        try {
            ((Resource) resource).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(AssignmentPersistenceInterface assignment, SharkTransaction trans) throws PersistenceException {
        try {
            ((Assignment) assignment).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(AssignmentPersistenceInterface assignment, String oldResUname, SharkTransaction trans) throws PersistenceException {
        // TODO: look into this, if it is used only for re-assigning then have it expire/create new assignment
        persist(assignment, trans);
    }

    public void persist(ProcessVariablePersistenceInterface processVariable, SharkTransaction trans) throws PersistenceException {
        try {
            ((ProcessVariable) processVariable).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(ActivityVariablePersistenceInterface activityVariable, SharkTransaction trans) throws PersistenceException {
        try {
            ((ActivityVariable) activityVariable).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(AndJoinEntryInterface andJoin, SharkTransaction trans) throws PersistenceException {
        try {
            ((AndJoinEntry) andJoin).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void persist(DeadlinePersistenceInterface dpe, SharkTransaction ti) throws PersistenceException {
        try {
            ((Deadline) dpe).store();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    // restore methods
    public ProcessMgrPersistenceInterface restoreProcessMgr(String mgrName, SharkTransaction trans) throws PersistenceException {
        return ProcessMgr.getInstance(this, mgrName);
    }

    public ProcessPersistenceInterface restoreProcess(String processId, SharkTransaction trans) throws PersistenceException {
        return Process.getInstance(this, processId);
    }

    public ActivityPersistenceInterface restoreActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        return Activity.getInstance(this, activityId);
    }

    public ResourcePersistenceInterface restoreResource(String resourceId, SharkTransaction trans) throws PersistenceException {
        return Resource.getInstance(this, resourceId);
    }

    public AssignmentPersistenceInterface restoreAssignment(String activityId, String userName, SharkTransaction trans) throws PersistenceException {
        return Assignment.getInstance(this, activityId, userName);
    }

    public boolean restore(ProcessVariablePersistenceInterface processVariablePersistenceInterface, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: ProcessVariablePersistenceInterface ::", module);
        if (processVariablePersistenceInterface == null) {
            return false;
        }
        try {
            ((ProcessVariable) processVariablePersistenceInterface).reload();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new PersistenceException(e);
        }
        return true;
    }

    public boolean restore(ActivityVariablePersistenceInterface activityVariablePersistenceInterface, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: ActivityVariablePersistenceInterface ::", module);
        if (activityVariablePersistenceInterface == null) {
            return false;
        }
        try {
            ((ActivityVariable) activityVariablePersistenceInterface).reload();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new PersistenceException(e);
        }
        return true;
    }

    // remove/delete methods
    public void deleteProcessMgr(String mgrName, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: deleteProcessMgr ::", module);
        try {
            ((ProcessMgr) restoreProcessMgr(mgrName, trans)).remove();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void deleteProcess(String processId, boolean admin, SharkTransaction trans) throws PersistenceException {
        if (admin) {
            if (Debug.infoOn()) Debug.log(":: deleteProcess ::", module);
            Process process = (Process) this.restoreProcess(processId, trans);
            List activities = this.getAllActivitiesForProcess(processId, trans);
            if (activities != null) {
                Iterator i = activities.iterator();
                while (i.hasNext()) {
                    Activity activity = (Activity) i.next();
                    try {
                        activity.remove();
                    } catch (GenericEntityException e) {
                        throw new PersistenceException(e);
                    }
                }
            }

            try {
                process.remove();
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        }
    }

    public void deleteActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: deleteActivity ::", module);
        Activity activity = (Activity) this.restoreActivity(activityId, trans);
        List assignments = this.getAllAssignmentsForActivity(activityId, trans);
        if (assignments != null) {
            Iterator i = assignments.iterator();
            while (i.hasNext()) {
                Assignment assignment = (Assignment) i.next();
                try {
                    assignment.remove();
                } catch (GenericEntityException e) {
                    throw new PersistenceException(e);
                }
            }

            try {
                activity.remove();
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        }
    }

    public void deleteResource(String userName, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: deleteResource ::", module);
        try {
            ((Resource) restoreResource(userName, trans)).remove();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void deleteAssignment(String activityId, String userName, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: deleteAssignment ::", module);
        try {
            ((Assignment) restoreAssignment(activityId, userName, trans)).remove();
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void deleteAndJoinEntries(String procId, String asDefId, String aDefId, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: deleteAndJoinEntries ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            delegator.removeByAnd("WfAndJoin", UtilMisc.toMap("processId", procId,
                    "activitySetDefId", asDefId, "activityDefId", aDefId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void deleteDeadlines(String procId, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            delegator.removeByAnd("WfDeadline", UtilMisc.toMap("processId", procId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void deleteDeadlines(String procId, String actId, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            delegator.removeByAnd("WfDeadline", UtilMisc.toMap("processId", procId, "activityId", actId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
    }

    public void delete(ProcessVariablePersistenceInterface processVariable, SharkTransaction trans) throws PersistenceException {
        // don't delete variables
    }

    public void delete(ActivityVariablePersistenceInterface activityVariable, SharkTransaction trans) throws PersistenceException {
        // don't delete variables
    }

    public List getProcessMgrsWhere(SharkTransaction trans, String sqlWhere) throws PersistenceException {
        if (sqlWhere != null) {
            Debug.log("Attempt to call getProcessMgrsWhere() - " + sqlWhere, module);
            throw new PersistenceException("Method not available to this implementation! (" + sqlWhere + ")");
        }
        return this.getAllProcessMgrs(trans);
    }

    public List getAllProcessMgrs(SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllProcessMgrs ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findAll("WfProcessMgr");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(ProcessMgr.getInstance(this, v));
            }
        }
        if (Debug.verboseOn()) Debug.log("ProcessMgr : " + createdList.size(), module);
        return createdList;
    }

    public List getResourcesWhere(SharkTransaction trans, String sqlWhere) throws PersistenceException {
        if (sqlWhere != null) {
            Debug.log("Attempt to call getResourcesWhere() - " + sqlWhere, module);
            throw new PersistenceException("Method not available to this implementation! (" + sqlWhere + ")");
        }
        return this.getAllResources(trans);
    }

    public List getAllResources(SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllResources ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findAll("WfResource");
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Resource.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllAssignments(SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllAssignments ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findAll("WfAssignment");
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Assignment.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllProcesses(SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllProcesses ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findAll("WfProcess");
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Process.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllActivities(SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllActivities ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findAll("WfActivity");
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Activity.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllProcessesForMgr(String mgrName, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllProcessesForMgr ::", module);
        return this.getProcessesForMgr(mgrName, null, trans);
    }

    public List getProcessesForMgr(String mgrName, String state, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getProcessesForMgr ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;

        Map findBy = UtilMisc.toMap("mgrName", mgrName);
        if (state != null) {
            findBy.put("currentState", state);
        }

        try {
            lookupList = delegator.findByAnd("WfProcess", findBy);
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Process.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllRunningProcesses(SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List runningStates = UtilMisc.toList("open.running");
        List order = UtilMisc.toList("startedTime");
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByCondition("WfProcess",
                    makeStateListCondition("currentState", runningStates, EntityOperator.EQUALS, EntityOperator.OR), null, order);
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (!UtilValidate.isEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Process.getInstance(this, v));
            }
        }
        return createdList;
    }

    protected List findFinishedProcesses(SharkTransaction trans, String packageId, String processDefId, String packageVer, Date finishedBefore) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List finsihedStates = UtilMisc.toList("closed.completed", "closed.terminated", "closed.aborted");
        List order = UtilMisc.toList("lastStateTime");
        List createdList = new ArrayList();
        List lookupList = null;

        try {
            EntityCondition stateCond = this.makeStateListCondition("currentState", finsihedStates, EntityOperator.EQUALS, EntityOperator.OR);
            EntityCondition cond = this.makeProcessFilterCondition(stateCond, packageId, processDefId, packageVer, finishedBefore);
            lookupList = delegator.findByCondition("WfProcess", cond, null, order);
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (!UtilValidate.isEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Process.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllFinishedProcesses(SharkTransaction trans) throws PersistenceException {
        return this.findFinishedProcesses(trans, null, null, null, null);
    }

    public List getAllFinishedProcesses(SharkTransaction trans, Date finishedBefore) throws PersistenceException {
        return this.findFinishedProcesses(trans, null, null, null, finishedBefore);
    }

    public List getAllFinishedProcesses(SharkTransaction trans, String packageId) throws PersistenceException {
        return this.findFinishedProcesses(trans, packageId, null, null, null);
    }

    public List getAllFinishedProcesses(SharkTransaction trans, String packageId, String processDefId) throws PersistenceException {
        return this.findFinishedProcesses(trans, packageId, processDefId, null, null);
    }

    public List getAllFinishedProcesses(SharkTransaction trans, String packageId, String processDefId, String packageVer) throws PersistenceException {
        return this.findFinishedProcesses(trans, packageId, processDefId, packageVer, null);
    }

    private EntityCondition makeStateListCondition(String field, List states, EntityComparisonOperator operator, EntityJoinOperator jop) throws GenericEntityException {
        if (states != null) {
            if (states.size() > 1) {
                List exprs = new LinkedList();
                Iterator i = states.iterator();
                while (i.hasNext()) {
                    exprs.add(new EntityExpr(field, operator, i.next()));
                }
                return new EntityConditionList(exprs, jop);
            } else {
                return new EntityExpr(field, operator, states.get(0));
            }
        } else {
            throw new GenericEntityException("Cannot create entity condition from list :" + states);
        }
    }

    private EntityCondition makeProcessFilterCondition(EntityCondition cond, String packageId, String processDefId, String packageVer, Date finishBefore) {
        EntityCondition newCond = null;
        List exprs = new LinkedList();
        if (packageId != null) {
            exprs.add(new EntityExpr("packageId", EntityOperator.EQUALS, packageId));
        }
        if (processDefId != null) {
            exprs.add(new EntityExpr("definitionId", EntityOperator.EQUALS, processDefId));
        }
        if (packageVer != null) {
            exprs.add(new EntityExpr("packageVer", EntityOperator.EQUALS, packageVer));
        }
        if (finishBefore != null) {
            exprs.add(new EntityExpr("lastStateTime", EntityOperator.LESS_THAN, finishBefore));
        }

        if (exprs.size() > 0) {
            newCond = new EntityConditionList(exprs, EntityJoinOperator.AND);
        }

        if (newCond != null) {
            if (cond != null) {
                return new EntityConditionList(UtilMisc.toList(cond, newCond), EntityJoinOperator.AND);
            } else {
                return newCond;
            }
        } else {
            return cond;
        }
    }

    protected List findProcessActivities(String processId, List states, EntityComparisonOperator operator, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List order = UtilMisc.toList("lastStateTime");
        List lookupList = null;
        try {
            EntityCondition cond = null;
            EntityCondition proc = new EntityExpr("processId", EntityOperator.EQUALS, processId);

            if (states != null) {
                EntityCondition stateCond = this.makeStateListCondition("currentState", states, operator, EntityOperator.OR);
                cond = new EntityConditionList(UtilMisc.toList(proc, stateCond), EntityOperator.AND);
            } else {
                cond = proc;
            }
            lookupList = delegator.findByCondition("WfActivity", cond, null, order);
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Activity.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getAllActivitiesForProcess(String processId, SharkTransaction trans) throws PersistenceException {
        return this.findProcessActivities(processId, null, null, trans);
    }

    public List getActivitiesForProcess(String processId, String actState, SharkTransaction trans) throws PersistenceException {
        return this.findProcessActivities(processId, UtilMisc.toList(actState), null, trans);
    }

    public List getAllFinishedActivitiesForProcess(String processId, SharkTransaction trans) throws PersistenceException {
        List finishedStates = UtilMisc.toList("closed.completed", "closed.terminated", "closed.aborted");
        return this.findProcessActivities(processId, finishedStates, EntityOperator.EQUALS, trans);
    }

    public List getAllActiveActivitiesForProcess(String processId, SharkTransaction trans) throws PersistenceException {
        List finishedStates = UtilMisc.toList("closed.completed", "closed.terminated", "closed.aborted");
        return this.findProcessActivities(processId, finishedStates, EntityOperator.NOT_EQUAL, trans);
    }

    /**
    * Returns all assignments for the resource, no matter if its activity
    * is in "closed" state (or some of its sub-states).
    */
    public List getAllAssignmentsForResource(String user, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfAssignment", UtilMisc.toMap("userName", user));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Assignment.getInstance(this, v));
            }
        }
        return createdList;
    }

    /**
    * Returns all assignments which activity is not in "closed" state, or some
    * of its sub-states.
    */
    public List getAllAssignmentsForNotClosedActivitiesForResource(String user, SharkTransaction trans) throws PersistenceException {
        List allAssignments = getAllAssignmentsForResource(user, trans);
        List notClosed = new ArrayList();
        Iterator i = allAssignments.iterator();
        while (i.hasNext()) {
            Assignment as = (Assignment) i.next();
            Activity at = Activity.getInstance(this, as.getActivityId());
            if (!at.getState().startsWith("closed")) {
                notClosed.add(as);
            }
        }
        return notClosed;
    }

    /**
    * Returns only the assignments that can be currently executed by the resource
    * with a given username. This means the ones which activity is not finished
    * and not accepted (it doesn't have getResourceUsername() field set), and the
    * ones which activity is accepted by this resource (its getResourceUsername()
    * field is set to the resource with given username).
    */
    public List getAllValidAssignmentsForResource(String user, SharkTransaction trans) throws PersistenceException {
        List allAssignments = getAllAssignmentsForResource(user, trans);
        List valid = new ArrayList();
        Iterator i = allAssignments.iterator();
        while (i.hasNext()) {
            Assignment as = (Assignment) i.next();
            Activity at = Activity.getInstance(this, as.getActivityId());
            if (!at.getState().startsWith("closed")) {
                if (at.getResourceUsername() == null || user.equals(at.getResourceUsername())) {
                    valid.add(as);
                }
            }
        }
        return valid;
    }

    /**
    * Returns all assignments that are ever created for that activity, no
    * matter if activity is already in "closed" state or some of its sub-states.
    */
    public List getAllAssignmentsForActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfAssignment", UtilMisc.toMap("activityId", activityId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(Assignment.getInstance(this, v));
            }
        }
        return createdList;
    }

    /**
    * If activity is in "closed" state, or some of its sub-states, returns an
    * empty list, otherwise returns all assignments that are ever created for
    * that activity.
    */
    public List getAllAssignmentsForNotClosedActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        Activity at = Activity.getInstance(this, activityId);
        if (at.getState().startsWith("closed")) {
            return new ArrayList();
        } else {
            return getAllAssignmentsForActivity(activityId, trans);
        }
    }

    /**
    * If activity is in "closed" state, or some of its sub-states, returns an
    * empty list, otherwise it returns either all assignments that are ever
    * created for that activity if activity is not accepted, or just the
    * assignment for the resource that accepted activity.
    */
    public List getAllValidAssignmentsForActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        Activity at = Activity.getInstance(this, activityId);
        if (at.getState().startsWith("closed")) {
            return new ArrayList();
        }

        List assignments = getAllAssignmentsForActivity(activityId, trans);
        if (at.getResourceUsername() == null) {
            return assignments;
        }

        List valid = new ArrayList();
        Iterator i = assignments.iterator();
        while (i.hasNext()) {
            Assignment as = (Assignment) i.next();
            if (at.getResourceUsername().equals(as.getResourceUsername())) {
                valid.add(as);
            }
        }
        return valid;
    }

    public List getAllVariablesForProcess(String processId, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllVariablesForProcess ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfProcessVariable", UtilMisc.toMap("processId", processId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Debug.log("Lookup list contains : " + lookupList.size(), module);
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(ProcessVariable.getInstance(this, v));
            }
        } else {
            Debug.log("Lookup list empty", module);
        }

        if (Debug.verboseOn()) Debug.log("Returning list : " + createdList.size(), module);
        //Debug.log(new Exception(), "Stack Trace", module);
        return createdList;
    }

    public List getAllVariablesForActivity(String activityId, SharkTransaction trans) throws PersistenceException {
        if (Debug.verboseOn()) Debug.log(":: getAllVariablesForActivity ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createdList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfActivityVariable", UtilMisc.toMap("activityId", activityId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(ActivityVariable.getInstance(this, v));
            }
        }
        return createdList;
    }

    public List getResourceRequestersProcessIds(String userName, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List idList = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfProcess", UtilMisc.toMap("resourceReqId", userName));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (!UtilValidate.isEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                idList.add(v.getString("processId"));
            }
        }
        return idList;
    }

    public List getAndJoinEntries(String procId, String asDefId, String aDefId, SharkTransaction trans) throws PersistenceException {
        List createdList = new ArrayList();
        List lookupList = getAndJoinValues(procId, asDefId, aDefId);

        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                createdList.add(AndJoinEntry.getInstance(this, v));
            }
        }
        return createdList;
    }

    public int howManyAndJoinEntries(String procId, String asDefId, String aDefId, SharkTransaction trans) throws PersistenceException {
        List lookupList = getAndJoinValues(procId, asDefId, aDefId);
        return lookupList.size();
    }

    public List getAllDeadlinesForProcess(String procId, SharkTransaction trans) throws PersistenceException {
        List lookupList = getDeadlineValues(UtilMisc.toList(new EntityExpr("processId", EntityOperator.EQUALS, procId)));
        return getDealineObjects(lookupList);
    }

    public List getAllDeadlinesForProcess(String procId, long timeLimit, SharkTransaction trans) throws PersistenceException {
        List lookupList = getDeadlineValues(UtilMisc.toList(new EntityExpr("processId", EntityOperator.EQUALS, procId),
                new EntityExpr("timeLimit", EntityOperator.LESS_THAN, new Long(timeLimit))));
        return getDealineObjects(lookupList);
    }

    public List getAllIdsForProcessesWithExpiriedDeadlines(long l, SharkTransaction trans) throws PersistenceException {
        if (Debug.infoOn()) Debug.log(":: getAllIdsForProcessesWithExpiriedDeadlines ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List processIds = new ArrayList();

        DynamicViewEntity view = new DynamicViewEntity();
        view.addMemberEntity("WFDL", "WfDeadline");
        view.addMemberEntity("WFPR", "WfProcess");
        view.addMemberEntity("WFAC", "WfActivity");
        view.addAlias("WFPR", "currentState", "processState", null, null, null, null);
        view.addAlias("WFAC", "currentState", "activityState", null, null, null, null);
        view.addViewLink("WFDL", "WFPR", Boolean.FALSE, ModelKeyMap.makeKeyMapList("processId"));
        view.addViewLink("WFDL", "WFAC", Boolean.FALSE, ModelKeyMap.makeKeyMapList("activityId"));

        EntityListIterator eli = null;
        try {
            EntityCondition procState = new EntityExpr("processState", EntityOperator.EQUALS, "open.running");
            EntityCondition actState1 = new EntityExpr("activityState", EntityOperator.EQUALS, "open.not_running.not_started");
            EntityCondition actState2 = new EntityExpr("activityState", EntityOperator.EQUALS, "open.running");

            EntityCondition actState = new EntityConditionList(UtilMisc.toList(actState1, actState2), EntityOperator.OR);
            EntityCondition timeCond = new EntityExpr("timeLimit", EntityOperator.LESS_THAN, new Long(l));

            EntityCondition cond = new EntityConditionList(UtilMisc.toList(timeCond, procState, actState), EntityOperator.AND);
            eli = delegator.findListIteratorByCondition(view, cond, null, null, null, null);
            GenericValue v = null;
            while ((v = (GenericValue) eli.next()) != null) {
                processIds.add(v.getString("processId"));
            }
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        } finally {
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    throw new PersistenceException(e);
                }
            }
        }
        return processIds;
    }

    public List getAllDeadlinesForActivity(String procId, String actId, SharkTransaction trans) throws PersistenceException {
        List lookupList = getDeadlineValues(UtilMisc.toList(new EntityExpr("processId", EntityOperator.EQUALS, procId),
                new EntityExpr("activityId", EntityOperator.EQUALS, actId)));
        return getDealineObjects(lookupList);
    }

    public List getAllDeadlinesForActivity(String procId, String actId, long timeLimit, SharkTransaction trans) throws PersistenceException {
        List lookupList = getDeadlineValues(UtilMisc.toList(new EntityExpr("processId", EntityOperator.EQUALS, procId),
                new EntityExpr("activityId", EntityOperator.EQUALS, actId),
                new EntityExpr("timeLimit", EntityOperator.LESS_THAN, new Long(timeLimit))));
        return getDealineObjects(lookupList);
    }

    public int getExecuteCount(String procId, String asDefId, String aDefId, SharkTransaction trans) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        long count = 0;
        try {
            count = delegator.findCountByAnd("WfActivity", UtilMisc.toMap("processId", procId, "setDefinitionId", asDefId, "definitionId", aDefId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }

        return (int) count;
    }

    private List getAndJoinValues(String processId, String activitySetDefId, String activityDefId) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd("WfAndJoin", UtilMisc.toMap("processId", processId,
                    "activitySetDefId", activitySetDefId, "activityDefId", activityDefId));
        } catch (GenericEntityException e) {
            throw new PersistenceException(e);
        }
        if (lookupList == null) {
            lookupList = new ArrayList();
        }
        return lookupList;
    }

    private List getDeadlineValues(List exprList) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        if (exprList == null) {
            lookupList = new ArrayList();
        } else {
            try {
                lookupList = delegator.findByAnd("WfDeadline", exprList);
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
            if (lookupList == null) {
                lookupList = new ArrayList();
            }
        }
        return lookupList;
    }

    private List getDealineObjects(List deadlineValues) {
        List deadlines = new ArrayList();
        if (deadlineValues != null) {
            Iterator i = deadlineValues.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                deadlines.add(Deadline.getInstance(this, v));
            }
        }
        return deadlines;
    }

    // create methods
    public ActivityPersistenceInterface createActivity() {
        return new Activity(this, SharkContainer.getDelegator());
    }

    public ProcessPersistenceInterface createProcess() {
        return new Process(this, SharkContainer.getDelegator());
    }

    public ProcessMgrPersistenceInterface createProcessMgr() {
        return new ProcessMgr(this, SharkContainer.getDelegator());
    }

    public AssignmentPersistenceInterface createAssignment() {
        return new Assignment(this, SharkContainer.getDelegator());
    }

    public ResourcePersistenceInterface createResource() {
        return new Resource(this, SharkContainer.getDelegator());
    }

    public ProcessVariablePersistenceInterface createProcessVariable() {
        return new ProcessVariable(this, SharkContainer.getDelegator());
    }

    public ActivityVariablePersistenceInterface createActivityVariable() {
        return new ActivityVariable(this, SharkContainer.getDelegator());
    }

    public AndJoinEntryInterface createAndJoinEntry() {
        return new AndJoinEntry(this, SharkContainer.getDelegator());
    }

    public DeadlinePersistenceInterface createDeadline() {
        return new Deadline(this, SharkContainer.getDelegator());
    }

    public synchronized String getNextId(String string) throws PersistenceException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        return delegator.getNextSeqId("SharkWorkflowSeq").toString();
    }

    public List getAssignmentsWhere(SharkTransaction trans, String sqlWhere) throws PersistenceException {
        if (sqlWhere != null) {
            Debug.log("Attempt to call getAssignmentsWhere() - " + sqlWhere, module);
            throw new PersistenceException("Method not available to this implementation! (" + sqlWhere + ")");
        }
        return this.getAllAssignments(trans);
    }

    public List getProcessesWhere(SharkTransaction trans, String sqlWhere) throws PersistenceException {
        if (sqlWhere != null) {
            Debug.log("Attempt to call getProcessesWhere() - " + sqlWhere, module);
            throw new PersistenceException("Method not available to this implementation! (" + sqlWhere + ")");
        }
        return this.getAllProcesses(trans);
    }

    public List getActivitiesWhere(SharkTransaction trans, String sqlWhere) throws PersistenceException {
        if (sqlWhere != null) {
            Debug.log("Attempt to call getActivitiesWhere() - " + sqlWhere, module);
            throw new PersistenceException("Method not available to this implementation! (" + sqlWhere + ")");
        }
        return this.getAllActivities(trans);
    }
}
