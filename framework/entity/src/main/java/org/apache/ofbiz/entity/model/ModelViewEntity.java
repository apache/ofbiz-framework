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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionValue;
import org.apache.ofbiz.entity.condition.EntityFieldValue;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class extends ModelEntity and provides additional information appropriate to view entities
 */
@SuppressWarnings("serial")
public class ModelViewEntity extends ModelEntity {
    public static final String module = ModelViewEntity.class.getName();

    private static final Map<String, String> functionPrefixMap = new HashMap<>();
    private static final Set<String> numericFunctionsSet = new HashSet<>(); // names of functions that return a numeric type
    static {
        functionPrefixMap.put("min", "MIN(");
        functionPrefixMap.put("max", "MAX(");
        functionPrefixMap.put("sum", "SUM(");
        functionPrefixMap.put("avg", "AVG(");
        functionPrefixMap.put("count", "COUNT(");
        functionPrefixMap.put("count-distinct", "COUNT(DISTINCT ");
        functionPrefixMap.put("upper", "UPPER(");
        functionPrefixMap.put("lower", "LOWER(");
        functionPrefixMap.put("extract-year", "EXTRACT(YEAR FROM ");
        functionPrefixMap.put("extract-month", "EXTRACT(MONTH FROM ");
        functionPrefixMap.put("extract-day", "EXTRACT(DAY FROM ");
        numericFunctionsSet.add("count");
        numericFunctionsSet.add("count-distinct");
        numericFunctionsSet.add("extract-year");
        numericFunctionsSet.add("extract-month");
        numericFunctionsSet.add("extract-day");
    }

    /** Contains member-entity alias name definitions: key is alias, value is ModelMemberEntity */
    protected Map<String, ModelMemberEntity> memberModelMemberEntities = new HashMap<>();

    /** A list of all ModelMemberEntity entries; this is mainly used to preserve the original order of member entities from the XML file */
    protected List<ModelMemberEntity> allModelMemberEntities = new LinkedList<>();

    /** Contains member-entity ModelEntities: key is alias, value is ModelEntity; populated with fields */
    protected Map<String, String> memberModelEntities = new HashMap<>();

    /** List of alias-alls which act as a shortcut for easily pulling over member entity fields */
    protected List<ModelAliasAll> aliasAlls = new LinkedList<>();

    /** List of aliases with information in addition to what is in the standard field list */
    protected List<ModelAlias> aliases = new LinkedList<>();

    /** List of view links to define how entities are connected (or "joined") */
    protected List<ModelViewLink> viewLinks = new LinkedList<>();

    /** A List of the Field objects for the View Entity, one for each GROUP BY field */
    protected List<ModelField> groupBys = new LinkedList<>();

    /** List of field names to group by */
    protected List<String> groupByFields = new LinkedList<>();

