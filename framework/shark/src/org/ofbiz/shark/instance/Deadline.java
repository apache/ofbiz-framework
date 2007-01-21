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

import java.util.Date;

import org.enhydra.shark.api.internal.instancepersistence.DeadlinePersistenceInterface;
import org.enhydra.shark.api.internal.instancepersistence.PersistenceException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.ofbiz.shark.container.SharkContainer;


public class Deadline extends InstanceEntityObject implements DeadlinePersistenceInterface {

    public static final String module = Deadline.class.getName();

    protected GenericValue deadline = null;
    protected boolean newValue = false;

    protected Deadline(EntityPersistentMgr mgr, GenericDelegator delegator, String deadlineId) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.deadline = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfDeadline, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.deadlineId, deadlineId));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected Deadline(EntityPersistentMgr mgr, GenericValue deadline) {
        super(mgr, deadline.getDelegator());
        this.deadline = deadline;
    }

    public Deadline(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.deadline = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfDeadline, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.deadlineId, delegator.getNextSeqId(org.ofbiz.shark.SharkConstants.WfDeadline)));
    }

    public static Deadline getInstance(EntityPersistentMgr mgr, GenericValue deadlineV) {
        Deadline deadline = new Deadline(mgr, deadlineV);
        if (deadline.isLoaded()) {
            return deadline;
        }
        return null;
    }

    public static Deadline getInstance(EntityPersistentMgr mgr, String deadlineId) throws PersistenceException {
        Deadline deadline = new Deadline(mgr, SharkContainer.getDelegator(), deadlineId);
        if (deadline.isLoaded()) {
            return deadline;
        }
        return null;
    }

    public boolean isLoaded() {
        if (deadline == null) {
            return false;
        }
        return true;
    }

    public void setProcessId(String procId) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.processId, procId);
    }

    public String getProcessId() {
        return this.deadline.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setActivityId(String actId) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.activityId, actId);
    }

    public String getActivityId() {
        return this.deadline.getString(org.ofbiz.shark.SharkConstants.activityId);
    }

    public void setTimeLimit(long timeLimit) {
        deadline.set(org.ofbiz.shark.SharkConstants.timeLimit, new Long(timeLimit));
    }

    public long getTimeLimit() {
        if (this.deadline.get(org.ofbiz.shark.SharkConstants.timeLimit) != null) {
            return this.deadline.getLong(org.ofbiz.shark.SharkConstants.timeLimit).longValue();
        } else {
            return -1;
        }
    }

    public void setExceptionName(String exceptionName) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.exceptionName, exceptionName);
    }

    public String getExceptionName() {
        return this.deadline.getString(org.ofbiz.shark.SharkConstants.exceptionName);
    }

    public void setSynchronous(boolean sync) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.isSync, sync ? "Y" : "N");
    }

    public boolean isSynchronous() {
        return (this.deadline.get(org.ofbiz.shark.SharkConstants.isSync) == null ? false : "Y".equalsIgnoreCase(this.deadline.getString(org.ofbiz.shark.SharkConstants.isSync)));
    }

    public void setExecuted(boolean ex) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.isExecuted, ex ? "Y" : "N");
    }

    public boolean isExecuted() {
        return (this.deadline.get(org.ofbiz.shark.SharkConstants.isExecuted) == null ? false : "Y".equalsIgnoreCase(this.deadline.getString(org.ofbiz.shark.SharkConstants.isExecuted)));
    }

    public void setUniqueId(String s) {
        this.deadline.set(org.ofbiz.shark.SharkConstants.deadlineId, s);
    }

    public String getUniqueId() {
        return this.deadline.getString(org.ofbiz.shark.SharkConstants.deadlineId);
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            newValue = false;
            delegator.createOrStore(deadline);
        } else {
            delegator.store(deadline);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            deadline.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(deadline);
        }
    }
}
