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

package org.ofbiz.entity.util;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityDateFilterCondition;
import org.ofbiz.entity.condition.EntityFieldMap;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.OrderByList;
import org.ofbiz.entity.model.ModelField;

/**
 * Helper methods when dealing with Entities, especially ones that follow certain conventions
 */
public class EntityUtil {

    public static final String module = EntityUtil.class.getName();

    public static GenericValue getFirst(List values) {
        if ((values != null) && (values.size() > 0)) {
            return (GenericValue) values.get(0);
        } else {
            return null;
        }
    }

    public static GenericValue getOnly(List values) {
        if (values != null) {
            if (values.size() <= 0) {
                return null;
            }
            if (values.size() == 1) {
                return (GenericValue) values.get(0);
            } else {
                throw new IllegalArgumentException("Passed List had more than one value.");
            }
        } else {
            return null;
        }
    }

    public static EntityCondition getFilterByDateExpr() {
        return new EntityDateFilterCondition("fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(String fromDateName, String thruDateName) {
        return new EntityDateFilterCondition(fromDateName, thruDateName);
    }

    public static EntityCondition getFilterByDateExpr(java.util.Date moment) {
        return EntityDateFilterCondition.makeCondition(new java.sql.Timestamp(moment.getTime()), "fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(java.sql.Timestamp moment) {
        return EntityDateFilterCondition.makeCondition(moment, "fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(java.sql.Timestamp moment, String fromDateName, String thruDateName) {
        return EntityDateFilterCondition.makeCondition(moment, fromDateName, thruDateName);
    }

    /**
     *returns the values that are currently active.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@return List of GenericValue's that are currently active
     */
    public static List filterByDate(List datedValues) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), null, null, true);
    }