    protected Map<String, ModelConversion[]> conversions = new HashMap<>();

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
                Debug.logError("[new ModelViewEntity]: entity-alias or entity-name missing on member-entity element of the view-entity " + this.entityName, module);
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
        super(modelReader, new ModelInfo(
                dynamicViewEntity.getTitle(),
                ModelInfo.DEFAULT.getDescription(),
                ModelInfo.DEFAULT.getCopyright(),
                ModelInfo.DEFAULT.getAuthor(),
                ModelInfo.DEFAULT.getVersion(),
                dynamicViewEntity.getDefaultResourceName()));
        this.entityName = dynamicViewEntity.getEntityName();
        this.packageName = dynamicViewEntity.getPackageName();

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
        String entityName = this.memberModelEntities.get(alias);
        return entityName != null ? this.getModelReader().getModelEntityNoCheck(entityName) : null;
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
        List<ModelAlias> newList = new ArrayList<>(this.aliases);
        return newList;
    }

    public int getGroupBysSize() {
        return this.groupBys.size();
    }

    public List<ModelField> getGroupBysCopy() {
        return getGroupBysCopy(null);
    }

    public List<ModelField> getGroupBysCopy(List<ModelField> selectFields) {
        List<ModelField> newList = new ArrayList<>(this.groupBys.size());
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
        List<ModelViewLink> newList = new ArrayList<>(this.viewLinks);
        return newList;
    }

    public void addViewLink(ModelViewLink viewLink) {
        this.viewLinks.add(viewLink);
    }

    public void populateViewEntityConditionInformation(ModelFieldTypeReader modelFieldTypeReader, List<EntityCondition> whereConditions, List<EntityCondition> havingConditions, List<String> orderByList, List<String> entityAliasStack) {
        if (entityAliasStack == null) {
            entityAliasStack = new LinkedList<>();
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
    }

    @Deprecated @Override
    public String colNameString(String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), separator, afterLast, alias);
    }

    @Override
    public StringBuilder colNameString(StringBuilder sb, String prefix, String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), sb, prefix, separator, afterLast, alias);
    }

    @Deprecated @Override
    public String colNameString(List<ModelField> flds, String separator, String afterLast, boolean alias) {
        return colNameString(flds, new StringBuilder(), "", separator, afterLast, alias).toString();
    }

    @Override
    public StringBuilder colNameString(List<ModelField> flds, StringBuilder sb, String prefix, String separator, String afterLast, boolean alias) {
        if (flds.size() < 1) {
            return sb;
        }

        sb.append(prefix);
        Iterator<ModelField> fldsIt = flds.iterator();
        while (fldsIt.hasNext()) {
            ModelField field = fldsIt.next();
            sb.append(field.getColValue());
            if (alias) {
                ModelAlias modelAlias = this.getAlias(field.getName());
                if (modelAlias != null) {
                    sb.append(" AS ").append(modelAlias.getColAlias());
                }
            }
            if (fldsIt.hasNext()) {
                sb.append(separator);
            }
        }

        sb.append(afterLast);
        return sb;
    }

    protected ModelEntity aliasedModelEntity = new ModelEntity();

    public ModelEntity getAliasedModelEntity() {
        return this.aliasedModelEntity;
    }

    public ModelEntity getAliasedEntity(String entityAlias, ModelReader modelReader) {
        ModelMemberEntity modelMemberEntity = this.memberModelMemberEntities.get(entityAlias);
        if (modelMemberEntity == null) {
            Debug.logError("[" + this.getEntityName() + "]: No member entity with alias " + entityAlias + " found; this view-entity will NOT be usable...", module);
            return null;
        }

        String aliasedEntityName = modelMemberEntity.getEntityName();
        ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
        if (aliasedEntity == null) {
            Debug.logError("[" + this.getEntityName() + "]: [ModelViewEntity.populateFields] ERROR: could not find ModelEntity for entity name: " + aliasedEntityName, module);
            return null;
        }

        return aliasedEntity;
    }

    public ModelField getAliasedField(ModelEntity aliasedEntity, String field, ModelReader modelReader) {
        ModelField aliasedField = aliasedEntity.getField(field);
        if (aliasedField == null) {
            Debug.logError("[" + this.getEntityName() + "]: [ModelViewEntity.populateFields] ERROR: could not find ModelField for entity name: " + aliasedEntity.getEntityName() + " and field: " + field, module);
            return null;
        }
        return aliasedField;
    }

    public void populateFields(ModelReader modelReader) {
        populateFieldsBasic(modelReader);
        populateReverseLinks();
    }

    public void populateFieldsBasic(ModelReader modelReader) {
        for (Map.Entry<String, ModelMemberEntity> entry: memberModelMemberEntities.entrySet()) {

            ModelMemberEntity modelMemberEntity = entry.getValue();
            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
            if (aliasedEntity == null) {
                continue;
            }
            memberModelEntities.put(entry.getKey(), aliasedEntityName);
            Iterator<ModelField> aliasedFieldIterator = aliasedEntity.getFieldsIterator();
            while (aliasedFieldIterator.hasNext()) {
                ModelField aliasedModelField = aliasedFieldIterator.next();
                ModelField newModelField = ModelField.create(this, aliasedModelField.getDescription(), modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getName(),
                        aliasedModelField.getType(), modelMemberEntity.getEntityAlias() + "." + aliasedModelField.getColName(), null, null, false, false, false, false,
                        false, aliasedModelField.getValidators());
                aliasedModelEntity.addField(newModelField);
            }
        }

        expandAllAliasAlls(modelReader);

        for (ModelAlias alias: aliases) {
            // show a warning if function is specified and groupBy is true
            if (UtilValidate.isNotEmpty(alias.function) && alias.groupBy) {
                Debug.logWarning("[" + this.getEntityName() + "]: The view-entity alias with name=" + alias.name + " has a function value and is specified as a group-by field; this may be an error, but is not necessarily.", module);
            }
            String description = alias.description;
            String name = alias.name;
            String type = "";
            String colName = "";
            String colValue = "";
            String fieldSet = "";
            boolean isNotNull = false;
            boolean isPk = false;
            ModelField.EncryptMethod encryptMethod = ModelField.EncryptMethod.FALSE;
            boolean isAutoCreatedInternal = false;
            boolean enableAuditLog = false;
            List<String> validators = null;
            if (alias.isComplexAlias()) {
                // if this is a complex alias, make a complex column name...
                StringBuilder colNameBuffer = new StringBuilder();
                StringBuilder fieldTypeBuffer = new StringBuilder();
                alias.makeAliasColName(colNameBuffer, fieldTypeBuffer, this, modelReader);
                colValue = colNameBuffer.toString();
                colName = ModelUtil.javaNameToDbName(alias.name);
                type = fieldTypeBuffer.toString();
                isPk = false;
                fieldSet = alias.getFieldSet();
            } else {
                ModelEntity aliasedEntity = getAliasedEntity(alias.entityAlias, modelReader);
                ModelField aliasedField = getAliasedField(aliasedEntity, alias.field, modelReader);
                if (aliasedField == null) {
                    Debug.logError("[" + this.getEntityName() + "]: [populateFields] ERROR: could not find ModelField for field name \"" +
                        alias.field + "\" on entity with name: " + aliasedEntity.getEntityName(), module);
                    continue;
                }
                if (alias.isPk != null) {
                    isPk = alias.isPk;
                } else {
                    isPk = aliasedField.getIsPk();
                }
                encryptMethod = aliasedField.getEncryptMethod();
                type = aliasedField.getType();
                validators = aliasedField.getValidators();
                colValue = alias.entityAlias + "." + SqlJdbcUtil.filterColName(aliasedField.getColName());
                colName = SqlJdbcUtil.filterColName(colValue);
                if (description.isEmpty()) {
                    description = aliasedField.getDescription();
                }
                if (alias.getFieldSet().isEmpty()) {
                    String aliasedFieldSet = aliasedField.getFieldSet();
                    if (!aliasedFieldSet.isEmpty()) {
                        StringBuilder fieldSetBuffer = new StringBuilder(alias.entityAlias);
                        fieldSetBuffer.append("_");
                        fieldSetBuffer.append(Character.toUpperCase(aliasedFieldSet.charAt(0)));
                        fieldSetBuffer.append(aliasedFieldSet.substring(1));
                        fieldSet = fieldSetBuffer.toString().intern();
                        Debug.logInfo("[" + this.getEntityName() + "]: copied field set on [" + name + "]: " + fieldSet, module);
                    }
                } else {
                    fieldSet = alias.getFieldSet();
                }
            }
            if (numericFunctionsSet.contains(alias.function)) {
                // if we have a numeric function we have to change the type
                type = "numeric";
            }
            if (UtilValidate.isNotEmpty(alias.function)) {
                String prefix = functionPrefixMap.get(alias.function);
                if (prefix == null) {
                    Debug.logWarning("[" + this.getEntityName() + "]: Specified alias function [" + alias.function + "] not valid; must be: min, max, sum, avg, count or count-distinct; using a column name with no function function", module);
                } else {
                    colValue = prefix + colValue + ")";
                }
            }
            ModelField field = ModelField.create(this, description, name, type, colName, colValue, fieldSet, isNotNull, isPk, encryptMethod, isAutoCreatedInternal, enableAuditLog, validators);
            // if this is a groupBy field, add it to the groupBys list
            if (alias.groupBy || groupByFields.contains(alias.name)) {
                this.groupBys.add(field);
            }
            addField(field);
        }
    }

    protected ModelConversion getOrCreateModelConversion(String aliasName) {
        ModelEntity member = getMemberModelEntity(aliasName);
        if (member == null) {
            String errMsg = "No member found for aliasName - " + aliasName;
            Debug.logWarning("[" + this.getEntityName() + "]: " + errMsg, module);
            throw new RuntimeException("[" + this.getEntityName() + "]: Cannot create View Entity: " + errMsg);
        }

        ModelConversion[] allConversions = conversions.get(member.getEntityName());
        if (allConversions == null) {
            ModelConversion conversion = new ModelConversion(aliasName, member);
            conversions.put(member.getEntityName(), new ModelConversion[] {conversion});
            return conversion;
        }
        for (ModelConversion conversion: allConversions) {
            if (conversion.aliasName.equals(aliasName)) {
                return conversion;
            }
        }
        ModelConversion[] newConversions = new ModelConversion[allConversions.length + 1];
        System.arraycopy(allConversions, 0, newConversions, 0, allConversions.length);
        ModelConversion conversion = new ModelConversion(aliasName, member);
        newConversions[allConversions.length] = conversion;
        conversions.put(member.getEntityName(), newConversions);
        return conversion;
    }

    public void populateReverseLinks() {
        Map<String, List<String>> containedModelFields = new HashMap<>();
        Iterator<ModelAlias> it = getAliasesIterator();
        while (it.hasNext()) {
            ModelViewEntity.ModelAlias alias = it.next();
            if (alias.isComplexAlias()) {
                // TODO: conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly
                Debug.logWarning("[" + this.getEntityName() + "]: Conversion for complex-alias needs to be implemented for cache and in-memory eval stuff to work correctly, will not work for alias: " + alias.getName(), module);
            } else {
                ModelConversion conversion = getOrCreateModelConversion(alias.getEntityAlias());
                conversion.addConversion(alias.getField(), alias.getName());
            }

            List<String> aliases = containedModelFields.get(alias.getField());
            if (aliases == null) {
                aliases = new LinkedList<>();
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
        for (ModelConversion[] aliasConversions: conversions.values()) {
            currentIndex[i] = 0;
            maxIndex[i] = aliasConversions.length;
            allConversions[i] = new ModelConversion[aliasConversions.length];
            System.arraycopy(aliasConversions, 0, allConversions[i], 0, aliasConversions.length);
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
        ModelConversion[] conversions = this.conversions.get(fromEntityName);
        if (conversions == null) return null;
        List<Map<String, Object>> values = new LinkedList<>();
        for (ModelConversion conversion: conversions) {
            conversion.convert(values, data);
        }
        return values;
    }

    /**
     * Go through all aliasAlls and create an alias for each field of each member entity
     */
    private void expandAllAliasAlls(ModelReader modelReader) {
        for (ModelAliasAll aliasAll: aliasAlls) {
            String entityAlias = aliasAll.getEntityAlias();
            String prefix = aliasAll.getPrefix();
            String function = aliasAll.getFunction();
            boolean groupBy = aliasAll.getGroupBy();
            String aliasAllFieldSet = aliasAll.getFieldSet();

            ModelMemberEntity modelMemberEntity = memberModelMemberEntities.get(entityAlias);
            if (modelMemberEntity == null) {
                Debug.logError("[" + this.getEntityName() + "]: Member entity referred to in alias-all not found, ignoring: " + entityAlias, module);
                continue;
            }

            String aliasedEntityName = modelMemberEntity.getEntityName();
            ModelEntity aliasedEntity = modelReader.getModelEntityNoCheck(aliasedEntityName);
            if (aliasedEntity == null) {
                Debug.logError("[" + this.getEntityName() + "]: Entity referred to in member-entity " + entityAlias + " not found, ignoring: " + aliasedEntityName, module);
                continue;
            }

            List<String> entFieldList = aliasedEntity.getAllFieldNames();
            if (entFieldList == null) {
                Debug.logError("[" + this.getEntityName() + "]: Entity referred to in member-entity " + entityAlias + " has no fields, ignoring: " + aliasedEntityName, module);
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
                String fieldSet;
                if (UtilValidate.isEmpty(aliasAllFieldSet)) {
                    String aliasedFieldSet = modelField.getFieldSet();
                    if (UtilValidate.isNotEmpty(aliasedFieldSet)) {
                        StringBuilder fieldSetBuffer = new StringBuilder(entityAlias);
                        if (UtilValidate.isNotEmpty(prefix)) {
                            fieldSetBuffer.append(Character.toUpperCase(prefix.charAt(0)));
                            fieldSetBuffer.append(prefix.substring(1));
                        }
                        fieldSetBuffer.append(Character.toUpperCase(aliasedFieldSet.charAt(0)));
                        fieldSetBuffer.append(aliasedFieldSet.substring(1));
                        fieldSet = fieldSetBuffer.toString();
                    } else {
                        fieldSet = "";
                    }
                } else {
                    fieldSet = aliasAllFieldSet;
                }
                if (UtilValidate.isNotEmpty(fieldSet)) {
                    Debug.logInfo("[" + this.getEntityName() + "]: set field-set on [" + aliasName + "]: " + fieldSet, module);
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
                    String warnMsg = "[" + this.getEntityName() + "]: Throwing out field alias in view entity because one already exists with the alias name [" + aliasName + "] and field name [" + modelMemberEntity.getEntityAlias() + "(" + aliasedEntity.getEntityName() + ")." + fieldName + "], existing field name is [" + existingAlias.getEntityAlias() + "." + existingAlias.getField() + "]";
                    if (isInViewLink) {
                        Debug.logVerbose(warnMsg, module);
                    } else {
                        Debug.logInfo(warnMsg, module);
                    }
                    continue;
                }

                ModelAlias expandedAlias = new ModelAlias(aliasAll.getEntityAlias(), aliasName, fieldName, ModelUtil.javaNameToDbName(UtilXml.checkEmpty(aliasName)), null, groupBy, function, fieldSet, true);
                expandedAlias.setDescription(modelField.getDescription());

                aliases.add(expandedAlias);
            }
        }
    }

    @Override
    public String toString() {
        return "ModelViewEntity[" + getEntityName() + "]";
    }

    public static final class ModelMemberEntity implements Serializable {
        public final String entityAlias;
        public final String entityName;

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

    public static final class ModelAliasAll implements Serializable, Iterable<String> {
        public final String entityAlias;
        public final String prefix;
        public final Set<String> fieldsToExclude;
        public final boolean groupBy;
        // is specified this alias is a calculated value; can be: min, max, sum, avg, count, count-distinct
        public final String function;
        public final String fieldSet;

        @Deprecated
        public ModelAliasAll(String entityAlias, String prefix) {
            this(entityAlias, prefix, false, null, null, null);
        }

        @Deprecated
        public ModelAliasAll(String entityAlias, String prefix, boolean groupBy, String function, Collection<String> excludes) {
            this(entityAlias, prefix, groupBy, function, null, excludes);
        }

        public ModelAliasAll(String entityAlias, String prefix, boolean groupBy, String function, String fieldSet, Collection<String> excludes) {
            this.entityAlias = entityAlias;
            this.prefix = prefix;
            this.groupBy = groupBy;
            this.function = function;
            this.fieldSet = fieldSet;
            if (UtilValidate.isNotEmpty(excludes)) {
                this.fieldsToExclude = new HashSet<>(excludes.size());
                this.fieldsToExclude.addAll(excludes);
            } else {
                this.fieldsToExclude = null;
            }
        }

        public ModelAliasAll(Element aliasAllElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasAllElement.getAttribute("entity-alias")).intern();
            this.prefix = UtilXml.checkEmpty(aliasAllElement.getAttribute("prefix")).intern();
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasAllElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasAllElement.getAttribute("function"));
            this.fieldSet = UtilXml.checkEmpty(aliasAllElement.getAttribute("field-set")).intern();

            List<? extends Element> excludes = UtilXml.childElementList(aliasAllElement, "exclude");
            if (UtilValidate.isNotEmpty(excludes)) {
                this.fieldsToExclude = new HashSet<>();
                for (Element excludeElement: excludes) {
                    this.fieldsToExclude.add(excludeElement.getAttribute("field").intern());
                }
            } else {
                this.fieldsToExclude = null;
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

        public String getFieldSet() {
            return this.fieldSet;
        }

        public boolean shouldExclude(String fieldName) {
            if (this.fieldsToExclude == null) {
                return false;
            } else {
                return this.fieldsToExclude.contains(fieldName);
            }
        }

        @Override
        public Iterator<String> iterator() {
            if (this.fieldsToExclude == null) {
                return Collections.<String>emptySet().iterator();
            } else {
                return fieldsToExclude.iterator();
            }
        }
    }

    public static final class ModelAlias implements Serializable {
        public final String entityAlias;
        public final String name;
        public final String field;
        public final String colAlias;
        // this is a Boolean object for a tri-state: null, true or false
        public final Boolean isPk;
        public final boolean groupBy;
        // is specified this alias is a calculated value; can be: min, max, sum, avg, count, count-distinct
        public final String function;
        public final String fieldSet;
        public final boolean isFromAliasAll;
        public ComplexAliasMember complexAliasMember;
        // The description for documentation purposes
        public String description = "";

        public ModelAlias(Element aliasElement) {
            this.entityAlias = UtilXml.checkEmpty(aliasElement.getAttribute("entity-alias")).intern();
            this.name = UtilXml.checkEmpty(aliasElement.getAttribute("name")).intern();
            this.field = UtilXml.checkEmpty(aliasElement.getAttribute("field"), this.name).intern();
            this.colAlias = UtilXml.checkEmpty(aliasElement.getAttribute("col-alias"), ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name))).intern();
            String primKeyValue = UtilXml.checkEmpty(aliasElement.getAttribute("prim-key"));

            if (UtilValidate.isNotEmpty(primKeyValue)) {
                this.isPk = "true".equals(primKeyValue);
            } else {
                this.isPk = null;
            }
            this.groupBy = "true".equals(UtilXml.checkEmpty(aliasElement.getAttribute("group-by")));
            this.function = UtilXml.checkEmpty(aliasElement.getAttribute("function")).intern();
            this.fieldSet = UtilXml.checkEmpty(aliasElement.getAttribute("field-set")).intern();
            this.isFromAliasAll = false;
            this.description = UtilXml.checkEmpty(UtilXml.childElementValue(aliasElement, "description")).intern();

            Element complexAliasElement = UtilXml.firstChildElement(aliasElement, "complex-alias");
            if (complexAliasElement != null) {
                complexAliasMember = new ComplexAlias(complexAliasElement);
            }
        }

        @Deprecated
        public ModelAlias(String entityAlias, String name, String field, String colAlias, Boolean isPk, Boolean groupBy, String function) {
            this(entityAlias, name, field, colAlias, isPk, groupBy, function, null, false);
        }

        public ModelAlias(String entityAlias, String name, String field, String colAlias, Boolean isPk, Boolean groupBy, String function, String fieldSet) {
            this(entityAlias, name, field, colAlias, isPk, groupBy, function, fieldSet, false);
        }

        protected ModelAlias(String entityAlias, String name, String field, String colAlias, Boolean isPk, Boolean groupBy, String function, String fieldSet, boolean isFromAliasAll) {
            this.entityAlias = entityAlias;
            this.name = name;
            this.field = UtilXml.checkEmpty(field, this.name);
            this.colAlias = UtilXml.checkEmpty(colAlias, ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name)));
            this.isPk = isPk;
            if (groupBy != null) {
                this.groupBy = groupBy;
            } else {
                this.groupBy = false;
            }
            this.function = function;
            this.fieldSet = UtilXml.checkEmpty(fieldSet).intern();
            this.isFromAliasAll = isFromAliasAll;
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

        public String getFieldSet() {
            return fieldSet;
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

    public static final class ComplexAlias implements ComplexAliasMember {
        public final List<ComplexAliasMember> complexAliasMembers = new LinkedList<>();
        public final String operator;

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

        @Override
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

    public static final class ComplexAliasField implements ComplexAliasMember {
        public final String entityAlias;
        public final String field;
        public final String defaultValue;
        public final String function;
        public final String value;

        public ComplexAliasField(Element complexAliasFieldElement) {
            this.entityAlias = complexAliasFieldElement.getAttribute("entity-alias").intern();
            this.field = complexAliasFieldElement.getAttribute("field").intern();
            this.defaultValue = complexAliasFieldElement.getAttribute("default-value").intern();
            this.function = complexAliasFieldElement.getAttribute("function").intern();
            this.value = complexAliasFieldElement.getAttribute("value").intern();
        }

        public ComplexAliasField(String entityAlias, String field, String defaultValue, String function) {
            this.entityAlias = entityAlias;
            this.field = field;
            this.defaultValue = defaultValue;
            this.function = function;
            this.value = null;
        }
        public ComplexAliasField(String entityAlias, String field, String defaultValue, String function, String value) {
            this.entityAlias = entityAlias;
            this.field = field;
            this.defaultValue = defaultValue;
            this.function = function;
            this.value = value;
        }

        /**
         * Make the alias as follows: function(coalesce(entityAlias.field, defaultValue))
         */
        @Override
        public void makeAliasColName(StringBuilder colNameBuffer, StringBuilder fieldTypeBuffer, ModelViewEntity modelViewEntity, ModelReader modelReader) {
            if(UtilValidate.isEmpty(entityAlias) 
                    && UtilValidate.isEmpty(field) 
                    && UtilValidate.isNotEmpty(value)){
                colNameBuffer.append(value);
            }
            else {
                ModelEntity modelEntity = modelViewEntity.getAliasedEntity(entityAlias, modelReader);
                ModelField modelField = modelViewEntity.getAliasedField(modelEntity, field, modelReader);
                String colName = entityAlias + "." + modelField.getColName();

                if (UtilValidate.isNotEmpty(defaultValue)) {
                    colName = "COALESCE(" + colName + "," + defaultValue + ")";
                }

                if (UtilValidate.isNotEmpty(function)) {
                    String prefix = functionPrefixMap.get(function);
                    if (prefix == null) {
                        Debug.logWarning("[" + modelViewEntity.getEntityName() + "]: Specified alias function [" + function + "] not valid; must be: min, max, sum, avg, count or count-distinct; using a column name with no function function", module);
                    } else {
                        colName = prefix + colName + ")";
                    }
                }

                colNameBuffer.append(colName);
                //set fieldTypeBuffer if not already set
                if (fieldTypeBuffer.length() == 0) {
                    fieldTypeBuffer.append(modelField.getType());
                }
            }
        }
    }

    public static final class ModelViewLink implements Serializable, Iterable<ModelKeyMap> {
        public final String entityAlias;
        public final String relEntityAlias;
        public final boolean relOptional;
        public final List<ModelKeyMap> keyMaps = new LinkedList<>();
        public final ViewEntityCondition viewEntityCondition;

        public ModelViewLink(ModelViewEntity modelViewEntity, Element viewLinkElement) {
            this.entityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("entity-alias")).intern();
            this.relEntityAlias = UtilXml.checkEmpty(viewLinkElement.getAttribute("rel-entity-alias")).intern();
            // if anything but true will be false; ie defaults to false
            this.relOptional = "true".equals(viewLinkElement.getAttribute("rel-optional"));

            NodeList keyMapList = viewLinkElement.getElementsByTagName("key-map");
            int keyMapLength = keyMapList.getLength();
            for (int j = 0; j < keyMapLength; j++) {
                Element keyMapElement = (Element) keyMapList.item(j);
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                keyMaps.add(keyMap);
            }

            Element entityConditionElement = UtilXml.firstChildElement(viewLinkElement, "entity-condition");
            if (entityConditionElement != null) {
                this.viewEntityCondition = new ViewEntityCondition(modelViewEntity, this, entityConditionElement);
            } else {
                this.viewEntityCondition = null;
            }
        }

        @Deprecated
        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, ModelKeyMap... keyMaps) {
            this(entityAlias, relEntityAlias, relOptional, null, Arrays.asList(keyMaps));
        }

        @Deprecated
        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, List<ModelKeyMap> keyMaps) {
            this(entityAlias, relEntityAlias, relOptional, null, keyMaps);
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, ViewEntityCondition viewEntityCondition, ModelKeyMap... keyMaps) {
            this(entityAlias, relEntityAlias, relOptional, viewEntityCondition, Arrays.asList(keyMaps));
        }

        public ModelViewLink(String entityAlias, String relEntityAlias, Boolean relOptional, ViewEntityCondition viewEntityCondition, List<ModelKeyMap> keyMaps) {
            this.entityAlias = entityAlias;
            this.relEntityAlias = relEntityAlias;
            if (relOptional != null) {
                this.relOptional = relOptional;
            } else {
                this.relOptional = false;
            }
            this.keyMaps.addAll(keyMaps);
            this.viewEntityCondition = viewEntityCondition;
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

        @Override
        public Iterator<ModelKeyMap> iterator() {
            return this.keyMaps.iterator();
        }

        public List<ModelKeyMap> getKeyMapsCopy() {
            List<ModelKeyMap> newList = new ArrayList<>(this.keyMaps);
            return newList;
        }

        public ViewEntityCondition getViewEntityCondition() {
            return this.viewEntityCondition;
        }
    }

    public final class ModelConversion implements Serializable {
        public final String aliasName;
        public final ModelEntity fromModelEntity;
        public final Map<String, String> fieldMap = new HashMap<>();
        public final Set<String> wildcards = new HashSet<>();

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
            Map<String, Object> newValue = new HashMap<>();
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

    public static final class ViewEntityCondition {
        public final ModelViewEntity modelViewEntity;
        public final ModelViewLink modelViewLink;
        public final boolean filterByDate;
        public final boolean distinct;
        public final List<String> orderByList;
        public final ViewCondition whereCondition;
        public final ViewCondition havingCondition;

        // TODO: add programatic constructor
        public ViewEntityCondition(ModelViewEntity modelViewEntity, ModelViewLink modelViewLink, Element element) {
            this.modelViewEntity = modelViewEntity;
            this.modelViewLink = modelViewLink;
            this.filterByDate = "true".equals(element.getAttribute("filter-by-date"));
            this.distinct = "true".equals(element.getAttribute("distinct"));
            // process order-by
            List<? extends Element> orderByElementList = UtilXml.childElementList(element, "order-by");
            if (orderByElementList.size() > 0) {
                orderByList = new ArrayList<>(orderByElementList.size());
                for (Element orderByElement: orderByElementList) {
                    orderByList.add(orderByElement.getAttribute("field-name"));
                }
            } else {
                orderByList = null;
            }

            Element conditionExprElement = UtilXml.firstChildElement(element, "condition-expr");
            Element conditionListElement = UtilXml.firstChildElement(element, "condition-list");
            if (conditionExprElement != null) {
                this.whereCondition = new ViewConditionExpr(this, conditionExprElement);
            } else if (conditionListElement != null) {
                this.whereCondition = new ViewConditionList(this, conditionListElement);
            } else {
                this.whereCondition = null;
            }

            Element havingConditionListElement = UtilXml.firstChildElement(element, "having-condition-list");
            if (havingConditionListElement != null) {
                this.havingCondition = new ViewConditionList(this, havingConditionListElement);
            } else {
                this.havingCondition = null;
            }
        }

        public List<String> getOrderByList() {
            return this.orderByList;
        }

        public EntityCondition getWhereCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {

            List<EntityCondition> conditionList = new LinkedList<>();
            if(this.filterByDate) {
                conditionList.add(EntityUtil.getFilterByDateExpr());
            }
            if (this.whereCondition != null) {
                conditionList.add(whereCondition.createCondition(modelFieldTypeReader, entityAliasStack));
            }

            return EntityCondition.makeCondition(conditionList, EntityOperator.AND);
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

    public static final class ViewConditionExpr implements ViewCondition {
        public final ViewEntityCondition viewEntityCondition;
        public final String entityAlias;
        public final String fieldName;
        public final EntityComparisonOperator<?, ?> operator;
        public final String relEntityAlias;
        public final String relFieldName;
        public final Object value;
        public final boolean ignoreCase;

        // TODO: add programatic constructor
        public ViewConditionExpr(ViewEntityCondition viewEntityCondition, Element conditionExprElement) {
            this.viewEntityCondition = viewEntityCondition;
            String entityAlias = conditionExprElement.getAttribute("entity-alias");
            this.fieldName = conditionExprElement.getAttribute("field-name");

            String operator = UtilFormatOut.checkEmpty(conditionExprElement.getAttribute("operator"), "equals");
            try {
                this.operator = EntityOperator.lookupComparison(operator);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Could not find an entity operator for the name: " + operator);
            }
            String relEntityAlias = conditionExprElement.getAttribute("rel-entity-alias");
            String relFieldNameStr = conditionExprElement.getAttribute("rel-field-name");
            if (UtilValidate.isEmpty(relFieldNameStr)) {
                this.relFieldName = null;
            } else {
                this.relFieldName = relFieldNameStr;
            }
            String valueStr = conditionExprElement.getAttribute("value");
            if (UtilValidate.isEmpty(valueStr)) {
                this.value = null;
            } else {
                this.value = valueStr;
            }
            this.ignoreCase = "true".equals(conditionExprElement.getAttribute("ignore-case"));

            // if we are in a view-link, default to the entity-alias and rel-entity-alias there
            if (this.viewEntityCondition.modelViewLink != null) {
                if (UtilValidate.isEmpty(entityAlias)) {
                    entityAlias = this.viewEntityCondition.modelViewLink.getEntityAlias();
                }
                if (UtilValidate.isEmpty(relEntityAlias)) {
                    relEntityAlias = this.viewEntityCondition.modelViewLink.getRelEntityAlias();
                }
            }
            this.entityAlias = entityAlias;
            this.relEntityAlias = relEntityAlias;
        }

        @Override
        public EntityCondition createCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            Object value = this.value;
            // If IN or BETWEEN operator, see if value is a literal list and split it
            if ((this.operator == EntityOperator.IN || this.operator == EntityOperator.BETWEEN)
                    && value instanceof String) {
                String delim = null;
                if (((String) value).indexOf('|') >= 0) {
                    delim = "|";
                } else if (((String) value).indexOf(',') >= 0) {
                    delim = ",";
                }
                if (UtilValidate.isNotEmpty(delim)) {
                    value = StringUtil.split((String) value, delim);
                }
            }

            EntityConditionValue lhs = EntityFieldValue.makeFieldValue(this.fieldName, this.entityAlias, entityAliasStack, this.viewEntityCondition.modelViewEntity);
            ModelField lhsField = lhs.getModelField(this.viewEntityCondition.modelViewEntity);
            if (lhsField == null) {
                throw new IllegalArgumentException("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Error in Entity Find: could not find field [" + fieldName + "]");
            }

            // don't convert the field to the desired type if this is an IN or BETWEEN operator and we have a Collection
            if (!((this.operator == EntityOperator.IN || this.operator == EntityOperator.BETWEEN)
                    && value instanceof Collection<?>)) {
                // now to a type conversion for the target fieldName
                value = this.viewEntityCondition.modelViewEntity.convertFieldValue(lhsField, value,modelFieldTypeReader, new HashMap<>());
            }

            if (Debug.verboseOn()) Debug.logVerbose("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Got value for fieldName [" + fieldName + "]: " + value, module);

            Object rhs = null;
            if (value != null) {
                rhs = value;
            } else {
                rhs = EntityFieldValue.makeFieldValue(this.relFieldName, this.relEntityAlias, entityAliasStack, this.viewEntityCondition.modelViewEntity);
            }

            EntityCondition entityCondition;

            if (this.operator == EntityOperator.NOT_EQUAL && value != null) {
                // since some databases don't consider nulls in != comparisons, explicitly include them
                // this makes more sense logically, but if anyone ever needs it to not behave this way we should add an "or-null" attribute that is true by default
                if (ignoreCase) {
                    entityCondition = EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.UPPER(lhs), this.operator, EntityFunction.UPPER(rhs)),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(lhs, EntityOperator.EQUALS, null));
                } else {
                    entityCondition = EntityCondition.makeCondition(
                            EntityCondition.makeCondition(lhs, this.operator, rhs),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(lhs, EntityOperator.EQUALS, null));
                }
            } else if ( value == null && this.relFieldName == null && (this.operator == EntityOperator.EQUALS || this.operator == EntityOperator.NOT_EQUAL)) {
                entityCondition = EntityCondition.makeCondition(lhs, this.operator, null);
            } else {
                if (ignoreCase) {
                    // use the stuff to upper case both sides
                    entityCondition = EntityCondition.makeCondition(EntityFunction.UPPER(lhs), this.operator, EntityFunction.UPPER(rhs));
                } else {
                    entityCondition = EntityCondition.makeCondition(lhs, this.operator, rhs);
                }
            }

            if(this.viewEntityCondition.filterByDate) {
                List<EntityCondition> conditionList = new LinkedList<>();
                conditionList.add(entityCondition);
                conditionList.add(EntityUtil.getFilterByDateExpr());
                return EntityCondition.makeCondition(conditionList, EntityOperator.AND);
            } else {
                return entityCondition;
            }

        }
    }

    public static final class ViewConditionList implements ViewCondition {
        public final ViewEntityCondition viewEntityCondition;
        public final List<ViewCondition> conditionList = new LinkedList<>();
        public final EntityJoinOperator operator;

        public ViewConditionList(ViewEntityCondition viewEntityCondition, Element conditionListElement) {
            this.viewEntityCondition = viewEntityCondition;
            String combine = conditionListElement.getAttribute("combine");
            try {
                this.operator = EntityOperator.lookupJoin(combine);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Could not find an entity operator for the name: " + combine);
            }

            List<? extends Element> subElements = UtilXml.childElementList(conditionListElement);
            for (Element subElement: subElements) {
                if ("condition-expr".equals(subElement.getNodeName())) {
                    conditionList.add(new ViewConditionExpr(this.viewEntityCondition, subElement));
                } else if ("condition-list".equals(subElement.getNodeName())) {
                    conditionList.add(new ViewConditionList(this.viewEntityCondition, subElement));
                } else {
                    throw new IllegalArgumentException("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Invalid element with name [" + subElement.getNodeName() + "] found under a condition-list element.");
                }
            }
        }

        public ViewConditionList(ViewEntityCondition viewEntityCondition, String combine, List<ViewCondition> conditionList) {
            this.viewEntityCondition = viewEntityCondition;
            try {
                this.operator = EntityOperator.lookupJoin(combine);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("[" + this.viewEntityCondition.modelViewEntity.getEntityName() + "]: Could not find an entity operator for the name: " + combine);
            }
            if (UtilValidate.isNotEmpty(conditionList)) {
                this.conditionList.addAll(conditionList);
            }
        }

        @Override
        public EntityCondition createCondition(ModelFieldTypeReader modelFieldTypeReader, List<String> entityAliasStack) {
            if (this.conditionList.size() == 0) {
                return null;
            }
            if (this.conditionList.size() == 1) {
                ViewCondition condition = this.conditionList.get(0);
                return condition.createCondition(modelFieldTypeReader, entityAliasStack);
            }

            List<EntityCondition> entityConditionList = new LinkedList<>();
            for (ViewCondition curCondition: conditionList) {
                EntityCondition econd = curCondition.createCondition(modelFieldTypeReader, entityAliasStack);
                if (econd != null) {
                    entityConditionList.add(econd);
                }
            }

            if(this.viewEntityCondition.filterByDate) {
                entityConditionList.add(EntityUtil.getFilterByDateExpr());
            }

            if(this.viewEntityCondition.filterByDate) {
                entityConditionList.add(EntityUtil.getFilterByDateExpr());
            }

            return EntityCondition.makeCondition(entityConditionList, this.operator);
        }
    }
}
