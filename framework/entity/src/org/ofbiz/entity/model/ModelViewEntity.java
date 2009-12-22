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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionValue;
import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class extends ModelEntity and provides additional information appropriate to view entities
 */
@SuppressWarnings("serial")
public class ModelViewEntity extends ModelEntity {
    public static final String module = ModelViewEntity.class.getName();

    public static Map<String, String> functionPrefixMap = FastMap.newInstance();
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
    protected Map<String, ModelMemberEntity> memberModelMemberEntities = FastMap.newInstance();

    /** A list of all ModelMemberEntity entries; this is mainly used to preserve the original order of member entities from the XML file */
    protected List<ModelMemberEntity> allModelMemberEntities = FastList.newInstance();

    /** Contains member-entity ModelEntities: key is alias, value is ModelEntity; populated with fields */
    protected Map<String, ModelEntity> memberModelEntities = null;

    /** List of alias-alls which act as a shortcut for easily pulling over member entity fields */
    protected List<ModelAliasAll> aliasAlls = FastList.newInstance();

    /** List of aliases with information in addition to what is in the standard field list */
    protected List<ModelAlias> aliases = FastList.newInstance();

    /** List of view links to define how entities are connected (or "joined") */
    protected List<ModelViewLink> viewLinks = FastList.newInstance();

    /** A List of the Field objects for the View Entity, one for each GROUP BY field */
    protected List<ModelField> groupBys = FastList.newInstance();

    /** List of field names to group by */
    protected List<String> groupByFields = FastList.newInstance();

    protected Map<String, Map<String, ModelConversion>> conversions = FastMap.newInstance();

    protected ViewEntityCondition viewEntityCondition = null;

    public ModelViewEntity(ModelReader reader, Element entityElement, UtilTimer utilTimer, ModelInfo def) {
        super(reader, entityElement, def);

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before general/basic info");
        this.populateBasicInfo(entityElement);

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before \"member-entity\"s");
        for (Element memberEntityElement: UtilXml.childElementList(entityElement, "member-entity")) {
            String alias = UtilXml.checkEmpty(memberEntityElement.getAttribute("entity-alias")).intern();
            String name = UtilXml.checkEmpty(memberEntityElement.getAttribute("entity-name")).intern();
            if (name.length() <= 0 || alias.length() <= 0) {
                Debug.logError("[new ModelViewEntity] entity-alias or entity-name missing on member-entity element of the view-entity " + this.entityName, module);
            } else {
                ModelMemberEntity modelMemberEntity = new ModelMemberEntity(alias, name);
                this.addMemberModelMemberEntity(modelMemberEntity);
            }
        }

        // when reading aliases and alias-alls, just read them into the alias list, there will be a pass
        // after loading all entities to go back and fill in all of the ModelField entries
        for (Element aliasElement: UtilXml.childElementList(entityElement, "alias-all")) {
            ModelViewEntity.ModelAliasAll aliasAll = new ModelAliasAll(aliasElement);
            this.aliasAlls.add(aliasAll);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelViewEntity: before aliases");
        for (Element aliasElement: UtilXml.childElementList(entityElement, "alias")) {
            ModelViewEntity.ModelAlias alias = new ModelAlias(aliasElement);
            this.aliases.add(alias);
        }

        for (Element viewLinkElement: UtilXml.childElementList(entityElement, "view-link")) {
            ModelViewLink viewLink = new ModelViewLink(this, viewLinkElement);
            this.addViewLink(viewLink);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);

        Element entityConditionElement = UtilXml.firstChildElement(entityElement, "entity-condition");
        if (entityConditionElement != null) {
            this.viewEntityCondition = new ViewEntityCondition(this, null, entityConditionElement);
        }

        // before finishing, make sure the table name is null, this should help bring up errors early...
        this.tableName = null;
    }

