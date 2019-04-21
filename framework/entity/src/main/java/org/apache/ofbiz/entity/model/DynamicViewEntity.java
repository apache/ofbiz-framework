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
package org.apache.ofbiz.entity.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.model.ModelViewEntity.ComplexAliasMember;
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelAlias;
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelAliasAll;
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelMemberEntity;
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelViewLink;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 * This class is used for declaring Dynamic View Entities, to be used and thrown away.
 * A special method exists on the Delegator to accept a DynamicViewEntity instead
 * of an entity-name.
 *
 */
public class DynamicViewEntity {
    public static final String module = DynamicViewEntity.class.getName();

    /** The entity-name of the Entity */
    protected String entityName = "DynamicViewEntity";

    /** The package-name of the Entity */
    protected String packageName = "org.apache.ofbiz.dynamicview";

    /** The default-resource-name of the Entity, used with the getResource call to check for a value in a resource bundle */
    protected String defaultResourceName = "";

    /** The title for documentation purposes */
    protected String title = "";

    /** Contains member-entity alias name definitions: key is alias, value is ModelMemberEntity */
    protected Map<String, ModelMemberEntity> memberModelMemberEntities = new HashMap<>();

    /** List of alias-alls which act as a shortcut for easily pulling over member entity fields */
    protected List<ModelAliasAll> aliasAlls = new ArrayList<>();

    /** List of aliases with information in addition to what is in the standard field list */
    protected List<ModelAlias> aliases = new ArrayList<>();

    /** List of fields to group by */
    protected List<String> groupBy;

    /** List of view links to define how entities are connected (or "joined") */
    protected List<ModelViewLink> viewLinks = new ArrayList<>();

    /** relations defining relationships between this entity and other entities */
    protected List<ModelRelation> relations = new ArrayList<>();

    public DynamicViewEntity() {
    }

    public ModelViewEntity makeModelViewEntity(Delegator delegator) {
        ModelViewEntity modelViewEntity = new ModelViewEntity(this, delegator.getModelReader());
        return modelViewEntity;
    }

    public String getViewXml(String entityName) throws IOException {
        Document doc = UtilXml.makeEmptyXmlDocument();
        Element viewElement = getViewElement(doc, entityName);
        return UtilXml.writeXmlDocument(viewElement);
    }

    public Element getViewElement(Document doc, String entityName) {
        Element viewElement = doc.createElement("view-entity");
        viewElement.setAttribute("entity-name", entityName);

        for (ModelMemberEntity member: memberModelMemberEntities.values()) {
            Element memberElement = doc.createElement("member-entity");
            memberElement.setAttribute("entity-alias", member.getEntityAlias());
            memberElement.setAttribute("entity-name", member.getEntityName());
            viewElement.appendChild(memberElement);
        }
        for (ModelAliasAll aliasAll: aliasAlls) {
            Element aliasAllElement = doc.createElement("alias-all");
            aliasAllElement.setAttribute("entity-alias", aliasAll.getEntityAlias());
            if (UtilValidate.isNotEmpty(aliasAll.getPrefix())) aliasAllElement.setAttribute("prefix", aliasAll.getPrefix());
            if (aliasAll.getGroupBy()) aliasAllElement.setAttribute("group-by", "true");
            if (UtilValidate.isNotEmpty(aliasAll.getFunction())) aliasAllElement.setAttribute("function", aliasAll.getFunction());
            for (String excludeField: aliasAll) {
                Element excludeElement = doc.createElement("exclude");
                excludeElement.setAttribute("field", excludeField);
                aliasAllElement.appendChild(excludeElement);
            }
            viewElement.appendChild(aliasAllElement);
        }
        for (ModelAlias alias: aliases) {
            Element aliasElement = doc.createElement("alias");
            aliasElement.setAttribute("entity-alias", alias.getEntityAlias());
            aliasElement.setAttribute("name", alias.getName());
            if (!alias.getName().equals(alias.getField())) aliasElement.setAttribute("field", alias.getField());
            String colAlias = ModelUtil.dbNameToVarName(alias.getColAlias());
            if (!alias.getName().equals(colAlias)) aliasElement.setAttribute("col-alias", colAlias);
            if (alias.getIsPk() != null) aliasElement.setAttribute("prim-key", alias.getIsPk().toString());
            if (alias.getGroupBy()) aliasElement.setAttribute("group-by", "true");
            if (UtilValidate.isNotEmpty(alias.getFunction())) aliasElement.setAttribute("function", alias.getFunction());
            // TODO: description, complex-alias
            viewElement.appendChild(aliasElement);
        }
        for (ModelViewLink viewLink: viewLinks) {
            Element viewLinkElement = doc.createElement("view-link");
            viewLinkElement.setAttribute("entity-alias", viewLink.getEntityAlias());
            if (viewLink.isRelOptional()) viewLinkElement.setAttribute("rel-optional", "true");
            viewLinkElement.setAttribute("rel-entity-alias", viewLink.getRelEntityAlias());
            for (ModelKeyMap keyMap: viewLink) {
                Element keyMapElement = doc.createElement("key-map");
                keyMapElement.setAttribute("field-name", keyMap.getFieldName());
                if (!keyMap.getFieldName().equals(keyMap.getRelFieldName())) keyMapElement.setAttribute("rel-field-name", keyMap.getRelFieldName());
                viewLinkElement.appendChild(keyMapElement);
            }
            // TODO: conditions
            viewElement.appendChild(viewLinkElement);
        }
        for (ModelRelation relation: relations) {
            viewElement.appendChild(relation.toXmlElement(doc));
        }
        return viewElement;
    }

