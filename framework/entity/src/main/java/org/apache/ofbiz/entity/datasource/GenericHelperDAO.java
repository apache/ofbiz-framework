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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;

/**
 * Generic Entity Helper Class
 *
 */
public class GenericHelperDAO implements GenericHelper {

    public static final String module = GenericHelperDAO.class.getName();

    protected GenericDAO genericDAO;
    protected GenericHelperInfo helperInfo;

    public GenericHelperDAO(GenericHelperInfo helperInfo) {
        this.helperInfo = helperInfo;
        genericDAO = GenericDAO.getGenericDAO(helperInfo);
    }

    @Override
    public String getHelperName() {
        return this.helperInfo.getHelperFullName();
    }

    /** Creates a Entity in the form of a GenericValue and write it to the database
     *@return GenericValue instance containing the new instance
     */
    @Override
    public GenericValue create(GenericValue value) throws GenericEntityException {
        if (value == null) {
            return null;
        }
        int retVal = genericDAO.insert(value);
        if (Debug.verboseOn()) Debug.logVerbose("Insert Return Value : " + retVal, module);
        return value;
    }

    /** Find a Generic Entity by its Primary Key
     *@param primaryKey The primary key to find by.
     *@return The GenericValue corresponding to the primaryKey
     */
    @Override
    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        if (primaryKey == null) {
            return null;
        }
        GenericValue genericValue = GenericValue.create(primaryKey);

        genericDAO.select(genericValue);
        return genericValue;
    }

    /** Find a Generic Entity by its Primary Key and only returns the values requested by the passed keys (names)
     *@param primaryKey The primary key to find by.
     *@param keys The keys, or names, of the values to retrieve; only these values will be retrieved
     *@return The GenericValue corresponding to the primaryKey
     */
    @Override
    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException {
        if (primaryKey == null) {
            return null;
        }
        GenericValue genericValue = GenericValue.create(primaryKey);

        genericDAO.partialSelect(genericValue, keys);
        return genericValue;
    }

    /** Find a number of Generic Value objects by their Primary Keys, all at once
     * This is done here for the DAO GenericHelper; for a client-server helper it
     * would be done on the server side to reduce network round trips.
     *@param primaryKeys A List of primary keys to find by.
     *@return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    @Override
    public List<GenericValue> findAllByPrimaryKeys(List<GenericPK> primaryKeys) throws GenericEntityException {
        if (primaryKeys == null) return null;
        List<GenericValue> results = new LinkedList<>();

        for (GenericPK primaryKey: primaryKeys) {
            GenericValue result = this.findByPrimaryKey(primaryKey);

            if (result != null) results.add(result);
        }
        return results;
    }

    /** Remove a Generic Entity corresponding to the primaryKey
     *@param  primaryKey  The primary key of the entity to remove.
     *@return int representing number of rows effected by this operation
     */
    @Override
    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        if (primaryKey == null) return 0;
        if (Debug.verboseOn()) Debug.logVerbose("Removing GenericPK: " + primaryKey.toString(), module);
        return genericDAO.delete(primaryKey);
    }

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param whereEntityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@param findOptions An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for more details.
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     *      DONE WITH IT (preferably in a finally block), 
     *      AND DON'T LEAVE IT OPEN TOO LONG BECAUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    @Override
    public EntityListIterator findListIteratorByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition whereEntityCondition,
        EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
        throws GenericEntityException {
        return genericDAO.selectListIteratorByCondition(delegator, modelEntity, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderBy, findOptions);
    }

    @Override
    public List<GenericValue> findByMultiRelation(GenericValue value, ModelRelation modelRelationOne, ModelEntity modelEntityOne,
        ModelRelation modelRelationTwo, ModelEntity modelEntityTwo, List<String> orderBy) throws GenericEntityException {
        return genericDAO.selectByMultiRelation(value, modelRelationOne, modelEntityOne, modelRelationTwo, modelEntityTwo, orderBy);
    }

    @Override
    public long findCountByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        return genericDAO.selectCountByCondition(delegator, modelEntity, whereEntityCondition, havingEntityCondition, findOptions);
    }

    /** Removes/deletes Generic Entity records found by all the specified condition
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param condition The condition that restricts the list of removed values
     *@return int representing number of rows effected by this operation
     */
    @Override
    public int removeByCondition(Delegator delegator, ModelEntity modelEntity, EntityCondition condition) throws GenericEntityException {
        if (modelEntity == null || condition == null) {
            return 0;
        }
        return genericDAO.deleteByCondition(delegator, modelEntity, condition);
    }

    /** Store the Entity from the GenericValue to the persistent store
     *@param value GenericValue instance containing the entity
     *@return int representing number of rows effected by this operation
     */
    @Override
    public int store(GenericValue value) throws GenericEntityException {
        if (value == null) {
            return 0;
        }
        return genericDAO.update(value);
    }

    /** Updates a group of values in a single pass.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param fieldsToSet The fields of the named entity to set in the database
     *@param condition The condition that restricts the list of removed values
     *@return int representing number of rows effected by this operation
     *@throws GenericEntityException
     */
    @Override
    public int storeByCondition(Delegator delegator, ModelEntity modelEntity, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException {
        if (modelEntity == null || condition == null) {
            return 0;
        }
        return genericDAO.updateByCondition(delegator, modelEntity, fieldsToSet, condition);
    }

    /** Check the datasource to make sure the entity definitions are correct, optionally adding missing entities or fields on the server
     *@param modelEntities Map of entityName names and ModelEntity values
     *@param messages List to put any result messages in
     *@param addMissing Flag indicating whether or not to add missing entities and fields on the server
     */
    @Override
    public void checkDataSource(Map<String, ModelEntity> modelEntities, List<String> messages, boolean addMissing) throws GenericEntityException {
        genericDAO.checkDb(modelEntities, messages, addMissing);
    }
}
