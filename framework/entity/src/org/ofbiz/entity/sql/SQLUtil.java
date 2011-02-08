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
package org.ofbiz.entity.sql;

import java.io.StringReader;

import org.ofbiz.sql.OrderByItem;
import org.ofbiz.sql.ParseException;
import org.ofbiz.sql.Parser;

public class SQLUtil {
    private static final EntityPlanner planner = new EntityPlanner();

    private static Parser updateParserFlags(Parser parser) {
        return parser.deleteSupportsUsing(false).updateSupportsFrom(false);
    }

    public static EntitySelectPlan parseSelect(String sql) throws ParseException {
       return planner.planSelect(updateParserFlags(new Parser(new StringReader(sql))).SelectStatement());
    }

    public static OrderByItem parseOrderByItem(String sql) throws ParseException {
       return updateParserFlags(new Parser(new StringReader(sql))).OrderByItem();
    }
    /*
    public static EntityCondition parseCondition(String condition) throws ParseException {
        return new Parser(new StringReader(condition)).EntityCondition();
    }
    */
}