    public ModelViewEntity(DynamicViewEntity dynamicViewEntity, ModelReader modelReader) {
        this.entityName = dynamicViewEntity.getEntityName();
        this.packageName = dynamicViewEntity.getPackageName();
        this.title = dynamicViewEntity.getTitle();
        this.defaultResourceName = dynamicViewEntity.getDefaultResourceName();

        // member-entities
        Iterator<Map.Entry<String, ModelMemberEntity>> modelMemberEntitiesEntryIter = dynamicViewEntity.getModelMemberEntitiesEntryIter();
        while (modelMemberEntitiesEntryIter.hasNext()) {
            Map.Entry<String, ModelMemberEntity> entry = modelMemberEntitiesEntryIter.next();
            this.addMemberModelMemberEntity(entry.getValue());
        }

        // alias-alls
        dynamicViewEntity.addAllAliasAllsToList(this.aliasAlls);

        // aliases
        dynamicViewEntity.addAllAliasesToList(this.aliases);

        // view-links
        dynamicViewEntity.addAllViewLinksToList(this.viewLinks);

        // relations
        dynamicViewEntity.addAllRelationsToList(this.relations);

        dynamicViewEntity.addAllGroupByFieldsToList(this.groupByFields);

        // finalize stuff
        // note that this doesn't result in a call to populateReverseLinks because a DynamicViewEntity should never be cached anyway, and will blow up when attempting to make the reverse links to the DynamicViewEntity
        this.populateFieldsBasic(modelReader);
    }

    public Map<String, ModelMemberEntity> getMemberModelMemberEntities() {
        return this.memberModelMemberEntities;
    }

    public List<ModelMemberEntity> getAllModelMemberEntities() {
        return this.allModelMemberEntities;
    }

    public ModelMemberEntity getMemberModelMemberEntity(String alias) {
        return this.memberModelMemberEntities.get(alias);
    }

    public ModelEntity getMemberModelEntity(String alias) {
        if (this.memberModelEntities == null) {
            this.memberModelEntities = FastMap.newInstance();
            populateFields(this.getModelReader());
        }
        return this.memberModelEntities.get(alias);
    }

    public void addMemberModelMemberEntity(ModelMemberEntity modelMemberEntity) {
        this.memberModelMemberEntities.put(modelMemberEntity.getEntityAlias(), modelMemberEntity);
        this.allModelMemberEntities.add(modelMemberEntity);
    }

    public void removeMemberModelMemberEntity(String alias) {
        ModelMemberEntity modelMemberEntity = this.memberModelMemberEntities.remove(alias);

        if (modelMemberEntity == null) return;
        this.allModelMemberEntities.remove(modelMemberEntity);
    }

