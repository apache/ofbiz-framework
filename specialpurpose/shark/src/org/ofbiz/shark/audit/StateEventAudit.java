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

import org.enhydra.shark.api.internal.eventaudit.StateEventAuditPersistenceInterface;

/**
 * Persistance Object
 */
public class StateEventAudit extends EventAudit implements StateEventAuditPersistenceInterface {

    public static final String module = AssignmentEventAudit.class.getName();
    protected GenericValue stateEventAudit = null;
    private boolean newValue = false;

    public StateEventAudit(EntityAuditMgr mgr, GenericDelegator delegator, String eventAuditId) {
        super(mgr, delegator, eventAuditId);
        if (this.delegator != null) {
            try {
                this.stateEventAudit = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfStateEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.eventAuditId, eventAuditId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    public StateEventAudit(EntityAuditMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.stateEventAudit = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfStateEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.eventAuditId, this.eventAuditId));
    }

    public StateEventAudit(EntityAuditMgr mgr, GenericValue stateEventAudit) {
        super(mgr, stateEventAudit.getDelegator(), stateEventAudit.getString(org.ofbiz.shark.SharkConstants.eventAuditId));
        this.stateEventAudit = stateEventAudit;
    }

    public void setOldState(String os) {
        stateEventAudit.set(org.ofbiz.shark.SharkConstants.oldState, os);
    }

    public String getOldState() {
        return stateEventAudit.getString(org.ofbiz.shark.SharkConstants.oldState);
    }

    public void setNewState(String ns) {
        stateEventAudit.set(org.ofbiz.shark.SharkConstants.newState, ns);
    }

    public String getNewState() {
        return stateEventAudit.getString(org.ofbiz.shark.SharkConstants.newState);
    }

    public void store() throws GenericEntityException {
        super.store();
        if (newValue) {
            newValue = false;
            delegator.createOrStore(stateEventAudit);
        } else {
            delegator.store(stateEventAudit);
        }
    }

    public void reload() throws GenericEntityException {
        super.reload();
        if (!newValue) {
            stateEventAudit.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        super.remove();
        if (!newValue) {
            delegator.removeValue(stateEventAudit);
        }
    }
}
