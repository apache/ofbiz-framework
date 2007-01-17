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
package org.ofbiz.service.calendar;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Recurrence Utilities
 */
public class RecurrenceUtil {

    /** Returns a Date object from a String. */
    public static Date parseDate(String dateStr) {
        String formatString = new String();

        if (dateStr.length() == 16)
            dateStr = dateStr.substring(0, 14);
        if (dateStr.length() == 15)
            formatString = "yyyyMMdd'T'hhmmss";
        if (dateStr.length() == 8)
            formatString = "yyyyMMdd";

        SimpleDateFormat formatter = new SimpleDateFormat(formatString);
        ParsePosition pos = new ParsePosition(0);

        return formatter.parse(dateStr, pos);
    }

    /** Returns a List of parsed date strings. */
    public static List parseDateList(List dateList) {
        List newList = new ArrayList();

        if (dateList == null)
            return newList;
        Iterator i = dateList.iterator();

        while (i.hasNext())
            newList.add(parseDate((String) i.next()));
        return newList;
    }

    /** Returns a String from a Date object */
    public static String formatDate(Date date) {
        String formatString = new String();
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);
        if (cal.isSet(Calendar.MINUTE))
            formatString = "yyyyMMdd'T'hhmmss";
        else
            formatString = "yyyyMMdd";
        SimpleDateFormat formatter = new SimpleDateFormat(formatString);

        return formatter.format(date);
    }

    /** Returns a Llist of date strings from a List of Date objects */
    public static List formatDateList(List dateList) {
        List newList = new ArrayList();
        Iterator i = dateList.iterator();

        while (i.hasNext())
            newList.add(formatDate((Date) i.next()));
        return newList;
    }

    /** Returns the time as of now. */
    public static long now() {
        return (new Date()).getTime();
    }

}