    /** The col-name of the Field, the alias of the field if this is on a view-entity */
    @Override
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
        return this.aliases.get(index);
    }

    public ModelAlias getAlias(String name) {
        Iterator<ModelAlias> aliasIter = getAliasesIterator();
        while (aliasIter.hasNext()) {
            ModelAlias alias = aliasIter.next();
            if (alias.name.equals(name)) {
                return alias;
            }
        }
        return null;
    }

    public int getAliasesSize() {
        return this.aliases.size();
    }

    public Iterator<ModelAlias> getAliasesIterator() {
        return this.aliases.iterator();
    }

    public List<ModelAlias> getAliasesCopy() {
        List<ModelAlias> newList = FastList.newInstance();
        newList.addAll(this.aliases);
        return newList;
    }

    public List<ModelField> getGroupBysCopy() {
        return getGroupBysCopy(null);
    }

    public List<ModelField> getGroupBysCopy(List<ModelField> selectFields) {
        List<ModelField> newList = FastList.newInstance();
        if (UtilValidate.isEmpty(selectFields)) {
            newList.addAll(this.groupBys);
        } else {
            for (ModelField groupByField: this.groupBys) {
                if (selectFields.contains(groupByField)) {
                    newList.add(groupByField);
                }
            }
        }
        return newList;
    }

    /** List of view links to define how entities are connected (or "joined") */
    public ModelViewLink getViewLink(int index) {
        return this.viewLinks.get(index);
    }

    public int getViewLinksSize() {
        return this.viewLinks.size();
    }

    public Iterator<ModelViewLink> getViewLinksIterator() {
        return this.viewLinks.iterator();
    }

    public List<ModelViewLink> getViewLinksCopy() {
        List<ModelViewLink> newList = FastList.newInstance();
        newList.addAll(this.viewLinks);
        return newList;
    }

    public void addViewLink(ModelViewLink viewLink) {
        this.viewLinks.add(viewLink);
    }

    public void populateViewEntityConditionInformation(ModelFieldTypeReader modelFieldTypeReader, List<EntityCondition> whereConditions, List<EntityCondition> havingConditions, List<String> orderByList, List<String> entityAliasStack) {
        if (entityAliasStack == null) {
            entityAliasStack = FastList.newInstance();
        }

        if (this.viewEntityCondition != null) {
            EntityCondition whereCondition = this.viewEntityCondition.getWhereCondition(modelFieldTypeReader, entityAliasStack);
            if (whereCondition != null) {
                whereConditions.add(whereCondition);
            }

            EntityCondition havingCondition = this.viewEntityCondition.getHavingCondition(modelFieldTypeReader, entityAliasStack);
            if (havingCondition != null) {
                havingConditions.add(havingCondition);
            }

            // add the current one first so it overrides the lower level ones
            List<String> currentOrderByList = this.viewEntityCondition.getOrderByList();
            if (currentOrderByList != null) {
                orderByList.addAll(currentOrderByList);
            }
        }

        for (Map.Entry<String, ModelEntity> memberEntityEntry: this.memberModelEntities.entrySet()) {
            if (memberEntityEntry.getValue() instanceof ModelViewEntity) {
                ModelViewEntity memberViewEntity = (ModelViewEntity) memberEntityEntry.getValue();
                entityAliasStack.add(memberEntityEntry.getKey());
                memberViewEntity.populateViewEntityConditionInformation(modelFieldTypeReader, whereConditions, havingConditions, orderByList, entityAliasStack);
                entityAliasStack.remove(entityAliasStack.size() - 1);
            }
        }
    }

    @Override
    public String colNameString(String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), separator, afterLast, alias);
    }

    @Override
    public String colNameString(List<ModelField> flds, String separator, String afterLast, boolean alias) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        Iterator<ModelField> fldsIt = flds.iterator();
        while (fldsIt.hasNext()) {
            ModelField field = fldsIt.next();
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
        ModelMemberEntity modelMemberEntity = this.memberModelMemberEntities.get(entityAlias);
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

        for (Map.Entry<String, ModelMemberEntity> entry: memberModelMemberEntities.entrySet()) {

            ModelMemberEntity modelMemberEntity = entry.getValue();
            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
            if (aliasedEntity == null) {
                continue;
            }
            memberModelEntities.put(entry.getKey(), aliasedEntity);
            Iterator<ModelField> aliasedFieldIterator = aliasedEntity.getFieldsIterator();
            while (aliasedFieldIterator.hasNext()) {
                ModelField aliasedModelField = aliasedFieldIterator.next();
                ModelField newModelField = new ModelField();
                for (int i = 0; i < aliasedModelField.getValidatorsSize(); i++) {
                    newModelField.addValidator(aliasedModelField.getValidator(i));
                }
                newModelField.setColName(modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getColName());
                newModelField.setName(modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getName());
                newModelField.setType(aliasedModelField.getType());
                newModelField.setDescription(aliasedModelField.getDescription());
                newModelField.setIsPk(false);
                aliasedModelEntity.addField(newModelField);
            }
        }

        expandAllAliasAlls(modelReader);

        for (ModelAlias alias: aliases) {
            ModelField field = new ModelField();
            field.setModelEntity(this);
            field.name = alias.name;
            field.description = alias.description;

            // if this is a groupBy field, add it to the groupBys list
            if (alias.groupBy || groupByFields.contains(alias.name)) {
                this.groupBys.add(field);
            }

            // show a warning if function is specified and groupBy is true
            if (UtilValidate.isNotEmpty(alias.function) && alias.groupBy) {
                Debug.logWarning("The view-entity alias with name=" + alias.name + " has a function value and is specified as a group-by field; this may be an error, but is not necessarily.", module);
            }

            if (alias.isComplexAlias()) {
                // if this is a complex alias, make a complex column name...
                StringBuilder colNameBuffer = new StringBuilder();
                StringBuilder fieldTypeBuffer = new StringBuilder();
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

                field.encrypt = aliasedField.encrypt;

                field.type = aliasedField.type;
                field.validators = aliasedField.validators;

                field.colName = alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.colName);
                if (UtilValidate.isEmpty(field.description)) {
                    field.description = aliasedField.description;
                }
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
                String prefix = functionPrefixMap.get(alias.function);
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

        Map<String, ModelConversion> aliasConversions = conversions.get(member.getEntityName());
        if (aliasConversions == null) {
            aliasConversions = FastMap.newInstance();
            conversions.put(member.getEntityName(), aliasConversions);
        }
        ModelConversion conversion = aliasConversions.get(aliasName);
        if (conversion == null) {
            conversion = new ModelConversion(aliasName, member);
            aliasConversions.put(aliasName, conversion);
        }
        return conversion;
    }

    public void populateReverseLinks() {
        Map<String, List<String>> containedModelFields = FastMap.newInstance();
        Iterator<ModelAlias> it = getAliasesIterator();
        while (it.hasNext()) {
            ModelViewEntity.ModelAlias alias = it.next();
            if (alias.isComplexAlias()) {
                // TODO: conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly
                Debug.logWarning("Conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly, will not work for alias: " + alias.getName() + " of view-entity " + this.getEntityName(), module);
            } else {
                ModelConversion conversion = getOrCreateModelConversion(alias.getEntityAlias());
                conversion.addConversion(alias.getField(), alias.getName());
            }

            List<String> aliases = containedModelFields.get(alias.getField());
            if (aliases == null) {
                aliases = FastList.newInstance();
                containedModelFields.put(alias.getField(), aliases);
            }
            aliases.add(alias.getName());
        }

        Iterator<ModelViewLink> it2 = getViewLinksIterator();
        while (it2.hasNext()) {
            ModelViewEntity.ModelViewLink link = it2.next();

            String leftAlias = link.getEntityAlias();
            String rightAlias = link.getRelEntityAlias();
            ModelConversion leftConversion = getOrCreateModelConversion(leftAlias);
            ModelConversion rightConversion = getOrCreateModelConversion(rightAlias);
            Iterator<ModelKeyMap> it3 = link.getKeyMapsIterator();
            Debug.logVerbose(leftAlias + "<->" + rightAlias, module);
            while (it3.hasNext()) {
                ModelKeyMap mkm = it3.next();
                String leftFieldName = mkm.getFieldName();
                String rightFieldName = mkm.getRelFieldName();
                rightConversion.addAllAliasConversions(containedModelFields.get(leftFieldName), rightFieldName);
                leftConversion.addAllAliasConversions(containedModelFields.get(rightFieldName), leftFieldName);
            }
        }
        int[] currentIndex = new int[conversions.size()];
        int[] maxIndex = new int[conversions.size()];
        ModelConversion[][] allConversions = new ModelConversion[conversions.size()][];
        int i = 0;
        for (Map<String, ModelConversion> aliasConversions: conversions.values()) {
            currentIndex[i] = 0;
            maxIndex[i] = aliasConversions.size();
            allConversions[i] = new ModelConversion[aliasConversions.size()];
            int j = 0;
            for (ModelConversion conversion: aliasConversions.values()) {
                allConversions[i][j] = conversion;
                j++;
            }
            i++;
        }
        int ptr = 0;
        ModelConversion[] currentConversions = new ModelConversion[conversions.size()];
        for (int j = 0, k; j < currentIndex.length; j++) {
            for (int l = 0; l < maxIndex[ j ]; l++) {
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

    public List<Map<String, Object>> convert(String fromEntityName, Map<String, ? extends Object> data) {
        Map<String, ModelConversion> conversions = this.conversions.get(fromEntityName);
        if (conversions == null) return null;
        List<Map<String, Object>> values = FastList.newInstance();
        for (ModelConversion conversion: conversions.values()) {
            conversion.convert(values, data);
        }
        return values;
    }

    /**
     * Go through all aliasAlls and create an alias for each field of each member entity
     */
    private void expandAllAliasAlls(ModelReader modelReader) {
        for (ModelAliasAll aliasAll: aliasAlls) {
            String prefix = aliasAll.getPrefix();
            String function = aliasAll.getFunction();
            boolean groupBy = aliasAll.getGroupBy();

            ModelMemberEntity modelMemberEntity = memberModelMemberEntities.get(aliasAll.getEntityAlias());
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

            List<String> entFieldList = aliasedEntity.getAllFieldNames();
            if (entFieldList == null) {
                Debug.logError("Entity referred to in member-entity " + aliasAll.getEntityAlias() + " has no fields, ignoring: " + aliasedEntityName, module);
                continue;
            }

            for (String fieldName: entFieldList) {
                // now merge the lists, leaving out any that duplicate an existing alias name
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
                    StringBuilder newAliasBuffer = new StringBuilder(prefix);
                    //make sure the first letter is uppercase to delineate the field name
                    newAliasBuffer.append(Character.toUpperCase(aliasName.charAt(0)));
                    newAliasBuffer.append(aliasName.substring(1));
                    aliasName = newAliasBuffer.toString();
                }

                ModelAlias existingAlias = this.getAlias(aliasName);
                if (existingAlias != null) {
                    //log differently if this is part of a view-link key-map because that is a common case when a field will be auto-expanded multiple times
                    boolean isInViewLink = false;
                    Iterator<ModelViewLink> viewLinkIter = this.getViewLinksIterator();
                    while (viewLinkIter.hasNext() && !isInViewLink) {
                        ModelViewLink modelViewLink = viewLinkIter.next();
                        boolean isRel = false;
                        if (modelViewLink.getRelEntityAlias().equals(aliasAll.getEntityAlias())) {
                            isRel = true;
                        } else if (!modelViewLink.getEntityAlias().equals(aliasAll.getEntityAlias())) {
                            // not the rel-entity-alias or the entity-alias, so move along
                            continue;
                        }
                        Iterator<ModelKeyMap> keyMapIter = modelViewLink.getKeyMapsIterator();
                        while (keyMapIter.hasNext() && !isInViewLink) {
                            ModelKeyMap modelKeyMap = keyMapIter.next();
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
                expandedAlias.function = function;
                expandedAlias.groupBy = groupBy;
                expandedAlias.description = modelField.getDescription();

                aliases.add(expandedAlias);
            }
        }
    }

    @Override
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

    public static class ModelAliasAll implements Serializable, Iterable<String> {
        protected String entityAlias = "";
        protected String prefix = "";
        protected Set<String> fieldsToExclude = null;
        protected boolean groupBy = false;
        // is specified this alias is a calculated value; can be: min, max, sum, avg, count, count-distinct
        protected String function = null;

        protected ModelAliasAll() {}

        public ModelAliasAll(String entityAlias, String prefix) {
            this.entityAlias = entityAlias;
            this.prefix = prefix;
        }

        public ModelAliasAll(Element aliasAllElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasAllElement.getAttribute("entity-alias")).intern();
            this.prefix = UtilXml.checkEmpty(aliasAllElement.getAttribute("prefix")).intern();
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasAllElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasAllElement.getAttribute("function"));

            List<? extends Element> excludes = UtilXml.childElementList(aliasAllElement, "exclude");
            if (UtilValidate.isNotEmpty(excludes)) {
                this.fieldsToExclude = new HashSet<String>();
                for (Element excludeElement: excludes) {
                    this.fieldsToExclude.add(excludeElement.getAttribute("field").intern());
                }
            }

        }

        public String getEntityAlias() {
            return this.entityAlias;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public boolean getGroupBy() {
            return this.groupBy;
        }

        public String getFunction() {
            return this.function;
        }

        public boolean shouldExclude(String fieldName) {
            if (this.fieldsToExclude == null) {
                return false;
            } else {
                return this.fieldsToExclude.contains(fieldName);
            }
        }

        public Iterator<String> iterator() {
            return fieldsToExclude.iterator();
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
        // The description for documentation purposes
        protected String description = "";

        protected ModelAlias() {}

        public ModelAlias(Element aliasElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasElement.getAttribute("entity-alias")).intern();
            this.name = UtilXml.checkEmpty(aliasElement.getAttribute("name")).intern();
            this.field = UtilXml.checkEmpty(aliasElement.getAttribute("field"), this.name).intern();
            this.colAlias = UtilXml.checkEmpty(aliasElement.getAttribute("col-alias"), ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name))).intern();
            String primKeyValue = UtilXml.checkEmpty(aliasElement.getAttribute("prim-key"));

            if (UtilValidate.isNotEmpty(primKeyValue)) {
                this.isPk = Boolean.valueOf("true".equals(primKeyValue));
            } else {
                this.isPk = null;
            }
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasElement.getAttribute("function")).intern();
            this.description = UtilXml.checkEmpty(UtilXml.childElementValue(aliasElement, "description")).intern();

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

        public void makeAliasColName(StringBuilder colNameBuffer, StringBuilder fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
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

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean getIsFromAliasAll() {
            return this.isFromAliasAll;
        }
    }

    public static interface ComplexAliasMember extends Serializable {
        public void makeAliasColName(StringBuilder colNameBuffer, StringBuilder fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader);
    }

    public static class ComplexAlias implements ComplexAliasMember {
        protected List<ComplexAliasMember> complexAliasMembers = FastList.newInstance();
        protected String operator;

        public ComplexAlias(String operator) {
            this.operator = operator;
        }

        public ComplexAlias(Element complexAliasElement) {
            this.operator = complexAliasElement.getAttribute("operator").intern();
            // handle all complex-alias and complex-alias-field sub-elements
            for (Element subElement: UtilXml.childElementList(complexAliasElement)) {
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

        public void addAllComplexAliasMembers(List<ComplexAliasMember> complexAliasMembers) {
            this.complexAliasMembers.addAll(complexAliasMembers);
        }

        public void makeAliasColName(StringBuilder colNameBuffer, StringBuilder fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            if (complexAliasMembers.size() == 0) {
                return;
            } else if (complexAliasMembers.size() == 1) {
                ComplexAliasMember complexAliasMember = complexAliasMembers.iterator().next();
                complexAliasMember.makeAliasColName(colNameBuffer, fieldTypeBuffer, modelViewEntity, modelReader);
            } else {
                colNameBuffer.append('(');
                Iterator<ComplexAliasMember> complexAliasMemberIter = complexAliasMembers.iterator();
                while (complexAliasMemberIter.hasNext()) {
                    ComplexAliasMember complexAliasMember = complexAliasMemberIter.next();
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
            this.entityAlias = complexAliasFieldElement.getAttribute("entity-alias").intern();
            this.field = complexAliasFieldElement.getAttribute("field").intern();
            this.defaultValue = complexAliasFieldElement.getAttribute("default-value").intern();
            this.function = complexAliasFieldElement.getAttribute("function").intern();
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
        public void makeAliasColName(StringBuilder colNameBuffer, StringBuilder fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            ModelEntity modelEntity = modelViewEntity.getAliasedEntity(entityAlias, modelReader);
            ModelField modelField = modelViewEntity.getAliasedField(modelEntity, field, modelReader);

            String colName = entityAlias + "." + modelField.getColName();

            if (UtilValidate.isNotEmpty(defaultValue)) {
                colName = "COALESCE(" + colName + "," + defaultValue + ")";
            }

            if (UtilValidate.isNotEmpty(function)) {
                String prefix = functionPrefixMap.get(function);
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

    public static class ModelViewLink implements Serializable, Iterable<ModelKeyMap> {
        protected String entityAlias = "";
        protected String relEntityAlias = "";
        protected boolean relOptional = false;
        protected List<ModelKeyMap> keyMaps = FastList.newInstance();
        protected ViewEntityCondition viewEntityCondition = null;

        protected ModelViewLink() {}

        public ModelViewLink(ModelViewEntity modelViewEntity, Element viewLinkElement) {
            this.entityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("entity-alias")).intern();
            this.relEntityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("rel-entity-alias")).intern();
            // if anything but true will be false; ie defaults to false
            this.relOptional = "true".equals(viewLinkElement.getAttribute("rel-optional"));

            NodeList keyMapList = viewLinkElement.getElementsByTagName("key-map");
            for (int j = 0; j < keyMapList.getLength(); j++) {
                Element keyMapElement = (Element) keyMapList.item(j);
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                if (keyMap != null) keyMaps.add(keyMap);
            }

            Element entityConditionElement = UtilXml.firstChildElement(viewLinkElement, "entity-condition");
            if (entityConditionElement != null) {
                this.viewEntityCondition = new ViewEntityCondition(modelViewEntity, this, entityConditionElement);
            }
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, ModelKeyMap... keyMaps) {
            this(entityAlias, relEntityAlias, relOptional, Arrays.asList(keyMaps));
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, List<ModelKeyMap> keyMaps) {
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
            return this.keyMaps.get(index);
        }

        public int getKeyMapsSize() {
            return this.keyMaps.size();
        }

        public Iterator<ModelKeyMap> getKeyMapsIterator() {
            return this.keyMaps.iterator();
        }

        public Iterator<ModelKeyMap> iterator() {
            return this.keyMaps.iterator();
        }

        public List<ModelKeyMap> getKeyMapsCopy() {
            List<ModelKeyMap> newList = FastList.newInstance();
            newList.addAll(this.keyMaps);
            return newList;
        }
    }

    public class ModelConversion implements Serializable {
        protected String aliasName;
        protected ModelEntity fromModelEntity;
        protected Map<String, String> fieldMap = FastMap.newInstance();
        protected Set<String> wildcards = new HashSet<String>();

        public ModelConversion(String aliasName, ModelEntity fromModelEntity) {
            this.aliasName = aliasName;
            this.fromModelEntity = fromModelEntity;
            Iterator<ModelField> it = getFieldsIterator();
            while (it.hasNext()) {
                ModelField field = it.next();
                wildcards.add(field.getName());
            }
        }

        @Override
        public int hashCode() {
            return fromModelEntity.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ModelConversion)) return false;
            ModelConversion other = (ModelConversion) obj;
            return fromModelEntity.equals(other.fromModelEntity);
        }

        public void addConversion(String fromFieldName, String toFieldName) {
            wildcards.remove(toFieldName);
            fieldMap.put(fromFieldName, toFieldName);
        }

        @Override
        public String toString() {
            //return fromModelEntity.getEntityName() + ":" + fieldMap + ":" + wildcards;
            return aliasName + "(" + fromModelEntity.getEntityName() + ")";
        }

        public void convert(List<Map<String, Object>> values, Map<String, ? extends Object> value) {
            Map<String, Object> newValue = FastMap.newInstance();
            for (Map.Entry<String, String> entry: fieldMap.entrySet()) {
                newValue.put(entry.getValue(), value.get(entry.getKey()));
            }
            for (String key: wildcards) {
                newValue.put(key, EntityOperator.WILDCARD);
            }
            values.add(newValue);
        }

        public void addAllAliasConversions(String fieldName, String... aliases) {
            addAllAliasConversions(Arrays.asList(aliases), fieldName);
        }

        public void addAllAliasConversions(List<String> aliases, String fieldName) {
            if (aliases != null) {
                for (String alias: aliases) {
                    addConversion(fieldName, alias);
                }
            }
        }
    }

    public static class ViewEntityCondition {
        protected ModelViewEntity modelViewEntity;
        protected ModelViewLink modelViewLink;
        protected boolean filterByDate;
        protected boolean distinct;
        protected List<String> orderByList;
        protected ViewCondition whereCondition;
        protected ViewCondition havingCondition;

        public ViewEntityCondition(ModelViewEntity modelViewEntity, ModelViewLink modelViewLink, Element element) {
            this.modelViewEntity = modelViewEntity;
            this.modelViewLink = modelViewLink;
            this.filterByDate = "true".equals(element.getAttribute("filter-by-date"));
            this.distinct = "true".equals(element.getAttribute("distinct"));
            // process order-by
            List<? extends Element> orderByElementList = UtilXml.childElementList(element, "order-by");
            if (orderByElementList.size() > 0) {
                orderByList = FastList.newInstance();
                for (Element orderByElement: orderByElementList) {
                    orderByList.add(orderByElement.getAttribute("field-name"));
                }
            }

            Element conditionExprElement = UtilXml.firstChildElement(element, "condition-expr");
            Element conditionListElement = UtilXml.firstChildElement(element, "condition-list");
            if (conditionExprElement != null) {
                this.whereCondition = new ViewConditionExpr(this, conditionExprElement);
            } else if (conditionListElement != null) {
                this.whereCondition = new ViewConditionList(this, conditionListElement);
            }

            Element havingConditionListElement = UtilXml.firstChildElement(element, "having-condition-list");
            if (havingConditionListElement != null) {
                this.havingCondition = new ViewConditionList(this, havingConditionListElement);
            }
        }

        public List<String> getOrderByList() {
            return this.orderByList;
        }

        public EntityCondition getWhereCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            if (this.whereCondition != null) {
                return this.whereCondition.createCondition(modelFieldTypeReader, entityAliasStack);
            } else {
                return null;
            }
        }

        public EntityCondition getHavingCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            if (this.havingCondition != null) {
                return this.havingCondition.createCondition(modelFieldTypeReader, entityAliasStack);
            } else {
                return null;
            }
        }
    }

    public static interface ViewCondition extends Serializable {
        public EntityCondition createCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack);
    }

    public static class ViewConditionExpr implements ViewCondition {
        protected ViewEntityCondition viewEntityCondition;
        protected String entityAlias;
        protected String fieldName;
        protected String operator;
        protected String relEntityAlias;
        protected String relFieldName;
        protected Object value;
        protected boolean ignoreCase;

        public ViewConditionExpr(ViewEntityCondition viewEntityCondition, Element conditionExprElement) {
            this.viewEntityCondition = viewEntityCondition;
            this.entityAlias = conditionExprElement.getAttribute("entity-alias");
            this.fieldName = conditionExprElement.getAttribute("field-name");

            this.operator = UtilFormatOut.checkEmpty(conditionExprElement.getAttribute("operator"), "equals");
            this.relEntityAlias = conditionExprElement.getAttribute("rel-entity-alias");
            this.relFieldName = conditionExprElement.getAttribute("rel-field-name");
            this.value = conditionExprElement.getAttribute("value");
            this.ignoreCase = "true".equals(conditionExprElement.getAttribute("ignore-case"));

            // if we are in a view-link, default to the entity-alias and rel-entity-alias there
            if (this.viewEntityCondition.modelViewLink != null) {
                if (UtilValidate.isEmpty(this.entityAlias)) {
                    this.entityAlias = this.viewEntityCondition.modelViewLink.getEntityAlias();
                }
                if (UtilValidate.isEmpty(this.relEntityAlias)) {
                    this.relEntityAlias = this.viewEntityCondition.modelViewLink.getRelEntityAlias();
                }
            }
        }

        public EntityCondition createCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            EntityOperator<?,?,?> operator = EntityOperator.lookup(this.operator);
            if (operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + this.operator);
            }

            // If IN or BETWEEN operator, see if value is a literal list and split it
            if ((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN))
                    && value instanceof String) {
                String delim = null;
                if (((String)value).indexOf("|") >= 0) {
                    delim = "|";
                } else if (((String)value).indexOf(",") >= 0) {
                    delim = ",";
                }
                if (UtilValidate.isNotEmpty(delim)) {
                    value = StringUtil.split((String) value, delim);
                }
            }

            if (this.viewEntityCondition.modelViewEntity.getField(fieldName) == null) {
                throw new IllegalArgumentException("Error in Entity Find: could not find field [" + fieldName + "] in entity with name [" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]");
            }

            // don't convert the field to the desired type if this is an IN or BETWEEN operator and we have a Collection
            if (!((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN))
                    && value instanceof Collection)) {
                // now to a type conversion for the target fieldName
                value = this.viewEntityCondition.modelViewEntity.convertFieldValue(this.viewEntityCondition.modelViewEntity.getField(fieldName), value, modelFieldTypeReader, FastMap.<String, Object>newInstance());
            }

            if (Debug.verboseOn()) Debug.logVerbose("Got value for fieldName [" + fieldName + "]: " + value, module);

            EntityConditionValue lhs = EntityFieldValue.makeFieldValue(this.fieldName, this.entityAlias, entityAliasStack, this.viewEntityCondition.modelViewEntity);
            Object rhs = null;
            if (value != null) {
                rhs = value;
            } else {
                rhs = EntityFieldValue.makeFieldValue(this.relFieldName, this.relEntityAlias, entityAliasStack, this.viewEntityCondition.modelViewEntity);
            }

            if (operator.equals(EntityOperator.NOT_EQUAL) && value != null) {
                // since some databases don't consider nulls in != comparisons, explicitly include them
                // this makes more sense logically, but if anyone ever needs it to not behave this way we should add an "or-null" attribute that is true by default
                if (ignoreCase) {
                    return EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.UPPER(lhs), UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), EntityFunction.UPPER(rhs)),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(lhs, EntityOperator.EQUALS, null));
                } else {
                    return EntityCondition.makeCondition(
                            EntityCondition.makeCondition(lhs, UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), rhs),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(lhs, EntityOperator.EQUALS, null));
                }
            } else {
                if (ignoreCase) {
                    // use the stuff to upper case both sides
                    return EntityCondition.makeCondition(EntityFunction.UPPER(lhs), UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), EntityFunction.UPPER(rhs));
                } else {
                    return EntityCondition.makeCondition(lhs, UtilGenerics.<EntityComparisonOperator<?,?>>cast(operator), rhs);
                }
            }
        }
    }

    public static class ViewConditionList implements ViewCondition {
        protected ViewEntityCondition viewEntityCondition;
        List<ViewCondition> conditionList = new LinkedList<ViewCondition>();
        String combine;

        public ViewConditionList(ViewEntityCondition viewEntityCondition, Element conditionListElement) {
            this.viewEntityCondition = viewEntityCondition;
            this.combine = conditionListElement.getAttribute("combine");

            List<? extends Element> subElements = UtilXml.childElementList(conditionListElement);
            for (Element subElement: subElements) {
                if ("condition-expr".equals(subElement.getNodeName())) {
                    conditionList.add(new ViewConditionExpr(this.viewEntityCondition, subElement));
                } else if ("condition-list".equals(subElement.getNodeName())) {
                    conditionList.add(new ViewConditionList(this.viewEntityCondition, subElement));
                } else {
                    throw new IllegalArgumentException("Invalid element with name [" + subElement.getNodeName() + "] found under a condition-list element.");
                }
            }
        }

        public EntityCondition createCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            if (this.conditionList.size() == 0) {
                return null;
            }
            if (this.conditionList.size() == 1) {
                ViewCondition condition = this.conditionList.get(0);
                return condition.createCondition(modelFieldTypeReader, entityAliasStack);
            }

            List<EntityCondition> entityConditionList = FastList.<EntityCondition>newInstance();
            for (ViewCondition curCondition: conditionList) {
                EntityCondition econd = curCondition.createCondition(modelFieldTypeReader, entityAliasStack);
                if (econd != null) {
                    entityConditionList.add(econd);
                }
            }

            EntityOperator<?,?,?> operator = EntityOperator.lookup(this.combine);
            if (operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operator);
            }

            return EntityCondition.makeCondition(entityConditionList, UtilGenerics.<EntityJoinOperator>cast(operator));
        }
    }
}
