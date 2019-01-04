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

import java.io.Serializable;
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
 * Represents a condition expression that can be used in a SQL 'WHERE' clause
 * which is used to constrain a SQL query.
 * <p>
 * An EntityCondition can represent various type of constraints, including:
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 */
public interface EntityCondition extends IsEmpty, Serializable {

    static final long serialVersionUID = -7601205800717391212L;

    /**
     * Constructs a condition expression to represent a comparison between two elements.
     *
     * @param lhs the left hand side element
     * @param operator the binary infix operator
     * @param rhs the right hand side element
     * @return a condition expression representing a comparison between two elements
     */
    static <L,R,LL,RR> EntityExpr makeCondition(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        return new EntityExpr(lhs, operator, rhs);
    }

    /**
     * Constructs a condition expression to represent a equality comparison between a field and a value.
     *
     * @param fieldName the name of the field to compare
     * @param value the value to find in field
     * @return a condition expression representing a equality comparison
     */
    static <R> EntityExpr makeCondition(String fieldName, R value) {
        return new EntityExpr(fieldName, EntityOperator.EQUALS, value);
    }

    /**
     * Constructs a condition expression to represent a combination of two condition expressions.
     *
     * @param lhs the left hand side condition
     * @param operator the binary infix operator used to combine {@code lhs} and {@code rhs} conditions
     * @param rhs the right hand side condition
     * @return a condition expression representing a combination of condition expression
     */
    static EntityExpr makeCondition(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        return new EntityExpr(lhs, operator, rhs);
    }

    /**
     * Constructs a condition expression to represent a combination of condition expressions.
     *
     * @param operator the binary infix operator used to join every elements of {@code conditionList}
     * @param conditionList the list of condition expressions to join
     * @return a condition expression representing a combination of condition expressions.
     */
    @SafeVarargs
    static <R extends EntityCondition, T extends R>
    EntityConditionList<R> makeCondition(EntityJoinOperator operator, T... conditionList) {
        return new EntityConditionList<>(Arrays.asList(conditionList), operator);
    }

    /**
     * Constructs a condition expression to represent a conjunction of condition expressions
     *
     * @param conditionList the condition expressions to join with {@link EntityOperator#AND}
     * @return a condition expression representing a conjunction of condition expressions
     */
    @SafeVarargs
    static <R extends EntityCondition, T extends R>
    EntityConditionList<R> makeCondition(T... conditionList) {
        return new EntityConditionList<>(Arrays.asList(conditionList), EntityOperator.AND);
    }

    /**
     * Constructs a condition expression to represent a combination of condition expressions.
     *
     * @param conditionList the list of condition expressions to join
     * @param operator the binary infix operator used to join every elements of {@code conditionList}
     * @return a condition expression representing a combination of condition expressions.
     */
    static <T extends EntityCondition>
    EntityConditionList<T> makeCondition(List<? extends T> conditionList, EntityJoinOperator operator) {
        return new EntityConditionList<>(conditionList, operator);
    }

    /**
     * Constructs a condition expression to represent a conjunction of condition expressions
     *
     * @param conditionList the condition expressions to join with {@link EntityOperator#AND}
     * @return a condition expression representing a conjunction of condition expressions
     */
    static <T extends EntityCondition> EntityConditionList<T> makeCondition(List<? extends T> conditionList) {
        return new EntityConditionList<>(conditionList, EntityOperator.AND);
    }

