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
package org.ofbiz.entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.entity.cache.Cache;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.datasource.GenericHelper;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.model.ModelGroupReader;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.SequenceUtil;

/**
 * Delegator Interface
 */
public interface DelegatorInterface {

    String getDelegatorName();

    ModelReader getModelReader();

    ModelGroupReader getModelGroupReader();

    ModelEntity getModelEntity(String entityName);

    String getEntityGroupName(String entityName);

    Map<String, ModelEntity> getModelEntityMapByGroup(String groupName) throws GenericEntityException;

    String getGroupHelperName(String groupName);

    String getEntityHelperName(String entityName);

    String getEntityHelperName(ModelEntity entity);

    GenericHelper getEntityHelper(String entityName) throws GenericEntityException;

    GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException;

    ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException;

    Collection<String> getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException;

    GenericValue makeValue(String entityName);

    GenericValue makeValue(String entityName, Object... fields);

    GenericValue makeValue(String entityName, Map<String, ? extends Object> fields);

    GenericValue makeValueSingle(String entityName, Object singlePkValue);

    GenericValue makeValidValue(String entityName, Object... fields);

    GenericValue makeValidValue(String entityName, Map<String, ? extends Object> fields);

    GenericPK makePK(String entityName);

    GenericPK makePK(String entityName, Object... fields);

    GenericPK makePK(String entityName, Map<String, ? extends Object> fields);

    GenericPK makePKSingle(String entityName, Object singlePkValue);

    GenericValue create(String entityName, Object... fields) throws GenericEntityException;

    GenericValue create(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    GenericValue createSingle(String entityName, Object singlePkValue) throws GenericEntityException;

    GenericValue create(GenericValue value) throws GenericEntityException;

    GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    GenericValue create(GenericPK primaryKey) throws GenericEntityException;

    GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException;

    GenericValue createOrStore(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    GenericValue createOrStore(GenericValue value) throws GenericEntityException;

    GenericValue findOne(String entityName, Map<String, ? extends Object> fields, boolean useCache) throws GenericEntityException;

    GenericValue findByPrimaryKey(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    GenericValue findByPrimaryKeyCache(String entityName, Object... fields) throws GenericEntityException;

    GenericValue findByPrimaryKeyCache(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException;

    int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException;

    int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException;

    int removeValue(GenericValue value) throws GenericEntityException;

    int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    List<GenericValue> findByAnd(String entityName, Object... fields) throws GenericEntityException;

    List<GenericValue> findByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    List<GenericValue> findByAnd(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException;

    //List<GenericValue> findByAnd(ModelEntity modelEntity, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException;

    List<GenericValue> findByOr(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException;

    List<GenericValue> findByAndCache(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    List<GenericValue> findByAndCache(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException;

    EntityListIterator find(String entityName, EntityCondition whereEntityCondition,
            EntityCondition havingEntityCondition, Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
            throws GenericEntityException;

    List<GenericValue> findList(String entityName, EntityCondition entityCondition,
            Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions, boolean useCache)
            throws GenericEntityException;

    int removeByAnd(String entityName, Object... fields) throws GenericEntityException;

    int removeByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException;

    int removeByAnd(String entityName, boolean doCacheClear, Object... fields) throws GenericEntityException;

    int removeByAnd(String entityName, Map<String, ? extends Object> fields, boolean doCacheClear) throws GenericEntityException;

    int removeByCondition(String entityName, EntityCondition condition) throws GenericEntityException;

    int removeByCondition(String entityName, EntityCondition condition, boolean doCacheClear) throws GenericEntityException;

    List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException;

    List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy, GenericValue value) throws GenericEntityException;

    GenericPK getRelatedDummyPK(String relationName, Map<String, ? extends Object> byAndFields, GenericValue value) throws GenericEntityException;

    List<GenericValue> getRelatedCache(String relationName, GenericValue value) throws GenericEntityException;

    GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException;

    GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException;

    int removeRelated(String relationName, GenericValue value) throws GenericEntityException;

    int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException;

    void refresh(GenericValue value) throws GenericEntityException;

    void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    int store(GenericValue value) throws GenericEntityException;

    int store(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    int storeAll(List<GenericValue> values) throws GenericEntityException;

    int storeAll(List<GenericValue> values, boolean doCacheClear) throws GenericEntityException;

    int storeByCondition(String entityName, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException;

    int storeByCondition(String entityName, Map<String, ? extends Object> fieldsToSet, EntityCondition condition, boolean doCacheClear) throws GenericEntityException;

    int removeAll(String entityName) throws GenericEntityException;

    int removeAll(List<? extends GenericEntity> dummyPKs) throws GenericEntityException;

    int removeAll(List<? extends GenericEntity> dummyPKs, boolean doCacheClear) throws GenericEntityException;

    void clearAllCaches();

    void clearAllCaches(boolean distribute);

    void clearCacheLine(String entityName);

    void clearCacheLine(String entityName, Object... fields);

    void clearCacheLine(String entityName, Map<String, ? extends Object> fields);

    void clearCacheLineFlexible(GenericEntity dummyPK);

    void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute);

    void clearCacheLine(GenericPK primaryKey);

    void clearCacheLine(GenericPK primaryKey, boolean distribute);

    void clearCacheLine(GenericValue value);

    void clearCacheLine(GenericValue value, boolean distribute);

    void clearCacheLineByCondition(String entityName, EntityCondition condition);

    void clearAllCacheLinesByDummyPK(Collection<GenericPK> dummyPKs);

    void clearAllCacheLinesByValue(Collection<GenericValue> values);

    GenericValue getFromPrimaryKeyCache(GenericPK primaryKey);

    String getNextSeqId(String seqName);
    String getNextSeqId(String seqName, long staggerMax);
    Long getNextSeqIdLong(String seqName);
    Long getNextSeqIdLong(String seqName, long staggerMax);

    void setSequencer(SequenceUtil sequencer);

    void refreshSequencer();

    Cache getCache();
}
