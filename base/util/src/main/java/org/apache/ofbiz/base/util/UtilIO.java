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
package org.apache.ofbiz.base.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class UtilIO {
    private static final String MODULE = UtilIO.class.getName();

    private UtilIO() { }

    /** Copy a Reader to an Appendable.
     * @param reader the Reader to copy from
     * @param out the Appendable to copy to
     * @throws IOException if an error occurs
     */
    public static void copy(Reader reader, Appendable out) throws IOException {
        CharBuffer buffer = CharBuffer.allocate(4096);
        while (reader.read(buffer) > 0) {
            buffer.flip();
            buffer.rewind();
            out.append(buffer);
        }
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     * @param bytes the array of bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes) {
        return readString(bytes, 0, bytes.length, StandardCharsets.UTF_8);
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  The conversion is limited to the specified offset/length
     * pair, and uses a default {@link Charset UTF-8} charset.
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes, int offset, int length) {
        return readString(bytes, offset, length, StandardCharsets.UTF_8);
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  The conversion is limited to the specified offset/length
     * pair, and uses the requested charset to decode the bytes.
     * @param bytes the array of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes, String charset) {
        return readString(bytes, 0, bytes.length, Charset.forName(charset));
    }

    /** Convert a byte array to a string; consistently uses \n line
     * endings in java.  The conversion is limited to the specified
     * offset/length  pair, and uses the requested charset to decode the
     * bytes.
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes, int offset, int length, String charset) {
        return readString(bytes, 0, bytes.length, Charset.forName(charset));
    }

    /** Convert a byte array to a string, consistently uses \n line
     * endings in java.  The specified {@link Charset charset} is used
     * to decode the bytes.
     * @param bytes the array of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes, Charset charset) {
        return readString(bytes, 0, bytes.length, charset);
    }

    /** Convert a byte array to a string, consistently uses \n line
     * endings in java.  The conversion is limited to the specified
     * offset/length  pair, and uses the requested {@link Charset
     * charset} to decode the bytes.
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(byte[] bytes, int offset, int length, Charset charset) {
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(bytes, offset, length);
        buf.flip();
        return filterLineEndings(new StringBuilder(charset.decode(buf).toString())).toString();
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     * @param stream the stream of bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(InputStream stream) throws IOException {
        return readString(stream, StandardCharsets.UTF_8);
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     * @param stream the stream of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(InputStream stream, String charset) throws IOException {
        return readString(stream, Charset.forName(charset));
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     * @param stream the stream of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(InputStream stream, Charset charset) throws IOException {
        return readString(new InputStreamReader(new BufferedInputStream(stream), charset));
    }

    /** Convert an {@link Reader} to a string; consistently uses \n line endings
     * in java.
     * @param reader the stream of characters convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static String readString(Reader reader) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int r;
            while ((r = reader.read(buf, 0, 4096)) > 0) {
                sb.append(buf, 0, r);
            }
            return filterLineEndings(sb).toString();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, "Error closing after reading text: " + e.toString(), MODULE);
            }
        }
    }

    private static StringBuilder filterLineEndings(StringBuilder sb) {
        String nl = System.getProperty("line.separator");
        if (!"\n".equals(nl)) {
            int r = 0;
            while (r < sb.length()) {
                int i = sb.indexOf(nl, r);
                if (i == -1) {
                    break;
                }
                sb.replace(i, i + nl.length(), "\n");
                r = i + 1;
            }
        }
        return sb;
    }

    /** Convert a \n string to a platform encoding.  This uses a default
     * {@link Charset UTF-8} charset.
     * @param file where to write the converted bytes to
     * @param value the value to write
     */
    public static void writeString(File file, String value) throws IOException {
        writeString(new FileOutputStream(file), StandardCharsets.UTF_8, value);
    }

    /** Convert a \n string to a platform encoding.  This uses a default
     * {@link Charset UTF-8} charset.
     * @param out where to write the converted bytes to
     * @param value the value to write
     */
    public static void writeString(OutputStream out, String value) throws IOException {
        writeString(out, StandardCharsets.UTF_8, value);
    }

    /** Convert a \n string to a platform encoding.  This uses the
     * specified charset to extract the raw bytes.
     * @param out where to write the converted bytes to
     * @param charset the charset to use to convert the raw bytes
     * @param value the value to write
     */
    public static void writeString(OutputStream out, String charset, String value) throws IOException {
        writeString(out, Charset.forName(charset), value);
    }

    /** Convert a \n string to a platform encoding.  This uses the
     * specified charset to extract the raw bytes.
     * @param out where to write the converted bytes to
     * @param charset the charset to use to convert the raw bytes
     * @param value the value to write
     * @throws IOException
     */
    public static void writeString(OutputStream out, Charset charset, String value) throws IOException {
        try (Writer writer = new OutputStreamWriter(out, charset)) {
            String nl = System.getProperty("line.separator");
            int r = 0;
            while (r < value.length()) {
                int i = value.indexOf("\n", r);
                if (i == -1) {
                    break;
                }
                writer.write(value.substring(r, i));
                writer.write(nl);
                r = i + 1;
            }
            writer.write(value.substring(r));
        }
    }
}
