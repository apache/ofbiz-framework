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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class extends ModelEntity and provides additional information appropriate to view entities
 */
public class ModelViewEntity extends ModelEntity {
    public static final String module = ModelViewEntity.class.getName();

    public static Map functionPrefixMap = FastMap.newInstance();
    static {
        functionPrefixMap.put("min", "MIN(");
        functionPrefixMap.put("max", "MAX(");
        functionPrefixMap.put("sum", "SUM(");
        functionPrefixMap.put("avg", "AVG(");
        functionPrefixMap.put("count", "COUNT(");
        functionPrefixMap.put("count-distinct", "COUNT(DISTINCT ");
        functionPrefixMap.put("upper", "UPPER(");
        functionPrefixMap.put("lower", "LOWER(");
    }

    /** Contains member-entity alias name definitions: key is alias, value is ModelMemberEntity */
    protected Map memberModelMemberEntities = FastMap.newInstance();

    /** A list of all ModelMemberEntity entries; this is mainly used to preserve the original order of member entities from the XML file */
    protected List allModelMemberEntities = FastList.newInstance();

    /** Contains member-entity ModelEntities: key is alias, value is ModelEntity; populated with fields */
    protected Map memberModelEntities = null;

    /** List of alias-alls which act as a shortcut for easily pulling over member entity fields */
    protected List aliasAlls = FastList.newInstance();

    /** List of aliases with information in addition to what is in the standard field list */
    protected List aliases = FastList.newInstance();

    /** List of view links to define how entities are connected (or "joined") */
    protected List viewLinks = FastList.newInstance();

    /** A List of the Field objects for the View Entity, one for each GROUP BY field */
    protected List groupBys = FastList.newInstance();

    protected Map conversions = FastMap.newInstance();

    public ModelViewEntity(ModelReader reader, Element entityElement, UtilTimer utilTimer, ModelInfo def) {
        super(reader, entityElement, def);

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before general/basic info");
        this.populateBasicInfo(entityElement);

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before \"member-entity\"s");
        List memberEntityList = UtilXml.childElementList(entityElement, "member-entity");
        Iterator memberEntityIter = memberEntityList.iterator();
        while (memberEntityIter.hasNext()) {
            Element memberEntityElement = (Element) memberEntityIter.next();
            String alias = UtilXml.checkEmpty(memberEntityElement.getAttribute("entity-alias"));
            String name = UtilXml.checkEmpty(memberEntityElement.getAttribute("entity-name"));
            if (name.length() <= 0 || alias.length() <= 0) {
                Debug.logError("[new ModelViewEntity] entity-alias or entity-name missing on member-entity element of the view-entity " + this.entityName, module);
            } else {
                ModelMemberEntity modelMemberEntity = new ModelMemberEntity(alias, name);
                this.addMemberModelMemberEntity(modelMemberEntity);
            }
        }

        // when reading aliases and alias-alls, just read them into the alias list, there will be a pass
        // after loading all entities to go back and fill in all of the ModelField entries
        List aliasAllList = UtilXml.childElementList(entityElement, "alias-all");
        Iterator aliasAllIter = aliasAllList.iterator();
        while (aliasAllIter.hasNext()) {
            Element aliasElement = (Element) aliasAllIter.next();
            ModelViewEntity.ModelAliasAll aliasAll = new ModelAliasAll(aliasElement);
            this.aliasAlls.add(aliasAll);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before aliases");
        List aliasList = UtilXml.childElementList(entityElement, "alias");
        Iterator aliasIter = aliasList.iterator();
        while (aliasIter.hasNext()) {
            Element aliasElement = (Element) aliasIter.next();
            ModelViewEntity.ModelAlias alias = new ModelAlias(aliasElement);
            this.aliases.add(alias);
        }
        
        List viewLinkList = UtilXml.childElementList(entityElement, "view-link");
        Iterator viewLinkIter = viewLinkList.iterator();
        while (viewLinkIter.hasNext()) {
            Element viewLinkElement = (Element) viewLinkIter.next();
            ModelViewLink viewLink = new ModelViewLink(viewLinkElement);
            this.addViewLink(viewLink);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);

        // before finishing, make sure the table name is null, this should help bring up errors early...
        this.tableName = null;
    }
    