    /**
     * Constructs a condition expression to represent a combination of field/value comparisons.
     *
     * @param fieldMap the map associating a field to the value to match
     * @param compOp the binary infix operator used to compare the field and the value
     * @param joinOp the binary infix operator used to join the field/value comparisons
     * @return a condition expression representing a combination of field/value comparisons
     */
    static <L,R> EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap,
            EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp) {
        return new EntityFieldMap(fieldMap, compOp, joinOp);
    }

    /**
     * Constructs a condition expression to represent a combination of field/value equality comparisons.
     *
     * @param fieldMap the map associating a field to the value to match with {@link EntityOperator#EQUALS}
     * @param joinOp the binary infix operator used to join the field/value equality comparisons
     * @return a condition expression representing a combination of field/value equality comparisons
     */
    static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap, EntityJoinOperator joinOp) {
        return new EntityFieldMap(fieldMap, EntityOperator.EQUALS, joinOp);
    }

    /**
     * Constructs a condition expression to represent a conjunction of field/value equality comparisons.
     *
     * @param fieldMap the map associating a field to the value to match with {@link EntityOperator#EQUALS}
     * @return a condition expression representing a conjunction of field/value equality comparisons
     */
    static EntityFieldMap makeCondition(Map<String, ? extends Object> fieldMap) {
        return new EntityFieldMap(fieldMap, EntityOperator.EQUALS, EntityOperator.AND);
    }

    /**
     * Constructs a condition expression to represent a combination of field/value comparisons.
     *
     * @param compOp the binary infix operator used to compare the field and the value
     * @param joinOp the binary infix operator used to join the field/value comparisons
     * @param keysValues the field/values pairs to match
     * @return a condition expression representing a combination of field/value comparisons
     */
    static <L,R> EntityFieldMap makeCondition(EntityComparisonOperator<L,R> compOp, EntityJoinOperator joinOp,
            Object... keysValues) {
        return new EntityFieldMap(compOp, joinOp, keysValues);
    }

    /**
     * Constructs a condition expression to represent a combination of field/value equality comparisons.
     *
     * @param joinOp the binary infix operator used to join the field/value equality comparisons
     * @param keysValues the field/values pairs to match with {@link EntityOperator#EQUALS}
     * @return a condition expression representing a combination of field/value equality comparisons
     */
    static EntityFieldMap makeCondition(EntityJoinOperator joinOp, Object... keysValues) {
        return new EntityFieldMap(EntityOperator.EQUALS, joinOp, keysValues);
    }

    /**
     * Constructs a condition expression to represent a conjunction of field/value equality comparisons.
     *
     * @param keysValues the field/values pairs to match with {@link EntityOperator#EQUALS}
     * @return a condition expression representing a conjunction of field/value equality comparisons
     */
    static EntityFieldMap makeConditionMap(Object... keysValues) {
        return new EntityFieldMap(EntityOperator.EQUALS, EntityOperator.AND, keysValues);
    }

    /**
     * Constructs a condition expression to filter rows that are currently valid.
     *
     * This means that we remove rows whose from/thru date range does not match the current date.
     * The <i>current date</i> is the one computed when the SQL query is generated.
     *
     * @param fromDateName the name of the field corresponding to the from date
     * @param thruDateName the name of the field corresponding to the thru date
     * @return a condition expression filtering rows that are currently valid
     */
    static EntityDateFilterCondition makeConditionDate(String fromDateName, String thruDateName) {
        return new EntityDateFilterCondition(fromDateName, thruDateName);
    }

    /**
     * Constructs a condition expression backed by a raw SQL string
     *
     * @param sqlString the SQL string
     * @return a raw SQL string condition expression
     */
    static EntityWhereString makeConditionWhere(String sqlString) {
        return new EntityWhereString(sqlString);
    }

    /**
     * Applies a visitor to this condition.
     *
     * @param visitor the visitor to be applied
     */
    void accept(EntityConditionVisitor visitor);

    /**
     * Dumps the corresponding SQL string.
     *
     * @param modelEntity the model of the entity
     * @param entityConditionParams the effective parameters used to substitute '?' parameters
     * @param datasourceInfo the model of the data source interpreting the SQL
     * @return the corresponding SQL string
     */
    String makeWhereString(ModelEntity modelEntity, List<EntityConditionParam> entityConditionParams,
            Datasource datasourceInfo);

    /**
     * Verifies that this condition expression is valid.
     *
     * @param modelEntity the model of the entity
     * @throws GenericModelException when this condition expression is not valid
     */
    void checkCondition(ModelEntity modelEntity) throws GenericModelException;

    /**
     * Checks that this condition expression matches a particular entity.
     *
     * @param entity the entity to match
     * @return {@code true} if this condition expression matches {@code entity}
     */
    default boolean entityMatches(GenericEntity entity) {
        return mapMatches(entity.getDelegator(), entity);
    }

    /**
     * Checks that this condition expression matches a particular entity.
     *
     * @param delegator the delegator used to match
     * @param map the entity definition to match
     * @return {@code true} if this condition expression matches {@code map} when using {@code delegator}
     */
    boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map);

    /**
     * Create a Frozen condition expression corresponding to this condition expression.
     *
     * @return the frozen condition expression
     */
    EntityCondition freeze();

    /**
     * Dumps the corresponding SQL string without substituting '?' parameters.
     *
     * @return the corresponding SQL string
     */
    default String makeWhereString() {
        return makeWhereString(null, new ArrayList<EntityConditionParam>(), null);
    }
}
