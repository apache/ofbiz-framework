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
package org.apache.ofbiz.base.util.test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.TimeDuration;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;

import com.ibm.icu.util.Calendar;

@SourceMonitored
public class ObjectTypeTests extends GenericTestCaseBase {
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

    public ObjectTypeTests(String name) {
        super(name);
        ntstmp = new Timestamp(781L);
        ntstmp.setNanos(123000000);
        list = new ArrayList<Object>();
        list.add("one");
        list.add("two");
        list.add("three");
        map = new LinkedHashMap<String, Object>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        set = new LinkedHashSet<Object>(list);
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

    public static Object simpleTypeConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail) throws GeneralException {
        return ObjectType.simpleTypeConvert(obj, type, format, timeZone, locale, noTypeFail);
    }

    public static void simpleTypeConvertTest(String label, Object toConvert, String type, Object wanted) throws GeneralException {
        basicTest(label, toConvert);
        assertEquals(label + ":null target type", toConvert, simpleTypeConvert(toConvert, null, null, null, null, true));
        assertEquals(label + ":null source object", (Object) null, simpleTypeConvert(null, type, null, null, null, true));
        assertEquals(label, wanted, simpleTypeConvert(toConvert, type, null, null, null, true));
        if (toConvert instanceof String) {
            String str = (String) toConvert;
            Document doc = UtilXml.makeEmptyXmlDocument();
            assertEquals(label + ":text-node proxy", wanted, simpleTypeConvert(doc.createTextNode(str), type, null, null, null, true));
        }
    }

