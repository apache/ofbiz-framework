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
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 */
public class Activity extends InstanceEntityObject implements ActivityPersistenceInterface {

    public static final String module = Activity.class.getName();

    protected GenericValue activity = null;
    protected boolean newValue = false;

    protected Activity(EntityPersistentMgr mgr, GenericDelegator delegator, String activityId) throws PersistenceException {
        super(mgr, delegator);
        this.delegator = delegator;
        if (this.delegator != null) {
            try {
                this.activity = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfActivity, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.activityId, activityId));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected Activity(EntityPersistentMgr mgr, GenericValue activity) {
        super(mgr, activity.getDelegator());
        this.activity = activity;
    }

    public Activity(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.activity = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfActivity, null);
    }

    public static Activity getInstance(EntityPersistentMgr mgr, GenericValue activity) {
        Activity act = new Activity(mgr, activity);
        if (act.isLoaded()) {
            return act;
        }
        return null;
    }

    public static Activity getInstance(EntityPersistentMgr mgr, String activityId) throws PersistenceException {
        Activity act = new Activity(mgr, SharkContainer.getDelegator(), activityId);
        if (act.isLoaded()) {
            return act;
        }
        return null;
    }

    public boolean isLoaded() {
        if (activity == null) {
            return false;
        }
        return true;
    }

    public void setId(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.activityId, s);
    }

    public String getId() {
        return activity.getString(org.ofbiz.shark.SharkConstants.activityId);
    }

    public void setActivitySetDefinitionId(String asdId) {
        activity.set(org.ofbiz.shark.SharkConstants.setDefinitionId, asdId);
    }

    public String getActivitySetDefinitionId() {
        return activity.getString(org.ofbiz.shark.SharkConstants.setDefinitionId);
    }

    public void setActivityDefinitionId(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.definitionId, s);
    }

    public String getActivityDefinitionId() {
       return activity.getString(org.ofbiz.shark.SharkConstants.definitionId);
    }

    public void setProcessId(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.processId, s);
    }

    public String getProcessId() {
        return activity.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setSubflowProcessId(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.subFlowId, s);
    }

    public String getSubflowProcessId() {
        return activity.getString(org.ofbiz.shark.SharkConstants.subFlowId);
    }

    public void setSubflowAsynchronous(boolean b) {
        activity.set(org.ofbiz.shark.SharkConstants.isSubAsync, (b ? "Y" : "N"));
    }

    public boolean isSubflowAsynchronous() {
        String isAsync = activity.getString(org.ofbiz.shark.SharkConstants.isSubAsync);
        return isAsync != null && isAsync.equals("Y") ? true : false;
    }

    public void setResourceUsername(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.resourceUser, s);
    }

    public String getResourceUsername() {
        return activity.getString(org.ofbiz.shark.SharkConstants.resourceUser);
    }

    public void setState(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.currentState, s);
    }

    public String getState() {
        return activity.getString(org.ofbiz.shark.SharkConstants.currentState);
    }

    public void setBlockActivityId(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.blockId, s);
    }

    public String getBlockActivityId() {
        return activity.getString(org.ofbiz.shark.SharkConstants.blockId);
    }

    public String getName() {
        return activity.getString(org.ofbiz.shark.SharkConstants.activityName);
    }

    public void setName(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.activityName, s);
    }

    public String getDescription() {
        return activity.getString(org.ofbiz.shark.SharkConstants.description);
    }

    public void setDescription(String s) {
        activity.set(org.ofbiz.shark.SharkConstants.description, s);
    }

    public short getPriority() {
        return activity.getLong(org.ofbiz.shark.SharkConstants.priority).shortValue();
    }

    public void setPriority(int i) {
        activity.set(org.ofbiz.shark.SharkConstants.priority, new Long(i));
    }

    public long getLastStateTime() {
        return activity.get(org.ofbiz.shark.SharkConstants.lastStateTime) != null ? activity.getLong(org.ofbiz.shark.SharkConstants.lastStateTime).longValue() : 0;
    }

    public void setLastStateTime(long timestamp) {
        activity.set(org.ofbiz.shark.SharkConstants.lastStateTime, new Long(timestamp));
    }

    public long getAcceptedTime() {
        return activity.get(org.ofbiz.shark.SharkConstants.acceptedTime) != null ? activity.getLong(org.ofbiz.shark.SharkConstants.acceptedTime).longValue() : 0;
    }

    public void setAcceptedTime(long timestamp) 
    {
        activity.set(org.ofbiz.shark.SharkConstants.acceptedTime, new Long(timestamp));
    }

    public long getActivatedTime() {
        return activity.get(org.ofbiz.shark.SharkConstants.activatedTime) != null ? activity.getLong(org.ofbiz.shark.SharkConstants.activatedTime).longValue() : 0;
    }

    public void setActivatedTime(long timestamp) {
        activity.set(org.ofbiz.shark.SharkConstants.activatedTime, new Long(timestamp));
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(activity);
            newValue = false;
        } else {
            delegator.store(activity);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            activity.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(activity);
            Debug.log("**** REMOVED : " + this, module);
        }
    }

    public void setProcessMgrName(String arg0) 
    {
        activity.set(org.ofbiz.shark.SharkConstants.processMgrName, arg0);
    }

    public String getProcessMgrName() {
        return (String)activity.get(org.ofbiz.shark.SharkConstants.processMgrName);
    }

    public void setPriority(short arg0) {
        activity.set(org.ofbiz.shark.SharkConstants.priority, new Long(arg0));
    }


    public long getLimitTime() {
        if (this.activity.get(org.ofbiz.shark.SharkConstants.timeLimit) != null) {
            return this.activity.getLong(org.ofbiz.shark.SharkConstants.timeLimit).longValue();
        } else {
            return -1;
        }
    }

    public void setLimitTime(long timeLimit) {
        activity.set(org.ofbiz.shark.SharkConstants.timeLimit, new Long(timeLimit));
    }
}
