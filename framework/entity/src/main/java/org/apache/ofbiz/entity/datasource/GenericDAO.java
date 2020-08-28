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
package org.apache.ofbiz.entity.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.EntityLockedException;
import org.apache.ofbiz.entity.GenericDataSourceException;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.GenericNotImplementedException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionParam;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.jdbc.DatabaseUtil;
import org.apache.ofbiz.entity.jdbc.SQLProcessor;
import org.apache.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Generic Entity Data Access Object - Handles persistence for any defined entity.
 */
public class GenericDAO {

    private static final String MODULE = GenericDAO.class.getName();

    private static final ConcurrentHashMap<String, GenericDAO> GENERIC_DAOS = new ConcurrentHashMap<>();
    private final GenericHelperInfo helperInfo;
    private final ModelFieldTypeReader modelFieldTypeReader;
    private final Datasource datasource;

    public GenericDAO(GenericHelperInfo helperInfo) {
        this.helperInfo = helperInfo;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperInfo.getHelperBaseName());
        this.datasource = EntityConfig.getDatasource(helperInfo.getHelperBaseName());
    }

    public static GenericDAO getGenericDAO(GenericHelperInfo helperInfo) {
        String cacheKey = helperInfo.getHelperFullName();
        GenericDAO newGenericDAO = GENERIC_DAOS.get(cacheKey);
        if (newGenericDAO == null) {
            GENERIC_DAOS.putIfAbsent(cacheKey, new GenericDAO(helperInfo));
            newGenericDAO = GENERIC_DAOS.get(cacheKey);
        }
        return newGenericDAO;
    }

    private static void addFieldIfMissing(List<ModelField> fieldsToSave, String fieldName, ModelEntity modelEntity) {
        for (ModelField fieldToSave : fieldsToSave) {
            if (fieldName.equals(fieldToSave.getName())) {
                return;
            }
        }
        // at this point we didn't find it
        fieldsToSave.add(modelEntity.getField(fieldName));
    }

    /**
     * Insert int.
     * @param entity the entity
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int insert(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        try (SQLProcessor sqlP = new SQLProcessor(entity.getDelegator(), helperInfo)) {
            try {
                return singleInsert(entity, modelEntity, modelEntity.getFieldsUnmodifiable(), sqlP);
            } catch (GenericEntityException e) {
                sqlP.rollback();
                // no need to create nested, just throw original which will have all info: throw new
                // GenericEntityException("Exception while inserting the following entity: " + entity.toString(), e);
                throw e;
            }
        }
    }

    private int singleInsert(GenericEntity entity, ModelEntity modelEntity, List<ModelField> fieldsToSave, SQLProcessor sqlP)
            throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, sqlP);
        }

        // if we have a STAMP_TX_FIELD or CREATE_STAMP_TX_FIELD then set it with NOW, always do this before the STAMP_FIELD
        // NOTE: these fairly complicated if statements have a few objectives:
        //   1. don't run the TransationUtil.getTransaction*Stamp() methods when we don't need to
        //   2. don't set the stamp values if it is from an EntitySync (ie maintain original values), unless the stamps are null then set it
        // anyway, ie even if it was from an EntitySync (also used for imports and such)
        boolean stampTxIsField = modelEntity.isField(ModelEntity.STAMP_TX_FIELD);
        boolean createStampTxIsField = modelEntity.isField(ModelEntity.CREATE_STAMP_TX_FIELD);
        if ((stampTxIsField || createStampTxIsField) && (!entity.getIsFromEntitySync()
                || (stampTxIsField && entity.get(ModelEntity.STAMP_TX_FIELD) == null)
                || (createStampTxIsField && entity.get(ModelEntity.CREATE_STAMP_TX_FIELD) == null))) {
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
        if ((stampIsField || createStampIsField) && (!entity.getIsFromEntitySync() || (stampIsField && entity.get(ModelEntity.STAMP_FIELD) == null)
                || (createStampIsField && entity.get(ModelEntity.CREATE_STAMP_FIELD) == null))) {
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

        StringBuilder sqlB = new StringBuilder("INSERT INTO ").append(modelEntity.getTableName(datasource)).append(" (");

        modelEntity.colNameString(fieldsToSave, sqlB, "");
        sqlB.append(") VALUES (");
        modelEntity.fieldsStringList(fieldsToSave, sqlB, "?", ", ");
        String sql = sqlB.append(")").toString();

        try {
            sqlP.prepareStatement(sql);
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            int retVal = sqlP.executeUpdate();

            entity.synchronizedWithDatasource();
            return retVal;
        } catch (GenericEntityException e) {
            throw new GenericEntityException("Error while inserting: " + entity.toString(), e);
        }
    }

    /**
     * Update all int.
     * @param entity the entity
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int updateAll(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        return customUpdate(entity, modelEntity, modelEntity.getNopksCopy());
    }

    /**
     * Update int.
     * @param entity the entity
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int update(GenericEntity entity) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        // we don't want to update ALL fields, just the nonpk fields that are in the passed GenericEntity
        List<ModelField> partialFields = new LinkedList<>();
        Collection<String> keys = entity.getAllKeys();

        Iterator<ModelField> nopkIter = modelEntity.getNopksIterator();
        while (nopkIter.hasNext()) {
            ModelField curField = nopkIter.next();
            if (keys.contains(curField.getName())) {
                partialFields.add(curField);
            }
        }

        return customUpdate(entity, modelEntity, partialFields);
    }

    private int customUpdate(GenericEntity entity, ModelEntity modelEntity, List<ModelField> fieldsToSave) throws GenericEntityException {
        try (SQLProcessor sqlP = new SQLProcessor(entity.getDelegator(), helperInfo)) {
            try {
                return singleUpdate(entity, modelEntity, fieldsToSave, sqlP);
            } catch (GenericEntityException e) {
                sqlP.rollback();
                // no need to create nested, just throw original which will have all info: throw new
                // GenericEntityException("Exception while updating the following entity: " + entity.toString(), e);
                throw e;
            }
        }
    }

    private int singleUpdate(GenericEntity entity, ModelEntity modelEntity, List<ModelField> fieldsToSave, SQLProcessor sqlP)
            throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            return singleUpdateView(entity, (ModelViewEntity) modelEntity, fieldsToSave, sqlP);
        }

        // no non-primaryKey fields, update doesn't make sense, so don't do it
        if (fieldsToSave.size() <= 0) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Trying to do an update on an entity with no non-PK fields, returning having done nothing; entity=" + entity,
                        MODULE);
            }
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
        //   2. don't set the stamp values if it is from an EntitySync (ie maintain original values), unless the stamps are null then set it
        // anyway, ie even if it was from an EntitySync (also used for imports and such)
        if (modelEntity.isField(ModelEntity.STAMP_TX_FIELD) && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_TX_FIELD) == null)) {
            entity.set(ModelEntity.STAMP_TX_FIELD, TransactionUtil.getTransactionStartStamp());
            addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_TX_FIELD, modelEntity);
        }

        // if we have a STAMP_FIELD then update it with NOW.
        if (modelEntity.isField(ModelEntity.STAMP_FIELD) && (!entity.getIsFromEntitySync() || entity.get(ModelEntity.STAMP_FIELD) == null)) {
            entity.set(ModelEntity.STAMP_FIELD, TransactionUtil.getTransactionUniqueNowStamp());
            addFieldIfMissing(fieldsToSave, ModelEntity.STAMP_FIELD, modelEntity);
        }

        StringBuilder sql = new StringBuilder().append("UPDATE ").append(modelEntity.getTableName(datasource)).append(" SET ");
        modelEntity.colNameString(fieldsToSave, sql, "", "=?, ", "=?", false);
        sql.append(" WHERE ");
        SqlJdbcUtil.makeWhereStringFromFields(sql, modelEntity.getPkFieldsUnmodifiable(), entity, "AND");

        int retVal = 0;

        try {
            sqlP.prepareStatement(sql.toString());
            SqlJdbcUtil.setValues(sqlP, fieldsToSave, entity, modelFieldTypeReader);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            retVal = sqlP.executeUpdate();
            entity.synchronizedWithDatasource();
        } catch (GenericEntityException e) {
            throw new GenericEntityException("Error while updating: " + entity.toString(), e);
        }

        if (retVal == 0) {
            throw new GenericEntityNotFoundException("Tried to update an entity that does not exist, entity: " + entity.toString());
        }
        return retVal;
    }

    /**
     * Update by condition int.
     * @param delegator the delegator
     * @param modelEntity the model entity
     * @param fieldsToSet the fields to set
     * @param condition the condition
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int updateByCondition(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fieldsToSet,
                                 EntityCondition condition) throws GenericEntityException {

        try (SQLProcessor sqlP = new SQLProcessor(delegator, helperInfo)) {
            try {
                return updateByCondition(modelEntity, fieldsToSet, condition, sqlP);
            } catch (GenericDataSourceException e) {
                sqlP.rollback();
                throw new GenericDataSourceException("Generic Entity Exception occurred in updateByCondition", e);
            }
        }
    }

    /**
     * Update by condition int.
     * @param modelEntity the model entity
     * @param fieldsToSet the fields to set
     * @param condition the condition
     * @param sqlP the sql p
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int updateByCondition(ModelEntity modelEntity, Map<String, ? extends Object> fieldsToSet, EntityCondition condition, SQLProcessor sqlP)
            throws GenericEntityException {
        if (modelEntity == null || fieldsToSet == null || condition == null) {
            return 0;
        }
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.apache.ofbiz.entity.GenericNotImplementedException("Operation updateByCondition not supported yet for view entities");
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(modelEntity.getTableName(datasource));
        sql.append(" SET ");
        List<EntityConditionParam> params = new LinkedList<>();
        for (Map.Entry<String, ? extends Object> entry : fieldsToSet.entrySet()) {
            String name = entry.getKey();
            ModelField field = modelEntity.getField(name);
            if (field != null) {
                if (!params.isEmpty()) {
                    sql.append(", ");
                }
                sql.append(field.getColName()).append(" = ?");
                params.add(new EntityConditionParam(field, entry.getValue()));
            }
        }
        sql.append(" WHERE ").append(condition.makeWhereString(modelEntity, params, this.datasource));

        sqlP.prepareStatement(sql.toString());
        for (EntityConditionParam param : params) {
            SqlJdbcUtil.setValue(sqlP, param.getModelField(), modelEntity.getEntityName(), param.getFieldValue(), modelFieldTypeReader);
        }

        return sqlP.executeUpdate();
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * Try to update the given ModelViewEntity by trying to insert/update on the entities of which the view is composed.
     * <p>
     * Works fine with standard O/R mapped models, but has some restrictions meeting more complicated view entities.
     * <li>A direct link is required, which means that one of the ModelViewLink field entries must have a value found
     * in the given view entity, for each ModelViewLink</li>
     * <li>For now, each member entity is updated iteratively, so if eg. the second member entity fails to update,
     * the first is written although. See code for details. Try to use "clean" views, until code is more robust ...</li>
     * <li>For now, aliased field names in views are not processed correctly, I guess. To be honest, I did not
     * find out how to construct such a view - so view fieldnames must have same named fields in member entities.</li>
     * <li>A new exception, e.g. GenericViewNotUpdatable, should be defined and thrown if the update fails</li>
     */
    private int singleUpdateView(GenericEntity entity, ModelViewEntity modelViewEntity, List<ModelField> fieldsToSave, SQLProcessor sqlP)
            throws GenericEntityException {
        Delegator delegator = entity.getDelegator();

        int retVal = 0;
        ModelEntity memberModelEntity = null;

        // Construct insert/update for each model entity
        for (ModelViewEntity.ModelMemberEntity modelMemberEntity : modelViewEntity.getMemberModelMemberEntities().values()) {
            String meName = modelMemberEntity.getEntityName();
            String meAlias = modelMemberEntity.getEntityAlias();

            if (Debug.verboseOn()) {
                Debug.logVerbose("[singleUpdateView]: Processing MemberEntity " + meName + " with Alias " + meAlias, MODULE);
            }
            try {
                memberModelEntity = delegator.getModelReader().getModelEntity(meName);
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Failed to get model entity for " + meName, e);
            }

            Map<String, Object> findByMap = new HashMap<>();

            // Now iterate the ModelViewLinks to construct the "WHERE" part for update/insert
            Iterator<ModelViewEntity.ModelViewLink> linkIter = modelViewEntity.getViewLinksIterator();

            while (linkIter != null && linkIter.hasNext()) {
                ModelViewEntity.ModelViewLink modelViewLink = linkIter.next();

                if (modelViewLink.getEntityAlias().equals(meAlias) || modelViewLink.getRelEntityAlias().equals(meAlias)) {

                    Iterator<ModelKeyMap> kmIter = modelViewLink.getKeyMapsIterator();

                    while (kmIter != null && kmIter.hasNext()) {
                        ModelKeyMap keyMap = kmIter.next();

                        String fieldName = "";

                        if (modelViewLink.getEntityAlias().equals(meAlias)) {
                            fieldName = keyMap.getFieldName();
                        } else {
                            fieldName = keyMap.getRelFieldName();
                        }

                        if (Debug.verboseOn()) {
                            Debug.logVerbose("[singleUpdateView]: --- Found field to set: " + meAlias + "." + fieldName, MODULE);
                        }
                        Object value = null;

                        if (modelViewEntity.isField(keyMap.getFieldName())) {
                            value = entity.get(keyMap.getFieldName());
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString(), MODULE);
                            }
                        } else if (modelViewEntity.isField(keyMap.getRelFieldName())) {
                            value = entity.get(keyMap.getRelFieldName());
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("[singleUpdateView]: --- Found map value: " + value.toString(), MODULE);
                            }
                        } else {
                            throw new GenericNotImplementedException("Update on view entities: no direct link found, unable to update");
                        }

                        findByMap.put(fieldName, value);
                    }
                }
            }

            // Look what there already is in the database
            List<GenericValue> meResult = null;

            try {
                meResult = EntityQuery.use(delegator).from(meName).where(findByMap).queryList();
            } catch (GenericEntityException e) {
                throw new GenericEntityException("Error while retrieving partial results for entity member: " + meName, e);
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("[singleUpdateView]: --- Found " + meResult.size() + " results for entity member " + meName, MODULE);
            }

            // Got results 0 -> INSERT, 1 -> UPDATE, >1 -> View is nor updatable
            GenericValue meGenericValue = null;

            if (meResult.isEmpty()) {
                // Create new value to insert
                try {
                    // Create new value to store
                    meGenericValue = delegator.makeValue(meName, findByMap);
                } catch (Exception e) {
                    throw new GenericEntityException("Could not create new value for member entity" + meName + " of view "
                            + modelViewEntity.getEntityName(), e);
                }
            } else if (meResult.size() == 1) {
                // Update existing value
                meGenericValue = meResult.iterator().next();
            } else {
                throw new GenericEntityException("Found more than one result for member entity " + meName + " in view "
                        + modelViewEntity.getEntityName() + " - this is no updatable view");
            }

            // Construct fieldsToSave list for this member entity
            List<ModelField> meFieldsToSave = new LinkedList<>();
            for (ModelField modelField : fieldsToSave) {
                if (memberModelEntity.isField(modelField.getName())) {
                    ModelField meModelField = memberModelEntity.getField(modelField.getName());

                    if (meModelField != null) {
                        meGenericValue.set(meModelField.getName(), entity.get(modelField.getName()));
                        meFieldsToSave.add(meModelField);
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("[singleUpdateView]: --- Added field to save: " + meModelField.getName() + " with value "
                                    + meGenericValue.get(meModelField.getName()), MODULE);
                        }
                    } else {
                        throw new GenericEntityException("Could not get field " + modelField.getName() + " from model entity "
                                + memberModelEntity.getEntityName());
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
            if (meResult.isEmpty()) {
                retVal += singleInsert(meGenericValue, memberModelEntity, memberModelEntity.getFieldsUnmodifiable(), sqlP);
            } else {
                if (!meFieldsToSave.isEmpty()) {
                    retVal += singleUpdate(meGenericValue, memberModelEntity, meFieldsToSave, sqlP);
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("[singleUpdateView]: No update on member entity " + memberModelEntity.getEntityName() + " needed", MODULE);
                    }
                }
            }
        }

        return retVal;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * Select.
     * @param entity the entity
     * @throws GenericEntityException the generic entity exception
     */
    public void select(GenericEntity entity) throws GenericEntityException {
        try (SQLProcessor sqlP = new SQLProcessor(entity.getDelegator(), helperInfo)) {
            select(entity, sqlP);
        }
    }

    /**
     * Select.
     * @param entity the entity
     * @param sqlP the sql p
     * @throws GenericEntityException the generic entity exception
     */
    public void select(GenericEntity entity, SQLProcessor sqlP) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity.getPksSize() <= 0) {
            throw new GenericEntityException("Entity has no primary keys, cannot select by primary key");
        }

        StringBuilder sqlBuffer = new StringBuilder("SELECT ");

        if (modelEntity.getNopksSize() > 0) {
            modelEntity.colNameString(modelEntity.getNopksCopy(), sqlBuffer, "", ", ", "", datasource.getAliasViewColumns());
        } else {
            sqlBuffer.append("*");
        }

        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, modelFieldTypeReader, datasource));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPkFieldsUnmodifiable(), entity, "AND", datasource.getJoinStyle()));

        sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
        sqlP.executeQuery();

        if (sqlP.next()) {
            int idx = 1;
            Iterator<ModelField> nopkIter = modelEntity.getNopksIterator();
            while (nopkIter.hasNext()) {
                ModelField curField = nopkIter.next();
                SqlJdbcUtil.getValue(sqlP.getResultSet(), idx, curField, entity, modelFieldTypeReader);
                idx++;
            }

            entity.synchronizedWithDatasource();
        } else {
            // Debug.logWarning("[GenericDAO.select]: select failed, result set was empty for entity: " + entity.toString(), MODULE);
            throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
        }
    }

    /**
     * Partial select.
     * @param entity the entity
     * @param keys the keys
     * @throws GenericEntityException the generic entity exception
     */
    public void partialSelect(GenericEntity entity, Set<String> keys) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity instanceof ModelViewEntity) {
            throw new org.apache.ofbiz.entity.GenericNotImplementedException("Operation partialSelect not supported yet for view entities");
        }

        // we don't want to select ALL fields, just the nonpk fields that are in the passed GenericEntity
        List<ModelField> partialFields = new LinkedList<>();

        Set<String> tempKeys = new TreeSet<>(keys);

        Iterator<ModelField> entityFieldIter = modelEntity.getFieldsIterator();
        while (entityFieldIter.hasNext()) {
            ModelField curField = entityFieldIter.next();
            if (tempKeys.contains(curField.getName())) {
                partialFields.add(curField);
                tempKeys.remove(curField.getName());
            }
        }

        if (!tempKeys.isEmpty()) {
            throw new GenericModelException("In partialSelect invalid field names specified: " + tempKeys.toString());
        }

        StringBuilder sqlBuffer = new StringBuilder("SELECT ");

        if (!partialFields.isEmpty()) {
            modelEntity.colNameString(partialFields, sqlBuffer, "", ", ", "", datasource.getAliasViewColumns());
        } else {
            sqlBuffer.append("*");
        }
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, modelFieldTypeReader, datasource));
        sqlBuffer.append(SqlJdbcUtil.makeWhereClause(modelEntity, modelEntity.getPkFieldsUnmodifiable(), entity, "AND", datasource.getJoinStyle()));

        try (SQLProcessor sqlP = new SQLProcessor(entity.getDelegator(), helperInfo)) {
            sqlP.prepareStatement(sqlBuffer.toString(), true, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
            sqlP.executeQuery();

            if (sqlP.next()) {
                for (int j = 0; j < partialFields.size(); j++) {
                    ModelField curField = partialFields.get(j);
                    SqlJdbcUtil.getValue(sqlP.getResultSet(), j + 1, curField, entity, modelFieldTypeReader);
                }

                entity.synchronizedWithDatasource();
            } else {
                throw new GenericEntityNotFoundException("Result set was empty for entity: " + entity.toString());
            }
        }
    }

    /* ====================================================================== */
    /* ====================================================================== */

    /**
     * Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     * @param modelEntity           The ModelEntity of the Entity as defined in the entity XML file
     * @param whereEntityCondition  The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is
     *                             a view entity with group-by aliases)
     * @param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is
     *                              a view entity with group-by aliases)
     * @param fieldsToSelect        The fields of the named entity to get from the database; if empty or null all fields will be retreived
     * @param orderBy               The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for
     *                              descending
     * @param findOptions           An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for
     *                              more details.
     * @return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     * DONE WITH IT (preferably in a finally block),
     * AND DON'T LEAVE IT OPEN TOO LONG BECAUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator selectListIteratorByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                                            EntityCondition havingEntityCondition, Collection<String> fieldsToSelect,
                                                            List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        if (modelEntity == null) {
            return null;
        }

        ModelViewEntity modelViewEntity = null;
        if (modelEntity instanceof ModelViewEntity) {
            modelViewEntity = (ModelViewEntity) modelEntity;
        }

        // if no find options passed, use default
        if (findOptions == null) findOptions = new EntityFindOptions();

        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            if (Debug.verboseOn()) {
                Debug.logVerbose("Doing selectListIteratorByCondition with whereEntityCondition: " + whereEntityCondition, MODULE);
            }
        }

        // make two ArrayLists of fields, one for fields to select and the other for where clause fields (to find by)
        List<ModelField> selectFields = new LinkedList<>();
        if (UtilValidate.isNotEmpty(fieldsToSelect)) {
            Set<String> tempKeys = new HashSet<>();
            tempKeys.addAll(fieldsToSelect);
            Set<String> fieldSetsToInclude = new HashSet<>();
            Set<String> addedFields = new HashSet<>();
            for (String fieldToSelect : fieldsToSelect) {
                if (tempKeys.contains(fieldToSelect)) {
                    ModelField curField = modelEntity.getField(fieldToSelect);
                    if (curField != null) {
                        fieldSetsToInclude.add(curField.getFieldSet());
                        selectFields.add(curField);
                        tempKeys.remove(fieldToSelect);
                        addedFields.add(fieldToSelect);
                    }
                }
            }

            if (!tempKeys.isEmpty()) {
                throw new GenericModelException("In selectListIteratorByCondition invalid field names specified: " + tempKeys.toString());
            }
            fieldSetsToInclude.remove("");
            if (verboseOn) {
                Debug.logInfo("[" + modelEntity.getEntityName() + "]: field-sets to include: " + fieldSetsToInclude, MODULE);
            }
            if (UtilValidate.isNotEmpty(fieldSetsToInclude)) {
                Iterator<ModelField> fieldIter = modelEntity.getFieldsIterator();
                Set<String> extraFields = new HashSet<>();
                Set<String> reasonSets = new HashSet<>();
                while (fieldIter.hasNext()) {
                    ModelField curField = fieldIter.next();
                    String fieldSet = curField.getFieldSet();
                    if (UtilValidate.isEmpty(fieldSet)) {
                        continue;
                    }
                    if (!fieldSetsToInclude.contains(fieldSet)) {
                        continue;
                    }
                    String fieldName = curField.getName();
                    if (addedFields.contains(fieldName)) {
                        continue;
                    }
                    reasonSets.add(fieldSet);
                    extraFields.add(fieldName);
                    addedFields.add(fieldName);
                    selectFields.add(curField);
                }
                if (verboseOn) {
                    Debug.logInfo("[" + modelEntity.getEntityName() + "]: auto-added select fields: " + extraFields, MODULE);
                    Debug.logInfo("[" + modelEntity.getEntityName() + "]: auto-added field-sets: " + reasonSets, MODULE);
                }
            }
        } else {
            selectFields = modelEntity.getFieldsUnmodifiable();
        }

        StringBuilder sqlBuffer = new StringBuilder("SELECT ");

        if (findOptions.getDistinct()) {
            sqlBuffer.append("DISTINCT ");
        }

        if (!selectFields.isEmpty()) {
            modelEntity.colNameString(selectFields, sqlBuffer, "", ", ", "", datasource.getAliasViewColumns());
        } else {
            sqlBuffer.append("*");
        }

        // populate the info from entity-condition in the view-entity, if it is one and there is one
        List<EntityCondition> viewWhereConditions = null;
        List<EntityCondition> viewHavingConditions = null;
        List<String> viewOrderByList = null;
        if (modelViewEntity != null) {
            viewWhereConditions = new LinkedList<>();
            viewHavingConditions = new LinkedList<>();
            viewOrderByList = new LinkedList<>();
            modelViewEntity.populateViewEntityConditionInformation(modelFieldTypeReader, viewWhereConditions, viewHavingConditions, viewOrderByList,
                    null);
        }

        // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, modelFieldTypeReader, datasource));

        // WHERE clause
        List<EntityConditionParam> whereEntityConditionParams = new LinkedList<>();
        makeConditionWhereString(sqlBuffer, " WHERE ", modelEntity, whereEntityCondition, viewWhereConditions, whereEntityConditionParams);

        // GROUP BY clause for view-entity
        if (modelViewEntity != null) {
            modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(selectFields), sqlBuffer, " GROUP BY ", ", ", "", false);
        }

        // HAVING clause
        List<EntityConditionParam> havingEntityConditionParams = new LinkedList<>();
        makeConditionHavingString(sqlBuffer, " HAVING ", modelEntity, havingEntityCondition, viewHavingConditions, havingEntityConditionParams);

        // ORDER BY clause
        List<String> orderByExpanded = new LinkedList<>();
        // add the manually specified ones, then the ones in the view entity's entity-condition
        if (orderBy != null) {
            orderByExpanded.addAll(orderBy);
        }
        if (viewOrderByList != null) {
            // add to end of other order by so that those in method call will override those in view
            orderByExpanded.addAll(viewOrderByList);
        }
        sqlBuffer.append(SqlJdbcUtil.makeOrderByClause(modelEntity, orderByExpanded, datasource));

        // OFFSET clause
        makeOffsetString(sqlBuffer, findOptions);

        // make the final SQL String
        String sql = sqlBuffer.toString();

        SQLProcessor sqlP = new SQLProcessor(delegator, helperInfo);
        sqlP.prepareStatement(sql, findOptions.getSpecifyTypeAndConcur(), findOptions.getResultSetType(),
                findOptions.getResultSetConcurrency(), findOptions.getFetchSize(), findOptions.getMaxRows());

        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            if (Debug.verboseOn()) {
                Debug.logVerbose("Setting the whereEntityConditionParams: " + whereEntityConditionParams, MODULE);
            }
        }
        // set all of the values from the Where EntityCondition
        for (EntityConditionParam whereEntityConditionParam : whereEntityConditionParams) {
            SqlJdbcUtil.setValue(sqlP, whereEntityConditionParam.getModelField(), modelEntity.getEntityName(),
                    whereEntityConditionParam.getFieldValue(), modelFieldTypeReader);
        }
        if (verboseOn) {
            // put this inside an if statement so that we don't have to generate the string when not used...
            if (Debug.verboseOn()) {
                Debug.logVerbose("Setting the havingEntityConditionParams: " + havingEntityConditionParams, MODULE);
            }
        }
        // set all of the values from the Having EntityCondition
        for (EntityConditionParam havingEntityConditionParam : havingEntityConditionParams) {
            SqlJdbcUtil.setValue(sqlP, havingEntityConditionParam.getModelField(), modelEntity.getEntityName(),
                    havingEntityConditionParam.getFieldValue(), modelFieldTypeReader);
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
                Debug.logTiming("Ran query in " + queryTotalTime + " milli-seconds: " + " EntityName: " + modelEntity.getEntityName() + " Sql: "
                        + sql + " where clause:" + whereEntityConditionParams, MODULE);
            }
        }
        return new EntityListIterator(sqlP, modelEntity, selectFields, modelFieldTypeReader, this, whereEntityCondition, havingEntityCondition,
                findOptions.getDistinct());
    }

    /**
     * Make condition where string string builder.
     * @param modelEntity the model entity
     * @param whereEntityCondition the where entity condition
     * @param viewWhereConditions the view where conditions
     * @param whereEntityConditionParams the where entity condition params
     * @return the string builder
     * @throws GenericEntityException the generic entity exception
     */
    @Deprecated
    protected StringBuilder makeConditionWhereString(ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                                     List<EntityCondition> viewWhereConditions,
                                                     List<EntityConditionParam> whereEntityConditionParams) throws GenericEntityException {
        return makeConditionWhereString(new StringBuilder(), "", modelEntity, whereEntityCondition, viewWhereConditions, whereEntityConditionParams);
    }

    /**
     * Make condition where string string builder.
     * @param whereString the where string
     * @param prefix the prefix
     * @param modelEntity the model entity
     * @param whereEntityCondition the where entity condition
     * @param viewWhereConditions the view where conditions
     * @param whereEntityConditionParams the where entity condition params
     * @return the string builder
     * @throws GenericEntityException the generic entity exception
     */
    protected StringBuilder makeConditionWhereString(StringBuilder whereString, String prefix, ModelEntity modelEntity,
                                                     EntityCondition whereEntityCondition, List<EntityCondition> viewWhereConditions,
                                                     List<EntityConditionParam> whereEntityConditionParams) throws GenericEntityException {
        ModelViewEntity modelViewEntity = null;
        if (modelEntity instanceof ModelViewEntity) {
            modelViewEntity = (ModelViewEntity) modelEntity;
        }

        List<EntityCondition> conditions = new LinkedList<>();
        if (UtilValidate.isNotEmpty(whereEntityCondition)) {
            conditions.add(whereEntityCondition);
        }

        if (modelViewEntity != null && !viewWhereConditions.isEmpty()) {
            EntityCondition viewWhereEntityCondition = EntityCondition.makeCondition(viewWhereConditions);
            if (!viewWhereEntityCondition.isEmpty()) {
                conditions.add(viewWhereEntityCondition);
            }
        }

        String viewClause = SqlJdbcUtil.makeViewWhereClause(modelEntity, datasource.getJoinStyle());

        if (!viewClause.isEmpty()) {
            conditions.add(EntityCondition.makeConditionWhere(viewClause));
        }

        if (!conditions.isEmpty()) {
            whereString.append(prefix);
            whereString.append(EntityCondition.makeCondition(conditions, EntityOperator.AND).makeWhereString(modelEntity,
                    whereEntityConditionParams, this.datasource));
        }

        return whereString;
    }

    /**
     * Make condition having string string builder.
     * @param modelEntity the model entity
     * @param havingEntityCondition the having entity condition
     * @param viewHavingConditions the view having conditions
     * @param havingEntityConditionParams the having entity condition params
     * @return the string builder
     * @throws GenericEntityException the generic entity exception
     */
    @Deprecated
    protected StringBuilder makeConditionHavingString(ModelEntity modelEntity, EntityCondition havingEntityCondition,
                                                      List<EntityCondition> viewHavingConditions,
                                                      List<EntityConditionParam> havingEntityConditionParams) throws GenericEntityException {
        return makeConditionHavingString(new StringBuilder(), "", modelEntity, havingEntityCondition, viewHavingConditions,
                havingEntityConditionParams);
    }

    /**
     * Make condition having string string builder.
     * @param havingString the having string
     * @param prefix the prefix
     * @param modelEntity the model entity
     * @param havingEntityCondition the having entity condition
     * @param viewHavingConditions the view having conditions
     * @param havingEntityConditionParams the having entity condition params
     * @return the string builder
     * @throws GenericEntityException the generic entity exception
     */
    protected StringBuilder makeConditionHavingString(StringBuilder havingString, String prefix, ModelEntity modelEntity,
                                                      EntityCondition havingEntityCondition, List<EntityCondition> viewHavingConditions,
                                                      List<EntityConditionParam> havingEntityConditionParams) throws GenericEntityException {
        ModelViewEntity modelViewEntity = null;
        if (modelEntity instanceof ModelViewEntity) {
            modelViewEntity = (ModelViewEntity) modelEntity;
        }

        String entityCondHavingString = "";
        if (havingEntityCondition != null) {
            entityCondHavingString = havingEntityCondition.makeWhereString(modelEntity, havingEntityConditionParams, this.datasource);
        }

        String viewEntityCondHavingString = null;
        if (modelViewEntity != null) {
            EntityCondition viewHavingEntityCondition = EntityCondition.makeCondition(viewHavingConditions);
            viewEntityCondHavingString = viewHavingEntityCondition.makeWhereString(modelEntity,
                    havingEntityConditionParams, this.datasource);
        }

        if (UtilValidate.isNotEmpty(entityCondHavingString) || UtilValidate.isNotEmpty(viewEntityCondHavingString)) {
            havingString.append(prefix);
        }

        if (UtilValidate.isNotEmpty(entityCondHavingString)) {
            boolean addParens = entityCondHavingString.charAt(0) != '(';
            if (addParens) havingString.append("(");
            havingString.append(entityCondHavingString);
            if (addParens) havingString.append(")");
        }
        if (UtilValidate.isNotEmpty(viewEntityCondHavingString)) {
            if (UtilValidate.isNotEmpty(entityCondHavingString)) havingString.append(" AND ");
            boolean addParens = viewEntityCondHavingString.charAt(0) != '(';
            if (addParens) havingString.append("(");
            havingString.append(viewEntityCondHavingString);
            if (addParens) havingString.append(")");
        }

        return havingString;
    }

    /**
     * Make offset string string builder.
     * @param offsetString the offset string
     * @param findOptions the find options
     * @return the string builder
     */
    protected StringBuilder makeOffsetString(StringBuilder offsetString, EntityFindOptions findOptions) {
        if (UtilValidate.isNotEmpty(datasource.getOffsetStyle())) {
            if ("limit".equals(datasource.getOffsetStyle())) {
                // use the LIMIT/OFFSET style
                if (findOptions.getLimit() > -1) {
                    offsetString.append(" LIMIT " + findOptions.getLimit());
                    if (findOptions.getOffset() > -1) {
                        offsetString.append(" OFFSET " + findOptions.getOffset());
                    }
                }
            } else {
                // use SQL2008 OFFSET/FETCH style by default
                if (findOptions.getOffset() > -1) {
                    offsetString.append(" OFFSET ").append(findOptions.getOffset()).append(" ROWS");
                    if (findOptions.getLimit() > -1) {
                        offsetString.append(" FETCH FIRST ").append(findOptions.getLimit()).append(" ROWS ONLY");
                    }
                }
            }
        }
        return offsetString;
    }

    /**
     * Select by multi relation list.
     * @param value the value
     * @param modelRelationOne the model relation one
     * @param modelEntityOne the model entity one
     * @param modelRelationTwo the model relation two
     * @param modelEntityTwo the model entity two
     * @param orderBy the order by
     * @return the list
     * @throws GenericEntityException the generic entity exception
     */
    public List<GenericValue> selectByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
                                                    ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List<String> orderBy)
            throws GenericEntityException {

        // get the tables names
        String atable = modelEntityOne.getTableName(datasource);
        String ttable = modelEntityTwo.getTableName(datasource);

        // get the column name string to select
        StringBuilder selsb = new StringBuilder();
        List<String> fldlist = new LinkedList<>();

        for (Iterator<ModelField> iterator = modelEntityTwo.getFieldsIterator(); iterator.hasNext();) {
            ModelField mf = iterator.next();

            fldlist.add(mf.getName());
            selsb.append(ttable).append(".").append(mf.getColName());
            if (iterator.hasNext()) {
                selsb.append(", ");
            } else {
                selsb.append(" ");
            }
        }

        // construct assoc->target relation string
        StringBuilder wheresb = new StringBuilder();
        for (ModelKeyMap mkm : modelRelationTwo.getKeyMaps()) {
            String lfname = mkm.getFieldName();
            String rfname = mkm.getRelFieldName();

            if (wheresb.length() > 0) {
                wheresb.append(" AND ");
            }
            wheresb.append(atable).append(".").append(modelEntityOne.getField(lfname).getColName()).append(" = ").append(ttable).append(".")
                    .append(modelEntityTwo.getField(rfname).getColName());
        }

        // construct the source entity qualifier
        // get the fields from relation description
        Map<ModelField, Object> bindMap = new HashMap<>();
        for (ModelKeyMap mkm : modelRelationOne.getKeyMaps()) {
            // get the equivalent column names in the relation
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
            wheresb.append(atable).append(".").append(lcolname).append(" = ? ");
        }

        // construct a join sql query
        StringBuilder sqlsb = new StringBuilder();

        sqlsb.append("SELECT ");
        sqlsb.append(selsb.toString());
        sqlsb.append(" FROM ");
        sqlsb.append(atable).append(", ").append(ttable);
        sqlsb.append(" WHERE ");
        sqlsb.append(wheresb.toString());
        sqlsb.append(SqlJdbcUtil.makeOrderByClause(modelEntityTwo, orderBy, true, datasource));

        // now execute the query
        List<GenericValue> retlist = new LinkedList<>();
        Delegator gd = value.getDelegator();

        try (SQLProcessor sqlP = new SQLProcessor(value.getDelegator(), helperInfo)) {
            sqlP.prepareStatement(sqlsb.toString());
            for (Map.Entry<ModelField, Object> entry : bindMap.entrySet()) {
                ModelField mf = entry.getKey();
                Object curvalue = entry.getValue();

                SqlJdbcUtil.setValue(sqlP, mf, modelEntityOne.getEntityName(), curvalue, modelFieldTypeReader);
            }
            sqlP.executeQuery();
            //int collsize = collist.size();

            while (sqlP.next()) {
                Map<String, Object> emptyMap = Collections.emptyMap();
                GenericValue gv = gd.makeValue(modelEntityTwo.getEntityName(), emptyMap);

                // loop thru all columns for in one row
                int idx = 1;
                for (String fldname : fldlist) {
                    ModelField mf = modelEntityTwo.getField(fldname);
                    SqlJdbcUtil.getValue(sqlP.getResultSet(), idx, mf, gv, modelFieldTypeReader);
                    idx++;
                }
                retlist.add(gv);
            }
        }

        return retlist;
    }

    /**
     * Select count by condition long.
     * @param delegator the delegator
     * @param modelEntity the model entity
     * @param whereEntityCondition the where entity condition
     * @param havingEntityCondition the having entity condition
     * @param findOptions the find options
     * @return the long
     * @throws GenericEntityException the generic entity exception
     */
    public long selectCountByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                       EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        return selectCountByCondition(delegator, modelEntity, whereEntityCondition, havingEntityCondition, null, findOptions);
    }

    /**
     * Select count by condition long.
     * @param delegator the delegator
     * @param modelEntity the model entity
     * @param whereEntityCondition the where entity condition
     * @param havingEntityCondition the having entity condition
     * @param selectFields the select fields
     * @param findOptions the find options
     * @return the long
     * @throws GenericEntityException the generic entity exception
     */
    public long selectCountByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                       EntityCondition havingEntityCondition, List<ModelField> selectFields, EntityFindOptions findOptions)
            throws GenericEntityException {
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
            if (Debug.verboseOn()) {
                Debug.logVerbose("Doing selectListIteratorByCondition with whereEntityCondition: " + whereEntityCondition, MODULE);
            }
        }

        boolean isGroupBy = false;
        ModelViewEntity modelViewEntity = null;
        if (modelEntity instanceof ModelViewEntity) {
            modelViewEntity = (ModelViewEntity) modelEntity;
            isGroupBy = modelViewEntity.getGroupBysSize() > 0;
        }

        // To get a count of the rows that will be returned when there is a GROUP BY, must do something like:
        //     SELECT COUNT(1) FROM (SELECT COUNT(1) FROM OFBIZ.POSTAL_ADDRESS PA GROUP BY PA.CITY) TEMP_NAME
        // instead of a simple:
        //     SELECT COUNT(1) FROM OFBIZ.POSTAL_ADDRESS PA GROUP BY PA.CITY

        StringBuilder sqlBuffer = new StringBuilder("SELECT ");

        if (isGroupBy) {
            sqlBuffer.append("COUNT(1) FROM (SELECT ");
        }

        if (findOptions.getDistinct()) {
            // old style, not sensitive to selecting limited columns: sqlBuffer.append("DISTINCT COUNT(*) ");
            /* DEJ20100304: the code below was causing problems so the line above may be used instead, but hopefully this is fixed now
             * may need varying SQL for different databases, and also in view-entities in some cases it seems to
             * cause the "COUNT(DISTINCT " to appear twice, causing an attempt to try to count a count (function="count-distinct", distinct=true in
              * find options)
             */
            if (selectFields != null && !selectFields.isEmpty()) {
                ModelField firstSelectField = selectFields.get(0);
                ModelViewEntity.ModelAlias firstModelAlias = modelViewEntity != null ? modelViewEntity.getAlias(firstSelectField.getName()) : null;
                if (firstModelAlias != null && UtilValidate.isNotEmpty(firstModelAlias.getFunction())) {
                    // if the field has a function already we don't want to count just it, would be meaningless
                    sqlBuffer.append("COUNT(DISTINCT *) ");
                } else {
                    sqlBuffer.append("COUNT(DISTINCT ");
                    // this only seems to support a single column, which is not desirable but seems a lot better than no columns or in certain
                    // cases all columns
                    sqlBuffer.append(firstSelectField.getColValue());
                    // sqlBuffer.append(modelEntity.colNameString(selectFields, ", ", "", datasource.aliasViews));
                    sqlBuffer.append(")");
                }
            } else {
                sqlBuffer.append("COUNT(DISTINCT *) ");
            }
        } else {
            // NOTE DEJ20080701 Changed from COUNT(*) to COUNT(1) to improve performance, and should get the same results at least when there is no
            // DISTINCT
            sqlBuffer.append("COUNT(1) ");
        }

        // populate the info from entity-condition in the view-entity, if it is one and there is one
        List<EntityCondition> viewWhereConditions = null;
        List<EntityCondition> viewHavingConditions = null;
        List<String> viewOrderByList = null;
        if (modelViewEntity != null) {
            viewWhereConditions = new LinkedList<>();
            viewHavingConditions = new LinkedList<>();
            viewOrderByList = new LinkedList<>();
            modelViewEntity.populateViewEntityConditionInformation(modelFieldTypeReader, viewWhereConditions, viewHavingConditions, viewOrderByList,
                    null);
        }

        // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
        sqlBuffer.append(SqlJdbcUtil.makeFromClause(modelEntity, modelFieldTypeReader, datasource));

        // WHERE clause
        List<EntityConditionParam> whereEntityConditionParams = new LinkedList<>();
        makeConditionWhereString(sqlBuffer, " WHERE ", modelEntity, whereEntityCondition, viewWhereConditions, whereEntityConditionParams);

        // GROUP BY clause for view-entity
        if (isGroupBy) {
            modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(selectFields), sqlBuffer, " GROUP BY ", ", ", "", false);
        }

        // HAVING clause
        List<EntityConditionParam> havingEntityConditionParams = new LinkedList<>();
        makeConditionHavingString(sqlBuffer, " HAVING ", modelEntity, havingEntityCondition, viewHavingConditions, havingEntityConditionParams);

        if (isGroupBy) {
            sqlBuffer.append(") TEMP_NAME");
        }

        String sql = sqlBuffer.toString();
        if (Debug.verboseOn()) {
            Debug.logVerbose("Count select sql: " + sql, MODULE);
        }

        try (SQLProcessor sqlP = new SQLProcessor(delegator, helperInfo)) {
            sqlP.prepareStatement(sql, findOptions.getSpecifyTypeAndConcur(), findOptions.getResultSetType(),
                    findOptions.getResultSetConcurrency(), findOptions.getFetchSize(), findOptions.getMaxRows());
            if (verboseOn) {
                // put this inside an if statement so that we don't have to generate the string when not used...
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Setting the whereEntityConditionParams: " + whereEntityConditionParams, MODULE);
                }
            }
            // set all of the values from the Where EntityCondition
            for (EntityConditionParam whereEntityConditionParam : whereEntityConditionParams) {
                SqlJdbcUtil.setValue(sqlP, whereEntityConditionParam.getModelField(), modelEntity.getEntityName(),
                        whereEntityConditionParam.getFieldValue(), modelFieldTypeReader);
            }
            if (verboseOn) {
                // put this inside an if statement so that we don't have to generate the string when not used...
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Setting the havingEntityConditionParams: " + havingEntityConditionParams, MODULE);
                }
            }
            // set all of the values from the Having EntityCondition
            for (EntityConditionParam havingEntityConditionParam : havingEntityConditionParams) {
                SqlJdbcUtil.setValue(sqlP, havingEntityConditionParam.getModelField(), modelEntity.getEntityName(),
                        havingEntityConditionParam.getFieldValue(), modelFieldTypeReader);
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
            }
        }
    }

    /**
     * Delete int.
     * @param entity the entity
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int delete(GenericEntity entity) throws GenericEntityException {
        try (SQLProcessor sqlP = new SQLProcessor(entity.getDelegator(), helperInfo)) {
            try {
                return delete(entity, sqlP);
            } catch (GenericDataSourceException e) {
                sqlP.rollback();
                throw new GenericDataSourceException("Exception while deleting the following entity: " + entity.toString(), e);
            }
        }
    }

    /**
     * Delete int.
     * @param entity the entity
     * @param sqlP the sql p
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int delete(GenericEntity entity, SQLProcessor sqlP) throws GenericEntityException {
        ModelEntity modelEntity = entity.getModelEntity();

        if (modelEntity instanceof ModelViewEntity) {
            throw new org.apache.ofbiz.entity.GenericNotImplementedException("Operation delete not supported yet for view entities");
        }

        StringBuilder sql = new StringBuilder().append("DELETE FROM ").append(modelEntity.getTableName(datasource)).append(" WHERE ");
        SqlJdbcUtil.makeWhereStringFromFields(sql, modelEntity.getPkFieldsUnmodifiable(), entity, "AND");

        int retVal;

        sqlP.prepareStatement(sql.toString());
        SqlJdbcUtil.setPkValues(sqlP, modelEntity, entity, modelFieldTypeReader);
        retVal = sqlP.executeUpdate();
        entity.removedFromDatasource();
        return retVal;
    }

    /**
     * Delete by condition int.
     * @param delegator the delegator
     * @param modelEntity the model entity
     * @param condition the condition
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int deleteByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition condition) throws GenericEntityException {
        try (SQLProcessor sqlP = new SQLProcessor(delegator, helperInfo)) {
            try {
                return deleteByCondition(modelEntity, condition, sqlP);
            } catch (GenericDataSourceException e) {
                sqlP.rollback();
                throw new GenericDataSourceException("Generic Entity Exception occurred in deleteByCondition", e);
            }
        }
    }

    /**
     * Delete by condition int.
     * @param modelEntity the model entity
     * @param condition the condition
     * @param sqlP the sql p
     * @return the int
     * @throws GenericEntityException the generic entity exception
     */
    public int deleteByCondition(ModelEntity modelEntity, EntityCondition condition, SQLProcessor sqlP) throws GenericEntityException {
        if (modelEntity == null || condition == null) {
            return 0;
        }
        if (modelEntity instanceof ModelViewEntity) {
            throw new org.apache.ofbiz.entity.GenericNotImplementedException("Operation deleteByCondition not supported yet for view entities");
        }

        StringBuilder sql = new StringBuilder("DELETE FROM ").append(modelEntity.getTableName(this.datasource));

        String whereCondition = condition.makeWhereString(modelEntity, null, this.datasource);
        if (UtilValidate.isNotEmpty(whereCondition)) {
            sql.append(" WHERE ").append(whereCondition);
        }
        sqlP.prepareStatement(sql.toString());

        return sqlP.executeUpdate();
    }

    /**
     * Check db.
     * @param modelEntities the model entities
     * @param messages the messages
     * @param addMissing the add missing
     */
    public void checkDb(Map<String, ModelEntity> modelEntities, List<String> messages, boolean addMissing) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperInfo);
        dbUtil.checkDb(modelEntities, messages, addMissing);
    }

    /**
     * Creates a list of ModelEntity objects based on meta data from the database
     */
    public List<ModelEntity> induceModelFromDb(Collection<String> messages) {
        DatabaseUtil dbUtil = new DatabaseUtil(this.helperInfo);
        return dbUtil.induceModelFromDb(messages);
    }
}
