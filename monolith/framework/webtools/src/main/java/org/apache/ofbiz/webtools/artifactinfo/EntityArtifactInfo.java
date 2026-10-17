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
 * The type Entity artifact info.
 */
public class EntityArtifactInfo extends ArtifactInfoBase {
    private ModelEntity modelEntity;
    private Set<EntityArtifactInfo> entitiesRelatedOne = new TreeSet<>();
    private Set<EntityArtifactInfo> entitiesRelatedMany = new TreeSet<>();

    public EntityArtifactInfo(String entityName, ArtifactInfoFactory aif) throws GenericEntityException {
        super(aif);
        this.modelEntity = this.getAif().getModelEntity(entityName);
    }

    /**
     * Populate all.
     * @throws GeneralException the general exception
     */
    public void populateAll() throws GeneralException {
        List<ModelRelation> relationOneList = modelEntity.getRelationsOneList();
        for (ModelRelation relationOne: relationOneList) {
            this.entitiesRelatedOne.add(this.getAif().getEntityArtifactInfo(relationOne.getRelEntityName()));
        }

        List<ModelRelation> relationManyList = modelEntity.getRelationsManyList();
        for (ModelRelation relationMany: relationManyList) {
            this.entitiesRelatedMany.add(this.getAif().getEntityArtifactInfo(relationMany.getRelEntityName()));
        }
    }

    /**
     * Gets model entity.
     * @return the model entity
     */
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
        return ArtifactInfoFactory.ENTITY_INFO_TYPE_ID;
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

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets entities related one.
     * @return the entities related one
     */
    public Set<EntityArtifactInfo> getEntitiesRelatedOne() {
        return this.entitiesRelatedOne;
    }

    /**
     * Gets entities related many.
     * @return the entities related many
     */
    public Set<EntityArtifactInfo> getEntitiesRelatedMany() {
        return this.entitiesRelatedMany;
    }

    /** Get the Services that use this Entity */
    public Set<ServiceArtifactInfo> getServicesUsingEntity() {
        return this.getAif().getAllServiceInfosReferringToEntityName().get(this.modelEntity.getEntityName());
    }

    /** Get the Services called by Entity ECA */
    public Set<ServiceArtifactInfo> getServicesCalledByEntityEca() {
        Set<ServiceArtifactInfo> serviceSet = new HashSet<>();
        // TODO: implement this
        return serviceSet;
    }

    /**
     * Gets entity eca rules.
     * @return the entity eca rules
     */
    public Set<EntityEcaRule> getEntityEcaRules() {
        Set<EntityEcaRule> eecaSet = new HashSet<>();
        // TODO: implement this
        return eecaSet;
    }

    /**
     * Gets forms using entity.
     * @return the forms using entity
     */
    public Set<FormWidgetArtifactInfo> getFormsUsingEntity() {
        return this.getAif().getAllFormInfosReferringToEntityName().get(this.modelEntity.getEntityName());
    }

    /**
     * Gets screens using entity.
     * @return the screens using entity
     */
    public Set<ScreenWidgetArtifactInfo> getScreensUsingEntity() {
        return this.getAif().getAllScreenInfosReferringToEntityName().get(this.modelEntity.getEntityName());
    }
}
