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
package org.apache.ofbiz.base.util.string.uel

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.UelMapping

import java.sql.Timestamp

/**
 * Class for importing the Date Uel
 */
class DateUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('date:second', UtilDateTime.getMethod('getSecond', Timestamp, TimeZone, Locale),
                        'Returns the second value of Timestamp (0 - 59).'),
                new UelMapping('date:minute', UtilDateTime.getMethod('getMinute', Timestamp, TimeZone, Locale),
                        'Returns the minute value of Timestamp (0 - 59).'),
                new UelMapping('date:hour', UtilDateTime.getMethod('getHour', Timestamp, TimeZone, Locale),
                        'Returns the hour value of Timestamp (0 - 23).'),
                new UelMapping('date:dayOfMonth', UtilDateTime.getMethod('getDayOfMonth', Timestamp, TimeZone, Locale),
                        'Returns the day of month value of Timestamp (1 - 31) .'),
                new UelMapping('date:dayOfWeek', UtilDateTime.getMethod('getDayOfWeek', Timestamp, TimeZone, Locale),
                        'Returns the day of week value of Timestamp (Sunday = 1, Saturday = 7).'),
                new UelMapping('date:dayOfYear', UtilDateTime.getMethod('getDayOfYear', Timestamp, TimeZone, Locale),
                        'Returns the day of year value of Timestamp.'),
                new UelMapping('date:week', UtilDateTime.getMethod('getWeek', Timestamp, TimeZone, Locale),
                        'Returns the week value of Timestamp.'),
                new UelMapping('date:month', UtilDateTime.getMethod('getMonth', Timestamp, TimeZone, Locale),
                        'Returns the month value of Timestamp (January = 0, December = 11).'),
                new UelMapping('date:year', UtilDateTime.getMethod('getYear', Timestamp, TimeZone, Locale),
                        'Returns the year value of Timestamp.'),
                new UelMapping('date:dayStart', UtilDateTime.getMethod('getDayStart', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to start of day.'),
                new UelMapping('date:dayEnd', UtilDateTime.getMethod('getDayEnd', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to end of day.'),
                new UelMapping('date:weekStart', UtilDateTime.getMethod('getWeekStart', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to start of week.'),
                new UelMapping('date:weekEnd', UtilDateTime.getMethod('getWeekEnd', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to end of week.'),
                new UelMapping('date:monthStart', UtilDateTime.getMethod('getMonthStart', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to start of month.'),
                new UelMapping('date:monthEnd', UtilDateTime.getMethod('getMonthEnd', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to end of month.'),
                new UelMapping('date:yearStart', UtilDateTime.getMethod('getYearStart', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to start of year.'),
                new UelMapping('date:yearEnd', UtilDateTime.getMethod('getYearEnd', Timestamp, TimeZone, Locale),
                        'Returns Timestamp set to end of year.'),
                new UelMapping('date:dateStr', UelFunctions.getMethod('dateString', Timestamp, TimeZone, Locale),
                        'Returns Timestamp as a date String (yyyy-mm-dd) .'),
                new UelMapping('date:localizedDateStr', UelFunctions.getMethod('localizedDateString', Timestamp, TimeZone, Locale),
                        'Returns Timestamp as a date String formatted according to the supplied locale.'),
                new UelMapping('date:localizedDateTimeStr', UelFunctions.getMethod('localizedDateTimeString', Timestamp, TimeZone, Locale),
                        'Returns Timestamp as a date-time String formatted according to the supplied locale.'),
                new UelMapping('date:timeStr', UelFunctions.getMethod('timeString', Timestamp, TimeZone, Locale),
                        'Returns Timestamp as a time String (hh:mm) .'),
                new UelMapping('date:nowTimestamp', UtilDateTime.getMethod('nowTimestamp'),
                        'Returns Timestamp for right now.')
        ]
    }

}
