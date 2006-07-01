/*
 * $Id: ProcessMgr.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
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

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class ProcessMgr extends InstanceEntityObject implements ProcessMgrPersistenceInterface {

    public static final String module = ProcessMgr.class.getName();
    
    protected GenericValue processMgr = null;
    protected boolean newValue = false;

    protected ProcessMgr(EntityPersistentMgr mgr, GenericDelegator delegator, String name) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.processMgr = delegator.findByPrimaryKey("WfProcessMgr", UtilMisc.toMap("mgrName", name));
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
        this.processMgr = delegator.makeValue("WfProcessMgr", UtilMisc.toMap("currentState", new Long(0)));
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
        processMgr.set("mgrName", name);
    }

    public String getName() {
        return processMgr.getString("mgrName");
    }

    public void setPackageId(String pkgId) {
        processMgr.set("packageId", pkgId);
    }

    public String getPackageId() {
        return processMgr.getString("packageId");
    }

    public void setProcessDefinitionId(String pdId) {
        processMgr.set("definitionId", pdId);
    }

    public String getProcessDefinitionId() {
        return processMgr.getString("definitionId");
    }

    public void setState(int state) {
        processMgr.set("currentState", new Long(state));
    }

    public int getState() {
        return processMgr.getLong("currentState").intValue();
    }

    public String getVersion() {
        return processMgr.getString("packageVer");
    }

    public void setVersion(String version) {
        processMgr.set("packageVer", version);
    }

    public String getCreated() {
        return processMgr.getString("created");
    }

    public void setCreated(String created) {
        processMgr.set("created", created);
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
}
