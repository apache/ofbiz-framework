/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.base.test.uel

import org.apache.ofbiz.base.util.string.FlexibleStringExpander

import java.util.stream.Stream

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * ./gradlew test --tests "org.apache.ofbiz.base.test.uel.MathUelTest"
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */
class MathUelTest {

    @BeforeAll
    static void loadUelFunctions() {
        UelTestSupport.ensureUelFunctionsLoaded()
    }

    @ParameterizedTest(name = '{0}')
    @MethodSource('mathUelExpressions')
    void mathUelExpressionMatchesExpectedValue(String uelInput, Map context,
            Closure<?> expectedFunction) { // codenarc-disable JUnitTestMethodWithoutAssert
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert new BigDecimal(fse.expand(context)) == new BigDecimal(expectedFunction(context.a, context.b))
    }

    @Test
    void mathRandomUelReturnsNonZeroDouble() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${math:random()}')
        assert fse.expand([:]) instanceof Double
        assert fse.expand([:]) != BigDecimal.ZERO
    }

    @SuppressWarnings('UnusedPrivateMethod')
    private static Stream<Arguments> mathUelExpressions() {
        Stream.of(
            Arguments.of('${math:absDouble(a)}', [a: -0.3, b: null], { a, b -> Math.abs(a as double) }),
            Arguments.of('${math:absFloat(a)}', [a: -0.3, b: null], { a, b -> Math.abs(a as float) }),
            Arguments.of('${math:absInt(a)}', [a: -2, b: null], { a, b -> Math.abs(a as int) }),
            Arguments.of('${math:absLong(a)}', [a: -5, b: null], { a, b -> Math.abs(a as long) }),
            Arguments.of('${math:acos(a)}', [a: 0.2, b: null], { a, b -> Math.acos(a) }),
            Arguments.of('${math:asin(a)}', [a: 0.2, b: null], { a, b -> Math.asin(a) }),
            Arguments.of('${math:atan(a)}', [a: 0.2, b: null], { a, b -> Math.atan(a) }),
            Arguments.of('${math:atan2(a, b)}', [a: 0.2, b: 0.2], { a, b -> Math.atan2(a, b) }),
            Arguments.of('${math:cbrt(a)}', [a: 10, b: null], { a, b -> Math.cbrt(a) }),
            Arguments.of('${math:ceil(a)}', [a: 20, b: null], { a, b -> Math.ceil(a) }),
            Arguments.of('${math:cos(a)}', [a: 0.2, b: null], { a, b -> Math.cos(a) }),
            Arguments.of('${math:cosh(a)}', [a: 0.2, b: null], { a, b -> Math.cosh(a) }),
            Arguments.of('${math:exp(a)}', [a: 29, b: null], { a, b -> Math.exp(a) }),
            Arguments.of('${math:expm1(a)}', [a: 20, b: null], { a, b -> Math.expm1(a) }),
            Arguments.of('${math:floor(a)}', [a: 23.4, b: null], { a, b -> Math.floor(a) }),
            Arguments.of('${math:hypot(a, b)}', [a: 29, b: 12], { a, b -> Math.hypot(a, b) }),
            Arguments.of('${math:IEEEremainder(a, b)}', [a: 12, b: 1.3], { a, b -> Math.IEEEremainder(a, b) }),
            Arguments.of('${math:log(a)}', [a: 20, b: null], { a, b -> Math.log(a) }),
            Arguments.of('${math:log10(a)}', [a: 29, b: null], { a, b -> Math.log10(a) }),
            Arguments.of('${math:log1p(a)}', [a: 12, b: null], { a, b -> Math.log1p(a) }),
            Arguments.of('${math:maxDouble(a, b)}', [a: 12, b: 13], { a, b -> Math.max(a as double, b as double) }),
            Arguments.of('${math:maxFloat(a, b)}', [a: 2.4, b: 3.9], { a, b -> Math.max(a as float, b as float) }),
            Arguments.of('${math:maxInt(a, b)}', [a: 2.6, b: 3.7], { a, b -> Math.max(a as int, b as int) }),
            Arguments.of('${math:maxLong(a, b)}', [a: 23, b: 32], { a, b -> Math.max(a as long, b as long) }),
            Arguments.of('${math:minDouble(a, b)}', [a: 10, b: 20], { a, b -> Math.min(a as double, b as double) }),
            Arguments.of('${math:minFloat(a, b)}', [a: 1.2, b: 2.5], { a, b -> Math.min(a as float, b as float) }),
            Arguments.of('${math:minInt(a, b)}', [a: 12, b: 13], { a, b -> Math.min(a as int, b as int) }),
            Arguments.of('${math:minLong(a, b)}', [a: 24, b: 14], { a, b -> Math.min(a as long, b as long) }),
            Arguments.of('${math:pow(a, b)}', [a: 12, b: 13], { a, b -> Math.pow(a, b) }),
            Arguments.of('${math:rint(a)}', [a: 29, b: null], { a, b -> Math.rint(a) }),
            Arguments.of('${math:roundDouble(a)}', [a: 23.4, b: null], { a, b -> Math.round(a as double) }),
            Arguments.of('${math:roundFloat(a)}', [a: 12.4, b: null], { a, b -> Math.round(a as float) }),
            Arguments.of('${math:signumDouble(a)}', [a: 23, b: null], { a, b -> Math.signum(a as double) }),
            Arguments.of('${math:signumFloat(a)}', [a: 23, b: null], { a, b -> Math.signum(a as float) }),
            Arguments.of('${math:sin(a)}', [a: 12, b: null], { a, b -> Math.sin(a) }),
            Arguments.of('${math:sinh(a)}', [a: 12, b: null], { a, b -> Math.sinh(a) }),
            Arguments.of('${math:sqrt(a)}', [a: 34, b: null], { a, b -> Math.sqrt(a) }),
            Arguments.of('${math:tan(a)}', [a: 13, b: null], { a, b -> Math.tan(a) }),
            Arguments.of('${math:tanh(a)}', [a: 30, b: null], { a, b -> Math.tanh(a) }),
            Arguments.of('${math:toDegrees(a)}', [a: 30, b: null], { a, b -> Math.toDegrees(a) }),
            Arguments.of('${math:toRadians(a)}', [a: 12, b: null], { a, b -> Math.toRadians(a) }),
            Arguments.of('${math:ulpDouble(a)}', [a: 12, b: null], { a, b -> Math.ulp(a as double) }),
            Arguments.of('${math:ulpFloat(a)}', [a: 30, b: null], { a, b -> Math.ulp(a as float) })
        )
    }

}
