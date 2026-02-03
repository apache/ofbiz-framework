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
package org.apache.ofbiz.base.util.string

import org.junit.Test

/* codenarc-disable GStringExpressionWithinString, JUnitTestMethodWithoutAssert */

/**
 * ./gradlew test --tests '*GroovyFlexibleStringExpander*'
 */
class FlexibleStringExpanderParserTests {

    @Test
    void testSimpleParserIntegrity() {
        List<String> stringsToTest = [
                '${\'Hello ${var}\'}!',
                'Hello ${${groovy:' + FlexibleStringExpanderParserTests + '.staticReturnNull()}}World!',
                '${${throwException.value}}',
                '${a${}${}',
                '',
                '${groovy:}',
                '\\${}',
                'a\\${}',
                '\\${groovy:}',
                '${',
                '${a${}',
                '${a${${}',
                '\\${',
                'a\\${',
                '${?currency(',
                '${?currency()',
                '${price?currency(',
                '${price?currency()',
                '${?currency(usd',
                '${?currency(usd)',
                '${price?currency(usd',
                '${price?currency(usd)',
                '${?currency(}',
                '${?currency()}',
                '${?currency(usd}',
                '${?currency(usd)}',
                '${price?currency(}',
                '${price?currency()}',
                '${price?currency(usd}',
                '${price?currency(usd)}',
                'a${price?currency(usd)}b']
        for (String stringToTest in stringsToTest) {
            doParserIntegrityTest(stringToTest)
        }
    }

    @Test
    void testParserIntegrityWithNullInput() {
        doParserIntegrityTest(null, '')
    }

    @Test
    void testParserIntegrityWithSingleCharInput() {
        doParserIntegrityTest('a', 'a', false)
    }

    private static void doParserIntegrityTest(String input, String toString = input, boolean checkCache = true) {
        assert toString == FlexibleStringExpander.getInstance(input, false).toString()
        assert toString == FlexibleStringExpander.getInstance(input, true).toString()
        if (checkCache) {
            assert FlexibleStringExpander.getInstance(input, true) === FlexibleStringExpander.getInstance(input, true)
        }
    }

}
