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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.shark.transaction.JtaTransaction;
import org.ofbiz.base.util.UtilMisc;

import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.internal.partmappersistence.ParticipantMap;
import org.enhydra.shark.api.internal.partmappersistence.ParticipantMappingManager;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.ParticipantMappingTransaction;
import org.enhydra.shark.api.TransactionException;

/**
 * Shark Participant Mappings Implementation
 */

public class EntityParticipantMappingMgr implements ParticipantMappingManager {

    public static final String module = EntityParticipantMappingMgr.class.getName();

    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callbackUtilities) throws RootException {
        this.callBack = callbackUtilities;
    }

    public boolean saveParticipantMapping(ParticipantMappingTransaction mappingTransaction, ParticipantMap participantMap) throws RootException {
        ((EntityParticipantMap) participantMap).store();
        return true;
    }

    public boolean deleteParticipantMapping(ParticipantMappingTransaction mappingTransaction, ParticipantMap participantMap) throws RootException {
        ((EntityParticipantMap) participantMap).remove();
        return true;
    }

    public List getAllParticipantMappings(ParticipantMappingTransaction mappingTransaction) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        try {
            lookupList = delegator.findAll(org.ofbiz.shark.SharkConstants.WfParticipantMap);
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        if (lookupList != null) {
            List compiledList = new ArrayList();
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                compiledList.add(EntityParticipantMap.getInstance(v));
            }
            return compiledList;
        } else {
            return new ArrayList();
        }
    }

    public boolean doesParticipantMappingExist(ParticipantMappingTransaction mappingTransaction, ParticipantMap participantMap) throws RootException {
        List mappings = getParticipantMappings(mappingTransaction, participantMap.getPackageId(), participantMap.getProcessDefinitionId(), participantMap.getParticipantId());
        if (mappings != null && mappings.size() > 0) {
            return true;
        }
        return false;
    }

    public ParticipantMap createParticipantMap() {
        return new EntityParticipantMap(SharkContainer.getDelegator());
    }

    public List getParticipantMappings(ParticipantMappingTransaction mappingTransaction, String packageId, String processDefId, String participantId) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfParticipantMap, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.packageId, packageId, org.ofbiz.shark.SharkConstants.processDefId, processDefId, org.ofbiz.shark.SharkConstants.participantId, participantId));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        if (lookupList != null) {
            List compiledList = new ArrayList();
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                compiledList.add(EntityParticipantMap.getInstance(v));
            }
            return compiledList;
        } else {
            return new ArrayList();
        }
    }

    public List getParticipantMappings(ParticipantMappingTransaction mappingTransaction, String userName) throws RootException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List lookupList = null;
        try {
            lookupList = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfParticipantMap, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.userName, userName));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
        if (lookupList != null) {
            List compiledList = new ArrayList();
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                compiledList.add(EntityParticipantMap.getInstance(v));
            }
            return compiledList;
        } else {
            return new ArrayList();
        }
    }

    public boolean deleteParticipantMappings(ParticipantMappingTransaction mappingTransaction, String packageId, String processDefId, String participantId) throws RootException {
        List participants = this.getParticipantMappings(mappingTransaction, packageId, processDefId, participantId);
        if (participants != null) {
            Iterator i = participants.iterator();
            while (i.hasNext()) {
                EntityParticipantMap map = (EntityParticipantMap) i.next();
                map.remove();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean deleteParticipantMappings(ParticipantMappingTransaction mappingTransaction, String userName) throws RootException {
        List participants = this.getParticipantMappings(mappingTransaction, userName);
        if (participants != null) {
            Iterator i = participants.iterator();
            while (i.hasNext()) {
                EntityParticipantMap map = (EntityParticipantMap) i.next();
                map.remove();
            }
            return true;
        } else {
            return false;
        }
    }

    public List getUsernames(ParticipantMappingTransaction mappingTransaction, String packageId, String processDefId, String participantId) throws RootException {
        List participants = this.getParticipantMappings(mappingTransaction, packageId, processDefId, participantId);
        List compiledList = new ArrayList();
        if (participants != null) {
            Iterator i = participants.iterator();
            while (i.hasNext()) {
                EntityParticipantMap map = (EntityParticipantMap) i.next();
                compiledList.add(map.getUsername());
            }
        }
        return compiledList;
    }

    public ParticipantMappingTransaction getParticipantMappingTransaction() throws TransactionException {
        return new JtaTransaction();
    }
}