    public String getOneRealEntityName() {
        // return first entity name for memberModelMemberEntities Map
        if (this.memberModelMemberEntities.size() == 0) {
            return null;
        }

        ModelMemberEntity modelMemberEntity = this.memberModelMemberEntities.entrySet().iterator().next().getValue();
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

    public Iterator<Map.Entry<String, ModelMemberEntity>> getModelMemberEntitiesEntryIter() {
        return this.memberModelMemberEntities.entrySet().iterator();
    }

    /**
     * @deprecated use {@link #addAliasAll(String, String, Collection)}
     */
    @Deprecated
    public void addAliasAll(String entityAlias, String prefix) {
        addAliasAll(entityAlias, prefix, null);
    }

    public void addAliasAll(String entityAlias, String prefix, Collection<String> excludes) {
        ModelAliasAll aliasAll = new ModelAliasAll(entityAlias, prefix, false, null, null, excludes);
        this.aliasAlls.add(aliasAll);
    }

    public void addAllAliasAllsToList(List<ModelAliasAll> addList) {
        addList.addAll(this.aliasAlls);
    }

    public void addAlias(String entityAlias, String name) {
        this.addAlias(entityAlias, name, null, null, null, null, null);
    }

    /** Add an alias, full detail. All parameters can be null except entityAlias and name. */
    public void addAlias(String entityAlias, String name, String field, String colAlias, Boolean primKey, Boolean groupBy, String function) {
        addAlias(entityAlias, name, field, colAlias, primKey, groupBy, function, null, null);
    }

    public void addAlias(String entityAlias, String name, String field, String colAlias, Boolean primKey, Boolean groupBy, String function, ComplexAliasMember complexAliasMember) {
        addAlias(entityAlias, name, field, colAlias, primKey, groupBy, function, null, complexAliasMember);
    }

    public void addAlias(String entityAlias, String name, String field, String colAlias, Boolean primKey, Boolean groupBy, String function, String fieldSet, ComplexAliasMember complexAliasMember) {
        if (entityAlias == null && complexAliasMember == null) {
            throw new IllegalArgumentException("entityAlias cannot be null if this is not a complex alias in call to DynamicViewEntity.addAlias");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null in call to DynamicViewEntity.addAlias");
        }

        ModelAlias alias = new ModelAlias(entityAlias, name, field, colAlias, primKey, groupBy, function, fieldSet);
        if (complexAliasMember != null) {
            alias.setComplexAliasMember(complexAliasMember);
        }
        this.aliases.add(alias);
    }

    public void addAllAliasesToList(List<ModelAlias> addList) {
        addList.addAll(this.aliases);
    }

    public void addViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, List<ModelKeyMap> modelKeyMaps) {
        ModelViewLink modelViewLink = new ModelViewLink(entityAlias, relEntityAlias, relOptional, null, modelKeyMaps);
        this.viewLinks.add(modelViewLink);
    }

    public void addAllViewLinksToList(List<ModelViewLink> addList) {
        addList.addAll(this.viewLinks);
    }

    public void addRelation(String type, String title, String relEntityName, List<ModelKeyMap> modelKeyMaps) {
        ModelRelation relation = ModelRelation.create(null, null, type, title, relEntityName, null, modelKeyMaps, false);
        this.relations.add(relation);
    }

    public void addAllRelationsToList(List<ModelRelation> addList) {
        addList.addAll(this.relations);
    }

    public void setGroupBy(List<String> groupBy) {
        this.groupBy = groupBy;
    }

    public void addAllGroupByFieldsToList(List<String> addList) {
        if (groupBy != null) {
            addList.addAll(this.groupBy);
        }
    }
}
