/*
 * $Id: DelegatorInterface.java 5831 2005-09-26 06:52:24Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 *
 * @author     <a href="mailto:plightbo@cisco.com">Patrick Lightbody</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface DelegatorInterface {

    String getDelegatorName();

    ModelReader getModelReader();

    ModelGroupReader getModelGroupReader();

    ModelEntity getModelEntity(String entityName);

    String getEntityGroupName(String entityName);

    List getModelEntitiesByGroup(String groupName);

    Map getModelEntityMapByGroup(String groupName);

    String getGroupHelperName(String groupName);

    String getEntityHelperName(String entityName);

    String getEntityHelperName(ModelEntity entity);

    GenericHelper getEntityHelper(String entityName) throws GenericEntityException;

    GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException;

    ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException;

    Collection getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException;

    GenericValue makeValue(String entityName, Map fields);

    GenericValue makeValidValue(String entityName, Map fields);

    GenericPK makePK(String entityName, Map fields);

    GenericValue create(String entityName, Map fields) throws GenericEntityException;

    GenericValue create(GenericValue value) throws GenericEntityException;

    GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    GenericValue create(GenericPK primaryKey) throws GenericEntityException;

    GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException;

    GenericValue createOrStore(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    GenericValue createOrStore(GenericValue value) throws GenericEntityException;

    GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException;

    GenericValue findByPrimaryKeyCache(GenericPK primaryKey) throws GenericEntityException;

    GenericValue findByPrimaryKey(String entityName, Map fields) throws GenericEntityException;

    GenericValue findByPrimaryKeyCache(String entityName, Map fields) throws GenericEntityException;

    GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set keys) throws GenericEntityException;

    List findAllByPrimaryKeys(Collection primaryKeys) throws GenericEntityException;

    List findAllByPrimaryKeysCache(Collection primaryKeys) throws GenericEntityException;

    int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException;

    int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException;

    int removeValue(GenericValue value) throws GenericEntityException;

    int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    List findAll(String entityName) throws GenericEntityException;

    List findAll(String entityName, List orderBy) throws GenericEntityException;

    List findAllCache(String entityName) throws GenericEntityException;

    List findAllCache(String entityName, List orderBy) throws GenericEntityException;

    List findByAnd(String entityName, Map fields) throws GenericEntityException;

    List findByOr(String entityName, Map fields) throws GenericEntityException;

    List findByAnd(String entityName, Map fields, List orderBy) throws GenericEntityException;

    //List findByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException;

    List findByOr(String entityName, Map fields, List orderBy) throws GenericEntityException;

    List findByAndCache(String entityName, Map fields) throws GenericEntityException;

    List findByAndCache(String entityName, Map fields, List orderBy) throws GenericEntityException;

    List findByAnd(String entityName, List expressions) throws GenericEntityException;

    List findByOr(String entityName, List expressions) throws GenericEntityException;

    List findByAnd(String entityName, List expressions, List orderBy) throws GenericEntityException;

    List findByOr(String entityName, List expressions, List orderBy) throws GenericEntityException;

    List findByLike(String entityName, Map fields) throws GenericEntityException;

    List findByLike(String entityName, Map fields, List orderBy) throws GenericEntityException;

    List findByCondition(String entityName, EntityCondition entityCondition, Collection fieldsToSelect, List orderBy) throws GenericEntityException;

    EntityListIterator findListIteratorByCondition(String entityName, EntityCondition entityCondition,
        Collection fieldsToSelect, List orderBy) throws GenericEntityException;

    EntityListIterator findListIteratorByCondition(String entityName, EntityCondition whereEntityCondition,
        EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
        throws GenericEntityException;

    int removeByAnd(String entityName, Map fields) throws GenericEntityException;

    int removeByAnd(String entityName, Map fields, boolean doCacheClear) throws GenericEntityException;

    int removeByCondition(String entityName, EntityCondition condition) throws GenericEntityException;

    int removeByCondition(String entityName, EntityCondition condition, boolean doCacheClear) throws GenericEntityException;

    List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List orderBy) throws GenericEntityException;

    List getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo) throws GenericEntityException;

    List getRelated(String relationName, GenericValue value) throws GenericEntityException;

    List getRelatedByAnd(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException;

    List getRelatedOrderBy(String relationName, List orderBy, GenericValue value) throws GenericEntityException;

    List getRelated(String relationName, Map byAndFields, List orderBy, GenericValue value) throws GenericEntityException;

    GenericPK getRelatedDummyPK(String relationName, Map byAndFields, GenericValue value) throws GenericEntityException;

    List getRelatedCache(String relationName, GenericValue value) throws GenericEntityException;

    GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException;

    GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException;

    int removeRelated(String relationName, GenericValue value) throws GenericEntityException;

    int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException;

    void refresh(GenericValue value) throws GenericEntityException;

    void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    int store(GenericValue value) throws GenericEntityException;

    int store(GenericValue value, boolean doCacheClear) throws GenericEntityException;

    int storeAll(List values) throws GenericEntityException;

    int storeAll(List values, boolean doCacheClear) throws GenericEntityException;

    int storeByCondition(String entityName, Map fieldsToSet, EntityCondition condition) throws GenericEntityException;

    int storeByCondition(String entityName, Map fieldsToSet, EntityCondition condition, boolean doCacheClear) throws GenericEntityException;

    int removeAll(List dummyPKs) throws GenericEntityException;

    int removeAll(List dummyPKs, boolean doCacheClear) throws GenericEntityException;

    void clearAllCaches();

    void clearAllCaches(boolean distribute);

    void clearCacheLine(String entityName, Map fields);

    void clearCacheLineFlexible(GenericEntity dummyPK);

    void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute);

    void clearCacheLine(GenericPK primaryKey);

    void clearCacheLine(GenericPK primaryKey, boolean distribute);

    void clearCacheLine(GenericValue value);

    void clearCacheLine(GenericValue value, boolean distribute);

    void clearCacheLineByCondition(String entityName, EntityCondition condition);

    void clearAllCacheLinesByDummyPK(Collection dummyPKs);

    void clearAllCacheLinesByValue(Collection values);

    GenericValue getFromPrimaryKeyCache(GenericPK primaryKey);

    String getNextSeqId(String seqName);
    String getNextSeqId(String seqName, long staggerMax);
    Long getNextSeqIdLong(String seqName);
    Long getNextSeqIdLong(String seqName, long staggerMax);

    void setSequencer(SequenceUtil sequencer);

    void refreshSequencer();

    Cache getCache();
}
