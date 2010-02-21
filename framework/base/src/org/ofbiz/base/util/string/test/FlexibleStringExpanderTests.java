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
package org.ofbiz.base.util.string.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.conversion.AbstractConverter;
import org.ofbiz.base.conversion.ConversionException;
import org.ofbiz.base.conversion.Converters;

public class FlexibleStringExpanderTests extends TestCase {
    private static final Locale localeToTest = new Locale("en", "US");
    private static final Locale badLocale = new Locale("fr");
    private static final TimeZone timeZoneToTest = TimeZone.getTimeZone("PST");
    private static final TimeZone badTimeZone = TimeZone.getTimeZone("GMT");

    private boolean wasVerbose;

    public FlexibleStringExpanderTests(String name) {
        super(name);
    }

    private static void parserTest(String label, String input, String toString) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(input);
        assertEquals(label + ":toString", toString, fse.toString());
    }

    public void testParsing() {
        parserTest("visible nested replacement", "${'Hello ${var}'}!", "${'Hello ${var}'}!");
        parserTest("hidden (runtime) nested null callreplacement", "Hello ${${groovy:" + FlexibleStringExpanderTests.class.getName() + ".StaticReturnNull()}}World!", "Hello ${${groovy:" + FlexibleStringExpanderTests.class.getName() + ".StaticReturnNull()}}World!");
        parserTest("UEL integration(nested): throw Exception", "${${throwException.value}}", "${throwException.value}");
        parserTest("nested-constant-emptynest-emptynest", "${a${}${}", "a${}${}");
        parserTest("null", null, "");
        parserTest("empty", "", "");
        parserTest("constant-only", "a", "a");
        parserTest("nested-constant-emptynest-emptynest", "${a${}${}", "a${}${}");
        parserTest("bsh", "${bsh:}", "");
        parserTest("groovy", "${groovy:}", "");

        parserTest("escaped", "\\${}", "\\${}");
        parserTest("constant-escaped", "a\\${}", "a\\${}");
        parserTest("escaped-bsd", "\\${bsh:}", "\\${bsh:}");
        parserTest("escaped-groovy", "\\${groovy:}", "\\${groovy:}");

        parserTest("missing-}", "${", "${");
        parserTest("nested-constant-missing-}", "${a${}", "a${}");
        parserTest("nested-constant-nested-nested-missing-}", "${a${${}", "a${${}");
        parserTest("escaped-missing-}", "\\${", "\\${");
        parserTest("constant-escaped-missing-}", "a\\${", "a\\${");

        parserTest("currency", "${?currency(", "${?currency(");
        parserTest("currency", "${?currency()", "${?currency()");
        parserTest("currency", "${price?currency(", "${price?currency(");
        parserTest("currency", "${price?currency()", "${price?currency()");
        parserTest("currency", "${?currency(usd", "${?currency(usd");
        parserTest("currency", "${?currency(usd)", "${?currency(usd)");
        parserTest("currency", "${price?currency(usd", "${price?currency(usd");
        parserTest("currency", "${price?currency(usd)", "${price?currency(usd)");
        parserTest("currency", "${?currency(}", "?currency(");
        parserTest("currency", "${?currency()}", "?currency()");
        parserTest("currency", "${?currency(usd}", "?currency(usd");
        parserTest("currency", "${?currency(usd)}", "?currency(usd)");
        parserTest("currency", "${price?currency(}", "price?currency(");
        parserTest("currency", "${price?currency()}", "price?currency()");
        parserTest("currency", "${price?currency(usd}", "price?currency(usd");
        parserTest("currency", "${price?currency(usd)}", "price?currency(usd)");
        parserTest("currency", "a${price?currency(usd)}b", "a${price?currency(usd)}b");
    }

    private static void fseTest(String label, String input, Map<String, Object> context, Object compare, boolean isEmpty) {
        fseTest(label, input, context, null, null, compare, isEmpty);
    }

    private static void doFseTest(String label, String input, FlexibleStringExpander fse, Map<String, Object> context, TimeZone timeZone, Locale locale, Object compare, boolean isEmpty) {
        assertEquals("isEmpty:" + label, isEmpty, fse.isEmpty());
        if (input == null) {
            assertEquals("getOriginal():" + label, "", fse.getOriginal());
            assertEquals("toString():" + label, "", fse.toString());
            assertEquals("expandString(null):" + label, "", fse.expandString(null));
            if (timeZone == null) {
                assertEquals("expandString(null):" + label, "", fse.expandString(null, locale));
            } else {
                assertEquals("expandString(null):" + label, "", fse.expandString(null, timeZone, locale));
            }
        } else {
            //assertEquals("getOriginal():" + label, input, fse.getOriginal());
            //assertEquals("toString():" + label, input, fse.toString());
            //assertEquals("expandString(null):" + label, input, fse.expandString(null));
            //if (timeZone == null) {
            //    assertEquals("expandString(null):" + label, input, fse.expandString(null, locale));
            //} else {
            //    assertEquals("expandString(null):" + label, input, fse.expandString(null, timeZone, locale));
            //}
        }
        if (locale == null) {
            assertEquals(label, compare, fse.expandString(context));
        } else {
            Locale defaultLocale = Locale.getDefault();
            TimeZone defaultTimeZone = TimeZone.getDefault();
            try {
                Locale.setDefault(locale);
                TimeZone.setDefault(timeZone);
                assertEquals(label, compare, fse.expandString(context, null, null));
                Locale.setDefault(badLocale);
                TimeZone.setDefault(badTimeZone);
                assertNotSame(label, compare, fse.expandString(context, null, null));
                Map<String, Object> autoUserLogin = new HashMap<String, Object>();
                autoUserLogin.put("lastLocale", locale.toString());
                autoUserLogin.put("lastTimeZone", timeZone == null ? null : timeZone.getID());
                context.put("autoUserLogin", autoUserLogin);
                assertEquals(label, compare, fse.expandString(context, null, null));
                autoUserLogin.put("lastLocale", badLocale.toString());
                autoUserLogin.put("lastTimeZone", badTimeZone.getID());
                assertNotSame(label, compare, fse.expandString(context, null, null));
                context.remove("autoUserLogin");
                context.put("locale", locale);
                context.put("timeZone", timeZone);
                assertEquals(label, compare, fse.expandString(context, null, null));
                context.put("locale", badLocale);
                context.put("timeZone", badTimeZone);
                assertNotSame(label, compare, fse.expandString(context, null, null));
                context.remove("locale");
                context.remove("timeZone");
                assertEquals(label, compare, fse.expandString(context, timeZone, locale));
                assertNotSame(label, compare, fse.expandString(context, badTimeZone, badLocale));
            } finally {
                Locale.setDefault(defaultLocale);
                TimeZone.setDefault(defaultTimeZone);
            }
        }
    }

    private static void fseTest(String label, String input, Map<String, Object> context, TimeZone timeZone, Locale locale, Object compare, boolean isEmpty) {
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(input);
        doFseTest(label, input, fse, context, timeZone, locale, compare, isEmpty);
        assertEquals("static expandString:" + label, compare, FlexibleStringExpander.expandString(input, context, timeZone, locale));
        if (input == null) {
            assertEquals("static expandString(null, null):" + label, "", FlexibleStringExpander.expandString(input, null));
            assertEquals("static expandString(null, null):" + label, "", FlexibleStringExpander.expandString(input, null, locale));
        } else {
            assertEquals("static expandString(input, null):" + label, input, FlexibleStringExpander.expandString(input, null));
            assertEquals("static expandString(input, null):" + label, input, FlexibleStringExpander.expandString(input, null, locale));
        }
        if (!fse.isEmpty()) {
            fse = FlexibleStringExpander.getInstance(input);
            doFseTest(label, input, fse, context, timeZone, locale, compare, isEmpty);
        }
    }

    public static String StaticReturnNull() {
        return null;
    }

    public void setUp() {
        wasVerbose = Debug.isOn(Debug.VERBOSE);
        if (getName().equals("testWithVerbosity")) {
            Debug.set(Debug.VERBOSE, true);
        }
        Converters.registerConverter(new SpecialNumberToString());
    }

    public void tearDown() {
        Debug.set(Debug.VERBOSE, wasVerbose);
    }

    public static class ThrowException {
        public Object getValue() throws Exception {
            throw new Exception();
        }
    }

    public static class ThrowNPE {
        public Object getValue() {
            throw new NullPointerException();
        }
    }

    @SuppressWarnings("unchecked")
    public static class SpecialNumberToString extends AbstractConverter {
        public SpecialNumberToString() {
            super(SpecialNumber.class, String.class);
        }

        public Object convert(Object obj) throws ConversionException {
            throw new NullPointerException();
        }
    }

    public static class SpecialNumber extends BigDecimal {
        public SpecialNumber(String value) {
            super(value);
        }

        public String toString() {
            return getClass().getName();
        }
    }

    public void testWithVerbosity() {
        everythingTest();
    }

    public void testQuietly() {
        everythingTest();
    }

    private void everythingTest() {
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("date", new java.util.Date(1234567890));
        testMap.put("usd", "USD");
        testMap.put("amount", new BigDecimal("1234567.89"));
        testMap.put("blank", "");
        testMap.put("exc", "Exception");
        testMap.put("nullVar", null);
        testMap.put("throwException", new ThrowException());
        testMap.put("throwNPE", new ThrowNPE());
        testMap.put("var", "World");
        testMap.put("nested", "Hello ${var}");
        testMap.put("testMap", testMap);
        testMap.put("nestedNull", "Hello ${nullVar}${var}");
        testMap.put("specialNumber", new SpecialNumber("1.00"));
        List<String> testList = new ArrayList<String>();
        testList.add("World");
        testMap.put("testList", testList);
        fseTest("null FlexibleStringExpander, null map", null, null, "", true);
        fseTest("null FlexibleStringExpander", null, testMap, "", true);
        fseTest("null context", "Hello World!", null, "Hello World!", false);
        fseTest("simple replacement", "Hello ${var}!", testMap, "Hello World!", false);
        fseTest("null FlexibleStringExpander with timeZone/locale", null, testMap, timeZoneToTest, localeToTest, "", true);
        fseTest("empty FlexibleStringExpander", "", testMap, "", true);
        fseTest("UEL integration(nested): throw Exception", "${${throwException.value}}", testMap, "", false);
        fseTest("UEL integration: throw Exception", "${throwException.value}", testMap, "", false);
        fseTest("hidden (runtime) nested replacement", "${nested}!", testMap, "Hello World!", false);
        fseTest("hidden (runtime) nested null replacement", "Hello ${${nullVar}}World!", testMap, "Hello World!", false);
        fseTest("hidden (runtime) nested null callreplacement", "Hello ${${groovy:" + FlexibleStringExpanderTests.class.getName() + ".StaticReturnNull()}}World!", testMap, "Hello World!", false);
        fseTest("UEL integration(nested): throw Exception", "${throw${exc}.value}", testMap, "", false);
        fseTest("UEL integration(nested): throw NPE", "${throwNPE${blank}.value}", testMap, "", false);
        fseTest("visible nested replacement", "${'Hello ${var}'}!", testMap, "Hello World!", false);
        fseTest("blank nested replacement", "${'Hel${blank}lo ${var}'}!", testMap, "Hello World!", false);
        fseTest("UEL integration(nested): null", "${${nu${nullVar}ll}}", testMap, "", false);
        fseTest("UEL integration(nested): NPE", "${${nullVar.noProp}}", testMap, "", false);
        fseTest("UEL integration(nested): missing", "${${noL${nullVar}ist[0]}}", testMap, "", false);
        fseTest("date w/ timezone", "The date is ${date}.", testMap, timeZoneToTest, localeToTest, "The date is 1970-01-14 22:56:07.890.", false);
        fseTest("just bad", "${foobar", testMap, "${foobar", false);
        fseTest("constant and bad", "Hello${foobar", testMap, "Hello${foobar", false);
        fseTest("good and bad", "Hello ${var}${foobar", testMap, "Hello World${foobar", false);
        fseTest("currency(USD)", "The total is ${amount?currency(${usd})}.", testMap, null, localeToTest, "The total is $1,234,567.89.", false);
        fseTest("currency(USD): null", "The total is ${testMap.missing?currency(${usd})}.", testMap, null, localeToTest, "The total is .", false);
        fseTest("currency(USD): missing", "The total is ${noList[0]?currency(${usd})}.", testMap, null, localeToTest, "The total is .", false);
        fseTest("currency(USD): exception", "The total is ${throwException.value?currency(${usd})}.", testMap, null, localeToTest, "The total is .", false);
        fseTest("null nested", "${${nullVar}}!", testMap, "!", false);
        fseTest("bsh: script", "${bsh:return \"Hello \" + var + \"!\";}", testMap, "Hello World!", false);
        fseTest("bsh: null", "${bsh:return null;}!", testMap, "!", false);
        fseTest("bsh: throw Exception", "${bsh:return throwException.value;}!", testMap, "!", false);
        fseTest("bsh: converter exception", "${bsh:return specialNumber;}!", testMap, SpecialNumber.class.getName() + "!", false);
        fseTest("groovy: script", "${groovy:return \"Hello \" + var + \"!\";}", testMap, "Hello World!", false);
        fseTest("groovy: null", "${groovy:return null;}!", testMap, "!", false);
        fseTest("groovy missing property", "${groovy: return noList[0]}", testMap, "", false);
        fseTest("groovy: throw Exception", "${groovy:return throwException.value;}!", testMap, "!", false);
        fseTest("groovy: converter exception", "${groovy:return specialNumber;}!", testMap, SpecialNumber.class.getName() + "!", false);
        fseTest("UEL integration: Map", "Hello ${testMap.var}!", testMap, "Hello World!", false);
        fseTest("UEL integration: blank", "Hello ${testMap.blank}World!", testMap, "Hello World!", false);
        fseTest("UEL integration: List", "Hello ${testList[0]}!", testMap, "Hello World!", false);
        fseTest("UEL integration: null", "${null}", testMap, "", false);
        fseTest("UEL integration: null dereference", "${nullVar.noProp}", testMap, "", false);
        fseTest("UEL integration: throw NPE", "${" + FlexibleStringExpanderTests.class.getName() + ".ThrowNPE.noProp}", testMap, "", false);
        fseTest("UEL integration: missing", "${noList[0]}", testMap, "", false);
        fseTest("Escaped expression", "This is an \\${escaped} expression", testMap, "This is an ${escaped} expression", false);
        fseTest("Escaped(bsh) expression", "This is an \\${bsh:escaped} expression", testMap, "This is an ${bsh:escaped} expression", false);
        fseTest("Escaped(groovy) expression", "This is an \\${groovy:escaped} expression", testMap, "This is an ${groovy:escaped} expression", false);
    }
}
