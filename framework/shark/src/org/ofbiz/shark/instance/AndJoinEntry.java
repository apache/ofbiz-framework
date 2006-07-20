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
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class AndJoinEntry extends InstanceEntityObject implements AndJoinEntryInterface {

    public static final String module = AndJoinEntry.class.getName();

    protected GenericValue andJoin = null;
    protected boolean newValue = false;

    protected AndJoinEntry(EntityPersistentMgr mgr, GenericDelegator delegator, String andJoinId) {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.andJoin = delegator.findByPrimaryKey("WfAndJoin", UtilMisc.toMap("andJoinId", andJoinId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected AndJoinEntry(EntityPersistentMgr mgr, GenericValue andJoin) {
        super(mgr, andJoin.getDelegator());
        this.andJoin = andJoin;
    }

    public AndJoinEntry(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.andJoin = delegator.makeValue("WfAndJoin", UtilMisc.toMap("andJoinId", delegator.getNextSeqId("WfAndJoin")));
    }

    public static AndJoinEntry getInstance(EntityPersistentMgr mgr, GenericValue andJoin) {
        AndJoinEntry var = new AndJoinEntry(mgr, andJoin);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public static AndJoinEntry getInstance(EntityPersistentMgr mgr, String andJoinId) {
        AndJoinEntry var = new AndJoinEntry(mgr, SharkContainer.getDelegator(), andJoinId);
        if (var.isLoaded()) {
            return var;
        }
        return null;
    }

    public boolean isLoaded() {
        if (andJoin == null) {
            return false;
        }
        return true;
    }

    public void setProcessId(String procId) {
        andJoin.set("processId", procId);
    }

    public String getProcessId() {
        return andJoin.getString("processId");
    }

    public void setActivitySetDefinitionId(String asdId) {
        andJoin.set("activitySetDefId", asdId);
    }

    public String getActivitySetDefinitionId() {
        return andJoin.getString("activitySetDefId");
    }

    public void setActivityDefinitionId(String adId) {
        andJoin.set("activityDefId", adId);
    }

    public String getActivityDefinitionId() {
        return andJoin.getString("activityDefId");
    }

    public void setActivityId(String actId) {
        andJoin.set("activityId", actId);
    }

    public String getActivityId() {
        return andJoin.getString("activityId");
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            newValue = false;
            delegator.createOrStore(andJoin);
        } else {
            delegator.store(andJoin);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            andJoin.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(andJoin);
        }
    }
}
