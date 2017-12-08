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
package org.apache.ofbiz.entity.model;

import java.io.PrintWriter;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilPlist;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.jdbc.DatabaseUtil;
import org.apache.ofbiz.entity.model.ModelIndex.Field;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;entity&gt;</code> element.
 *
 */
public class ModelEntity implements Comparable<ModelEntity>, Serializable {

    public static final String module = ModelEntity.class.getName();

    /** The name of the time stamp field for locking/synchronization */
    public static final String STAMP_FIELD = "lastUpdatedStamp";
    public static final String STAMP_TX_FIELD = "lastUpdatedTxStamp";
    public static final String CREATE_STAMP_FIELD = "createdStamp";
    public static final String CREATE_STAMP_TX_FIELD = "createdTxStamp";

    private ModelInfo modelInfo;

    /** The ModelReader that created this Entity */
    private final ModelReader modelReader;

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

    /** Synchronization object used to control access to the ModelField collection objects.
     * A single lock is used for all ModelField collections so collection updates are atomic. */
    private final Object fieldsLock = new Object();

    /** Model fields in the order they were defined. This list duplicates the values in fieldsMap, but
     *  we must keep the list in its original sequence for SQL DISTINCT operations to work properly. */
    private final List<ModelField> fieldsList = new ArrayList<>();

    private final Map<String, ModelField> fieldsMap = new HashMap<>();

    private final ArrayList<String> pkFieldNames = new ArrayList<>();

    /** A List of the Field objects for the Entity, one for each Primary Key */
    private final ArrayList<ModelField> pks = new ArrayList<>();

    /** A List of the Field objects for the Entity, one for each NON Primary Key */
    private final ArrayList<ModelField> nopks = new ArrayList<>();

    /** relations defining relationships between this entity and other entities */
    protected CopyOnWriteArrayList<ModelRelation> relations = new CopyOnWriteArrayList<>();

    /** indexes on fields/columns in this entity */
    private CopyOnWriteArrayList<ModelIndex> indexes = new CopyOnWriteArrayList<>();

    /** The reference of the dependentOn entity model */
    protected ModelEntity specializationOfModelEntity = null;

    /** The list of entities that are specialization of on this entity */
    protected Map<String, ModelEntity> specializedEntities = new HashMap<>();

    /** map of ModelViewEntities that references this model */
    private final Set<String> viewEntities = new HashSet<>();

    /** An indicator to specify if this entity requires locking for updates */
    protected boolean doLock = false;

    /** Can be used to disable automatically creating update stamp fields and populating them on inserts and updates */
    protected boolean noAutoStamp = false;

    /** An indicator to specify if this entity is never cached.
     * If true causes the delegator to not clear caches on write and to not get
     * from cache on read showing a warning messages to that effect
     */
    protected boolean neverCache = false;

    protected boolean neverCheck = false;

    protected boolean autoClearCache = true;

    /** The location of this entity's definition */
    protected String location = "";

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelEntity() {
        this.modelReader = null;
        this.modelInfo = ModelInfo.DEFAULT;
    }

    protected ModelEntity(ModelReader reader) {
        this.modelReader = reader;
        this.modelInfo = ModelInfo.DEFAULT;
    }

    protected ModelEntity(ModelReader reader, ModelInfo modelInfo) {
        this.modelReader = reader;
        this.modelInfo = modelInfo;
    }

    /** XML Constructor */
    protected ModelEntity(ModelReader reader, Element entityElement, ModelInfo modelInfo) {
        this.modelReader = reader;
        this.modelInfo = ModelInfo.createFromAttributes(modelInfo, entityElement);
    }

