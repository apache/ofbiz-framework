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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.MemoryType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.ofbiz.base.util.UtilIO;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.test.GenericTestCaseBase;

public class UtilIOTests extends GenericTestCaseBase {
    private static final byte[] trademarkBytes = new byte[] {
        (byte) 0xE2, (byte) 0x84, (byte) 0xA2
    };
    public UtilIOTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testReadString() throws Exception {
        readStringTest_0("unix line ending", "\n", new byte[] { 0x0A });
        readStringTest_0("mac line ending", "\r", new byte[] { 0x0D });
        readStringTest_0("windows line ending", "\r\n", new byte[] { 0x0D, 0x0A });
    }

    private static byte[] join(byte[]... parts) {
        int count = 0;
        for (byte[] part: parts) {
            count += part.length;
        }
        byte[] result = new byte[count];
        int i = 0;
        for (byte[] part: parts) {
            System.arraycopy(part, 0, result, i, part.length);
            i += part.length;
        }
        return result;
    }

    private static void readStringTest_0(String label, String lineSeparator, byte[] extra) throws IOException {
        String originalLineSeparator = System.getProperty("line.separator");
        try {
            System.getProperties().put("line.separator", lineSeparator);
            readStringTest_1(label + ":mark", "\u2122", join(trademarkBytes));
            readStringTest_1(label + ":mark NL", "\u2122\n", join(trademarkBytes, extra));
            readStringTest_1(label + ":NL mark", "\n\u2122", join(extra, trademarkBytes));
        } finally {
            System.getProperties().put("line.separator", originalLineSeparator);
        }
    }

    private static void readStringTest_1(String label, String wanted, byte[] toRead) throws IOException {
        assertEquals("readString bytes default:" + label, wanted, UtilIO.readString(toRead));
        assertEquals("readString bytes UTF-8:" + label, wanted, UtilIO.readString(toRead, "UTF-8"));
        assertEquals("readString bytes UTF8:" + label, wanted, UtilIO.readString(toRead, UtilIO.UTF8));
        assertEquals("readString bytes offset/length default:" + label, wanted, UtilIO.readString(toRead, 0, toRead.length));
        assertEquals("readString bytes offset/length UTF-8:" + label, wanted, UtilIO.readString(toRead, 0, toRead.length, "UTF-8"));
        assertEquals("readString bytes offset/length UTF8:" + label, wanted, UtilIO.readString(toRead, 0, toRead.length, UtilIO.UTF8));
        assertEquals("readString stream default:" + label, wanted, UtilIO.readString(new ByteArrayInputStream(toRead)));
        assertEquals("readString stream UTF-8:" + label, wanted, UtilIO.readString(new ByteArrayInputStream(toRead), "UTF-8"));
        assertEquals("readString stream UTF8:" + label, wanted, UtilIO.readString(new ByteArrayInputStream(toRead), UtilIO.UTF8));
    }

    public void testWriteString() throws Exception {
        writeStringTest_0("unix line ending", "\n", new byte[] { 0x0A });
        writeStringTest_0("mac line ending", "\r", new byte[] { 0x0D });
        writeStringTest_0("windows line ending", "\r\n", new byte[] { 0x0D, 0x0A });
    }

    private static void writeStringTest_0(String label, String lineSeparator, byte[] extra) throws IOException {
        String originalLineSeparator = System.getProperty("line.separator");
        try {
            System.getProperties().put("line.separator", lineSeparator);
            writeStringTest_1(label + ":mark", join(trademarkBytes), "\u2122");
            writeStringTest_1(label + ":mark NL", join(trademarkBytes, extra), "\u2122\n");
            writeStringTest_1(label + ":NL mark", join(extra, trademarkBytes), "\n\u2122");
        } finally {
            System.getProperties().put("line.separator", originalLineSeparator);
        }
    }

