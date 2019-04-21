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
package org.apache.ofbiz.entity.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.DynamicViewEntity;

/**
 * Used to setup various options for and subsequently execute entity queries.
 *
 * All methods to set options modify the EntityQuery instance then return this modified object to allow method call chaining. It is
 * important to note that this object is not immutable and is modified internally, and returning EntityQuery is just a
 * self reference for convenience.
 *
 * After a query the object can be further modified and then used to perform another query if desired.
 */
public class EntityQuery {

    public static final String module = EntityQuery.class.getName();

    private Delegator delegator;
    private String entityName = null;
    private DynamicViewEntity dynamicViewEntity = null;
    private boolean useCache = false;
    private EntityCondition whereEntityCondition = null;
    private Set<String> fieldsToSelect = null;
    private List<String> orderBy = null;
    private Integer resultSetType = EntityFindOptions.TYPE_FORWARD_ONLY;
    private Integer fetchSize = null;
    private Integer maxRows = null;
    private Boolean distinct = null;
    private EntityCondition havingEntityCondition = null;
    private boolean filterByDate = false;
    private Timestamp filterByDateMoment;
    private List<String> filterByFieldNames = null;
    private boolean searchPkOnly = false;
    private Map<String, Object> fieldMap = null;



    /** Construct an EntityQuery object for use against the specified Delegator
     * @param delegator The delegator instance to use for the query
     */
    public static EntityQuery use(Delegator delegator) {
        return new EntityQuery(delegator);
    }

    /** Construct an EntityQuery object for use against the specified Delegator
     * @param delegator The delegator instance to use for the query
     */
    public EntityQuery(Delegator delegator) {
        this.delegator = delegator;
    }

    /** Set the fields to be returned when the query is executed.
     * 
     * Note that the select methods are not additive, if a subsequent 
     * call is made to select then the existing fields for selection 
     * will be replaced.
     * @param fieldsToSelect - A Set of Strings containing the field names to be selected
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery select(Set<String> fieldsToSelect) {
        this.fieldsToSelect = fieldsToSelect;
        return this;
    }

    /** Set the fields to be returned when the query is executed.
     * 
     * Note that the select methods are not additive, if a subsequent 
     * call is made to select then the existing fields for selection 
     * will be replaced.
     * @param fields - Strings containing the field names to be selected
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery select(String...fields) {
        this.fieldsToSelect = UtilMisc.toSetArray(fields);
        return this;
    }

    /** Set the entity to query against
     * @param entityName - The name of the entity to query against
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery from(String entityName) {
        this.entityName = entityName;
        this.dynamicViewEntity = null;
        return this;
    }

    /** Set the entity to query against
     * @param dynamicViewEntity - The DynamicViewEntity object to query against
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery from(DynamicViewEntity dynamicViewEntity) {
        this.dynamicViewEntity  = dynamicViewEntity;
        this.entityName = null;
        return this;
    }

    /** Set the EntityCondition to be used as the WHERE clause for the query
     * 
     * NOTE: Each successive call to any of the where(...) methods will replace the currently set condition for the query.
     * @param entityCondition - An EntityCondition object to be used as the where clause for this query
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery where(EntityCondition entityCondition) {
        this.whereEntityCondition = entityCondition;
        return this;
    }

    /** Set a Map of field name/values to be ANDed together as the WHERE clause for the query
     * 
     * NOTE: Each successive call to any of the where(...) methods will replace the currently set condition for the query.
     * @param fieldMap - A Map of field names/values to be ANDed together as the where clause for the query
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery where(Map<String, Object> fieldMap) {
        this.fieldMap = fieldMap;
        return this;
    }

    /** Set a series of field name/values to be ANDed together as the WHERE clause for the query
     * 
     * NOTE: Each successive call to any of the where(...) methods will replace the currently set condition for the query.
     * @param fields - A series of field names/values to be ANDed together as the where clause for the query
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery where(Object...fields) {
        this.whereEntityCondition = EntityCondition.makeCondition(UtilMisc.toMap(fields));
        return this;
    }

    /** Set a series of EntityConditions to be ANDed together as the WHERE clause for the query
     * 
     * NOTE: Each successive call to any of the where(...) methods will replace the currently set condition for the query.
     * @param entityCondition - A series of EntityConditions to be ANDed together as the where clause for the query
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery where(EntityCondition...entityCondition) {
        this.whereEntityCondition = EntityCondition.makeCondition(Arrays.asList(entityCondition));
        return this;
    }

    /** Set a list of EntityCondition objects to be ANDed together as the WHERE clause for the query
     * 
     * NOTE: Each successive call to any of the where(...) methods will replace the currently set condition for the query.
     * @param andConditions - A list of EntityCondition objects to be ANDed together as the WHERE clause for the query
     * @return this EntityQuery object, to enable chaining
     */
    public <T extends EntityCondition> EntityQuery where(List<T> andConditions) {
        this.whereEntityCondition = EntityCondition.makeCondition(andConditions);
        return this;
    }