    public static void simpleTypeConvertTest(String label, Object toConvert, String type, String format, LocaleData localeData, Object wanted) throws GeneralException {
        basicTest(label, toConvert);
        Locale defaultLocale = Locale.getDefault();
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(localeData.goodLocale);
            TimeZone.setDefault(localeData.goodTimeZone);
            assertEquals(label + ":default-timezone/locale", wanted, simpleTypeConvert(toConvert, type, format, null, null, true));
            assertNotEquals(label + ":bad-passed-timezone/locale", wanted, simpleTypeConvert(toConvert, type, format, localeData.badTimeZone, localeData.badLocale, true));
            Locale.setDefault(localeData.badLocale);
            TimeZone.setDefault(localeData.badTimeZone);
            assertNotEquals(label + ":bad-default-timezone/locale", wanted, simpleTypeConvert(toConvert, type, format, null, null, true));
            assertEquals(label + ":passed-timezone/locale", wanted, simpleTypeConvert(toConvert, type, format, localeData.goodTimeZone, localeData.goodLocale, true));
        } finally {
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    public static void simpleTypeConvertTestSingleMulti(String label, Object toConvert, String[] types, Object wanted) throws GeneralException {
        for (int j = 0; j < types.length; j++) {
            simpleTypeConvertTest(label + "(:" + j + ")", toConvert, types[j], wanted);
        }
    }

    public static void simpleTypeConvertTestMultiMulti(String label, Object[] toConvert, String[] types, Object wanted) throws GeneralException {
        for (int i = 0; i < toConvert.length; i++) {
            for (int j = 0; j < types.length; j++) {
                simpleTypeConvertTest(label + "(" + i + ":" + j + ")", toConvert[i], types[j], wanted);
            }
        }
    }

    public static void simpleTypeConvertTestSingleMulti(String label, Object toConvert, String[] types, String format, LocaleData localeData, Object wanted) throws GeneralException {
        for (int j = 0; j < types.length; j++) {
            simpleTypeConvertTest(label + "(:" + j + ")", toConvert, types[j], format, localeData, wanted);
        }
    }

    public static void simpleTypeConvertTestMultiMulti(String label, Object[] toConvert, String[] types, String format, LocaleData localeData, Object wanted) throws GeneralException {
        for (int i = 0; i < toConvert.length; i++) {
            for (int j = 0; j < types.length; j++) {
                simpleTypeConvertTest(label + "(" + i + ":" + j + ")", toConvert[i], types[j], format, localeData, wanted);
            }
        }
    }

    public static void simpleTypeConvertTestError(String label, Object toConvert, String type) throws GeneralException {
        GeneralException caught = null;
        try {
            simpleTypeConvert(toConvert, type, null, null, null, true);
        } catch (GeneralException e) {
            caught = e;
        } finally {
            assertNotNull(label + ":caught", caught);
        }
    }

    public static void simpleTypeConvertTestError(String label, Object toConvert, String[] types) throws GeneralException {
        simpleTypeConvertTestError(label + ":this", toConvert, GeneralException.class.getName());
        for (String type: types) {
            simpleTypeConvertTestError(label + ":" + type, toConvert, type);
        }
    }

    public static void simpleTypeConvertTestNoError(String label, Object toConvert, String type) throws GeneralException {
        assertSame(label, toConvert, simpleTypeConvert(toConvert, type, null, null, null, false));
    }

    public static void simpleTypeConvertTestNoError(String label, Object toConvert, String[] types) throws GeneralException {
        simpleTypeConvertTestNoError(label + ":this", toConvert, GeneralException.class.getName());
        for (String type: types) {
            simpleTypeConvertTestNoError(label + ":" + type, toConvert, type);
        }
    }

    public static void basicTest(String label, Object toConvert) throws GeneralException {
        assertEquals(label + ":PlainString", toConvert.toString(), simpleTypeConvert(toConvert, "PlainString", null, null, null, true));
        assertSame(label + ":same", toConvert, simpleTypeConvert(toConvert, toConvert.getClass().getName(), null, null, null, true));
        assertSame(label + ":to-Object", toConvert, simpleTypeConvert(toConvert, "Object", null, null, null, true));
        assertSame(label + ":to-java.lang.Object", toConvert, simpleTypeConvert(toConvert, "java.lang.Object", null, null, null, true));
    }

    public void testLoadClassWithNonExistentClass() {
        Exception exception = null;
        try {
            ObjectType.loadClass("foobarbaz");
        } catch (Exception e) {
            exception = e;
        }
        assertTrue("Exception thrown by loadClass(\"foobarbaz\") is not ClassNotFoundException", exception instanceof ClassNotFoundException);
    }

    public void testLoadClassWithPrimitives() {
        try {
            Class<?> theClass;
            theClass = ObjectType.loadClass("boolean");
            assertEquals("Wrong class returned by loadClass(\"boolean\")", (Boolean.TYPE).getName(), theClass.getName());
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
            assertEquals("Wrong class returned by loadClass(\"char\")", (Character.TYPE).getName(), theClass.getName());
        } catch (Exception e) {
            fail("Exception thrown by loadClass: " + e.getMessage());
        }
    }

    public void testLoadClassWithAlias() {
        try {
            Class<?> theClass;
            // first try with a class full name
            theClass = ObjectType.loadClass("java.lang.String");
            assertEquals("Wrong class returned by loadClass(\"java.lang.String\")", "java.lang.String", theClass.getName());
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

    public void testClassNotFound() {
        GeneralException caught = null;
        try {
            ObjectType.simpleTypeConvert(this, "foobarbaz", null, null, null, false);
        } catch (GeneralException e) {
            caught = e;
        } finally {
            assertNotNull("class not found", caught);
        }
    }

    public void testArray() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Object[]->List", new Object[] {"one", "two", "three"}, new String[] {"List", "java.util.List"}, list);
        simpleTypeConvertTestSingleMulti("int[]->List", new int[] {1, 2, 3}, new String[] {"List", "java.util.List"}, list(1, 2, 3));
        simpleTypeConvertTestError("Object[]->error", new Object[] {"one", "two", "three"}, new String[] {"Map"});
        simpleTypeConvertTestError("int[]->error", new int[] {1, 2, 3}, new String[] {"java.util.ArrayList", "Map"});
    }

    public void testString() throws GeneralException, Exception {
        simpleTypeConvertTest("String->String", "one", "String", "one");
        simpleTypeConvertTest("String->String", "one", "java.lang.String", "one");
        simpleTypeConvertTestSingleMulti("empty-String->anything", "", new String[] {"List", "Map"}, null);
        simpleTypeConvertTestError("String->error", "one", new String[] {});
        simpleTypeConvertTestMultiMulti("String->Boolean(true)", new String[] {"true", " true ", " TrUe"}, new String[] {"Boolean", "java.lang.Boolean"}, Boolean.TRUE);
        simpleTypeConvertTestMultiMulti("String->Boolean(false)", new String[] {"false", " false ", " FaLsE"}, new String[] {"Boolean", "java.lang.Boolean"}, Boolean.FALSE);
        simpleTypeConvertTestSingleMulti("String->Locale", "en_us", new String[] {"Locale", "java.util.Locale"}, localeData.goodLocale);
        simpleTypeConvertTestError("String->error-Locale", "o", new String[] {"Locale", "java.util.Locale"});
        // TZ can never be null, will default to GMT if it can't be parsed(from the javadocs of java.util.TimeZone)
        simpleTypeConvertTestSingleMulti("String->TimeZone", "Pacific/Wake", new String[] {"TimeZone", "java.util.TimeZone"}, localeData.goodTimeZone);
        simpleTypeConvertTestSingleMulti("String->BigDecimal", "78,125E-2", new String[] {"BigDecimal", "java.math.BigDecimal"}, null, localeData, dcml);
        simpleTypeConvertTestError("String->error-BigDecimal", "o", new String[] {"BigDecimal", "java.math.BigDecimal"});
        simpleTypeConvertTestSingleMulti("String->Double", "78,125E-2", new String[] {"Double", "java.lang.Double"}, null, localeData, dbl);
        simpleTypeConvertTestError("String->error-Double", "o", new String[] {"Double", "java.lang.Double"});
        simpleTypeConvertTestSingleMulti("String->Float", "78,125E-2", new String[] {"Float", "java.lang.Float"}, null, localeData, flt);
        simpleTypeConvertTestError("String->error-Float", "o", new String[] {"Float", "java.lang.Float"});
        simpleTypeConvertTestSingleMulti("String->Long", "78,125E-2", new String[] {"Long", "java.lang.Long"}, null, localeData, lng);
        simpleTypeConvertTestError("String->error-Long", "o", new String[] {"Long", "java.lang.Long"});
        simpleTypeConvertTestSingleMulti("String->Integer", "78,125E-2", new String[] {"Integer", "java.lang.Integer"}, null, localeData, intg);
        simpleTypeConvertTestError("String->error-Integer", "o", new String[] {"Integer", "java.lang.Integer"});

        simpleTypeConvertTestSingleMulti("String->java.sql.Date", "1969-12-31", new String[] {"Date", "java.sql.Date"}, null, localeData, sqlDt);
        simpleTypeConvertTestSingleMulti("String->java.sql.Date", "1969-12-31", new String[] {"Date", "java.sql.Date"}, "", localeData, sqlDt);
        simpleTypeConvertTestSingleMulti("String->java.sql.Date", "12-31-1969", new String[] {"Date", "java.sql.Date"}, "MM-dd-yyyy", localeData, sqlDt);
        simpleTypeConvertTestError("String->error-java.sql.Date", "o", new String[] {"Date", "java.sql.Date"});
        simpleTypeConvertTestSingleMulti("String->java.sql.Time", "12:34:56", new String[] {"Time", "java.sql.Time"}, null, localeData, sqlTm);
        simpleTypeConvertTestSingleMulti("String->java.sql.Time", "12:34:56", new String[] {"Time", "java.sql.Time"}, "", localeData, sqlTm);
        simpleTypeConvertTestSingleMulti("String->java.sql.Time", "563412", new String[] {"Time", "java.sql.Time"}, "ssmmHH", localeData, sqlTm);
        simpleTypeConvertTestError("String->error-java.sql.Time", "o", new String[] {"Time", "java.sql.Time"});
        simpleTypeConvertTestSingleMulti("String->Timestamp", "1970-01-01 12:00:00.123", new String[] {"Timestamp", "java.sql.Timestamp"}, null, localeData, ntstmp);
        simpleTypeConvertTestSingleMulti("String->Timestamp", "1970-01-01 12:00:00.123", new String[] {"Timestamp", "java.sql.Timestamp"}, "", localeData, ntstmp);
        simpleTypeConvertTestSingleMulti("String->Timestamp", "01-01-1970 12:00:00/123", new String[] {"Timestamp", "java.sql.Timestamp"}, "dd-MM-yyyy HH:mm:ss/SSS", localeData, ntstmp);
        simpleTypeConvertTestMultiMulti("String->Timestamp", new String[] {"1970-01-01", "1970-01-01 00:00:00", "1970-01-01 00:00:00.0", "1970-01-01 00:00:00.000"}, new String[] {"Timestamp", "java.sql.Timestamp"}, null, localeData, new Timestamp(-43200000));
        simpleTypeConvertTestError("String->error-Timestamp", "o", new String[] {"Timestamp", "java.sql.Timestamp"});
        simpleTypeConvertTestSingleMulti("String->List", "[one, two, three]", new String[] {"List", "List<java.lang.String>", "java.util.List"}, list);
        simpleTypeConvertTestSingleMulti("String->List", "[one, two, three", new String[] {"List", "List<java.lang.String>", "java.util.List"}, list("[one, two, three"));
        simpleTypeConvertTestSingleMulti("String->List", "one, two, three]", new String[] {"List", "List<java.lang.String>", "java.util.List"}, list("one, two, three]"));
        simpleTypeConvertTestSingleMulti("String->Set", "[one, two, three]", new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, set);
        simpleTypeConvertTestSingleMulti("String->Set", "[one, two, three", new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, set("[one, two, three"));
        simpleTypeConvertTestSingleMulti("String->Set", "one, two, three]", new String[] {"Set", "Set<java.lang.String>", "java.util.Set"}, set("one, two, three]"));
        simpleTypeConvertTestSingleMulti("String->Map", "{one=1, two=2, three=3}", new String[] {"Map", "Map<String, String>", "java.util.Map"}, map);
        simpleTypeConvertTestError("String->Map(error-1)", "{one=1, two=2, three=3", new String[] {"Map", "java.util.Map"});
        simpleTypeConvertTestError("String->Map(error-2)", "one=1, two=2, three=3}", new String[] {"Map", "java.util.Map"});
        simpleTypeConvertTestSingleMulti("String->TimeDuration(number)", "3,661,001", new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, null, localeData, duration);
        simpleTypeConvertTestMultiMulti("String->TimeDuration(string)", new String[] {"1:1:1:1"}, new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("String->error-TimeDuration", "o", new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"});
    }

    public void testDouble() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Double->String", Double.valueOf("1234567"), new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeConvertTestSingleMulti("Double->BigDecimal", dbl, new String[] {"BigDecimal", "java.math.BigDecimal"}, dcml);
        simpleTypeConvertTestSingleMulti("Double->Double", dbl, new String[] {"Double", "java.lang.Double"}, new Double("781.25"));
        simpleTypeConvertTestSingleMulti("Double->Float", dbl, new String[] {"Float", "java.lang.Float"}, flt);
        simpleTypeConvertTestSingleMulti("Double->Long", dbl, new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeConvertTestSingleMulti("Double->Integer", dbl, new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeConvertTestSingleMulti("Double->List", dbl, new String[] {"List", "List<java.lang.Double>", "java.util.List"}, list(dbl));
        simpleTypeConvertTestSingleMulti("Double->Set", dbl, new String[] {"Set", "Set<java.lang.Double>", "java.util.Set"}, set(dbl));
        simpleTypeConvertTestSingleMulti("Double->TimeDuration", Double.valueOf("3661001.25"), new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("Double->error", dbl, new String[] {});
    }

    public void testFloat() throws GeneralException {
        // does not support to java.lang variants
        simpleTypeConvertTestSingleMulti("Float->String", Float.valueOf("1234567"), new String[] {"String"}, null, localeData, "1,234,567");
        simpleTypeConvertTestSingleMulti("Float->BigDecimal", flt, new String[] {"BigDecimal", "java.math.BigDecimal"}, dcml);
        simpleTypeConvertTestSingleMulti("Float->Double", flt, new String[] {"Double", "java.lang.Double"}, dbl);
        simpleTypeConvertTestSingleMulti("Float->Float", flt, new String[] {"Float", "java.lang.Float"}, new Float("781.25"));
        simpleTypeConvertTestSingleMulti("Float->Long", flt, new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeConvertTestSingleMulti("Float->Integer", flt, new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeConvertTestSingleMulti("Float->List", flt, new String[] {"List", "List<java.lang.Float>", "java.util.List"}, list(flt));
        simpleTypeConvertTestSingleMulti("Float->Set", flt, new String[] {"Set", "Set<java.lang.Float>", "java.util.Set"}, set(flt));
        simpleTypeConvertTestSingleMulti("Float->TimeDuration", Float.valueOf("3661001.25"), new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("Float->error", flt, new String[] {});
    }

    public void testLong() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Long->String", Long.valueOf("1234567"), new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeConvertTestSingleMulti("Long->BigDecimal", lng, new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781"));
        simpleTypeConvertTestSingleMulti("Long->Double", lng, new String[] {"Double", "java.lang.Double"}, new Double("781"));
        simpleTypeConvertTestSingleMulti("Long->Float", lng, new String[] {"Float", "java.lang.Float"}, new Float("781"));
        simpleTypeConvertTestSingleMulti("Long->Long", lng, new String[] {"Long", "java.lang.Long"}, new Long("781"));
        simpleTypeConvertTestSingleMulti("Long->Integer", lng, new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeConvertTestSingleMulti("Long->List", lng, new String[] {"List", "List<java.lang.Long>", "java.util.List"}, list(lng));
        simpleTypeConvertTestSingleMulti("Long->Set", lng, new String[] {"Set", "Set<java.lang.Long>", "java.util.Set"}, set(lng));
        simpleTypeConvertTestSingleMulti("Long->java.util.Date", 781L, new String[] {"java.util.Date"}, utlDt);
        simpleTypeConvertTestSingleMulti("Long->Timestamp", lng, new String[] {"Timestamp", "java.sql.Timestamp"}, tstmp);
        simpleTypeConvertTestSingleMulti("Long->TimeDuration", Long.valueOf("3661001"), new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("Long->error", lng, new String[] {});
    }

    public void testInteger() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Integer->String", Integer.valueOf("1234567"), new String[] {"String", "java.lang.String"}, null, localeData, "1,234,567");
        simpleTypeConvertTestSingleMulti("Integer->BigDecimal", intg, new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781"));
        simpleTypeConvertTestSingleMulti("Integer->Double", intg, new String[] {"Double", "java.lang.Double"}, new Double("781"));
        simpleTypeConvertTestSingleMulti("Integer->Float", intg, new String[] {"Float", "java.lang.Float"}, new Float("781"));
        simpleTypeConvertTestSingleMulti("Integer->Long", intg, new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeConvertTestSingleMulti("Integer->Integer", intg, new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("781"));
        simpleTypeConvertTestSingleMulti("Integer->List", intg, new String[] {"List", "List<java.lang.Integer>", "java.util.List"}, list(intg));
        simpleTypeConvertTestSingleMulti("Integer->Set", intg, new String[] {"Set", "Set<java.lang.Integer>", "java.util.Set"}, set(intg));
        simpleTypeConvertTestSingleMulti("Integer->TimeDuration", Integer.valueOf("3661001"), new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("Integer->error", intg, new String[] {});
    }

    public void testBigDecimal() throws GeneralException {
        simpleTypeConvertTestSingleMulti("BigDecimal->String", new BigDecimal("12345.67"), new String[] {"String", "java.lang.String"}, null, localeData, "12,345.67");
        simpleTypeConvertTestSingleMulti("BigDecimal->BigDecimal", dcml, new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("781.25"));
        simpleTypeConvertTestSingleMulti("BigDecimal->Double", dcml, new String[] {"Double", "java.lang.Double"}, dbl);
        simpleTypeConvertTestSingleMulti("BigDecimal->Float", dcml, new String[] {"Float", "java.lang.Float"}, flt);
        simpleTypeConvertTestSingleMulti("BigDecimal->Long", dcml, new String[] {"Long", "java.lang.Long"}, lng);
        simpleTypeConvertTestSingleMulti("BigDecimal->Integer", dcml, new String[] {"Integer", "java.lang.Integer"}, intg);
        simpleTypeConvertTestSingleMulti("BigDecimal->List", dcml, new String[] {"List", "List<java.math.BigDecimal>", "java.util.List"}, list(dcml));
        simpleTypeConvertTestSingleMulti("BigDecimal->Set", dcml, new String[] {"Set", "Set<java.math.BigDecimal>", "java.util.Set"}, set(dcml));
        simpleTypeConvertTestSingleMulti("BigDecimal->TimeDuration", new BigDecimal("3661001"), new String[] {"TimeDuration", "org.apache.ofbiz.base.util.TimeDuration"}, duration);
        simpleTypeConvertTestError("BigDecimal->error", dcml, new String[] {});
    }

    public void testSqlDate() throws GeneralException {
        simpleTypeConvertTestSingleMulti("SqlDate->String", sqlDt, new String[] {"String", "java.lang.String"}, null, localeData, "1969-12-31");
        simpleTypeConvertTestSingleMulti("SqlDate->String", sqlDt, new String[] {"String", "java.lang.String"}, "", localeData, "1969-12-31");
        simpleTypeConvertTestSingleMulti("SqlDate->String", sqlDt, new String[] {"String", "java.lang.String"}, "dd-MM-yyyy", localeData, "31-12-1969");
        simpleTypeConvertTestSingleMulti("SqlDate->SqlDate", sqlDt, new String[] {"Date", "java.sql.Date"}, new java.sql.Date(-129600000));
        simpleTypeConvertTestSingleMulti("SqlDate->Timestamp", sqlDt, new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(-129600000));
        simpleTypeConvertTestSingleMulti("SqlDate->List", sqlDt, new String[] {"List", "List<java.sql.Date>", "java.util.List"}, list(sqlDt));
        simpleTypeConvertTestSingleMulti("SqlDate->Set", sqlDt, new String[] {"Set", "Set<java.sql.Date>", "java.util.Set"}, set(sqlDt));
        simpleTypeConvertTestSingleMulti("SqlDate->Long", sqlDt, new String[] {"Long", "java.lang.Long"}, Long.valueOf("-129600000"));
        simpleTypeConvertTestError("SqlDate->error", sqlDt, new String[] {"Time", "java.sql.Time"});
    }

    public void testSqlTime() throws GeneralException {
        simpleTypeConvertTestSingleMulti("SqlTime->String", sqlTm, new String[] {"String", "java.lang.String"}, null, localeData, "12:34:56");
        simpleTypeConvertTestSingleMulti("SqlTime->String", sqlTm, new String[] {"String", "java.lang.String"}, "", localeData, "12:34:56");
        simpleTypeConvertTestSingleMulti("SqlTime->String", sqlTm, new String[] {"String", "java.lang.String"}, "ss:mm:HH", localeData, "56:34:12");
        simpleTypeConvertTestSingleMulti("SqlTime->SqlTime", sqlTm, new String[] {"Time", "java.sql.Time"}, new java.sql.Time(2096000));
        simpleTypeConvertTestSingleMulti("SqlTime->Timestamp", sqlTm, new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(2096000));
        simpleTypeConvertTestSingleMulti("SqlTime->List", sqlTm, new String[] {"List", "List<java.sql.Time>", "java.util.List"}, list(sqlTm));
        simpleTypeConvertTestSingleMulti("SqlTime->Set", sqlTm, new String[] {"Set", "Set<java.sql.Time>", "java.util.Set"}, set(sqlTm));
        simpleTypeConvertTestError("SqlTime->error", sqlTm, new String[] {"Date", "java.sql.Date"});
    }

    public void testTimestamp() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Timestamp->String", tstmp, new String[] {"String", "java.lang.String"}, null, localeData, "1970-01-01 12:00:00.781");
        simpleTypeConvertTestSingleMulti("Timestamp->String", tstmp, new String[] {"String", "java.lang.String"}, "", localeData, "1970-01-01 12:00:00.781");
        simpleTypeConvertTestSingleMulti("Timestamp->String", tstmp, new String[] {"String", "java.lang.String"}, "dd-MM-yyyy HH:mm:ss/SSS", localeData, "01-01-1970 12:00:00/781");
        simpleTypeConvertTestSingleMulti("Timestamp->Date", tstmp, new String[] {"Date", "java.sql.Date"}, new java.sql.Date(781));
        simpleTypeConvertTestSingleMulti("Timestamp->Time", tstmp, new String[] {"Time", "java.sql.Time"}, new java.sql.Time(781));
        simpleTypeConvertTestSingleMulti("Timestamp->Timestamp", tstmp, new String[] {"Timestamp", "java.sql.Timestamp"}, new Timestamp(781));
        simpleTypeConvertTestSingleMulti("Timestamp->List", tstmp, new String[] {"List", "List<java.sql.Timestamp>", "java.util.List"}, list(tstmp));
        simpleTypeConvertTestSingleMulti("Timestamp->Set", tstmp, new String[] {"Set", "Set<java.sql.Timestamp>", "java.util.Set"}, set(tstmp));
        simpleTypeConvertTestSingleMulti("Timestamp->Long", tstmp, new String[] {"Long", "java.lang.Long"}, Long.valueOf("781"));
        simpleTypeConvertTestError("Timestamp->error", tstmp, new String[] {});
    }

    public void testBoolean() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Boolean->Boolean", true, new String[] {"Boolean", "java.lang.Boolean"}, Boolean.TRUE);
        simpleTypeConvertTestSingleMulti("Boolean->Boolean", false, new String[] {"Boolean", "java.lang.Boolean"}, Boolean.FALSE);
        simpleTypeConvertTestSingleMulti("Boolean->String", true, new String[] {"String", "java.lang.String"}, "true");
        simpleTypeConvertTestSingleMulti("Boolean->String", false, new String[] {"String", "java.lang.String"}, "false");
        simpleTypeConvertTestSingleMulti("Boolean->Integer", true, new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("1"));
        simpleTypeConvertTestSingleMulti("Boolean->Integer", false, new String[] {"Integer", "java.lang.Integer"}, Integer.valueOf("0"));
        simpleTypeConvertTestSingleMulti("Boolean->List", true, new String[] {"List", "List<java.lang.Boolean>", "java.util.List"}, list(true));
        simpleTypeConvertTestSingleMulti("Boolean->Set", true, new String[] {"Set", "Set<java.lang.Boolean>", "java.util.Set"}, set(true));
        simpleTypeConvertTestError("Boolean->error", true, new String[] {});
    }

    public void testLocale() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Locale->Locale", localeData.goodLocale, new String[] {"Locale", "java.util.Locale"}, localeData.goodLocale);
        simpleTypeConvertTestSingleMulti("Locale->String", localeData.goodLocale, new String[] {"String", "java.lang.String"}, localeData.goodLocale.toString());
        simpleTypeConvertTestError("Locale->error", localeData.goodLocale, new String[] {});
    }

    public void testTimeZone() throws GeneralException {
        simpleTypeConvertTestSingleMulti("TimeZone->TimeZone", localeData.goodTimeZone, new String[] {"TimeZone", "java.util.TimeZone"}, localeData.goodTimeZone);
        simpleTypeConvertTestSingleMulti("TimeZone->String", localeData.goodTimeZone, new String[] {"String", "java.lang.String"}, localeData.goodTimeZone.getID());
        simpleTypeConvertTestError("TimeZone->error", localeData.goodTimeZone, new String[] {});
    }


    public void testMap() throws GeneralException {
        simpleTypeConvertTestSingleMulti("Map->Map", map, new String[] {"Map", "java.util.Map"}, map("one", "1", "two", "2", "three", "3"));
        simpleTypeConvertTestSingleMulti("Map->String", map, new String[] {"String", "java.lang.String"}, "{one=1, two=2, three=3}");
        simpleTypeConvertTestSingleMulti("Map->List", map, new String[] {"List", "List<java.util.Map>", "java.util.List"}, list(map));
        simpleTypeConvertTestSingleMulti("Map->Set", map, new String[] {"Set", "Set<java.util.Map>", "java.util.Set"}, set(map));
        simpleTypeConvertTestError("Map->error", map, new String[] {});
    }

    public void testList() throws GeneralException {
        simpleTypeConvertTestSingleMulti("List->String", list, new String[] {"String", "java.lang.String"}, "[one, two, three]");
        simpleTypeConvertTestSingleMulti("List->List", list, new String[] {"List", "java.util.List"}, list("one", "two", "three"));
        simpleTypeConvertTestError("List->error", list, new String[] {});
    }


    // Node tests are done for all String-> conversions
    // org.w3c.dom.Node

    public void testTimeDuration() throws GeneralException {
        simpleTypeConvertTestSingleMulti("TimeDuration->String", duration, new String[] {"String", "java.lang.String"}, "0:0:0:1:1:1:1");
        simpleTypeConvertTestSingleMulti("TimeDuration->BigDecimal", duration, new String[] {"BigDecimal", "java.math.BigDecimal"}, new BigDecimal("3661001"));
        simpleTypeConvertTestSingleMulti("TimeDuration->Double", duration, new String[] {"Double", "java.lang.Double"}, Double.valueOf("3661001"));
        simpleTypeConvertTestSingleMulti("TimeDuration->Float", duration, new String[] {"Float", "java.lang.Float"}, Float.valueOf("3661001"));
        simpleTypeConvertTestSingleMulti("TimeDuration->Long", duration, new String[] {"Long", "java.lang.Long"}, Long.valueOf("3661001"));
        simpleTypeConvertTestSingleMulti("TimeDuration->List", duration, new String[] {"List", "java.util.List"}, list(duration));
        simpleTypeConvertTestSingleMulti("TimeDuration->Set", duration, new String[] {"Set", "java.util.Set"}, set(duration));
        simpleTypeConvertTestError("TimeDuration->error", duration, new String[] {});
    }

    public void testOther() throws GeneralException {
        simpleTypeConvertTestSingleMulti("this->String", this, new String[] {"String", "java.lang.String"}, this.toString());
        simpleTypeConvertTestError("this->error", this, new String[] {"List", "Map", "Date"});
        simpleTypeConvertTestNoError("this->no-error", this, new String[] {"List", "Map", "Date"});
    }
}
