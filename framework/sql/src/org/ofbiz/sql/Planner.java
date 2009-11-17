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

public abstract class Planner<P extends Planner<P, C, D, I, S, U, V>, C, D extends DeletePlan<D>, I extends InsertPlan<I>, S extends SelectPlan<S>, U extends UpdatePlan<U>, V extends ViewPlan<V>> {
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

    public abstract D plan(SQLDelete<?> deleteStatement);
    public abstract I plan(SQLInsert<?> insertStatement);
    public abstract S plan(SQLSelect<?> selectStatement);
    public abstract U plan(SQLUpdate<?> updateStatement);
    public abstract V plan(SQLView<?> viewStatement);
}
