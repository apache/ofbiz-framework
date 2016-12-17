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

import java.util.List;

/**
 * <p>Represents the conditions to be used to constrain a query.</p>
 * <p>An EntityCondition can represent various type of constraints, including:</p>
 * <ul>
 *  <li>EntityConditionList: a list of EntityConditions, combined with the operator specified
 *  <li>EntityExpr: for simple expressions or expressions that combine EntityConditions
 *  <li>EntityFieldMap: a map of fields where the field (key) equals the value, combined with the operator specified
 * </ul>
 * These can be used in various combinations using the EntityConditionList and EntityExpr objects.
 *
 */
public interface EntityConditionVisitor {
    <T> void visit(T obj);
    <T> void accept(T obj);
    void acceptObject(Object obj);
    void acceptEntityCondition(EntityCondition condition);
    <T extends EntityCondition> void acceptEntityJoinOperator(EntityJoinOperator op, List<T> conditions);
    <L,R,T> void acceptEntityOperator(EntityOperator<L, R, T> op, L lhs, R rhs);
    <L,R> void acceptEntityComparisonOperator(EntityComparisonOperator<L, R> op, L lhs, R rhs);
    void acceptEntityConditionValue(EntityConditionValue value);
    void acceptEntityFieldValue(EntityFieldValue value);

    void acceptEntityExpr(EntityExpr expr);
    <T extends EntityCondition> void acceptEntityConditionList(EntityConditionList<T> list);
    void acceptEntityFieldMap(EntityFieldMap fieldMap);
    void acceptEntityConditionFunction(EntityConditionFunction func, EntityCondition nested);
    <T extends Comparable<?>> void acceptEntityFunction(EntityFunction<T> func);
    void acceptEntityWhereString(EntityWhereString condition);

    void acceptEntityDateFilterCondition(EntityDateFilterCondition condition);
}