    /** Set the EntityCondition to be used as the HAVING clause for the query.
     * 
     * NOTE: Each successive call to any of the having(...) methods will replace the currently set condition for the query.
     * @param entityCondition - The EntityCondition object that specifies how to constrain
     *            this query after any groupings are done (if this is a view
     *            entity with group-by aliases)
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery having(EntityCondition entityCondition) {
        this.havingEntityCondition = entityCondition;
        return this;
    }

    /** The fields of the named entity to order the resultset by; optionally add a " ASC" for ascending or " DESC" for descending
     * 
     * NOTE: Each successive call to any of the orderBy(...) methods will replace the currently set orderBy fields for the query.
     * @param orderBy - The fields of the named entity to order the resultset by
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery orderBy(List<String> orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /** The fields of the named entity to order the resultset by; optionally add a " ASC" for ascending or " DESC" for descending
     * 
     * NOTE: Each successive call to any of the orderBy(...) methods will replace the currently set orderBy fields for the query.
     * @param fields - The fields of the named entity to order the resultset by
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery orderBy(String...fields) {
        this.orderBy = Arrays.asList(fields);
        return this;
    }

    /** Indicate that the ResultSet object's cursor may move only forward (this is the default behavior)
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery cursorForwardOnly() {
        this.resultSetType = EntityFindOptions.TYPE_FORWARD_ONLY;
        return this;
    }

    /** Indicate that the ResultSet object's cursor is scrollable but generally sensitive to changes to the data that underlies the ResultSet.
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery cursorScrollSensitive() {
        this.resultSetType = EntityFindOptions.TYPE_SCROLL_SENSITIVE;
        return this;
    }

    /** Indicate that the ResultSet object's cursor is scrollable but generally not sensitive to changes to the data that underlies the ResultSet.
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery cursorScrollInsensitive() {
        this.resultSetType = EntityFindOptions.TYPE_SCROLL_INSENSITIVE;
        return this;
    }

    /** Specifies the fetch size for this query. -1 will fall back to datasource settings.
     * 
     * @param fetchSize - The fetch size for this query
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery fetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    /** Specifies the max number of rows to return, 0 means all rows.
     * 
     * @param maxRows - the max number of rows to return
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery maxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

    /** Specifies that the values returned should be filtered to remove duplicate values.
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery distinct() {
        this.distinct = true;
        return this;
    }

    /** Specifies whether the values returned should be filtered to remove duplicate values.
     * 
     * @param distinct - boolean indicating whether the values returned should be filtered to remove duplicate values
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /** Specifies whether results should be read from the cache (or written to the cache if the results have not yet been cached)
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery cache() {
        this.useCache = true;
        return this;
    }

    /** Specifies whether results should be read from the cache (or written to the cache if the results have not yet been cached)
     * 
     * @param useCache - boolean to indicate if the cache should be used or not
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery cache(boolean useCache) {
        this.useCache = useCache;
        return this;
    }

    /** Specifies whether the query should return only values that are currently active using from/thruDate fields.
     * 
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery filterByDate() {
        this.filterByDate  = true;
        this.filterByDateMoment = null;
        this.filterByFieldNames = null;
        return this;
    }

    /** Specifies whether the query should return only values that are active during the specified moment using from/thruDate fields.
     * 
     * @param moment - Timestamp representing the moment in time that the values should be active during
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery filterByDate(Timestamp moment) {
        if (moment != null) {
            this.filterByDate = true;
            this.filterByDateMoment = moment;
            this.filterByFieldNames = null;
        } else {
            // Maintain existing behavior exhibited by EntityUtil.filterByDate(moment) when moment is null and perform no date filtering
            this.filterByDate = false;
            this.filterByDateMoment = null;
            this.filterByFieldNames = null;
        }
        return this;
    }

    /** Specifies whether the query should return only values that are active during the specified moment using from/thruDate fields.
     * 
     * @param moment - Date representing the moment in time that the values should be active during
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery filterByDate(Date moment) {
        this.filterByDate(new java.sql.Timestamp(moment.getTime()));
        return this;
    }

    /** Specifies whether the query should return only values that are currently active using the specified from/thru field name pairs.
     * 
     * @param filterByFieldName - String pairs representing the from/thru date field names e.g. "fromDate", "thruDate", "contactFromDate", "contactThruDate"
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery filterByDate(String... filterByFieldName) {
        return this.filterByDate(null, filterByFieldName);
    }

    /** Specifies whether the query should return only values that are active during the specified moment using the specified from/thru field name pairs.
     * 
     * @param moment - Timestamp representing the moment in time that the values should be active during
     * @param filterByFieldName - String pairs representing the from/thru date field names e.g. "fromDate", "thruDate", "contactFromDate", "contactThruDate"
     * @return this EntityQuery object, to enable chaining
     */
    public EntityQuery filterByDate(Timestamp moment, String... filterByFieldName) {
        this.filterByDate  = true;
        this.filterByDateMoment = moment;
        if (filterByFieldName.length % 2 != 0) {
            throw new IllegalArgumentException("You must pass an even sized array to this method, each pair should represent a from date field name and a thru date field name");
        }
        this.filterByFieldNames = Arrays.asList(filterByFieldName);
        return this;
    }

