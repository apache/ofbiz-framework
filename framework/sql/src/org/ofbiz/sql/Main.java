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

public final class Main {
	public static void main(String[] args) throws Exception {
        Planner<?, ?, ?, ?, ?, ?> planner = new DebugPlanner();
		List<SQLStatement<?, ?>> statements = new Parser(System.in).SQLFile();
        for (SQLStatement<?, ?> statement: statements) {
            run(statement, planner);
        }
	}

    private static final void run(SQLStatement statement, Planner planner) {
        System.err.println(statement);
        SQLPlan plan = statement.plan(planner);
        System.err.println("\tplan=" + plan);
    }

    private final static class DebugPlanner extends Planner {
        public SQLPlan plan(SQLStatement statement) {
            return null;
        }

        public DeletePlan plan(SQLDelete deleteStatement) {
            return null;
        }

        public InsertPlan plan(SQLInsert insertStatement) {
            return null;
        }

        public SelectPlan plan(SQLSelect selectStatement) {
            return null;
        }

        public UpdatePlan plan(SQLUpdate updateStatement) {
            return null;
        }

        public ViewPlan plan(SQLView viewStatement) {
            return null;
        }
    }
}
