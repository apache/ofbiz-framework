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

import java.util.List;
import java.util.Map;

public final class Main {
    public static void main(String[] args) throws Exception {
        Planner<?, ?, ?, ?, ?, ?, ?> planner = new DebugPlanner();
        List<SQLStatement<?>> statements = new Parser(System.in).SQLFile();
        for (SQLStatement<?> statement: statements) {
            run(statement, planner);
        }
    }

    private static final void run(SQLStatement statement, Planner planner) {
        System.err.println(statement);
        SQLPlan plan = planner.plan(statement);
        System.err.println("\tplan=" + plan);
    }

    private final static class DebugPlanner extends Planner<DebugPlanner, DebugCondition, DebugDeletePlan, DebugInsertPlan, DebugSelectPlan, DebugUpdatePlan, DebugViewPlan> {
        public DebugPlanner() {
            super(new DebugConditionPlanner());
        }

        public DebugDeletePlan planDelete(SQLDelete deleteStatement) {
            return null;
        }

        public DebugInsertPlan planInsert(SQLInsert insertStatement) {
            return null;
        }

        public DebugSelectPlan planSelect(SQLSelect selectStatement) {
            return null;
        }

        public DebugUpdatePlan planUpdate(SQLUpdate updateStatement) {
            return null;
        }

        public DebugViewPlan planView(SQLView viewStatement) {
            return null;
        }
    }

    private final static class DebugConditionPlanner implements ConditionPlanner<DebugCondition> {
        public DebugCondition parse(Condition originalCondition, Map<String, ? extends Object> params) throws ParameterizedConditionException {
            return null;
        }
    }

    private final static class DebugCondition {
    }

    private final static class DebugDeletePlan extends DeletePlan<DebugDeletePlan, DebugCondition> {
        protected DebugDeletePlan(ConditionPlan<DebugCondition> wherePlan) {
            super(wherePlan);
        }
    }

    private final static class DebugInsertPlan extends InsertPlan<DebugInsertPlan> {
    }

    private final static class DebugSelectPlan extends SelectPlan<DebugSelectPlan, DebugCondition> {
        protected DebugSelectPlan(ConditionPlan<DebugCondition> wherePlan, ConditionPlan<DebugCondition> havingPlan) {
            super(wherePlan, havingPlan);
        }
    }

    private final static class DebugUpdatePlan extends UpdatePlan<DebugUpdatePlan, DebugCondition> {
        protected DebugUpdatePlan(ConditionPlan<DebugCondition> wherePlan) {
            super(wherePlan);
        }
    }

    private final static class DebugViewPlan extends ViewPlan<DebugViewPlan> {
    }
}