    public ModelViewEntity(DynamicViewEntity dynamicViewEntity, ModelReader modelReader) {
        this.entityName = dynamicViewEntity.getEntityName();
        this.packageName = dynamicViewEntity.getPackageName();
        this.title = dynamicViewEntity.getTitle();
        this.defaultResourceName = dynamicViewEntity.getDefaultResourceName();
        
        // member-entities
        Iterator modelMemberEntitiesEntryIter = dynamicViewEntity.getModelMemberEntitiesEntryIter();
        while (modelMemberEntitiesEntryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) modelMemberEntitiesEntryIter.next();
            this.addMemberModelMemberEntity((ModelMemberEntity) entry.getValue());
        }
        
        // alias-alls
        dynamicViewEntity.addAllAliasAllsToList(this.aliasAlls);
        
        // aliases
        dynamicViewEntity.addAllAliasesToList(this.aliases);
        
        // view-links
        dynamicViewEntity.addAllViewLinksToList(this.viewLinks);
        
        // relations
        dynamicViewEntity.addAllRelationsToList(this.relations);
        
        // finalize stuff
        // note that this doesn't result in a call to populateReverseLinks because a DynamicViewEntity should never be cached anyway, and will blow up when attempting to make the reverse links to the DynamicViewEntity 
        this.populateFieldsBasic(modelReader);
    }

    public Map getMemberModelMemberEntities() {
        return this.memberModelMemberEntities;
    }

    public List getAllModelMemberEntities() {
        return this.allModelMemberEntities;
    }

    public ModelMemberEntity getMemberModelMemberEntity(String alias) {
        return (ModelMemberEntity) this.memberModelMemberEntities.get(alias);
    }

    public ModelEntity getMemberModelEntity(String alias) {
        if (this.memberModelEntities == null) {
            this.memberModelEntities = FastMap.newInstance();
            populateFields(this.getModelReader());
        }
        return (ModelEntity) this.memberModelEntities.get(alias);
    }

    public void addMemberModelMemberEntity(ModelMemberEntity modelMemberEntity) {
        this.memberModelMemberEntities.put(modelMemberEntity.getEntityAlias(), modelMemberEntity);
        this.allModelMemberEntities.add(modelMemberEntity);
    }

    public void removeMemberModelMemberEntity(String alias) {
        ModelMemberEntity modelMemberEntity = (ModelMemberEntity) this.memberModelMemberEntities.remove(alias);

        if (modelMemberEntity == null) return;
        this.allModelMemberEntities.remove(modelMemberEntity);
    }

    /** The col-name of the Field, the alias of the field if this is on a view-entity */
    public String getColNameOrAlias(String fieldName) {
        ModelField modelField = this.getField(fieldName);
        String fieldString = modelField.getColName();
        ModelViewEntity.ModelAlias alias = getAlias(fieldName);
        if (alias != null) {
            fieldString = alias.getColAlias();
        }
        return fieldString;
    }

    /** List of aliases with information in addition to what is in the standard field list */
    public ModelAlias getAlias(int index) {
        return (ModelAlias) this.aliases.get(index);
    }
    
    public ModelAlias getAlias(String name) {
        Iterator aliasIter = getAliasesIterator();
        while (aliasIter.hasNext()) {
            ModelAlias alias = (ModelAlias) aliasIter.next();
            if (alias.name.equals(name)) {
                return alias;
            }
        }
        return null;
    }

    public int getAliasesSize() {
        return this.aliases.size();
    }

    public Iterator getAliasesIterator() {
        return this.aliases.iterator();
    }

    public List getAliasesCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.aliases);
        return newList;
    }

    public List getGroupBysCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.groupBys);
        return newList;
    }

    /** List of view links to define how entities are connected (or "joined") */
    public ModelViewLink getViewLink(int index) {
        return (ModelViewLink) this.viewLinks.get(index);
    }

    public int getViewLinksSize() {
        return this.viewLinks.size();
    }

    public Iterator getViewLinksIterator() {
        return this.viewLinks.iterator();
    }

    public List getViewLinksCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.viewLinks);
        return newList;
    }

    public void addViewLink(ModelViewLink viewLink) {
        this.viewLinks.add(viewLink);
    }
    
    public String colNameString(List flds, String separator, String afterLast, boolean alias) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        Iterator fldsIt = flds.iterator();
        while (fldsIt.hasNext()) {
            ModelField field = (ModelField) fldsIt.next();
            returnString.append(field.colName);
            if (alias) {
                ModelAlias modelAlias = this.getAlias(field.name);
                if (modelAlias != null) {
                    returnString.append(" AS " + modelAlias.getColAlias());
                }
            }
            if (fldsIt.hasNext()) {
                returnString.append(separator);
            }
        }

        returnString.append(afterLast);
        return returnString.toString();
    }

    protected ModelEntity aliasedModelEntity = new ModelEntity();

    public ModelEntity getAliasedModelEntity() {
        return this.aliasedModelEntity;
    }

    public ModelEntity getAliasedEntity(String entityAlias, ModelReader modelReader) {
        ModelMemberEntity modelMemberEntity = (ModelMemberEntity) this.memberModelMemberEntities.get(entityAlias);
        if (modelMemberEntity == null) {
            Debug.logError("No member entity with alias " + entityAlias + " found in view-entity " + this.getEntityName() + "; this view-entity will NOT be usable...", module);
            return null;
        }

        String aliasedEntityName = modelMemberEntity.getEntityName();
        ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
        if (aliasedEntity == null) {
            Debug.logError("[ModelViewEntity.populateFields] ERROR: could not find ModelEntity for entity name: " + aliasedEntityName, module);
            return null;
        }
        
        return aliasedEntity;
    }
    
    public ModelField getAliasedField(ModelEntity aliasedEntity, String field, ModelReader modelReader) {
        ModelField aliasedField = aliasedEntity.getField(field);
        if (aliasedField == null) {
            Debug.logError("[ModelViewEntity.populateFields] ERROR: could not find ModelField for entity name: " + aliasedEntity.getEntityName() + " and field: " + field, module);
            return null;
        }
        return aliasedField;
    }
    
    public void populateFields(ModelReader modelReader) {
        populateFieldsBasic(modelReader);
        populateReverseLinks();
    }
    
    public void populateFieldsBasic(ModelReader modelReader) {
        if (this.memberModelEntities == null) {
            this.memberModelEntities = FastMap.newInstance();
        }

        Iterator meIter = memberModelMemberEntities.entrySet().iterator();
        while (meIter.hasNext()) {
            Map.Entry entry = (Map.Entry) meIter.next();

            ModelMemberEntity modelMemberEntity = (ModelMemberEntity) entry.getValue();
            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
            if (aliasedEntity == null) {
                continue;
            }
            memberModelEntities.put(entry.getKey(), aliasedEntity);
            Iterator aliasedFieldIterator = aliasedEntity.getFieldsIterator();
            while (aliasedFieldIterator.hasNext()) {
                ModelField aliasedModelField = (ModelField) aliasedFieldIterator.next();
                ModelField newModelField = new ModelField();
                for (int i = 0; i < aliasedModelField.getValidatorsSize(); i++) {
                    newModelField.addValidator(aliasedModelField.getValidator(i));
                }
                newModelField.setColName(modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getColName());
                newModelField.setName(modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getName());
                newModelField.setType(aliasedModelField.getType());
                newModelField.setIsPk(false);
                aliasedModelEntity.addField(newModelField);
            }
        }

        expandAllAliasAlls(modelReader);

        for (int i = 0; i < aliases.size(); i++) {
            ModelAlias alias = (ModelAlias) aliases.get(i);
            ModelField field = new ModelField();
            field.setModelEntity(this);
            field.name = alias.name;

            // if this is a groupBy field, add it to the groupBys list
            if (alias.groupBy) {
                this.groupBys.add(field);
            }

            // show a warning if function is specified and groupBy is true
            if (UtilValidate.isNotEmpty(alias.function) && alias.groupBy) {
                Debug.logWarning("The view-entity alias with name=" + alias.name + " has a function value and is specified as a group-by field; this may be an error, but is not necessarily.", module);
            }

            if (alias.isComplexAlias()) {
                // if this is a complex alias, make a complex column name...
                StringBuffer colNameBuffer = new StringBuffer();
                StringBuffer fieldTypeBuffer = new StringBuffer();
                alias.makeAliasColName(colNameBuffer, fieldTypeBuffer, this, modelReader);
                field.colName = colNameBuffer.toString();
                field.type = fieldTypeBuffer.toString();
                field.isPk = false;
            } else {
                ModelEntity aliasedEntity = getAliasedEntity(alias.entityAlias, modelReader);
                ModelField aliasedField = getAliasedField(aliasedEntity, alias.field, modelReader);
                if (aliasedField == null) {
                    Debug.logError("[ModelViewEntity.populateFields (" + this.getEntityName() + ")] ERROR: could not find ModelField for field name \"" +
                        alias.field + "\" on entity with name: " + aliasedEntity.getEntityName(), module);
                    continue;
                }

                if (alias.isPk != null) {
                    field.isPk = alias.isPk.booleanValue();
                } else {
                    field.isPk = aliasedField.isPk;
                }

                field.type = aliasedField.type;
                field.validators = aliasedField.validators;
                
                field.colName = alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName);
            }

            this.fields.add(field);
            if (field.isPk) {
                this.pks.add(field);
            } else {
                this.nopks.add(field);
            }
            
            if ("count".equals(alias.function) || "count-distinct".equals(alias.function)) {
                // if we have a "count" function we have to change the type
                field.type = "numeric";
            }

            if (UtilValidate.isNotEmpty(alias.function)) {
                String prefix = (String) functionPrefixMap.get(alias.function);
                if (prefix == null) {
                    Debug.logWarning("Specified alias function [" + alias.function + "] not valid; must be: min, max, sum, avg, count or count-distinct; using a column name with no function function", module);
                } else {
                    field.colName = prefix + field.colName + ")";
                }
            }
        }
    }
    
    protected ModelConversion getOrCreateModelConversion(String aliasName) {
        ModelEntity member = getMemberModelEntity(aliasName);
        if (member == null) {
            String errMsg = "No member found for aliasName - " + aliasName;
            Debug.logWarning(errMsg, module);
            throw new RuntimeException("Cannot create View Entity: " + errMsg);
        }
        
        Map aliasConversions = (Map) conversions.get(member.getEntityName());
        if (aliasConversions == null) {
            aliasConversions = FastMap.newInstance();
            conversions.put(member.getEntityName(), aliasConversions);
        }
        ModelConversion conversion = (ModelConversion) aliasConversions.get(aliasName);
        if (conversion == null) {
            conversion = new ModelConversion(aliasName, member);
            aliasConversions.put(aliasName, conversion);
        }
        return conversion;
    }

    public void populateReverseLinks() {
        Map containedModelFields = FastMap.newInstance();
        Iterator it = getAliasesIterator();
        while (it.hasNext()) {
            ModelViewEntity.ModelAlias alias = (ModelViewEntity.ModelAlias) it.next();
            if (alias.isComplexAlias()) {
                // TODO: conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly
                Debug.logWarning("Conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly, will not work for alias: " + alias.getName() + " of view-entity " + this.getEntityName(), module);
            } else {
                ModelConversion conversion = getOrCreateModelConversion(alias.getEntityAlias());
                conversion.addConversion(alias.getField(), alias.getName());
            }

            List aliases = (List) containedModelFields.get(alias.getField());
            if (aliases == null) {
                aliases = FastList.newInstance();
                containedModelFields.put(alias.getField(), aliases);
            }
            aliases.add(alias.getName());
        }

        it = getViewLinksIterator();
        while (it.hasNext()) {
            ModelViewEntity.ModelViewLink link = (ModelViewEntity.ModelViewLink) it.next();

            String leftAlias = link.getEntityAlias();
            String rightAlias = link.getRelEntityAlias();
            ModelConversion leftConversion = getOrCreateModelConversion(leftAlias);
            ModelConversion rightConversion = getOrCreateModelConversion(rightAlias);
            Iterator it2 = link.getKeyMapsIterator();
            Debug.logVerbose(leftAlias + "<->" + rightAlias, module);
            while (it2.hasNext()) {
                ModelKeyMap mkm = (ModelKeyMap) it2.next();
                String leftFieldName = mkm.getFieldName();
                String rightFieldName = mkm.getRelFieldName();
                rightConversion.addAllAliasConversions((List) containedModelFields.get(leftFieldName), rightFieldName);
                leftConversion.addAllAliasConversions((List) containedModelFields.get(rightFieldName), leftFieldName);
            }
        }
        it = conversions.entrySet().iterator();
        int[] currentIndex = new int[conversions.size()];
        int[] maxIndex = new int[conversions.size()];
        ModelConversion[][] allConversions = new ModelConversion[conversions.size()][];
        int i = 0;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Map aliasConversions = (Map) entry.getValue();
            currentIndex[i] = 0;
            maxIndex[i] = aliasConversions.size();
            allConversions[i] = new ModelConversion[aliasConversions.size()];
            Iterator it2 = aliasConversions.values().iterator();
            for (int j = 0; it2.hasNext() && j < aliasConversions.size(); j++) {
                allConversions[i][j] = (ModelConversion) it2.next();
            }
            i++;
        }
        int ptr = 0;
        ModelConversion[] currentConversions = new ModelConversion[conversions.size()];
        for (int j = 0, k; j < currentIndex.length; j++) {
            for (int l = 0; l < maxIndex[ j ]; l++ ) {
                while (true) {
                    for (i = 0, k = 0; i < currentIndex.length; i++) {
                        if (i == j && currentIndex[i] == l) continue;
                        currentConversions[k++] = allConversions[i][currentIndex[i]];
                    }
                    Debug.logVerbose(j + "," + l + ":" + Arrays.asList(currentConversions), module);
                    while (ptr < currentIndex.length && ++currentIndex[ptr] == maxIndex[ptr]) {
                        currentIndex[ptr] = 0;
                        ptr++;
                    }
                    if (ptr == currentIndex.length) break;
                    ptr = 0;
                }
            }
        }
        Debug.logVerbose(this + ":" + conversions, module);
    }

    public List convert(String fromEntityName, Map data) {
        Map foo = (Map) conversions.get(fromEntityName);
        if (foo == null) return null;
        Iterator it = foo.values().iterator();
        List values = FastList.newInstance();
        while (it.hasNext()) {
            ModelConversion conversion = (ModelConversion) it.next();
            values.add(conversion.convert(data));
        }
        return values;
    }

    /**
     * Go through all aliasAlls and create an alias for each field of each member entity
     */
    private void expandAllAliasAlls(ModelReader modelReader) {
        Iterator aliasAllIter = aliasAlls.iterator();
        while (aliasAllIter.hasNext()) {
            ModelAliasAll aliasAll = (ModelAliasAll) aliasAllIter.next();
            String prefix = aliasAll.getPrefix();

            ModelMemberEntity modelMemberEntity = (ModelMemberEntity) memberModelMemberEntities.get(aliasAll.getEntityAlias());
            if (modelMemberEntity == null) {
                Debug.logError("Member entity referred to in alias-all not found, ignoring: " + aliasAll.getEntityAlias(), module);
                continue;
            }

            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
            if (aliasedEntity == null) {
                Debug.logError("Entity referred to in member-entity " + aliasAll.getEntityAlias() + " not found, ignoring: " + aliasedEntityName, module);
                continue;
            }

            List entFieldList = aliasedEntity.getAllFieldNames();
            if (entFieldList == null) {
                Debug.logError("Entity referred to in member-entity " + aliasAll.getEntityAlias() + " has no fields, ignoring: " + aliasedEntityName, module);
                continue;
            }

            Iterator fieldnamesIterator = entFieldList.iterator();
            while (fieldnamesIterator.hasNext()) {
                // now merge the lists, leaving out any that duplicate an existing alias name
                String fieldName = (String) fieldnamesIterator.next();
                String aliasName = fieldName;
                ModelField modelField = aliasedEntity.getField(fieldName);
                if (modelField.getIsAutoCreatedInternal()) {
                    // never auto-alias these
                    continue;
                }
                if (aliasAll.shouldExclude(fieldName)) {
                    // if specified as excluded, leave it out
                    continue;
                }
                
                if (UtilValidate.isNotEmpty(prefix)) {
                    StringBuffer newAliasBuffer = new StringBuffer(prefix);
                    //make sure the first letter is uppercase to delineate the field name
                    newAliasBuffer.append(Character.toUpperCase(aliasName.charAt(0)));
                    newAliasBuffer.append(aliasName.substring(1));
                    aliasName = newAliasBuffer.toString();
                }
                
                ModelAlias existingAlias = this.getAlias(aliasName);
                if (existingAlias != null) {
                    //log differently if this is part of a view-link key-map because that is a common case when a field will be auto-expanded multiple times
                    boolean isInViewLink = false;
                    Iterator viewLinkIter = this.getViewLinksIterator();
                    while (viewLinkIter.hasNext() && !isInViewLink) {
                        ModelViewLink modelViewLink = (ModelViewLink) viewLinkIter.next();
                        boolean isRel = false;
                        if (modelViewLink.getRelEntityAlias().equals(aliasAll.getEntityAlias())) {
                            isRel = true;
                        } else if (!modelViewLink.getEntityAlias().equals(aliasAll.getEntityAlias())) {
                            // not the rel-entity-alias or the entity-alias, so move along
                            continue;
                        }
                        Iterator keyMapIter = modelViewLink.getKeyMapsIterator();
                        while (keyMapIter.hasNext() && !isInViewLink) {
                            ModelKeyMap modelKeyMap = (ModelKeyMap) keyMapIter.next();
                            if (!isRel && modelKeyMap.getFieldName().equals(fieldName)) {
                                isInViewLink = true;
                            } else if (isRel && modelKeyMap.getRelFieldName().equals(fieldName)) {
                                isInViewLink = true;
                            }
                        }
                    }
                    
                    //already exists, oh well... probably an override, but log just in case
                    String warnMsg = "Throwing out field alias in view entity " + this.getEntityName() + " because one already exists with the alias name [" + aliasName + "] and field name [" + modelMemberEntity.getEntityAlias() + "(" + aliasedEntity.getEntityName() + ")." + fieldName + "], existing field name is [" + existingAlias.getEntityAlias() + "." + existingAlias.getField() + "]";
                    if (isInViewLink) {
                        Debug.logVerbose(warnMsg, module);
                    } else {
                        Debug.logInfo(warnMsg, module);
                    }
                    continue;
                }
                
                ModelAlias expandedAlias = new ModelAlias();
                expandedAlias.name = aliasName;
                expandedAlias.field = fieldName;
                expandedAlias.entityAlias = aliasAll.getEntityAlias();
                expandedAlias.isFromAliasAll = true;
                expandedAlias.colAlias = ModelUtil.javaNameToDbName(UtilXml.checkEmpty(expandedAlias.name));
                aliases.add(expandedAlias);
            }
        }
    }

    public String toString() {
        return "ModelViewEntity[" + getEntityName() + "]";
    }

    public static class ModelMemberEntity implements Serializable {
        protected String entityAlias = "";
        protected String entityName = "";

        public ModelMemberEntity(String entityAlias, String entityName) {
            this.entityAlias = entityAlias;
            this.entityName = entityName;
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getEntityName() {
            return this.entityName;
        }
    }

    public static class ModelAliasAll implements Serializable {
        protected String entityAlias = "";
        protected String prefix = "";
        protected Set fieldsToExclude = null;

        protected ModelAliasAll() {}

        public ModelAliasAll(String entityAlias, String prefix) {
            this.entityAlias = entityAlias;
            this.prefix = prefix;
        }

        public ModelAliasAll(Element aliasAllElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasAllElement.getAttribute("entity-alias"));
            this.prefix = UtilXml.checkEmpty(aliasAllElement.getAttribute("prefix"));
            
            List excludes = UtilXml.childElementList(aliasAllElement, "exclude");
            if (excludes != null && excludes.size() > 0) {
                this.fieldsToExclude = new HashSet();
                Iterator excludeIter = excludes.iterator();
                while (excludeIter.hasNext()) {
                    Element excludeElement = (Element) excludeIter.next();
                    this.fieldsToExclude.add(excludeElement.getAttribute("field"));
                }
            }
            
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public boolean shouldExclude(String fieldName) {
            if (this.fieldsToExclude == null) {
                return false;
            } else {
                return this.fieldsToExclude.contains(fieldName);
            }
        }
    }

    public static class ModelAlias implements Serializable {
        protected String entityAlias = "";
        protected String name = "";
        protected String field = "";
        protected String colAlias = "";
        // this is a Boolean object for a tri-state: null, true or false
        protected Boolean isPk = null;
        protected boolean groupBy = false;
        // is specified this alias is a calculated value; can be: min, max, sum, avg, count, count-distinct
        protected String function = null;
        protected boolean isFromAliasAll = false;
        protected ComplexAliasMember complexAliasMember = null;

        protected ModelAlias() {}

        public ModelAlias(Element aliasElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasElement.getAttribute("entity-alias"));
            this.name = UtilXml.checkEmpty(aliasElement.getAttribute("name"));
            this.field = UtilXml.checkEmpty(aliasElement.getAttribute("field"), this.name);
            this.colAlias = UtilXml.checkEmpty(aliasElement.getAttribute("col-alias"), ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name)));
            String primKeyValue = UtilXml.checkEmpty(aliasElement.getAttribute("prim-key"));

            if (UtilValidate.isNotEmpty(primKeyValue)) {
                this.isPk = new Boolean("true".equals(primKeyValue));
            } else {
                this.isPk = null;
            }
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasElement.getAttribute("function"));
            
            Element complexAliasElement = UtilXml.firstChildElement(aliasElement, "complex-alias");
            if (complexAliasElement != null) {
                complexAliasMember = new ComplexAlias(complexAliasElement);
            }
        }
        
        public ModelAlias(String entityAlias, String name, String field, String colAlias, Boolean isPk, Boolean groupBy, String function) {
            this.entityAlias = entityAlias;
            this.name = name;
            this.field = UtilXml.checkEmpty(field, this.name);
            this.colAlias = UtilXml.checkEmpty(colAlias, ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name)));
            this.isPk = isPk;
            if (groupBy != null) {
                this.groupBy = groupBy.booleanValue();
            } else {
                this.groupBy = false;
            }
            this.function = function;
        }
        
        public void setComplexAliasMember(ComplexAliasMember complexAliasMember) {
            this.complexAliasMember = complexAliasMember;
        }
        
        public boolean isComplexAlias() {
            return complexAliasMember != null;
        }
        
        public void makeAliasColName(StringBuffer colNameBuffer, StringBuffer fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            if (complexAliasMember != null) {
                complexAliasMember.makeAliasColName(colNameBuffer, fieldTypeBuffer, modelViewEntity, modelReader);
            }
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getName() {
            return this.name;
        }
        
        public String getColAlias() {
            return this.colAlias;
        }

        public String getField() {
            return this.field;
        }

        public Boolean getIsPk() {
            return this.isPk;
        }

        public boolean getGroupBy() {
            return this.groupBy;
        }

        public String getFunction() {
            return this.function;
        }

        public boolean getIsFromAliasAll() {
            return this.isFromAliasAll;
        }
    }

    public static interface ComplexAliasMember extends Serializable {
        public void makeAliasColName(StringBuffer colNameBuffer, StringBuffer fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader);
    }
    
    public static class ComplexAlias implements ComplexAliasMember {
        protected List complexAliasMembers = FastList.newInstance();
        protected String operator;
        
        public ComplexAlias(String operator) {
            this.operator = operator;
        }
        
        public ComplexAlias(Element complexAliasElement) {
            this.operator = complexAliasElement.getAttribute("operator");
            // handle all complex-alias and complex-alias-field sub-elements
            List subElements = UtilXml.childElementList(complexAliasElement);
            Iterator subElementIter = subElements.iterator();
            while (subElementIter.hasNext()) {
                Element subElement = (Element) subElementIter.next();
                String nodeName = subElement.getNodeName();
                if ("complex-alias".equals(nodeName)) {
                    this.addComplexAliasMember(new ComplexAlias(subElement));
                } else if ("complex-alias-field".equals(nodeName)) {
                    this.addComplexAliasMember(new ComplexAliasField(subElement));
                }
            }
        }
        
        public void addComplexAliasMember(ComplexAliasMember complexAliasMember) {
            this.complexAliasMembers.add(complexAliasMember);
        }
        
        public void makeAliasColName(StringBuffer colNameBuffer, StringBuffer fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            if (complexAliasMembers.size() == 0) {
                return;
            } else if (complexAliasMembers.size() == 1) {
                ComplexAliasMember complexAliasMember = (ComplexAliasMember) complexAliasMembers.iterator().next();
                complexAliasMember.makeAliasColName(colNameBuffer, fieldTypeBuffer, modelViewEntity, modelReader);
            } else {
                colNameBuffer.append('(');
                Iterator complexAliasMemberIter = complexAliasMembers.iterator();
                while (complexAliasMemberIter.hasNext()) {
                    ComplexAliasMember complexAliasMember = (ComplexAliasMember) complexAliasMemberIter.next();
                    complexAliasMember.makeAliasColName(colNameBuffer, fieldTypeBuffer, modelViewEntity, modelReader);
                    if (complexAliasMemberIter.hasNext()) {
                        colNameBuffer.append(' ');
                        colNameBuffer.append(this.operator);
                        colNameBuffer.append(' ');
                    }
                }
                colNameBuffer.append(')');
            }
        }
    }
    
    public static class ComplexAliasField implements ComplexAliasMember {
        protected String entityAlias = "";
        protected String field = "";
        protected String defaultValue = null;
        protected String function = null;
        
        public ComplexAliasField(Element complexAliasFieldElement) {
            this.entityAlias = complexAliasFieldElement.getAttribute("entity-alias");
            this.field = complexAliasFieldElement.getAttribute("field");
            this.defaultValue = complexAliasFieldElement.getAttribute("default-value");
            this.function = complexAliasFieldElement.getAttribute("function");
        }

        public ComplexAliasField(String entityAlias, String field, String defaultValue, String function) {
            this.entityAlias = entityAlias;
            this.field = field;
            this.defaultValue = defaultValue;
            this.function = function;
        }

        /**
         * Make the alias as follows: function(coalesce(entityAlias.field, defaultValue))
         */
        public void makeAliasColName(StringBuffer colNameBuffer, StringBuffer fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            ModelEntity modelEntity = modelViewEntity.getAliasedEntity(entityAlias, modelReader);
            ModelField modelField = modelViewEntity.getAliasedField(modelEntity, field, modelReader);
            
            String colName = entityAlias + "." + modelField.getColName();
            
            if (UtilValidate.isNotEmpty(defaultValue)) {
                colName = "COALESCE(" + colName + "," + defaultValue + ")";
            }

            if (UtilValidate.isNotEmpty(function)) {
                String prefix = (String) functionPrefixMap.get(function);
                if (prefix == null) {
                    Debug.logWarning("Specified alias function [" + function + "] not valid; must be: min, max, sum, avg, count or count-distinct; using a column name with no function function", module);
                } else {
                    colName = prefix + colName + ")";
                }
            }
            
            colNameBuffer.append(colName);
            
            //set fieldTypeBuffer if not already set
            if (fieldTypeBuffer.length() == 0) {
                fieldTypeBuffer.append(modelField.type);
            }
        }
    }

    public static class ModelViewLink implements Serializable {
        protected String entityAlias = "";
        protected String relEntityAlias = "";
        protected boolean relOptional = false;
        protected List keyMaps = FastList.newInstance();

        protected ModelViewLink() {}

        public ModelViewLink(Element viewLinkElement) {
            this.entityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("entity-alias"));
            this.relEntityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("rel-entity-alias"));
            // if anything but true will be false; ie defaults to false
            this.relOptional = "true".equals(viewLinkElement.getAttribute("rel-optional"));

            NodeList keyMapList = viewLinkElement.getElementsByTagName("key-map");
            for (int j = 0; j < keyMapList.getLength(); j++) {
                Element keyMapElement = (Element) keyMapList.item(j);
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                if (keyMap != null) keyMaps.add(keyMap);
            }
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, List keyMaps) {
            this.entityAlias = entityAlias;
            this.relEntityAlias = relEntityAlias;
            if (relOptional != null) {
                this.relOptional = relOptional.booleanValue();
            }
            this.keyMaps.addAll(keyMaps);
        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getRelEntityAlias() {
            return this.relEntityAlias;
        }

        public boolean isRelOptional() {
            return this.relOptional;
        }

        public ModelKeyMap getKeyMap(int index) {
            return (ModelKeyMap) this.keyMaps.get(index);
        }

        public int getKeyMapsSize() {
            return this.keyMaps.size();
        }

        public Iterator getKeyMapsIterator() {
            return this.keyMaps.iterator();
        }

        public List getKeyMapsCopy() {
            List newList = FastList.newInstance();
            newList.addAll(this.keyMaps);
            return newList;
        }
    }

    public class ModelConversion implements Serializable {
        protected String aliasName;
        protected ModelEntity fromModelEntity;
        protected Map fieldMap = FastMap.newInstance();
        protected Set wildcards = new HashSet();

        public ModelConversion(String aliasName, ModelEntity fromModelEntity) {
            this.aliasName = aliasName;
            this.fromModelEntity = fromModelEntity;
            Iterator it = getFieldsIterator();
            while (it.hasNext()) {
                ModelField field = (ModelField) it.next();
                wildcards.add(field.getName());
            }
        }

        public int hashCode() {
            return fromModelEntity.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ModelConversion)) return false;
            ModelConversion other = (ModelConversion) obj;
            return fromModelEntity.equals(other.fromModelEntity);
        }

        public void addConversion(String fromFieldName, String toFieldName) {
            wildcards.remove(toFieldName);
            fieldMap.put(fromFieldName, toFieldName);
        }

        public String toString() {
            //return fromModelEntity.getEntityName() + ":" + fieldMap + ":" + wildcards;
            return aliasName + "(" + fromModelEntity.getEntityName() + ")";
        }

        public Map convert(Map values) {
            Map newValues = FastMap.newInstance();
            Iterator it = fieldMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                newValues.put(entry.getValue(), values.get((String) entry.getKey()));
            }
            it = wildcards.iterator();
            while (it.hasNext()) {
                newValues.put((String) it.next(), EntityOperator.WILDCARD);
            }
            return newValues;
        }

        public void addAllAliasConversions(List aliases, String fieldName) {
            if (aliases != null) {
                Iterator it3 = aliases.iterator();
                while (it3.hasNext()) {
                    addConversion(fieldName, (String) it3.next());
                }
            }
        }
    }
}
