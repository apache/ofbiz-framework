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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.StringUtil;
import org.junit.Test;

public class StringUtilTests {

    @Test
    public void testInternString() {
        assertSame("intern-constant", StringUtil.internString("foo"), StringUtil.internString("foo"));
        assertSame("intern-new", StringUtil.internString("foo"), StringUtil.internString("foo"));
        assertSame("intern-char", StringUtil.internString("foo"), StringUtil.internString(new String(new char[] {'f', 'o', 'o'})));
        assertSame("intern-null", StringUtil.internString(null), StringUtil.internString(null));
    }

    @Test
    public void testReplaceString() {
        assertNull("null", StringUtil.replaceString(null, "old", "new"));
        assertEquals("empty old", "the old dog jumped over the old fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "", "new"));
        assertEquals("replace", "the new dog jumped over the new fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "old", "new"));
        assertEquals("replace-null", "the  dog jumped over the  fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "old", null));
        assertEquals("replace-not-found", "the old dog jumped over the old fence",
                StringUtil.replaceString("the old dog jumped over the old fence", "cat", "feline"));
    }

    @Test
    public void testJoin() {
        assertNull("null-list", StringUtil.join(null, ","));
        assertNull("empty-list", StringUtil.join(Collections.emptyList(), ","));
        assertEquals("single", "1", StringUtil.join(Collections.singleton("1"), ","));
        assertEquals("double", "1,2", StringUtil.join(Arrays.asList("1", "2"), ","));
    }

    @Test
    public void testSplit() {
        assertNull("null-string", StringUtil.split(null, ","));
        assertEquals("single", Arrays.asList("1"), StringUtil.split("1", ","));
        assertEquals("double", Arrays.asList("1", "2"), StringUtil.split("1,2", ","));
        assertEquals("no-sep", Arrays.asList("1", "2", "3", "4", "5", "6"), StringUtil.split("1 2\t3\n4\r5\f6", null));
    }

    @Test
    public void testStrToMap() {
        assertNull("null-string", StringUtil.strToMap(null, false));
        assertEquals("missing =", Collections.emptyMap(), StringUtil.strToMap("1", false));
        assertEquals("single", UtilMisc.toMap("1", "one"), StringUtil.strToMap("1=one"));
        assertEquals("double", UtilMisc.toMap("2", "two", "1", "one"), StringUtil.strToMap("1=one|2=two"));
        assertEquals("double-no-trim", UtilMisc.toMap(" 2 ", " two ", " 1 ", " one "),
                StringUtil.strToMap(" 1 = one | 2 = two "));
        assertEquals("double-trim", UtilMisc.toMap("2", "two", "1", "one"),
                StringUtil.strToMap(" 1 = one | 2 = two ", true));
    }

    @Test
    public void testToMap() {
        for (String s: new String[] {"", "{", "}", "}{"}) {
            IllegalArgumentException caught = null;
            try {
                StringUtil.toMap(s);
            } catch (IllegalArgumentException e) {
                caught = e;
            } finally {
                assertNotNull("bad(" + s + ")", caught);
            }
        }
        assertEquals("single", UtilMisc.toMap("1", "one"), StringUtil.toMap("{1=one}"));
        assertEquals("double", UtilMisc.toMap("2", "two", "1", "one"), StringUtil.toMap("{1=one, 2=two}"));
        assertEquals("double-space", UtilMisc.toMap("2", "two ", " 1", "one"), StringUtil.toMap("{ 1=one, 2=two }"));
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
                assertNotNull("bad(" + s + ")", caught);
            }
        }
        assertEquals("single", Arrays.asList("1"), StringUtil.toList("[1]"));
        assertEquals("double", Arrays.asList("1", "2"), StringUtil.toList("[1, 2]"));
        assertEquals("double-space", Arrays.asList(" 1", "2 "), StringUtil.toList("[ 1, 2 ]"));
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
                assertNotNull("bad(" + s + ")", caught);
            }
        }
        assertEquals("single", UtilMisc.toSet("1"), StringUtil.toSet("[1]"));
        assertEquals("double", UtilMisc.toSet("1", "2"), StringUtil.toSet("[1, 2]"));
        assertEquals("double-space", UtilMisc.toSet(" 1", "2 "), StringUtil.toSet("[ 1, 2 ]"));
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
                assertNotNull("bad(" + i + ")", caught);
            }
        }
        assertEquals("parse", UtilMisc.toMap("1", "one", "2", "two"), StringUtil.createMap(Arrays.asList("1", "2"),
                Arrays.asList("one", "two")));
    }

    @Test
    public void testCleanUpPathPrefix() {
        assertEquals("null", "", StringUtil.cleanUpPathPrefix(null));
        assertEquals("empty", "", StringUtil.cleanUpPathPrefix(""));
        for (String s: new String[] {"\\a\\b\\c", "\\a\\b\\c\\", "a\\b\\c\\", "a\\b\\c", "/a/b/c", "/a/b/c/", "a/b/c/", "a/b/c"}) {
            assertEquals("cleanup(" + s + ")", "/a/b/c", StringUtil.cleanUpPathPrefix(s));
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
        assertEquals("16 bytes", "000102030405060708090a0b0c0d0e0f",
                StringUtil.toHexString(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}));
    }

    @Test
    public void testCleanHexString() {
        assertEquals("clean hex", "rtwertetwretw", StringUtil.cleanHexString("rtwer:tetw retw"));
    }

    @Test
    public void testFromHexString() {
        assertArrayEquals("16 bytes", new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
                StringUtil.fromHexString("000102030405060708090a0b0c0d0e0f"));
        GeneralRuntimeException caught = null;
        try {
            StringUtil.fromHexString("0-");
        } catch (GeneralRuntimeException e) {
            caught = e;
        } finally {
            assertNotNull("bad-char", caught);
        }
    }

    @Test
    public void testEncodeInt() {
        assertArrayEquals("one octet", new char[] {'0', '5'}, StringUtil.encodeInt(5, 0, new char[2]));
        assertArrayEquals("two octets", new char[] {'1', '5'}, StringUtil.encodeInt(21, 0, new char[2]));
        // these next two are really weird, note the start offset being != 0.
        assertArrayEquals("three octets", new char[] {'3', '1', '5'}, StringUtil.encodeInt(789, 1, new char[3]));
        assertArrayEquals("four octets", new char[] {'7', '3', '1', '5'}, StringUtil.encodeInt(29461, 2, new char[4]));
    }

    @Test
    public void testRemoveNonNumeric() {
        assertEquals("just numbers", "12345", StringUtil.removeNonNumeric("a1'2;3]4!5("));
    }

    @Test
    public void testRemoveRegex() {
    }

    @Test
    public void testAddToNumberString() {
        assertNull("null pass-thru", StringUtil.addToNumberString(null, 0));
        assertEquals("no-change", "12345", StringUtil.addToNumberString("12345", 0));
        assertEquals("increase", "112344", StringUtil.addToNumberString("12345", 99999));
        assertEquals("subtract", "00345", StringUtil.addToNumberString("12345", -12000));
    }

    @Test
    public void testPadNumberString() {
        assertEquals("less", "12345", StringUtil.padNumberString("12345", 3));
        assertEquals("same", "12345", StringUtil.padNumberString("12345", 5));
        assertEquals("more", "00012345", StringUtil.padNumberString("12345", 8));
    }

    @Test
    public void testConvertOperatorSubstitutions() {
        assertNull("null pass-thru", StringUtil.convertOperatorSubstitutions(null));
        assertEquals("none", "abc", StringUtil.convertOperatorSubstitutions("abc"));
        assertEquals("none", "a'c", StringUtil.convertOperatorSubstitutions("a'c"));
        assertEquals("all converions", "one && two || three > four >= five < six <= seven",
                StringUtil.convertOperatorSubstitutions(
                        "one @and two @or three @gt four @gteq five @lt six @lteq seven"));
    }
}
