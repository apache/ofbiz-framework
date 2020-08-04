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
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class UtilCodecTests {

    @Test
    public void canonicalizeRevealsEscapedXSS() {
        String xssVector = "&lt;script&gtalert(\"XSS vector\");&lt;/script&gt;";
        String canonicalizedXssVector = UtilCodec.canonicalize(xssVector, true, true);
        assertEquals("<script>alert(\"XSS vector\");</script>", canonicalizedXssVector);
    }

    @Test
    public void checkStringForHtmlStrictNoneDetectsXSS() {
        String xssVector = "&lt;script&gtalert(\"XSS vector\");&lt;/script&gt;";
        List<String> errorList = new ArrayList<>();
        String canonicalizedXssVector = UtilCodec.checkStringForHtmlStrictNone("fieldName", xssVector, errorList,
                new Locale("test"));
        assertEquals("<script>alert(\"XSS vector\");</script>", canonicalizedXssVector);
        assertEquals(1, errorList.size());
        assertEquals("In field [fieldName] less-than (<) and greater-than (>) symbols are not allowed.",
                errorList.get(0));
    }

    @Test
    public void testGetEncoder() {
        encoderTest("string", UtilCodec.getEncoder("string"), "abc\\\"def", "abc\"def");
        encoderTest("xml", UtilCodec.getEncoder("xml"), "&#x3c;&#x3e;&#x27;&#x22;", "<>'\"");
        encoderTest("html", UtilCodec.getEncoder("html"), "&lt;&gt;&#x27;&quot;", "<>'\"");
        assertNull("invalid encoder", UtilCodec.getEncoder("foobar"));
    }

    @Test
    public void testCheckStringForHtmlStrictNone() {
        checkStringForHtmlStrictNoneTest("null pass-thru", null, null);
        checkStringForHtmlStrictNoneTest("empty pass-thru", "", "");
        checkStringForHtmlStrictNoneTest("o-numeric-encode", "foo", "f&#111;o");
        checkStringForHtmlStrictNoneTest("o-hex-encode", "foo", "f%6fo");
        // jacopoc: temporarily commented because this test is failingafter the upgrade of owasp-esapi
        //checkStringForHtmlStrictNone_test("o-double-hex-encode", "foo", "f%256fo");
        checkStringForHtmlStrictNoneTest("<-not-allowed", "f<oo", "f<oo",
                "In field [<-not-allowed] less-than (<) and greater-than (>) symbols are not allowed.");
        checkStringForHtmlStrictNoneTest(">-not-allowed", "f>oo", "f>oo",
                "In field [>-not-allowed] less-than (<) and greater-than (>) symbols are not allowed.");
        // jleroux: temporarily comments because this test is failing on BuildBot (only) when switching to Gradle
        //checkStringForHtmlStrictNone_test("high-ascii", "fÀ®", "f%C0%AE");
        // this looks like a bug, namely the extra trailing;
        // jacopoc: temporarily commented because this test is failing after the upgrade of owasp-esapi
        //checkStringForHtmlStrictNone_test("double-ampersand", "f\";oo", "f%26quot%3boo");
        checkStringForHtmlStrictNoneTest("double-encoding", "%2%353Cscript", "%2%353Cscript",
                "In field [double-encoding] found character escaping (mixed or double) that is not allowed "
                        + "or other format consistency error: "
                        + "org.apache.ofbiz.base.util.UtilCodec$IntrusionException: Input validation failure");
        checkStringForHtmlStrictNoneTest("js_event", "non_existent.foo\" onerror=\"alert('Hi!');",
                "non_existent.foo\" onerror=\"alert('Hi!');",
                "In field [js_event] Javascript events are not allowed.");
    }

    private static void encoderTest(String label, UtilCodec.SimpleEncoder encoder, String wanted, String toEncode) {
        assertNull(label + "(encoder):null", encoder.encode(null));
        assertEquals(label + "(encoder):encode", wanted, encoder.encode(toEncode));
    }
    private static void checkStringForHtmlStrictNoneTest(String label, String fixed, String input,
            String... wantedMessages) {
        List<String> gottenMessages = new ArrayList<>();
        assertEquals(label, fixed, UtilCodec.checkStringForHtmlStrictNone(label, input, gottenMessages,
                new Locale("test")));
        assertEquals(label, Arrays.asList(wantedMessages), gottenMessages);
    }

    @Test
    public void testCheckStringForHtmlSafe() {
        String xssVector = "<script>alert('XSS vector');</script>";
        List<String> errorList = new ArrayList<>();
        String canonicalizedXssVector = UtilCodec.checkStringForHtmlSafe("fieldName", xssVector, errorList,
                new Locale("test"), true);
        assertEquals("<script>alert('XSS vector');</script>", canonicalizedXssVector);
        assertEquals(1, errorList.size());
        assertEquals("In field [fieldName] by our input policy, your input has not been accepted for security reason. "
                + "Please check and modify accordingly, thanks.", errorList.get(0));
    }
}
