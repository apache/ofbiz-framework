/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.util.string

import junit.framework.TestCase
import org.apache.ofbiz.base.util.string.tool.SpecialNumber
import org.apache.ofbiz.base.util.string.tool.TestNpe
import org.apache.ofbiz.base.util.string.tool.TestException
import org.junit.Test

/* codenarc-disable GStringExpressionWithinString, JUnitTestMethodWithoutAssert, UnnecessaryBigDecimalInstantiation */

/**
 * Test Class for FlexibleStringExpander object
 */
class FlexibleStringExpanderTests extends TestCase {

    private static final Locale LOCALE_TO_TEST = new Locale('en', 'US')
    private static final Locale OTHER_LOCALE = new Locale('fr')
    private static final TimeZone TIME_ZONE_TO_TEST = TimeZone.getTimeZone('PST')
    private static final TimeZone OTHER_TIME_ZONE = TimeZone.getTimeZone('GMT')

    // Required to avoid ambiguous method calls. And makes tests easier to read as a bonus.
    private static final Map NULL_CONTEXT = null
    private static final Locale NULL_LOCALE = null
    private static final TimeZone NULL_TIMEZONE = null
    private static final Object NULL_OBJECT = null

    @Test
    void testEmptyFSE() {
        doEmptyFseTest(null, NULL_CONTEXT, NULL_TIMEZONE, NULL_LOCALE, '')
        doEmptyFseTest(null, [foo: 'bar'], NULL_TIMEZONE, NULL_LOCALE, '')
        doEmptyFseTest(null, [foo: 'bar'], TIME_ZONE_TO_TEST, LOCALE_TO_TEST, '')
        doEmptyFseTest('', [foo: 'bar'], null, null, '')
    }

    @Test
    void testFSECommonCases() {
        doFseTest('Hello World!', NULL_CONTEXT, NULL_TIMEZONE, NULL_LOCALE, 'Hello World!', NULL_OBJECT)
        doFseTest('Hello World!', [foo: 'bar'], 'Hello World!')
        doFseTest('Hello ${var}!', [var: 'World'], 'Hello World!')
        doFseTest('${\'Hello ${var}\'}!', [var: 'World'], 'Hello World!')
        doFseTest('${\'Hel${blank}lo ${var}\'}!', [var: 'World', blank: ''], 'Hello World!')
    }

    @Test
    void testFSEUelExceptionIntegration() {
        Map baseCtx = [exception: new TestException()]
        doFseTest('${exception.value}', [*: baseCtx], NULL_TIMEZONE, NULL_LOCALE, '', NULL_OBJECT)
        doFseEmptyExpandTest('${${exception.value}}', [*: baseCtx])
        doFseEmptyExpandTest('${excep${var}.value}', [*: baseCtx, var: 'tion'])
        doFseEmptyExpandTest('${exception${var}.value}', [*: baseCtx, var: ''])
        doFseTest('${npe.value}', [npe: new TestNpe()], NULL_TIMEZONE, NULL_LOCALE, '', NULL_OBJECT)
        doFseTest('The total is ${exception.value?currency(${usd})}.', [*: baseCtx], 'The total is .')
    }

    @Test
    void testFSEUelIntegrationOnNullOrEmpty() {
        Map baseCtx = [nullVar: null]
        doFseEmptyExpandTest('${${nu${nullVar}ll}}', [*: baseCtx])
        doFseEmptyExpandTest('${${nullVar.noProp}}', [*: baseCtx])
        doFseEmptyExpandTest('${${unknonL${nullVar}ist[0]}}', [nullVar: null])
        doFseTest('${null}', [:], NULL_TIMEZONE, NULL_LOCALE, '', NULL_OBJECT)
        doFseTest('${nullVar.noProp}', [*: baseCtx], NULL_TIMEZONE, NULL_LOCALE, '', NULL_OBJECT)
        doFseTest('${noList[0]}', [:], NULL_TIMEZONE, NULL_LOCALE, '', NULL_OBJECT)
        doFseTest('The total is ${map.missing?currency(${usd})}.', [map: [:]], NULL_TIMEZONE, LOCALE_TO_TEST,
                'The total is .', 'The total is .')
        doFseTest('The total is ${noList[0]?currency(${usd})}.', [noList: []], NULL_TIMEZONE, LOCALE_TO_TEST,
                'The total is .', 'The total is .')
        doFseTest('${${nullVar}}!', [*: baseCtx], '!')
    }

