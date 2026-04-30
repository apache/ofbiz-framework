/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License') you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.util.collection

import junit.framework.TestCase
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor
import org.apache.ofbiz.base.util.string.tool.TestException
import org.apache.ofbiz.base.util.string.tool.TestingMap
import org.junit.Assert
import org.junit.Test

/* codenarc-disable GStringExpressionWithinString, UnnecessaryBigDecimalInstantiation */

class FlexibleMapAccessorTest extends TestCase {

    private static final Locale LOCALE_TO_TEST = new Locale('en', 'US')
    private static final FlexibleMapAccessor<?> EMPTY_FMA = FlexibleMapAccessor.getInstance('')
    private static final FlexibleMapAccessor<?> NULL_FMA = FlexibleMapAccessor.getInstance(null)

    @Test
    void testNestedExpressionDetection() {
        assert FlexibleMapAccessor.getInstance('Hello ${parameters.var}!').containsNestedExpression()
        assert !FlexibleMapAccessor.getInstance('Hello World!').containsNestedExpression()
    }

    @Test
    void testBasis() {
        List stringsToTest = ['parameters.var',
                              'parameters.someList[0]',
                              'para${\'meter\'}s.var',
                              'The total is ${total?currency(USD)}.']
        for (stringToTest in stringsToTest) {
            FlexibleMapAccessor fmaInstance = FlexibleMapAccessor.getInstance(stringToTest)
            assert stringToTest == fmaInstance.getOriginalName()
            assert stringToTest == fmaInstance.toString()
            assert !fmaInstance.isEmpty()
            assert fmaInstance === FlexibleMapAccessor.getInstance(stringToTest)
            assert EMPTY_FMA != fmaInstance
            assert NULL_FMA != fmaInstance
        }
    }

    @Test
    void testAscendingTest() {
        String fmaInput = 'parameters.var'
        FlexibleMapAccessor ascendingFma = FlexibleMapAccessor.getInstance('+' + fmaInput)
        assert '+' + fmaInput == ascendingFma.toString()
        assert ascendingFma.getIsAscending()
        FlexibleMapAccessor descendingFma = FlexibleMapAccessor.getInstance('-' + fmaInput)
        assert '-' + fmaInput == descendingFma.toString()
        assert !descendingFma.getIsAscending()
    }

    @Test
    void testFmaPutCRUD() {
        String fmaInput = 'parameters.var'
        Object var = 'Foo'
        Map map = [:]
        FlexibleMapAccessor fmaInstance = FlexibleMapAccessor.getInstance(fmaInput)
        assert fmaInstance.get(map) == null
        fmaInstance.put(map, var)
        assert !map.isEmpty()
        assert var == fmaInstance.get(map)
        assert var == fmaInstance.remove(map)
        assert fmaInstance.remove(map) == null
    }

    @Test
    void testFmaWithLocale() {
        String fmaInput = '\'The total is ${total?currency(USD)}.\''
        String output = 'The total is $12,345,678.90.'
        Object variable = new BigDecimal('12345678.90')
        Map map = [:]
        FlexibleMapAccessor fmaInstance = FlexibleMapAccessor.getInstance(fmaInput)
        FlexibleMapAccessor fmaVarInstance = FlexibleMapAccessor.getInstance('total')
        fmaVarInstance.put(map, variable)
        assert !map.isEmpty()
        assert output == fmaInstance.get(map, LOCALE_TO_TEST)
        assert output == fmaInstance.get(map, null)
        // TODO BUG: fmaGet modifies testMap, even tho it shouldn't ?
        // Is this bug still relevant ?
    }

    @Test
    void testAutoVivify() {
        String fmaInput = 'parameters.var'
        Map map = [:]
        FlexibleMapAccessor fmaVarInstance = FlexibleMapAccessor.getInstance(fmaInput)
        fmaVarInstance.put(map, null)
        Map outParameters = map.parameters
        assert !outParameters.isEmpty()
        assert outParameters.keySet().contains('var')
        assert outParameters.('var') === null
        Assert.assertThrows(IllegalArgumentException, () -> fmaVarInstance.put(null, 'Foo'))
    }

    @Test
    void testAutoVivifyList() {
        String fmaInput = 'parameters.someList[0]'
        Map map = [:]
        FlexibleMapAccessor fmaVarInstance = FlexibleMapAccessor.getInstance(fmaInput)
        fmaVarInstance.put(map, null)
        Map parameters = map.parameters
        assert !parameters.isEmpty()
        assert parameters.keySet().contains('someList')
        assert parameters.('someList') == []
        Assert.assertThrows(IllegalArgumentException, () -> fmaVarInstance.put(null, 'Foo'))
    }

    @Test
    void testFmaAndExceptions() {
        Map map = new TestingMap()
        map.put('exception', new TestException())
        Object result = FlexibleMapAccessor.getInstance('exception.value').get(map)
        assert result == null
        FlexibleMapAccessor.getInstance('exception.value').put(map, 'Foo')
        FlexibleMapAccessor.getInstance('exception').remove(map)
        assert null != map.get('exception')
    }

// codenarc-disable JUnitTestMethodWithoutAssert
    @Test
    void testEmptyOrNullFma() {
        doEmptyFmaTest('')
        doEmptyFmaTest(null)
        doEmptyFmaTest('null')
    }
// codenarc-enable JUnitTestMethodWithoutAssert

    private static void doEmptyFmaTest(String text) {
        FlexibleMapAccessor fma = FlexibleMapAccessor.getInstance(text)
        assert fma.isEmpty()
        Map testMap = [:]
        assert fma.get(null) == null
        assert fma.get(testMap) == null
        assert testMap.isEmpty()
        fma.put(testMap, FlexibleMapAccessorTest)
        assert testMap.isEmpty()
        fma.put(null, FlexibleMapAccessorTest)
        assert testMap.isEmpty()
        assert NULL_FMA === fma
        assert EMPTY_FMA === fma
        assert fma.getOriginalName() == ''
        assert fma.remove(testMap) == null
        assert fma.toString() != null
    }

}
