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
                this.eventAudit = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.eventAuditId, eventAuditId));
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
        this.eventAuditId = delegator.getNextSeqId(org.ofbiz.shark.SharkConstants.WfEventAudit);
        this.eventAudit = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfEventAudit, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.eventAuditId, eventAuditId));
    }

    public EventAudit(EntityAuditMgr mgr, GenericValue eventAudit) {
        super(mgr, eventAudit.getDelegator());
        this.eventAuditId = eventAudit.getString(org.ofbiz.shark.SharkConstants.eventAuditId);
        this.eventAudit = eventAudit;
    }

    public String getEventAuditId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.eventAuditId);
    }

    public void setUTCTime(String ts) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.auditTime, ts);
    }

    public String getUTCTime() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.auditTime);
    }

    public void setType(String t) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.auditType, t);
    }

    public String getType() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.auditType);
    }

    public void setActivityId(String aId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.activityId, aId);
    }

    public String getActivityId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.activityId);
    }

    public void setActivityName(String an) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.activityName, an);
    }

    public String getActivityName() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.activityName);
    }

    public void setProcessId(String pId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.processId, pId);
    }

    public String getProcessId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setProcessName(String pn) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.processName, pn);
    }

    public String getProcessName() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.processName);
    }

    public void setProcessDefinitionName(String pdn) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.processDefName, pdn);
    }

    public String getProcessDefinitionName() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.processDefName);
    }

    public void setProcessDefinitionVersion(String pdv) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.processDefVer, pdv);
    }

    public String getProcessDefinitionVersion() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.processDefVer);
    }

    public void setActivityDefinitionId(String adId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.activityDefId, adId);
    }

    public String getActivityDefinitionId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.activityDefId);
    }

    public void setActivitySetDefinitionId(String adId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.activitySetDefId, adId);
    }

    public String getActivitySetDefinitionId()
    {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.activitySetDefId);
    }

    public void setProcessDefinitionId(String pdId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.processDefId, pdId);
    }

    public String getProcessDefinitionId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.processDefId);
    }

    public void setPackageId(String pkgId) {
        eventAudit.set(org.ofbiz.shark.SharkConstants.packageId, pkgId);
    }

    public String getPackageId() {
        return eventAudit.getString(org.ofbiz.shark.SharkConstants.packageId);
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
