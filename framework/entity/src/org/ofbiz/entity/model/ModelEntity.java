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
package org.ofbiz.entity.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilPlist;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generic Entity - Entity model class
 *
 */
@SuppressWarnings("serial")
public class ModelEntity extends ModelInfo implements Comparable<ModelEntity>, Serializable {

    @SuppressWarnings("hiding")
    public static final String module = ModelEntity.class.getName();

    /** The name of the time stamp field for locking/synchronization */
    public static final String STAMP_FIELD = "lastUpdatedStamp";
    public static final String STAMP_TX_FIELD = "lastUpdatedTxStamp";
    public static final String CREATE_STAMP_FIELD = "createdStamp";
    public static final String CREATE_STAMP_TX_FIELD = "createdTxStamp";

    /** The ModelReader that created this Entity */
    protected ModelReader modelReader = null;

    /** The entity-name of the Entity */
    protected String entityName = "";

    /** The table-name of the Entity */
    protected String tableName = "";

    /** The package-name of the Entity */
    protected String packageName = "";

    /** The entity-name of the Entity that this Entity is dependent on, if empty then no dependency */
    protected String dependentOn = "";

    /** The sequence-bank-size of the Entity */
    protected Integer sequenceBankSize = null;

    /** A List of the Field objects for the Entity */
    protected List<ModelField> fields = FastList.newInstance();
    protected Map<String, ModelField> fieldsMap = null;

    /** A List of the Field objects for the Entity, one for each Primary Key */
    protected List<ModelField> pks = FastList.newInstance();

    /** A List of the Field objects for the Entity, one for each NON Primary Key */
    protected List<ModelField> nopks = FastList.newInstance();

    /** relations defining relationships between this entity and other entities */
    protected List<ModelRelation> relations = FastList.newInstance();

    /** indexes on fields/columns in this entity */
    protected List<ModelIndex> indexes = FastList.newInstance();

    /** The reference of the dependentOn entity model */
    protected ModelEntity specializationOfModelEntity = null;

    /** The list of entities that are specialization of on this entity */
    protected Map<String, ModelEntity> specializedEntities = FastMap.newInstance();

    /** map of ModelViewEntities that references this model */
    protected Set<String> viewEntities = FastSet.newInstance();

    /** An indicator to specify if this entity requires locking for updates */
    protected boolean doLock = false;

    /** Can be used to disable automatically creating update stamp fields and populating them on inserts and updates */
    protected boolean noAutoStamp = false;

    /** An indicator to specify if this entity is never cached.
     * If true causes the delegator to not clear caches on write and to not get
     * from cache on read showing a warning messages to that effect
     */
    protected boolean neverCache = false;

    protected boolean autoClearCache = true;

    protected Boolean hasFieldWithAuditLog = null;

    /** The location of this entity's definition */
    protected String location = "";

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelEntity() {}

    protected ModelEntity(ModelReader reader) {
        this.modelReader = reader;
    }

    protected ModelEntity(ModelReader reader, ModelInfo def) {
        super(def);
        this.modelReader = reader;
    }

    /** XML Constructor */
    protected ModelEntity(ModelReader reader, Element entityElement, ModelInfo def) {
        this(reader, def);
        populateFromAttributes(entityElement);
    }

