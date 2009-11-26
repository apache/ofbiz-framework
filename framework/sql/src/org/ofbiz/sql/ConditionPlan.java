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

import java.util.Map;

public final class ConditionPlan<C> extends SQLPlan<ConditionPlan<C>> {
    private final ConditionPlanner<C> planner;
    private final Condition originalCondition;
    private final C condition;

    public ConditionPlan(ConditionPlanner<C> planner, Condition originalCondition, C condition) {
        this.planner = planner;
        this.originalCondition = originalCondition;
        this.condition = condition;
    }

    public C getCondition(Map<String, ? extends Object> params) throws ParameterizedConditionException {
        if (originalCondition != null) {
            return planner.parse(originalCondition, params);
        } else {
            return condition;
        }
    }

    public StringBuilder appendTo(StringBuilder sb) {
        sb.append("ConditionPlan[");
        if (originalCondition != null) {
            sb.append("original=" + originalCondition);
        } else {
            sb.append("condition=" + condition);
        }
        return sb.append("]");
    }
}
