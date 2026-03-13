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
package org.apache.ofbiz.base.test.uel

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.string.FlexibleStringExpander
import org.apache.ofbiz.base.util.string.UelFunctions
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

/**
 * ./gradlew 'ofbiz -t component=base -t suitename=basetests'
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */
class DateUelTest extends OFBizTestCase {

    DateUelTest(String name) {
        super(name)
    }

    void testDateUel() { // codenarc-disable JUnitTestMethodWithoutAssert
        doUelDateTest('${date:second(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getSecond(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:minute(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getMinute(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:hour(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getHour(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dayOfMonth(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getDayOfMonth(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dayOfWeek(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getDayOfWeek(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dayOfYear(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getDayOfYear(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:week(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getWeek(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:month(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getMonth(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:year(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getYear(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dayStart(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getDayStart(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dayEnd(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getDayEnd(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:weekStart(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getWeekStart(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:weekEnd(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getWeekEnd(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:monthStart(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getMonthStart(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:monthEnd(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getMonthEnd(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:yearStart(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getYearStart(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:yearEnd(now, timeZone, locale)}', { Timestamp now ->
            UtilDateTime.getYearEnd(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:dateStr(now, timeZone, locale)}', { Timestamp now ->
            UelFunctions.dateString(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:localizedDateStr(now, timeZone, locale)}', { Timestamp now ->
            UelFunctions.localizedDateString(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:localizedDateTimeStr(now, timeZone, locale)}', { Timestamp now ->
            UelFunctions.localizedDateTimeString(now, TimeZone.getDefault(), Locale.getDefault())
        })
        doUelDateTest('${date:timeStr(now, timeZone, locale)}', { Timestamp now ->
            UelFunctions.timeString(now, TimeZone.getDefault(), Locale.getDefault())
        })
    }

    void testNowTimestampUel() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${date:nowTimestamp()}')
        assert (fse.expand([:]).time - UtilDateTime.nowTimestamp().time).abs() < 15 // less than 10 ns appart
    }

    private void doUelDateTest(String uelInput, Closure uelFunction) {
        Timestamp now = UtilDateTime.nowTimestamp()
        Map context = [now: now,
                       timeZone: TimeZone.getDefault(),
                       locale: Locale.getDefault()]
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert fse.expand(context) == uelFunction(now)
    }

}
