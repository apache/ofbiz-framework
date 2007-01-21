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

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 */
public class ProcessMgr extends InstanceEntityObject implements ProcessMgrPersistenceInterface {

    public static final String module = ProcessMgr.class.getName();

    protected GenericValue processMgr = null;
    protected boolean newValue = false;

    protected ProcessMgr(EntityPersistentMgr mgr, GenericDelegator delegator, String name) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.processMgr = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfProcessMgr, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.mgrName, name));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected ProcessMgr(EntityPersistentMgr mgr, GenericValue processMgr) {
        super(mgr, processMgr.getDelegator());
        this.processMgr = processMgr;
    }

    public ProcessMgr(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.processMgr = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfProcessMgr, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.currentState, new Long(0)));
    }

    public static ProcessMgr getInstance(EntityPersistentMgr pmgr, GenericValue processMgr) {
        ProcessMgr mgr = new ProcessMgr(pmgr, processMgr);
        if (mgr.isLoaded()) {
            return mgr;
        }
        return null;
    }

    public static ProcessMgr getInstance(EntityPersistentMgr pmgr, String name) throws PersistenceException {
        ProcessMgr mgr = new ProcessMgr(pmgr, SharkContainer.getDelegator(), name);
        if (mgr.isLoaded()) {
            return mgr;
        }
        return null;
    }

    public boolean isLoaded() {
        if (processMgr == null) {
            return false;
        }
        return true;
    }

    public void setName(String name) {
        processMgr.set(org.ofbiz.shark.SharkConstants.mgrName, name);
    }

    public String getName() {
        return processMgr.getString(org.ofbiz.shark.SharkConstants.mgrName);
    }

    public void setPackageId(String pkgId) {
        processMgr.set(org.ofbiz.shark.SharkConstants.packageId, pkgId);
    }

    public String getPackageId() {
        return processMgr.getString(org.ofbiz.shark.SharkConstants.packageId);
    }

    public void setProcessDefinitionId(String pdId) {
        processMgr.set(org.ofbiz.shark.SharkConstants.definitionId, pdId);
    }

    public String getProcessDefinitionId() {
        return processMgr.getString(org.ofbiz.shark.SharkConstants.definitionId);
    }

    public void setState(int state) {
        processMgr.set(org.ofbiz.shark.SharkConstants.currentState, new Long(state));
    }

    public int getState() {
        return processMgr.getLong(org.ofbiz.shark.SharkConstants.currentState).intValue();
    }

    public String getVersion() {
        return processMgr.getString(org.ofbiz.shark.SharkConstants.packageVer);
    }

    public void setVersion(String version) {
        processMgr.set(org.ofbiz.shark.SharkConstants.packageVer, version);
    }
    public long getCreated()
    {
        return processMgr.getLong(org.ofbiz.shark.SharkConstants.created).longValue();
    }
    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(processMgr);
            newValue = false;
        } else {
            delegator.store(processMgr);
        }
    }
    public void reload() throws GenericEntityException {
        if (!newValue) {
            processMgr.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(processMgr);
            Debug.log("**** REMOVED : " + this, module);
        }
    }

    public void setCreated(long created)
    {
        processMgr.set(org.ofbiz.shark.SharkConstants.created, new Long(created));
    }
}
