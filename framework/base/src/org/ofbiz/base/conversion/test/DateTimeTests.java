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
import org.ofbiz.base.test.GenericTestCaseBase;

public class DateTimeTests extends GenericTestCaseBase {

    public DateTimeTests(String name) {
        super(name);
    }

    public void testDateTimeConverters() throws Exception {
        // Source class = java.util.Date
        java.util.Date utilDate = new java.util.Date();
        long dateMillis = utilDate.getTime();
        Converter<java.util.Date, Long> dateToLong = new DateTimeConverters.DateToLong();
        {
            Long target = dateToLong.convert(utilDate);
            assertEquals("DateToLong", dateMillis, target.longValue());
        }
        Converter<java.util.Date, java.sql.Date> dateToSqlDate = new DateTimeConverters.DateToSqlDate();
        {
            java.sql.Date target = dateToSqlDate.convert(utilDate);
            assertEquals("DateToSqlDate", dateMillis, target.getTime());
        }
        Converter<java.util.Date, String> dateToString = new DateTimeConverters.DateToString();
        {
            String target = dateToString.convert(utilDate);
            assertEquals("DateToString", utilDate.toString(), target);
        }
        Converter<java.util.Date, java.sql.Timestamp> dateToTimestamp = new DateTimeConverters.DateToTimestamp();
        {
            java.sql.Timestamp timestamp = dateToTimestamp.convert(utilDate);
            assertEquals("DateToTimestamp", dateMillis, timestamp.getTime());
        }
        // Source class = java.sql.Date
        java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
        Converter<java.sql.Date, java.util.Date> sqlDateToDate = new DateTimeConverters.SqlDateToDate();
        {
            java.util.Date target = sqlDateToDate.convert(sqlDate);
            assertEquals("SqlDateToDate", sqlDate.getTime(), target.getTime());
        }
        Converter<java.sql.Date, String> sqlDateToString = new DateTimeConverters.SqlDateToString();
        {
            String target = sqlDateToString.convert(sqlDate);
            assertEquals("SqlDateToString", sqlDate.toString(), target);
        }
        Converter<java.sql.Date, java.sql.Timestamp> sqlDateToTimestamp = new DateTimeConverters.SqlDateToTimestamp();
        {
            java.sql.Timestamp target = sqlDateToTimestamp.convert(sqlDate);
            assertEquals("SqlDateToTimestamp", sqlDate.getTime(), target.getTime());
        }
    }
}
