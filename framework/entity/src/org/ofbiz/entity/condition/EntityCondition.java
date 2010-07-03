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
package org.ofbiz.entity.condition;

import static org.ofbiz.base.util.UtilGenerics.cast;

import java.util.List;
import java.util.Map;

import javolution.lang.Reusable;
import javolution.util.FastList;

import org.ofbiz.base.lang.IsEmpty;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.model.ModelEntity;

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
public abstract class EntityCondition extends EntityConditionBase implements IsEmpty, Reusable {

    public static <L,R,LL,RR> EntityExpr makeCondition(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(lhs, operator, rhs);
        return expr;
    }

    public static <R> EntityExpr makeCondition(String fieldName, R value) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(fieldName, EntityOperator.EQUALS, value);
        return expr;
    }

    public static EntityExpr makeCondition(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        EntityExpr expr = EntityExpr.entityExprFactory.object();
        expr.init(lhs, operator, rhs);
        return expr;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(EntityJoinOperator operator, T... conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(operator, conditionList);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(T... conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(EntityOperator.AND, conditionList);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList, EntityJoinOperator operator) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(conditionList, operator);
        return ecl;
    }

    public static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<T> conditionList) {
        EntityConditionList<T> ecl = cast(EntityConditionList.entityConditionListFactory.object());
        ecl.init(conditionList, EntityOperator.AND);
        return ecl;
    }

    public static <L,R> EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, compOp, joinOp);
        return efm;
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityJoinOperator joinOp) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, EntityOperator.EQUALS, joinOp);
        return efm;
    }

    public static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(fieldMap, EntityOperator.EQUALS, EntityOperator.AND);
        return efm;
    }

    public static <L,R> EntityFieldMap makeCondition(EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp, Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(compOp, joinOp, keysValues);
        return efm;
    }

    public static EntityFieldMap makeCondition(EntityJoinOperator joinOp, Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(EntityOperator.EQUALS, joinOp, keysValues);
        return efm;
    }

    public static EntityFieldMap makeConditionMap(Object... keysValues) {
        EntityFieldMap efm = EntityFieldMap.entityFieldMapFactory.object();
        efm.init(EntityOperator.EQUALS, EntityOperator.AND, keysValues);
        return efm;
    }

    public static EntityDateFilterCondition makeConditionDate(String fromDateName, String thruDateName) {
        EntityDateFilterCondition edfc = EntityDateFilterCondition.entityDateFilterConditionFactory.object();
        edfc.init(fromDateName, thruDateName);
        return edfc;
    }

    public static EntityWhereString makeConditionWhere(String sqlString) {
        EntityWhereString ews = EntityWhereString.entityWhereStringFactory.object();
        ews.init(sqlString);
        return ews;
    }

    @Override
    public String toString() {
        return makeWhereString(null, FastList.<EntityConditionParam>newInstance(), null);
    }

    public void accept(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".accept not implemented");
    }

    abstract public String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo);

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

    abstract public void encryptConditionFields(ModelEntity modelEntity, Delegator delegator);

    public void visit(EntityConditionVisitor visitor) {
        throw new IllegalArgumentException(getClass().getName() + ".visit not implemented");
    }
}
