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
package org.ofbiz.base.json.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.ofbiz.base.json.JSON;
import org.ofbiz.base.json.JSONConstants;
import org.ofbiz.base.json.JSONWriter;
import org.ofbiz.base.json.ParseException;
import org.ofbiz.base.json.Token;
import org.ofbiz.base.json.TokenMgrError;
import org.ofbiz.base.test.GenericTestCaseBase;

public class JSONTests extends GenericTestCaseBase {
    public JSONTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected Object parseJSON(String value, boolean allowResolve) throws Exception {
        return new JSON(new StringReader(value)).allowResolve(allowResolve).JSONValue();
    }

    protected String getJSON(Object object, boolean allowResolve) throws Exception {
        StringWriter writer = new StringWriter();
        JSONWriter jsonWriter;
        if (allowResolve) {
            jsonWriter = new JSONWriter(writer, JSONWriter.ResolvingFallbackHandler);
        } else {
            jsonWriter = new JSONWriter(writer);
        };
        jsonWriter.write(object);
        return writer.toString();
    }

    protected void assertSimpleJSONByte(byte n, String json) throws Exception {
        assertSimpleJSON("integer - byte", new Byte(n), json, new Long(n));
        assertSimpleJSONShort((short) n, json);
    }

    protected void assertSimpleJSONShort(short n, String json) throws Exception {
        assertSimpleJSON("integer - short", new Integer(n), json, new Long(n));
        assertSimpleJSONInteger((int) n, json);
    }

    protected void assertSimpleJSONInteger(int n, String json) throws Exception {
        assertSimpleJSON("integer - int", new Short((short) n), json, new Long(n));
        assertSimpleJSONLong((long) n, json);
    }

    protected void assertSimpleJSONLong(long n, String json) throws Exception {
        assertSimpleJSON("integer - long", new Long(n), json, new Long(n));
    }

    protected void assertSimpleJSONFloat(float n, String json) throws Exception {
        assertSimpleJSON("float - float", new Float(n), json, new Double(n));
        assertSimpleJSONDouble((double) n, json);
    }

    protected void assertSimpleJSONDouble(double n, String json) throws Exception {
        assertSimpleJSON("float - double", new Double(n), json);
    }

    protected void assertSimpleJSON(String type, Object object, String json) throws Exception {
        assertSimpleJSON(type, object, json, object);
    }

    protected void assertSimpleJSON(String type, Object before, String json, Object after) throws Exception {
        assertEquals("write " + type, json, getJSON(before, false));
        assertEquals("parse " + type, after, parseJSON(json, false));
    }

    protected void assertResolveJSON(String type, Object obj, String json) throws Exception {
        assertEquals("write " + type, json, getJSON(obj, true));
        assertEquals("parse " + type, obj, parseJSON(json, true));
    }

    public void testParseBasicTypes() throws Exception {
        assertSimpleJSON("character", new Character('c'), "\"c\"", "c");
        assertSimpleJSON("false", Boolean.FALSE, "false");
        assertSimpleJSON("null", null, "null");
        assertSimpleJSON("true", Boolean.TRUE, "true");
        assertSimpleJSON("simple string", "foo", "\"foo\"");
        assertSimpleJSONByte((byte) 42, "42");
        assertSimpleJSONFloat(Float.valueOf("1.0625"), "1.0625");
        assertSimpleJSON(
            "complex string",
            "quote(\") backslash(\\) forwardslash(/) backspace(\b) formfeed(\f) newline(\n) carriagereturn(\r) tab(\t) trademark(\u2122)",
            "\"quote(\\\") backslash(\\\\) forwardslash(\\/) backspace(\\b) formfeed(\\f) newline(\\n) carriagereturn(\\r) tab(\\t) trademark(\\u2122)\""
        );
    }

