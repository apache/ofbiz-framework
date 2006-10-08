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

import org.enhydra.shark.api.internal.eventaudit.EventAuditPersistenceInterface;

/**
 * Persistance Object
 */
public class EventAudit extends AuditEntityObject implements EventAuditPersistenceInterface {

    public static final String module = EventAudit.class.getName();

    protected String eventAuditId = null;
    private GenericValue eventAudit = null;
    private boolean newValue = false;

    public EventAudit(EntityAuditMgr mgr, GenericDelegator delegator, String eventAuditId) {
        super(mgr, delegator);
        this.eventAuditId = eventAuditId;
        if (this.delegator != null) {
            try {
                this.eventAudit = delegator.findByPrimaryKey("WfEventAudit", UtilMisc.toMap("eventAuditId", eventAuditId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    public EventAudit(EntityAuditMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.eventAuditId = delegator.getNextSeqId("WfEventAudit");
        this.eventAudit = delegator.makeValue("WfEventAudit", UtilMisc.toMap("eventAuditId", eventAuditId));
    }

    public EventAudit(EntityAuditMgr mgr, GenericValue eventAudit) {
        super(mgr, eventAudit.getDelegator());
        this.eventAuditId = eventAudit.getString("eventAuditId");
        this.eventAudit = eventAudit;
    }

    public String getEventAuditId() {
        return eventAudit.getString("eventAuditId");    
    }

    public void setUTCTime(String ts) {
        eventAudit.set("auditTime", ts);
    }

    public String getUTCTime() {
        return eventAudit.getString("auditTime");
    }

    public void setType(String t) {
        eventAudit.set("auditType", t);
    }

    public String getType() {
        return eventAudit.getString("auditType");
    }

    public void setActivityId(String aId) {
        eventAudit.set("activityId", aId);
    }

    public String getActivityId() {
        return eventAudit.getString("activityId");
    }

    public void setActivityName(String an) {
        eventAudit.set("activityName", an);
    }

    public String getActivityName() {
        return eventAudit.getString("activityName");
    }

    public void setProcessId(String pId) {
        eventAudit.set("processId", pId);
    }

    public String getProcessId() {
        return eventAudit.getString("processId");
    }

    public void setProcessName(String pn) {
        eventAudit.set("processName", pn);
    }

    public String getProcessName() {
        return eventAudit.getString("processName");
    }

    public void setProcessDefinitionName(String pdn) {
        eventAudit.set("processDefName", pdn);
    }

    public String getProcessDefinitionName() {
        return eventAudit.getString("processDefName");
    }

    public void setProcessDefinitionVersion(String pdv) {
        eventAudit.set("processDefVer", pdv);
    }

    public String getProcessDefinitionVersion() {
        return eventAudit.getString("processDefVer");
    }

    public void setActivityDefinitionId(String adId) {
        eventAudit.set("activityDefId", adId);
    }

    public String getActivityDefinitionId() {
        return eventAudit.getString("activityDefId");
    }

    public void setActivitySetDefinitionId(String s) {
        // TODO: Implement Me!
    }

    public String getActivitySetDefinitionId() {
        return null;  // TODO: Implement Me!
    }

    public void setProcessDefinitionId(String pdId) {
        eventAudit.set("processDefId", pdId);
    }

    public String getProcessDefinitionId() {
        return eventAudit.getString("processDefId");
    }

    public void setPackageId(String pkgId) {
        eventAudit.set("packageId", pkgId);
    }

    public String getPackageId() {
        return eventAudit.getString("packageId");
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            newValue = false;
            delegator.createOrStore(eventAudit);
        } else {
            delegator.store(eventAudit);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            eventAudit.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(eventAudit);
        }
    }
}
