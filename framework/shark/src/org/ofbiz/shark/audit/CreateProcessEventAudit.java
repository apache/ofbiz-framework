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
package org.ofbiz.shark.audit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.enhydra.shark.api.internal.eventaudit.CreateProcessEventAuditPersistenceInterface;

/**
 * Persistance Object
 */
public class CreateProcessEventAudit extends EventAudit implements CreateProcessEventAuditPersistenceInterface {

    public static final String module = AssignmentEventAudit.class.getName();
    protected GenericValue createProcessEventAudit = null;
    private boolean newValue = false;

    public CreateProcessEventAudit(EntityAuditMgr mgr, GenericDelegator delegator, String eventAuditId) {
        super(mgr, delegator, eventAuditId);
        if (this.delegator != null) {
            try {
                this.createProcessEventAudit = delegator.findByPrimaryKey("WfCreateProcessEventAudit", UtilMisc.toMap("eventAuditId", eventAuditId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    public CreateProcessEventAudit(EntityAuditMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.createProcessEventAudit = delegator.makeValue("WfCreateProcessEventAudit", UtilMisc.toMap("eventAuditId", this.eventAuditId));
    }

    public CreateProcessEventAudit(EntityAuditMgr mgr, GenericValue createProcessEventAudit) {
        super(mgr, createProcessEventAudit.getDelegator(), createProcessEventAudit.getString("eventAuditId"));
        this.createProcessEventAudit = createProcessEventAudit;
    }

    public void setPActivityId(String paId) {
        createProcessEventAudit.set("pActivityId", paId);
    }

    public String getPActivityId() {
        return createProcessEventAudit.getString("pActivityId");
    }

    public void setPProcessId(String ppId) {
        createProcessEventAudit.set("pProcessId", ppId);
    }

    public String getPProcessId() {
        return createProcessEventAudit.getString("pProcessId");
    }

    public void setPProcessName(String ppn) {
        createProcessEventAudit.set("pProcessName", ppn);
    }

    public String getPProcessName() {
        return createProcessEventAudit.getString("pProcessName");
    }

    public void setPProcessDefinitionName(String ppdn) {
        createProcessEventAudit.set("pProcessDefName", ppdn);
    }

    public String getPProcessDefinitionName() {
        return createProcessEventAudit.getString("pProcessDefName");
    }

    public void setPProcessDefinitionVersion(String ppdv) {
        createProcessEventAudit.set("pProcessDefVer", ppdv);
    }

    public String getPProcessDefinitionVersion() {
        return createProcessEventAudit.getString("pProcessDefVer");
    }

    public void setPActivityDefinitionId(String padId) {
        createProcessEventAudit.set("pActivityDefId", padId);
    }

    public String getPActivityDefinitionId() {
        return createProcessEventAudit.getString("pActivityDefId");
    }

    public void setPActivitySetDefinitionId(String s) {
        // TODO: Implement Me!
    }

    public String getPActivitySetDefinitionId() {
        return null;  // TODO: Implement Me!
    }

    public void setPProcessDefinitionId(String ppdId) {
        createProcessEventAudit.set("pProcessDefId", ppdId);
    }

    public String getPProcessDefinitionId() {
        return createProcessEventAudit.getString("pProcessDefId");
    }

    public void setPPackageId(String ppkgId) {
        createProcessEventAudit.set("pPackageId", ppkgId);
    }

    public String getPPackageId() {
        return createProcessEventAudit.getString("pPackageId");
    }

    public void store() throws GenericEntityException {
        super.store();
        if (newValue) {
            newValue = false;
            delegator.createOrStore(createProcessEventAudit);
        } else {
            delegator.store(createProcessEventAudit);
        }
    }

    public void reload() throws GenericEntityException {
        super.reload();
        if (!newValue) {
            createProcessEventAudit.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        super.remove();
        if (!newValue) {
            delegator.removeValue(createProcessEventAudit);
        }
    }
}
