/*
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
 */
package org.ofbiz.sql;

public abstract class Planner<P extends Planner<P, C, D, I, S, U, V>, C, D extends DeletePlan<D, C>, I extends InsertPlan<I>, S extends SelectPlan<S, C>, U extends UpdatePlan<U, C>, V extends ViewPlan<V>> {
    private final ConditionPlanner<C> conditionPlanner;

    protected Planner(ConditionPlanner<C> conditionPlanner) {
        this.conditionPlanner = conditionPlanner;
    }

    public ConditionPlanner<C> getConditionPlanner() {
        return conditionPlanner;
    }

    public ConditionPlan<C> plan(Condition condition) {
        try {
            return new ConditionPlan<C>(conditionPlanner, null, conditionPlanner.parse(condition, null));
        } catch (ParameterizedConditionException e) {
            return new ConditionPlan<C>(conditionPlanner, condition, null);
        }
    }

    public SQLPlan<?> plan(SQLStatement statement) {
        if (statement instanceof SQLDelete) return planDelete((SQLDelete) statement);
        if (statement instanceof SQLInsert) return planInsert((SQLInsert) statement);
        if (statement instanceof SQLSelect) return planSelect((SQLSelect) statement);
        if (statement instanceof SQLUpdate) return planUpdate((SQLUpdate) statement);
        if (statement instanceof SQLView) return planView((SQLView) statement);
        return null;
    }

    public abstract D planDelete(SQLDelete deleteStatement);
    public abstract I planInsert(SQLInsert insertStatement);
    public abstract S planSelect(SQLSelect selectStatement);
    public abstract U planUpdate(SQLUpdate updateStatement);
    public abstract V planView(SQLView viewStatement);
}
