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

import com.ibm.icu.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class StringUtilTests {

    @Test
    public void testInternString() {
        assertSame(StringUtil.internString("foo"), StringUtil.internString("foo"), "intern-constant");
        assertSame(StringUtil.internString("foo"), StringUtil.internString("foo"), "intern-new");
        assertSame(StringUtil.internString("foo"), StringUtil.internString(new String(new char[] {'f', 'o', 'o'})), "intern-char");
        assertSame(StringUtil.internString(null), StringUtil.internString(null), "intern-null");
    }

    @Test
    public void testReplaceString() {
        assertNull(StringUtil.replaceString(null, "old", "new"), "null");
        assertEquals("the old dog jumped over the old fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "", "new"), "empty old");
        assertEquals("the new dog jumped over the new fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "old", "new"), "replace");
        assertEquals("the  dog jumped over the  fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "old", null), "replace-null");
        assertEquals("the old dog jumped over the old fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "cat", "feline"), "replace-not-found");
    }

    @Test
    public void testJoin() {
        assertNull(StringUtil.join(null, ","), "null-list");
        assertNull(StringUtil.join(Collections.emptyList(), ","), "empty-list");
        assertEquals("1", StringUtil.join(Collections.singleton("1"), ","), "single");
        assertEquals("1,2", StringUtil.join(Arrays.asList("1", "2"), ","), "double");
    }

    @Test
    public void testSplit() {
        assertNull(StringUtil.split(null, ","), "null-string");
        assertEquals(Arrays.asList("1"), StringUtil.split("1", ","), "single");
        assertEquals(Arrays.asList("1", "2"), StringUtil.split("1,2", ","), "double");
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), StringUtil.split("1 2\t3\n4\r5\f6", null), "no-sep");
    }

    @Test
    public void testStrToMap() {
        assertNull(StringUtil.strToMap(null, false), "null-string");
        assertNull(StringUtil.strToMap("", false), "empty");
        assertEquals(Collections.emptyMap(), StringUtil.strToMap("1", false), "missing =");
        assertEquals(UtilMisc.toMap("1", "one"), StringUtil.strToMap("1=one"), "single");
        assertEquals(UtilMisc.toMap("2", "two", "1", "one"), StringUtil.strToMap("1=one|2=two"), "double");
        assertEquals(UtilMisc.toMap(" 2 ", " two ", " 1 ", " one "), StringUtil.strToMap(" 1 = one | 2 = two "), "double-no-trim");
        assertEquals(UtilMisc.toMap("2", "two", "1", "one"), StringUtil.strToMap(" 1 = one | 2 = two ", true), "double-trim");
    }

    @Test
    public void testMapToStr() {
        // Test Null
        assertNull(StringUtil.mapToStr(null), "null-string");
        assertNull(StringUtil.mapToStr(Map.of()), "empty");

        // Test simple case
        assertEquals("1=one", StringUtil.mapToStr(Map.of("1", "one")), "single");
        LinkedHashMap<String, String> doubleMap = new LinkedHashMap<>();
        doubleMap.put("1", "one");
        doubleMap.put("2", "two");
        assertEquals("1=one|2=two", StringUtil.mapToStr(doubleMap), "double");

        // Test with object case
        LinkedHashMap<Object, Object> doubleObjectMap = new LinkedHashMap<>();
        doubleObjectMap.put(Integer.valueOf(1), Long.valueOf(1));
        doubleObjectMap.put(Integer.valueOf(2), BigDecimal.ONE);
        assertEquals("1=1|2=1", StringUtil.mapToStr(doubleObjectMap), "double with number classe");

        // Test with special char
        assertEquals("1=%3Done", StringUtil.mapToStr(Map.of("1", "=one")), "single with =");
        LinkedHashMap<String, String> doublePipeMap = new LinkedHashMap<>();
        doublePipeMap.put("1", "|one");
        doublePipeMap.put("2|", "two");
        assertEquals("1=%7Cone|2%7C=two", StringUtil.mapToStr(doublePipeMap), "double with pipe");
    }

    @Test
    public void testToList() {
        for (String s: new String[] {"", "[", "]", "]["}) {
            IllegalArgumentException caught = null;
            try {
                StringUtil.toList(s);
            } catch (IllegalArgumentException e) {
                caught = e;
            } finally {
                assertNotNull(caught, "bad(" + s + ")");
            }
        }
        assertEquals(Arrays.asList("1"), StringUtil.toList("[1]"), "single");
        assertEquals(Arrays.asList("1", "2"), StringUtil.toList("[1, 2]"), "double");
        assertEquals(Arrays.asList(" 1", "2 "), StringUtil.toList("[ 1, 2 ]"), "double-space");
    }

    @Test
    public void testToSet() {
        for (String s: new String[] {"", "[", "]", "]["}) {
            IllegalArgumentException caught = null;
            try {
                StringUtil.toSet(s);
            } catch (IllegalArgumentException e) {
                caught = e;
            } finally {
                assertNotNull(caught, "bad(" + s + ")");
            }
        }
        assertEquals(UtilMisc.toSet("1"), StringUtil.toSet("[1]"), "single");
        assertEquals(UtilMisc.toSet("1", "2"), StringUtil.toSet("[1, 2]"), "double");
        assertEquals(UtilMisc.toSet(" 1", "2 "), StringUtil.toSet("[ 1, 2 ]"), "double-space");
    }

    @Test
    public void testCreateMap() {
        List<List<String>> badKeys = Arrays.asList(null, Arrays.asList("1"), Arrays.asList("2"));
        List<List<String>> badValues = Arrays.asList(Arrays.asList("one"), null, Arrays.asList("two", "extra"));
        for (int i = 0; i < badKeys.size(); i++) {
            IllegalArgumentException caught = null;
            try {
                StringUtil.createMap(badKeys.get(i), badValues.get(i));
            } catch (IllegalArgumentException e) {
                caught = e;
            } finally {
                assertNotNull(caught, "bad(" + i + ")");
            }
        }
        assertEquals(UtilMisc.toMap("1", "one", "2", "two"), StringUtil.createMap(Arrays.asList("1", "2"),
                Arrays.asList("one", "two")), "parse");
    }

    @Test
    public void testCleanUpPathPrefix() {
        assertEquals("", StringUtil.cleanUpPathPrefix(null), "null");
        assertEquals("", StringUtil.cleanUpPathPrefix(""), "empty");
        for (String s: new String[] {"\\a\\b\\c", "\\a\\b\\c\\",
                "a\\b\\c\\", "a\\b\\c", "/a/b/c", "/a/b/c/", "a/b/c/", "a/b/c"}) {
            assertEquals("/a/b/c", StringUtil.cleanUpPathPrefix(s), "cleanup(" + s + ")");
        }
    }

    @Test
    public void testRemoveSpaces() {
        assertEquals("", StringUtil.removeSpaces(""));
        assertEquals("abcd", StringUtil.removeSpaces(" a b c d "));
        assertEquals("a\\cd", StringUtil.removeSpaces(" a \\ c d "));
    }

    @Test
    public void testToHexString() {
        assertEquals("000102030405060708090a0b0c0d0e0f",
                StringUtil.toHexString(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}), "16 bytes");
    }

    @Test
    public void testCleanHexString() {
        assertEquals("rtwertetwretw", StringUtil.cleanHexString("rtwer:tetw retw"), "clean hex");
    }

    @Test
    public void testFromHexString() {
        assertArrayEquals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
                StringUtil.fromHexString("000102030405060708090a0b0c0d0e0f"), "16 bytes");
        GeneralRuntimeException caught = null;
        try {
            StringUtil.fromHexString("0-");
        } catch (GeneralRuntimeException e) {
            caught = e;
        } finally {
            assertNotNull(caught, "bad-char");
        }
    }

    @Test
    public void testEncodeInt() {
        assertArrayEquals(new char[] {'0', '5'}, StringUtil.encodeInt(5, 0, new char[2]), "one octet");
        assertArrayEquals(new char[] {'1', '5'}, StringUtil.encodeInt(21, 0, new char[2]), "two octets");
        // these next two are really weird, note the start offset being != 0.
        assertArrayEquals(new char[] {'3', '1', '5'}, StringUtil.encodeInt(789, 1, new char[3]), "three octets");
        assertArrayEquals(new char[] {'7', '3', '1', '5'}, StringUtil.encodeInt(29461, 2, new char[4]), "four octets");
    }

    @Test
    public void testRemoveNonNumeric() {
        assertEquals("12345", StringUtil.removeNonNumeric("a1'2;3]4!5("), "just numbers");
    }

    @Test
    public void testRemoveRegex() {
    }

    @Test
    public void testAddToNumberString() {
        assertNull(StringUtil.addToNumberString(null, 0), "null pass-thru");
        assertEquals("12345", StringUtil.addToNumberString("12345", 0), "no-change");
        assertEquals("112344", StringUtil.addToNumberString("12345", 99999), "increase");
        assertEquals("00345", StringUtil.addToNumberString("12345", -12000), "subtract");
    }

    @Test
    public void testPadNumberString() {
        assertEquals("12345", StringUtil.padNumberString("12345", 3), "less");
        assertEquals("12345", StringUtil.padNumberString("12345", 5), "same");
        assertEquals("00012345", StringUtil.padNumberString("12345", 8), "more");
    }

    @Test
    public void testConvertOperatorSubstitutions() {
        assertNull(StringUtil.convertOperatorSubstitutions(null), "null pass-thru");
        assertEquals("abc", StringUtil.convertOperatorSubstitutions("abc"), "none");
        assertEquals("a'c", StringUtil.convertOperatorSubstitutions("a'c"), "none");
        assertEquals("one && two || three > four >= five < six <= seven", StringUtil.convertOperatorSubstitutions(
                        "one @and two @or three @gt four @gteq five @lt six @lteq seven"), "all converions");
    }

    @Test
    public void testTruncateString() {
        assertEquals("this is a truncated long string",
                StringUtil.truncateEncodedStringToLength("this is a truncated long string", 40), "no truncate");
        assertEquals("this",
                StringUtil.truncateEncodedStringToLength("this", 5), "no truncate to short");
        assertEquals("this is a t…ing",
                StringUtil.truncateEncodedStringToLength("this is a truncated long string", 15), "normal");
        assertEquals("this …",
                StringUtil.truncateEncodedStringToLength("this is a truncated long string", 5), "normal short");
        assertEquals("this ( are … ok",
                StringUtil.truncateEncodedStringToLength("this ( are managed correctly ) ok", 15), "with parenthesis");
        assertEquals("this ( are …d )",
                StringUtil.truncateEncodedStringToLength("this ( are managed correctly, with the end )", 15), "with parenthesis at end");
        assertEquals("this ( are …d )",
                StringUtil.truncateEncodedStringToLength("this ( are a semicolon far ; managed correctly, with the end )", 15),
                "with parenthesis and semicolon ignored");
        assertEquals("this ( are;…d )",
                StringUtil.truncateEncodedStringToLength("this ( are; a semicolon closer managed correctly, with the end )", 15),
                "with parenthesis and semicolon closer");
        assertEquals("this ( are&eacut;…end",
                StringUtil.truncateEncodedStringToLength("this ( are&eacut; managed correctly, with the ) end", 15),
                "with parenthesis and é managed");
        assertEquals("this ( a&eacut;e&eacut;…end",
                StringUtil.truncateEncodedStringToLength("this ( a&eacut;e&eacut; managed correctly, with the ) end", 15),
                "with parenthesis and é é managed");
        assertEquals("this ( are …n&eacut;d",
                StringUtil.truncateEncodedStringToLength("this ( are & closer managed correctly, with th&e ) en&eacut;d", 15),
                "with parenthesis and é closer");
        assertEquals("this ( are …&eacut;&eacut;d",
                StringUtil.truncateEncodedStringToLength("this ( are & closer managed correctly, with th&e )en&eacut;&eacut;d", 15),
                "with parenthesis and é é closer");
        assertEquals("this ( are …&#235;&#235;d",
                StringUtil.truncateEncodedStringToLength("this ( are & closer managed correctly, with th&e )en&#235;&#235;d", 15),
                "with parenthesis and # # closer");
    }
}
