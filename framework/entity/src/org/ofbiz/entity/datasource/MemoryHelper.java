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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;

import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericNotImplementedException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldTypeReader;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * Partial GenericHelper implementation that is entirely memory-based,
 * to be used for simple unit testing (can't do anything beyond searches
 * for primary keys, findByOr and findByAnd).
 *
 */
public class MemoryHelper implements GenericHelper {

    public static final String module = MemoryHelper.class.getName();
    private static Map<String, HashMap<GenericPK, GenericValue>> cache = new HashMap<String, HashMap<GenericPK, GenericValue>>();
    private static final ThreadGroup MEMORY_HELPER_THREAD_GROUP = new ThreadGroup("MemoryHelper");

    public static void clearCache() {
        cache = new HashMap<String, HashMap<GenericPK, GenericValue>>();
    }

    private String helperName;
    protected ExecutorService executor;

    private boolean addToCache(GenericValue value) {
        if (value == null) {
            return false;
        }

        if (!veryifyValue(value)) {
            return false;
        }

        value = (GenericValue) value.clone();
        HashMap<GenericPK, GenericValue> entityCache = cache.get(value.getEntityName());
        if (entityCache == null) {
            entityCache = new HashMap<GenericPK, GenericValue>();
            cache.put(value.getEntityName(), entityCache);
        }

        entityCache.put(value.getPrimaryKey(), value);
        return true;
    }

    private GenericValue findFromCache(GenericPK pk) {
        if (pk == null) {
            return null;
        }

        HashMap<GenericPK, GenericValue> entityCache = cache.get(pk.getEntityName());
        if (entityCache == null) {
            return null;
        }

        GenericValue value = entityCache.get(pk);
        if (value == null) {
            return null;
        } else {
            return (GenericValue) value.clone();
        }
    }

    private int removeFromCache(GenericPK pk) {
        if (pk == null) {
            return 0;
        }

        HashMap<GenericPK, GenericValue> entityCache = cache.get(pk.getEntityName());
        if (entityCache == null) {
            return 0;
        }

        Object o = entityCache.remove(pk);
        if (o == null) {
            return 0;
        } else {
            return 1;
        }
    }

