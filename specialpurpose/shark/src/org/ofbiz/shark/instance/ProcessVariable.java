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
import org.enhydra.shark.api.internal.instancepersistence.ProcessVariablePersistenceInterface;
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

public class ProcessVariable extends InstanceEntityObject implements ProcessVariablePersistenceInterface {

    public static final String module = ProcessVariable.class.getName();

    protected GenericValue processVariable = null;
    protected boolean newValue = false;

    protected ProcessVariable(EntityPersistentMgr mgr, GenericDelegator delegator, String processVariableId) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.processVariable = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfProcessVariable, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.processVariableId, processVariableId));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected ProcessVariable(EntityPersistentMgr mgr, GenericValue processVariable) {
        super(mgr, processVariable.getDelegator());
        this.processVariable = processVariable;
    }

    public ProcessVariable(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.processVariable = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfProcessVariable, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.processVariableId, delegator.getNextSeqId(org.ofbiz.shark.SharkConstants.WfProcessVariable)));
        Debug.log("******* New process variable created", module);
    }

    public static ProcessVariable getInstance(EntityPersistentMgr mgr, GenericValue processVariable) {
        ProcessVariable var = new ProcessVariable(mgr, processVariable);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public static ProcessVariable getInstance(EntityPersistentMgr mgr, String processVariableId) throws PersistenceException {
        ProcessVariable var = new ProcessVariable(mgr, SharkContainer.getDelegator(), processVariableId);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public boolean isLoaded() {
        if (processVariable == null) {
            return false;
        }
        return true;
    }

    public void setProcessId(String pId) {
        processVariable.set(org.ofbiz.shark.SharkConstants.processId, pId);
    }

    public String getProcessId() {
        return processVariable.getString(org.ofbiz.shark.SharkConstants.processId);
    }

    public void setDefinitionId(String defId) {
        processVariable.set(org.ofbiz.shark.SharkConstants.definitionId, defId);
    }

    public String getDefinitionId() {
        return processVariable.getString(org.ofbiz.shark.SharkConstants.definitionId);
    }

    public void setValue(Object val) {
        if (val instanceof String) {
            processVariable.set(org.ofbiz.shark.SharkConstants.valueField, org.ofbiz.shark.SharkConstants.strValue);
            processVariable.set(org.ofbiz.shark.SharkConstants.strValue, val);
        } else if (val instanceof Number) {
            if (val instanceof Double) {
                processVariable.set(org.ofbiz.shark.SharkConstants.valueField, org.ofbiz.shark.SharkConstants.dblValue);
                processVariable.set(org.ofbiz.shark.SharkConstants.dblValue, val);
            } else {
                processVariable.set(org.ofbiz.shark.SharkConstants.valueField, org.ofbiz.shark.SharkConstants.numValue);
                processVariable.set(org.ofbiz.shark.SharkConstants.numValue, val);
            }
        } else {
            byte[] value = UtilObject.getBytes(val);
            processVariable.setBytes(org.ofbiz.shark.SharkConstants.objValue, (value != null ? value : null));
        }
    }

    public Object getValue() {
        String fieldName = processVariable.getString(org.ofbiz.shark.SharkConstants.valueField);
        if (org.ofbiz.shark.SharkConstants.objValue.equals(fieldName)) {
            byte[] value = processVariable.getBytes(fieldName);
            return UtilObject.getObject(value);
        } else {
            return processVariable.get(fieldName);
        }
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(processVariable);
            newValue = false;
        } else {
            delegator.store(processVariable);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            processVariable.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(processVariable);
        }
    }
}
