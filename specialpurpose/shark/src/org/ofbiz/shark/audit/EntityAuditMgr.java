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
package org.ofbiz.shark.audit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.internal.eventaudit.AssignmentEventAuditPersistenceInterface;
import org.enhydra.shark.api.internal.eventaudit.CreateProcessEventAuditPersistenceInterface;
import org.enhydra.shark.api.internal.eventaudit.DataEventAuditPersistenceInterface;
import org.enhydra.shark.api.internal.eventaudit.EventAuditException;
import org.enhydra.shark.api.internal.eventaudit.EventAuditManagerInterface;
import org.enhydra.shark.api.internal.eventaudit.StateEventAuditPersistenceInterface;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
public class EntityAuditMgr implements EventAuditManagerInterface {

    public static final String module = EntityAuditMgr.class.getName();

    protected CallbackUtilities callBackUtil = null;

    public void configure(CallbackUtilities callBackUtil) throws RootException {
        this.callBackUtil = callBackUtil;
    }

    // create (new) methods
    public AssignmentEventAuditPersistenceInterface createAssignmentEventAudit() {
        return new AssignmentEventAudit(this, SharkContainer.getDelegator());
    }

    public CreateProcessEventAuditPersistenceInterface createCreateProcessEventAudit() {
        return new CreateProcessEventAudit(this, SharkContainer.getDelegator());
    }

    public DataEventAuditPersistenceInterface createDataEventAudit() {
        return new DataEventAudit(this, SharkContainer.getDelegator());
    }

    public StateEventAuditPersistenceInterface createStateEventAudit() {
        return new StateEventAudit(this, SharkContainer.getDelegator());
    }

    // persist (store) methods
    public void persist(AssignmentEventAuditPersistenceInterface assignmentEvent, SharkTransaction trans) throws EventAuditException {
        try {
            ((AssignmentEventAudit) assignmentEvent).store();
        } catch (GenericEntityException e) {
            throw new EventAuditException(e);
        }
    }

    public void persist(CreateProcessEventAuditPersistenceInterface processEvent, SharkTransaction trans) throws EventAuditException {
        try {
            ((CreateProcessEventAudit) processEvent).store();
        } catch (GenericEntityException e) {
            throw new EventAuditException(e);
        }
    }

    public void persist(DataEventAuditPersistenceInterface dataEvent, SharkTransaction trans) throws EventAuditException {
        try {
            ((DataEventAudit) dataEvent).store();
        } catch (GenericEntityException e) {
            throw new EventAuditException(e);
        }
    }

    public void persist(StateEventAuditPersistenceInterface stateEvent, SharkTransaction trans) throws EventAuditException {
        try {
            ((StateEventAudit) stateEvent).store();
        } catch (GenericEntityException e) {
            throw new EventAuditException(e);
        }
    }

    // restore methods
    public boolean restore(AssignmentEventAuditPersistenceInterface assignment, SharkTransaction trans) throws EventAuditException {
        if (assignment == null) {
            return false;
        }
        return true;
    }

    public boolean restore(CreateProcessEventAuditPersistenceInterface createProcess, SharkTransaction trans) throws EventAuditException {
        if (createProcess == null) {
            return false;
        }
        return true;
    }

    public boolean restore(DataEventAuditPersistenceInterface data, SharkTransaction trans) throws EventAuditException {
        if (data == null) {
            return false;
        }
        return true;
    }

    public boolean restore(StateEventAuditPersistenceInterface state, SharkTransaction trans) throws EventAuditException {
        if (state == null) {
            return false;
        }
        return true;
    }

    // delete methods
    public void delete(AssignmentEventAuditPersistenceInterface assignmentEventAuditPersistenceInterface, SharkTransaction trans) throws EventAuditException {
        // don't delete events
    }

    public void delete(CreateProcessEventAuditPersistenceInterface createProcessEventAuditPersistenceInterface, SharkTransaction trans) throws EventAuditException {
        // don't delete events
    }

    public void delete(DataEventAuditPersistenceInterface dataEventAuditPersistenceInterface, SharkTransaction trans) throws EventAuditException {
        // don't delete events
    }

    public void delete(StateEventAuditPersistenceInterface stateEventAuditPersistenceInterface, SharkTransaction trans) throws EventAuditException {
        // don't delete events
    }

    // history methods
    public List restoreProcessHistory(String processId, SharkTransaction trans) throws EventAuditException {
        List processHistory = new ArrayList();
        processHistory.addAll(getCreateProcessEvents(processId));
        processHistory.addAll(getProcessDataEvents(processId));
        processHistory.addAll(getProcessStateEvents(processId));
        if (Debug.verboseOn()) Debug.log(":: restoreProcessHistory :: " + processHistory.size(), module);
        return processHistory;
    }

    public List restoreActivityHistory(String processId, String activityId, SharkTransaction trans) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: restoreActivityHistory ::", module);
        List activityHistory = new ArrayList();
        activityHistory.addAll(getAssignmentEvents(processId, activityId));
        activityHistory.addAll(getActivityDataEvents(processId, activityId));
        activityHistory.addAll(getActivityStateEvents(processId, activityId));
        if (Debug.verboseOn()) Debug.log(":: restoreActivityHistory :: " + activityHistory.size(), module);
        return activityHistory;
    }

    // process history
    private List getCreateProcessEvents(String processId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getCreateProcessEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List createProcessEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "processCreated", org.ofbiz.shark.SharkConstants.processId, processId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    createProcessEvents.add(new CreateProcessEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return createProcessEvents;
    }

    private List getProcessStateEvents(String processId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getProcessStateEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List stateEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "processStateChanged", org.ofbiz.shark.SharkConstants.processId, processId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    stateEvents.add(new StateEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return stateEvents;
    }

    private List getProcessDataEvents(String processId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getProcessDataEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List dataEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "processContextChanged", org.ofbiz.shark.SharkConstants.processId, processId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    dataEvents.add(new DataEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return dataEvents;
    }

    // activity history
    private List getAssignmentEvents(String processId, String activityId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getAssignmentEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List assignmentEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "activityAssignmentChanged", org.ofbiz.shark.SharkConstants.processId, processId, org.ofbiz.shark.SharkConstants.activityId, activityId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    assignmentEvents.add(new AssignmentEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return assignmentEvents;
    }

    private List getActivityStateEvents(String processId, String activityId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getActivityStateEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List stateEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "activityStateChanged", org.ofbiz.shark.SharkConstants.processId, processId, org.ofbiz.shark.SharkConstants.activityId, activityId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    stateEvents.add(new StateEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return stateEvents;
    }

    private List getActivityDataEvents(String processId, String activityId) throws EventAuditException {
        if (Debug.verboseOn()) Debug.log(":: getActivityDataEvents ::", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List dataEvents = new ArrayList();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.auditType, "activityContextChanged", org.ofbiz.shark.SharkConstants.processId, processId, org.ofbiz.shark.SharkConstants.activityId, activityId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new EventAuditException(e);
        }
        if (lookupList != null && lookupList.size() > 0) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                if (v != null) {
                    dataEvents.add(new DataEventAudit(this, delegator, v.getString(org.ofbiz.shark.SharkConstants.eventAuditId)));
                }
            }
        }
        return dataEvents;
    }

    public synchronized String getNextId(String string) throws EventAuditException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        return delegator.getNextSeqId("SharkAuditSeq").toString();
    }
}
