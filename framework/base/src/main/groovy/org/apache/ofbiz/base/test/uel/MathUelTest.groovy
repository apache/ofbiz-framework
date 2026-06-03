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
import org.apache.ofbiz.service.testtools.OFBizTestCase

/**
 * ./gradlew 'ofbiz -t component=base -t suitename=basetests'
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */

class MathUelTest extends OFBizTestCase {

    MathUelTest(String name) { super(name) }

    void testMathUel() { // codenarc-disable JUnitTestMethodWithoutAssert
        doMathTest('${math:absDouble(a)}', [a: -0.3, b: null], { a, b -> Math.abs(a as double) })
        doMathTest('${math:absFloat(a)}', [a: -0.3, b: null], { a, b -> Math.abs(a as float) })
        doMathTest('${math:absInt(a)}', [a: -2, b: null], { a, b -> Math.abs(a as int) })
        doMathTest('${math:absLong(a)}', [a: -5, b: null], { a, b -> Math.abs(a as long) })
        doMathTest('${math:acos(a)}', [a: 0.2, b: null], { a, b -> Math.acos(a) })
        doMathTest('${math:asin(a)}', [a: 0.2, b: null], { a, b -> Math.asin(a) })
        doMathTest('${math:atan(a)}', [a: 0.2, b: null], { a, b -> Math.atan(a) })
        doMathTest('${math:atan2(a, b)}', [a: 0.2, b: 0.2], { a, b -> Math.atan2(a, b) })
        doMathTest('${math:cbrt(a)}', [a: 10, b: null], { a, b -> Math.cbrt(a) })
        doMathTest('${math:ceil(a)}', [a: 20, b: null], { a, b -> Math.ceil(a) })
        doMathTest('${math:cos(a)}', [a: 0.2, b: null], { a, b -> Math.cos(a) })
        doMathTest('${math:cosh(a)}', [a: 0.2, b: null], { a, b -> Math.cosh(a) })
        doMathTest('${math:exp(a)}', [a: 29, b: null], { a, b -> Math.exp(a) })
        doMathTest('${math:expm1(a)}', [a: 20, b: null], { a, b -> Math.expm1(a) })
        doMathTest('${math:floor(a)}', [a: 23.4, b: null], { a, b -> Math.floor(a) })
        doMathTest('${math:hypot(a, b)}', [a: 29, b: 12], { a, b -> Math.hypot(a, b) })
        doMathTest('${math:IEEEremainder(a, b)}', [a: 12, b: 1.3], { a, b -> Math.IEEEremainder(a, b) })
        doMathTest('${math:log(a)}', [a: 20, b: null], { a, b -> Math.log(a) })
        doMathTest('${math:log10(a)}', [a: 29, b: null], { a, b -> Math.log10(a) })
        doMathTest('${math:log1p(a)}', [a: 12, b: null], { a, b -> Math.log1p(a) })
        doMathTest('${math:maxDouble(a, b)}', [a: 12, b: 13], { a, b -> Math.max(a as double, b as double) })
        doMathTest('${math:maxFloat(a, b)}', [a: 2.4, b: 3.9], { a, b -> Math.max(a as float, b as float) })
        doMathTest('${math:maxInt(a, b)}', [a: 2.6, b: 3.7], { a, b -> Math.max(a as int, b as int) })
        doMathTest('${math:maxLong(a, b)}', [a: 23, b: 32], { a, b -> Math.max(a as long, b as long) })
        doMathTest('${math:minDouble(a, b)}', [a: 10, b: 20], { a, b -> Math.min(a as double, b as double) })
        doMathTest('${math:minFloat(a, b)}', [a: 1.2, b: 2.5], { a, b -> Math.min(a as float, b as float) })
        doMathTest('${math:minInt(a, b)}', [a: 12, b: 13], { a, b -> Math.min(a as int, b as int) })
        doMathTest('${math:minLong(a, b)}', [a: 24, b: 14], { a, b -> Math.min(a as long, b as long) })
        doMathTest('${math:pow(a, b)}', [a: 12, b: 13], { a, b -> Math.pow(a, b) })
        doMathTest('${math:rint(a)}', [a: 29, b: null], { a, b -> Math.rint(a) })
        doMathTest('${math:roundDouble(a)}', [a: 23.4, b: null], { a, b -> Math.round(a as double) })
        doMathTest('${math:roundFloat(a)}', [a: 12.4, b: null], { a, b -> Math.round(a as float) })
        doMathTest('${math:signumDouble(a)}', [a: 23, b: null], { a, b -> Math.signum(a as double) })
        doMathTest('${math:signumFloat(a)}', [a: 23, b: null], { a, b -> Math.signum(a as float) })
        doMathTest('${math:sin(a)}', [a: 12, b: null], { a, b -> Math.sin(a) })
        doMathTest('${math:sinh(a)}', [a: 12, b: null], { a, b -> Math.sinh(a) })
        doMathTest('${math:sqrt(a)}', [a: 34, b: null], { a, b -> Math.sqrt(a) })
        doMathTest('${math:tan(a)}', [a: 13, b: null], { a, b -> Math.tan(a) })
        doMathTest('${math:tanh(a)}', [a: 30, b: null], { a, b -> Math.tanh(a) })
        doMathTest('${math:toDegrees(a)}', [a: 30, b: null], { a, b -> Math.toDegrees(a) })
        doMathTest('${math:toRadians(a)}', [a: 12, b: null], { a, b -> Math.toRadians(a) })
        doMathTest('${math:ulpDouble(a)}', [a: 12, b: null], { a, b -> Math.ulp(a as double) })
        doMathTest('${math:ulpFloat(a)}', [a: 30, b: null], { a, b -> Math.ulp(a as float) })
    }

    void testMathRandom() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${math:random()}')
        assert fse.expand([:]) instanceof Double
        assert fse.expand([:]) != BigDecimal.ZERO
    }

    private void doMathTest(String uelInput, Map context, Closure uelFunction) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert new BigDecimal(fse.expand(context)) == new BigDecimal(uelFunction(context.a, context.b))
    }

}
