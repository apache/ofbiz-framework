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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.enhydra.shark.api.internal.eventaudit.AssignmentEventAuditPersistenceInterface;

/**
 * Persistance Object
 * 
 */
public class AssignmentEventAudit extends EventAudit implements AssignmentEventAuditPersistenceInterface {

    public static final String module = AssignmentEventAudit.class.getName();
    protected GenericValue assignmentEventAudit = null;
    private boolean newValue = false;

    public AssignmentEventAudit(EntityAuditMgr mgr, GenericDelegator delegator, String eventAuditId) {
        super(mgr, delegator, eventAuditId);
        if (this.delegator != null) {
            try {
                this.assignmentEventAudit = delegator.findByPrimaryKey("WfAssignmentEventAudit", UtilMisc.toMap("eventAuditId", eventAuditId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    public AssignmentEventAudit(EntityAuditMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.assignmentEventAudit = delegator.makeValue("WfAssignmentEventAudit", UtilMisc.toMap("eventAuditId", this.eventAuditId));
    }

    public AssignmentEventAudit(EntityAuditMgr mgr, GenericValue assignmentEventAudit) {
        super(mgr, assignmentEventAudit.getDelegator(), assignmentEventAudit.getString("eventAuditId"));
        this.assignmentEventAudit = assignmentEventAudit;
    }

    public void setOldResourceUsername(String un) {
        assignmentEventAudit.set("oldUserName", un);
    }

    public String getOldResourceUsername() {
        return assignmentEventAudit.getString("oldUserName");
    }

    public void setOldResourceName(String nm) {
        assignmentEventAudit.set("oldName", nm);
    }

    public String getOldResourceName() {
        return assignmentEventAudit.getString("oldName");
    }

    public void setNewResourceUsername(String un) {
        assignmentEventAudit.set("newUserName", un);
    }

    public String getNewResourceUsername() {
        return assignmentEventAudit.getString("newUserName");
    }

    public void setNewResourceName(String nm) {
        assignmentEventAudit.set("newName", nm);
    }

    public String getNewResourceName() {
        return assignmentEventAudit.getString("newName");
    }

    public void setIsAccepted(boolean acc) {
        assignmentEventAudit.set("isAccepted", new Boolean(acc));
    }

    public boolean getIsAccepted() {
        return assignmentEventAudit.getBoolean("isAccepted").booleanValue();
    }

    public void store() throws GenericEntityException {
        super.store();
        if (newValue) {
            newValue = false;
            delegator.createOrStore(assignmentEventAudit);
        } else {
            delegator.store(assignmentEventAudit);
        }
    }

    public void reload() throws GenericEntityException {
        super.reload();
        if (!newValue) {
            assignmentEventAudit.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        super.remove();
        if (!newValue) {
            delegator.removeValue(assignmentEventAudit);
        }
    }
}