    private static void writeStringTest_1(String label, byte[] wanted, String toWrite) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, toWrite);
        assertEquals("writeString default:" + label, wanted, baos.toByteArray());
        baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, "UTF-8", toWrite);
        assertEquals("writeString UTF-8:" + label, wanted, baos.toByteArray());
        baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, UtilIO.UTF8, toWrite);
        assertEquals("writeString UTF8:" + label, wanted, baos.toByteArray());
    }

    protected void checkBasicReadObject(Object value, String text) throws Exception {
        byte[] bytes = text.getBytes("UTF-8");
        assertEquals("read bytes " + value.getClass().getName(), value, UtilIO.readObject(new ByteArrayInputStream(bytes)));
        assertEquals("read chars " + value.getClass().getName(), value, UtilIO.readObject(text.toCharArray()));
        assertEquals("read chars offset " + value.getClass().getName(), value, UtilIO.readObject(text.toCharArray(), 0, text.length()));
    }

    protected void checkBasicReadWriteObject(Object value, String text) throws Exception {
        byte[] bytes = text.getBytes("UTF-8");
        assertEquals("read bytes " + value.getClass().getName(), value, UtilIO.readObject(new ByteArrayInputStream(bytes)));
        assertEquals("read chars " + value.getClass().getName(), value, UtilIO.readObject(text.toCharArray()));
        assertEquals("read chars offset " + value.getClass().getName(), value, UtilIO.readObject(text.toCharArray(), 0, text.length()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UtilIO.writeObject(baos, value);
        assertEquals("write stream " + value.getClass().getName(), text, new String(baos.toByteArray(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        UtilIO.writeObject(sb, value);
        sb.append('\n');
        assertEquals("write builder " + value.getClass().getName(), text, sb.toString());
    }

    public void testReadWriteObject() throws Exception {
        checkBasicReadWriteObject(Boolean.TRUE, "java.lang.Boolean:true\n");
        checkBasicReadWriteObject(Byte.valueOf("1"), "java.lang.Byte:1\n");
        checkBasicReadWriteObject(Double.valueOf("1.0"), "java.lang.Double:1.0\n");
        checkBasicReadWriteObject(Float.valueOf("1.0"), "java.lang.Float:1.0\n");
        checkBasicReadWriteObject(Integer.valueOf("1"), "java.lang.Integer:1\n");
        checkBasicReadWriteObject(Long.valueOf("1"), "java.lang.Long:1\n");
        checkBasicReadWriteObject(Short.valueOf("1"), "java.lang.Short:1\n");
        checkBasicReadWriteObject(BigDecimal.valueOf(500.5), "java.math.BigDecimal:500.5\n");
        checkBasicReadWriteObject(BigInteger.valueOf(500), "java.math.BigInteger:500\n");
        checkBasicReadWriteObject("1", "java.lang.String:1\n");
        checkBasicReadObject(Arrays.asList(new Object[] {"a", UtilMisc.toMap("b", Long.valueOf(1))}), "[\n \"a\",\n {\n  \"b\": 1\n }\n]\n");
        checkBasicReadWriteObject(MemoryType.HEAP, "java.lang.management.MemoryType:HEAP\n");
        checkBasicReadWriteObject(MemoryType.NON_HEAP, "java.lang.management.MemoryType:NON_HEAP\n");
        checkBasicReadWriteObject(UtilIO.UTF8, "java.nio.charset.Charset:UTF-8\n");
        checkBasicReadWriteObject(InetAddress.getByAddress("localhost", new byte[] {127, 0, 0, 1}), "java.net.InetAddress:localhost\n");
        //checkBasicReadWriteObject(Pattern.compile("^([a-z]{3}.*?):$"), "java.util.regex.Pattern:^([a-z]{3}.*?):$\n");
        checkBasicReadWriteObject(Time.valueOf("12:34:56"), "java.sql.Time:12:34:56\n");
        //checkBasicReadWriteObject(new Timestamp(1234567890), "java.sql.Timestamp:1234567890 00:00:00\n");
        //checkBasicReadWriteObject(new java.util.Date(1234567890), "java.util.Date:1234567890\n");
        checkBasicReadWriteObject(UUID.fromString("c3241927-9f77-43e1-be16-bd71d245ef64"), "java.util.UUID:c3241927-9f77-43e1-be16-bd71d245ef64\n");
        checkBasicReadWriteObject(TimeZone.getTimeZone("America/Chicago"), "java.util.TimeZone:America/Chicago\n");
        checkBasicReadWriteObject(new SimpleDateFormat("MM/dd/yyyy hh:mm a"), "java.text.SimpleDateFormat:MM/dd/yyyy hh:mm a\n");
        checkBasicReadWriteObject(new Locale("en", "us"), "java.util.Locale:en_US\n");
    }
}
