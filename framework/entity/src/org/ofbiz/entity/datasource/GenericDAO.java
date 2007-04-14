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
package org.ofbiz.entity.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionParam;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldTypeReader;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * Generic Entity Data Access Object - Handles persisntence for any defined entity.
 *
 */
public class GenericDAO {

    public static final String module = GenericDAO.class.getName();

    protected static Map genericDAOs = FastMap.newInstance();
    protected String helperName;
    protected ModelFieldTypeReader modelFieldTypeReader = null;
    protected DatasourceInfo datasourceInfo;

    public static GenericDAO getGenericDAO(String helperName) {
        GenericDAO newGenericDAO = (GenericDAO) genericDAOs.get(helperName);

        if (newGenericDAO == null) { // don't want to block here
            synchronized (GenericDAO.class) {
                newGenericDAO = (GenericDAO) genericDAOs.get(helperName);
                if (newGenericDAO == null) {
                    newGenericDAO = new GenericDAO(helperName);
                    genericDAOs.put(helperName, newGenericDAO);
                }
            }
        }
        return newGenericDAO;
    }

    public GenericDAO(String helperName) {
        this.helperName = helperName;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        this.datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
    }

    private void addFieldIfMissing(List fieldsToSave, String fieldName, ModelEntity modelEntity) {
        Iterator fieldsToSaveIter = fieldsToSave.iterator();
        while (fieldsToSaveIter.hasNext()) {
            ModelField fieldToSave = (ModelField) fieldsToSaveIter.next();
            if (fieldName.equals(fieldToSave.getName())) {
                return;
            }
        }
        // at this point we didn't find it
        fieldsToSave.add(modelEntity.getField(fieldName));
    }

