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
package org.ofbiz.shark.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.shark.transaction.JtaTransaction;

import org.enhydra.shark.api.ApplicationMappingTransaction;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.TransactionException;
import org.enhydra.shark.api.internal.appmappersistence.ApplicationMap;
import org.enhydra.shark.api.internal.appmappersistence.ApplicationMappingManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;

/**
 * Shark Application Mappings Implementation
 */

public class EntityApplicationMappingMgr implements ApplicationMappingManager {

    public static final String module = EntityApplicationMappingMgr.class.getName();
    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callbackUtilities) throws RootException {
        this.callBack = callbackUtilities;
    }

    public boolean saveApplicationMapping(ApplicationMappingTransaction mappingTransaction, ApplicationMap applicationMap) throws RootException {
        ((EntityApplicationMap) applicationMap).store();
        return true;
    }

    public boolean deleteApplicationMapping(ApplicationMappingTransaction mappingTransaction, ApplicationMap applicationMap) throws RootException {
        ((EntityApplicationMap) applicationMap).remove();
        return true;
    }

    public boolean updateApplicationMapping(ApplicationMappingTransaction mappingTransaction, ApplicationMap applicationMap) throws RootException {
        return saveApplicationMapping(mappingTransaction, applicationMap);
    }

    public List getAllApplicationMappings(ApplicationMappingTransaction mappingTransaction) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        try {
            lookupList = delegator.findAll(org.ofbiz.shark.SharkConstants.WfApplicationMap);
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        if (lookupList != null) {
            List compiledList = new ArrayList();
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                compiledList.add(EntityApplicationMap.getInstance(v));
            }
            return compiledList;
        } else {
            return new ArrayList();
        }
    }

    public ApplicationMap createApplicationMap() {
        return new EntityApplicationMap(SharkContainer.getDelegator());
    }

    public boolean deleteApplicationMapping(ApplicationMappingTransaction mappingTransaction, String packageId, String processDefId, String appDefId) throws RootException {
        EntityApplicationMap app = (EntityApplicationMap) this.getApplicationMap(mappingTransaction, packageId, processDefId, appDefId);
        if (app != null && app.isLoaded()) {
            app.remove();
            return true;
        } else {
            return false;
        }
    }

    public ApplicationMap getApplicationMap(ApplicationMappingTransaction mappingTransaction, String packageId, String processDefId, String appDefId) throws RootException {
        return EntityApplicationMap.getInstance(packageId, processDefId, appDefId);
    }

    public ApplicationMappingTransaction getApplicationMappingTransaction() throws TransactionException {
        return new JtaTransaction();
    }
}