    @Test
    void testFSEUelIntegration() {
        doFseTest('Hello ${testMap.var}!', [testMap: [var: 'World']], 'Hello World!')
        doFseTest('Hello ${testMap.blank}World!', [testMap: [var: '']], 'Hello World!')
        doFseTest('Hello ${testList[0]}!', [testList: ['World']], 'Hello World!')
        doFseTestWithLocaleAndTimezone('${amount}', [amount: new BigDecimal('1234567.89')], NULL_TIMEZONE,
                LOCALE_TO_TEST, '1,234,567.89', new BigDecimal('1234567.89'))
        doFseTestWithLocaleAndTimezone('${a${\'moun\'}t}', [amount: new BigDecimal('1234567.89')], NULL_TIMEZONE,
                LOCALE_TO_TEST, '1,234,567.89', new BigDecimal('1234567.89'))
    }

    /* codenarc-disable NoJavaUtilDate */

    @Test
    void testFSEDate() {
        Date firstOfbizCommit = new Date(1154466300000)
        doFseTestWithLocaleAndTimezone('The date is ${date}.', [date: firstOfbizCommit], TIME_ZONE_TO_TEST,
                LOCALE_TO_TEST, 'The date is 2006-08-01 14:05:00.000.', 'The date is 2006-08-01 14:05:00.000.')
    }
    /* codenarc-enable NoJavaUtilDate */

    @Test
    void testMisformedFSE() {
        doFseTest('${foobar', [:], '${foobar')
        doFseTest('Hello${foobar', [:], 'Hello${foobar')
        doFseTest('Hello ${var}${foobar', [var: 'World'], 'Hello World${foobar')
    }

    @Test
    void testCurrencyFSE() {
        Map baseCtx = [usd: 'USD', amount: new BigDecimal('1234567.89')]
        doFseTestWithLocaleAndTimezone('${amount?currency(${usd})}', [*: baseCtx], NULL_TIMEZONE, LOCALE_TO_TEST,
                '$1,234,567.89', '$1,234,567.89')
        doFseTestWithLocaleAndTimezone('The total is ${amount?currency(${usd})}.', [*: baseCtx], NULL_TIMEZONE,
                LOCALE_TO_TEST, 'The total is $1,234,567.89.', 'The total is $1,234,567.89.')
    }

    @Test
    void testGroovyScriptFSE() {
        Map baseCtx = [var: 'World',
                       exception: new TestException(),
                       specialNumber: new SpecialNumber('1.00')]
        doFseTest('${groovy: return \'Hello \' + var + \'!\'}', [*: baseCtx], 'Hello World!')
        doFseTest('${groovy: return null}!', [*: baseCtx], '!')
        doFseTest('${groovy: return noList[0]}', [*: baseCtx], NULL_OBJECT)
        doFseTest('${groovy: return exception.value}!', [*: baseCtx], '!')
        doFseTest('${groovy: return specialNumber}!', [*: baseCtx], '1!')
        doFseTest('This is a groovy ${groovy: if (true) {return \'bracket\'}} expression', [:],
                'This is a groovy bracket expression')
        doFseTest('This is a groovy ${groovy: if (true) {if (true) {return \'with 2 brackets\'}}} expression',
                [:], 'This is a groovy with 2 brackets expression')
        doFseTestWithLocaleAndTimezone('${groovy: return amount}', [amount: new BigDecimal('1234567.89')],
                NULL_TIMEZONE, LOCALE_TO_TEST, '1,234,567.89', new BigDecimal('1234567.89'))
    }

    @Test
    void testGroovyScriptFSESecurity() {
        doFseTest('${groovy: java.util.Map.of(\'key\', \'value\')}!', [:], '!')
        doFseTest('${groovy: \'ls /\'.execute()}!', [:], '!')
        doFseTest('${groovy: new File(\'/etc/passwd\').getText()}!', [:], '!')
        doFseTest('${groovy: (new File \'/etc/passwd\') .getText()}!', [:], '!')
        doFseTest('${groovy: Eval.me(\'1\')}!', [:], '!')
        doFseTest('${groovy: Eval . me(\'1\')}!', [:], '!')
        doFseTest('${groovy: System.properties[\'ofbiz.home\']}!', [:], '!')
        doFseTest('${groovy: new groovyx.net.http.HTTPBuilder(\'https://XXXX.XXXX.com:443\')}!', [:], '!')
    }

