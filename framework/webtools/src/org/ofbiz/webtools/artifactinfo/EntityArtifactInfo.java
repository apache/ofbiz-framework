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

import java.util.List;

import javolution.util.FastList;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entityext.eca.EntityEcaRule;
import org.ofbiz.webtools.artifactinfo.ArtifactInfoFactory;

/**
 *
 */
public class EntityArtifactInfo {
    protected ArtifactInfoFactory aif;
    protected ModelEntity modelEntity;
    
    public EntityArtifactInfo(String entityName, ArtifactInfoFactory aif) throws GenericEntityException {
        this.aif = aif;
        this.modelEntity = this.aif.getModelEntity(entityName);
    }
    
    public ModelEntity getModelEntity() {
        return this.modelEntity;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof EntityArtifactInfo) {
            return this.modelEntity.getEntityName().equals(((EntityArtifactInfo) obj).modelEntity.getEntityName());
        } else {
            return false;
        }
    }
    
    public List<EntityArtifactInfo> getEntitiesRelatedOne() {
        List<EntityArtifactInfo> entityList = FastList.newInstance();
        // TODO: implement this
        return entityList;
    }

    public List<EntityArtifactInfo> getEntitiesRelatedMany() {
        List<EntityArtifactInfo> entityList = FastList.newInstance();
        // TODO: implement this
        return entityList;
    }
    
    /** Get the Services that use this Entity */
    public List<ServiceArtifactInfo> getServicesUsingEntity() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: implement this
        return serviceList;
    }
    
    /** Get the Services called by Entity ECA */
    public List<ServiceArtifactInfo> getServicesCalledByEntityEca() {
        List<ServiceArtifactInfo> serviceList = FastList.newInstance();
        // TODO: implement this
        return serviceList;
    }
    
    public List<EntityEcaRule> getEntityEcaRules() {
        List<EntityEcaRule> eecaList = FastList.newInstance();
        // TODO: implement this
        return eecaList;
    }
    
    public List<FormWidgetArtifactInfo> getFormsUsingEntity() {
        List<FormWidgetArtifactInfo> formList = FastList.newInstance();
        // TODO: implement this
        return formList;
    }
    
    public List<ScreenWidgetArtifactInfo> getScreensUsingEntity() {
        List<ScreenWidgetArtifactInfo> screenList = FastList.newInstance();
        // TODO: implement this
        return screenList;
    }
}
