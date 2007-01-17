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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.DatabaseUtil;

/**
 * Generic Entity - Entity model class
 *
 */
public class ModelEntity extends ModelInfo implements Comparable, Serializable {

    public static final String module = ModelEntity.class.getName();

    /** The name of the time stamp field for locking/syncronization */
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

    /** The default-resource-name of the Entity, used with the getResource call to check for a value in a resource bundle */
    protected String defaultResourceName = "";

    /** The entity-name of the Entity that this Entity is dependent on, if empty then no dependency */
    protected String dependentOn = "";

    /** A List of the Field objects for the Entity */
    protected List fields = FastList.newInstance();
    protected Map fieldsMap = null;

    /** A List of the Field objects for the Entity, one for each Primary Key */
    protected List pks = FastList.newInstance();

    /** A List of the Field objects for the Entity, one for each NON Primary Key */
    protected List nopks = FastList.newInstance();

    /** relations defining relationships between this entity and other entities */
    protected List relations = FastList.newInstance();

    /** indexes on fields/columns in this entity */
    protected List indexes = FastList.newInstance();

    /** map of ModelViewEntities that references this model */
    protected Map viewEntities = FastMap.newInstance();

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

    // ===== CONSTRUCTORS =====
    /** Default Constructor */
    public ModelEntity() {}

    /** XML Constructor */
    protected ModelEntity(ModelReader reader, Element entityElement, ModelInfo def) {
        super(def);
        populateFromAttributes(entityElement);
        this.modelReader = reader;
    }

