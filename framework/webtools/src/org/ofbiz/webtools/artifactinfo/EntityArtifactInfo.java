/*
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
 */
package org.ofbiz.webtools.artifactinfo;

import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entityext.eca.EntityEcaRule;

/**
 *
 */
public class EntityArtifactInfo extends ArtifactInfoBase {
    protected ModelEntity modelEntity;
    
    public EntityArtifactInfo(String entityName, ArtifactInfoFactory aif) throws GenericEntityException {
        super(aif);
        this.modelEntity = this.aif.getModelEntity(entityName);
    }
    
    public ModelEntity getModelEntity() {
        return this.modelEntity;
    }
    
    public String getDisplayName() {
        return this.getUniqueId();
    }
    
    public String getDisplayType() {
        return "Entity";
    }
    
    public String getType() {
        return ArtifactInfoFactory.EntityInfoTypeId;
    }
    
    public String getUniqueId() {
        return this.modelEntity.getEntityName();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof EntityArtifactInfo) {
            return this.modelEntity.getEntityName().equals(((EntityArtifactInfo) obj).modelEntity.getEntityName());
        } else {
            return false;
        }
    }
    
    public Set<EntityArtifactInfo> getEntitiesRelatedOne() {
        Set<EntityArtifactInfo> entitySet = FastSet.newInstance();
        // TODO: implement this
        return entitySet;
    }

    public Set<EntityArtifactInfo> getEntitiesRelatedMany() {
        Set<EntityArtifactInfo> entitySet = FastSet.newInstance();
        // TODO: implement this
        return entitySet;
    }
    
    /** Get the Services that use this Entity */
    public Set<ServiceArtifactInfo> getServicesUsingEntity() {
        return this.aif.allServiceInfosReferringToEntityName.get(this.modelEntity.getEntityName());
    }
    
    /** Get the Services called by Entity ECA */
    public Set<ServiceArtifactInfo> getServicesCalledByEntityEca() {
        Set<ServiceArtifactInfo> serviceSet = FastSet.newInstance();
        // TODO: implement this
        return serviceSet;
    }
    
    public Set<EntityEcaRule> getEntityEcaRules() {
        Set<EntityEcaRule> eecaSet = FastSet.newInstance();
        // TODO: implement this
        return eecaSet;
    }
    
    public Set<FormWidgetArtifactInfo> getFormsUsingEntity() {
        Set<FormWidgetArtifactInfo> formSet = FastSet.newInstance();
        // TODO: implement this
        return formSet;
    }
    
    public Set<ScreenWidgetArtifactInfo> getScreensUsingEntity() {
        Set<ScreenWidgetArtifactInfo> screenSet = FastSet.newInstance();
        // TODO: implement this
        return screenSet;
    }
}
