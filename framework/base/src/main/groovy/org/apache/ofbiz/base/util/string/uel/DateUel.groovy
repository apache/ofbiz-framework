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
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.base.util.string.IUelMappingLibrary
import org.apache.ofbiz.base.util.string.UelMapping

import java.lang.reflect.Method
import java.sql.Timestamp

/**
 * Class for importing the Date Uel
 */
class DateUel implements IUelMappingLibrary {

    @Override
    List<UelMapping> getUelMappingList() {
        return [
                new UelMapping('date:second', UtilDateTime.getMethod('getSecond', Timestamp, TimeZone, Locale)),
                new UelMapping('date:minute', UtilDateTime.getMethod('getMinute', Timestamp, TimeZone, Locale)),
                new UelMapping('date:hour', UtilDateTime.getMethod('getHour', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dayOfMonth', UtilDateTime.getMethod('getDayOfMonth', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dayOfWeek', UtilDateTime.getMethod('getDayOfWeek', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dayOfYear', UtilDateTime.getMethod('getDayOfYear', Timestamp, TimeZone, Locale)),
                new UelMapping('date:week', UtilDateTime.getMethod('getWeek', Timestamp, TimeZone, Locale)),
                new UelMapping('date:month', UtilDateTime.getMethod('getMonth', Timestamp, TimeZone, Locale)),
                new UelMapping('date:year', UtilDateTime.getMethod('getYear', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dayStart', UtilDateTime.getMethod('getDayStart', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dayEnd', UtilDateTime.getMethod('getDayEnd', Timestamp, TimeZone, Locale)),
                new UelMapping('date:weekStart', UtilDateTime.getMethod('getWeekStart', Timestamp, TimeZone, Locale)),
                new UelMapping('date:weekEnd', UtilDateTime.getMethod('getWeekEnd', Timestamp, TimeZone, Locale)),
                new UelMapping('date:monthStart', UtilDateTime.getMethod('getMonthStart', Timestamp, TimeZone, Locale)),
                new UelMapping('date:monthEnd', UtilDateTime.getMethod('getMonthEnd', Timestamp, TimeZone, Locale)),
                new UelMapping('date:yearStart', UtilDateTime.getMethod('getYearStart', Timestamp, TimeZone, Locale)),
                new UelMapping('date:yearEnd', UtilDateTime.getMethod('getYearEnd', Timestamp, TimeZone, Locale)),
                new UelMapping('date:dateStr', UelFunctions.getMethod('dateString', Timestamp, TimeZone, Locale)),
                new UelMapping('date:localizedDateStr', UelFunctions.getMethod('localizedDateString', Timestamp, TimeZone, Locale)),
                new UelMapping('date:localizedDateTimeStr', UelFunctions.getMethod('localizedDateTimeString', Timestamp, TimeZone, Locale)),
                new UelMapping('date:timeStr', UelFunctions.getMethod('timeString', Timestamp, TimeZone, Locale)),
                new UelMapping('date:nowTimestamp', UtilDateTime.getMethod('nowTimestamp'))
        ]
    }

}
