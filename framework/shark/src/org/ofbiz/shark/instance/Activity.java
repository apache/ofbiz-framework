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
                this.activity = delegator.findByPrimaryKey("WfActivity", UtilMisc.toMap("activityId", activityId));
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
        this.activity = delegator.makeValue("WfActivity", null);
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
        activity.set("activityId", s);
    }

    public String getId() {
        return activity.getString("activityId");
    }

    public void setActivitySetDefinitionId(String asdId) {
        activity.set("setDefinitionId", asdId);
    }

    public String getActivitySetDefinitionId() {
        return activity.getString("setDefinitionId");
    }

    public void setActivityDefinitionId(String s) {
        activity.set("definitionId", s);
    }

    public String getActivityDefinitionId() {
       return activity.getString("definitionId");
    }

    public void setProcessId(String s) {
        activity.set("processId", s);
    }

    public String getProcessId() {
        return activity.getString("processId");
    }

    public void setSubflowProcessId(String s) {
        activity.set("subFlowId", s);
    }

    public String getSubflowProcessId() {
        return activity.getString("subFlowId");
    }

    public void setSubflowAsynchronous(boolean b) {
        activity.set("isSubAsync", (b ? "Y" : "N"));
    }

    public boolean isSubflowAsynchronous() {
        String isAsync = activity.getString("isSubAsync");
        return isAsync != null && isAsync.equals("Y") ? true : false;        
    }

    public void setResourceUsername(String s) {
        activity.set("resourceUser", s);
    }

    public String getResourceUsername() {
        return activity.getString("resourceUser");
    }

    public void setState(String s) {
        activity.set("currentState", s);
    }

    public String getState() {
        return activity.getString("currentState");
    }

    public void setBlockActivityId(String s) {
        activity.set("blockId", s);
    }

    public String getBlockActivityId() {
        return activity.getString("blockId");
    }

    public String getName() {
        return activity.getString("activityName");
    }

    public void setName(String s) {
        activity.set("activityName", s);
    }

    public String getDescription() {
        return activity.getString("description");
    }

    public void setDescription(String s) {
        activity.set("description", s);
    }

    public int getPriority() {
        return activity.getLong("priority").intValue();
    }

    public void setPriority(int i) {
        activity.set("priority", new Long(i));
    }

    public long getLastStateTime() {
        return activity.get("lastStateTime") != null ? activity.getTimestamp("lastStateTime").getTime() : 0;
    }

    public void setLastStateTime(long timestamp) {
        activity.set("lastStateTime", UtilDateTime.getTimestamp(timestamp));
    }

    public long getAcceptedTime() {
        return activity.get("acceptedTime") != null ? activity.getTimestamp("acceptedTime").getTime() : 0;
    }

    public void setAcceptedTime(long timestamp) {
        activity.set("acceptedTime", UtilDateTime.getTimestamp(timestamp));
    }

    public long getActivatedTime() {
        return activity.get("activatedTime") != null ? activity.getTimestamp("activatedTime").getTime() : 0;
    }

    public void setActivatedTime(long timestamp) {
        activity.set("activatedTime", UtilDateTime.getTimestamp(timestamp));
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
}
