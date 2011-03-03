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
package org.ofbiz.sql.test;

import java.io.StringReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.sql.AggregateFunction;
import org.ofbiz.sql.BetweenCondition;
import org.ofbiz.sql.BooleanCondition;
import org.ofbiz.sql.Condition;
import org.ofbiz.sql.ConditionList;
import org.ofbiz.sql.ConstantValue;
import org.ofbiz.sql.FieldAll;
import org.ofbiz.sql.FieldDef;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.FunctionCall;
import org.ofbiz.sql.InsertRow;
import org.ofbiz.sql.InsertValues;
import org.ofbiz.sql.Joined;
import org.ofbiz.sql.Joiner;
import org.ofbiz.sql.KeyMap;
import org.ofbiz.sql.ListCondition;
import org.ofbiz.sql.MathValue;
import org.ofbiz.sql.NumberValue;
import org.ofbiz.sql.OrderByItem;
import org.ofbiz.sql.ParameterValue;
import org.ofbiz.sql.Parser;
import org.ofbiz.sql.Relation;
import org.ofbiz.sql.SQLDelete;
import org.ofbiz.sql.SQLIndex;
import org.ofbiz.sql.SQLInsert;
import org.ofbiz.sql.SQLSelect;
import org.ofbiz.sql.SQLStatement;
import org.ofbiz.sql.SQLUpdate;
import org.ofbiz.sql.SQLView;
import org.ofbiz.sql.SetField;
import org.ofbiz.sql.StringValue;
import org.ofbiz.sql.Table;
import org.ofbiz.sql.TableName;
import org.ofbiz.sql.Value;

public class SQLTest extends GenericTestCaseBase {
    public SQLTest(String name) {
        super(name);
    }

    private static Parser parser(SQLStatement<?> s) throws Exception {
        return new Parser(new StringReader(s.toString()));
    }