    public void testParseComplexTypes() throws Exception {
        assertEquals(
            "parse simple array",
            list(new Object[] {"foo", new Long(1234), new Double(5.678)}),
            parseJSON("[, ,\t,\r,\n,\r\n,\"foo\", 1234, 5.678,]", false)
        );
        assertSimpleJSON(
            "simple empty list",
            list(new Object[] {}),
            "[]"
        );
        assertSimpleJSON(
            "simple empty array",
            new Object[] {},
            "[]",
            list(new Object[] {})
        );
        assertSimpleJSON(
            "simple array->list",
            new Object[] {"foo", new Long(1234), new Double(5.678)},
            "[\n \"foo\",\n 1234,\n 5.678\n]",
            list(new Object[] {"foo", new Long(1234), new Double(5.678)})
        );
        assertSimpleJSON(
            "simple array",
            list(new Object[] {"foo", new Long(1234), new Double(5.678)}),
            "[\n \"foo\",\n 1234,\n 5.678\n]",
            list(new Object[] {"foo", new Long(1234), new Double(5.678)})
        );
        assertEquals(
            "parse simple map",
            map(new Object[] {"foo", new Long(1234), "bar", new Double(5.678)}),
            parseJSON("{, ,\t,\r,\n,\r\n,\"foo\": 1234, \"bar\": 5.678,}", false)
        );
        assertSimpleJSON(
            "parse map",
            map(new Object[] {"foo", new Long(1234), "bar", new Double(5.678)}),
            "{\n \"foo\": 1234,\n \"bar\": 5.678\n}"
        );
        assertSimpleJSON(
            "parse empty map",
            map(new Object[] {}),
            "{}"
        );
        assertEquals(
            "parse nested map",
            map(new Object[] {
                "string",       "this is a string",
                "integer",      new Long(5000),
                "double",       new Double(3.1415926),
                "array",        new Object[] {
                    "string",
                    new Long(6000)
                },
                "list",         list(new Object[] {
                    "nested string",
                    "something",
                }),
                "empty-list",   new ArrayList(),
                "empty-array",  new String[0],
                "empty-map",    new HashMap(),
            }),
            parseJSON("{\"string\": \"this is a string\", \"integer\": 5000, \"double\": 3.1415926, \"array\": [\"string\", 6000], \"list\": [\"nested string\", \"something\"], \"empty-list\": [], \"empty-array\": [], \"empty-map\": {}}", false)
        );
    }

    public void testParseErrors() throws Exception {
        for (char c = 1; c < 1024; c++) {
            switch (c) {
                case '[':
                    doParseExceptionTest("[:", JSONConstants.KEY_SEP);
                    break;
                case ']':
                    doParseExceptionTest("]", JSONConstants.ARRAY_END);
                    break;
                case '{':
                    doParseExceptionTest("{:", JSONConstants.KEY_SEP);
                    break;
                case '}':
                    doParseExceptionTest("}", JSONConstants.OBJECT_END);
                    break;
                case ':':
                    doParseExceptionTest(":", JSONConstants.KEY_SEP);
                    break;
                case ',':
                    doParseExceptionTest(",", JSONConstants.ITEM_SEP);
                    break;
                case '"':
                    doParseExceptionTest("\"", JSONConstants.EOF);
                    break;
                case 't':
                    doParseExceptionTest("true:", JSONConstants.KEY_SEP);
                    break;
                case 'f':
                    doParseExceptionTest("false:", JSONConstants.KEY_SEP);
                    break;
                case 'n':
                    doParseExceptionTest("null:", JSONConstants.KEY_SEP);
                    break;
                case '-':   // numbers
                case '.':
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                case '\t':
                    doWhitespaceExceptionTest(Character.toString(c), 8);
                    break;
                case '\n':
                case '\r':
                case ' ':
                    doWhitespaceExceptionTest(Character.toString(c), 1);
                    break;
                default:
                    doTokenMgrErrorTest(c);
                    break;
            }
        }
    }

    protected void doWhitespaceExceptionTest(String s, int column) {
        ParseException caught = null;
        try {
            new JSON(new StringReader(s)).JSONValue();
        } catch (ParseException e) {
            caught = e;
        } finally {
            assertNotNull("caught exception", caught);
            assertNotNull("next token(" + s + ")", caught.currentToken);
            Token next = caught.currentToken.next;
            assertEquals("next token(" + s + ") is eof", 0, next.kind);
            assertEquals("begin line(" + s + ")", 1, next.beginLine);
            assertEquals("begin column(" + s + ")", column, next.beginColumn);
        }
    }

    protected void doParseExceptionTest(String s, int nextKind) {
        ParseException caught = null;
        try {
            new JSON(new StringReader(s)).JSONValue();
        } catch (ParseException e) {
            caught = e;
        } finally {
            assertNotNull("caught exception", caught);
            assertNotNull("exception message(" + s + ")", caught.getMessage());
            assertNotNull("next token(" + s + ")", caught.currentToken);
            Token next = caught.currentToken.next;
            assertEquals("next token(" + s + ") is correct", nextKind, next.kind);
            assertEquals("begin line(" + s + ")", 1, next.beginLine);
            assertEquals("begin column(" + s + ")", s.length(), next.beginColumn);
        }
    }

    protected void doTokenMgrErrorTest(char c) throws Exception {
        TokenMgrError caught = null;
        try {
            parseJSON(c + "\"string\"", false);
        } catch (TokenMgrError e) {
            caught = e;
        } finally {
            assertNotNull("No TokenMgrError thrown for character(" + ((int) c) + ")", caught);
            // FIXME: maybe extend javacc to return more info in TokenMgrError
        }
    }

    public void testResolve() throws Exception {
        assertResolveJSON("url", new URL("http://ofbiz.apache.org"), "resolve(\"java.net.URL:http:\\/\\/ofbiz.apache.org\")");
    }
}
