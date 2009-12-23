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

import org.ofbiz.base.conversion.*;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilValidate;

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

    public void testDateTimeConverters() {
        // Source class = java.util.Date
        java.util.Date utilDate = new java.util.Date();
        long dateMillis = utilDate.getTime();
        Converter<java.util.Date, Long> dateToLong = new DateTimeConverters.DateToLong();
        try {
            Long target = dateToLong.convert(utilDate);
            assertEquals("DateToLong", dateMillis, target.longValue());
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        Converter<java.util.Date, java.sql.Date> dateToSqlDate = new DateTimeConverters.DateToSqlDate();
        try {
            java.sql.Date target = dateToSqlDate.convert(utilDate);
            assertEquals("DateToSqlDate", dateMillis, target.getTime());
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        Converter<java.util.Date, String> dateToString = new DateTimeConverters.DateToString();
        try {
            String target = dateToString.convert(utilDate);
            assertEquals("DateToString", utilDate.toString(), target);
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        Converter<java.util.Date, java.sql.Timestamp> dateToTimestamp = new DateTimeConverters.DateToTimestamp();
        try {
            java.sql.Timestamp timestamp = dateToTimestamp.convert(utilDate);
            assertEquals("DateToTimestamp", dateMillis, timestamp.getTime());
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        // Source class = java.sql.Date
        java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
        Converter<java.sql.Date, java.util.Date> sqlDateToDate = new DateTimeConverters.SqlDateToDate();
        try {
            java.util.Date target = sqlDateToDate.convert(sqlDate);
            assertEquals("SqlDateToDate", sqlDate.getTime(), target.getTime());
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        Converter<java.sql.Date, String> sqlDateToString = new DateTimeConverters.SqlDateToString();
        try {
            String target = sqlDateToString.convert(sqlDate);
            assertEquals("SqlDateToString", sqlDate.toString(), target);
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
        Converter<java.sql.Date, java.sql.Timestamp> sqlDateToTimestamp = new DateTimeConverters.SqlDateToTimestamp();
        try {
            java.sql.Timestamp target = sqlDateToTimestamp.convert(sqlDate);
            assertEquals("SqlDateToTimestamp", sqlDate.getTime(), target.getTime());
        } catch (ConversionException e) {
            fail(e.getMessage());
        }
    }
}
