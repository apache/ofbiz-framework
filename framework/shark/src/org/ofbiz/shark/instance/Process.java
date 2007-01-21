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

import org.enhydra.shark.api.internal.instancepersistence.PersistenceException;
import org.enhydra.shark.api.internal.instancepersistence.ProcessMgrPersistenceInterface;
import org.enhydra.shark.api.internal.instancepersistence.ProcessPersistenceInterface;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;



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
                this.process = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfProcess, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.processId, processId));
            } catch (GenericEntityException e) {
                Debug.logError("Invalid delegator object passed", module);
                e.printStackTrace();
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
        this.process = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfProcess, null);
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
        process.set(org.ofbiz.shark.SharkConstants.processId, s);
    }

    public String getId() {
        return process.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setProcessMgrName(String s) {
        process.set(org.ofbiz.shark.SharkConstants.mgrName, s);
        try {
            ProcessMgrPersistenceInterface pm = mgr.restoreProcessMgr(s, null);
            process.set(org.ofbiz.shark.SharkConstants.packageId, pm.getPackageId());
            process.set(org.ofbiz.shark.SharkConstants.packageVer, pm.getVersion());
        } catch (PersistenceException e) {
            Debug.logError(e, "Unable to set package information", module);
        }
    }

    public String getProcessMgrName() {
        return process.getString(org.ofbiz.shark.SharkConstants.mgrName);
    }

    public void setExternalRequester(Object o) {
        byte[] value = UtilObject.getBytes(o);
        process.setBytes(org.ofbiz.shark.SharkConstants.externalReq, (value != null ? value : null));
    }

    public Object getExternalRequester() {
        byte[] value = process.getBytes(org.ofbiz.shark.SharkConstants.externalReq);
        return UtilObject.getObject(value);
    }

    public void setActivityRequesterId(String s) {
        process.set(org.ofbiz.shark.SharkConstants.activityReqId, s);
    }

    public String getActivityRequesterId() {
        return process.getString(org.ofbiz.shark.SharkConstants.activityReqId);
    }

    public void setActivityRequestersProcessId(String s) {
        process.set(org.ofbiz.shark.SharkConstants.activityReqProcessId, s);
    }

    public String getActivityRequestersProcessId() {
        return process.getString(org.ofbiz.shark.SharkConstants.activityReqProcessId);
    }

    public void setResourceRequesterId(String s) {
        process.set(org.ofbiz.shark.SharkConstants.resourceReqId, s);
    }

    public String getResourceRequesterId() {
        return process.getString(org.ofbiz.shark.SharkConstants.resourceReqId);
    }

    public void setState(String s) {
        process.set(org.ofbiz.shark.SharkConstants.currentState, s);
    }

    public String getState() {
        return process.getString(org.ofbiz.shark.SharkConstants.currentState);
    }

    public String getName() {
        return process.getString(org.ofbiz.shark.SharkConstants.processName);
    }

    public void setName(String s) {
        process.set(org.ofbiz.shark.SharkConstants.processName, s);
    }

    public String getDescription() {
        return process.getString(org.ofbiz.shark.SharkConstants.description);
    }

    public void setDescription(String s) {
        process.set(org.ofbiz.shark.SharkConstants.description, s);
    }

    public short getPriority() {
        return process.getLong(org.ofbiz.shark.SharkConstants.priority).shortValue();
    }

    public void setPriority(int i) {
        process.set(org.ofbiz.shark.SharkConstants.priority, new Long(i));
    }

    public long getLastStateTime() {
        return process.get(org.ofbiz.shark.SharkConstants.lastStateTime) != null ? process.getLong(org.ofbiz.shark.SharkConstants.lastStateTime).longValue() : 0;
    }

    public void setLastStateTime(long timestamp) {
        process.set(org.ofbiz.shark.SharkConstants.lastStateTime, new Long(timestamp));
    }

    public long getCreatedTime() {
        return process.get(org.ofbiz.shark.SharkConstants.createdTime) != null ? process.getLong(org.ofbiz.shark.SharkConstants.createdTime).longValue() : 0;
    }

    public void setCreatedTime(long time) {
        process.set(org.ofbiz.shark.SharkConstants.createdTime, new Long(time));
    }

    public long getStartedTime() {
        return process.get(org.ofbiz.shark.SharkConstants.startedTime) != null ? process.getLong(org.ofbiz.shark.SharkConstants.startedTime).longValue() : 0;
    }

    public void setStartedTime(long timestamp) 
    {
        process.set(org.ofbiz.shark.SharkConstants.startedTime, new Long(timestamp));
    }


    public void store() throws GenericEntityException {
        if (newValue) 
        {
            delegator.createOrStore(process);
            newValue = false;
        }
        else 
        {
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
        delegator.removeByAnd(org.ofbiz.shark.SharkConstants.WfRequester, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.processId, this.getId()));
    }

    public void setExternalRequesterClassName(String arg0) {
        process.set(org.ofbiz.shark.SharkConstants.ExternalRequesterClassName, arg0);
    }

    public String getExternalRequesterClassName() {
        return (String)process.get(org.ofbiz.shark.SharkConstants.ExternalRequesterClassName);
    }

    public void setPriority(short arg0) {
        process.set(org.ofbiz.shark.SharkConstants.priority, new Long(arg0));

    }

    public long getLimitTime() {
        if (this.process.get(org.ofbiz.shark.SharkConstants.timeLimit) != null) {
            return this.process.getLong(org.ofbiz.shark.SharkConstants.timeLimit).longValue();
        } else {
            return -1;
        }
    }

    public void setLimitTime(long timeLimit) {
        process.set(org.ofbiz.shark.SharkConstants.timeLimit, new Long(timeLimit));
    }
}
