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
package org.ofbiz.base.util.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.lang.Appender;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.test.GenericTestCaseBase;

@SourceMonitored
public class StringUtilTests extends GenericTestCaseBase {
    public StringUtilTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStringUtil() throws Exception {
        assertStaticHelperClass(StringUtil.class);
        assertTrue("correct INSTANCE", StringUtil.INSTANCE instanceof StringUtil);
    }

    public void testInternString() {
        assertSame("intern-constant", StringUtil.internString("foo"), StringUtil.internString("foo"));
        assertSame("intern-new", StringUtil.internString("foo"), StringUtil.internString(new String("foo")));
        assertSame("intern-char", StringUtil.internString("foo"), StringUtil.internString(new String(new char[] {'f', 'o', 'o'})));
        assertSame("intern-null", StringUtil.internString(null), StringUtil.internString(null));
    }

    public void testReplaceString() {
        assertNull("null", StringUtil.replaceString(null, "old", "new"));
        assertEquals("empty old", "the old dog jumped over the old fence", StringUtil.replaceString("the old dog jumped over the old fence", "", "new"));
        assertEquals("replace", "the new dog jumped over the new fence", StringUtil.replaceString("the old dog jumped over the old fence", "old", "new"));
        assertEquals("replace-null", "the  dog jumped over the  fence", StringUtil.replaceString("the old dog jumped over the old fence", "old", null));
        assertEquals("replace-not-found", "the old dog jumped over the old fence", StringUtil.replaceString("the old dog jumped over the old fence", "cat", "feline"));
    }

    public void testJoin() {
        assertNull("null-list", StringUtil.join(null, ","));
        assertNull("empty-list", StringUtil.join(Collections.emptyList(), ","));
        assertEquals("single", "1", StringUtil.join(list("1"), ","));
        assertEquals("double", "1,2", StringUtil.join(list("1", "2"), ","));
    }

    public void testSplit() {
        assertNull("null-string", StringUtil.split(null, ","));
        assertEquals("single", list("1"), StringUtil.split("1", ","));
        assertEquals("double", list("1", "2"), StringUtil.split("1,2", ","));
        assertEquals("no-sep", list("1", "2", "3", "4", "5", "6"), StringUtil.split("1 2\t3\n4\r5\f6", null));
    }

    public void testQuoteStrList() {
        assertEquals("single", list("'1'"), StringUtil.quoteStrList(list("1")));
        assertEquals("double", list("'1'", "'2'"), StringUtil.quoteStrList(list("1", "2")));
    }

    public void testStrToMap() {
        assertNull("null-string", StringUtil.strToMap(null, false));
        //assertEquals("empty", Collections.emptyMap(), StringUtil.strToMap("", false));
        assertEquals("missing =", Collections.emptyMap(), StringUtil.strToMap("1", false));
        assertEquals("single", map("1", "one"), StringUtil.strToMap("1=one"));
        assertEquals("double", map("2", "two", "1", "one"), StringUtil.strToMap("1=one|2=two"));
        assertEquals("double-no-trim", map(" 2 ", " two ", " 1 ", " one "), StringUtil.strToMap(" 1 = one | 2 = two "));
        assertEquals("double-trim", map("2", "two", "1", "one"), StringUtil.strToMap(" 1 = one | 2 = two ", true));
    }

