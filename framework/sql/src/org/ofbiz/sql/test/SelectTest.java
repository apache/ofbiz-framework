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
import java.util.Set;

import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.sql.ConstantValue;
import org.ofbiz.sql.FieldAll;
import org.ofbiz.sql.FieldDef;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.FunctionCall;
import org.ofbiz.sql.OrderByItem;
import org.ofbiz.sql.Parser;
import org.ofbiz.sql.StaticValue;
import org.ofbiz.sql.Value;

@SourceMonitored
public class SelectTest extends GenericTestCaseBase {
    private static final FieldValue fv1 = new FieldValue("partyId");
    private static final FieldValue fv4 = new FieldValue(null, "firstName");
    private static final FunctionCall fc1 = new FunctionCall("LOWER", GenericTestCaseBase.<Value>list(fv1));

    public SelectTest(String name) {
        super(name);
    }

    private static Parser parser(Object v) {
        return new Parser(new StringReader(v.toString()));
    }

    private static <V> void basicTest(String label, Class<V> clz, V v, V o, boolean matches) {
        assertNotEquals(label + ":not-equals-this", v, SelectTest.class);
        if (o != null) {
            if (matches) {
                assertEquals(label + ":equals", o, v);
            } else {
                assertNotEquals(label + ":not-equals", o, v);
            }
        }
    }

    private static void fieldAllTest(String label, FieldAll v, String alias, Set<String> exclude, FieldAll o, boolean matches) throws Exception {
        assertEquals(label + ":alias", alias, v.getAlias());
        assertEquals(label + ":exclude", exclude, set(v));
        assertEquals(label + ":parse", v, parser(v).parse_FieldAll());
        basicTest(label, FieldAll.class, v, o, matches);
    }

    public void testFieldAll() throws Exception {
        FieldAll v1 = new FieldAll("a", set("a", "b"));
        fieldAllTest("v1", v1, "a", set("a", "b"), null, false);
        FieldAll v2 = new FieldAll("b", set("a", "b"));
        fieldAllTest("v2", v2, "b", set("a", "b"), v1, false);
        FieldAll v3 = new FieldAll("a", set("b", "c"));
        fieldAllTest("v3", v3, "a", set("b", "c"), v1, false);
        FieldAll v4 = new FieldAll("a", Collections.<String>emptySet());
        fieldAllTest("v4", v4, "a", Collections.<String>emptySet(), v1, false);
        FieldAll v5 = new FieldAll("a", set("b", "a"));
        fieldAllTest("v5", v5, "a", set("b", "a"), v1, true);
    }

    private static void fieldDefTest(String label, FieldDef v, StaticValue value, String alias, String defaultName, FieldDef o, boolean matches) throws Exception {
        assertEquals(label + ":value", value, v.getValue());
        assertEquals(label + ":alias", alias, v.getAlias());
        assertEquals(label + ":default-name", defaultName, v.getDefaultName());
        assertEquals(label + ":parse", v, parser(v).parse_FieldDef());
        basicTest(label, FieldDef.class, v, o, matches);
    }

    public void testFieldDef() throws Exception {
        FieldDef v1 = new FieldDef(fv1, null);
        fieldDefTest("v1", v1, fv1, null, "partyId", null, false);
        FieldDef v2 = new FieldDef(fv1, "partyId");
        fieldDefTest("v2", v2, fv1, "partyId", "partyId", v1, false);
        FieldDef v3 = new FieldDef(fv4, null);
        fieldDefTest("v3", v3, fv4, null, "firstName", v1, false);
        FieldDef v4 = new FieldDef(fv4, "partyId");
        fieldDefTest("v4", v4, fv4, "partyId", "partyId", v1, false);
        FieldDef v5 = new FieldDef(fv1, null);
        fieldDefTest("v5", v5, fv1, null, "partyId", v1, true);
    }

    private static void orderByItemTest(String label, OrderByItem v, OrderByItem.Order order, OrderByItem.Nulls nulls, ConstantValue value, OrderByItem o, boolean matches) throws Exception {
        assertEquals(label + ":order", order, v.getOrder());
        assertEquals(label + ":nulls", nulls, v.getNulls());
        assertEquals(label + ":value", value, v.getValue());
        assertEquals(label + ":parse", v, parser(v).parse_OrderByItem());
        basicTest(label, OrderByItem.class, v, o, matches);
    }

    public void testOrderByItem() throws Exception {
        OrderByItem v1 = new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fv1);
        orderByItemTest("v1", v1, OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fv1, null, false);
        OrderByItem v2 = new OrderByItem(OrderByItem.Order.ASCENDING, OrderByItem.Nulls.FIRST, fv1);
        orderByItemTest("v3", v2, OrderByItem.Order.ASCENDING, OrderByItem.Nulls.FIRST, fv1, v1, false);
        OrderByItem v3 = new OrderByItem(OrderByItem.Order.DESCENDING, OrderByItem.Nulls.LAST, fv1);
        orderByItemTest("v2", v3, OrderByItem.Order.DESCENDING, OrderByItem.Nulls.LAST, fv1, v1, false);
        OrderByItem v4 = new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fc1);
        orderByItemTest("v4", v4, OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fc1, v1, false);
        OrderByItem v5 = new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fv4);
        orderByItemTest("v5", v5, OrderByItem.Order.DEFAULT, OrderByItem.Nulls.DEFAULT, fv4, v1, false);
        OrderByItem v6 = new OrderByItem(OrderByItem.Order.DEFAULT, OrderByItem.Nulls.LAST, fv4);
        orderByItemTest("v6", v6, OrderByItem.Order.DEFAULT, OrderByItem.Nulls.LAST, fv4, v1, false);
    }
}
