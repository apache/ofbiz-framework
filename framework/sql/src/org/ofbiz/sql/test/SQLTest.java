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

import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import junit.framework.TestCase;

import org.ofbiz.sql.Condition;
import org.ofbiz.sql.FieldAll;
import org.ofbiz.sql.FieldDef;
import org.ofbiz.sql.FieldDefFieldValue;
import org.ofbiz.sql.FieldDefValue;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.FunctionCall;
import org.ofbiz.sql.MathValue;
import org.ofbiz.sql.Parser;
import org.ofbiz.sql.SQLSelect;
import org.ofbiz.sql.SQLStatement;
import org.ofbiz.sql.Value;

import org.ofbiz.base.test.GenericTestCaseBase;

public class SQLTest extends GenericTestCaseBase {
    public SQLTest(String name) {
        super(name);
    }

    public void testParse() throws Exception {
        List<SQLStatement<?>> statements = new Parser(getClass().getResourceAsStream("GoodParseAll.sql")).SQLFile();
        for (SQLStatement<?> statement: statements) {
            System.err.println(statement);
        }
        Iterator<SQLStatement<?>> stmtIt = statements.iterator();
        assertTrue("has more statements", stmtIt.hasNext());
        {
            SQLStatement stmt = stmtIt.next();
            assertTrue("is select", stmt instanceof SQLSelect);
            SQLSelect select = (SQLSelect) stmt;
            Iterator<FieldAll> fieldAllIt = select.getFieldAlls().iterator();

            assertTrue("has first field all", fieldAllIt.hasNext());
            FieldAll fieldAll = fieldAllIt.next();
            assertEquals("first fieldAll.alias", "a", fieldAll.getAlias());
            assertEquals("no excludes", GenericTestCaseBase.<FieldAll>set(), set(fieldAll));

            assertTrue("has second field all", fieldAllIt.hasNext());
            fieldAll = fieldAllIt.next();
            assertEquals("first fieldAll.alias", "b", fieldAll.getAlias());
            assertEquals("no excludes", set("partyId"), set(fieldAll));

            assertTrue("has third field all", fieldAllIt.hasNext());
            fieldAll = fieldAllIt.next();
            assertEquals("first fieldAll.alias", "c", fieldAll.getAlias());
            assertEquals("no excludes", set("partyId"), set(fieldAll));

            assertFalse("has no more field all", fieldAllIt.hasNext());

            Iterator<FieldDef> fieldDefIt = select.getFieldDefs().iterator();

            assertTrue("has first field def", fieldDefIt.hasNext());
            FieldDef fieldDef = fieldDefIt.next();
            assertEquals("first fieldDef.alias", "roleTypeId", fieldDef.getAlias());
            assertTrue("first is FieldDefFieldValue", fieldDef instanceof FieldDefFieldValue);
            FieldDefFieldValue fdfv = (FieldDefFieldValue) fieldDef;
            assertEquals("first fieldDef.tableName", "d", fdfv.getFieldValue().getTableName());
            assertEquals("first fieldDef.fieldName", "roleTypeId", fdfv.getFieldValue().getFieldName());
            assertEquals("first fieldDef.defaultName", "roleTypeId", fdfv.getFieldValue().getDefaultName());

            assertTrue("has second field def", fieldDefIt.hasNext());
            fieldDef = fieldDefIt.next();
            assertEquals("second fieldDef.alias", "roleDescription", fieldDef.getAlias());
            assertTrue("second is FieldDefFieldValue", fieldDef instanceof FieldDefFieldValue);
            fdfv = (FieldDefFieldValue) fieldDef;
            assertEquals("second fieldDef.tableName", "d", fdfv.getFieldValue().getTableName());
            assertEquals("second fieldDef.fieldName", "description", fdfv.getFieldValue().getFieldName());
            assertEquals("second fieldDef.defaultName", "description", fdfv.getFieldValue().getDefaultName());

            assertTrue("has third field def", fieldDefIt.hasNext());
            fieldDef = fieldDefIt.next();
            assertEquals("third fieldDef.alias", "SUM", fieldDef.getAlias());
            assertTrue("third is FieldDefValue", fieldDef instanceof FieldDefValue);
            FieldDefValue fdv = (FieldDefValue) fieldDef;
            assertTrue("third fieldDefValue.staticValue is FunctionCall", fdv.getValue() instanceof FunctionCall);
            FunctionCall fc = (FunctionCall) fdv.getValue();
            assertEquals("third arg count", 1, fc.getArgCount());
            Iterator<Value> valueIt = fc.iterator();
            assertTrue("third args hasNext", valueIt.hasNext());
            Value argValue = valueIt.next();
            assertTrue("third first arg is FieldValue", argValue instanceof FieldValue);
            FieldValue fieldValue = (FieldValue) argValue;
            assertEquals("third first arg tableName", "a", fieldValue.getTableName());
            assertEquals("third first arg fieldName", "partyId", fieldValue.getFieldName());
            assertFalse("third no more args", valueIt.hasNext());

            assertTrue("has fourth field def", fieldDefIt.hasNext());
            fieldDef = fieldDefIt.next();
            assertEquals("fourth fieldDef.alias", "baz", fieldDef.getAlias());
            assertTrue("fourth is FieldDefValue", fieldDef instanceof FieldDefValue);
            fdv = (FieldDefValue) fieldDef;

            assertTrue("has fifth field def", fieldDefIt.hasNext());
            fieldDef = fieldDefIt.next();
            assertEquals("fifth fieldDef.alias", "one", fieldDef.getAlias());
            assertTrue("fifth is FieldDefValue", fieldDef instanceof FieldDefValue);
            fdv = (FieldDefValue) fieldDef;

            assertFalse("has no more field def", fieldDefIt.hasNext());

        }
    }
}
