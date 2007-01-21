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
package org.ofbiz.shark.instance;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 */

public class Assignment extends InstanceEntityObject implements AssignmentPersistenceInterface {

    public static final String module = Assignment.class.getName();

    protected GenericValue assignment = null;
    protected boolean newValue = false;

    protected Assignment(EntityPersistentMgr mgr, GenericDelegator delegator, String activityId, String userName) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.assignment = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfAssignment, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.activityId, activityId, org.ofbiz.shark.SharkConstants.userName, userName));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected Assignment(EntityPersistentMgr mgr, GenericValue assignment) {
        super(mgr, assignment.getDelegator());
        this.assignment = assignment;
    }

    public Assignment(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.assignment = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfAssignment, null);
        Debug.log("******* New assignment created", module);
    }

    public static Assignment getInstance(EntityPersistentMgr mgr, GenericValue assignment) {
        Assignment assign = new Assignment(mgr, assignment);
        if (assign.isLoaded()) {
            return assign;
        }
        return null;
    }

    public static Assignment getInstance(EntityPersistentMgr mgr, String activityId, String userName) throws PersistenceException {
        Assignment assign = new Assignment(mgr, SharkContainer.getDelegator(), activityId, userName);
        if (assign.isLoaded()) {
            return assign;
        }
        return null;
    }

    public boolean isLoaded() {
        if (assignment == null) {
            return false;
        }
        return true;
    }

    public void setActivityId(String actId) {
        assignment.set(org.ofbiz.shark.SharkConstants.activityId, actId);
        Debug.log("Set activityId on assignment : " + actId, module);
        // set the processId - kludge
        /* should not be needed anymore
        try {
            Activity activity = Activity.getInstance(actId);
            if (activity != null) {
                this.setProcessId(activity.getProcessId());
            }
        } catch (PersistenceException e) {
            Debug.logError(e, module);
        }
        */
    }

    public String getActivityId() {
        return assignment.getString(org.ofbiz.shark.SharkConstants.activityId);
    }

    public void setResourceUsername(String username) {
        assignment.set(org.ofbiz.shark.SharkConstants.userName, username);
        Debug.log("Set userName on assignment : " + username, module);
    }

    public String getResourceUsername() {
        return assignment.getString(org.ofbiz.shark.SharkConstants.userName);
    }

    public void setProcessId(String procId) {
        assignment.set(org.ofbiz.shark.SharkConstants.processId, procId);
        Debug.log("Set processId on assignment : " + procId, module);
    }

    public String getProcessId() {
        return assignment.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setValid(boolean valid) {
        assignment.set(org.ofbiz.shark.SharkConstants.isValid, valid ? "Y" : "N");
    }

    public boolean isValid() {
        return (assignment.get(org.ofbiz.shark.SharkConstants.isValid) == null ? false : "Y".equalsIgnoreCase(assignment.getString(org.ofbiz.shark.SharkConstants.isValid)));
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(assignment);
            newValue = false;
        } else {
            delegator.store(assignment);
        }
        Debug.log("Stored assignment : " + assignment, module);
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            assignment.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(assignment);
            Debug.log("**** REMOVED : " + this, module);
        }
    }

    public void setProcessMgrName(String s) 
    {
        assignment.set(org.ofbiz.shark.SharkConstants.mgrName, s);
    }

    public String getProcessMgrName() {
        return assignment.getString(org.ofbiz.shark.SharkConstants.mgrName);
    }

    public void setAccepted(boolean valid) 
    {
        assignment.set(org.ofbiz.shark.SharkConstants.accepted, valid ? "Y" : "N");

    }

    public boolean isAccepted() {
        return (assignment.get(org.ofbiz.shark.SharkConstants.accepted) == null ? false : "Y".equalsIgnoreCase(assignment.getString(org.ofbiz.shark.SharkConstants.accepted)));
    }
}
