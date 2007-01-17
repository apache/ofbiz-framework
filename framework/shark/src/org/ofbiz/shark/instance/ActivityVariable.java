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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.ActivityVariablePersistenceInterface;
import org.enhydra.shark.api.internal.instancepersistence.PersistenceException;

/**
 * Persistance Object
 */
public class ActivityVariable extends InstanceEntityObject implements ActivityVariablePersistenceInterface {

    public static final String module = ActivityVariable.class.getName();

    protected GenericValue activityVariable = null;
    protected boolean newValue = false;

    protected ActivityVariable(EntityPersistentMgr mgr, GenericDelegator delegator, String activityVariableId) throws PersistenceException {
        super(mgr, delegator);
        this.delegator = delegator;
        if (this.delegator != null) {
            try {
                this.activityVariable = delegator.findByPrimaryKey("WfActivityVariable", UtilMisc.toMap("activityVariableId", activityVariableId));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected ActivityVariable(EntityPersistentMgr mgr, GenericValue activityVariable) {
        super(mgr, activityVariable.getDelegator());
        this.activityVariable = activityVariable;
    }

    public ActivityVariable(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.activityVariable = delegator.makeValue("WfActivityVariable", UtilMisc.toMap("activityVariableId", delegator.getNextSeqId("WfActivityVariable")));
        Debug.log("******* New activity variable created", module);
    }

    public static ActivityVariable getInstance(EntityPersistentMgr mgr, GenericValue activityVariable) {
        ActivityVariable var = new ActivityVariable(mgr, activityVariable);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public static ActivityVariable getInstance(EntityPersistentMgr mgr, String activityVariableId) throws PersistenceException {
        ActivityVariable var = new ActivityVariable(mgr, SharkContainer.getDelegator(), activityVariableId);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public boolean isLoaded() {
        if (activityVariable == null) {
            return false;
        }
        return true;
    }

    public void setActivityId(String aId) {
        activityVariable.set("activityId", aId);
    }

    public String getActivityId() {
        return activityVariable.getString("activityId");
    }

    public void setDefinitionId(String defId) {
        activityVariable.set("definitionId", defId);
    }

    public String getDefinitionId() {
        return activityVariable.getString("definitionId");
    }

    public void setValue(Object val) {
        if (val instanceof String) {
            activityVariable.set("valueField", "strValue");
            activityVariable.set("strValue", val);
        } else if (val instanceof Number) {
            if (val instanceof Double) {
                activityVariable.set("valueField", "dblValue");
                activityVariable.set("dblValue", val);
            } else {
                activityVariable.set("valueField", "numValue");
                activityVariable.set("numValue", val);
            }
        } else {
            byte[] value = UtilObject.getBytes(val);
            activityVariable.setBytes("objValue", (value != null ? value : null));
        }
    }

    public Object getValue() {
        String fieldName = activityVariable.getString("valueField");
        if ("objValue".equals(fieldName)) {
            byte[] value = activityVariable.getBytes(fieldName);
            return UtilObject.getObject(value);
        } else {
            return activityVariable.get(fieldName);
        }
    }

    public void setResultVariable(boolean modified) {
        activityVariable.set("isModified", (modified ? "Y" : "N"));
    }

    public boolean isResultVariable() {
        return (activityVariable.get("isModified") != null ?
                ("Y".equals(activityVariable.getString("isModified")) ? true : false) : false);
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(activityVariable);
            newValue = false;
        } else {
            delegator.store(activityVariable);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            activityVariable.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(activityVariable);
        }
    }
}
