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

package org.apache.ofbiz.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.model.ModelEntity;


/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 *
 */
@SuppressWarnings("serial")
public class GenericValue extends GenericEntity {

    public static final GenericValue NULL_VALUE = new NullGenericValue();

    /** Creates new GenericValue */
    public static GenericValue create(ModelEntity modelEntity) {
        GenericValue newValue = new GenericValue();
        newValue.init(modelEntity);
        return newValue;
    }

    /** Creates new GenericValue from existing Map */
    public static GenericValue create(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fields) {
        GenericValue newValue = new GenericValue();
        newValue.init(delegator, modelEntity, fields);
        return newValue;
    }

    /** Creates new GenericValue from existing Map */
    public static GenericValue create(Delegator delegator, ModelEntity modelEntity, Object singlePkValue) {
        GenericValue newValue = new GenericValue();
        newValue.init(delegator, modelEntity, singlePkValue);
        return newValue;
    }

    /** Creates new GenericValue from existing GenericValue */
    public static GenericValue create(GenericValue value) {
        GenericValue newValue = new GenericValue();
        newValue.init(value);
        return newValue;
    }

    /** Creates new GenericValue from existing GenericValue */
    public static GenericValue create(GenericPK primaryKey) {
        GenericValue newValue = new GenericValue();
        newValue.init(primaryKey);
        return newValue;
    }

    public GenericValue create() throws GenericEntityException {
        return this.getDelegator().create(this);
    }

    public void store() throws GenericEntityException {
        this.getDelegator().store(this);
    }

    public void remove() throws GenericEntityException {
        this.getDelegator().removeValue(this);
    }

    public void refresh() throws GenericEntityException {
        this.getDelegator().refresh(this);
    }

    public void refreshFromCache() throws GenericEntityException {
        this.getDelegator().refreshFromCache(this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     *@deprecated use {@link #getRelated(String, Map, List, boolean)}
     */
    @Deprecated
    public List<GenericValue> getRelated(String relationName) throws GenericEntityException {
        Debug.logWarning("deprecated method, please replace as suggested in API Java Doc, and link to OFBIZ-6651", getStackTraceAsString());
        return this.getDelegator().getRelated(relationName, null, null, this, false);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     *@deprecated use {@link #getRelated(String, Map, List, boolean)}
     */
    @Deprecated
    public List<GenericValue> getRelated(String relationName, List<String> orderBy) throws GenericEntityException {
        Debug.logWarning("deprecated method, please replace as suggested in API Java Doc, and link to OFBIZ-6651", getStackTraceAsString());
        return this.getDelegator().getRelated(relationName, null, orderBy, this, false);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     *@deprecated use {@link #getRelated(String, Map, List, boolean)}
     */
    @Deprecated
    public List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy) throws GenericEntityException {
        Debug.logWarning("deprecated method, please replace as suggested in API Java Doc, and link to OFBIZ-6651", getStackTraceAsString());
        return this.getDelegator().getRelated(relationName, byAndFields, orderBy, this, false);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @param useCache Whether to cache the results
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy, boolean useCache) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, byAndFields, orderBy, this, useCache);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedMulti(String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException {
        return this.getDelegator().getMultiRelation(this, relationNameOne, relationNameTwo, orderBy);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedMulti(String relationNameOne, String relationNameTwo) throws GenericEntityException {
        return this.getDelegator().getMultiRelation(this, relationNameOne, relationNameTwo, null);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     *@deprecated use {@link #getRelatedOne(String, boolean)}
     */
    @Deprecated
    public GenericValue getRelatedOne(String relationName) throws GenericEntityException {
        Debug.logWarning("deprecated method, please replace as suggested in API Java Doc, and link to OFBIZ-6651", getStackTraceAsString());
        return this.getDelegator().getRelatedOne(relationName, this, false);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param useCache Whether to cache the results
     *@return The single related GenericValue instance
     */
    public GenericValue getRelatedOne(String relationName, boolean useCache) throws GenericEntityException {
        return this.getDelegator().getRelatedOne(relationName, this, useCache);
    }

    /** Remove the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     */
    public void removeRelated(String relationName) throws GenericEntityException {
        this.getDelegator().removeRelated(relationName, this);
    }

    /** Get a dummy primary key for the named Related Entity for the GenericValue
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelatedDummyPK(relationName, null, this);
    }

    /** Get a dummy primary key for the named Related Entity for the GenericValue
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(String relationName, Map<String, ? extends Object> byAndFields) throws GenericEntityException {
        return this.getDelegator().getRelatedDummyPK(relationName, byAndFields, this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericValue) {
            return super.equals(obj);
        }
        return false;
    }

    /** Clones this GenericValue, this is a shallow clone and uses the default shallow HashMap clone
     *  @return Object that is a clone of this GenericValue
     */
    @Override
    public Object clone() {
        return GenericValue.create(this);
    }

    protected static class NullGenericValue extends GenericValue implements NULL {
        @Override
        public String getEntityName() {
            return "[null-entity-value]";
        }
        @Override
        public String toString() {
            return "[null-entity-value]";
        }
    }

    public static String getStackTraceAsString() {
        return Arrays.toString(Thread.currentThread().getStackTrace());
    }
}
