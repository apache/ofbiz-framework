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
package org.apache.ofbiz.base.test;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilValidate;

import junit.framework.TestCase;

public class BaseUnitTests extends TestCase {

    public BaseUnitTests(String name) {
        super(name);
    }

    public void testDebug() {
        boolean debugVerbose = Debug.get(Debug.VERBOSE);
        boolean debugInfo = Debug.get(Debug.INFO);
        try {
            Debug.set(Debug.VERBOSE, true);
            assertTrue(Debug.verboseOn());
            Debug.set(Debug.VERBOSE, false);
            assertFalse(Debug.verboseOn());
            Debug.set(Debug.INFO, true);
            assertTrue(Debug.infoOn());
        } finally {
            Debug.set(Debug.VERBOSE, debugVerbose);
            Debug.set(Debug.INFO, debugInfo);
        }
    }

    public void testFormatPrintableCreditCard_1() {
        assertEquals("test 4111111111111111 to ************111",
                "************1111",
                UtilFormatOut.formatPrintableCreditCard("4111111111111111"));
    }

    public void testFormatPrintableCreditCard_2() {
        assertEquals("test 4111 to 4111",
                "4111",
                UtilFormatOut.formatPrintableCreditCard("4111"));
    }

    public void testFormatPrintableCreditCard_3() {
        assertEquals("test null to null",
                null,
                UtilFormatOut.formatPrintableCreditCard(null));
    }
    public void testIsDouble_1() {
        assertFalse(UtilValidate.isDouble("10.0", true, true, 2, 2));
    }
    public void testIsFloat_1() {
        assertFalse(UtilValidate.isFloat("10.0", true, true, 2, 2));
    }
    public void testIsDouble_2() {
        assertTrue(UtilValidate.isDouble("10.000", true, true, 3, 3));
    }
    public void testIsFloat_2() {
        assertTrue(UtilValidate.isFloat("10.000", true, true, 3, 3));
    }

    public void testStringUtil() {
        byte[] testArray = {-1};
        byte[] result = StringUtil.fromHexString(StringUtil.toHexString(testArray));
        assertEquals("Hex conversions", testArray[0], result[0]);
    }

}
