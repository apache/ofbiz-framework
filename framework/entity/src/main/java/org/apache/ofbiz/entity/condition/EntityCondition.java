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
package org.apache.ofbiz.entity.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.IsEmpty;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;

/**
 * Represents the conditions to be used to constrain a query
 * <br/>An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 */
@SuppressWarnings("serial")
public abstract class EntityCondition extends EntityConditionBase implements IsEmpty {

    public static <L,R,LL,RR> EntityExpr makeCondition(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        return new EntityExpr(lhs, operator, rhs);
    }

    public static <R> EntityExpr makeCondition(String fieldName, R value) {
        return new EntityExpr(fieldName, EntityOperator.EQUALS, value);
    }

    public static EntityExpr makeCondition(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        return new EntityExpr(lhs, operator, rhs);
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(EntityJoinOperator operator, T... conditionList) {
        return new EntityConditionList<T>(Arrays.<T>asList(conditionList), operator);
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(T... conditionList) {
        return new EntityConditionList<T>(Arrays.<T>asList(conditionList), EntityOperator.AND);
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList, EntityJoinOperator operator) {
        return new EntityConditionList<T>(conditionList, operator);
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList) {
        return new EntityConditionList<T>(conditionList, EntityOperator.AND);
    }

    public static <L,R> EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp) {
        return new EntityFieldMap(fieldMap, compOp, joinOp);
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityJoinOperator joinOp) {
        return new EntityFieldMap(fieldMap, EntityOperator.EQUALS, joinOp);
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap) {
        return new EntityFieldMap(fieldMap, EntityOperator.EQUALS, EntityOperator.AND);
    }

    public static <L,R> EntityFieldMap makeCondition(EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp, Object... keysValues) {
        return new EntityFieldMap(compOp, joinOp, keysValues);
    }

    public static EntityFieldMap makeCondition(EntityJoinOperator joinOp, Object... keysValues) {
        return new EntityFieldMap(EntityOperator.EQUALS, joinOp, keysValues);
    }

    public static EntityFieldMap makeConditionMap(Object... keysValues) {
        return new EntityFieldMap(EntityOperator.EQUALS, EntityOperator.AND, keysValues);
    }

    public static EntityDateFilterCondition makeConditionDate(String fromDateName, String thruDateName) {
        return new EntityDateFilterCondition(fromDateName, thruDateName);
    }

    public static EntityWhereString makeConditionWhere(String sqlString) {
        return new EntityWhereString(sqlString);
    }

    @Override
    public String toString() {
        return makeWhereString(null, new ArrayList<EntityConditionParam>(), null);
    }

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
    }

    abstract public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, Datasource datasourceInfo);

    abstract public void checkCondition(ModelEntity modelEntity) throws GenericModelException;

    public boolean entityMatches(GenericEntity entity) {
        return mapMatches(entity.getDelegator(), entity);
    }

    public Boolean eval(GenericEntity entity) {
        return eval(entity.getDelegator(), entity);
    }

    public Boolean eval(Delegator delegator, Map<String, ? extends Object> map) {
        return mapMatches(delegator, map) ? Boolean.TRUE : Boolean.FALSE;
    }

    abstract public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map);

    abstract public EntityCondition freeze();

    public void visit(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".visit not implemented");
    }
}