    private int removeFromCache(String entityName, EntityCondition condition) {
        if (entityName == null || condition == null) {
            return 0;
        }

        HashMap<GenericPK, GenericValue> entityCache = cache.get(entityName);
        if (entityCache == null) {
            return 0;
        }

        Iterator<GenericValue> it = entityCache.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            GenericValue value = it.next();
            if (condition.entityMatches(value)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    private boolean isAndMatch(Map<String, Object> values, Map<String, Object> fields) {
        for (Map.Entry<String, Object> mapEntry: fields.entrySet()) {
            if (mapEntry.getValue() == null) {
                if (values.get(mapEntry.getKey()) != null) {
                    return false;
                }
            } else {
                try {
                    if (!mapEntry.getValue().equals(values.get(mapEntry.getKey()))) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isOrMatch(Map<String, Object> values, Map<String, Object> fields) {
        for (Map.Entry<String, Object> mapEntry: fields.entrySet()) {
            if (mapEntry.getValue() == null) {
                if (values.get(mapEntry.getKey()) == null) {
                    return true;
                }
            } else {
                try {
                    if (mapEntry.getValue().equals(values.get(mapEntry.getKey()))) {
                        return true;
                    }
                } catch (Exception e) {
                    Debug.logError(e, module);
                }
            }
        }

        return false;
    }

    private boolean veryifyValue(GenericValue value) {
        ModelEntity me = value.getModelEntity();

        // make sure the PKs exist
        for (Iterator<ModelField> iterator = me.getPksIterator(); iterator.hasNext();) {
            ModelField field = iterator.next();
            if (!value.containsKey(field.getName())) {
                return false;
            }
        }

        // make sure the value doesn't have any extra (unknown) fields
        for (Map.Entry<String, Object> entry: value.entrySet()) {
            if (me.getField(entry.getKey()) == null) {
                return false;
            }
        }

        // make sure all fields that are in the value are of the right type
        for (Iterator<ModelField> iterator = me.getFieldsIterator(); iterator.hasNext();) {
            ModelField field = iterator.next();
            Object o = value.get(field.getName());
            int typeValue = 0;
            try {
                typeValue = SqlJdbcUtil.getType(modelFieldTypeReader.getModelFieldType(field.getType()).getJavaType());
            } catch (GenericNotImplementedException e) {
                return false;
            }

            if (o != null) {
                switch (typeValue) {
                    case 1:
                        if (!(o instanceof String)) {
                            return false;
                        }
                        break;
                    case 2:
                        if (!(o instanceof java.sql.Timestamp)) {
                            return false;
                        }
                        break;

                    case 3:
                        if (!(o instanceof java.sql.Time)) {
                            return false;
                        }
                        break;

                    case 4:
                        if (!(o instanceof java.sql.Date)) {
                            return false;
                        }
                        break;

                    case 5:
                        if (!(o instanceof Integer)) {
                            return false;
                        }
                        break;

                    case 6:
                        if (!(o instanceof Long)) {
                            return false;
                        }
                        break;

                    case 7:
                        if (!(o instanceof Float)) {
                            return false;
                        }
                        break;

                    case 8:
                        if (!(o instanceof Double)) {
                            return false;
                        }
                        break;

                    case 10:
                        if (!(o instanceof Boolean)) {
                            return false;
                        }
                        break;
                }
            }
        }

        return true;
    }

    private ModelFieldTypeReader modelFieldTypeReader;

    public MemoryHelper(String helperName) {
        this.helperName = helperName;
        modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        Datasource datasourceInfo = EntityConfigUtil.getDatasource(helperName);
        this.executor = ExecutionPool.getExecutor(MEMORY_HELPER_THREAD_GROUP, "OFBiz-entity-datasource(" + helperName + ")", datasourceInfo.getMaxWorkerPoolSize(), false);
    }

    public String getHelperName() {
        return helperName;
    }

    public <T> Future<T> submitWork(Callable<T> callable) throws GenericEntityException {
        return this.executor.submit(callable);
    }

    public GenericValue create(GenericValue value) throws GenericEntityException {
        if (addToCache(value)) {
            return value;
        } else {
            return null;
        }
    }

    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        return create(GenericValue.create(primaryKey));
    }

    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return findFromCache(primaryKey);
    }

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException {
        GenericValue value = findFromCache(primaryKey);
        value.setFields(value.getFields(keys));
        return value;
    }

    public List<GenericValue> findAllByPrimaryKeys(List<GenericPK> primaryKeys) throws GenericEntityException {
        ArrayList<GenericValue> result = new ArrayList<GenericValue>(primaryKeys.size());
        for (GenericPK pk: primaryKeys) {
            result.add(this.findByPrimaryKey(pk));
        }

        return result;
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return removeFromCache(primaryKey);
    }

    public List<GenericValue> findByAnd(ModelEntity modelEntity, Map<String, Object> fields, List<String> orderBy) throws GenericEntityException {
        HashMap<GenericPK, GenericValue> entityCache = cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.emptyList();
        }

        ArrayList<GenericValue> result = new ArrayList<GenericValue>();
        for (GenericValue value: entityCache.values()) {
            if (isAndMatch(value.getAllFields(), fields)) {
                result.add(value);
            }
        }

        return result;
    }

    public List<GenericValue> findByAnd(ModelEntity modelEntity, List<EntityCondition> expressions, List<String> orderBy) throws GenericEntityException {
        return null;
    }

    public List<GenericValue> findByLike(ModelEntity modelEntity, Map<String, Object> fields, List<String> orderBy) throws GenericEntityException {
        return null;
    }

    public List<GenericValue> findByOr(ModelEntity modelEntity, Map<String, Object> fields, List<String> orderBy) throws GenericEntityException {
        HashMap<GenericPK, GenericValue> entityCache = cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.emptyList();
        }

        ArrayList<GenericValue> result = new ArrayList<GenericValue>();
        for (GenericValue value: entityCache.values()) {
            if (isOrMatch(value.getAllFields(), fields)) {
                result.add(value);
            }
        }

        return result;

    }

    public List<GenericValue> findByOr(ModelEntity modelEntity, List<EntityCondition> expressions, List<String> orderBy) throws GenericEntityException {
        return null;
    }

    public List<GenericValue> findByCondition(ModelEntity modelEntity, EntityCondition entityCondition,
                                Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        return null;
    }

    public List<GenericValue> findByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
                                    ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List<String> orderBy) throws GenericEntityException {
        return null;
    }

    public EntityListIterator findListIteratorByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                                          EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        return null;
    }

    public long findCountByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        return 0;
    }

    public int removeByAnd(ModelEntity modelEntity, Map<String, Object> fields) throws GenericEntityException {
        HashMap<GenericPK, GenericValue> entityCache = cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return 0;
        }

        ArrayList<GenericPK> removeList = new ArrayList<GenericPK>();
        for (Map.Entry<GenericPK, GenericValue> mapEntry: entityCache.entrySet()) {
            GenericValue value = mapEntry.getValue();
            if (isAndMatch(value.getAllFields(), fields)) {
                removeList.add(mapEntry.getKey());
            }
        }

        return removeAll(removeList);
    }

    public int removeByCondition(ModelEntity modelEntity, EntityCondition condition) throws GenericEntityException {
        return removeFromCache(modelEntity.getEntityName(), condition);
    }

    public int storeByCondition(ModelEntity modelEntity, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException {
        return 0;
    }

    public int store(GenericValue value) throws GenericEntityException {
        if (addToCache(value)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int storeAll(List<GenericValue> values) throws GenericEntityException {
        int count = 0;
        for (GenericValue gv: values) {
            if (addToCache(gv)) {
                count++;
            }
        }

        return count;
    }

    public int removeAll(List<GenericPK> dummyPKs) throws GenericEntityException {
        int count = 0;
        for (GenericPK pk: dummyPKs) {
            count = count + removeFromCache(pk);
        }

        return count;
    }

    public void checkDataSource(Map<String, ModelEntity> modelEntities, List<String> messages, boolean addMissing) throws GenericEntityException {
        messages.add("checkDataSource not implemented for MemoryHelper");
    }
}
