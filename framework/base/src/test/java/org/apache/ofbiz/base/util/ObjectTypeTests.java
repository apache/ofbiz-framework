/*
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
 */
package org.apache.ofbiz.base.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.junit.Test;
import org.w3c.dom.Document;

import com.ibm.icu.util.Calendar;

public class ObjectTypeTests {
    public static final String module = ObjectTypeTests.class.getName();
    private static final LocaleData localeData = new LocaleData("en_US", "Pacific/Wake", "fr", "GMT");
    private final TimeDuration duration = new TimeDuration(0, 0, 0, 1, 1, 1, 1);
    // These numbers are all based on 1 / 128, which is a binary decimal
    // that can be represented by both float and double
    private final BigDecimal dcml = new BigDecimal("781.25");
    private final Double dbl = Double.valueOf("7.8125E2");
    private final Float flt = Float.valueOf("7.8125E2");
    private final Long lng = Long.valueOf("781");
    private final Integer intg = Integer.valueOf("781");
    private final Timestamp tstmp = new Timestamp(781L);
    private final Timestamp ntstmp;
    private final java.util.Date utlDt = new java.util.Date(781);
    private final java.sql.Date sqlDt;
    private final java.sql.Time sqlTm = new java.sql.Time(2096000);
    private final List<Object> list;
    private final Map<String, Object> map;
    private final Set<Object> set;