    /** XML Constructor */
    public ModelEntity(ModelReader reader, Element entityElement, UtilTimer utilTimer, ModelInfo modelInfo) {
        this.modelReader = reader;
        this.modelInfo = ModelInfo.createFromAttributes(modelInfo, entityElement);
        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before general/basic info");
        this.populateBasicInfo(entityElement);
        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before prim-keys");
        for (Element pkElement: UtilXml.childElementList(entityElement, "prim-key")) {
            pkFieldNames.add(pkElement.getAttribute("field").intern());
        }
        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before fields");
        for (Element fieldElement: UtilXml.childElementList(entityElement, "field")) {
            String fieldName = UtilXml.checkEmpty(fieldElement.getAttribute("name")).intern();
            boolean isPk = pkFieldNames.contains(fieldName);
            ModelField field = ModelField.create(this, fieldElement, isPk);
            internalAddField(field, pkFieldNames);
        }
        // if applicable automatically add the STAMP_FIELD and STAMP_TX_FIELD fields
        if ((this.doLock || !this.noAutoStamp) && !fieldsMap.containsKey(STAMP_FIELD)) {
            ModelField newField = ModelField.create(this, "", STAMP_FIELD, "date-time", null, null, null, false, false, false, true, false, null);
            internalAddField(newField, pkFieldNames);
        }
        if (!this.noAutoStamp && !fieldsMap.containsKey(STAMP_TX_FIELD)) {
            ModelField newField = ModelField.create(this, "", STAMP_TX_FIELD, "date-time", null, null, null, false, false, false, true, false, null);
            internalAddField(newField, pkFieldNames);
            // also add an index for this field
            String indexName = ModelUtil.shortenDbName(this.tableName + "_TXSTMP", 18);
            Field indexField = new Field(STAMP_TX_FIELD, null);
            ModelIndex txIndex = ModelIndex.create(this, null, indexName, UtilMisc.toList(indexField), false);
            indexes.add(txIndex);
        }
        // if applicable automatically add the CREATE_STAMP_FIELD and CREATE_STAMP_TX_FIELD fields
        if ((this.doLock || !this.noAutoStamp) && !fieldsMap.containsKey(CREATE_STAMP_FIELD)) {
            ModelField newField = ModelField.create(this, "", CREATE_STAMP_FIELD, "date-time", null, null, null, false, false, false, true, false, null);
            internalAddField(newField, pkFieldNames);
        }
        if (!this.noAutoStamp && !fieldsMap.containsKey(CREATE_STAMP_TX_FIELD)) {
            ModelField newField = ModelField.create(this, "", CREATE_STAMP_TX_FIELD, "date-time", null, null, null, false, false, false, true, false, null);
            internalAddField(newField, pkFieldNames);
            // also add an index for this field
            String indexName = ModelUtil.shortenDbName(this.tableName + "_TXCRTS", 18);
            Field indexField = new Field(CREATE_STAMP_TX_FIELD, null);
            ModelIndex txIndex = ModelIndex.create(this, null, indexName, UtilMisc.toList(indexField), false);
            indexes.add(txIndex);
        }
        // Must be done last to preserve pk field sequence
        for (String pkFieldName : pkFieldNames) {
            ModelField pkField = fieldsMap.get(pkFieldName);
            if (pkField == null) {
                Debug.logWarning("Error in entity definition - primary key is invalid for entity " + this.getEntityName(), module);
            } else {
                pks.add(pkField);
            }
        }
        pkFieldNames.trimToSize();
        pks.trimToSize();
        nopks.trimToSize();
        reader.incrementFieldCount(fieldsMap.size());
        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);
        this.populateIndexes(entityElement);
    }

    /** DB Names Constructor */
    public ModelEntity(String tableName, Map<String, DatabaseUtil.ColumnCheckInfo> colMap, ModelFieldTypeReader modelFieldTypeReader, boolean isCaseSensitive) {
        // if there is a dot in the name, remove it and everything before it, should be the schema name
        this.modelReader = null;
        this.modelInfo = ModelInfo.DEFAULT;
        this.tableName = tableName;
        int dotIndex = this.tableName.indexOf('.');
        if (dotIndex >= 0) {
            this.tableName = this.tableName.substring(dotIndex + 1);
        }
        this.entityName = ModelUtil.dbNameToClassName(this.tableName);
        for (Map.Entry<String, DatabaseUtil.ColumnCheckInfo> columnEntry : colMap.entrySet()) {
            DatabaseUtil.ColumnCheckInfo ccInfo = columnEntry.getValue();
            ModelField newField = ModelField.create(this, ccInfo, modelFieldTypeReader);
            addField(newField);
        }
    }

    protected void populateBasicInfo(Element entityElement) {
        this.entityName = UtilXml.checkEmpty(entityElement.getAttribute("entity-name")).intern();
        this.tableName = UtilXml.checkEmpty(entityElement.getAttribute("table-name"), ModelUtil.javaNameToDbName(this.entityName)).intern();
        this.packageName = UtilXml.checkEmpty(entityElement.getAttribute("package-name")).intern();
        this.dependentOn = UtilXml.checkEmpty(entityElement.getAttribute("dependent-on")).intern();
        this.doLock = UtilXml.checkBoolean(entityElement.getAttribute("enable-lock"), false);
        this.noAutoStamp = UtilXml.checkBoolean(entityElement.getAttribute("no-auto-stamp"), false);
        this.neverCache = UtilXml.checkBoolean(entityElement.getAttribute("never-cache"), false);
        this.neverCheck = UtilXml.checkBoolean(entityElement.getAttribute("never-check"), false);
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

    private void internalAddField(ModelField newField, List<String> pkFieldNames) {
        if (!newField.getIsPk()) {
            this.nopks.add(newField);
        }
        this.fieldsList.add(newField);
        this.fieldsMap.put(newField.getName(), newField);
    }

    protected void populateRelated(ModelReader reader, Element entityElement) {
        List<ModelRelation> tempList = new ArrayList<>(this.relations);
        for (Element relationElement: UtilXml.childElementList(entityElement, "relation")) {
            ModelRelation relation = reader.createRelation(this, relationElement);
            if (relation != null) {
                tempList.add(relation);
            }
        }
        this.relations = new CopyOnWriteArrayList<>(tempList);
    }


    protected void populateIndexes(Element entityElement) {
        List<ModelIndex> tempList = new ArrayList<>(this.indexes);
        for (Element indexElement: UtilXml.childElementList(entityElement, "index")) {
            ModelIndex index = ModelIndex.create(this, indexElement);
            tempList.add(index);
        }
        this.indexes = new CopyOnWriteArrayList<>(tempList);
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
        if (extendEntityElement.hasAttribute("enable-lock")) {
            this.doLock = UtilXml.checkBoolean(extendEntityElement.getAttribute("enable-lock"), false);
        }

        if (extendEntityElement.hasAttribute("no-auto-stamp")) {
            this.noAutoStamp = UtilXml.checkBoolean(extendEntityElement.getAttribute("no-auto-stamp"), false);
        }

        if (extendEntityElement.hasAttribute("auto-clear-cache")) {
            this.autoClearCache = UtilXml.checkBoolean(extendEntityElement.getAttribute("auto-clear-cache"), false);
        }

        if (extendEntityElement.hasAttribute("never-cache")) {
            this.neverCache = UtilXml.checkBoolean(extendEntityElement.getAttribute("never-cache"), false);
        }

        if (extendEntityElement.hasAttribute("sequence-bank-size")) {
            String sequenceBankSizeStr = UtilXml.checkEmpty(extendEntityElement.getAttribute("sequence-bank-size"));
            if (UtilValidate.isNotEmpty(sequenceBankSizeStr)) {
                try {
                    this.sequenceBankSize = Integer.valueOf(sequenceBankSizeStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Error parsing sequence-bank-size value [" + sequenceBankSizeStr + "] for entity [" + this.entityName + "]", module);
                }
            }
        }

        for (Element fieldElement : UtilXml.childElementList(extendEntityElement, "field")) {
            ModelField newField = ModelField.create(this, fieldElement, false);
            ModelField existingField = this.getField(newField.getName());
            if (existingField != null) {
                // override the existing field's attributes
                // TODO: only overrides of type, colName, description and enable-audit-log are currently supported
                String type = existingField.getType();
                if (!newField.getType().isEmpty()) {
                    type = newField.getType();
                }
                String colName = existingField.getColName();
                if (!newField.getColName().isEmpty()) {
                    colName = newField.getColName();
                }
                String description = existingField.getDescription();
                if (!newField.getDescription().isEmpty()) {
                    description = newField.getDescription();
                }
                boolean enableAuditLog = existingField.getEnableAuditLog();
                if (UtilValidate.isNotEmpty(fieldElement.getAttribute("enable-audit-log"))) {
                    enableAuditLog = "true".equals(fieldElement.getAttribute("enable-audit-log"));
                }
                newField = ModelField.create(this, description, existingField.getName(), type, colName, existingField.getColValue(), existingField.getFieldSet(),
                        existingField.getIsNotNull(), existingField.getIsPk(), existingField.getEncryptMethod(), existingField.getIsAutoCreatedInternal(),
                        enableAuditLog, existingField.getValidators());
            }
            // add to the entity as a new field
            synchronized (fieldsLock) {
                if (existingField != null) {
                    this.fieldsList.remove(existingField);
                }
                this.fieldsList.add(newField);
                this.fieldsMap.put(newField.getName(), newField);
                if (!newField.getIsPk()) {
                    if (existingField != null) {
                        this.nopks.remove(existingField);
                    }
                    this.nopks.add(newField);
                } else {
                    if (existingField != null) {
                        this.pks.remove(existingField);
                    }
                    this.pks.add(newField);
                    if (!this.pkFieldNames.contains(newField.getName())) {
                        this.pkFieldNames.add(newField.getName());
                    }
                }
            }
        }
        this.modelInfo = ModelInfo.createFromAttributes(this.modelInfo, extendEntityElement);
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
        return getTableName(EntityConfig.getDatasource(helperName));
    }

    /** The table-name of the Entity including a Schema name if specified in the datasource config */
    public String getTableName(Datasource datasourceInfo) {
        if (datasourceInfo != null && UtilValidate.isNotEmpty(datasourceInfo.getSchemaName())) {
            return datasourceInfo.getSchemaName() + "." + this.tableName;
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

    /**
     * An indicator to specific if this entity should ignore automatic DB checks.
     * This should be set when the entity is mapped to a database view to prevent
     * warnings and attempts to modify the schema.     
     */
    public boolean getNeverCheck() {
        return neverCheck;
    }

    public void setNeverCheck(boolean neverCheck) {
        this.neverCheck = neverCheck;
    }

    public boolean getAutoClearCache() {
        return this.autoClearCache;
    }

    public void setAutoClearCache(boolean autoClearCache) {
        this.autoClearCache = autoClearCache;
    }

    public boolean getHasFieldWithAuditLog() {
        for (ModelField mf : getFieldsUnmodifiable()) {
            if (mf.getEnableAuditLog()) {
                return true;
            }
        }
        return false;
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

    public boolean isField(String fieldName) {
        if (fieldName == null) return false;
        synchronized (fieldsLock) {
            return fieldsMap.containsKey(fieldName);
        }
    }

    public boolean areFields(Collection<String> fieldNames) {
        if (fieldNames == null) return false;
        for (String fieldName: fieldNames) {
            if (!isField(fieldName)) return false;
        }
        return true;
    }

    public int getPksSize() {
        synchronized (fieldsLock) {
            return this.pks.size();
        }
    }

    public ModelField getOnlyPk() {
        synchronized (fieldsLock) {
            if (this.pks.size() == 1) {
                return this.pks.get(0);
            } else {
                throw new IllegalArgumentException("Error in getOnlyPk, the [" + this.getEntityName() + "] entity has more than one pk!");
            }
        }
    }

    public Iterator<ModelField> getPksIterator() {
        return getPkFields().iterator();
    }

    public List<ModelField> getPkFields() {
        synchronized (fieldsLock) {
            return new ArrayList<>(this.pks);
        }
    }

    public List<ModelField> getPkFieldsUnmodifiable() {
        return Collections.unmodifiableList(getPkFields());
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
        synchronized (fieldsLock) {
            return this.nopks.size();
        }
    }

    public Iterator<ModelField> getNopksIterator() {
        return getNopksCopy().iterator();
    }

    public List<ModelField> getNopksCopy() {
        synchronized (fieldsLock) {
            return new ArrayList<>(this.nopks);
        }
    }

    public int getFieldsSize() {
        synchronized (fieldsLock) {
            return this.fieldsList.size();
        }
    }

    public Iterator<ModelField> getFieldsIterator() {
        synchronized (fieldsLock) {
            List<ModelField> newList = new ArrayList<>(this.fieldsList);
            return newList.iterator();
        }
    }

    public List<ModelField> getFieldsUnmodifiable() {
        synchronized (fieldsLock) {
            List<ModelField> newList = new ArrayList<>(this.fieldsList);
            return Collections.unmodifiableList(newList);
        }
    }

    /** The col-name of the Field, the alias of the field if this is on a view-entity */
    public String getColNameOrAlias(String fieldName) {
        ModelField modelField = this.getField(fieldName);
        String fieldString = modelField.getColName();
        return fieldString;
    }

    public ModelField getField(String fieldName) {
        if (fieldName == null) return null;
        synchronized (fieldsLock) {
            return fieldsMap.get(fieldName);
        }
    }

    public void addField(ModelField field) {
        if (field == null)
            return;
        synchronized (fieldsLock) {
            this.fieldsList.add(field);
            fieldsMap.put(field.getName(), field);
            if (field.getIsPk()) {
                pks.add(field);
                if (!pkFieldNames.contains(field.getName())) {
                    pkFieldNames.add(field.getName());
                }
            } else {
                nopks.add(field);
            }
        }
    }

    public ModelField removeField(String fieldName) {
        if (fieldName == null)
            return null;
        synchronized (fieldsLock) {
            ModelField field = fieldsMap.remove(fieldName);
            if (field != null) {
                this.fieldsList.remove(field);
                if (field.getIsPk()) {
                    pks.remove(field);
                    pkFieldNames.remove(field.getName());
                } else {
                    nopks.remove(field);
                }
            }
            return field;
        }
    }

    public List<String> getAllFieldNames() {
        synchronized (fieldsLock) {
            return new ArrayList<>(this.fieldsMap.keySet());
        }
    }

    public List<String> getPkFieldNames() {
        synchronized (fieldsLock) {
            return new ArrayList<>(pkFieldNames);
        }
    }

    public List<String> getNoPkFieldNames() {
        return getFieldNamesFromFieldVector(getNopksCopy());
    }

    private List<String> getFieldNamesFromFieldVector(List<ModelField> modelFields) {
        List<String> nameList = new ArrayList<>(modelFields.size());
        for (ModelField field: modelFields) {
            nameList.add(field.getName());
        }
        return nameList;
    }

    /**
     * @return field names list, managed by entity-engine
     */
    public List<String> getAutomaticFieldNames() {
        List<String> nameList = new LinkedList<>();
        if (! this.noAutoStamp) {
            nameList.add(STAMP_FIELD);
            nameList.add(STAMP_TX_FIELD);
            nameList.add(CREATE_STAMP_FIELD);
            nameList.add(CREATE_STAMP_TX_FIELD);
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
        List<ModelRelation> relationsList = new LinkedList<>();
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
            if (relationName.equals(relation.getTitle() + relation.getRelEntityName())) return relation;
        }
        return null;
    }

    public void addRelation(ModelRelation relation) {
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
        this.indexes.add(index);
    }

    public ModelIndex removeIndex(int index) {
        return this.indexes.remove(index);
    }

    public int getViewEntitiesSize() {
        synchronized (viewEntities) {
            return this.viewEntities.size();
        }
    }

    public Iterator<String> getViewConvertorsIterator() {
        synchronized (viewEntities) {
            return new HashSet<>(this.viewEntities).iterator();
        }
    }

    public void addViewEntity(ModelViewEntity view) {
        synchronized (viewEntities) {
            this.viewEntities.add(view.getEntityName());
        }
    }

    public List<? extends Map<String, Object>> convertToViewValues(String viewEntityName, GenericEntity entity) {
        if (entity == null || entity == GenericEntity.NULL_ENTITY || entity == GenericValue.NULL_VALUE) return UtilMisc.toList(entity);
        ModelViewEntity view = (ModelViewEntity) entity.getDelegator().getModelEntity(viewEntityName);
        return view.convert(getEntityName(), entity);
    }

    public boolean removeViewEntity(String viewEntityName) {
        synchronized (viewEntities) {
            return this.viewEntities.remove(viewEntityName);
        }
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
            returnString.append(flds.get(i).getName());
            returnString.append(separator);
        }
        returnString.append(flds.get(i).getName());
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
            returnString.append(curField.getType());
            returnString.append(" ");
            returnString.append(curField.getName());
            returnString.append(", ");
        }
        ModelField curField = flds.get(i);
        returnString.append(curField.getType());
        returnString.append(" ");
        returnString.append(curField.getName());
        return returnString.toString();
    }

    public String fieldNameString() {
        return fieldNameString(", ", "");
    }

    public String fieldNameString(String separator, String afterLast) {
        return nameString(getFieldsUnmodifiable(), separator, afterLast);
    }

    public String fieldTypeNameString() {
        return typeNameString(getFieldsUnmodifiable());
    }

    public String primKeyClassNameString() {
        return typeNameString(getPkFields());
    }

    public String pkNameString() {
        return pkNameString(", ", "");
    }

    public String pkNameString(String separator, String afterLast) {
        return nameString(getPkFields(), separator, afterLast);
    }

    public String nonPkNullList() {
        return fieldsStringList(getFieldsUnmodifiable(), "null", ", ", false, true);
    }

    @Deprecated
    public String fieldsStringList(String eachString, String separator, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, false, false);
    }

    public StringBuilder fieldsStringList(StringBuilder sb, String eachString, String separator, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), sb, eachString, separator, false, false);
    }

    @Deprecated
    public String fieldsStringList(List<ModelField> flds, String eachString, String separator) {
        return fieldsStringList(flds, eachString, separator, false, false);
    }

    public StringBuilder fieldsStringList(List<ModelField> flds, StringBuilder sb, String eachString, String separator) {
        return fieldsStringList(flds, sb, eachString, separator, false, false);
    }

    @Deprecated
    public String fieldsStringList(String eachString, String separator, boolean appendIndex, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, appendIndex, false);
    }

    public StringBuilder fieldsStringList(StringBuilder sb, String eachString, String separator, boolean appendIndex, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), sb, eachString, separator, appendIndex, false);
    }

    @Deprecated
    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex) {
        return fieldsStringList(flds, eachString, separator, appendIndex, false);
    }

    public StringBuilder fieldsStringList(List<ModelField> flds, StringBuilder sb, String eachString, String separator, boolean appendIndex) {
        return fieldsStringList(flds, sb, eachString, separator, appendIndex, false);
    }

    @Deprecated
    public String fieldsStringList(String eachString, String separator, boolean appendIndex, boolean onlyNonPK, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), eachString, separator, appendIndex, onlyNonPK);
    }

    public StringBuilder fieldsStringList(StringBuilder sb, String eachString, String separator, boolean appendIndex, boolean onlyNonPK, ModelField... flds) {
        return fieldsStringList(Arrays.asList(flds), sb, eachString, separator, appendIndex, onlyNonPK);
    }

    @Deprecated
    public String fieldsStringList(List<ModelField> flds, String eachString, String separator, boolean appendIndex, boolean onlyNonPK) {
        return fieldsStringList(flds, new StringBuilder(), eachString, separator, appendIndex, onlyNonPK).toString();
    }

    public StringBuilder fieldsStringList(List<ModelField> flds, StringBuilder sb, String eachString, String separator, boolean appendIndex, boolean onlyNonPK) {
        if (flds.size() < 1) {
            return sb;
        }

        int i = 0;

        for (; i < flds.size(); i++) {
            if (onlyNonPK && flds.get(i).getIsPk()) continue;
            sb.append(eachString);
            if (appendIndex) sb.append(i + 1);
            if (i < flds.size() - 1) sb.append(separator);
        }
        return sb;
    }

    @Deprecated
    public String colNameString(ModelField... flds) {
        return colNameString(new StringBuilder(), "", flds).toString();
    }

    public StringBuilder colNameString(StringBuilder sb, String prefix,  ModelField... flds) {
        return colNameString(Arrays.asList(flds), sb, prefix);
    }

    @Deprecated
    public String colNameString(List<ModelField> flds) {
        return colNameString(flds, new StringBuilder(), "", ", ", "", false).toString();
    }

    public StringBuilder colNameString(List<ModelField> flds, StringBuilder sb, String prefix) {
        return colNameString(flds, sb, prefix, ", ", "", false);
    }

    @Deprecated
    public String colNameString(String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), new StringBuilder(), "", separator, afterLast, alias).toString();
    }

    public StringBuilder colNameString(StringBuilder sb, String prefix, String separator, String afterLast, boolean alias, ModelField... flds) {
        return colNameString(Arrays.asList(flds), sb, prefix, separator, afterLast, alias);
    }

    @Deprecated
    public String colNameString(List<ModelField> flds, String separator, String afterLast, boolean alias) {
        return colNameString(flds, new StringBuilder(), "", separator, afterLast, alias).toString();
    }

    public StringBuilder colNameString(List<ModelField> flds, StringBuilder sb, String prefix, String separator, String afterLast, boolean alias) {
        if (flds.size() < 1) {
            return sb;
        }

        sb.append(prefix);
        Iterator<ModelField> fldsIt = flds.iterator();
        while (fldsIt.hasNext()) {
            ModelField field = fldsIt.next();
            sb.append(field.getColName());
            if (fldsIt.hasNext()) {
                sb.append(separator);
            }
        }

        sb.append(afterLast);
        return sb;
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
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
            returnString.append(separator);
        }
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
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
            returnString.append(flds.get(i).getColName());
            returnString.append(" like {");
            returnString.append(i);
            returnString.append("} AND ");
        }
        returnString.append(flds.get(i).getColName());
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
            returnString.append(flds.get(i).getColName());
            returnString.append("=\" + ");
            returnString.append(flds.get(i).getName());
            returnString.append(" + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getColName());
        returnString.append("=\" + ");
        returnString.append(flds.get(i).getName());
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
            returnString.append(flds.get(i).getColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getColName());
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
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
            returnString.append(flds.get(i).getColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(entityNameSuffix);
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(flds.get(i).getColName());
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(entityNameSuffix);
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(flds.get(i).getName()));
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
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).getName());

            if (keyMap != null) {
                returnString.append("\"");
                returnString.append(tableName);
                returnString.append("_");
                returnString.append(flds.get(i).getColName());
                returnString.append("=\" + ");
                returnString.append(ModelUtil.lowerFirstChar(relation.getModelEntity().entityName));
                returnString.append(".get");
                returnString.append(ModelUtil.upperFirstChar(keyMap.getFieldName()));
                returnString.append("() + \"&\" + ");
            } else {
                Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).getName() + " related entity: " + relation.getRelEntityName() + " main entity: " + relation.getModelEntity().entityName + " type: " + relation.getType(), module);
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).getName());

        if (keyMap != null) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(flds.get(i).getColName());
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(relation.getModelEntity().entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(keyMap.getFieldName()));
            returnString.append("()");
        } else {
            Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + flds.get(i).getName() + " related entity: " + relation.getRelEntityName() + " main entity: " + relation.getModelEntity().entityName + " type: " + relation.getType(), module);
        }
        return returnString.toString();
    }

    /*
     public String httpRelationArgList(ModelRelation relation) {
     String returnString = "";
     if (relation.keyMaps.size() < 1) { return ""; }

     int i = 0;
     for (; i < relation.keyMaps.size() - 1; i++) {
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

        if (relation.findKeyMapByRelated(flds.get(i).getName()) == null) {
            returnString.append(flds.get(i).getType());
            returnString.append(" ");
            returnString.append(flds.get(i).getName());
        }
        i++;
        for (; i < flds.size(); i++) {
            if (relation.findKeyMapByRelated(flds.get(i).getName()) == null) {
                if (returnString.length() > 0) returnString.append(", ");
                returnString.append(flds.get(i).getType());
                returnString.append(" ");
                returnString.append(flds.get(i).getName());
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
            ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).getName());

            if (keyMap != null) {
                returnString.append(keyMap.getFieldName());
                returnString.append(", ");
            } else {
                returnString.append(flds.get(i).getName());
                returnString.append(", ");
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(flds.get(i).getName());

        if (keyMap != null) returnString.append(keyMap.getFieldName());
        else returnString.append(flds.get(i).getName());
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

        if (this.getNeverCheck()) {
            root.setAttribute("never-check", "true");
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
        while (fieldIter.hasNext()) {
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
        while (relIter.hasNext()) {
            ModelRelation rel = relIter.next();
            root.appendChild(rel.toXmlElement(document));
        }

        // append index elements
        Iterator<ModelIndex> idxIter = this.getIndexesIterator();
        while (idxIter.hasNext()) {
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

        Map<String, Object> topLevelMap = new HashMap<>();

        topLevelMap.put("name", this.getEntityName());
        topLevelMap.put("externalName", this.getTableName(helperName));
        topLevelMap.put("className", "EOGenericRecord");

        // for classProperties add field names AND relationship names to get a nice, complete chart
        List<String> classPropertiesList = new LinkedList<>();
        topLevelMap.put("classProperties", classPropertiesList);
        for (ModelField field: this.fieldsList) {
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
        List<Map<String, Object>> attributesList = new LinkedList<>();
        topLevelMap.put("attributes", attributesList);
        for (ModelField field: this.fieldsList) {
            if (field.getIsAutoCreatedInternal()) continue;

            ModelFieldType fieldType = modelFieldTypeReader.getModelFieldType(field.getType());

            Map<String, Object> attributeMap = new HashMap<>();
            attributesList.add(attributeMap);

            if (field.getIsPk()) {
                attributeMap.put("name", field.getName() + "*");
            } else {
                attributeMap.put("name", field.getName());
            }
            attributeMap.put("columnName", field.getColName());
            attributeMap.put("valueClassName", fieldType.getJavaType());

            String sqlType = fieldType.getSqlType();
            if (sqlType.indexOf('(') >= 0) {
                attributeMap.put("externalType", sqlType.substring(0, sqlType.indexOf('(')));
                // since there is a field length set that
                String widthStr = sqlType.substring(sqlType.indexOf('(') + 1, sqlType.indexOf(')'));
                // if there is a comma split by it for width,precision
                if (widthStr.indexOf(',') >= 0) {
                    attributeMap.put("width", widthStr.substring(0, widthStr.indexOf(',')));
                    // since there is a field precision set that
                    attributeMap.put("precision", widthStr.substring(widthStr.indexOf(',') + 1));
                } else {
                    attributeMap.put("width", widthStr);
                }
            } else {
                attributeMap.put("externalType", sqlType);
            }
        }

        // primaryKeyAttributes
        List<String> primaryKeyAttributesList = new LinkedList<>();
        topLevelMap.put("primaryKeyAttributes", primaryKeyAttributesList);
        for (ModelField pkField : getPkFields()) {
            primaryKeyAttributesList.add(pkField.getName());
        }

        // relationships
        List<Map<String, Object>> relationshipsMapList = new LinkedList<>();
        for (ModelRelation relationship: this.relations) {
            if (entityNameIncludeSet.contains(relationship.getRelEntityName())) {
                ModelEntity relEntity = entityModelReader.getModelEntity(relationship.getRelEntityName());

                Map<String, Object> relationshipMap = new HashMap<>();
                relationshipsMapList.add(relationshipMap);

                if (useRelationshipNames || relationship.isAutoRelation()) {
                    relationshipMap.put("name", relationship.getCombinedName());
                } else {
                    relationshipMap.put("name", relationship.getKeyMaps().iterator().next().getFieldName());
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


                List<Map<String, Object>> joinsMapList = new LinkedList<>();
                relationshipMap.put("joins", joinsMapList);
                for (ModelKeyMap keyMap: relationship.getKeyMaps()) {
                    Map<String, Object> joinsMap = new HashMap<>();
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

    public String getAuthor() {
        return modelInfo.getAuthor();
    }

    public String getCopyright() {
        return modelInfo.getCopyright();
    }

    public String getDefaultResourceName() {
        return modelInfo.getDefaultResourceName();
    }

    public String getDescription() {
        return modelInfo.getDescription();
    }

    public String getTitle() {
        return modelInfo.getTitle();
    }

    public String getVersion() {
        return modelInfo.getVersion();
    }

}
