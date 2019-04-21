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
package org.apache.ofbiz.webtools.artifactinfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entityext.eca.EntityEcaRule;

/**
 *
 */
public class EntityArtifactInfo extends ArtifactInfoBase {
    protected ModelEntity modelEntity;

    protected Set<EntityArtifactInfo> entitiesRelatedOne = new TreeSet<>();
    protected Set<EntityArtifactInfo> entitiesRelatedMany = new TreeSet<>();

    public EntityArtifactInfo(String entityName, ArtifactInfoFactory aif) throws GenericEntityException {
        super(aif);
        this.modelEntity = this.aif.getModelEntity(entityName);
    }

    public void populateAll() throws GeneralException {
        List<ModelRelation> relationOneList = modelEntity.getRelationsOneList();
        for (ModelRelation relationOne: relationOneList) {
            this.entitiesRelatedOne.add(this.aif.getEntityArtifactInfo(relationOne.getRelEntityName()));
        }

        List<ModelRelation> relationManyList = modelEntity.getRelationsManyList();
        for (ModelRelation relationMany: relationManyList) {
            this.entitiesRelatedMany.add(this.aif.getEntityArtifactInfo(relationMany.getRelEntityName()));
        }
    }

    public ModelEntity getModelEntity() {
        return this.modelEntity;
    }

    @Override
    public String getDisplayName() {
        return this.getUniqueId();
    }

    @Override
    public String getDisplayType() {
        return "Entity";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.EntityInfoTypeId;
    }

    @Override
    public String getUniqueId() {
        return this.modelEntity.getEntityName();
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(modelEntity.getLocation());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EntityArtifactInfo) {
            return this.modelEntity.getEntityName().equals(((EntityArtifactInfo) obj).modelEntity.getEntityName());
        } else {
            return false;
        }
    }

    public Set<EntityArtifactInfo> getEntitiesRelatedOne() {
        return this.entitiesRelatedOne;
    }

    public Set<EntityArtifactInfo> getEntitiesRelatedMany() {
        return this.entitiesRelatedMany;
    }

    /** Get the Services that use this Entity */
    public Set<ServiceArtifactInfo> getServicesUsingEntity() {
        return this.aif.allServiceInfosReferringToEntityName.get(this.modelEntity.getEntityName());
    }

    /** Get the Services called by Entity ECA */
    public Set<ServiceArtifactInfo> getServicesCalledByEntityEca() {
        Set<ServiceArtifactInfo> serviceSet = new HashSet<>();
        // TODO: implement this
        return serviceSet;
    }

    public Set<EntityEcaRule> getEntityEcaRules() {
        Set<EntityEcaRule> eecaSet = new HashSet<>();
        // TODO: implement this
        return eecaSet;
    }

    public Set<FormWidgetArtifactInfo> getFormsUsingEntity() {
        return this.aif.allFormInfosReferringToEntityName.get(this.modelEntity.getEntityName());
    }

    public Set<ScreenWidgetArtifactInfo> getScreensUsingEntity() {
        return this.aif.allScreenInfosReferringToEntityName.get(this.modelEntity.getEntityName());
    }
}
