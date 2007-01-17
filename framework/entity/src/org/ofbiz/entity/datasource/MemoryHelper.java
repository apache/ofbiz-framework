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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericNotImplementedException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
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
    private static Map cache = new HashMap();

    public static void clearCache() {
        cache = new HashMap();
    }

    private String helperName;

    private boolean addToCache(GenericValue value) {
        if (value == null) {
            return false;
        }

        if (!veryifyValue(value)) {
            return false;
        }

        value = (GenericValue) value.clone();
        HashMap entityCache = (HashMap) cache.get(value.getEntityName());
        if (entityCache == null) {
            entityCache = new HashMap();
            cache.put(value.getEntityName(), entityCache);
        }

        entityCache.put(value.getPrimaryKey(), value);
        return true;
    }

    private GenericValue findFromCache(GenericPK pk) {
        if (pk == null) {
            return null;
        }

        HashMap entityCache = (HashMap) cache.get(pk.getEntityName());
        if (entityCache == null) {
            return null;
        }

        GenericValue value = (GenericValue) entityCache.get(pk);
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

        HashMap entityCache = (HashMap) cache.get(pk.getEntityName());
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

        HashMap entityCache = (HashMap) cache.get(entityName);
        if (entityCache == null) {
            return 0;
        }

        Iterator it = entityCache.values().iterator();
        int count = 0;
        while (it.hasNext()) {
            GenericValue value = (GenericValue) it.next();
            if (condition.entityMatches(value)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    private boolean isAndMatch(Map values, Map fields) {
        for (Iterator iterator = fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
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

    private boolean isOrMatch(Map values, Map fields) {
        for (Iterator iterator = fields.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
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
        for (Iterator iterator = me.getPksIterator(); iterator.hasNext();) {
            ModelField field = (ModelField) iterator.next();
            if (!value.containsKey(field.getName())) {
                return false;
            }
        }

        // make sure the value doesn't have any extra (unknown) fields
        for (Iterator iterator = value.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (me.getField((String) entry.getKey()) == null) {
                return false;
            }
        }

        // make sure all fields that are in the value are of the right type
        for (Iterator iterator = me.getFieldsIterator(); iterator.hasNext();) {
            ModelField field = (ModelField) iterator.next();
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

                    case 9:
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
    }

    public String getHelperName() {
        return helperName;
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

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set keys) throws GenericEntityException {
        GenericValue value = findFromCache(primaryKey);
        value.setFields(value.getFields(keys));
        return value;
    }

    public List findAllByPrimaryKeys(List primaryKeys) throws GenericEntityException {
        ArrayList result = new ArrayList(primaryKeys.size());
        for (Iterator iterator = primaryKeys.iterator(); iterator.hasNext();) {
            GenericPK pk = (GenericPK) iterator.next();
            result.add(this.findByPrimaryKey(pk));
        }

        return result;
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return removeFromCache(primaryKey);
    }

    public List findByAnd(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        HashMap entityCache = (HashMap) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.EMPTY_LIST;
        }

        ArrayList result = new ArrayList();
        for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            GenericValue value = (GenericValue) mapEntry.getValue();

            if (isAndMatch(value.getAllFields(), fields)) {
                result.add(value);
            }
        }

        return result;
    }

    public List findByAnd(ModelEntity modelEntity, List expressions, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByLike(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByOr(ModelEntity modelEntity, Map fields, List orderBy) throws GenericEntityException {
        HashMap entityCache = (HashMap) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return Collections.EMPTY_LIST;
        }

        ArrayList result = new ArrayList();
        for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            GenericValue value = (GenericValue) mapEntry.getValue();

            if (isOrMatch(value.getAllFields(), fields)) {
                result.add(value);
            }
        }

        return result;

    }

    public List findByOr(ModelEntity modelEntity, List expressions, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByCondition(ModelEntity modelEntity, EntityCondition entityCondition,
                                Collection fieldsToSelect, List orderBy) throws GenericEntityException {
        return null;
    }

    public List findByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
                                    ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List orderBy) throws GenericEntityException {
        return null;
    }

    public EntityListIterator findListIteratorByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition,
                                                          EntityCondition havingEntityCondition, Collection fieldsToSelect, List orderBy, EntityFindOptions findOptions)
            throws GenericEntityException {
        return null;
    }

    public long findCountByCondition(ModelEntity modelEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        return 0;
    }

    public int removeByAnd(ModelEntity modelEntity, Map fields) throws GenericEntityException {
        HashMap entityCache = (HashMap) cache.get(modelEntity.getEntityName());
        if (entityCache == null) {
            return 0;
        }

        ArrayList removeList = new ArrayList();
        for (Iterator iterator = entityCache.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            GenericValue value = (GenericValue) mapEntry.getValue();
            if (isAndMatch(value.getAllFields(), fields)) {
                removeList.add(mapEntry.getKey());
            }
        }

        return removeAll(removeList);
    }

    public int removeByCondition(ModelEntity modelEntity, EntityCondition condition) throws GenericEntityException {
        return removeFromCache(modelEntity.getEntityName(), condition);
    }

    public int storeByCondition(ModelEntity modelEntity, Map fieldsToSet, EntityCondition condition) throws GenericEntityException {
        return 0;
    }

    public int store(GenericValue value) throws GenericEntityException {
        if (addToCache(value)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int storeAll(List values) throws GenericEntityException {
        int count = 0;
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            GenericValue gv = (GenericValue) iterator.next();
            if (addToCache(gv)) {
                count++;
            }
        }

        return count;
    }

    public int removeAll(List dummyPKs) throws GenericEntityException {
        int count = 0;
        for (Iterator iterator = dummyPKs.iterator(); iterator.hasNext();) {
            GenericPK pk = (GenericPK) iterator.next();
            count = count + removeFromCache(pk);
        }

        return count;
    }

    public void checkDataSource(Map modelEntities, List messages, boolean addMissing) throws GenericEntityException {
        messages.add("checkDataSource not implemented for MemoryHelper");
    }
}