    /** Executes the EntityQuery and returns a list of results
     * 
     * @return Returns a List of GenericValues representing the results of the query
     */
    public List<GenericValue> queryList() throws GenericEntityException {
        return query(null);
    }

    /** Executes the EntityQuery and returns an EntityListIterator representing the result of the query. 
     * 
     * NOTE:  THAT THIS MUST BE CLOSED (preferably in a finally block) WHEN YOU
     *        ARE DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT
     *        WILL MAINTAIN A DATABASE CONNECTION.
     * 
     * @return Returns an EntityListIterator representing the result of the query
     */
    public EntityListIterator queryIterator() throws GenericEntityException {
        if (useCache) {
            Debug.logWarning("Call to iterator() with cache, ignoring cache", module);
        }
        if (dynamicViewEntity == null) {
            return delegator.find(entityName, makeWhereCondition(false), havingEntityCondition, fieldsToSelect, orderBy, makeEntityFindOptions());
        } else {
            return delegator.findListIteratorByCondition(dynamicViewEntity, makeWhereCondition(false), havingEntityCondition, fieldsToSelect, orderBy, makeEntityFindOptions());
        }
    }

    /** Executes the EntityQuery and returns the first result
     * 
     * @return GenericValue representing the first result record from the query
     */
    public GenericValue queryFirst() throws GenericEntityException {
        EntityFindOptions efo = makeEntityFindOptions();
        // Only limit results when the query isn't filtering by date in memory against a cached result
        if (!this.useCache && !this.filterByDate) {
            efo.setMaxRows(1);
        }
        GenericValue result =  EntityUtil.getFirst(query(efo));
        return result;
    }

    /** Executes the EntityQuery and a single result record
     * 
     * @return GenericValue representing the only result record from the query
     */
    public GenericValue queryOne() throws GenericEntityException {
        this.searchPkOnly = true;
        GenericValue result =  EntityUtil.getOnly(queryList());
        return result;
    }

    /** Executes the EntityQuery and returns the result count
     * 
     * If the query generates more than a single result then an exception is thrown
     * 
     * @return GenericValue representing the only result record from the query
     */
    public long queryCount() throws GenericEntityException {
        if (dynamicViewEntity != null) {
            try (EntityListIterator iterator = queryIterator()) {
                return iterator.getResultsSizeAfterPartialList();
            }
        }
        return delegator.findCountByCondition(entityName, makeWhereCondition(false), havingEntityCondition, makeEntityFindOptions());
    }