    /**
     *returns the values that are currently active.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param allAreSame Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we only have to see if the from and thru date fields are valid once
     *@return List of GenericValue's that are currently active
     */
    public static List filterByDate(List datedValues, boolean allAreSame) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), null, null, allAreSame);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.util.Date moment) {
        return filterByDate(datedValues, new java.sql.Timestamp(moment.getTime()), null, null, true);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.sql.Timestamp moment) {
        return filterByDate(datedValues, moment, null, null, true);
    }

    /**
     *returns the values that are active at the moment.
     *
     *@param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     *@param moment the moment in question
     *@param allAreSame Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we only have to see if the from and thru date fields are valid once
     *@return List of GenericValue's that are active at the moment
     */
    public static List filterByDate(List datedValues, java.sql.Timestamp moment, String fromDateName, String thruDateName, boolean allAreSame) {
        if (datedValues == null) return null;
        if (moment == null) return datedValues;
        if (fromDateName == null) fromDateName = "fromDate";
        if (thruDateName == null) thruDateName = "thruDate";

        List result = FastList.newInstance();
        Iterator iter = datedValues.iterator();

        if (allAreSame) {
            ModelField fromDateField = null;
            ModelField thruDateField = null;

            if (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();

                fromDateField = datedValue.getModelEntity().getField(fromDateName);
                if (fromDateField == null) throw new IllegalArgumentException("\"" + fromDateName + "\" is not a field of " + datedValue.getEntityName());
                thruDateField = datedValue.getModelEntity().getField(thruDateName);
                if (thruDateField == null) throw new IllegalArgumentException("\"" + thruDateName + "\" is not a field of " + datedValue.getEntityName());

                java.sql.Timestamp fromDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                java.sql.Timestamp thruDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
            while (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();
                java.sql.Timestamp fromDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                java.sql.Timestamp thruDate = (java.sql.Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
        } else {
            // if not all values are known to be of the same entity, must check each one...
            while (iter.hasNext()) {
                GenericValue datedValue = (GenericValue) iter.next();
                java.sql.Timestamp fromDate = datedValue.getTimestamp(fromDateName);
                java.sql.Timestamp thruDate = datedValue.getTimestamp(thruDateName);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                }// else not active at moment
            }
        }

        return result;
    }

    public static boolean isValueActive(GenericValue datedValue, java.sql.Timestamp moment) {
        return isValueActive(datedValue, moment, "fromDate", "thruDate");
    }

    public static boolean isValueActive(GenericValue datedValue, java.sql.Timestamp moment, String fromDateName, String thruDateName) {
        java.sql.Timestamp fromDate = datedValue.getTimestamp(fromDateName);
        java.sql.Timestamp thruDate = datedValue.getTimestamp(thruDateName);

        if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
            return true;
        } else {
            // else not active at moment
            return false;
        }
    }

    /**
     *returns the values that match the values in fields
     *
     *@param values List of GenericValues
     *@param fields the field-name/value pairs that must match
     *@return List of GenericValue's that match the values in fields
     */
    public static List filterByAnd(List values, Map fields) {
        if (values == null) return null;

        List result = null;
        if (fields == null || fields.size() == 0) {
            result = FastList.newInstance();
            result.addAll(values);
        } else {
            result = FastList.newInstance();
            Iterator iter = values.iterator();
            while (iter.hasNext()) {
                GenericValue value = (GenericValue) iter.next();
                if (value.matchesFields(fields)) {
                    result.add(value);
                }// else did not match
            }
        }
        return result;
    }

    /**
     *returns the values that match all of the exprs in list
     *
     *@param values List of GenericValues
     *@param exprs the expressions that must validate to true
     *@return List of GenericValue's that match the values in fields
     */
    public static List filterByAnd(List values, List exprs) {
        if (values == null) return null;
        if (exprs == null || exprs.size() == 0) {
            // no constraints... oh well
            return values;
        }

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
            Iterator exprIter = exprs.iterator();
            boolean include = true;

            while (exprIter.hasNext()) {
                EntityCondition condition = (EntityCondition) exprIter.next();
                include = condition.entityMatches(value);
                if (!include) break;
            }
            if (include) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     *returns the values that match any of the exprs in list
     *
     *@param values List of GenericValues
     *@param exprs the expressions that must validate to true
     *@return List of GenericValue's that match the values in fields
     */
    public static List filterByOr(List values, List exprs) {
        if (values == null) return null;
        if (exprs == null || exprs.size() == 0) {
            return values;
        }

        List result = FastList.newInstance();
        Iterator iter = values.iterator();

        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
            boolean include = false;

            Iterator exprIter = exprs.iterator();
            while (exprIter.hasNext()) {
                EntityCondition condition = (EntityCondition) exprIter.next();
                include = condition.entityMatches(value);
                if (include) break;
            }
            if (include) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     *returns the values in the order specified
     *
     *@param values List of GenericValues
     *@param orderBy The fields of the named entity to order the query by;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue's in the proper order
     */
    public static List orderBy(Collection values, List orderBy) {
        if (values == null) return null;
        if (values.size() == 0) return FastList.newInstance();
        if (orderBy == null || orderBy.size() == 0) {
            List newList = FastList.newInstance();
            newList.addAll(values);
            return newList;
        }

        List result = FastList.newInstance();
        result.addAll(values);
        if (Debug.verboseOn()) Debug.logVerbose("Sorting " + values.size() + " values, orderBy=" + orderBy.toString(), module);
        Collections.sort(result, new OrderByList(orderBy));
        return result;
    }

    public static List getRelated(String relationName, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelated(relationName));
        }
        return result;
    }

    public static List getRelatedCache(String relationName, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelatedCache(relationName));
        }
        return result;
    }

    public static List getRelatedByAnd(String relationName, Map fields, List values) throws GenericEntityException {
        if (values == null) return null;

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            result.addAll(((GenericValue) iter.next()).getRelatedByAnd(relationName, fields));
        }
        return result;
    }

    public static List filterByCondition(List values, EntityCondition condition) {
        if (values == null) return null;

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
            if (condition.entityMatches(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static List filterOutByCondition(List values, EntityCondition condition) {
        if (values == null) return null;

        List result = FastList.newInstance();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            GenericValue value = (GenericValue) iter.next();
            if (!condition.entityMatches(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static List findDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search) throws GenericEntityException {
        return findDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static List findDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search, Timestamp now) throws GenericEntityException {
        EntityCondition searchCondition = new EntityConditionList(UtilMisc.toList(
            new EntityFieldMap(search, EntityOperator.AND),
            EntityUtil.getFilterByDateExpr(now)
        ), EntityOperator.AND);
        return delegator.findByCondition(entityName,searchCondition,null,UtilMisc.toList("-fromDate"));
    }

    public static GenericValue newDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search) throws GenericEntityException {
        return newDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static GenericValue newDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search, Timestamp now) throws GenericEntityException {
        List entities = findDatedInclusionEntity(delegator, entityName, search, now);
        if (entities != null && entities.size() > 0) {
            search = null;
            for (int i = 0; i < entities.size(); i++) {
                GenericValue entity = (GenericValue)entities.get(i);
                if (now.equals(entity.get("fromDate"))) {
                    search = FastMap.newInstance();
                    search.putAll(entity.getPrimaryKey());
                    entity.remove("thruDate");
                } else {
                    entity.set("thruDate",now);
                }
                entity.store();
            }
            if (search == null) {
                search = FastMap.newInstance();
                search.putAll(EntityUtil.getFirst(entities));
            }
        } else {
            /* why is this being done? leaving out for now...
            search = new HashMap(search);
            */
        }
        if (now.equals(search.get("fromDate"))) {
            return EntityUtil.getOnly(delegator.findByAnd(entityName, search));
        } else {
            search.put("fromDate",now);
            search.remove("thruDate");
            return delegator.makeValue(entityName, search);
        }
    }

    public static void delDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search) throws GenericEntityException {
        delDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static void delDatedInclusionEntity(GenericDelegator delegator, String entityName, Map search, Timestamp now) throws GenericEntityException {
        List entities = findDatedInclusionEntity(delegator, entityName, search, now);
        for (int i = 0; entities != null && i < entities.size(); i++) {
            GenericValue entity = (GenericValue)entities.get(i);
            entity.set("thruDate",now);
            entity.store();
        }
    }
    
    public static List getFieldListFromEntityList(List genericValueList, String fieldName, boolean distinct) {
        if (genericValueList == null || fieldName == null) {
            return null;
        }
        List fieldList = FastList.newInstance();
        Set distinctSet = null;
        if (distinct) {
            distinctSet = FastSet.newInstance();
        }
        
        Iterator genericValueIter = genericValueList.iterator();
        while (genericValueIter.hasNext()) {
            GenericValue value = (GenericValue) genericValueIter.next();
            Object fieldValue = value.get(fieldName);
            if (fieldValue != null) {
                if (distinct) {
                    if (!distinctSet.contains(fieldValue)) {
                        fieldList.add(fieldValue);
                        distinctSet.add(fieldValue);
                    }
                } else {
                    fieldList.add(fieldValue);
                }
            }
        }
        
        return fieldList;
    }
    
    public static List getFieldListFromEntityListIterator(EntityListIterator genericValueEli, String fieldName, boolean distinct) {
        if (genericValueEli == null || fieldName == null) {
            return null;
        }
        List fieldList = FastList.newInstance();
        Set distinctSet = null;
        if (distinct) {
            distinctSet = FastSet.newInstance();
        }
        
        GenericValue value = null;
        while ((value = (GenericValue) genericValueEli.next()) != null) {
            Object fieldValue = value.get(fieldName);
            if (fieldValue != null) {
                if (distinct) {
                    if (!distinctSet.contains(fieldValue)) {
                        fieldList.add(fieldValue);
                        distinctSet.add(fieldValue);
                    }
                } else {
                    fieldList.add(fieldValue);
                }
            }
        }
        
        return fieldList;
    }
}