    /** XML Constructor */
    public ModelEntity(ModelReader reader, Element entityElement, UtilTimer utilTimer, ModelInfo def) {
        this(reader, entityElement, def);

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before general/basic info");
        this.populateBasicInfo(entityElement);

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before fields");
        for (Element fieldElement: UtilXml.childElementList(entityElement, "field")) {
            ModelField field = reader.createModelField(fieldElement);
            if (field != null) {
                field.setModelEntity(this);
                this.fields.add(field);
            }
        }

        // if applicable automatically add the STAMP_FIELD and STAMP_TX_FIELD fields
        if ((this.doLock || !this.noAutoStamp) && !this.isField(STAMP_FIELD)) {
            ModelField newField = reader.createModelField(STAMP_FIELD, "date-time", null, false);
            newField.setIsAutoCreatedInternal(true);
            newField.setModelEntity(this);
            this.fields.add(newField);
        }
        if (!this.noAutoStamp && !this.isField(STAMP_TX_FIELD)) {
            ModelField newField = reader.createModelField(STAMP_TX_FIELD, "date-time", null, false);
            newField.setIsAutoCreatedInternal(true);
            newField.setModelEntity(this);
            this.fields.add(newField);

            // also add an index for this field
            String indexName = ModelUtil.shortenDbName(this.tableName + "_TXSTMP", 18);
            ModelIndex txIndex = new ModelIndex(this, indexName, false);
            txIndex.addIndexField(ModelEntity.STAMP_TX_FIELD);
            txIndex.setModelEntity(this);
            indexes.add(txIndex);
        }

        // if applicable automatically add the CREATE_STAMP_FIELD and CREATE_STAMP_TX_FIELD fields
        if ((this.doLock || !this.noAutoStamp) && !this.isField(CREATE_STAMP_FIELD)) {
            ModelField newField = reader.createModelField(CREATE_STAMP_FIELD, "date-time", null, false);
            newField.setIsAutoCreatedInternal(true);
            newField.setModelEntity(this);
            this.fields.add(newField);
        }
        if (!this.noAutoStamp && !this.isField(CREATE_STAMP_TX_FIELD)) {
            ModelField newField = reader.createModelField(CREATE_STAMP_TX_FIELD, "date-time", null, false);
            newField.setIsAutoCreatedInternal(true);
            newField.setModelEntity(this);
            this.fields.add(newField);

            // also add an index for this field
            String indexName = ModelUtil.shortenDbName(this.tableName + "_TXCRTS", 18);
            ModelIndex txIndex = new ModelIndex(this, indexName, false);
            txIndex.addIndexField(ModelEntity.CREATE_STAMP_TX_FIELD);
            txIndex.setModelEntity(this);
            indexes.add(txIndex);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before prim-keys");
        for (Element pkElement: UtilXml.childElementList(entityElement, "prim-key")) {
            ModelField field = reader.findModelField(this, pkElement.getAttribute("field").intern());
            if (field != null) {
                this.pks.add(field);
                field.isPk = true;
                field.isNotNull = true;
            } else {
                Debug.logError("[ModelReader.createModelEntity] ERROR: Could not find field \"" +
                        pkElement.getAttribute("field") + "\" specified in a prim-key", module);
            }
        }

        // now that we have the pks and the fields, make the nopks vector
        this.nopks = FastList.newInstance();
        for (ModelField field: this.fields) {
            if (!field.isPk) this.nopks.add(field);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);
        this.populateIndexes(entityElement);
    }

    /** DB Names Constructor */
    public ModelEntity(String tableName, Map<String, DatabaseUtil.ColumnCheckInfo> colMap, ModelFieldTypeReader modelFieldTypeReader, boolean isCaseSensitive) {
        // if there is a dot in the name, remove it and everything before it, should be the schema name
        this.tableName = tableName;
        int dotIndex = this.tableName.indexOf(".");
        if (dotIndex >= 0) {
            this.tableName = this.tableName.substring(dotIndex + 1);
        }
        this.entityName = ModelUtil.dbNameToClassName(this.tableName);
        for (Map.Entry<String, DatabaseUtil.ColumnCheckInfo> columnEntry: colMap.entrySet()) {
            DatabaseUtil.ColumnCheckInfo ccInfo = columnEntry.getValue();
            ModelField newField = new ModelField(ccInfo, modelFieldTypeReader);
            this.fields.add(newField);
        }
        this.updatePkLists();
    }

    protected void populateBasicInfo(Element entityElement) {
        this.entityName = UtilXml.checkEmpty(entityElement.getAttribute("entity-name")).intern();
        this.tableName = UtilXml.checkEmpty(entityElement.getAttribute("table-name"), ModelUtil.javaNameToDbName(this.entityName)).intern();
        this.packageName = UtilXml.checkEmpty(entityElement.getAttribute("package-name")).intern();
        this.dependentOn = UtilXml.checkEmpty(entityElement.getAttribute("dependent-on")).intern();
        this.doLock = UtilXml.checkBoolean(entityElement.getAttribute("enable-lock"), false);
        this.noAutoStamp = UtilXml.checkBoolean(entityElement.getAttribute("no-auto-stamp"), false);
        this.neverCache = UtilXml.checkBoolean(entityElement.getAttribute("never-cache"), false);
        this.autoClearCache = UtilXml.checkBoolean(entityElement.getAttribute("auto-clear-cache"), true);

        String sequenceBankSizeStr = UtilXml.checkEmpty(entityElement.getAttribute("sequence-bank-size"));
        if (UtilValidate.isNotEmpty(sequenceBankSizeStr)) {
            try {
                this.sequenceBankSize = Integer.valueOf(sequenceBankSizeStr);
            } catch (NumberFormatException e) {
                Debug.logError("Error parsing sequence-bank-size value [" + sequenceBankSizeStr + "] for entity [" + this.entityName + "]", module);
            }
        }
    }


    protected void populateRelated(ModelReader reader, Element entityElement) {
        for (Element relationElement: UtilXml.childElementList(entityElement, "relation")) {
            ModelRelation relation = reader.createRelation(this, relationElement);
            if (relation != null) {
                relation.setModelEntity(this);
                this.relations.add(relation);
            }
        }
    }


    protected void populateIndexes(Element entityElement) {
        for (Element indexElement: UtilXml.childElementList(entityElement, "index")) {
            ModelIndex index = new ModelIndex(this, indexElement);
            index.setModelEntity(this);
            this.indexes.add(index);
        }
    }

    public boolean containsAllPkFieldNames(Set<String> fieldNames) {
        Iterator<ModelField> pksIter = this.getPksIterator();
        while (pksIter.hasNext()) {
            ModelField pkField = pksIter.next();
            if (!fieldNames.contains(pkField.getName())) {
                return false;
            }
        }
        return true;
    }


    public void addExtendEntity(ModelReader reader, Element extendEntityElement) {
        for (Element fieldElement: UtilXml.childElementList(extendEntityElement, "field")) {
            // TODO: should we look for existing fields of the same name here? for now just add to list...
            ModelField field = reader.createModelField(fieldElement);
            if (field != null) {
                field.setModelEntity(this);
                this.fields.add(field);
                // this will always be true for now as extend-entity fielsd are always nonpks
                if (!field.isPk) this.nopks.add(field);
            }
        }
        
        // override the default resource file
        String defResourceName = StringUtil.internString(extendEntityElement.getAttribute("default-resource-name"));
        //Debug.log("Extended entity - " + extendEntityElement.getAttribute("entity-name") + " new resource name : " + defResourceName, module);
        if (UtilValidate.isNotEmpty(defResourceName)) {
            this.setDefaultResourceName(defResourceName);
        }

        this.populateRelated(reader, extendEntityElement);
        this.populateIndexes(extendEntityElement);
        this.dependentOn = UtilXml.checkEmpty(extendEntityElement.getAttribute("dependent-on")).intern();
    }

    // ===== GETTERS/SETTERS =====


    public ModelReader getModelReader() {
        return modelReader;
    }

    /** The entity-name of the Entity */
    public String getEntityName() {
        return this.entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /** The plain table-name of the Entity without a schema name prefix */
    public String getPlainTableName() {
        return this.tableName;
    }

    /** The table-name of the Entity including a Schema name if specified in the datasource config */
    public String getTableName(String helperName) {
        return getTableName(EntityConfigUtil.getDatasourceInfo(helperName));
    }

    /** The table-name of the Entity including a Schema name if specified in the datasource config */
    public String getTableName(DatasourceInfo datasourceInfo) {
        if (UtilValidate.isNotEmpty(datasourceInfo.schemaName)) {
            return datasourceInfo.schemaName + "." + this.tableName;
        } else {
            return this.tableName;
        }
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /** The package-name of the Entity */
    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /** The entity-name of the Entity that this Entity is dependent on, if empty then no dependency */
    public String getDependentOn() {
        return this.dependentOn;
    }

    public void setDependentOn(String dependentOn) {
        this.dependentOn = dependentOn;
    }

    /** An indicator to specify if this entity is never cached.
     * If true causes the delegator to not clear caches on write and to not get
     * from cache on read showing a warning messages to that effect
     */
    public boolean getNeverCache() {
        return this.neverCache;
    }

    public void setNeverCache(boolean neverCache) {
        this.neverCache = neverCache;
    }

    public boolean getAutoClearCache() {
        return this.autoClearCache;
    }

    public void setAutoClearCache(boolean autoClearCache) {
        this.autoClearCache = autoClearCache;
    }

    public boolean getHasFieldWithAuditLog() {
        if (this.hasFieldWithAuditLog == null) {
            this.hasFieldWithAuditLog = false;
            for (ModelField mf: this.fields) {
                if (mf.getEnableAuditLog()) {
                    this.hasFieldWithAuditLog = true;
                }
            }
            return this.hasFieldWithAuditLog;
        } else {
            return this.hasFieldWithAuditLog;
        }
    }

    /* Get the location of this entity's definition */
    public String getLocation() {
        return this.location;
    }

    /* Set the location of this entity's definition */
    public void setLocation(String location) {
        this.location = location;
    }

    /** An indicator to specify if this entity requires locking for updates */
    public boolean getDoLock() {
        return this.doLock;
    }

    public void setDoLock(boolean doLock) {
        this.doLock = doLock;
    }

    public boolean lock() {
        if (doLock && isField(STAMP_FIELD)) {
            return true;
        } else {
            doLock = false;
            return false;
        }
    }

    public Integer getSequenceBankSize() {
        return this.sequenceBankSize;
    }

    public void updatePkLists() {
        pks = FastList.newInstance();
        nopks = FastList.newInstance();

        for (ModelField field: fields) {
            if (field.isPk)
                pks.add(field);
            else
                nopks.add(field);
        }
    }

    public boolean isField(String fieldName) {
        if (fieldName == null) return false;
        for (ModelField field: fields) {
            if (field.name.equals(fieldName)) return true;
        }
        return false;
    }

    public boolean areFields(Collection<String> fieldNames) {
        if (fieldNames == null) return false;
        for (String fieldName: fieldNames) {
            if (!isField(fieldName)) return false;
        }
        return true;
    }

    public int getPksSize() {
        return this.pks.size();
    }

    public ModelField getOnlyPk() {
        if (this.pks.size() == 1) {
            return this.pks.get(0);
        } else {
            throw new IllegalArgumentException("Error in getOnlyPk, the [" + this.getEntityName() + "] entity has more than one pk!");
        }
    }

    public Iterator<ModelField> getPksIterator() {
        return this.pks.iterator();
    }

    public List<ModelField> getPkFieldsUnmodifiable() {
        return Collections.unmodifiableList(this.pks);
    }

    public String getFirstPkFieldName() {
        List<String> pkFieldNames = this.getPkFieldNames();
        String idFieldName = null;
        if (UtilValidate.isNotEmpty(pkFieldNames)) {
            idFieldName = pkFieldNames.get(0);
        }
        return idFieldName;
    }

    public int getNopksSize() {
        return this.nopks.size();
    }

    public Iterator<ModelField> getNopksIterator() {
        return this.nopks.iterator();
    }

    public List<ModelField> getNopksCopy() {
        List<ModelField> newList = FastList.newInstance();
        newList.addAll(this.nopks);
        return newList;
    }

    public int getFieldsSize() {
        return this.fields.size();
    }

    public Iterator<ModelField> getFieldsIterator() {
        return this.fields.iterator();
    }

    public List<ModelField> getFieldsUnmodifiable() {
        return Collections.unmodifiableList(this.fields);
    }

    /** The col-name of the Field, the alias of the field if this is on a view-entity */
    public String getColNameOrAlias(String fieldName) {
        ModelField modelField = this.getField(fieldName);
        String fieldString = modelField.getColName();
        return fieldString;
    }

    public ModelField getField(String fieldName) {
        if (fieldName == null) return null;
        if (fieldsMap == null) {
            createFieldsMap();
        }
        ModelField modelField = fieldsMap.get(fieldName);
        if (modelField == null) {
            // sometimes weird things happen and this getField method is called before the fields are all populated, so before moving on just re-create the fieldsMap again real quick...
            // the purpose of the fieldsMap is for speed, but if failures are a little slower, no biggie
            createFieldsMap();
            modelField = fieldsMap.get(fieldName);
        }
        return modelField;
    }

    protected synchronized void createFieldsMap() {
        Map<String, ModelField> tempMap = FastMap.newInstance();
        for (int i = 0; i < fields.size(); i++) {
            ModelField field = fields.get(i);
            tempMap.put(field.name, field);
        }
        fieldsMap = tempMap;
    }

    public void addField(ModelField field) {
        if (field == null) return;
        field.setModelEntity(this);
        this.fields.add(field);

        if (field.isPk) {
            pks.add(field);
        } else {
            nopks.add(field);
        }
    }

    public ModelField removeField(int index) {
        ModelField field = null;

        field = fields.remove(index);
        if (field == null) return null;

        if (field.isPk) {
            pks.remove(field);
        } else {
            nopks.remove(field);
        }
        return field;
    }

    public ModelField removeField(String fieldName) {
        if (fieldName == null) return null;
        ModelField field = null;

        // FIXME: when the field is removed, i is still incremented
        // while not correct, this doesn't cause any problems
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            if (field.name.equals(fieldName)) {
                fields.remove(i);
                if (field.isPk) {
                    pks.remove(field);
                } else {
                    nopks.remove(field);
                }
            }
            field = null;
        }
        return field;
    }

    public List<String> getAllFieldNames() {
        return getFieldNamesFromFieldVector(fields);
    }

    public List<String> getPkFieldNames() {
        return getFieldNamesFromFieldVector(pks);
    }

    public List<String> getNoPkFieldNames() {
        return getFieldNamesFromFieldVector(nopks);
    }

    public List<String> getFieldNamesFromFieldVector(ModelField... modelFields) {
        return getFieldNamesFromFieldVector(Arrays.asList(modelFields));
    }

    public List<String> getFieldNamesFromFieldVector(List<ModelField> modelFields) {
        List<String> nameList = FastList.newInstance();

        if (modelFields == null || modelFields.size() <= 0) return nameList;
        for (ModelField field: modelFields) {
            nameList.add(field.name);
        }
        return nameList;
    }

    public int getRelationsSize() {
        return this.relations.size();
    }

    public int getRelationsOneSize() {
        int numRels = 0;
        Iterator<ModelRelation> relationsIter = this.getRelationsIterator();
        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();
            if ("one".equals(modelRelation.getType())) {
                numRels++;
            }
        }
        return numRels;
    }

    public ModelRelation getRelation(int index) {
        return this.relations.get(index);
    }

    public Iterator<ModelRelation> getRelationsIterator() {
        return this.relations.iterator();
    }

    public List<ModelRelation> getRelationsList(boolean includeOne, boolean includeOneNoFk, boolean includeMany) {
        List<ModelRelation> relationsList = FastList.newInstance();
        Iterator<ModelRelation> allIter = this.getRelationsIterator();
        while (allIter.hasNext()) {
            ModelRelation modelRelation = allIter.next();
            if (includeOne && "one".equals(modelRelation.getType())) {
                relationsList.add(modelRelation);
            } else if (includeOneNoFk && "one-nofk".equals(modelRelation.getType())) {
                relationsList.add(modelRelation);
            } else if (includeMany && "many".equals(modelRelation.getType())) {
                relationsList.add(modelRelation);
            }
        }
        return relationsList;
    }

    public List<ModelRelation> getRelationsOneList() {
        return getRelationsList(true, true, false);
    }

    public List<ModelRelation> getRelationsManyList() {
        return getRelationsList(false, false, true);
    }

    public ModelRelation getRelation(String relationName) {
        if (relationName == null) return null;
        for (ModelRelation relation: relations) {
            if (relationName.equals(relation.title + relation.relEntityName)) return relation;
        }
        return null;
    }

    public void addRelation(ModelRelation relation) {
        relation.setModelEntity(this);
        this.relations.add(relation);
    }

    public ModelRelation removeRelation(int index) {
        return this.relations.remove(index);
    }

    public int getIndexesSize() {
        return this.indexes.size();
    }

    public ModelIndex getIndex(int index) {
        return this.indexes.get(index);
    }

    public Iterator<ModelIndex> getIndexesIterator() {
        return this.indexes.iterator();
    }

    public ModelIndex getIndex(String indexName) {
        if (indexName == null) return null;
        for (ModelIndex index: indexes) {
            if (indexName.equals(index.getName())) return index;
        }
        return null;
    }

    public void addIndex(ModelIndex index) {
        index.setModelEntity(this);
        this.indexes.add(index);
    }

    public ModelIndex removeIndex(int index) {
        return this.indexes.remove(index);
    }

    public int getViewEntitiesSize() {
        return this.viewEntities.size();
    }

    public Iterator<String> getViewConvertorsIterator() {
        return this.viewEntities.iterator();
    }

    public void addViewEntity(ModelViewEntity view) {
        this.viewEntities.add(view.getEntityName());
    }

    public List<? extends Map<String, Object>> convertToViewValues(String viewEntityName, GenericEntity entity) {
        if (entity == null || entity == GenericEntity.NULL_ENTITY || entity == GenericValue.NULL_VALUE) return UtilMisc.toList(entity);
        ModelViewEntity view = (ModelViewEntity) entity.getDelegator().getModelEntity(viewEntityName);
        return view.convert(getEntityName(), entity);
    }

    public boolean removeViewEntity(String viewEntityName) {
        return this.viewEntities.remove(viewEntityName);
    }

    public boolean removeViewEntity(ModelViewEntity viewEntity) {
       return removeViewEntity(viewEntity.getEntityName());
    }

    public String nameString(List<ModelField> flds) {
        return nameString(flds, ", ", "");
    }

    public String nameString(List<ModelField> flds, String separator, String afterLast) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(flds.get(i).name);
            returnString.append(separator);
        }
        returnString.append(flds.get(i).name);
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String typeNameString(ModelField... flds) {
        return typeNameString(Arrays.asList(flds));
    }