    /** XML Constructor */
    public ModelEntity(ModelReader reader, Element entityElement, UtilTimer utilTimer, ModelInfo def) {
        this(reader, entityElement, def);

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before general/basic info");
        this.populateBasicInfo(entityElement);

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before fields");
        List fieldElementList = UtilXml.childElementList(entityElement, "field");
        Iterator fieldElementIter = fieldElementList.iterator();
        while (fieldElementIter.hasNext()) {
            Element fieldElement = (Element) fieldElementIter.next();
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
        List pkElementList = UtilXml.childElementList(entityElement, "prim-key");
        Iterator pkElementIter = pkElementList.iterator();
        while (pkElementIter.hasNext()) {
            Element pkElement = (Element) pkElementIter.next();
            ModelField field = reader.findModelField(this, pkElement.getAttribute("field"));
            if (field != null) {
                this.pks.add(field);
                field.isPk = true;
            } else {
                Debug.logError("[ModelReader.createModelEntity] ERROR: Could not find field \"" +
                        pkElement.getAttribute("field") + "\" specified in a prim-key", module);
            }
        }

        // now that we have the pks and the fields, make the nopks vector
        this.nopks = FastList.newInstance();
        for (int ind = 0; ind < this.fields.size(); ind++) {
            ModelField field = (ModelField) this.fields.get(ind);
            if (!field.isPk) this.nopks.add(field);
        }

        if (utilTimer != null) utilTimer.timerString("  createModelEntity: before relations");
        this.populateRelated(reader, entityElement);
        this.populateIndexes(entityElement);
    }

    /** DB Names Constructor */
    public ModelEntity(String tableName, Map colMap, ModelFieldTypeReader modelFieldTypeReader, boolean isCaseSensitive) {
        // if there is a dot in the name, remove it and everything before it, should be the schema name
        this.tableName = tableName;
        int dotIndex = this.tableName.indexOf(".");
        if (dotIndex >= 0) {
            this.tableName = this.tableName.substring(dotIndex + 1);
        }
        this.entityName = ModelUtil.dbNameToClassName(this.tableName);
        Iterator columnEntryIter = colMap.entrySet().iterator();
        while (columnEntryIter.hasNext()) {
            Map.Entry columnEntry = (Map.Entry) columnEntryIter.next();
            DatabaseUtil.ColumnCheckInfo ccInfo = (DatabaseUtil.ColumnCheckInfo) columnEntry.getValue();
            ModelField newField = new ModelField(ccInfo, modelFieldTypeReader);
            this.fields.add(newField);
        }
        this.updatePkLists();
    }

    protected void populateBasicInfo(Element entityElement) {
        this.entityName = UtilXml.checkEmpty(entityElement.getAttribute("entity-name"));
        this.tableName = UtilXml.checkEmpty(entityElement.getAttribute("table-name"), ModelUtil.javaNameToDbName(this.entityName));
        this.packageName = UtilXml.checkEmpty(entityElement.getAttribute("package-name"));
        this.defaultResourceName = UtilXml.checkEmpty(entityElement.getAttribute("default-resource-name"));
        this.dependentOn = UtilXml.checkEmpty(entityElement.getAttribute("dependent-on"));
        this.doLock = UtilXml.checkBoolean(entityElement.getAttribute("enable-lock"), false);
        this.noAutoStamp = UtilXml.checkBoolean(entityElement.getAttribute("no-auto-stamp"), false);
        this.neverCache = UtilXml.checkBoolean(entityElement.getAttribute("never-cache"), false);
        this.autoClearCache = UtilXml.checkBoolean(entityElement.getAttribute("auto-clear-cache"), true);
    }


    protected void populateRelated(ModelReader reader, Element entityElement) {
        List relationElementList = UtilXml.childElementList(entityElement, "relation");
        Iterator relationElementIter = relationElementList.iterator();
        while (relationElementIter.hasNext()) {
            Element relationElement = (Element) relationElementIter.next();
            ModelRelation relation = reader.createRelation(this, relationElement);
            if (relation != null) {
                relation.setModelEntity(this);
                this.relations.add(relation);
            }
        }
    }


    protected void populateIndexes(Element entityElement) {
        List indexElementList = UtilXml.childElementList(entityElement, "index");
        Iterator indexElementIter = indexElementList.iterator();
        while (indexElementIter.hasNext()) {
            Element indexElement = (Element) indexElementIter.next();
            ModelIndex index = new ModelIndex(this, indexElement);
            index.setModelEntity(this);
            this.indexes.add(index);
        }
    }

    public boolean containsAllPkFieldNames(Set fieldNames) {
        Iterator pksIter = this.getPksIterator();
        while (pksIter.hasNext()) {
            ModelField pkField = (ModelField) pksIter.next();
            if (!fieldNames.contains(pkField.getName())) {
                return false;
            }
        }
        return true;
    }


    public void addExtendEntity(ModelReader reader, Element extendEntityElement) {
        List fieldElementList = UtilXml.childElementList(extendEntityElement, "field");
        Iterator fieldElementIter = fieldElementList.iterator();
        while (fieldElementIter.hasNext()) {
            Element fieldElement = (Element) fieldElementIter.next();
            // TODO: should we look for existing fields of the same name here? for now just add to list...
            ModelField field = reader.createModelField(fieldElement);
            if (field != null) {
                field.setModelEntity(this);
                this.fields.add(field);
                // this will always be true for now as extend-entity fielsd are always nonpks
                if (!field.isPk) this.nopks.add(field);
            }
        }
        
        this.populateRelated(reader, extendEntityElement);
        this.populateIndexes(extendEntityElement);
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
        if (datasourceInfo != null && datasourceInfo.schemaName != null && datasourceInfo.schemaName.length() > 0) {
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

    /** The default-resource-name of the Entity */
    public String getDefaultResourceName() {
        return this.defaultResourceName;
    }

    public void setDefaultResourceName(String defaultResourceName) {
        this.defaultResourceName = defaultResourceName;
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

    public void updatePkLists() {
        pks = FastList.newInstance();
        nopks = FastList.newInstance();
        for (int i = 0; i < fields.size(); i++) {
            ModelField field = (ModelField) fields.get(i);

            if (field.isPk)
                pks.add(field);
            else
                nopks.add(field);
        }
    }

    public boolean isField(String fieldName) {
        if (fieldName == null) return false;
        for (int i = 0; i < fields.size(); i++) {
            ModelField field = (ModelField) fields.get(i);

            if (field.name.equals(fieldName)) return true;
        }
        return false;
    }

    public boolean areFields(Collection fieldNames) {
        if (fieldNames == null) return false;
        Iterator iter = fieldNames.iterator();

        while (iter.hasNext()) {
            String fieldName = (String) iter.next();

            if (!isField(fieldName)) return false;
        }
        return true;
    }

    public int getPksSize() {
        return this.pks.size();
    }

    /**
     * @deprecated
     */
    public ModelField getPk(int index) {
        return (ModelField) this.pks.get(index);
    }

    public ModelField getOnlyPk() {
        if (this.pks.size() == 1) {
            return (ModelField) this.pks.get(0);
        } else {
            throw new IllegalArgumentException("Error in getOnlyPk, the [" + this.getEntityName() + "] entity has more than one pk!");
        }
    }

    public Iterator getPksIterator() {
        return this.pks.iterator();
    }

    public List getPksCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.pks);
        return newList;
    }

    public String getFirstPkFieldName() {
        List pkFieldNames = this.getPkFieldNames();
        String idFieldName = null;
        if (pkFieldNames != null && pkFieldNames.size() > 0) {
            idFieldName = (String) pkFieldNames.get(0);
        }
        return idFieldName;
    }

    public int getNopksSize() {
        return this.nopks.size();
    }

    /**
     * @deprecated
     */
    public ModelField getNopk(int index) {
        return (ModelField) this.nopks.get(index);
    }

    public Iterator getNopksIterator() {
        return this.nopks.iterator();
    }

    public List getNopksCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.nopks);
        return newList;
    }

