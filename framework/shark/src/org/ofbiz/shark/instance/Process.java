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
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 */
public class Process extends InstanceEntityObject implements ProcessPersistenceInterface {

    public static final String module = Process.class.getName();

    protected GenericValue process = null;
    protected boolean newValue = false;

    protected Process(EntityPersistentMgr mgr, GenericDelegator delegator, String processId) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.process = delegator.findByPrimaryKey("WfProcess", UtilMisc.toMap("processId", processId));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected Process(EntityPersistentMgr mgr, GenericValue process) {
        super(mgr, process.getDelegator());
        this.process = process;
    }

    public Process(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.process = delegator.makeValue("WfProcess", null);
    }

    public static Process getInstance(EntityPersistentMgr mgr, GenericValue process) {
        Process proc = new Process(mgr, process);
        if (proc.isLoaded()) {
            return proc;
        }
        return null;
    }

    public static Process getInstance(EntityPersistentMgr mgr, String processId) throws PersistenceException {
        Process proc = new Process(mgr, SharkContainer.getDelegator(), processId);
        if (proc.isLoaded()) {
            Debug.log("Returning loaded Process", module);
            return proc;
        }
        Debug.log("Returning null Process ID : " + processId, module);
        if (processId == null) Debug.log(new Exception(), module);
        return null;
    }

    public boolean isLoaded() {
        if (process == null) {
            return false;
        }
        return true;
    }

    public void setId(String s) {
        process.set("processId", s);
    }

    public String getId() {
        return process.getString("processId");
    }

    public void setProcessMgrName(String s) {
        process.set("mgrName", s);
        try {
            ProcessMgrPersistenceInterface pm = mgr.restoreProcessMgr(s, null);
            process.set("packageId", pm.getPackageId());
            process.set("packageVer", pm.getVersion());
        } catch (PersistenceException e) {
            Debug.logError(e, "Unable to set package information", module);
        }
    }

    public String getProcessMgrName() {
        return process.getString("mgrName");
    }

    public void setExternalRequester(Object o) {
        byte[] value = UtilObject.getBytes(o);
        process.setBytes("externalReq", (value != null ? value : null));
    }

    public Object getExternalRequester() {
        byte[] value = process.getBytes("externalReq");
        return UtilObject.getObject(value);
    }

    public void setActivityRequesterId(String s) {
        process.set("activityReqId", s);
    }

    public String getActivityRequesterId() {
        return process.getString("activityReqId");
    }

    public void setActivityRequestersProcessId(String s) {
        process.set("activityReqProcessId", s);
    }

    public String getActivityRequestersProcessId() {
        return process.getString("activityReqProcessId");
    }

    public void setResourceRequesterId(String s) {
        process.set("resourceReqId", s);
    }

    public String getResourceRequesterId() {
        return process.getString("resourceReqId");
    }

    public void setState(String s) {
        process.set("currentState", s);
    }

    public String getState() {
        return process.getString("currentState");
    }

    public String getName() {
        return process.getString("processName");
    }

    public void setName(String s) {
        process.set("processName", s);
    }

    public String getDescription() {
        return process.getString("description");
    }

    public void setDescription(String s) {
        process.set("description", s);
    }

    public int getPriority() {
        return process.getLong("priority").intValue();
    }

    public void setPriority(int i) {
        process.set("priority", new Long(i));
    }

    public long getLastStateTime() {
        return process.get("lastStateTime") != null ? process.getTimestamp("lastStateTime").getTime() : 0;
    }

    public void setLastStateTime(long timestamp) {
        process.set("lastStateTime", UtilDateTime.getTimestamp(timestamp));
    }

    public long getCreatedTime() {
        return process.get("createdTime") != null ? process.getTimestamp("createdTime").getTime() : 0;
    }

    public void setCreatedTime(long time) {
        process.set("createdTime", UtilDateTime.getTimestamp(time));
    }

    public long getStartedTime() {
        return process.get("startedTime") != null ? process.getTimestamp("startedTime").getTime() : 0;
    }

    public void setStartedTime(long timestamp) {
        process.set("startedTime", UtilDateTime.getTimestamp(timestamp));
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(process);
            newValue = false;
        } else {
            delegator.store(process);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            process.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(process);
            Debug.log("**** REMOVED : " + this, module);
        }

        // remove all requesters
        delegator.removeByAnd("WfRequester", UtilMisc.toMap("processId", this.getId()));
    }
}