    public void testParse() throws Exception {
        List<SQLStatement<?>> statements = new Parser(getClass().getResourceAsStream("GoodParseAll.sql")).SQLFile();
        for (SQLStatement<?> statement: statements) {
            System.err.println(statement);
        }
        Iterator<SQLStatement<?>> stmtIt = statements.iterator();
        assertTrue("has more statements", stmtIt.hasNext());

        {
            SQLSelect select = new SQLSelect(
                false,
                list(
                    new FieldAll("a", Collections.<String>emptySet()),
                    new FieldAll("b", set("partyId")),
                    new FieldAll("c", set("partyId"))
                ),
                GenericTestCaseBase.<String, FieldDef>map(
                    "roleTypeId", new FieldDef(new FieldValue("d", "roleTypeId"), null),
                    "roleDescription", new FieldDef(new FieldValue("d", "description"), "roleDescription"),
                    "SUM",  new FieldDef(new AggregateFunction("SUM", false, new FieldValue("a", "partyId")), null),
                    "baz",  new FieldDef(new FunctionCall("FOO", GenericTestCaseBase.<Value>list(new FieldValue("a", "partyId"), new NumberValue<Integer>(Integer.valueOf(1)))), "baz"),
                    "one",  new FieldDef(new MathValue("||", list(new FieldValue("a", "partyId"), new StringValue("-"), new FieldValue("a", "partyTypeId"))), "one"),
                    "cnt1", new FieldDef(new AggregateFunction("COUNT", false, new FieldValue("a", "partyId")), "cnt1"),
                    "cnt2", new FieldDef(new AggregateFunction("COUNT", false, new FieldValue(null, "partyId")), "cnt2"),
                    "cnt3", new FieldDef(new AggregateFunction("COUNT", true, new FieldValue("a", "partyId")), "cnt3")
                ),
                new Table(
                    new TableName("Party", "a"),
                    new Joined(true, new TableName("Person", "b"), list(new KeyMap("partyId", "partyId")),
                        new Joined(true, new TableName("PartyGroup", "c"), list(new KeyMap("partyId", "partyId")),
                            new Joined(false, new TableName("PartyRole", "d"), list(new KeyMap("partyId", "partyId"), new KeyMap("partyId", "partyId")))
                        )
                    )
                ),
                GenericTestCaseBase.<String, Relation>map(
                    "MainAPerson", new Relation("one", "MainA", "Person", list(new KeyMap("partyId", "partyId"))),
                    "MainBPerson", new Relation(null, "MainB", "Person", list(new KeyMap("partyId", "partyId"))),
                    "Person", new Relation("one", null, "Person", list(new KeyMap("partyId", "partyId"))),
                    "PartyGroup", new Relation(null, null, "PartyGroup", list(new KeyMap("partyId", "partyId")))
                ),
                new ConditionList(
                    Joiner.OR,
                    GenericTestCaseBase.<Condition>list(
                        new ConditionList(
                            Joiner.AND,
                            list(
                                new BooleanCondition(new FieldValue("a", "partyTypeId"), "=", new StringValue("PERSON")),
                                new BooleanCondition(new FieldValue("b", "lastName"), "LIKE", new ParameterValue("lastName")),
                                new BetweenCondition(new FieldValue("b", "birthDate"), new StringValue("1974-12-01"), new StringValue("1974-12-31"))
                            )
                        ),
                        new ConditionList(
                            Joiner.AND,
                            list(
                                new ListCondition(new FieldValue("b", "partyId"), "IN", GenericTestCaseBase.<Value>list(
                                    new StringValue("1"),
                                    new StringValue("2"),
                                    new StringValue("3"),
                                    new StringValue("4")
                                )),
                                new BooleanCondition(new FieldValue("b", "gender"), "=", new StringValue("M"))
                            )
                        )
                    )
                ),
                new BooleanCondition(new FieldValue("b", "firstName"), "LIKE", new StringValue("%foo%")),
                null,
                list(
                    new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, new FunctionCall("LOWER", GenericTestCaseBase.<Value>list(new FieldValue(null, "lastName")))),
                    new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, new FieldValue(null, "firstName")),
                    new OrderByItem(OrderByItem.Order.DESCENDING, OrderByItem.Nulls.DEFAULT, new FieldValue(null, "birthDate"))
                ),
                5,
                10
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstSelect", select, stmt);
            assertEquals("firstSelect:parse", parser(select).SelectStatement(), parser(stmt).SelectStatement());
        }
        {
            SQLInsert insert = new SQLInsert(
                new TableName("Party", null),
                new InsertValues(
                    list(
                        new InsertRow(GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("PERSON"), new StringValue("PARTY_DISABLED"))),
                        new InsertRow(list(new NumberValue<Integer>(Integer.valueOf(5)), new StringValue("PARTY_GROUP"), new ParameterValue("name")))
                    )
                ),
                list("partyId", "partyTypeId", "statusId")
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstInsert", insert, stmt);
            assertEquals("firstInsert:parse", parser(insert).InsertStatement(), parser(stmt).InsertStatement());
        }
        {
            SQLInsert insert = new SQLInsert(
                new TableName("Person", null),
                new SQLSelect(
                    false,
                    null,
                    GenericTestCaseBase.<String, FieldDef>map(
                        "partyId", new FieldDef(new FieldValue(null, "partyId"), null),
                        "firstName",  new FieldDef(new MathValue("||", list(new FieldValue(null, "partyId"), new StringValue("-auto"))), "firstName")
                    ),
                    new Table(new TableName("Party", null), null),
                    null,
                    new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b"))),
                    null,
                    null,
                    null,
                    -1,
                    -1
                ),
                list("partyId", "firstName")
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("secondInsert", insert, stmt);
            assertEquals("secondInsert:parse", parser(insert).InsertStatement(), parser(stmt).InsertStatement());
        }
        {
            SQLUpdate update = new SQLUpdate(
                new Table(new TableName("Person", null), null),
                list(
                    new SetField("lastName", new MathValue("||", list(new StringValue("auto-"), new FieldValue(null, "partyId"))))
                ),
                new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b")))
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstUpdate", update, stmt);
            assertEquals("firstUpdate:parse", parser(update).UpdateStatement(), parser(stmt).UpdateStatement());
        }
        {
            SQLUpdate update = new SQLUpdate(
                new Table(new TableName("Person", null), null),
                list(
                    new SetField("lastName", new MathValue("||", list(new StringValue("auto-"), new FieldValue(null, "partyId")))),
                    new SetField("height", new NumberValue<Integer>(Integer.valueOf(5))),
                    new SetField("width", new NumberValue<Integer>(Integer.valueOf(7)))
                ),
                new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b")))
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("secondUpdate", update, stmt);
            assertEquals("secondUpdate:parse", parser(update).UpdateStatement(), parser(stmt).UpdateStatement());
        }
        {
            SQLUpdate update = new SQLUpdate(
                new Table(new TableName("Person", null), null),
                list(
                    new SetField("lastName", new MathValue("||", list(new StringValue("auto-"), new FieldValue(null, "partyId")))),
                    new SetField("height", new NumberValue<Integer>(Integer.valueOf(6))),
                    new SetField("width", new NumberValue<Integer>(Integer.valueOf(5))),
                    new SetField("nickname", new StringValue("a"))
                ),
                new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b")))
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("thirdUpdate", update, stmt);
            assertEquals("thirdUpdate:parse", parser(update).UpdateStatement(), parser(stmt).UpdateStatement());
        }
        {
            SQLDelete delete = new SQLDelete(
                new Table(new TableName("Person", null), null),
                new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b")))
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstDelete", delete, stmt);
            assertEquals("firstDelete:parse", parser(delete).DeleteStatement(), parser(stmt).DeleteStatement());
        }
        {
            SQLDelete delete = new SQLDelete(
                new Table(new TableName("Party", null), null),
                new ListCondition(new FieldValue(null, "partyId"), "IN", GenericTestCaseBase.<Value>list(new StringValue("a"), new StringValue("b")))
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("secondDelete", delete, stmt);
            assertEquals("secondDelete:parse", parser(delete).DeleteStatement(), parser(stmt).DeleteStatement());
        }
        {
            SQLView view = new SQLView(
                "viewOne",
                new SQLSelect(
                    false,
                    list(new FieldAll("a", Collections.<String>emptySet())),
                    null,
                    new Table(new TableName("Party", "a"), null),
                    null,
                    null,
                    null,
                    null,
                    null,
                    -1,
                    -1
                )
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstView", view, stmt);
            assertEquals("firstView:parse", parser(view).ViewStatement(), parser(stmt).ViewStatement());
        }
        {
            SQLIndex index = new SQLIndex(
                false,
                "testIndex",
                "Party",
                "btree",
                GenericTestCaseBase.<ConstantValue>list(
                    new FieldValue(null, "partyId")
                )
            );
            SQLStatement<?> stmt = stmtIt.next();
            assertEquals("firstIndex", index, stmt);
        }
        assertFalse("has no more statements", stmtIt.hasNext());
    }
/*
CREATE VIEW viewOne AS SELECT a.* FROM Party a;
*/
}