    @Test
    void testFSEEscape() {
        doFseTest('This is an \\${escaped} expression', [:], 'This is an ${escaped} expression')
        doFseTest('This is an \\${groovy:escaped} expression', [:], 'This is an ${groovy:escaped} expression')
    }

    private static void doFseBaseTest(String input, Map<String, Object> context, TimeZone timeZone, Locale locale,
                                      String expandedString, Object expandedObj, boolean isEmpty) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(input)
        assert isEmpty == fse.isEmpty()

        assert (input ?: '') == fse.getOriginal()
        assert (input ?: '') == fse.toString()
        expandedString = expandedString ?: ''

        assert expandedString == FlexibleStringExpander.expandString(input, context)
        assert expandedString == FlexibleStringExpander.expandString(input, context, locale)
        assert expandedString == FlexibleStringExpander.expandString(input, context, timeZone, locale)

        assert expandedString == fse.expandString(context)
        assert expandedString == fse.expandString(context as Map, locale as Locale)
        assert expandedString == fse.expandString(context as Map, timeZone as TimeZone, locale as Locale)

        assert expandedObj == fse.expand(context)
        assert expandedObj == fse.expand(context as Map, locale as Locale)
        assert expandedObj == fse.expand(context as Map, timeZone as TimeZone, locale as Locale)
    }

    private static void doEmptyFseTest(String input, Map<String, Object> context, TimeZone timeZone, Locale locale,
                                       String expandedString, Object expandedObj = NULL_OBJECT) {
        doFseBaseTest(input, context, timeZone, locale, expandedString, expandedObj, true)
    }

    private static void doFseTest(String input, Map<String, Object> context, String expandedString) {
        doFseBaseTest(input, context, NULL_TIMEZONE, NULL_LOCALE, expandedString, expandedString, false)
    }

    private static void doFseEmptyExpandTest(String input, Map<String, Object> context) {
        doFseBaseTest(input, context, NULL_TIMEZONE, NULL_LOCALE, '', '', false)
    }

    private static void doFseTest(String input, Map<String, Object> context, TimeZone timeZone, Locale locale,
                                  String expandedString, Object expandedObj = NULL_OBJECT) {
        doFseBaseTest(input, context, timeZone, locale, expandedString, expandedObj, false)
    }

    /* codenarc-disable LocaleSetDefault */

    private static void doFseTestWithLocaleAndTimezone(String input, Map<String, Object> context, TimeZone timeZone,
                                                       Locale locale, String expandedString, Object expandedObj) {
        // Test with only default locale
        Locale.setDefault(locale)
        TimeZone.setDefault(timeZone)
        doFseBaseTest(input, context, NULL_TIMEZONE, NULL_LOCALE, expandedString, expandedObj, false)
        Locale.setDefault(OTHER_LOCALE)
        TimeZone.setDefault(OTHER_TIME_ZONE)
        doFseNotEqualsTest(input, context, expandedString, expandedObj)

        // Test with only autoUserLogin
        context << [autoUserLogin: [
                lastLocale: locale ? locale.toString() : null,
                lastTimeZone: timeZone ? timeZone.getID() : null
        ]]
        doFseBaseTest(input, context, NULL_TIMEZONE, NULL_LOCALE, expandedString, expandedObj, false)
        context.autoUserLogin = [
                lastLocale: OTHER_LOCALE.toString(),
                lastTimeZone: OTHER_TIME_ZONE.getID()
        ]
        doFseNotEqualsTest(input, context, expandedString, expandedObj)

        // Test with only context
        context.put('locale', locale)
        context.put('timeZone', timeZone)
        doFseBaseTest(input, context, timeZone, locale, expandedString, expandedObj, false)
        context.put('locale', OTHER_LOCALE)
        context.put('timeZone', OTHER_TIME_ZONE)
        doFseNotEqualsTest(input, context, expandedString, expandedObj)
    }
    /* codenarc-enable LocaleSetDefault */

    private static void doFseNotEqualsTest(String input, Map<String, Object> context, String expandedString,
                                           Object expandedObj) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(input)
        assert expandedString != FlexibleStringExpander.expandString(input, context)
        assert expandedString != fse.expandString(context)
        if (expandedObj instanceof String) {
            assert expandedObj != fse.expand(context)
        }
    }

}
