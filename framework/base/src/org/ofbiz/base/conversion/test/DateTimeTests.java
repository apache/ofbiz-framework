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
package org.ofbiz.base.conversion.test;

import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.DateTimeConverters;
import org.ofbiz.base.lang.SourceMonitor;
import org.ofbiz.base.test.GenericTestCaseBase;
import org.ofbiz.base.util.UtilGenerics;

@SourceMonitor("Adam Heath")
public class DateTimeTests extends GenericTestCaseBase {

    public DateTimeTests(String name) {
        super(name);
    }

    public static <S, T> void assertConversion(String label, Converter<S, T> converter, S source, T target) throws Exception {
        assertTrue(label + " can convert", converter.canConvert(source.getClass(), target.getClass()));
        assertEquals(label + " converted", target, converter.convert(source));
    }

    public void testDateTimeConverters() throws Exception {
        // Source class = java.util.Date
        long currentTime = System.currentTimeMillis();
        java.util.Date utilDate = new java.util.Date(currentTime);
        assertConversion("DateToLong", new DateTimeConverters.DateToLong(), utilDate, currentTime);
        assertConversion("DateToSqlDate", new DateTimeConverters.DateToSqlDate(), utilDate, new java.sql.Date(currentTime));
        assertConversion("DateToString", new DateTimeConverters.DateToString(), utilDate, utilDate.toString());
        //assertConversion("StringToDate", new DateTimeConverters.StringToDate(), utilDate.toString(), utilDate);
        assertConversion("DateToTimestamp", new DateTimeConverters.DateToTimestamp(), utilDate, new java.sql.Timestamp(currentTime));
        // Source class = java.sql.Date
        java.sql.Date sqlDate = new java.sql.Date(currentTime);
        assertConversion("SqlDateToDate", new DateTimeConverters.SqlDateToDate(), sqlDate, utilDate);
        assertConversion("SqlDateToString", new DateTimeConverters.SqlDateToString(), sqlDate, sqlDate.toString());
        //assertConversion("StringToSqlDate", new DateTimeConverters.StringToSqlDate(), sqlDate.toString(), sqlDate);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(currentTime);
        assertConversion("SqlDateToTimestamp", new DateTimeConverters.SqlDateToTimestamp(), sqlDate, timestamp);
        assertConversion("StringToTimestamp", new DateTimeConverters.StringToTimestamp(), timestamp.toString(), timestamp);
        assertConversion("TimestampToDate", new DateTimeConverters.TimestampToDate(), timestamp, utilDate);
        assertConversion("TimestampToSqlDate", new DateTimeConverters.TimestampToSqlDate(), timestamp, sqlDate);
        //assertConversion("TimestampToString", new DateTimeConverters.TimestampToString(), timestamp, timestamp.toString());
    }
}