    public ObjectTypeTests() {
        ntstmp = new Timestamp(781L);
        ntstmp.setNanos(123000000);
        list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");
        map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        set = new LinkedHashSet<>(list);
        Calendar cal = UtilDateTime.getCalendarInstance(localeData.goodTimeZone, localeData.goodLocale);
        cal.set(1969, Calendar.DECEMBER, 31, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        sqlDt = new java.sql.Date(cal.getTimeInMillis());
    }

    public static class LocaleData {
        public final Locale goodLocale;
        public final TimeZone goodTimeZone;
        public final Locale badLocale;
        public final TimeZone badTimeZone;

        public LocaleData(String goodLocale, String goodTimeZone, String badLocale, String badTimeZone) {
            this.goodLocale = UtilMisc.parseLocale(goodLocale);
            this.goodTimeZone = TimeZone.getTimeZone(goodTimeZone);
            this.badLocale = UtilMisc.parseLocale(badLocale);
            this.badTimeZone = TimeZone.getTimeZone(badTimeZone);
        }
    }

    public static Object simpleTypeOrObjectConvert(Object obj, String type, String format, TimeZone timeZone,
            Locale locale, boolean noTypeFail) throws GeneralException {
        return ObjectType.simpleTypeOrObjectConvert(obj, type, format, timeZone, locale, noTypeFail);
    }

    public static void simpleTypeOrObjectConvertTest(String label, Object toConvert, String type, Object wanted)
            throws GeneralException {
        basicTest(label, toConvert);
        assertEquals(label + ":null target type", toConvert,
                simpleTypeOrObjectConvert(toConvert, null, null, null, null, true));
        assertEquals(label + ":null source object", (Object) null,
                simpleTypeOrObjectConvert(null, type, null, null, null, true));
        assertEquals(label, wanted, simpleTypeOrObjectConvert(toConvert, type, null, null, null, true));
        if (toConvert instanceof String) {
            String str = (String) toConvert;
            Document doc = UtilXml.makeEmptyXmlDocument();
            assertEquals(label + ":text-node proxy", wanted,
                    simpleTypeOrObjectConvert(doc.createTextNode(str), type, null, null, null, true));
        }
    }

    public static void simpleTypeOrObjectConvertTest(String label, Object toConvert, String type, String format,
            LocaleData localeData, Object wanted) throws GeneralException {
        basicTest(label, toConvert);
        Locale defaultLocale = Locale.getDefault();
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(localeData.goodLocale);
            TimeZone.setDefault(localeData.goodTimeZone);
            assertEquals(label + ":default-timezone/locale", wanted,
                    simpleTypeOrObjectConvert(toConvert, type, format, null, null, true));
            assertNotEquals(label + ":bad-passed-timezone/locale", wanted,
                    simpleTypeOrObjectConvert(toConvert, type, format, localeData.badTimeZone, localeData.badLocale,
                            true));
            Locale.setDefault(localeData.badLocale);
            TimeZone.setDefault(localeData.badTimeZone);
            assertNotEquals(label + ":bad-default-timezone/locale", wanted,
                    simpleTypeOrObjectConvert(toConvert, type, format, null, null, true));
            assertEquals(label + ":passed-timezone/locale", wanted, simpleTypeOrObjectConvert(toConvert, type, format,
                    localeData.goodTimeZone, localeData.goodLocale, true));
        } finally {
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    public static void simpleTypeOrObjectConvertTestSingleMulti(String label, Object toConvert, String[] types,
            Object wanted) throws GeneralException {
        for (int j = 0; j < types.length; j++) {
            simpleTypeOrObjectConvertTest(label + "(:" + j + ")", toConvert, types[j], wanted);
        }
    }

    public static void simpleTypeOrObjectConvertTestMultiMulti(String label, Object[] toConvert, String[] types,
            Object wanted) throws GeneralException {
        for (int i = 0; i < toConvert.length; i++) {
            for (int j = 0; j < types.length; j++) {
                simpleTypeOrObjectConvertTest(label + "(" + i + ":" + j + ")", toConvert[i], types[j], wanted);
            }
        }
    }

    public static void simpleTypeOrObjectConvertTestSingleMulti(String label, Object toConvert, String[] types,
            String format, LocaleData localeData, Object wanted) throws GeneralException {
        for (int j = 0; j < types.length; j++) {
            simpleTypeOrObjectConvertTest(label + "(:" + j + ")", toConvert, types[j], format, localeData, wanted);
        }
    }

    public static void simpleTypeOrObjectConvertTestMultiMulti(String label, Object[] toConvert, String[] types,
            String format, LocaleData localeData, Object wanted) throws GeneralException {
        for (int i = 0; i < toConvert.length; i++) {
            for (int j = 0; j < types.length; j++) {
                simpleTypeOrObjectConvertTest(label + "(" + i + ":" + j + ")", toConvert[i], types[j], format,
                        localeData, wanted);
            }
        }
    }

    public static void simpleTypeOrObjectConvertTestError(String label, Object toConvert, String type)
            throws GeneralException {
        GeneralException caught = null;
        try {
            simpleTypeOrObjectConvert(toConvert, type, null, null, null, true);
        } catch (GeneralException e) {
            caught = e;
        } finally {
            assertNotNull(label + ":caught", caught);
        }
    }

    public static void simpleTypeOrObjectConvertTestError(String label, Object toConvert, String[] types)
            throws GeneralException {
        simpleTypeOrObjectConvertTestError(label + ":this", toConvert, GeneralException.class.getName());
        for (String type: types) {
            simpleTypeOrObjectConvertTestError(label + ":" + type, toConvert, type);
        }
    }

    public static void simpleTypeOrObjectConvertTestNoError(String label, Object toConvert, String type)
            throws GeneralException {
        assertSame(label, toConvert, simpleTypeOrObjectConvert(toConvert, type, null, null, null, false));
    }

    public static void simpleTypeOrObjectConvertTestNoError(String label, Object toConvert, String[] types)
            throws GeneralException {
        simpleTypeOrObjectConvertTestNoError(label + ":this", toConvert, GeneralException.class.getName());
        for (String type: types) {
            simpleTypeOrObjectConvertTestNoError(label + ":" + type, toConvert, type);
        }
    }

    public static void basicTest(String label, Object toConvert) throws GeneralException {
        assertEquals(label + ":PlainString", toConvert.toString(),
                simpleTypeOrObjectConvert(toConvert, "PlainString", null, null, null, true));
        assertSame(label + ":same", toConvert,
                simpleTypeOrObjectConvert(toConvert, toConvert.getClass().getName(), null, null, null, true));
        assertSame(label + ":to-Object", toConvert,
                simpleTypeOrObjectConvert(toConvert, "Object", null, null, null, true));
        assertSame(label + ":to-java.lang.Object",
                toConvert, simpleTypeOrObjectConvert(toConvert, "java.lang.Object", null, null, null, true));
    }

    @Test
    public void testLoadClassWithNonExistentClass() {
        Exception exception = null;
        try {
            ObjectType.loadClass("foobarbaz");
        } catch (Exception e) {
            exception = e;
        }
        assertTrue("Exception thrown by loadClass(\"foobarbaz\") is not ClassNotFoundException",
                exception instanceof ClassNotFoundException);
    }

    @Test
    public void testLoadClassWithPrimitives() {
        try {
            Class<?> theClass;
            theClass = ObjectType.loadClass("boolean");
            assertEquals("Wrong class returned by loadClass(\"boolean\")", (Boolean.TYPE).getName(),
                    theClass.getName());
            theClass = ObjectType.loadClass("short");
            assertEquals("Wrong class returned by loadClass(\"short\")", (Short.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("int");
            assertEquals("Wrong class returned by loadClass(\"int\")", (Integer.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("long");
            assertEquals("Wrong class returned by loadClass(\"long\")", (Long.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("float");
            assertEquals("Wrong class returned by loadClass(\"float\")", (Float.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("double");
            assertEquals("Wrong class returned by loadClass(\"double\")", (Double.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("byte");
            assertEquals("Wrong class returned by loadClass(\"byte\")", (Byte.TYPE).getName(), theClass.getName());
            theClass = ObjectType.loadClass("char");
            assertEquals("Wrong class returned by loadClass(\"char\")", (Character.TYPE).getName(),
                    theClass.getName());
        } catch (Exception e) {
            fail("Exception thrown by loadClass: " + e.getMessage());
        }
    }

    @Test
    public void testLoadClassWithAlias() {
        try {
            Class<?> theClass;
            // first try with a class full name
            theClass = ObjectType.loadClass("java.lang.String");
            assertEquals("Wrong class returned by loadClass(\"java.lang.String\")", "java.lang.String",
                    theClass.getName());
            // now try with some aliases
            theClass = ObjectType.loadClass("String");
            assertEquals("Wrong class returned by loadClass(\"String\")", "java.lang.String", theClass.getName());
            theClass = ObjectType.loadClass("Object");
            assertEquals("Wrong class returned by loadClass(\"Object\")", "java.lang.Object", theClass.getName());
            theClass = ObjectType.loadClass("Date");
            assertEquals("Wrong class returned by loadClass(\"Date\")", "java.sql.Date", theClass.getName());
        } catch (Exception e) {
            fail("Exception thrown by loadClass: " + e.getMessage());
        }
    }

    @Test
    public void testClassNotFound() {
        GeneralException caught = null;
        try {
            ObjectType.simpleTypeOrObjectConvert(this, "foobarbaz", null, null, null, false);
        } catch (GeneralException e) {
            caught = e;
        } finally {
            assertNotNull("class not found", caught);
        }
    }

    @Test
    public void testArray() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Object[]->List", new Object[] {"one", "two", "three"},
                new String[] {"List", "java.util.List"}, list);
        simpleTypeOrObjectConvertTestSingleMulti("int[]->List", new int[] {1, 2, 3},
                new String[] {"List", "java.util.List"}, Arrays.asList(1, 2, 3));
        simpleTypeOrObjectConvertTestError("Object[]->error", new Object[] {"one", "two", "three"},
                new String[] {"Map"});
        simpleTypeOrObjectConvertTestError("int[]->error", new int[] {1, 2, 3},
                new String[] {"java.util.ArrayList", "Map"});
    }

    @Test
    public void testString() throws GeneralException, Exception {
        simpleTypeOrObjectConvertTest("String->String", "one", "String", "one");
        simpleTypeOrObjectConvertTest("String->String", "one", "java.lang.String", "one");
        simpleTypeOrObjectConvertTestSingleMulti("empty-String->anything", "", new String[] {"List", "Map"}, null);
        simpleTypeOrObjectConvertTestError("String->error", "one", new String[] {});
        simpleTypeOrObjectConvertTestMultiMulti("String->Boolean(true)", new String[] {"true", " true ", " TrUe"},
                new String[] {"Boolean", "java.lang.Boolean"}, Boolean.TRUE);
        simpleTypeOrObjectConvertTestMultiMulti("String->Boolean(false)", new String[] {"false", " false ", " FaLsE"},
                new String[] {"Boolean", "java.lang.Boolean"}, Boolean.FALSE);
        simpleTypeOrObjectConvertTestSingleMulti("String->Locale", "en_us", new String[] {"Locale", "java.util.Locale"},
                localeData.goodLocale);
        simpleTypeOrObjectConvertTestError("String->error-Locale", "o", new String[] {"Locale", "java.util.Locale"});
        // TZ can never be null, will default to GMT if it can't be parsed(from the javadocs of java.util.TimeZone)
        simpleTypeOrObjectConvertTestSingleMulti("String->TimeZone", "Pacific/Wake",
                new String[] {"TimeZone", "java.util.TimeZone"}, localeData.goodTimeZone);
        simpleTypeOrObjectConvertTestSingleMulti("String->BigDecimal", "78,125E-2",
                new String[] {"BigDecimal", "java.math.BigDecimal"}, null, localeData, dcml);
        simpleTypeOrObjectConvertTestError("String->error-BigDecimal", "o",
                new String[] {"BigDecimal", "java.math.BigDecimal"});
        simpleTypeOrObjectConvertTestSingleMulti("String->Double", "78,125E-2",
                new String[] {"Double", "java.lang.Double"}, null, localeData, dbl);
        simpleTypeOrObjectConvertTestError("String->error-Double", "o",
                new String[] {"Double", "java.lang.Double"});
        simpleTypeOrObjectConvertTestSingleMulti("String->Float", "78,125E-2",
                new String[] {"Float", "java.lang.Float"}, null, localeData, flt);
        simpleTypeOrObjectConvertTestError("String->error-Float", "o", new String[] {"Float", "java.lang.Float"});
        simpleTypeOrObjectConvertTestSingleMulti("String->Long", "78,125E-2",
                new String[] {"Long", "java.lang.Long"}, null, localeData, lng);
        simpleTypeOrObjectConvertTestError("String->error-Long", "o", new String[] {"Long", "java.lang.Long"});
        simpleTypeOrObjectConvertTestSingleMulti("String->Integer", "78,125E-2",
                new String[] {"Integer", "java.lang.Integer"}, null, localeData, intg);
        simpleTypeOrObjectConvertTestError("String->error-Integer", "o",
                new String[] {"Integer", "java.lang.Integer"});

        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Date", "1969-12-31",
                new String[] {"Date", "java.sql.Date"}, null, localeData, sqlDt);
        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Date", "1969-12-31",
                new String[] {"Date", "java.sql.Date"}, "", localeData, sqlDt);
        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Date", "12-31-1969",
                new String[] {"Date", "java.sql.Date"}, "MM-dd-yyyy", localeData, sqlDt);
        simpleTypeOrObjectConvertTestError("String->error-java.sql.Date", "o",
                new String[] {"Date", "java.sql.Date"});
        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Time", "12:34:56",
                new String[] {"Time", "java.sql.Time"}, null, localeData, sqlTm);
        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Time", "12:34:56",
                new String[] {"Time", "java.sql.Time"}, "", localeData, sqlTm);
        simpleTypeOrObjectConvertTestSingleMulti("String->java.sql.Time", "563412",
                new String[] {"Time", "java.sql.Time"}, "ssmmHH", localeData, sqlTm);
        simpleTypeOrObjectConvertTestError("String->error-java.sql.Time", "o",
                new String[] {"Time", "java.sql.Time"});
        simpleTypeOrObjectConvertTestSingleMulti("String->Timestamp", "1970-01-01 12:00:00.123",
                new String[] {"Timestamp", "java.sql.Timestamp"}, null, localeData, ntstmp);
        simpleTypeOrObjectConvertTestSingleMulti("String->Timestamp", "1970-01-01 12:00:00.123",
                new String[] {"Timestamp", "java.sql.Timestamp"}, "", localeData, ntstmp);
        simpleTypeOrObjectConvertTestSingleMulti("String->Timestamp", "01-01-1970 12:00:00/123",
                new String[] {"Timestamp", "java.sql.Timestamp"}, "dd-MM-yyyy HH:mm:ss/SSS", localeData, ntstmp);
        simpleTypeOrObjectConvertTestMultiMulti("String->Timestamp",
                new String[] {"1970-01-01", "1970-01-01 00:00:00", "1970-01-01 00:00:00.0", "1970-01-01 00:00:00.000"},
                new String[] {"Timestamp", "java.sql.Timestamp"}, null, localeData, new Timestamp(-43200000));
        simpleTypeOrObjectConvertTestError("String->error-Timestamp", "o",
                new String[] {"Timestamp", "java.sql.Timestamp"});
        simpleTypeOrObjectConvertTestSingleMulti("String->List", "[one, two, three]",
                new String[] {"List", "List<java.lang.String>", "java.util.List"}, list);
        simpleTypeOrObjectConvertTestSingleMulti("String->List", "[one, two, three",
                new String[] {"List", "List<java.lang.String>", "java.util.List"}, Arrays.asList("[one, two, three"));
        simpleTypeOrObjectConvertTestSingleMulti("String->List", "one, two, three]",
                new String[] {"List", "List<java.lang.String>", "java.util.List"}, Arrays.asList("one, two, three]"));
        simpleTypeOrObjectConvertTestSingleMulti("String->Set", "[one, two, three]",
                new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, set);
        simpleTypeOrObjectConvertTestSingleMulti("String->Set", "[one, two, three",
                new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, UtilMisc.toSet("[one, two, three"));
        simpleTypeOrObjectConvertTestSingleMulti("String->Set", "one, two, three]",
                new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, UtilMisc.toSet("one, two, three]"));
        simpleTypeOrObjectConvertTestSingleMulti("String->Map", "{one=1, two=2, three=3}",
                new String[] {"Map", "Map<String, String>", "java.util.Map"}, map);
        simpleTypeOrObjectConvertTestError("String->Map(error-1)", "{one=1, two=2, three=3",
                new String[] {"Map", "java.util.Map"});
        simpleTypeOrObjectConvertTestError("String->Map(error-2)", "one=1, two=2, three=3}",
                new String[] {"Map", "java.util.Map"});
        simpleTypeOrObjectConvertTestSingleMulti("String->TimeDuration(number)", "3,661,001",
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, null, localeData, duration);
        simpleTypeOrObjectConvertTestMultiMulti("String->TimeDuration(string)", new String[] {"1:1:1:1"},
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("String->error-TimeDuration", "o",
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"});
    }

    @Test
    public void testDouble() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Double->String", Double.valueOf("1234567"),
                new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeOrObjectConvertTestSingleMulti("Double->BigDecimal", dbl,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, dcml);
        simpleTypeOrObjectConvertTestSingleMulti("Double->Double", dbl,
                new String[] {"Double", "java.lang.Double"},  Double.valueOf("781.25"));
        simpleTypeOrObjectConvertTestSingleMulti("Double->Float", dbl,
                new String[] {"Float", "java.lang.Float"}, flt);
        simpleTypeOrObjectConvertTestSingleMulti("Double->Long", dbl,
                new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeOrObjectConvertTestSingleMulti("Double->Integer", dbl,
                new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeOrObjectConvertTestSingleMulti("Double->List", dbl,
                new String[] {"List", "List<java.lang.Double>", "java.util.List"}, Arrays.asList(dbl));
        simpleTypeOrObjectConvertTestSingleMulti("Double->Set", dbl,
                new String[] {"Set", "Set<java.lang.Double>", "java.util.Set"}, UtilMisc.toSet(dbl));
        simpleTypeOrObjectConvertTestSingleMulti("Double->TimeDuration", Double.valueOf("3661001.25"),
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("Double->error", dbl, new String[] {});
    }

    @Test
    public void testFloat() throws GeneralException {
        // does not support to java.lang variants
        simpleTypeOrObjectConvertTestSingleMulti("Float->String", Float.valueOf("1234567"), new String[] {"String"},
                null, localeData, "1,234,567");
        simpleTypeOrObjectConvertTestSingleMulti("Float->BigDecimal", flt,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, dcml);
        simpleTypeOrObjectConvertTestSingleMulti("Float->Double", flt,
                new String[] {"Double", "java.lang.Double"}, dbl);
        simpleTypeOrObjectConvertTestSingleMulti("Float->Float", flt,
                new String[] {"Float", "java.lang.Float"}, Float.valueOf("781.25"));
        simpleTypeOrObjectConvertTestSingleMulti("Float->Long", flt,
                new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeOrObjectConvertTestSingleMulti("Float->Integer", flt,
                new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeOrObjectConvertTestSingleMulti("Float->List", flt,
                new String[] {"List", "List<java.lang.Float>", "java.util.List"}, Arrays.asList(flt));
        simpleTypeOrObjectConvertTestSingleMulti("Float->Set", flt,
                new String[] {"Set", "Set<java.lang.Float>", "java.util.Set"}, UtilMisc.toSet(flt));
        simpleTypeOrObjectConvertTestSingleMulti("Float->TimeDuration", Float.valueOf("3661001.25"),
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("Float->error", flt, new String[] {});
    }

    @Test
    public void testLong() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Long->String", Long.valueOf("1234567"),
                new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeOrObjectConvertTestSingleMulti("Long->BigDecimal", lng,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Long->Double", lng,
                new String[] { "Double", "java.lang.Double" }, Double.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Long->Float", lng,
                new String[] { "Float", "java.lang.Float" }, Float.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Long->Long", lng,
                new String[] { "Long", "java.lang.Long" }, Long.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Long->Integer", lng,
                new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeOrObjectConvertTestSingleMulti("Long->List", lng,
                new String[] {"List", "List<java.lang.Long>", "java.util.List"}, Arrays.asList(lng));
        simpleTypeOrObjectConvertTestSingleMulti("Long->Set", lng,
                new String[] {"Set", "Set<java.lang.Long>", "java.util.Set"}, UtilMisc.toSet(lng));
        simpleTypeOrObjectConvertTestSingleMulti("Long->java.util.Date", 781L,
                new String[] {"java.util.Date"}, utlDt);
        simpleTypeOrObjectConvertTestSingleMulti("Long->Timestamp", lng,
                new String[] {"Timestamp", "java.sql.Timestamp"}, tstmp);
        simpleTypeOrObjectConvertTestSingleMulti("Long->TimeDuration", Long.valueOf("3661001"),
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("Long->error", lng, new String[] {});
    }

    @Test
    public void testInteger() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Integer->String", Integer.valueOf("1234567"),
                new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeOrObjectConvertTestSingleMulti("Integer->BigDecimal", intg,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->Double", intg,
                new String[] { "Double", "java.lang.Double" }, Double.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->Float", intg,
                new String[] { "Float", "java.lang.Float" }, Float.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->Long", intg,
                new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeOrObjectConvertTestSingleMulti("Integer->Integer", intg,
                new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("781"));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->List", intg,
                new String[] {"List", "List<java.lang.Integer>", "java.util.List"}, Arrays.asList(intg));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->Set", intg,
                new String[] {"Set", "Set<java.lang.Integer>", "java.util.Set"}, UtilMisc.toSet(intg));
        simpleTypeOrObjectConvertTestSingleMulti("Integer->TimeDuration", Integer.valueOf("3661001"),
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("Integer->error", intg, new String[] {});
    }

    @Test
    public void testBigDecimal() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->String", new BigDecimal("12345.67"),
                new String[] {"String", "java.lang.String"}, null, localeData, "12,345.67");
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->BigDecimal", dcml,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781.25"));
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->Double", dcml,
                new String[] {"Double", "java.lang.Double"}, dbl);
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->Float", dcml,
                new String[] {"Float", "java.lang.Float"}, flt);
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->Long", dcml,
                new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->Integer", dcml,
                new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->List", dcml,
                new String[] {"List", "List<java.math.BigDecimal>", "java.util.List"}, Arrays.asList(dcml));
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->Set", dcml,
                new String[] {"Set", "Set<java.math.BigDecimal>", "java.util.Set"}, UtilMisc.toSet(dcml));
        simpleTypeOrObjectConvertTestSingleMulti("BigDecimal->TimeDuration", new BigDecimal("3661001"),
                new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeOrObjectConvertTestError("BigDecimal->error", dcml, new String[] {});
    }

    @Test
    public void testSqlDate() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->String", sqlDt,
                new String[] {"String", "java.lang.String"}, null, localeData, "1969-12-31");
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->String", sqlDt,
                new String[] {"String", "java.lang.String"}, "", localeData, "1969-12-31");
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->String", sqlDt,
                new String[] {"String", "java.lang.String"}, "dd-MM-yyyy", localeData, "31-12-1969");
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->SqlDate", sqlDt,
                new String[] {"Date", "java.sql.Date"}, new java.sql.Date(-129600000));
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->Timestamp", sqlDt,
                new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(-129600000));
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->List", sqlDt,
                new String[] {"List", "List<java.sql.Date>", "java.util.List"}, Arrays.asList(sqlDt));
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->Set", sqlDt,
                new String[] {"Set", "Set<java.sql.Date>", "java.util.Set"}, UtilMisc.toSet(sqlDt));
        simpleTypeOrObjectConvertTestSingleMulti("SqlDate->Long", sqlDt,
                new String[] {"Long", "java.lang.Long"}, Long.valueOf("-129600000"));
        simpleTypeOrObjectConvertTestError("SqlDate->error", sqlDt,
                new String[] {"Time", "java.sql.Time"});
    }

    @Test
    public void testSqlTime() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->String", sqlTm,
                new String[] {"String", "java.lang.String"}, null, localeData, "12:34:56");
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->String", sqlTm,
                new String[] {"String", "java.lang.String"}, "", localeData, "12:34:56");
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->String", sqlTm,
                new String[] {"String", "java.lang.String"}, "ss:mm:HH", localeData, "56:34:12");
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->SqlTime", sqlTm,
                new String[] {"Time", "java.sql.Time"}, new java.sql.Time(2096000));
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->Timestamp", sqlTm,
                new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(2096000));
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->List", sqlTm,
                new String[] {"List", "List<java.sql.Time>", "java.util.List"}, Arrays.asList(sqlTm));
        simpleTypeOrObjectConvertTestSingleMulti("SqlTime->Set", sqlTm,
                new String[] {"Set", "Set<java.sql.Time>", "java.util.Set"}, UtilMisc.toSet(sqlTm));
        simpleTypeOrObjectConvertTestError("SqlTime->error", sqlTm,
                new String[] {"Date", "java.sql.Date"});
    }

    @Test
    public void testTimestamp() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->String", tstmp,
                new String[] {"String", "java.lang.String"}, null, localeData, "1970-01-01 12:00:00.781");
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->String", tstmp,
                new String[] {"String", "java.lang.String"}, "", localeData, "1970-01-01 12:00:00.781");
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->String", tstmp,
                new String[] {"String", "java.lang.String"}, "dd-MM-yyyy HH:mm:ss/SSS", localeData,
                "01-01-1970 12:00:00/781");
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->Date", tstmp,
                new String[] {"Date", "java.sql.Date"}, new java.sql.Date(781));
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->Time", tstmp,
                new String[] {"Time", "java.sql.Time"}, new java.sql.Time(781));
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->Timestamp", tstmp,
                new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(781));
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->List", tstmp,
                new String[] {"List", "List<java.sql.Timestamp>", "java.util.List"}, Arrays.asList(tstmp));
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->Set", tstmp,
                new String[] {"Set", "Set<java.sql.Timestamp>", "java.util.Set"}, UtilMisc.toSet(tstmp));
        simpleTypeOrObjectConvertTestSingleMulti("Timestamp->Long", tstmp,
                new String[] {"Long", "java.lang.Long"}, Long.valueOf("781"));
        simpleTypeOrObjectConvertTestError("Timestamp->error", tstmp, new String[] {});
    }

    @Test
    public void testBoolean() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->Boolean", true,
                new String[] {"Boolean", "java.lang.Boolean"}, Boolean.TRUE);
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->Boolean", false,
                new String[] {"Boolean", "java.lang.Boolean"}, Boolean.FALSE);
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->String", true,
                new String[] {"String", "java.lang.String"}, "true");
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->String", false,
                new String[] {"String", "java.lang.String"}, "false");
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->Integer", true,
                new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("1"));
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->Integer", false,
                new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("0"));
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->List", true,
                new String[] {"List", "List<java.lang.Boolean>", "java.util.List"}, Arrays.asList(true));
        simpleTypeOrObjectConvertTestSingleMulti("Boolean->Set", true,
                new String[] {"Set", "Set<java.lang.Boolean>", "java.util.Set"}, UtilMisc.toSet(true));
        simpleTypeOrObjectConvertTestError("Boolean->error", true, new String[] {});
    }

    @Test
    public void testLocale() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Locale->Locale", localeData.goodLocale,
                new String[] {"Locale", "java.util.Locale"}, localeData.goodLocale);
        simpleTypeOrObjectConvertTestSingleMulti("Locale->String", localeData.goodLocale,
                new String[] {"String", "java.lang.String"}, localeData.goodLocale.toString());
        simpleTypeOrObjectConvertTestError("Locale->error", localeData.goodLocale, new String[] {});
    }

    @Test
    public void testTimeZone() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("TimeZone->TimeZone", localeData.goodTimeZone,
                new String[] {"TimeZone", "java.util.TimeZone"}, localeData.goodTimeZone);
        simpleTypeOrObjectConvertTestSingleMulti("TimeZone->String", localeData.goodTimeZone,
                new String[] {"String", "java.lang.String"}, localeData.goodTimeZone.getID());
        simpleTypeOrObjectConvertTestError("TimeZone->error", localeData.goodTimeZone, new String[] {});
    }


    @Test
    public void testMap() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("Map->Map", map,
                new String[] {"Map", "java.util.Map"}, UtilMisc.toMap("one", "1", "two", "2", "three", "3"));
        simpleTypeOrObjectConvertTestSingleMulti("Map->String", map,
                new String[] {"String", "java.lang.String"}, "{one=1, two=2, three=3}");
        simpleTypeOrObjectConvertTestSingleMulti("Map->List", map,
                new String[] {"List", "List<java.util.Map>", "java.util.List"}, Arrays.asList(map));
        simpleTypeOrObjectConvertTestSingleMulti("Map->Set", map,
                new String[] {"Set", "Set<java.util.Map>", "java.util.Set"}, UtilMisc.toSet(map));
        simpleTypeOrObjectConvertTestError("Map->error", map, new String[] {});
    }

    @Test
    public void testList() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("List->String", list,
                new String[] {"String", "java.lang.String"}, "[one, two, three]");
        simpleTypeOrObjectConvertTestSingleMulti("List->List", list,
                new String[] {"List", "java.util.List"}, Arrays.asList("one", "two", "three"));
        simpleTypeOrObjectConvertTestError("List->error", list, new String[] {});
    }


    // Node tests are done for all String-> conversions
    // org.w3c.dom.Node

    @Test
    public void testTimeDuration() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->String", duration,
                new String[] {"String", "java.lang.String"}, "0:0:0:1:1:1:1");
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->BigDecimal", duration,
                new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("3661001"));
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->Double", duration,
                new String[] {"Double", "java.lang.Double"}, Double.valueOf("3661001"));
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->Float", duration,
                new String[] {"Float", "java.lang.Float"}, Float.valueOf("3661001"));
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->Long", duration,
                new String[] {"Long", "java.lang.Long"}, Long.valueOf("3661001"));
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->List", duration,
                new String[] {"List", "java.util.List"}, Arrays.asList(duration));
        simpleTypeOrObjectConvertTestSingleMulti("TimeDuration->Set", duration,
                new String[] {"Set", "java.util.Set"}, UtilMisc.toSet(duration));
        simpleTypeOrObjectConvertTestError("TimeDuration->error", duration, new String[] {});
    }

    @Test
    public void testOther() throws GeneralException {
        simpleTypeOrObjectConvertTestSingleMulti("this->String", this, new String[] {"String", "java.lang.String"},
                this.toString());
        simpleTypeOrObjectConvertTestError("this->error", this, new String[] {"List", "Map", "Date"});
        simpleTypeOrObjectConvertTestNoError("this->no-error", this, new String[] {"List", "Map", "Date"});
    }
}
