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
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.service.testtools.OFBizTestCase

/**
 * ./gradlew 'ofbiz -t component=base -t suitename=basetests'
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */

class StringUelTest extends OFBizTestCase {

    StringUelTest(String name) { super(name) }

    void testUelString() { // codenarc-disable JUnitTestMethodWithoutAssert
        doStringTest('${str:endsWith(a, b)}', [a: 'dog', b: 'og', c: null],
                { a, b, c -> UelFunctions.endsWith(a, b) })
        doStringTest('${str:indexOf(a, b)}', [a: 'dog', b: 'og', c: null],
                { a, b, c -> UelFunctions.indexOf(a, b) })
        doStringTest('${str:lastIndexOf(a, b)}', [a: 'dog', b: 'og', c: null],
                { a, b, c -> UelFunctions.lastIndexOf(a, b) })
        doStringTest('${str:length(a)}', [a: 'dog', b: null, c: null],
                { a, b, c -> UelFunctions.length(a) })
        doStringTest('${str:replace(a, b, c)}', [a: 'the dog', b: 'dog', c: 'cat'],
                { a, b, c -> UelFunctions.replace(a, b, c) })
        doStringTest('${str:replaceAll(a, b, c)}', [a: 'the dog', b: 'dog', c: 'cat'],
                { a, b, c -> UelFunctions.replaceAll(a, b, c) })
        doStringTest('${str:replaceFirst(a, b, c)}', [a: 'the dog', b: 'dog', c: 'cat'],
                { a, b, c -> UelFunctions.replaceFirst(a, b, c) })
        doStringTest('${str:startsWith(a, b)}', [a: 'the dog', b: 'the', c: null],
                { a, b, c -> UelFunctions.startsWith(a, b) })
        doStringTest('${str:endstring(a, b)}', [a: 'the dog', b: 3, c: null],
                { a, b, c -> UelFunctions.endString(a, b) })
        doStringTest('${str:substring(a, b, c)}', [a: 'the dog', b: 3, c: 7],
                { a, b, c -> UelFunctions.subString(a, b, c) })
        doStringTest('${str:toString(a)}', [a: 'foo', b: null, c: null],
                { a, b, c -> UelFunctions.toString(a) })
        doStringTest('${str:toLowerCase(a)}', [a: 'FOO', b: null, c: null],
                { a, b, c -> UelFunctions.toLowerCase(a) })
        doStringTest('${str:toUpperCase(a)}', [a: 'foo', b: null, c: null],
                { a, b, c -> UelFunctions.toUpperCase(a) })
        doStringTest('${str:trim(a)}', [a: ' foo ', b: null, c: null],
                { a, b, c -> UelFunctions.trim(a) })
    }

    private void doStringTest(String uelInput, Map context, Closure uelFunction) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert fse.expand(context) == uelFunction(context.a, context.b, context.c)
    }

}