    public void testMapToStr() {
        assertNull("null-map", StringUtil.mapToStr(null));
        assertEquals("empty", "", StringUtil.mapToStr(Collections.emptyMap()));
        assertEquals("single", "1=one", StringUtil.mapToStr(map("1", "one")));
        assertEquals("double", "1=one|2=two", StringUtil.mapToStr(map("1", "one", "2", "two")));
        assertEquals("double-with-non-string", "1=one|2=two", StringUtil.mapToStr(map("a", this, "1", "one", "2", "two", this, "a")));
    }

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
        //assertEquals("empty", Collections.emptyMap(), StringUtil.toMap("{}"));
        assertEquals("single", map("1", "one"), StringUtil.toMap("{1=one}"));
        assertEquals("double", map("2", "two", "1", "one"), StringUtil.toMap("{1=one, 2=two}"));
        assertEquals("double-space", map("2", "two ", " 1", "one"), StringUtil.toMap("{ 1=one, 2=two }"));
    }

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
        //assertEquals("empty", Collections.emptyList(), StringUtil.toList("[]"));
        assertEquals("single", list("1"), StringUtil.toList("[1]"));
        assertEquals("double", list("1", "2"), StringUtil.toList("[1, 2]"));
        assertEquals("double-space", list(" 1", "2 "), StringUtil.toList("[ 1, 2 ]"));
    }

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
        //assertEquals("empty", Collections.emptySet(), StringUtil.toSet("[]"));
        assertEquals("single", set("1"), StringUtil.toSet("[1]"));
        assertEquals("double", set("1", "2"), StringUtil.toSet("[1, 2]"));
        assertEquals("double-space", set(" 1", "2 "), StringUtil.toSet("[ 1, 2 ]"));
    }

    public void testCreateMap() {
        List<String>[] badKeys = UtilGenerics.cast(new List[] {null, list("1"), list("2")});
        List<String>[] badValues = UtilGenerics.cast(new List[] {list("one"), null, list("two", "extra")});
        for (int i = 0; i < badKeys.length; i++) {
            IllegalArgumentException caught = null;
            try {
                StringUtil.createMap(badKeys[i], badValues[i]);
            } catch (IllegalArgumentException e) {
                caught = e;
            } finally {
                assertNotNull("bad(" + i + ")", caught);
            }
        }
        assertEquals("parse", map("1", "one", "2", "two"), StringUtil.createMap(list("1", "2"), list("one", "two")));
    }

    public void testCleanUpPathPrefix() {
        assertEquals("null", "", StringUtil.cleanUpPathPrefix(null));
        assertEquals("empty", "", StringUtil.cleanUpPathPrefix(""));
        for (String s: new String[] {"\\a\\b\\c", "\\a\\b\\c\\", "a\\b\\c\\", "a\\b\\c", "/a/b/c", "/a/b/c/", "a/b/c/", "a/b/c"}) {
            assertEquals("cleanup(" + s + ")", "/a/b/c", StringUtil.cleanUpPathPrefix(s));
        }
    }

    public void testRemoveSpaces() {
        assertEquals("", StringUtil.removeSpaces(""));
        assertEquals("abcd", StringUtil.removeSpaces(" a b c d "));
        assertEquals("a\\cd", StringUtil.removeSpaces(" a \\ c d "));
    }

    public void testToHexString() {
        assertEquals("16 bytes", "000102030405060708090a0b0c0d0e0f", StringUtil.toHexString(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}));
    }

    public void testCleanHexString() {
        assertEquals("clean hex", "rtwertetwretw", StringUtil.cleanHexString("rtwer:tetw retw"));
    }

    public void testFromHexString() {
        assertEquals("16 bytes", new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, StringUtil.fromHexString("000102030405060708090a0b0c0d0e0f"));
        GeneralRuntimeException caught = null;
        try {
            StringUtil.fromHexString("0-");
        } catch (GeneralRuntimeException e) {
            caught = e;
        } finally {
            assertNotNull("bad-char", caught);
        }
    }

    public void testConvertChar() {
        Map<Character, Integer> conversions = new HashMap<Character, Integer>();
        conversions.put('0', 0); conversions.put('1', 1); conversions.put('2', 2); conversions.put('3', 3);
        conversions.put('4', 4); conversions.put('5', 5); conversions.put('6', 6); conversions.put('7', 7);
        conversions.put('8', 8); conversions.put('9', 9);
        conversions.put('a', 10); conversions.put('b', 11); conversions.put('c', 12);
        conversions.put('d', 13); conversions.put('e', 14); conversions.put('f', 15);
        conversions.put('A', 10); conversions.put('B', 11); conversions.put('C', 12);
        conversions.put('D', 13); conversions.put('E', 14); conversions.put('F', 15);
        for (int i = 0; i < 256; i++) {
            Integer wanted = conversions.get((char) i);
            if (wanted == null) {
                Exception caught = null;
                try {
                    StringUtil.convertChar((char) i);
                } catch (Exception e) {
                    caught = e;
                } finally {
                    assertNotNull(Integer.toString(i), caught);
                }
            } else {
                assertEquals(Integer.toString(i), wanted.intValue(), StringUtil.convertChar((char) i));
            }
        }
    }

    public void testEncodeInt() {
        assertEquals("one octet", new char[] {'0', '5'}, StringUtil.encodeInt(5, 0, new char[2]));
        assertEquals("two octets", new char[] {'1', '5'}, StringUtil.encodeInt(21, 0, new char[2]));
        // these next two are really weird, note the start offset being != 0.
        assertEquals("three octets", new char[] {'3', '1', '5'}, StringUtil.encodeInt(789, 1, new char[3]));
        assertEquals("four octets", new char[] {'7', '3', '1', '5'}, StringUtil.encodeInt(29461, 2, new char[4]));
    }

    public void testRemoveNonNumeric() {
        assertEquals("just numbers", "12345", StringUtil.removeNonNumeric("a1'2;3]4!5("));
    }

    public void testRemoveNumeric() {
        assertEquals("only numbers", "a';]!(", StringUtil.removeNumeric("a1'2;3]4!5("));
    }

    public void testRemoveRegex() {
    }

    public void testAddToNumberString() {
        assertNull("null pass-thru", StringUtil.addToNumberString(null, 0));
        assertEquals("no-change", "12345", StringUtil.addToNumberString("12345", 0));
        assertEquals("increase", "112344", StringUtil.addToNumberString("12345", 99999));
        assertEquals("subtract", "00345", StringUtil.addToNumberString("12345", -12000));
    }

    public void testPadNumberString() {
        assertEquals("less", "12345", StringUtil.padNumberString("12345", 3));
        assertEquals("same", "12345", StringUtil.padNumberString("12345", 5));
        assertEquals("more", "00012345", StringUtil.padNumberString("12345", 8));
    }

    public void testConvertOperatorSubstitutions() {
        assertNull("null pass-thru", StringUtil.convertOperatorSubstitutions(null));
        assertEquals("none", "abc", StringUtil.convertOperatorSubstitutions("abc"));
        assertEquals("none", "a'c", StringUtil.convertOperatorSubstitutions("a'c"));
        assertEquals("all converions", "one && two || three > four >= five < six <= seven", StringUtil.convertOperatorSubstitutions("one @and two @or three @gt four @gteq five @lt six @lteq seven"));
    }

    public void testCollapseNewlines() {
    }

    public void testCollapseSpaces() {
    }

    public void testCollapseCharacter() {
        assertEquals("not-found", "abcdefg", StringUtil.collapseCharacter("abcdefg", '.'));
        assertEquals("no-change", "abcdefg", StringUtil.collapseCharacter("abcdefg", 'a'));
        assertEquals("duplicate", "abcdefa", StringUtil.collapseCharacter("aabcdefaa", 'a'));
    }

    public void testWrapString() {
    }

    public void testMakeStringWrapper() {
    }

    protected static final class TestAppender implements Appender<StringBuilder> {
        private final String s;

        protected TestAppender(String s) {
            this.s = s;
        }

        public StringBuilder appendTo(StringBuilder sb) {
            return sb.append(s);
        }
    }

    public void testAppendTo() {
        assertEquals("111", "[1],[2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", ",").toString());
        assertEquals("011", "1],2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), null, "]", ",").toString());
        assertEquals("101", "[1,[2", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", null, ",").toString());
        assertEquals("110", "[1][2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", null).toString());
        assertEquals("11111", "[1]<,>[2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", "<", ",", ">").toString());
        assertEquals("01111", "1]<,>2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), null, "]", "<", ",", ">").toString());
        assertEquals("10111", "[1<,>[2", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", null, "<", ",", ">").toString());
        assertEquals("11011", "[1],>[2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", null, ",", ">").toString());
        assertEquals("11101", "[1][2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", "<", null, ">").toString());
        assertEquals("11110", "[1]<,[2]", StringUtil.appendTo(new StringBuilder(), list(new TestAppender("1"), new TestAppender("2")), "[", "]", "<", ",", null).toString());
    }

    public void testAppend() {
        assertEquals("111", "[1],[2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", ",").toString());
        assertEquals("011", "1],2]", StringUtil.append(new StringBuilder(), list("1", "2"), null, "]", ",").toString());
        assertEquals("101", "[1,[2", StringUtil.append(new StringBuilder(), list("1", "2"), "[", null, ",").toString());
        assertEquals("110", "[1][2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", null).toString());
        assertEquals("11111", "[1]<,>[2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", "<", ",", ">").toString());
        assertEquals("01111", "1]<,>2]", StringUtil.append(new StringBuilder(), list("1", "2"), null, "]", "<", ",", ">").toString());
        assertEquals("10111", "[1<,>[2", StringUtil.append(new StringBuilder(), list("1", "2"), "[", null, "<", ",", ">").toString());
        assertEquals("11011", "[1],>[2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", null, ",", ">").toString());
        assertEquals("11101", "[1][2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", "<", null, ">").toString());
        assertEquals("11110", "[1]<,[2]", StringUtil.append(new StringBuilder(), list("1", "2"), "[", "]", "<", ",", null).toString());
    }
}
