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

import java.util.Collections;
import java.util.List;

import org.ofbiz.sql.BetweenCondition;
import org.ofbiz.sql.BooleanCondition;
import org.ofbiz.sql.Condition;
import org.ofbiz.sql.ConditionList;
import org.ofbiz.sql.Joiner;
import org.ofbiz.sql.ListCondition;
import org.ofbiz.sql.NumberValue;
import org.ofbiz.sql.Value;

import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class ConditionsTest extends GenericTestCaseBase {
    private static final NumberValue<Long> l1 = NumberValue.valueOf(1);
    private static final NumberValue<Long> l2 = NumberValue.valueOf(5);
    private static final NumberValue<Long> l3 = NumberValue.valueOf(10);

    public ConditionsTest(String name) {
        super(name);
    }

    private static <C extends Condition> void basicTest(String label, Class<C> clz, C c, String s, C o, boolean matches) {
        assertEquals(label + ":toString", s, c.toString());
        assertNotEquals(label + ":not-equals-this", c, ConditionsTest.class);
        if (o != null) {
            if (matches) {
                assertEquals(label + ":equals", o, c);
            } else {
                assertNotEquals(label + ":not-equals", o, c);
            }
        }
        ConditionVisitorRecorder visitor = new ConditionVisitorRecorder();
        c.accept(visitor);
        c.accept(visitor);
        assertEquals(label + ":visited", 2, visitor.counts.remove(clz).intValue());
        assertTrue(label + ":nothing-else-visited", visitor.counts.isEmpty());
    }

    private static void betweenConditionTest(String label, BetweenCondition c, Value left, Value r1, Value r2, String s, BetweenCondition o, boolean matches) {
        assertEquals(label + ":left", left, c.getLeft());
        assertEquals(label + ":r1", r1, c.getR1());
        assertEquals(label + ":r2", r2, c.getR2());
        basicTest(label, BetweenCondition.class, c, s, o, matches);
    }

    public void testBetweenCondition() {
        BetweenCondition c1 = new BetweenCondition(l1, l2, l3);
        betweenConditionTest("c1", c1, l1, l2, l3, "1 BETWEEN 5 AND 10", null, false);
        BetweenCondition c2 = new BetweenCondition(l3, l1, l2);
        betweenConditionTest("c2", c2, l3, l1, l2, "10 BETWEEN 1 AND 5", c1, false);
        BetweenCondition c3 = new BetweenCondition(l1, l2, l3);
        betweenConditionTest("c3", c3, l1, l2, l3, "1 BETWEEN 5 AND 10", c1, true);
        BetweenCondition c4 = new BetweenCondition(l1, l3, l2);
        betweenConditionTest("c4", c4, l1, l3, l2, "1 BETWEEN 10 AND 5", c1, false);
        BetweenCondition c5 = new BetweenCondition(l1, l2, l1);
        betweenConditionTest("c5", c5, l1, l2, l1, "1 BETWEEN 5 AND 1", c1, false);
    }

    private static void booleanConditionTest(String label, BooleanCondition c, Value left, String op, Value right, String s, BooleanCondition o, boolean matches) {
        assertEquals(label + ":left", left, c.getLeft());
        assertEquals(label + ":op", op, c.getOp());
        assertEquals(label + ":right", right, c.getRight());
        basicTest(label, BooleanCondition.class, c, s, o, matches);
    }

    public void testBooleanCondition() {
        BooleanCondition c1 = new BooleanCondition(l1, "=", l2);
        booleanConditionTest("c1", c1, l1, "=", l2, "1 = 5", null, false);
        BooleanCondition c2 = new BooleanCondition(l1, "=", l3);
        booleanConditionTest("c2", c2, l1, "=", l3, "1 = 10", c1, false);
        BooleanCondition c3 = new BooleanCondition(l1, "<", l2);
        booleanConditionTest("c3", c3, l1, "<", l2, "1 < 5", c1, false);
        BooleanCondition c4 = new BooleanCondition(l2, ">", l3);
        booleanConditionTest("c4", c4, l2, ">", l3, "5 > 10", c1, false);
        BooleanCondition c5 = new BooleanCondition(l1, "=", l2);
        booleanConditionTest("c5", c5, l1, "=", l2, "1 = 5", c1, true);
    }

    private static void conditionListTest(String label, ConditionList c, Joiner joiner, List<? extends Condition> conditions, String s, ConditionList o, boolean matches) {
        assertEquals(label + ":joiner", joiner, c.getJoiner());
        basicTest(label, ConditionList.class, c, s, o, matches);
        BooleanCondition b = new BooleanCondition(l1, "=", l3);
        c.add(b);
        assertEqualsIterable(label + ":iterable", conditions, Collections.<Condition>emptyList(), false, c, list(b), true);
    }

    public void testConditionList() {
        BooleanCondition b1 = new BooleanCondition(l1, "=", l2);
        BooleanCondition b2 = new BooleanCondition(l2, "=", l3);
        ConditionList c1 = new ConditionList(Joiner.AND, GenericTestCaseBase.<Condition>list(b1));
        conditionListTest("c1", c1, Joiner.AND, list(b1), "( 1 = 5 )", null, false);
        ConditionList c2 = new ConditionList(Joiner.AND, GenericTestCaseBase.<Condition>list(b1, b2));
        conditionListTest("c2", c2, Joiner.AND, list(b1, b2), "( 1 = 5 AND 5 = 10 )", c1, false);
        ConditionList c3 = new ConditionList(Joiner.OR, GenericTestCaseBase.<Condition>list(b2, b1));
        conditionListTest("c3", c3, Joiner.OR, list(b2, b1), "( 5 = 10 OR 1 = 5 )", c1, false);
        ConditionList c4 = new ConditionList(Joiner.AND, GenericTestCaseBase.<Condition>list(b1));
        conditionListTest("c4", c4, Joiner.AND, list(b1), "( 1 = 5 )", c1, true);
    }

    private static void listConditionTest(String label, ListCondition c, Value left, String op, List<? extends Value> values, String s, ListCondition o, boolean matches) {
        assertEquals(label + ":left", left, c.getLeft());
        assertEquals(label + ":op", op, c.getOp());
        assertEquals(label + ":right", values, c.getValues());
        basicTest(label, ListCondition.class, c, s, o, matches);
    }

    @SuppressWarnings("unchecked")
    public void testListCondition() {
        ListCondition c1 = new ListCondition(l1, "IN", GenericTestCaseBase.<Value>list(l2));
        listConditionTest("c1", c1, l1, "in", list(l2), "1 in (5)", null, false);
        ListCondition c2 = new ListCondition(l1, "NOT IN", GenericTestCaseBase.<Value>list(l2));
        listConditionTest("c2", c2, l1, "not in", list(l2), "1 not in (5)", c1, false);
        ListCondition c3 = new ListCondition(l2, "IN", GenericTestCaseBase.<Value>list(l3));
        listConditionTest("c3", c3, l2, "in", list(l3), "5 in (10)", c1, false);
        ListCondition c4 = new ListCondition(l1, "IN", GenericTestCaseBase.<Value>list(l2, l3));
        listConditionTest("c4", c4, l1, "in", list(l2, l3), "1 in (5, 10)", c1, false);
        ListCondition c5 = new ListCondition(l1, "IN", GenericTestCaseBase.<Value>list(l2));
        listConditionTest("c5", c5, l1, "in", list(l2), "1 in (5)", c1, true);
    }

    public static class ConditionVisitorRecorder extends Recorder<Class<? extends Condition>> implements Condition.Visitor {
        public void visit(BetweenCondition condition) {
            record(BetweenCondition.class);
        }

        public void visit(BooleanCondition condition) {
            record(BooleanCondition.class);
        }

        public void visit(ConditionList condition) {
            record(ConditionList.class);
        }

        public void visit(ListCondition condition) {
            record(ListCondition.class);
        }
    }
}
