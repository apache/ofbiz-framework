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
package org.ofbiz.base.test;

import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;

public class BaseUnitTests extends TestCase {

    public BaseUnitTests(String name) {
        super(name);
    }

    public void testDebug() {
        Debug.set(Debug.VERBOSE, true);
        assertTrue(Debug.verboseOn());

        Debug.set(Debug.VERBOSE, false);
        assertTrue(!Debug.verboseOn());

        Debug.set(Debug.INFO, true);
        assertTrue(Debug.infoOn());
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
}
