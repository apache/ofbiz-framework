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
package org.ofbiz.entity.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasMember;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;
import org.ofbiz.entity.model.ModelViewEntity.ModelAliasAll;
import org.ofbiz.entity.model.ModelViewEntity.ModelMemberEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelViewLink;
/**
 * This class is used for declaring Dynamic View Entities, to be used and thrown away.
 * A special method exists on the GenericDelegator to accept a DynamicViewEntity instead
 * of an entity-name.
 *
 */
public class DynamicViewEntity {
    public static final String module = DynamicViewEntity.class.getName();

    /** The entity-name of the Entity */
    protected String entityName = "DynamicViewEntity";

    /** The package-name of the Entity */
    protected String packageName = "org.ofbiz.dynamicview";

    /** The default-resource-name of the Entity, used with the getResource call to check for a value in a resource bundle */
    protected String defaultResourceName = "";

    /** The title for documentation purposes */
    protected String title = "";

    /** Contains member-entity alias name definitions: key is alias, value is ModelMemberEntity */
    protected Map memberModelMemberEntities = new HashMap();
    
    /** List of alias-alls which act as a shortcut for easily pulling over member entity fields */
    protected List aliasAlls = new ArrayList();

    /** List of aliases with information in addition to what is in the standard field list */
    protected List aliases = new ArrayList();

    /** List of view links to define how entities are connected (or "joined") */
    protected List viewLinks = new ArrayList();
    
    /** relations defining relationships between this entity and other entities */
    protected List relations = new ArrayList();
    
    public DynamicViewEntity() {
    }
    
    public ModelViewEntity makeModelViewEntity(GenericDelegator delegator) {
        ModelViewEntity modelViewEntity = new ModelViewEntity(this, delegator.getModelReader());
        return modelViewEntity;
    }
    
    public String getOneRealEntityName() {
        // return first entity name for memberModelMemberEntities Map
        if (this.memberModelMemberEntities.size() == 0) {
            return null;
        }
        
        ModelMemberEntity modelMemberEntity = (ModelMemberEntity) ((Map.Entry) this.memberModelMemberEntities.entrySet().iterator().next()).getValue();
        return modelMemberEntity.getEntityName();
    }
    
    /** Getter for property entityName.
     * @return Value of property entityName.
     *
     */
    public String getEntityName() {
        return entityName;
    }
    
    /** Setter for property entityName.
     * @param entityName New value of property entityName.
     *
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    /** Getter for property packageName.
     * @return Value of property packageName.
     *
     */
    public String getPackageName() {
        return packageName;
    }
    
    /** Setter for property packageName.
     * @param packageName New value of property packageName.
     *
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /** Getter for property defaultResourceName.
     * @return Value of property defaultResourceName.
     *
     */
    public String getDefaultResourceName() {
        return defaultResourceName;
    }
    
    /** Setter for property defaultResourceName.
     * @param defaultResourceName New value of property defaultResourceName.
     *
     */
    public void setDefaultResourceName(String defaultResourceName) {
        this.defaultResourceName = defaultResourceName;
    }
    
    /** Getter for property title.
     * @return Value of property title.
     *
     */
    public String getTitle() {
        return title;
    }
    
    /** Setter for property title.
     * @param title New value of property title.
     *
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public void addMemberEntity(String entityAlias, String entityName) {
        ModelMemberEntity modelMemberEntity = new ModelMemberEntity(entityAlias, entityName);
        this.memberModelMemberEntities.put(entityAlias, modelMemberEntity);
    }
    
    public Iterator getModelMemberEntitiesEntryIter() {
        return this.memberModelMemberEntities.entrySet().iterator();
    }
    
    public void addAliasAll(String entityAlias, String prefix) {
        ModelAliasAll aliasAll = new ModelAliasAll(entityAlias, prefix);
        this.aliasAlls.add(aliasAll);
    }
    
    public void addAllAliasAllsToList(List addList) {
        addList.addAll(this.aliasAlls);
    }
    
    public void addAlias(String entityAlias, String name) {
        this.addAlias(entityAlias, name, null, null, null, null, null);
    }

    /** Add an alias, full detail. All parameters can be null except entityAlias and name. */
    public void addAlias(String entityAlias, String name, String field, String colAlias, Boolean primKey, Boolean groupBy, String function) {
        addAlias(entityAlias, name, field, colAlias, primKey, groupBy, function, null);
    }
    
    public void addAlias(String entityAlias, String name, String field, String colAlias, Boolean primKey, Boolean groupBy, String function, ComplexAliasMember complexAliasMember) {
        if (entityAlias == null && complexAliasMember == null) {
            throw new IllegalArgumentException("entityAlias cannot be null if this is not a complex alias in call to DynamicViewEntity.addAlias");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null in call to DynamicViewEntity.addAlias");
        }
        
        ModelAlias alias = new ModelAlias(entityAlias, name, field, colAlias, primKey, groupBy, function);
        if (complexAliasMember != null) {
            alias.setComplexAliasMember(complexAliasMember);
        }
        this.aliases.add(alias);
    }
    
    public void addAllAliasesToList(List addList) {
        addList.addAll(this.aliases);
    }
    
    public void addViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, List modelKeyMaps) {
        ModelViewLink modelViewLink = new ModelViewLink(entityAlias, relEntityAlias, relOptional, modelKeyMaps);
        this.viewLinks.add(modelViewLink);
    }
    
    public void addAllViewLinksToList(List addList) {
        addList.addAll(this.viewLinks);
    }
    
    public void addRelation(String type, String title, String relEntityName, List modelKeyMaps) {
        ModelRelation relation = new ModelRelation(type, title, relEntityName, null, modelKeyMaps);
        this.relations.add(relation);
    }
    
    public void addAllRelationsToList(List addList) {
        addList.addAll(this.relations);
    }
}