    public int insert(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            return singleInsert(entity, modelEntity, modelEntity.getFieldsCopy(), sqlP);
        } catch (GenericEntityException e) {
            sqlP.rollback();
            throw new GenericEntityException("Exception while inserting the following entity: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    private int singleInsert(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave, SQLProcessor sqlP) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, sqlP);
        }

        // if we have a STAMP_TX_FIELD or CREATE_STAMP_TX_FIELD then set it with NOW, always do this before the STAMP_FIELD
        // NOTE: these fairly complicated if statements have a few objectives:
        //   1. don't run the TransationUtil.getTransaction*Stamp() methods when we don't need to
        //   2. don't set the stamp values if it is from an EntitySync (ie maintain original values), unless the stamps are null then set it anyway, ie even if it was from an EntitySync (also used for imports and such)
        boolean stampTxIsField = modelEntity.isField(ModelEntity.STAMP_TX_FIELD);
        boolean createStampTxIsField = modelEntity.isField(ModelEntity.CREATE_STAMP_TX_FIELD);
        if ((stampTxIsField || createStampTxIsField) && (!entity.getIsFromEntitySync() || (stampTxIsField && entity.get(ModelEntity.STAMP_TX_FIELD) == null) || (createStampTxIsField && entity.get(ModelEntity.CREATE_STAMP_TX_FIELD) == null))) {
            Timestamp txStartStamp = TransactionUtil.getTransactionStartStamp();
            if (stampTxIsField && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_TX_FIELD) == null)) {
                entity.set(ModelEntity.STAMP_TX_FIELD, txStartStamp);
                addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_TX_FIELD, modelEntity);
            }
            if (createStampTxIsField && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.CREATE_STAMP_TX_FIELD) == null)) {
                entity.set(ModelEntity.CREATE_STAMP_TX_FIELD, txStartStamp);
                addFieldIfMissing(fieldsToSave, ModelEntity.CREATE_STAMP_TX_FIELD, modelEntity);
            }
        }

        // if we have a STAMP_FIELD or CREATE_STAMP_FIELD then set it with NOW
        boolean stampIsField = modelEntity.isField(ModelEntity.STAMP_FIELD);
        boolean createStampIsField = modelEntity.isField(ModelEntity.CREATE_STAMP_FIELD);
        if ((stampIsField || createStampIsField)  && (!entity.getIsFromEntitySync() || (stampIsField && entity.get(ModelEntity.STAMP_FIELD) == null) || (createStampIsField && entity.get(ModelEntity.CREATE_STAMP_FIELD) == null))) {
            Timestamp startStamp = TransactionUtil.getTransactionUniqueNowStamp();
            if (stampIsField && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_FIELD) == null)) {
                entity.set(ModelEntity.STAMP_FIELD, startStamp);
                addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_FIELD, modelEntity);
            }
            if (createStampIsField && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.CREATE_STAMP_FIELD) == null)) {
                entity.set(ModelEntity.CREATE_STAMP_FIELD, startStamp);
                addFieldIfMissing(fieldsToSave, ModelEntity.CREATE_STAMP_FIELD, modelEntity);
            }
        }

        String sql = "INSERT INTO " + modelEntity.getTableName(datasourceInfo) + " (" + modelEntity.colNameString(fieldsToSave) + ") VALUES (" +
            modelEntity.fieldsStringList(fieldsToSave, "?", ", ") + ")";

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            int retVal = sqlP.executeUpdate();

            entity.synchronizedWithDatasource();
            return retVal;
        } catch (GenericEntityException e) {
            throw new GenericEntityException("while inserting: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    public int updateAll(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        return customUpdate(entity, modelEntity, modelEntity.getNopksCopy());
    }

    public int update(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        // we don't want to update ALL fields, just the nonpk fields that are in the passed GenericEntity
        List partialFields = FastList.newInstance();
        Collection keys = entity.getAllKeys();

        Iterator nopkIter = modelEntity.getNopksIterator();
        while (nopkIter.hasNext()) {
            ModelField curField = (ModelField) nopkIter.next();
            if (keys.contains(curField.getName())) {
                partialFields.add(curField);
            }
        }

        return customUpdate(entity, modelEntity, partialFields);
    }

    private int customUpdate(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);
        try {
            return singleUpdate(entity, modelEntity, fieldsToSave, sqlP);
        } catch (GenericEntityException e) {
            sqlP.rollback();
            throw new GenericEntityException("Exception while updating the following entity: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    private int singleUpdate(GenericEntity entity, ModelEntity modelEntity, List fieldsToSave, SQLProcessor sqlP) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, sqlP);
        }

        // no non-primaryKey fields, update doesn't make sense, so don't do it
        if (fieldsToSave.size() <= 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Trying to do an update on an entity with no non-PK fields, returning having done nothing; entity=" + entity, module);
            // returning one because it was effectively updated, ie the same thing, so don't trigger any errors elsewhere
            return 1;
        }

        if (modelEntity.lock()) {
            GenericEntity entityCopy = GenericEntity.createGenericEntity(entity);

            select(entityCopy, sqlP);
            Object stampField = entity.get(ModelEntity.STAMP_FIELD);

            if ((stampField != null) && (!stampField.equals(entityCopy.get(ModelEntity.STAMP_FIELD)))) {
                String lockedTime = entityCopy.getTimestamp(ModelEntity.STAMP_FIELD).toString();

                throw new EntityLockedException("You tried to update an old version of this data. Version locked: (" + lockedTime + ")");
            }
        }

        // if we have a STAMP_TX_FIELD then set it with NOW, always do this before the STAMP_FIELD
        // NOTE: these fairly complicated if statements have a few objectives:
        //   1. don't run the TransationUtil.getTransaction*Stamp() methods when we don't need to
        //   2. don't set the stamp values if it is from an EntitySync (ie maintain original values), unless the stamps are null then set it anyway, ie even if it was from an EntitySync (also used for imports and such)
        if (modelEntity.isField(ModelEntity.STAMP_TX_FIELD) && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_TX_FIELD) == null)) {
            entity.set(ModelEntity.STAMP_TX_FIELD, TransactionUtil.getTransactionStartStamp());
            addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_TX_FIELD, modelEntity);
        }

        // if we have a STAMP_FIELD then update it with NOW.
        if (modelEntity.isField(ModelEntity.STAMP_FIELD) && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_FIELD) == null)) {
            entity.set(ModelEntity.STAMP_FIELD, TransactionUtil.getTransactionUniqueNowStamp());
            addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_FIELD, modelEntity);
        }

        String sql = "UPDATE " + modelEntity.getTableName(datasourceInfo) + " SET " + modelEntity.colNameString(fieldsToSave, "=?, ", "=?", false) + " WHERE " +
            SqlJdbcUtil.makeWhereStringFromFields(modelEntity.getPksCopy(), entity, "AND");

        int retVal = 0;

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            retVal = sqlP.executeUpdate();
            entity.synchronizedWithDatasource();
        } catch (GenericEntityException e) {
            throw new GenericEntityException("while updating: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }

        if (retVal == 0) {
            throw new GenericEntityNotFoundException("Tried to update an entity that does not exist.");
        }
        return retVal;
    }

    public int updateByCondition(ModelEntity modelEntity, Map fieldsToSet, EntityCondition condition) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            return updateByCondition(modelEntity, fieldsToSet, condition, sqlP);
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Generic Entity Exception occured in updateByCondition", e);
        } finally {
            sqlP.close();
        }
    }

    public int updateByCondition(ModelEntity modelEntity, Map fieldsToSet, EntityCondition condition, SQLProcessor sqlP) throws GenericEntityException {
        if (modelEntity == null || fieldsToSet == null || condition == null)
            return 0;
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.entity.GenericNotImplementedException("Operation updateByCondition not supported yet for view entities");
        }

        String sql = "UPDATE " + modelEntity.getTableName(datasourceInfo);
        sql += " SET ";
        Iterator i = fieldsToSet.keySet().iterator();
        List fieldList = new LinkedList();
        boolean firstField = true;
        while (i.hasNext()) {
            String name = (String) i.next();
            ModelField field = modelEntity.getField(name);
            if (field != null) {
                if (!firstField) {
                    sql += ", ";
                } else {
                    firstField = false;
                }
                sql += field.getColName() + " = ?";
                fieldList.add(field);
            }
        }
        sql += " WHERE " + condition.makeWhereString(modelEntity, null, this.datasourceInfo);

        try {
            sqlP.prepareStatement(sql);
            Iterator fi = fieldList.iterator();
            while (fi.hasNext()) {
                ModelField field = (ModelField) fi.next();
                Object value = fieldsToSet.get(field.getName());
                SqlJdbcUtil.setValue(sqlP, field, modelEntity.getEntityName(), value, modelFieldTypeReader );
            }

            return sqlP.executeUpdate();
        } finally {
            sqlP.close();
        }
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * Try to update the given ModelViewEntity by trying to insert/update on the entities of which the view is composed.
     *
     * Works fine with standard O/R mapped models, but has some restrictions meeting more complicated view entities.
     * <li>A direct link is required, which means that one of the ModelViewLink field entries must have a value found
     * in the given view entity, for each ModelViewLink</li>
     * <li>For now, each member entity is updated iteratively, so if eg. the second member entity fails to update,
     * the first is written although. See code for details. Try to use "clean" views, until code is more robust ...</li>
     * <li>For now, aliased field names in views are not processed correctly, I guess. To be honest, I did not
     * find out how to construct such a view - so view fieldnames must have same named fields in member entities.</li>
     * <li>A new exception, e.g. GenericViewNotUpdatable, should be defined and thrown if the update fails</li>
     *
     */
    private int singleUpdateView(GenericEntity entity, ModelViewEntity modelViewEntity, List fieldsToSave, SQLProcessor sqlP) throws GenericEntityException {
        GenericDelegator delegator = entity.getDelegator();

        int retVal = 0;
        ModelEntity memberModelEntity = null;

        // Construct insert/update for each model entity
        Iterator meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();
        while (meIter != null && meIter.hasNext()) {
            Map.Entry meMapEntry = (Map.Entry) meIter.next();
            ModelViewEntity.ModelMemberEntity modelMemberEntity = (ModelViewEntity.ModelMemberEntity) meMapEntry.getValue();
            String meName = modelMemberEntity.getEntityName();
            String meAlias = modelMemberEntity.getEntityAlias();

            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: Processing MemberEntity " + meName + " with Alias " + meAlias, module);
            try {
                memberModelEntity = delegator.getModelReader().getModelEntity(meName);
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Failed to get model entity for " + meName, e);
            }

            Map findByMap = FastMap.newInstance();

            // Now iterate the ModelViewLinks to construct the "WHERE" part for update/insert
            Iterator linkIter = modelViewEntity.getViewLinksIterator();

            while (linkIter != null && linkIter.hasNext()) {
                ModelViewEntity.ModelViewLink modelViewLink = (ModelViewEntity.ModelViewLink) linkIter.next();

                if (modelViewLink.getEntityAlias().equals(meAlias) || modelViewLink.getRelEntityAlias().equals(meAlias)) {

                    Iterator kmIter = modelViewLink.getKeyMapsIterator();

                    while (kmIter != null && kmIter.hasNext()) {
                        ModelKeyMap keyMap = (ModelKeyMap) kmIter.next();

                        String fieldName = "";

                        if (modelViewLink.getEntityAlias().equals(meAlias)) {
                            fieldName = keyMap.getFieldName();
                        } else {
                            fieldName = keyMap.getRelFieldName();
                        }

                        if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found field to set: " + meAlias + "." + fieldName, module);
                        Object value = null;

                        if (modelViewEntity.isField(keyMap.getFieldName())) {
                            value = entity.get(keyMap.getFieldName());
                            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString(), module);
                        } else if (modelViewEntity.isField(keyMap.getRelFieldName())) {
                            value = entity.get(keyMap.getRelFieldName());
                            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString(), module);
                        } else {
                            throw new GenericNotImplementedException("Update on view entities: no direct link found, unable to update");
                        }

                        findByMap.put(fieldName, value);
                    }
                }
            }

            // Look what there already is in the database
            List meResult = null;

            try {
                meResult = delegator.findByAnd(meName, findByMap);
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Error while retrieving partial results for entity member: " + meName, e);
            }
            if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Found " + meResult.size() + " results for entity member " + meName, module);

            // Got results 0 -> INSERT, 1 -> UPDATE, >1 -> View is nor updatable
            GenericValue meGenericValue = null;

            if (meResult.size() == 0) {
                // Create new value to insert
                try {
                    // Create new value to store
                    meGenericValue = delegator.makeValue(meName, findByMap);
                } catch (Exception e) {
                    throw new GenericEntityException("Could not create new value for member entity" + meName + " of view " + modelViewEntity.getEntityName(), e);
                }
            } else if (meResult.size() == 1) {
                // Update existing value
                meGenericValue = (GenericValue) meResult.iterator().next();
            } else {
                throw new GenericEntityException("Found more than one result for member entity " + meName + " in view " + modelViewEntity.getEntityName() + " - this is no updatable view");
            }

            // Construct fieldsToSave list for this member entity
            List meFieldsToSave = FastList.newInstance();
            Iterator fieldIter = fieldsToSave.iterator();

            while (fieldIter != null && fieldIter.hasNext()) {
                ModelField modelField = (ModelField) fieldIter.next();

                if (memberModelEntity.isField(modelField.getName())) {
                    ModelField meModelField = memberModelEntity.getField(modelField.getName());

                    if (meModelField != null) {
                        meGenericValue.set(meModelField.getName(), entity.get(modelField.getName()));
                        meFieldsToSave.add(meModelField);
                        if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: --- Added field to save: " + meModelField.getName() + " with value " + meGenericValue.get(meModelField.getName()), module);
                    } else {
                        throw new GenericEntityException("Could not get field " + modelField.getName() + " from model entity " + memberModelEntity.getEntityName());
                    }
                }
            }

            /*
             * Finally, do the insert/update
             * TODO:
             * Do the real inserts/updates outside the memberEntity-loop,
             * only if all of the found member entities are updatable.
             * This avoids partial creation of member entities, which would mean data inconsistency:
             * If not all member entities can be updated, then none should be updated
             */
            if (meResult.size() == 0) {
                retVal += singleInsert(meGenericValue, memberModelEntity, memberModelEntity.getFieldsCopy(), sqlP);
            } else {
                if (meFieldsToSave.size() > 0) {
                    retVal += singleUpdate(meGenericValue, memberModelEntity, meFieldsToSave, sqlP);
                } else {
                    if (Debug.verboseOn()) Debug.logVerbose("[singleUpdateView]: No update on member entity " + memberModelEntity.getEntityName() + " needed", module);
                }
            }
        }

        return retVal;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public void select(GenericEntity entity) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            select(entity, sqlP);
        } finally {
            sqlP.close();
        }
    }

    public void select(GenericEntity entity, SQLProcessor sqlP) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        if (modelEntity.getPksSize() <= 0) {
            throw new GenericEntityException("Entity has no primary keys, cannot select by primary key");
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (modelEntity.getNopksSize() > 0) {
            sqlBuffer.append(modelEntity.colNameString(modelEntity.getNopksCopy(), ", ", "", datasourceInfo.aliasViews));
        } else {
            sqlBuffer.append("*");
        }

        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPksCopy(), entity, "AND", datasourceInfo.joinStyle));

        try {
            sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            sqlP.executeQuery();

            if (sqlP.next()) {
                int idx = 1;
                Iterator nopkIter = modelEntity.getNopksIterator();
                while (nopkIter.hasNext()) {
                    ModelField curField = (ModelField) nopkIter.next();
                    SqlJdbcUtil.getValue(sqlP.getResultSet(), idx, curField, entity, modelFieldTypeReader);
                    idx++;
                }

                entity.synchronizedWithDatasource();
            } else {
                // Debug.logWarning("[GenericDAO.select]: select failed, result set was empty for entity: " + entity.toString(), module);
                throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
            }
        } finally {
            sqlP.close();
        }
    }

    public void partialSelect(GenericEntity entity, Set keys) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }

        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.entity.GenericNotImplementedException("Operation partialSelect not supported yet for view entities");
        }

        /*
         if(entity == null || entity.<%=modelEntity.pkNameString(" == null || entity."," == null")%>) {
         Debug.logWarning("[GenericDAO.select]: Cannot select GenericEntity: required primary key field(s) missing.", module);
         return false;
         }
         */
        // we don't want to select ALL fields, just the nonpk fields that are in the passed GenericEntity
        List partialFields = FastList.newInstance();

        Set tempKeys = new TreeSet(keys);

        Iterator nopkIter = modelEntity.getNopksIterator();
        while (nopkIter.hasNext()) {
            ModelField curField = (ModelField) nopkIter.next();
            if (tempKeys.contains(curField.getName())) {
                partialFields.add(curField);
                tempKeys.remove(curField.getName());
            }
        }

        if (tempKeys.size() > 0) {
            throw new GenericModelException("In partialSelect invalid field names specified: " + tempKeys.toString());
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (partialFields.size() > 0) {
            sqlBuffer.append(modelEntity.colNameString(partialFields, ", ", "", datasourceInfo.aliasViews));
        } else {
            sqlBuffer.append("*");
        }
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPksCopy(), entity, "AND", datasourceInfo.joinStyle));

        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            sqlP.executeQuery();

            if (sqlP.next()) {
                for (int j = 0; j < partialFields.size(); j++) {
                    ModelField curField = (ModelField) partialFields.get(j);
                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, curField, entity, modelFieldTypeReader);
                }

                entity.synchronizedWithDatasource();
            } else {
                // Debug.logWarning("[GenericDAO.select]: select failed, result set was empty.", module);
                throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
            }
        } finally {
            sqlP.close();
        }
    }

    /* ====================================================================== */
    /* ====================================================================== */

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param whereEntityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@param findOptions An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for more details.
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     *      DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator selectListIteratorByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition,
            EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        if (modelEntity == null) {
            return null;
        }

        // if no find options passed, use default
        if (findOptions == null) findOptions = new EntityFindOptions();

        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Doing selectListIteratorByCondition with whereEntityCondition: " + whereEntityCondition, module);
        }

        // make two ArrayLists of fields, one for fields to select and the other for where clause fields (to find by)
        List selectFields = FastList.newInstance();
        if (fieldsToSelect != null && fieldsToSelect.size() > 0) {
            Set tempKeys = FastSet.newInstance();
            tempKeys.addAll(fieldsToSelect);
            Iterator fieldIter = modelEntity.getFieldsIterator();
            while (fieldIter.hasNext()) {
                ModelField curField = (ModelField) fieldIter.next();
                if (tempKeys.contains(curField.getName())) {
                    selectFields.add(curField);
                    tempKeys.remove(curField.getName());
                }
            }

            if (tempKeys.size() > 0) {
                throw new GenericModelException("In selectListIteratorByCondition invalid field names specified: " + tempKeys.toString());
            }
        } else {
            selectFields = modelEntity.getFieldsCopy();
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (findOptions.getDistinct()) {
            sqlBuffer.append("DISTINCT ");
        }

        if (selectFields.size() > 0) {
            sqlBuffer.append(modelEntity.colNameString(selectFields, ", ", "", datasourceInfo.aliasViews));
        } else {
            sqlBuffer.append("*");
        }

        // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));

        // WHERE clause
        StringBuffer whereString = new StringBuffer();
        String entityCondWhereString = "";
        List whereEntityConditionParams = FastList.newInstance();
        if (whereEntityCondition != null) {
            entityCondWhereString = whereEntityCondition.makeWhereString(modelEntity, whereEntityConditionParams, this.datasourceInfo);
        }

        String viewClause = SqlJdbcUtil.makeViewWhereClause(modelEntity, datasourceInfo.joinStyle);

        if (viewClause.length() > 0) {
            if (entityCondWhereString.length() > 0) {
                whereString.append("(");
                whereString.append(entityCondWhereString);
                whereString.append(") AND ");
            }

            whereString.append(viewClause);
        } else {
            whereString.append(entityCondWhereString);
        }

        if (whereString.length() > 0) {
            sqlBuffer.append(" WHERE ");
            sqlBuffer.append(whereString.toString());
        }

        // GROUP BY clause for view-entity
        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "", false);

            if (UtilValidate.isNotEmpty(groupByString)) {
                sqlBuffer.append(" GROUP BY ");
                sqlBuffer.append(groupByString);
            }
        }

        // HAVING clause
        String entityCondHavingString = "";
        List havingEntityConditionParams = FastList.newInstance();

        if (havingEntityCondition != null) {
            entityCondHavingString = havingEntityCondition.makeWhereString(modelEntity, havingEntityConditionParams, this.datasourceInfo);
        }
        if (entityCondHavingString.length() > 0) {
            sqlBuffer.append(" HAVING ");
            sqlBuffer.append(entityCondHavingString);
        }

        // ORDER BY clause
        sqlBuffer.append(SqlJdbcUtil.makeOrderByClause(modelEntity, orderBy, datasourceInfo));
        String sql = sqlBuffer.toString();

        SQLProcessor sqlP = new SQLProcessor(helperName);
        sqlP.prepareStatement(sql, findOptions.getSpecifyTypeAndConcur(), findOptions.getResultSetType(),
                findOptions.getResultSetConcurrency(), findOptions.getFetchSize(), findOptions.getMaxRows());

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the whereEntityConditionParams: " + whereEntityConditionParams, module);
        }
        // set all of the values from the Where EntityCondition
        Iterator whereEntityConditionParamsIter = whereEntityConditionParams.iterator();
        while (whereEntityConditionParamsIter.hasNext()) {
            EntityConditionParam whereEntityConditionParam = (EntityConditionParam) whereEntityConditionParamsIter.next();

            SqlJdbcUtil.setValue(sqlP, whereEntityConditionParam.getModelField(), modelEntity.getEntityName(), whereEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the havingEntityConditionParams: " + havingEntityConditionParams, module);
        }
        // set all of the values from the Having EntityCondition
        Iterator havingEntityConditionParamsIter = havingEntityConditionParams.iterator();
        while (havingEntityConditionParamsIter.hasNext()) {
            EntityConditionParam havingEntityConditionParam = (EntityConditionParam) havingEntityConditionParamsIter.next();

            SqlJdbcUtil.setValue(sqlP, havingEntityConditionParam.getModelField(), modelEntity.getEntityName(), havingEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }

        long queryStartTime = 0;
        if (Debug.timingOn()) {
            queryStartTime = System.currentTimeMillis();
        }
        sqlP.executeQuery();
        if (Debug.timingOn()) {
            long queryEndTime = System.currentTimeMillis();
            long queryTotalTime = queryEndTime - queryStartTime;
            if (queryTotalTime > 150) {
                Debug.logTiming("Ran query in " + queryTotalTime + " milli-seconds: " + sql, module);
            }
        }        
        return new EntityListIterator(sqlP, modelEntity, selectFields, modelFieldTypeReader);
    }

    public List selectByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
        ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List orderBy) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);

        // get the tables names
        String atable = modelEntityOne.getTableName(datasourceInfo);
        String ttable = modelEntityTwo.getTableName(datasourceInfo);

        // get the column name string to select
        StringBuffer selsb = new StringBuffer();
        List collist = FastList.newInstance();
        List fldlist = FastList.newInstance();

        for (Iterator iterator = modelEntityTwo.getFieldsIterator(); iterator.hasNext();) {
            ModelField mf = (ModelField) iterator.next();

            collist.add(mf.getColName());
            fldlist.add(mf.getName());
            selsb.append(ttable + "." + mf.getColName());
            if (iterator.hasNext()) {
                selsb.append(", ");
            } else {
                selsb.append(" ");
            }
        }

        // construct assoc->target relation string
        int kmsize = modelRelationTwo.getKeyMapsSize();
        StringBuffer wheresb = new StringBuffer();

        for (int i = 0; i < kmsize; i++) {
            ModelKeyMap mkm = modelRelationTwo.getKeyMap(i);
            String lfname = mkm.getFieldName();
            String rfname = mkm.getRelFieldName();

            if (wheresb.length() > 0) {
                wheresb.append(" AND ");
            }
            wheresb.append(atable + "." + modelEntityOne.getField(lfname).getColName() + " = " + ttable + "." + modelEntityTwo.getField(rfname).getColName());
        }

        // construct the source entity qualifier
        // get the fields from relation description
        kmsize = modelRelationOne.getKeyMapsSize();
        Map bindMap = FastMap.newInstance();

        for (int i = 0; i < kmsize; i++) {
            // get the equivalent column names in the relation
            ModelKeyMap mkm = modelRelationOne.getKeyMap(i);
            String sfldname = mkm.getFieldName();
            String lfldname = mkm.getRelFieldName();
            ModelField amf = modelEntityOne.getField(lfldname);
            String lcolname = amf.getColName();
            Object rvalue = value.get(sfldname);

            bindMap.put(amf, rvalue);
            // construct one condition
            if (wheresb.length() > 0) {
                wheresb.append(" AND ");
            }
            wheresb.append(atable + "." + lcolname + " = ? ");
        }

        // construct a join sql query
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append("SELECT ");
        sqlsb.append(selsb.toString());
        sqlsb.append(" FROM ");
        sqlsb.append(atable + ", " + ttable);
        sqlsb.append(" WHERE ");
        sqlsb.append(wheresb.toString());
        sqlsb.append(SqlJdbcUtil.makeOrderByClause(modelEntityTwo, orderBy, true, datasourceInfo));

        // now execute the query
        List retlist = FastList.newInstance();
        GenericDelegator gd = value.getDelegator();

        try {
            sqlP.prepareStatement(sqlsb.toString());
            Set entrySet = bindMap.entrySet();

            for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                ModelField mf = (ModelField) entry.getKey();
                Object curvalue = entry.getValue();

                SqlJdbcUtil.setValue(sqlP, mf, modelEntityOne.getEntityName(), curvalue, modelFieldTypeReader);
            }
            sqlP.executeQuery();
            //int collsize = collist.size();

            while (sqlP.next()) {
                GenericValue gv = gd.makeValue(modelEntityTwo.getEntityName(), Collections.EMPTY_MAP);

                // loop thru all columns for in one row
                int idx = 1;
                Iterator fldIter = fldlist.iterator();
                while (fldIter.hasNext()) {
                    String fldname = (String) fldIter.next();
                    ModelField mf = modelEntityTwo.getField(fldname);
                    SqlJdbcUtil.getValue(sqlP.getResultSet(), idx, mf, gv, modelFieldTypeReader);
                    idx++;
                }
                retlist.add(gv);
            }
        } finally {
            sqlP.close();
        }

        return retlist;
    }

    public long selectCountByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        if (modelEntity == null) {
            return 0;
        }

        // if no find options passed, use default
        if (findOptions == null) {
            findOptions = new EntityFindOptions();
        }
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Doing selectListIteratorByCondition with whereEntityCondition: " + whereEntityCondition, module);
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT ");

        if (findOptions.getDistinct()) {
            sqlBuffer.append("DISTINCT ");
        }

        sqlBuffer.append("COUNT(*) ");

        // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, datasourceInfo));

        // WHERE clause
        StringBuffer whereString = new StringBuffer();
        String entityCondWhereString = "";
        List whereEntityConditionParams = FastList.newInstance();
        if (whereEntityCondition != null) {
            entityCondWhereString = whereEntityCondition.makeWhereString(modelEntity, whereEntityConditionParams, this.datasourceInfo);
        }

        String viewClause = SqlJdbcUtil.makeViewWhereClause(modelEntity, datasourceInfo.joinStyle);

        if (viewClause.length() > 0) {
            if (entityCondWhereString.length() > 0) {
                whereString.append("(");
                whereString.append(entityCondWhereString);
                whereString.append(") AND ");
            }

            whereString.append(viewClause);
        } else {
            whereString.append(entityCondWhereString);
        }

        if (whereString.length() > 0) {
            sqlBuffer.append(" WHERE ");
            sqlBuffer.append(whereString.toString());
        }

        // GROUP BY clause for view-entity
        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "", false);

            if (UtilValidate.isNotEmpty(groupByString)) {
                sqlBuffer.append(" GROUP BY ");
                sqlBuffer.append(groupByString);
            }
        }

        // HAVING clause
        String entityCondHavingString = "";
        List havingEntityConditionParams = FastList.newInstance();
        if (havingEntityCondition != null) {
            entityCondHavingString = havingEntityCondition.makeWhereString(modelEntity, havingEntityConditionParams, this.datasourceInfo);
        }
        if (entityCondHavingString.length() > 0) {
            sqlBuffer.append(" HAVING ");
            sqlBuffer.append(entityCondHavingString);
        }

        String sql = sqlBuffer.toString();

        SQLProcessor sqlP = new SQLProcessor(helperName);
        sqlP.prepareStatement(sql, findOptions.getSpecifyTypeAndConcur(), findOptions.getResultSetType(),
                findOptions.getResultSetConcurrency(), findOptions.getFetchSize(), findOptions.getMaxRows());
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the whereEntityConditionParams: " + whereEntityConditionParams, module);
        }
        // set all of the values from the Where EntityCondition
        Iterator whereEntityConditionParamsIter = whereEntityConditionParams.iterator();
        while (whereEntityConditionParamsIter.hasNext()) {
            EntityConditionParam whereEntityConditionParam = (EntityConditionParam) whereEntityConditionParamsIter.next();
            SqlJdbcUtil.setValue(sqlP, whereEntityConditionParam.getModelField(), modelEntity.getEntityName(), whereEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            Debug.logVerbose("Setting the havingEntityConditionParams: " + havingEntityConditionParams, module);
        }
        // set all of the values from the Having EntityCondition
        Iterator havingEntityConditionParamsIter = havingEntityConditionParams.iterator();
        while (havingEntityConditionParamsIter.hasNext()) {
            EntityConditionParam havingEntityConditionParam = (EntityConditionParam) havingEntityConditionParamsIter.next();
            SqlJdbcUtil.setValue(sqlP, havingEntityConditionParam.getModelField(), modelEntity.getEntityName(), havingEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }


        try {
            sqlP.executeQuery();
            long count = 0;
            ResultSet resultSet = sqlP.getResultSet();
            if (resultSet.next()) {
                count = resultSet.getLong(1);
            }
            return count;
        } catch (SQLException e) {
            throw new GenericDataSourceException("Error getting count value", e);
        } finally {
            sqlP.close();
        }
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public int delete(GenericEntity entity) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            return delete(entity, sqlP);
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Exception while deleting the following entity: " + entity.toString(), e);
        } finally {
            sqlP.close();
        }
    }

    public int delete(GenericEntity entity, SQLProcessor sqlP) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();
        if (modelEntity == null) {
            throw new GenericModelException("Could not find ModelEntity record for entityName: " + entity.getEntityName());
        }
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.entity.GenericNotImplementedException("Operation delete not supported yet for view entities");
        }

        String sql = "DELETE FROM " + modelEntity.getTableName(datasourceInfo) + " WHERE " + SqlJdbcUtil.makeWhereStringFromFields(modelEntity.getPksCopy(), entity, "AND");

        int retVal;

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            retVal = sqlP.executeUpdate();
            entity.removedFromDatasource();
        } finally {
            sqlP.close();
        }
        return retVal;
    }

    public int deleteByCondition(ModelEntity modelEntity, EntityCondition condition) throws GenericEntityException {
        SQLProcessor sqlP = new SQLProcessor(helperName);

        try {
            return deleteByCondition(modelEntity, condition, sqlP);
        } catch (GenericDataSourceException e) {
            sqlP.rollback();
            throw new GenericDataSourceException("Generic Entity Exception occured in deleteByCondition", e);
        } finally {
            sqlP.close();
        }
    }

    public int deleteByCondition(ModelEntity modelEntity, EntityCondition condition, SQLProcessor sqlP) throws GenericEntityException {
        if (modelEntity == null || condition == null)
            return 0;
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.ofbiz.entity.GenericNotImplementedException("Operation deleteByCondition not supported yet for view entities");
        }

        String sql = "DELETE FROM " + modelEntity.getTableName(this.datasourceInfo);

        sql += " WHERE " + condition.makeWhereString(modelEntity, null, this.datasourceInfo);

        try {
            sqlP.prepareStatement(sql);

            return sqlP.executeUpdate();
        } finally {
            sqlP.close();
        }
    }

    /* ====================================================================== */

    public void checkDb(Map modelEntities, List messages, boolean addMissing) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperName);
        dbUtil.checkDb(modelEntities, messages, addMissing);
    }

    /** Creates a list of ModelEntity objects based on meta data from the database */
    public List induceModelFromDb(Collection messages) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperName);
        return dbUtil.induceModelFromDb(messages);
    }
}
