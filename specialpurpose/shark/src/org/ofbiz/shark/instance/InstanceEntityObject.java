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

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericDelegator;

public abstract class InstanceEntityObject {

    protected transient GenericDelegator delegator = null;
    protected EntityPersistentMgr mgr = null;
    protected String delegatorName = null;

    public InstanceEntityObject(EntityPersistentMgr mgr, GenericDelegator delegator) {
        this.delegatorName = delegator.getDelegatorName();
        this.delegator = delegator;
        this.mgr = mgr;
    }

    public EntityPersistentMgr getPersistentManager() {
        return this.mgr;
    }

    public GenericDelegator getGenericDelegator() {
        if (this.delegator == null && delegatorName != null) {
            this.delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }
        return this.delegator;
    }

    public abstract boolean isLoaded();

    public abstract void store() throws GenericEntityException;

    public abstract void reload() throws GenericEntityException;

    public abstract void remove() throws GenericEntityException;
}
