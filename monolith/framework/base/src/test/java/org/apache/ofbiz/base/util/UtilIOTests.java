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

import static org.apache.ofbiz.base.util.UtilIO.readString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class UtilIOTests {
    private static final byte[] TRADEMARK_BYTES = new byte[] {
        (byte) 0xE2, (byte) 0x84, (byte) 0xA2
    };

    @Test
    public void testReadString() throws Exception {
        readStringTest0("unix line ending", "\n", new byte[] {0x0A});
        readStringTest0("mac line ending", "\r", new byte[] {0x0D});
        readStringTest0("windows line ending", "\r\n", new byte[] {0x0D, 0x0A});
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

    private static void readStringTest0(String label, String lineSeparator, byte[] extra) throws IOException {
        String originalLineSeparator = System.getProperty("line.separator");
        try {
            System.getProperties().put("line.separator", lineSeparator);
            readStringTest1(label + ":mark", "\u2122", join(TRADEMARK_BYTES));
            readStringTest1(label + ":mark NL", "\u2122\n", join(TRADEMARK_BYTES, extra));
            readStringTest1(label + ":NL mark", "\n\u2122", join(extra, TRADEMARK_BYTES));
        } finally {
            System.getProperties().put("line.separator", originalLineSeparator);
        }
    }

    private static void readStringTest1(String label, String wanted, byte[] toRead) throws IOException {
        assertEquals(wanted, readString(toRead), "readString bytes default:" + label);
        assertEquals(wanted, readString(toRead, "UTF-8"), "readString bytes UTF-8:" + label);
        assertEquals(wanted, readString(toRead, StandardCharsets.UTF_8), "readString bytes UTF8:" + label);
        assertEquals(wanted, readString(toRead, 0, toRead.length), "readString bytes offset/length default:" + label);
        assertEquals(wanted, readString(toRead, 0, toRead.length, "UTF-8"), "readString bytes offset/length UTF-8:" + label);
        assertEquals(wanted, readString(toRead, 0, toRead.length, StandardCharsets.UTF_8), "readString bytes offset/length UTF8:" + label);
        assertEquals(wanted, readString(new ByteArrayInputStream(toRead)), "readString stream default:" + label);
        assertEquals(wanted, readString(new ByteArrayInputStream(toRead), "UTF-8"), "readString stream UTF-8:" + label);
        assertEquals(wanted, readString(new ByteArrayInputStream(toRead), StandardCharsets.UTF_8), "readString stream UTF8:" + label);
    }

    @Test
    public void testWriteString() throws Exception {
        writeStringTest0("unix line ending", "\n", new byte[] {0x0A});
        writeStringTest0("mac line ending", "\r", new byte[] {0x0D});
        writeStringTest0("windows line ending", "\r\n", new byte[] {0x0D, 0x0A});
    }

    private static void writeStringTest0(String label, String lineSeparator, byte[] extra) throws IOException {
        String originalLineSeparator = System.getProperty("line.separator");
        try {
            System.getProperties().put("line.separator", lineSeparator);
            writeStringTest1(label + ":mark", join(TRADEMARK_BYTES), "\u2122");
            writeStringTest1(label + ":mark NL", join(TRADEMARK_BYTES, extra), "\u2122\n");
            writeStringTest1(label + ":NL mark", join(extra, TRADEMARK_BYTES), "\n\u2122");
        } finally {
            System.getProperties().put("line.separator", originalLineSeparator);
        }
    }

    private static void writeStringTest1(String label, byte[] wanted, String toWrite) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, toWrite);
        assertArrayEquals(wanted, baos.toByteArray(), "writeString default:" + label);
        baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, "UTF-8", toWrite);
        assertArrayEquals(wanted, baos.toByteArray(), "writeString UTF-8:" + label);
        baos = new ByteArrayOutputStream();
        UtilIO.writeString(baos, StandardCharsets.UTF_8, toWrite);
        assertArrayEquals(wanted, baos.toByteArray(), "writeString UTF8:" + label);
    }
}