    public int getFieldsSize() {
        return this.fields.size();
    }

    /**
     * @deprecated
     */
    public ModelField getField(int index) {
        return (ModelField) this.fields.get(index);
    }

    public Iterator getFieldsIterator() {
        return this.fields.iterator();
    }

    public List getFieldsCopy() {
        List newList = FastList.newInstance();
        newList.addAll(this.fields);
        return newList;
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
        ModelField modelField = (ModelField) fieldsMap.get(fieldName);
        if (modelField == null) {
            // sometimes weird things happen and this getField method is called before the fields are all populated, so before moving on just re-create the fieldsMap again real quick...
            // the purpose of the fieldsMap is for speed, but if failures are a little slower, no biggie
            createFieldsMap();
            modelField = (ModelField) fieldsMap.get(fieldName);
        }
        return modelField;
    }

    protected synchronized void createFieldsMap() {
        Map tempMap = FastMap.newInstance();
        for (int i = 0; i < fields.size(); i++) {
            ModelField field = (ModelField) fields.get(i);
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

        field = (ModelField) fields.remove(index);
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

        for (int i = 0; i < fields.size(); i++) {
            field = (ModelField) fields.get(i);
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

    public List getAllFieldNames() {
        return getFieldNamesFromFieldVector(fields);
    }

    public List getPkFieldNames() {
        return getFieldNamesFromFieldVector(pks);
    }

    public List getNoPkFieldNames() {
        return getFieldNamesFromFieldVector(nopks);
    }

    public List getFieldNamesFromFieldVector(List modelFields) {
        List nameList = FastList.newInstance();

        if (modelFields == null || modelFields.size() <= 0) return nameList;
        for (int i = 0; i < modelFields.size(); i++) {
            ModelField field = (ModelField) modelFields.get(i);

            nameList.add(field.name);
        }
        return nameList;
    }

    public int getRelationsSize() {
        return this.relations.size();
    }

    public int getRelationsOneSize() {
        int numRels = 0;
        Iterator relationsIter = this.getRelationsIterator();
        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = (ModelRelation) relationsIter.next();
            if ("one".equals(modelRelation.getType())) {
                numRels++;
            }
        }
        return numRels;
    }

    public ModelRelation getRelation(int index) {
        return (ModelRelation) this.relations.get(index);
    }

    public Iterator getRelationsIterator() {
        return this.relations.iterator();
    }

    public ModelRelation getRelation(String relationName) {
        if (relationName == null) return null;
        for (int i = 0; i < relations.size(); i++) {
            ModelRelation relation = (ModelRelation) relations.get(i);
            if (relationName.equals(relation.title + relation.relEntityName)) return relation;
        }
        return null;
    }

    public void addRelation(ModelRelation relation) {
        relation.setModelEntity(this);
        this.relations.add(relation);
    }

    public ModelRelation removeRelation(int index) {
        return (ModelRelation) this.relations.remove(index);
    }

    public int getIndexesSize() {
        return this.indexes.size();
    }

    public ModelIndex getIndex(int index) {
        return (ModelIndex) this.indexes.get(index);
    }

    public Iterator getIndexesIterator() {
        return this.indexes.iterator();
    }

    public ModelIndex getIndex(String indexName) {
        if (indexName == null) return null;
        for (int i = 0; i < indexes.size(); i++) {
            ModelIndex index = (ModelIndex) indexes.get(i);
            if (indexName.equals(index.getName())) return index;
        }
        return null;
    }

    public void addIndex(ModelIndex index) {
        index.setModelEntity(this);
        this.indexes.add(index);
    }

    public ModelIndex removeIndex(int index) {
        return (ModelIndex) this.indexes.remove(index);
    }

    public int getViewEntitiesSize() {
        return this.viewEntities.size();
    }

    public ModelViewEntity getViewEntity(String viewEntityName) {
        return (ModelViewEntity) this.viewEntities.get(viewEntityName);
    }

    public Iterator getViewConvertorsIterator() {
        return this.viewEntities.entrySet().iterator();
    }

    public void addViewEntity(ModelViewEntity view) {
        this.viewEntities.put(view.getEntityName(), view);
    }

    public List convertToViewValues(String viewEntityName, GenericEntity entity) {
        if (entity == null || entity == GenericEntity.NULL_ENTITY || entity == GenericValue.NULL_VALUE) return UtilMisc.toList(entity);
        ModelViewEntity view = (ModelViewEntity) this.viewEntities.get(viewEntityName);
        return view.convert(getEntityName(), entity);
    }

    public ModelViewEntity removeViewEntity(String viewEntityName) {
        return (ModelViewEntity) this.viewEntities.remove(viewEntityName);
    }

    public ModelViewEntity removeViewEntity(ModelViewEntity viewEntity) {
       return removeViewEntity(viewEntity.getEntityName());
    }

    public String nameString(List flds) {
        return nameString(flds, ", ", "");
    }

    public String nameString(List flds, String separator, String afterLast) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(((ModelField) flds.get(i)).name);
            returnString.append(separator);
        }
        returnString.append(((ModelField) flds.get(i)).name);
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String typeNameString(List flds) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelField curField = (ModelField) flds.get(i);
            returnString.append(curField.type);
            returnString.append(" ");
            returnString.append(curField.name);
            returnString.append(", ");
        }
        ModelField curField = (ModelField) flds.get(i);
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

