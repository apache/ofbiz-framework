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
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.test.GenericTestCaseBase;

import com.ibm.icu.util.Calendar;

@SourceMonitored
public class DateTimeTests extends GenericTestCaseBase {

    public DateTimeTests(String name) {
        super(name);
    }

    public static <S, T> void assertConversion(String label, Converter<S, T> converter, S source, T target) throws Exception {
        assertTrue(label + " can convert", converter.canConvert(source.getClass(), target.getClass()));
        assertEquals(label + " converted", target, converter.convert(source));
    }

    public void testDateTimeConverters() throws Exception {
        Calendar cal = Calendar.getInstance();
        long currentTime = cal.getTimeInMillis();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long longTime = cal.getTimeInMillis(); // Start of day today
        assertNotEquals("currentTime and longTime are not equal", currentTime, longTime);
        java.util.Date utilDate = new java.util.Date(longTime);
        java.sql.Date sqlDate = new java.sql.Date(longTime);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(longTime);
        // Source class = java.util.Date
        assertConversion("DateToLong", new DateTimeConverters.DateToLong(), utilDate, longTime);
        assertConversion("DateToSqlDate", new DateTimeConverters.DateToSqlDate(), utilDate, new java.sql.Date(longTime));
        assertConversion("DateToString", new DateTimeConverters.DateToString(), utilDate, utilDate.toString());
        assertConversion("DateToTimestamp", new DateTimeConverters.DateToTimestamp(), utilDate, timestamp);
        // Source class = java.sql.Date
        assertConversion("SqlDateToLong", new DateTimeConverters.DateToLong(), sqlDate, longTime);
        assertConversion("SqlDateToDate", new DateTimeConverters.SqlDateToDate(), sqlDate, utilDate);
        assertConversion("SqlDateToString", new DateTimeConverters.SqlDateToString(), sqlDate, sqlDate.toString());
        assertConversion("SqlDateToTimestamp", new DateTimeConverters.SqlDateToTimestamp(), sqlDate, timestamp);
        // Source class = java.sql.Timestamp
        assertConversion("TimestampToLong", new DateTimeConverters.DateToLong(), timestamp, longTime);
        assertConversion("TimestampToDate", new DateTimeConverters.TimestampToDate(), timestamp, utilDate);
        assertConversion("TimestampToSqlDate", new DateTimeConverters.TimestampToSqlDate(), timestamp, sqlDate);
        assertConversion("TimestampToString", new DateTimeConverters.TimestampToString(), timestamp, timestamp.toString());
        // Source class = java.lang.Long
        assertConversion("LongToDate", new DateTimeConverters.NumberToDate(), longTime, utilDate);
        assertConversion("LongToSqlDate", new DateTimeConverters.NumberToSqlDate(), longTime, sqlDate);
        assertConversion("LongToSqlDate", new DateTimeConverters.NumberToSqlDate(), currentTime, sqlDate); //Test conversion to start of day
        assertConversion("LongToTimestamp", new DateTimeConverters.NumberToTimestamp(), longTime, timestamp);
        // Source class = java.lang.String
        assertConversion("StringToTimestamp", new DateTimeConverters.StringToTimestamp(), timestamp.toString(), timestamp);
        //assertConversion("StringToDate", new DateTimeConverters.StringToDate(), utilDate.toString(), utilDate);
        //assertConversion("StringToSqlDate", new DateTimeConverters.StringToSqlDate(), sqlDate.toString(), sqlDate);
    }
}