    public String typeNameString(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelField curField = flds.get(i);
            returnString.append(curField.type);
            returnString.append(" ");
            returnString.append(curField.name);
            returnString.append(", ");
        }
        ModelField curField = flds.get(i);
        returnString.append(curField.type);
        returnString.append(" ");
        returnString.append(curField.name);
        return returnString.toString();
    }

    public String fieldNameString() {
        return fieldNameString(", ", "");
    }

    public String fieldNameString(String separator, String afterLast) {
        return nameString(fields, separator, afterLast);
    }

    public String fieldTypeNameString() {
        return typeNameString(fields);
    }

    public String primKeyClassNameString() {
        return typeNameString(pks);
    }

    public String pkNameString() {
        return pkNameString(", ", "");
    }

    public String pkNameString(String separator, String afterLast) {
        return nameString(pks, separator, afterLast);
    }

    public String nonPkNullList() {
        return fieldsStringList(fields, "null", ", ", false, true);
    }

    public String fieldsStringList(String eachString, String separator, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, false, false);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator) {
        return fieldsStringList(flds, eachString, separator, false, false);
    }

    public String fieldsStringList(String eachString, String separator, boolean appendIndex, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, appendIndex, false);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex) {
        return fieldsStringList(flds, eachString, separator, appendIndex, false);
    }

    public String fieldsStringList(String eachString, String separator, boolean appendIndex, boolean onlyNonPK, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, appendIndex, onlyNonPK);
    }

    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex, boolean onlyNonPK) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size(); i++) {
            if (onlyNonPK && flds.get(i).isPk) continue;
            returnString.append(eachString);
            if (appendIndex) returnString.append(i + 1);
            if (i < flds.size() - 1) returnString.append(separator);
        }
        return returnString.toString();
    }

    public String colNameString(ModelField... flds) {
        return colNameString(Arrays.asList(flds));
    }

    public String colNameString(List<ModelField> flds) {
        return colNameString(flds, ", ", "", false);
    }

    public String colNameString(String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), separator, afterLast, alias);
    }

    public String colNameString(List<ModelField> flds, String separator, String afterLast, boolean alias) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        Iterator<ModelField> fldsIt = flds.iterator();
        while (fldsIt.hasNext()) {
            ModelField field = fldsIt.next();
            returnString.append(field.colName);
            if (fldsIt.hasNext()) {
                returnString.append(separator);
            }
        }

        returnString.append(afterLast);
        return returnString.toString();
    }

    public String classNameString(ModelField... flds) {
        return classNameString(Arrays.asList(flds));
    }

    public String classNameString(List<ModelField> flds) {
        return classNameString(flds, ", ", "");
    }

    public String classNameString(String separator, String afterLast, ModelField... flds) {
        return classNameString(Arrays.asList(flds), separator, afterLast);
    }

    public String classNameString(List<ModelField> flds, String separator, String afterLast) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append(separator);
        }
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String finderQueryString(ModelField... flds) {
        return finderQueryString(Arrays.asList(flds));
    }

    public String finderQueryString(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(flds.get(i).colName);
            returnString.append(" like {");
            returnString.append(i);
            returnString.append("} AND ");
        }
        returnString.append(flds.get(i).colName);
        returnString.append(" like {");
        returnString.append(i);
        returnString.append("}");
        return returnString.toString();
    }

    public String httpArgList(ModelField... flds) {
        return httpArgList(Arrays.asList(flds));
    }

    public String httpArgList(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).colName);
            returnString.append("=\" + ");
            returnString.append(flds.get(i).name);
            returnString.append(" + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).colName);
        returnString.append("=\" + ");
        returnString.append(flds.get(i).name);
        return returnString.toString();
    }

    public String httpArgListFromClass(ModelField... flds) {
        return httpArgListFromClass(Arrays.asList(flds));
    }

    public String httpArgListFromClass(List<ModelField> flds) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).colName);
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpArgListFromClass(String entityNameSuffix, ModelField... flds) {
        return httpArgListFromClass(Arrays.asList(flds), entityNameSuffix);
    }

    public String httpArgListFromClass(List<ModelField> flds, String entityNameSuffix) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(entityNameSuffix);
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).colName);
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(entityNameSuffix);
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpRelationArgList(ModelRelation relation, ModelField... flds) {
        return httpRelationArgList(Arrays.asList(flds), relation);
    }

    public String httpRelationArgList(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

            if (keyMap != null) {
                returnString.append("\"");
                returnString.append(tableName);
                returnString.append("_");
                returnString.append(flds.get(i).colName);
                returnString.append("=\" + ");
                returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
                returnString.append(".get");
                returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
                returnString.append("() + \"&\" + ");
            } else {
                Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type, module);
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

        if (keyMap != null) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
            returnString.append("()");
        } else {
            Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type, module);
        }
        return returnString.toString();
    }

    /*
     public String httpRelationArgList(ModelRelation relation) {
     String returnString = "";
     if (relation.keyMaps.size() < 1) { return ""; }

     int i = 0;
     for(; i < relation.keyMaps.size() - 1; i++) {
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     if (keyMap != null)
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "() + \"&\" + ";
     }
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "()";
     return returnString;
     }
     */
    public String typeNameStringRelatedNoMapped(ModelRelation relation, ModelField... flds) {
        return typeNameStringRelatedNoMapped(Arrays.asList(flds), relation);
    }

    public String typeNameStringRelatedNoMapped(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        if (relation.findKeyMapByRelated(flds.get(i).name) == null) {
            returnString.append(flds.get(i).type);
            returnString.append(" ");
            returnString.append(flds.get(i).name);
        }
        i++;
        for (; i < flds.size(); i++) {
            if (relation.findKeyMapByRelated(flds.get(i).name) == null) {
                if (returnString.length() > 0) returnString.append(", ");
                returnString.append(flds.get(i).type);
                returnString.append(" ");
                returnString.append(flds.get(i).name);
            }
        }
        return returnString.toString();
    }

    public String typeNameStringRelatedAndMain(ModelRelation relation, ModelField... flds) {
        return typeNameStringRelatedAndMain(Arrays.asList(flds), relation);
    }

    public String typeNameStringRelatedAndMain(List<ModelField> flds, ModelRelation relation) {
        StringBuilder returnString = new StringBuilder();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

            if (keyMap != null) {
                returnString.append(keyMap.fieldName);
                returnString.append(", ");
            } else {
                returnString.append(flds.get(i).name);
                returnString.append(", ");
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).name);

        if (keyMap != null) returnString.append(keyMap.fieldName);
        else returnString.append(flds.get(i).name);
        return returnString.toString();
    }

    public int compareTo(ModelEntity otherModelEntity) {

        /* This DOESN'T WORK, so forget it... using two passes
         //sort list by fk dependencies

         if (this.getEntityName().equals(otherModelEntity.getEntityName())) {
         return 0;
         }

         //look through relations for dependencies from this entity to the other
         Iterator relationsIter = this.getRelationsIterator();
         while (relationsIter.hasNext()) {
         ModelRelation modelRelation = (ModelRelation) relationsIter.next();

         if ("one".equals(modelRelation.getType()) && modelRelation.getRelEntityName().equals(otherModelEntity.getEntityName())) {
         //this entity is dependent on the other entity, so put that entity earlier in the list
         return -1;
         }
         }

         //look through relations for dependencies from the other to this entity
         Iterator otherRelationsIter = otherModelEntity.getRelationsIterator();
         while (otherRelationsIter.hasNext()) {
         ModelRelation modelRelation = (ModelRelation) otherRelationsIter.next();

         if ("one".equals(modelRelation.getType()) && modelRelation.getRelEntityName().equals(this.getEntityName())) {
         //the other entity is dependent on this entity, so put that entity later in the list
         return 1;
         }
         }

         return 0;
         */

        return this.getEntityName().compareTo(otherModelEntity.getEntityName());
    }

    public void convertFieldMapInPlace(Map<String, Object> inContext, Delegator delegator) {
        convertFieldMapInPlace(inContext, delegator.getModelFieldTypeReader(this));
    }
    public void convertFieldMapInPlace(Map<String, Object> inContext, ModelFieldTypeReader modelFieldTypeReader) {
        Iterator<ModelField> modelFields = this.getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String fieldName = modelField.getName();
            Object oldValue = inContext.get(fieldName);
            if (oldValue != null) {
                inContext.put(fieldName, this.convertFieldValue(modelField, oldValue, modelFieldTypeReader, inContext));
            }
        }
    }

    public Object convertFieldValue(String fieldName, Object value, Delegator delegator) {
        ModelField modelField = this.getField(fieldName);
        if (modelField == null) {
            String errMsg = "Could not convert field value: could not find an entity field for the name: [" + fieldName + "] on the [" + this.getEntityName() + "] entity.";
            throw new IllegalArgumentException(errMsg);
        }
        return convertFieldValue(modelField, value, delegator);
    }

    public Object convertFieldValue(ModelField modelField, Object value, Delegator delegator) {
        if (value == null || value == GenericEntity.NULL_FIELD) {
            return null;
        }
        String fieldJavaType = null;
        try {
            fieldJavaType = delegator.getEntityFieldType(this, modelField.getType()).getJavaType();
        } catch (GenericEntityException e) {
            String errMsg = "Could not convert field value: could not find Java type for the field: [" + modelField.getName() + "] on the [" + this.getEntityName() + "] entity: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
        try {
            return ObjectType.simpleTypeConvert(value, fieldJavaType, null, null, false);
        } catch (GeneralException e) {
            String errMsg = "Could not convert field value for the field: [" + modelField.getName() + "] on the [" + this.getEntityName() + "] entity to the [" + fieldJavaType + "] type for the value [" + value + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }

    /** Convert a field value from one Java data type to another. This is the preferred method -
     * which takes into consideration the user's locale and time zone (for conversions that
     * require them).
     * @return the converted value
     */
    public Object convertFieldValue(ModelField modelField, Object value, Delegator delegator, Map<String, ? extends Object> context) {
        ModelFieldTypeReader modelFieldTypeReader = delegator.getModelFieldTypeReader(this);
        return this.convertFieldValue(modelField, value, modelFieldTypeReader, context);
    }
    /** Convert a field value from one Java data type to another. This is the preferred method -
     * which takes into consideration the user's locale and time zone (for conversions that
     * require them).
     * @return the converted value
     */
    public Object convertFieldValue(ModelField modelField, Object value, ModelFieldTypeReader modelFieldTypeReader, Map<String, ? extends Object> context) {
        if (value == null || value == GenericEntity.NULL_FIELD) {
            return null;
        }
        String fieldJavaType = modelFieldTypeReader.getModelFieldType(modelField.getType()).getJavaType();
        try {
            return ObjectType.simpleTypeConvert(value, fieldJavaType, null, (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
        } catch (GeneralException e) {
            String errMsg = "Could not convert field value for the field: [" + modelField.getName() + "] on the [" + this.getEntityName() + "] entity to the [" + fieldJavaType + "] type for the value [" + value + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
    }

    /**
     * @return Returns the noAutoStamp.
     */
    public boolean getNoAutoStamp() {
        return this.noAutoStamp;
    }

    /**
     * @param noAutoStamp The noAutoStamp to set.
     */
    public void setNoAutoStamp(boolean noAutoStamp) {
        this.noAutoStamp = noAutoStamp;
    }

    @Override
    public String toString() {
        return "ModelEntity[" + getEntityName() + "]";
    }

    public Element toXmlElement(Document document, String packageName) {
        if (UtilValidate.isNotEmpty(this.getPackageName()) && !packageName.equals(this.getPackageName())) {
            Debug.logWarning("Export EntityModel XML Element [" + this.getEntityName() + "] with a NEW package - " + packageName, module);
        }

        Element root = document.createElement("entity");
        root.setAttribute("entity-name", this.getEntityName());
        if (!this.getEntityName().equals(ModelUtil.dbNameToClassName(this.getPlainTableName())) ||
                !ModelUtil.javaNameToDbName(this.getEntityName()).equals(this.getPlainTableName())) {
                root.setAttribute("table-name", this.getPlainTableName());
        }
        root.setAttribute("package-name", packageName);

        // additional elements
        if (UtilValidate.isNotEmpty(this.getDefaultResourceName())) {
            root.setAttribute("default-resource-name", this.getDefaultResourceName());
        }

        if (UtilValidate.isNotEmpty(this.getDependentOn())) {
            root.setAttribute("dependent-on", this.getDependentOn());
        }

        if (this.getDoLock()) {
            root.setAttribute("enable-lock", "true");
        }

        if (this.getNoAutoStamp()) {
            root.setAttribute("no-auto-stamp", "true");
        }

        if (this.getNeverCache()) {
            root.setAttribute("never-cache", "true");
        }

        if (!this.getAutoClearCache()) {
            root.setAttribute("auto-clear-cache", "false");
        }

        if (this.getSequenceBankSize() != null) {
            root.setAttribute("sequence-bank-size", this.getSequenceBankSize().toString());
        }

        if (UtilValidate.isNotEmpty(this.getTitle())) {
            root.setAttribute("title", this.getTitle());
        }

        if (UtilValidate.isNotEmpty(this.getCopyright())) {
            root.setAttribute("copyright", this.getCopyright());
        }

        if (UtilValidate.isNotEmpty(this.getAuthor())) {
            root.setAttribute("author", this.getAuthor());
        }

        if (UtilValidate.isNotEmpty(this.getVersion())) {
            root.setAttribute("version", this.getVersion());
        }

        // description element
        if (UtilValidate.isNotEmpty(this.getDescription())) {
            UtilXml.addChildElementValue(root, "description", this.getDescription(), document);
        }

        // append field elements
        Iterator<ModelField> fieldIter = this.getFieldsIterator();
        while (fieldIter != null && fieldIter.hasNext()) {
            ModelField field = fieldIter.next();
            if (!field.getIsAutoCreatedInternal()) {
                root.appendChild(field.toXmlElement(document));
            }
        }

        // append PK elements
        Iterator<ModelField> pkIter = this.getPksIterator();
        while (pkIter != null && pkIter.hasNext()) {
            ModelField pk = pkIter.next();
            Element pkey = document.createElement("prim-key");
            pkey.setAttribute("field", pk.getName());
            root.appendChild(pkey);
        }

        // append relation elements
        Iterator<ModelRelation> relIter = this.getRelationsIterator();
        while (relIter != null && relIter.hasNext()) {
            ModelRelation rel = relIter.next();
            root.appendChild(rel.toXmlElement(document));
        }

        // append index elements
        Iterator<ModelIndex> idxIter = this.getIndexesIterator();
        while (idxIter != null && idxIter.hasNext()) {
            ModelIndex idx = idxIter.next();
            root.appendChild(idx.toXmlElement(document));

        }

        return root;
    }

    public Element toXmlElement(Document document) {
        return this.toXmlElement(document, this.getPackageName());
    }

    /**
     * Writes entity model information in the Apple EOModelBundle format.
     *
     * For document structure and definition see: http://developer.apple.com/documentation/InternetWeb/Reference/WO_BundleReference/Articles/EOModelBundle.html
     *
     * For examples see the JavaRealEstate.framework and JavaBusinessLogic.framework packages which are in the /Library/Frameworks directory after installing the WebObjects Examples package (get latest version of WebObjects download for this).
     *
     * This is based on examples and documentation from WebObjects 5.4, downloaded 20080221.
     *
     * @param writer
     * @param entityPrefix
     * @param helperName
     */
    public void writeEoModelText(PrintWriter writer, String entityPrefix, String helperName, Set<String> entityNameIncludeSet, ModelReader entityModelReader) throws GenericEntityException {
        if (entityPrefix == null) entityPrefix = "";
        if (helperName == null) helperName = "localderby";

        UtilPlist.writePlistPropertyMap(this.createEoModelMap(entityPrefix, helperName, entityNameIncludeSet, entityModelReader), 0, writer, false);
    }


    public Map<String, Object> createEoModelMap(String entityPrefix, String helperName, Set<String> entityNameIncludeSet, ModelReader entityModelReader) throws GenericEntityException {
        final boolean useRelationshipNames = false;
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        Map<String, Object> topLevelMap = FastMap.newInstance();

        topLevelMap.put("name", this.getEntityName());
        topLevelMap.put("externalName", this.getTableName(helperName));
        topLevelMap.put("className", "EOGenericRecord");

        // for classProperties add field names AND relationship names to get a nice, complete chart
        List<String> classPropertiesList = FastList.newInstance();
        topLevelMap.put("classProperties", classPropertiesList);
        for (ModelField field: this.fields) {
            if (field.getIsAutoCreatedInternal()) continue;
            if (field.getIsPk()) {
                classPropertiesList.add(field.getName() + "*");
            } else {
                classPropertiesList.add(field.getName());
            }
        }
        for (ModelRelation relationship: this.relations) {
            if (!entityNameIncludeSet.contains(relationship.getRelEntityName())) continue;
            if (useRelationshipNames || relationship.isAutoRelation()) {
                classPropertiesList.add(relationship.getCombinedName());
            }
        }

        // attributes
        List<Map<String, Object>> attributesList = FastList.newInstance();
        topLevelMap.put("attributes", attributesList);
        for (ModelField field: this.fields) {
            if (field.getIsAutoCreatedInternal()) continue;

            ModelFieldType fieldType = modelFieldTypeReader.getModelFieldType(field.getType());

            Map<String, Object> attributeMap = FastMap.newInstance();
            attributesList.add(attributeMap);

            if (field.getIsPk()) {
                attributeMap.put("name", field.getName() + "*");
            } else {
                attributeMap.put("name", field.getName());
            }
            attributeMap.put("columnName", field.getColName());
            attributeMap.put("valueClassName", fieldType.getJavaType());

            String sqlType = fieldType.getSqlType();
            if (sqlType.indexOf("(") >= 0) {
                attributeMap.put("externalType", sqlType.substring(0, sqlType.indexOf("(")));
                // since there is a field length set that
                String widthStr = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"));
                // if there is a comma split by it for width,precision
                if (widthStr.indexOf(",") >= 0) {
                    attributeMap.put("width", widthStr.substring(0, widthStr.indexOf(",")));
                    // since there is a field precision set that
                    attributeMap.put("precision", widthStr.substring(widthStr.indexOf(",") + 1));
                } else {
                    attributeMap.put("width", widthStr);
                }
            } else {
                attributeMap.put("externalType", sqlType);
            }
        }

        // primaryKeyAttributes
        List<String> primaryKeyAttributesList = FastList.newInstance();
        topLevelMap.put("primaryKeyAttributes", primaryKeyAttributesList);
        for (ModelField pkField: this.pks) {
            primaryKeyAttributesList.add(pkField.getName());
        }

        // relationships
        List<Map<String, Object>> relationshipsMapList = FastList.newInstance();
        for (ModelRelation relationship: this.relations) {
            if (entityNameIncludeSet.contains(relationship.getRelEntityName())) {
                ModelEntity relEntity = entityModelReader.getModelEntity(relationship.getRelEntityName());

                Map<String, Object> relationshipMap = FastMap.newInstance();
                relationshipsMapList.add(relationshipMap);

                if (useRelationshipNames || relationship.isAutoRelation()) {
                    relationshipMap.put("name", relationship.getCombinedName());
                } else {
                    relationshipMap.put("name", relationship.getKeyMapsIterator().next().getFieldName());
                }
                relationshipMap.put("destination", relationship.getRelEntityName());
                if ("many".equals(relationship.getType())) {
                    relationshipMap.put("isToMany", "Y");
                    relationshipMap.put("isMandatory", "N");
                } else {
                    relationshipMap.put("isToMany", "N");
                    relationshipMap.put("isMandatory", "Y");
                }
                relationshipMap.put("joinSemantic", "EOInnerJoin");


                List<Map<String, Object>> joinsMapList = FastList.newInstance();
                relationshipMap.put("joins", joinsMapList);
                for (ModelKeyMap keyMap: relationship.getKeyMapsClone()) {
                    Map<String, Object> joinsMap = FastMap.newInstance();
                    joinsMapList.add(joinsMap);

                    ModelField thisField = this.getField(keyMap.getFieldName());
                    if (thisField != null && thisField.getIsPk()) {
                        joinsMap.put("sourceAttribute", keyMap.getFieldName() + "*");
                    } else {
                        joinsMap.put("sourceAttribute", keyMap.getFieldName());
                    }

                    ModelField relField = null;
                    if (relEntity != null) relField = relEntity.getField(keyMap.getRelFieldName());
                    if (relField != null && relField.getIsPk()) {
                        joinsMap.put("destinationAttribute", keyMap.getRelFieldName() + "*");
                    } else {
                        joinsMap.put("destinationAttribute", keyMap.getRelFieldName());
                    }
                }
            }
        }
        if (relationshipsMapList.size() > 0) {
            topLevelMap.put("relationships", relationshipsMapList);
        }

        return topLevelMap;
    }
}
