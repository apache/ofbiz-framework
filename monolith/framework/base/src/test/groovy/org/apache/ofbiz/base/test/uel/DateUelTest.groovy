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

import java.sql.Timestamp
import java.util.stream.Stream

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * ./gradlew test --tests "org.apache.ofbiz.base.test.uel.DateUelTest"
 */
/* codenarc-disable GStringExpressionWithinString,ClosureAsLastMethodParameter */
class DateUelTest {

    @BeforeAll
    static void loadUelFunctions() {
        UelTestSupport.ensureUelFunctionsLoaded()
    }

    @ParameterizedTest(name = '{0}')
    @MethodSource('dateUelExpressions')
    void dateUelExpressionMatchesExpectedValue(String uelInput, Closure<?> expectedFunction) { // codenarc-disable JUnitTestMethodWithoutAssert
        Timestamp now = UtilDateTime.nowTimestamp()
        Map context = [now: now,
                       timeZone: TimeZone.getDefault(),
                       locale: Locale.getDefault()]
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(uelInput)
        assert fse.expand(context) == expectedFunction(now)
    }

    @Test
    void nowTimestampUelIsWithinToleranceOfActualNow() {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance('${date:nowTimestamp()}')
        assert (fse.expand([:]).time - UtilDateTime.nowTimestamp().time).abs() < 500 // less than 500 ms apart
    }

    @SuppressWarnings('UnusedPrivateMethod')
    private static Stream<Arguments> dateUelExpressions() {
        Stream.of(
            Arguments.of('${date:second(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getSecond(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:minute(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getMinute(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:hour(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getHour(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dayOfMonth(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getDayOfMonth(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dayOfWeek(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getDayOfWeek(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dayOfYear(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getDayOfYear(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:week(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getWeek(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:month(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getMonth(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:year(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getYear(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dayStart(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getDayStart(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dayEnd(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getDayEnd(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:weekStart(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getWeekStart(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:weekEnd(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getWeekEnd(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:monthStart(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getMonthStart(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:monthEnd(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getMonthEnd(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:yearStart(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getYearStart(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:yearEnd(now, timeZone, locale)}', { Timestamp now ->
                UtilDateTime.getYearEnd(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:dateStr(now, timeZone, locale)}', { Timestamp now ->
                UelFunctions.dateString(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:localizedDateStr(now, timeZone, locale)}', { Timestamp now ->
                UelFunctions.localizedDateString(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:localizedDateTimeStr(now, timeZone, locale)}', { Timestamp now ->
                UelFunctions.localizedDateTimeString(now, TimeZone.getDefault(), Locale.getDefault())
            }),
            Arguments.of('${date:timeStr(now, timeZone, locale)}', { Timestamp now ->
                UelFunctions.timeString(now, TimeZone.getDefault(), Locale.getDefault())
            })
        )
    }

}
