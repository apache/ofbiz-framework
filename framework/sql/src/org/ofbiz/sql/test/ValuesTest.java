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

import java.util.List;
import org.ofbiz.sql.ConstantValue;
import org.ofbiz.sql.CountAllFunction;
import org.ofbiz.sql.AggregateFunction;
import org.ofbiz.sql.FieldValue;
import org.ofbiz.sql.FunctionCall;
import org.ofbiz.sql.MathValue;
import org.ofbiz.sql.NumberValue;
import org.ofbiz.sql.ParameterValue;
import org.ofbiz.sql.StaticValue;
import org.ofbiz.sql.StringValue;
import org.ofbiz.sql.Value;

import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class ValuesTest extends GenericTestCaseBase {
    private static final FieldValue fv1 = new FieldValue("partyId");
    private static final FieldValue fv2 = new FieldValue("a", "partyId");
    private static final FieldValue fv3 = new FieldValue(null, "partyId");
    private static final FieldValue fv4 = new FieldValue(null, "firstName");
    private static final NumberValue<Long> nv1 = NumberValue.valueOf(1);
    private static final NumberValue<Double> nv2 = NumberValue.valueOf(2D);
    private static final NumberValue<Long> nv3 = NumberValue.valueOf(3);

    public ValuesTest(String name) {
        super(name);
    }

    private static <V extends Value> void basicTest(String label, Class<V> clz, V v, String defaultName, String s, V o, boolean matches) {
        if (v instanceof StaticValue) {
            assertEquals(label + ":default-name", defaultName, ((StaticValue) v).getDefaultName());
        }
        assertEquals(label + ":toString", s, v.toString());
        assertNotEquals(label + ":not-equals-this", v, ValuesTest.class);
        if (o != null) {
            if (matches) {
                assertEquals(label + ":equals", o, v);
            } else {
                assertNotEquals(label + ":not-equals", o, v);
            }
        }
        ValueVisitorRecorder visitor = new ValueVisitorRecorder();
        v.accept(visitor);
        v.accept(visitor);
        assertEquals(label + ":visited", 2, visitor.counts.remove(clz).intValue());
        assertTrue(label + ":nothing-else-visited", visitor.counts.isEmpty());
    }

    private static void countAllFunctionTest(String label, CountAllFunction v, String tableName, String s, CountAllFunction o, boolean matches) {
        assertEquals(label + ":left", tableName, v.getTableName());
        basicTest(label, CountAllFunction.class, v, "COUNT", s, o, matches);
    }

    public void testCountAllFunction() {
        CountAllFunction v1 = new CountAllFunction("a");
        countAllFunctionTest("v1", v1, "a", "COUNT(a.*)", null, false);
        CountAllFunction v2 = new CountAllFunction(null);
        countAllFunctionTest("v2", v2, null, "COUNT(*)", v1, false);
        CountAllFunction v3 = new CountAllFunction("a");
        countAllFunctionTest("v3", v3, "a", "COUNT(a.*)", v1, true);
    }

    private static void aggregateFunctionTest(String label, AggregateFunction v, String name, boolean isDistinct, FieldValue fv, String s, AggregateFunction o, boolean matches) {
        assertEquals(label + ":name", name, v.getName());
        assertEquals(label + ":left", isDistinct, v.isDistinct());
        assertEquals(label + ":field-value", fv, v.getValue());
        basicTest(label, AggregateFunction.class, v, name, s, o, matches);
    }

    public void testAggregateFunction() {
        AggregateFunction v1 = new AggregateFunction("COUNT", false, fv2);
        aggregateFunctionTest("v1", v1, "COUNT", false, fv2, "COUNT(a.partyId)", null, false);
        AggregateFunction v2 = new AggregateFunction("COUNT", true, fv2);
        aggregateFunctionTest("v2", v2, "COUNT", true, fv2, "COUNT(DISTINCT a.partyId)", v1, false);
        AggregateFunction v3 = new AggregateFunction("COUNT", true, fv1);
        aggregateFunctionTest("v3", v3, "COUNT", true, fv1, "COUNT(DISTINCT partyId)", v1, false);
        AggregateFunction v4 = new AggregateFunction("COUNT", false, fv1);
        aggregateFunctionTest("v4", v4, "COUNT", false, fv1, "COUNT(partyId)", v1, false);
        AggregateFunction v5 = new AggregateFunction("MAX", false, fv2);
        aggregateFunctionTest("v5", v5, "MAX", false, fv2, "MAX(a.partyId)", v1, false);
        AggregateFunction v6 = new AggregateFunction("COUNT", false, fv2);
        aggregateFunctionTest("v6", v6, "COUNT", false, fv2, "COUNT(a.partyId)", v1, true);
    }

    private static void fieldValueTest(String label, FieldValue v, String tableName, String fieldName, String s, FieldValue o, boolean matches) {
        assertEquals(label + ":table-name", tableName, v.getTableName());
        assertEquals(label + ":field-name", fieldName, v.getFieldName());
        basicTest(label, FieldValue.class, v, fieldName, s, o, matches);
    }

    public void testFieldValue() {
        fieldValueTest("fv1", fv1, null, "partyId", "partyId", null, false);
        fieldValueTest("fv2", fv2, "a", "partyId", "a.partyId", fv1, false);
        fieldValueTest("fv3", fv3, null, "partyId", "partyId", fv1, true);
        fieldValueTest("fv4", fv4, null, "firstName", "firstName", fv1, false);
    }

    private static void functionCallTest(String label, FunctionCall v, String name, List<? extends Value> values, String s, FunctionCall o, boolean matches) {
        assertEquals(label + ":name", name, v.getName());
        assertEquals(label + ":arg-count", values.size(), v.getArgCount());
        assertEqualsIterable(label, values, v);
        basicTest(label, FunctionCall.class, v, name, s, o, matches);
    }

    @SuppressWarnings("unchecked")
    public void testFunctionCall() {
        FunctionCall v1 = new FunctionCall("LENGTH", GenericTestCaseBase.<Value>list(nv1, nv2));
        functionCallTest("v1", v1, "LENGTH", list(nv1, nv2), "LENGTH(1, 2.0)", null, false);
        FunctionCall v2 = new FunctionCall("LENGTH", GenericTestCaseBase.<Value>list(nv1, nv3));
        functionCallTest("v2", v2, "LENGTH", list(nv1, nv3), "LENGTH(1, 3)", v1, false);
        FunctionCall v3 = new FunctionCall("LENGTH", GenericTestCaseBase.<Value>list(nv1, nv2));
        functionCallTest("v3", v3, "LENGTH", list(nv1, nv2), "LENGTH(1, 2.0)", v1, true);
        FunctionCall v4 = new FunctionCall("TRIM", GenericTestCaseBase.<Value>list(nv1, nv2));
        functionCallTest("v4", v4, "TRIM", list(nv1, nv2), "TRIM(1, 2.0)", v1, false);
    }

    private static void mathValueTest(String label, MathValue v, String op, List<? extends ConstantValue> values, String s, MathValue o, boolean matches) {
        assertEquals(label + ":op", op, v.getOp());
        assertEqualsIterable(label, values, v);
        basicTest(label, MathValue.class, v, null, s, o, matches);
    }

    @SuppressWarnings("unchecked")
    public void testMathValue() {
        MathValue v1 = new MathValue("+", GenericTestCaseBase.<ConstantValue>list(nv1, nv2));
        mathValueTest("v1", v1, "+", list(nv1, nv2), "(1 + 2.0)", null, false);
        MathValue v2 = new MathValue("+", GenericTestCaseBase.<ConstantValue>list(nv1, nv3));
        mathValueTest("v2", v2, "+", list(nv1, nv3), "(1 + 3)", v1, false);
        MathValue v3 = new MathValue("+", GenericTestCaseBase.<ConstantValue>list(nv1, nv2));
        mathValueTest("v3", v3, "+", list(nv1, nv2), "(1 + 2.0)", v1, true);
        MathValue v4 = new MathValue("-", GenericTestCaseBase.<ConstantValue>list(nv1, nv3));
        mathValueTest("v4", v4, "-", list(nv1, nv3), "(1 - 3)", v1, false);
    }

    public void testNull() {
        basicTest("null", Value.Null.class, Value.NULL, null, "NULL", null, false);
    }

    private static <N extends Number> void numberValueTest(String label, NumberValue<N> v, N n, String s, NumberValue<?> o, boolean matches) {
        assertEquals(label + ":number", n, v.getNumber());
        basicTest(label, NumberValue.class, v, null, s, o, matches);
    }

    public void testNumberValue() {
        numberValueTest("nv1", nv1, Long.valueOf(1), "1", null, false);
        numberValueTest("nv2", nv2, Double.valueOf(2), "2.0", nv1, false);
        NumberValue<Long> nv3 = NumberValue.valueOf(1);
        numberValueTest("nv3", nv3, Long.valueOf(1), "1", nv1, true);
    }

    private static void parameterValueTest(String label, ParameterValue v, String name, String s, ParameterValue o, boolean matches) {
        assertEquals(label + ":name", name, v.getName());
        basicTest(label, ParameterValue.class, v, null, s, o, matches);
    }

    public void testParameterValue() {
        ParameterValue v1 = new ParameterValue("a");
        parameterValueTest("v1", v1, "a", "?a", null, false);
        ParameterValue v2 = new ParameterValue("b");
        parameterValueTest("v2", v2, "b", "?b", v1, false);
        ParameterValue v3 = new ParameterValue("a");
        parameterValueTest("v3", v3, "a", "?a", v1, true);
    }

    private static void stringValueTest(String label, StringValue v, String string, String s, StringValue o, boolean matches) {
        assertEquals(label + ":name", string, v.getString());
        basicTest(label, StringValue.class, v, null, s, o, matches);
    }

    public void testStringValue() {
        StringValue v1 = new StringValue("foo");
        stringValueTest("v1", v1, "foo", "'foo'", null, false);
        StringValue v2 = new StringValue("b'r");
        stringValueTest("v2", v2, "b'r", "'b''r'", v1, false);
        StringValue v3 = new StringValue("foo");
        stringValueTest("v3", v3, "foo", "'foo'", v1, true);
    }

    public static class ValueVisitorRecorder extends Recorder<Class<? extends Value>> implements Value.Visitor {

        public void visit(AggregateFunction value) {
            record(AggregateFunction.class);
        }

        public void visit(FieldValue value) {
            record(FieldValue.class);
        }

        public void visit(FunctionCall value) {
            record(FunctionCall.class);
        }

        public void visit(MathValue value) {
            record(MathValue.class);
        }

        public void visit(Value.Null value) {
            record(Value.Null.class);
        }

        public void visit(NumberValue<?> value) {
            record(NumberValue.class);
        }

        public void visit(ParameterValue value) {
            record(ParameterValue.class);
        }

        public void visit(StringValue value) {
            record(StringValue.class);
        }

        public void visit(CountAllFunction value) {
            record(CountAllFunction.class);
        }
    }
}