    public String fieldsStringList(List flds, String eachString, String separator) {
        return fieldsStringList(flds, eachString, separator, false, false);
    }

    public String fieldsStringList(List flds, String eachString, String separator, boolean appendIndex) {
        return fieldsStringList(flds, eachString, separator, appendIndex, false);
    }

    public String fieldsStringList(List flds, String eachString, String separator, boolean appendIndex, boolean onlyNonPK) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size(); i++) {
            if (onlyNonPK && ((ModelField) flds.get(i)).isPk) continue;
            returnString.append(eachString);
            if (appendIndex) returnString.append(i + 1);
            if (i < flds.size() - 1) returnString.append(separator);
        }
        return returnString.toString();
    }

    public String colNameString(List flds) {
        return colNameString(flds, ", ", "", false);
    }

    public String colNameString(List flds, String separator, String afterLast, boolean alias) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        Iterator fldsIt = flds.iterator();
        while(fldsIt.hasNext()) {
            ModelField field = (ModelField) fldsIt.next();
            returnString.append(field.colName);
            if (fldsIt.hasNext()) {
                returnString.append(separator);
            }
        }

        returnString.append(afterLast);
        return returnString.toString();
    }

    public String classNameString(List flds) {
        return classNameString(flds, ", ", "");
    }

    public String classNameString(List flds, String separator, String afterLast) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
            returnString.append(separator);
        }
        returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
        returnString.append(afterLast);
        return returnString.toString();
    }

    public String finderQueryString(List flds) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append(((ModelField) flds.get(i)).colName);
            returnString.append(" like {");
            returnString.append(i);
            returnString.append("} AND ");
        }
        returnString.append(((ModelField) flds.get(i)).colName);
        returnString.append(" like {");
        returnString.append(i);
        returnString.append("}");
        return returnString.toString();
    }

    public String httpArgList(List flds) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }
        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(((ModelField) flds.get(i)).colName);
            returnString.append("=\" + ");
            returnString.append(((ModelField) flds.get(i)).name);
            returnString.append(" + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(((ModelField) flds.get(i)).colName);
        returnString.append("=\" + ");
        returnString.append(((ModelField) flds.get(i)).name);
        return returnString.toString();
    }

    public String httpArgListFromClass(List flds) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(((ModelField) flds.get(i)).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(((ModelField) flds.get(i)).colName);
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpArgListFromClass(List flds, String entityNameSuffix) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(((ModelField) flds.get(i)).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(entityName));
            returnString.append(entityNameSuffix);
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
            returnString.append("() + \"&\" + ");
        }
        returnString.append("\"");
        returnString.append(tableName);
        returnString.append("_");
        returnString.append(((ModelField) flds.get(i)).colName);
        returnString.append("=\" + ");
        returnString.append(ModelUtil.lowerFirstChar(entityName));
        returnString.append(entityNameSuffix);
        returnString.append(".get");
        returnString.append(ModelUtil.upperFirstChar(((ModelField) flds.get(i)).name));
        returnString.append("()");
        return returnString.toString();
    }

    public String httpRelationArgList(List flds, ModelRelation relation) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(((ModelField) flds.get(i)).name);

            if (keyMap != null) {
                returnString.append("\"");
                returnString.append(tableName);
                returnString.append("_");
                returnString.append(((ModelField) flds.get(i)).colName);
                returnString.append("=\" + ");
                returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
                returnString.append(".get");
                returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
                returnString.append("() + \"&\" + ");
            } else {
                Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + ((ModelField) flds.get(i)).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type, module);
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(((ModelField) flds.get(i)).name);

        if (keyMap != null) {
            returnString.append("\"");
            returnString.append(tableName);
            returnString.append("_");
            returnString.append(((ModelField) flds.get(i)).colName);
            returnString.append("=\" + ");
            returnString.append(ModelUtil.lowerFirstChar(relation.mainEntity.entityName));
            returnString.append(".get");
            returnString.append(ModelUtil.upperFirstChar(keyMap.fieldName));
            returnString.append("()");
        } else {
            Debug.logWarning("-- -- ENTITYGEN ERROR:httpRelationArgList: Related Key in Key Map not found for name: " + ((ModelField) flds.get(i)).name + " related entity: " + relation.relEntityName + " main entity: " + relation.mainEntity.entityName + " type: " + relation.type, module);
        }
        return returnString.toString();
    }

    /*
     public String httpRelationArgList(ModelRelation relation) {
     String returnString = "";
     if(relation.keyMaps.size() < 1) { return ""; }

     int i = 0;
     for(; i < relation.keyMaps.size() - 1; i++) {
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     if(keyMap != null)
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "() + \"&\" + ";
     }
     ModelKeyMap keyMap = (ModelKeyMap)relation.keyMaps.get(i);
     returnString = returnString + "\"" + tableName + "_" + keyMap.relColName + "=\" + " + ModelUtil.lowerFirstChar(relation.mainEntity.entityName) + ".get" + ModelUtil.upperFirstChar(keyMap.fieldName) + "()";
     return returnString;
     }
     */
    public String typeNameStringRelatedNoMapped(List flds, ModelRelation relation) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        if (relation.findKeyMapByRelated(((ModelField) flds.get(i)).name) == null) {
            returnString.append(((ModelField) flds.get(i)).type);
            returnString.append(" ");
            returnString.append(((ModelField) flds.get(i)).name);
        }
        i++;
        for (; i < flds.size(); i++) {
            if (relation.findKeyMapByRelated(((ModelField) flds.get(i)).name) == null) {
                if (returnString.length() > 0) returnString.append(", ");
                returnString.append(((ModelField) flds.get(i)).type);
                returnString.append(" ");
                returnString.append(((ModelField) flds.get(i)).name);
            }
        }
        return returnString.toString();
    }

    public String typeNameStringRelatedAndMain(List flds, ModelRelation relation) {
        StringBuffer returnString = new StringBuffer();

        if (flds.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < flds.size() - 1; i++) {
            ModelKeyMap keyMap = relation.findKeyMapByRelated(((ModelField) flds.get(i)).name);

            if (keyMap != null) {
                returnString.append(keyMap.fieldName);
                returnString.append(", ");
            } else {
                returnString.append(((ModelField) flds.get(i)).name);
                returnString.append(", ");
            }
        }
        ModelKeyMap keyMap = relation.findKeyMapByRelated(((ModelField) flds.get(i)).name);

        if (keyMap != null) returnString.append(keyMap.fieldName);
        else returnString.append(((ModelField) flds.get(i)).name);
        return returnString.toString();
    }

    public int compareTo(Object obj) {
        ModelEntity otherModelEntity = (ModelEntity) obj;

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

    public void convertFieldMapInPlace(Map inContext, GenericDelegator delegator) {
        Iterator modelFields = this.getFieldsIterator();
        while (modelFields.hasNext()) {
            ModelField modelField = (ModelField) modelFields.next();
            String fieldName = modelField.getName();
            Object oldValue = inContext.get(fieldName);
            if (oldValue != null) {
                inContext.put(fieldName, this.convertFieldValue(modelField, oldValue, delegator));
            }
        }
    }

    public Object convertFieldValue(String fieldName, Object value, GenericDelegator delegator) {
        ModelField modelField = this.getField(fieldName);
        if (modelField == null) {
            String errMsg = "Could not convert field value: could not find an entity field for the name: [" + fieldName + "] on the [" + this.getEntityName() + "] entity.";
            throw new IllegalArgumentException(errMsg);
        }
        return convertFieldValue(modelField, value, delegator);
    }

    public Object convertFieldValue(ModelField modelField, Object value, GenericDelegator delegator) {
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
        Iterator fieldIter = this.getFieldsIterator();
        while (fieldIter != null && fieldIter.hasNext()) {
            ModelField field = (ModelField) fieldIter.next();
            if (!field.getIsAutoCreatedInternal()) {
                root.appendChild(field.toXmlElement(document));
            }
        }

        // append PK elements
        Iterator pkIter = this.getPksIterator();
        while (pkIter != null && pkIter.hasNext()) {
            ModelField pk = (ModelField) pkIter.next();
            Element pkey = document.createElement("prim-key");
            pkey.setAttribute("field", pk.getName());
            root.appendChild(pkey);
        }

        // append relation elements
        Iterator relIter = this.getRelationsIterator();
        while (relIter != null && relIter.hasNext()) {
            ModelRelation rel = (ModelRelation) relIter.next();

        }

        // append index elements
        Iterator idxIter = this.getIndexesIterator();
        while (idxIter != null && idxIter.hasNext()) {
            ModelIndex idx = (ModelIndex) idxIter.next();
            root.appendChild(idx.toXmlElement(document));

        }

        return root;
    }

    public Element toXmlElement(Document document) {
        return this.toXmlElement(document, this.getPackageName());
    }
}

