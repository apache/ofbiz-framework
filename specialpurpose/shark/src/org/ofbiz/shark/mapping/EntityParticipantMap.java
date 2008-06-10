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

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.shark.container.SharkContainer;

import org.enhydra.shark.api.internal.partmappersistence.ParticipantMap;
import org.enhydra.shark.api.RootException;

/**
 * Shark Participant Map Implementation
 */

public class EntityParticipantMap implements ParticipantMap {

    public static final String module = EntityParticipantMap.class.getName();

    protected GenericDelegator delegator = null;
    protected GenericValue participant = null;
    protected boolean newValue = false;

    protected EntityParticipantMap(GenericDelegator delegator, String packageId, String processDefId, String participantId) throws RootException {
        this.delegator = delegator;
        try {
            this.participant = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfParticipantMap, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.packageId, packageId, org.ofbiz.shark.SharkConstants.processDefId, processDefId, org.ofbiz.shark.SharkConstants.participantId, participantId));
        } catch (GenericEntityException e) {
            throw new RootException(e);
        }
    }

    protected EntityParticipantMap(GenericValue application) {
        this.participant = application;
        this.delegator = application.getDelegator();
    }

    public EntityParticipantMap(GenericDelegator delegator) {
        this.newValue = true;
        this.delegator = delegator;

        this.participant = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfParticipantMap, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.participantMapId, delegator.getNextSeqId(org.ofbiz.shark.SharkConstants.WfParticipantMap)));
    }

    public static EntityParticipantMap getInstance(GenericValue participant) {
        EntityParticipantMap part = new EntityParticipantMap(participant);
        if (part.isLoaded()) {
            return part;
        }
        return null;
    }

    public static EntityParticipantMap getInstance(String packageId, String processDefId, String participantId) throws RootException {
        EntityParticipantMap part = new EntityParticipantMap(SharkContainer.getDelegator(), packageId, processDefId, participantId);
        if (part.isLoaded()) {
            return part;
        }
        return null;
    }

    public boolean isLoaded() {
        if (participant == null) {
            return false;
        }
        return true;
    }

    public void setParticipantId(String participantId) {
        participant.set(org.ofbiz.shark.SharkConstants.participantId, participantId);
    }

    public String getParticipantId() {
        return participant.getString(org.ofbiz.shark.SharkConstants.participantId);
    }

    public void setPackageId(String packageId) {
        participant.set(org.ofbiz.shark.SharkConstants.packageId, packageId);
    }

    public String getPackageId() {
        return participant.getString(org.ofbiz.shark.SharkConstants.participantId);
    }

    public void setProcessDefinitionId(String processDefId) {
        participant.set(org.ofbiz.shark.SharkConstants.processDefId, processDefId);
    }

    public String getProcessDefinitionId() {
        return participant.getString(org.ofbiz.shark.SharkConstants.processDefId);
    }

    public void setUsername(String userName) {
        participant.set(org.ofbiz.shark.SharkConstants.userName, userName);
    }

    public String getUsername() {
        return participant.getString(org.ofbiz.shark.SharkConstants.userName);
    }

    public boolean getIsGroupUser() {
        return (participant.getBoolean(org.ofbiz.shark.SharkConstants.isGroupUser) != null ? participant.getBoolean(org.ofbiz.shark.SharkConstants.isGroupUser).booleanValue() : false);
    }

    public void setIsGroupUser(boolean isGroupUser) {
        participant.set(org.ofbiz.shark.SharkConstants.isGroupUser, new Boolean(isGroupUser));
    }

    public void store() throws RootException {
        if (newValue) {
            try {
                delegator.create(participant);
                newValue = false;
            } catch (GenericEntityException e) {
                throw new RootException(e);
            }
        } else {
            try {
                delegator.store(participant);
            } catch (GenericEntityException e) {
                throw new RootException(e);
            }
        }
    }

    public void reload() throws RootException {
        if (!newValue) {
            try {
                delegator.refresh(participant);
            } catch (GenericEntityException e) {
                throw new RootException(e);
            }
        }
    }

    public void remove() throws RootException {
        Debug.log("::Remove Participant Map::", module);
        if (!newValue) {
            try {
                delegator.removeValue(participant);
            } catch (GenericEntityException e) {
                throw new RootException(e);
            }
        }
    }
}
