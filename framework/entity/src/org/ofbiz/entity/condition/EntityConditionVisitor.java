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

import java.util.List;

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
public interface EntityConditionVisitor {
    void visit(Object obj);
    void accept(Object obj);
    void acceptObject(Object obj);
    void acceptEntityCondition(EntityCondition condition);
    void acceptEntityJoinOperator(EntityJoinOperator op, List conditions);
    void acceptEntityOperator(EntityOperator op, Object lhs, Object rhs);
    void acceptEntityComparisonOperator(EntityComparisonOperator op, Object lhs, Object rhs);
    void acceptEntityConditionValue(EntityConditionValue value);
    void acceptEntityFieldValue(EntityFieldValue value);

    void acceptEntityExpr(EntityExpr expr);
    void acceptEntityConditionList(EntityConditionList list);
    void acceptEntityFieldMap(EntityFieldMap fieldMap);
    void acceptEntityConditionFunction(EntityConditionFunction func, EntityCondition nested);
    void acceptEntityFunction(EntityFunction func);
    void acceptEntityWhereString(EntityWhereString condition);

    void acceptEntityDateFilterCondition(EntityDateFilterCondition condition);
}
