/*
 * $Id: EntityParticipantMap.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class EntityParticipantMap implements ParticipantMap {

    public static final String module = EntityParticipantMap.class.getName();
    
    protected GenericDelegator delegator = null;
    protected GenericValue participant = null;
    protected boolean newValue = false;

    protected EntityParticipantMap(GenericDelegator delegator, String packageId, String processDefId, String participantId) throws RootException {
        this.delegator = delegator;
        try {
            this.participant = delegator.findByPrimaryKey("WfParticipantMap", UtilMisc.toMap("packageId", packageId, "processDefId", processDefId, "participantId", participantId));
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

        this.participant = delegator.makeValue("WfParticipantMap", UtilMisc.toMap("participantMapId", delegator.getNextSeqId("WfParticipantMap")));
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
        participant.set("participantId", participantId);
    }

    public String getParticipantId() {
        return participant.getString("participantId");
    }

    public void setPackageId(String packageId) {
        participant.set("packageId", packageId);
    }

    public String getPackageId() {
        return participant.getString("participantId");
    }

    public void setProcessDefinitionId(String processDefId) {
        participant.set("processDefId", processDefId);
    }

    public String getProcessDefinitionId() {
        return participant.getString("processDefId");
    }

    public void setUsername(String userName) {
        participant.set("userName", userName);
    }

    public String getUsername() {
        return participant.getString("userName");
    }

    public boolean getIsGroupUser() {
        return (participant.getBoolean("isGroupUser") != null ? participant.getBoolean("isGroupUser").booleanValue() : false);
    }

    public void setIsGroupUser(boolean isGroupUser) {
        participant.set("isGroupUser", new Boolean(isGroupUser));
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