    private List<GenericValue> query(EntityFindOptions efo) throws GenericEntityException {
        EntityFindOptions findOptions = null;
        if (efo == null) {
            findOptions = makeEntityFindOptions();
        } else {
            findOptions = efo;
        }
        List<GenericValue> result = null;
        if (dynamicViewEntity == null && this.havingEntityCondition == null) {
            result = delegator.findList(entityName, makeWhereCondition(useCache), fieldsToSelect, orderBy, findOptions, useCache);
        } else {
            try (EntityListIterator it = queryIterator()) { 
                result = it.getCompleteList();
            }
        }
        if (filterByDate && useCache) {
            return EntityUtil.filterByCondition(result, this.makeDateCondition());
        }
        return result;
    }

    private EntityFindOptions makeEntityFindOptions() {
        EntityFindOptions findOptions = new EntityFindOptions();
        if (resultSetType != null) {
            findOptions.setResultSetType(resultSetType);
        }
        if (fetchSize != null) {
            findOptions.setFetchSize(fetchSize);
        }
        if (maxRows != null) {
            findOptions.setMaxRows(maxRows);
        }
        if (distinct != null) {
            findOptions.setDistinct(distinct);
        }
        return findOptions;
    }

    private EntityCondition makeWhereCondition(boolean usingCache) {
        if (whereEntityCondition == null && fieldMap != null) {
            if (this.searchPkOnly) {
                //Resolve if the map contains a sub map parameters, use a containsKeys to avoid error when a GenericValue is given as map
                Map<String, Object> parameters =
                        fieldMap.containsKey("parameters") ? UtilGenerics.cast(fieldMap.get("parameters")) : null;
                GenericPK pk = GenericPK.create(delegator.getModelEntity(entityName));
                pk.setPKFields(parameters);
                pk.setPKFields(fieldMap);
                this.whereEntityCondition = EntityCondition.makeCondition(pk.getPrimaryKey());
            } else {
                this.whereEntityCondition = EntityCondition.makeCondition(fieldMap);
            }
        }
        // we don't use the useCache field here because not all queries will actually use the cache, e.g. findCountByCondition never uses the cache
        if (filterByDate && !usingCache) {
            if (whereEntityCondition != null) {
                return EntityCondition.makeCondition(whereEntityCondition, this.makeDateCondition());
            } else {
                return this.makeDateCondition();
            }
        }
        return whereEntityCondition;
    }

    private EntityCondition makeDateCondition() {
        List<EntityCondition> conditions = new ArrayList<>();
        if (UtilValidate.isEmpty(this.filterByFieldNames)) {
            this.filterByDate(filterByDateMoment, "fromDate", "thruDate");
        }

        for (int i = 0; i < this.filterByFieldNames.size();) {
            String fromDateFieldName = this.filterByFieldNames.get(i++);
            String thruDateFieldName = this.filterByFieldNames.get(i++);
            if (filterByDateMoment == null) {
                conditions.add(EntityUtil.getFilterByDateExpr(fromDateFieldName, thruDateFieldName));
            } else {
                conditions.add(EntityUtil.getFilterByDateExpr(this.filterByDateMoment, fromDateFieldName, thruDateFieldName));
            }
        }
        return EntityCondition.makeCondition(conditions);
    }

    public <T> List<T> getFieldList(final String fieldName) throws GenericEntityException {select(fieldName);
        try (EntityListIterator genericValueEli = queryIterator()) {
            if (Boolean.TRUE.equals(this.distinct)) {
                Set<T> distinctSet = new HashSet<>();
                GenericValue value = null;
                while ((value = genericValueEli.next()) != null) {
                    T fieldValue = UtilGenerics.<T>cast(value.get(fieldName));
                    if (fieldValue != null) {
                        distinctSet.add(fieldValue);
                    }
                }
                return new ArrayList<>(distinctSet);
            }
            else {
                List<T> fieldList = new LinkedList<>();
                GenericValue value = null;
                while ((value = genericValueEli.next()) != null) {
                    T fieldValue = UtilGenerics.<T>cast(value.get(fieldName));
                    if (fieldValue != null) {
                        fieldList.add(fieldValue);
                    }
                }
                return fieldList;
            }
        }
    }

    /**
     * @param viewIndex
     * @param viewSize
     * @return PagedList object with a subset of data items
     * @throws GenericEntityException
     * @see EntityUtil#getPagedList
     */
    public PagedList<GenericValue> queryPagedList(final int viewIndex, final int viewSize) throws GenericEntityException {
        try (EntityListIterator genericValueEli = queryIterator()) {
            return EntityUtil.getPagedList(genericValueEli, viewIndex, viewSize);
        }
    }

}
