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
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.instancepersistence.*;

/**
 * Persistance Object
 */

public class Resource extends InstanceEntityObject implements ResourcePersistenceInterface {

    public static final String module = Resource.class.getName();

    protected GenericValue resource = null;
    protected boolean newValue = false;

    protected Resource(EntityPersistentMgr mgr, GenericDelegator delegator, String name) throws PersistenceException {
        super(mgr, delegator);
        if (this.delegator != null) {
            try {
                this.resource = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfResource, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.userName, name));
            } catch (GenericEntityException e) {
                throw new PersistenceException(e);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    protected Resource(EntityPersistentMgr mgr, GenericValue resource) {
        super(mgr, resource.getDelegator());
        this.resource = resource;
    }

    public Resource(EntityPersistentMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.resource = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfResource, null);
    }

    public static Resource getInstance(EntityPersistentMgr mgr, GenericValue resource) {
        Resource res = new Resource(mgr, resource);
        if (res.isLoaded()) {
            return res;
        }
        return null;
    }

    public static Resource getInstance(EntityPersistentMgr mgr, String name) throws PersistenceException {
        Resource res = new Resource(mgr, SharkContainer.getDelegator(), name);
        if (res.isLoaded()) {
            return res;
        }
        return null;
    }

    public boolean isLoaded() {
        if (resource == null) {
            return false;
        }
        return true;
    }

    public void setUsername(String s) {
        resource.set(org.ofbiz.shark.SharkConstants.userName, s);
    }

    public String getUsername() {
        return resource.getString(org.ofbiz.shark.SharkConstants.userName);
    }

    public void setName(String s) {
        resource.set(org.ofbiz.shark.SharkConstants.resourceName, s);
    }

    public String getName() {
        return resource.getString(org.ofbiz.shark.SharkConstants.resourceName);
    }

    public void store() throws GenericEntityException {
        if (newValue) {
            delegator.createOrStore(resource);
            newValue = false;
        } else {
            delegator.store(resource);
        }
    }

    public void reload() throws GenericEntityException {
        if (!newValue) {
            resource.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        if (!newValue) {
            delegator.removeValue(resource);
            Debug.log("**** REMOVED : " + this, module);
        }
    }
}

