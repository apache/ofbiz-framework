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
package org.ofbiz.base.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.container.ClassLoaderContainer;
import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.Converters;
import org.ofbiz.base.json.JSON;
import org.ofbiz.base.json.JSONWriter;

public final class UtilIO {
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final String module = UtilIO.class.getName();

    /** Copy an InputStream to an OutputStream, optionally closing either
     *  the input or the output.
     *
     * @param in the InputStream to copy from
     * @param closeIn whether to close the input when the copy is done
     * @param out the OutputStream to copy to
     * @param closeOut whether to close the output when the copy is done
     * @throws IOException if an error occurs
     */
    public static void copy(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
        try {
            try {
                IOUtils.copy(in, out);
            } finally {
                if (closeIn) IOUtils.closeQuietly(in);
            }
        } finally {
            if (closeOut) IOUtils.closeQuietly(out);
        }
    }

    /** Copy a Reader to a Writer, optionally closing either the input or
     *  the output.
     *
     * @param reader the Reader to copy from
     * @param closeIn whether to close the input when the copy is done
     * @param writer the Writer to copy to
     * @param closeOut whether to close the output when the copy is done
     * @throws IOException if an error occurs
     */
    public static void copy(Reader reader, boolean closeIn, Writer writer, boolean closeOut) throws IOException {
        try {
            try {
                IOUtils.copy(reader, writer);
            } finally {
                if (closeIn) IOUtils.closeQuietly(reader);
            }
        } finally {
            if (closeOut) IOUtils.closeQuietly(writer);
        }
    }

    /** Copy a Reader to an Appendable, optionally closing the input.
     *
     * @param reader the Reader to copy from
     * @param closeIn whether to close the input when the copy is done
     * @param out the Appendable to copy to
     * @throws IOException if an error occurs
     */
    public static void copy(Reader reader, boolean closeIn, Appendable out) throws IOException {
        try {
            CharBuffer buffer = CharBuffer.allocate(4096);
            while (reader.read(buffer) > 0) {
                buffer.flip();
                buffer.rewind();
                out.append(buffer);
            }
        } finally {
            if (closeIn) IOUtils.closeQuietly(reader);
        }
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     *
     * @param bytes the array of bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(byte[] bytes) throws IOException {
        return readString(bytes, 0, bytes.length, UTF8);
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  The conversion is limited to the specified offset/length
     * pair, and uses a default {@link Charset UTF-8} charset.
     *
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(byte[] bytes, int offset, int length) throws IOException {
        return readString(bytes, offset, length, UTF8);
    }

    /** Convert a byte array to a string; consistently uses \n line endings
     * in java.  The conversion is limited to the specified offset/length
     * pair, and uses the requested charset to decode the bytes.
     *
     * @param bytes the array of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(byte[] bytes, String charset) throws IOException {
        return readString(bytes, 0, bytes.length, Charset.forName(charset));
    }

    /** Convert a byte array to a string; consistently uses \n line
     * endings in java.  The conversion is limited to the specified
     * offset/length  pair, and uses the requested charset to decode the
     * bytes.
     *
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(byte[] bytes, int offset, int length, String charset) throws IOException {
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
    public static final String readString(byte[] bytes, Charset charset) throws IOException {
        return readString(bytes, 0, bytes.length, charset);
    }

    /** Convert a byte array to a string, consistently uses \n line
     * endings in java.  The conversion is limited to the specified
     * offset/length  pair, and uses the requested {@link Charset
     * charset} to decode the bytes.
     *
     * @param bytes the array of bytes to convert
     * @param offset the start of the conversion
     * @param length how many bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(byte[] bytes, int offset, int length, Charset charset) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(bytes, offset, length);
        buf.flip();
        return filterLineEndings(new StringBuilder(charset.decode(buf).toString())).toString();
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     *
     * @param stream the stream of bytes to convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(InputStream stream) throws IOException {
        return readString(stream, UTF8);
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     *
     * @param stream the stream of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(InputStream stream, String charset) throws IOException {
        return readString(stream, Charset.forName(charset));
    }

    /** Convert an {@link InputStream} to a string; consistently uses \n line endings
     * in java.  This uses a default {@link Charset UTF-8} charset.
     *
     * @param stream the stream of bytes to convert
     * @param charset the charset to use to convert the raw bytes
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(InputStream stream, Charset charset) throws IOException {
        return readString(new InputStreamReader(new BufferedInputStream(stream), charset));
    }

    /** Convert an {@link Reader} to a string; consistently uses \n line endings
     * in java.
     *
     * @param reader the stream of characters convert
     * @return the converted string, with platform line endings converted
     * to \n
     */
    public static final String readString(Reader reader) throws IOException {
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
                Debug.logError(e, "Error closing after reading text: " + e.toString(), module);
            }
        }
    }

    private static StringBuilder filterLineEndings(StringBuilder sb) {
        String nl = System.getProperty("line.separator");
        if (!nl.equals("\n")) {
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
     *
     * @param file where to write the converted bytes to
     * @param value the value to write
     */
    public static void writeString(File file, String value) throws IOException {
        writeString(new FileOutputStream(file), UTF8, value);
    }

    /** Convert a \n string to a platform encoding.  This uses a default
     * {@link Charset UTF-8} charset.
     *
     * @param out where to write the converted bytes to
     * @param value the value to write
     */
    public static void writeString(OutputStream out, String value) throws IOException {
        writeString(out, UTF8, value);
    }

    /** Convert a \n string to a platform encoding.  This uses the
     * specified charset to extract the raw bytes.
     *
     * @param out where to write the converted bytes to
     * @param charset the charset to use to convert the raw bytes
     * @param value the value to write
     */
    public static void writeString(OutputStream out, String charset, String value) throws IOException {
        writeString(out, Charset.forName(charset), value);
    }

    /** Convert a \n string to a platform encoding.  This uses the
     * specified charset to extract the raw bytes.
     *
     * @param out where to write the converted bytes to
     * @param charset the charset to use to convert the raw bytes
     * @param value the value to write
     */
    public static void writeString(OutputStream out, Charset charset, String value) throws IOException {
        Writer writer = new OutputStreamWriter(out, charset);
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
        writer.close();
    }

    public static Object readObject(File file) throws ClassNotFoundException, IOException {
        return readObject(new FileInputStream(file), false);
    }

    public static Object readObject(File file, boolean allowJsonResolve) throws ClassNotFoundException, IOException {
        return readObject(new FileInputStream(file), allowJsonResolve);
    }

    public static Object readObject(InputStream in) throws ClassNotFoundException, IOException {
        return readObject(in, false);
    }

    public static Object readObject(InputStream in, boolean allowJsonResolve) throws ClassNotFoundException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(in, baos);
        in.close();
        byte[] bytes = baos.toByteArray();
        try {
            char[] buffer = StringUtils.chomp(readString(bytes)).toCharArray();
            return parseObject(buffer, 0, buffer.length, allowJsonResolve);
        } catch (Exception e) {
        }
        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Serializable value = (Serializable) oin.readObject();
        oin.close();
        return value;
    }

    public static Object readObject(char[] buffer) throws ClassNotFoundException, IOException {
        return parseObject(buffer, 0, buffer.length, false);
    }

    public static Object readObject(char[] buffer, int offset, int length) throws ClassNotFoundException, IOException {
        return parseObject(buffer, offset, length, false);
    }

    private static <S, T> T convertObject(Class<S> sourceClass, S value, Class<T> targetClass) throws Exception {
        Converter<S, T> converter = Converters.getConverter(sourceClass, targetClass);
        return converter.convert(targetClass, value);
    }

    private static Object parseObject(char[] buffer, int offset, int length, boolean allowJsonResolve) throws ClassNotFoundException, IOException {
        try {
            int i;
            for (i = offset; i < length && buffer[i] != ':'; i++);
            if (i > offset && i < length) {
                String className = new String(buffer, offset, i);
                Class<?> type = Class.forName(className, true, ClassLoaderContainer.getClassLoader());
                if (buffer[length - 1] == '\n') {
                    length--;
                }
                return convertObject(String.class, new String(buffer, i + 1, length - i - 1), type);
            }
        } catch (Exception e) {
        }
        try {
            return new JSON(new StringReader(new String(buffer, offset, length))).allowResolve(allowJsonResolve).JSONValue();
        } catch (Error e) {
        } catch (Exception e) {
        }
        throw new IOException("Can't read (" + new String(buffer, offset, length) + ")");
    }

    public static void writeObject(File file, Object value) throws IOException {
        writeObject(new FileOutputStream(file), value, false);
    }

    public static void writeObject(File file, Object value, boolean allowJsonResolve) throws IOException {
        writeObject(new FileOutputStream(file), value, allowJsonResolve);
    }

    public static void writeObject(OutputStream out, Object value) throws IOException {
        writeObject(out, value, false);
    }

    public static void writeObject(OutputStream out, Object value, boolean allowJsonResolve) throws IOException {
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF8));
            if (encodeObject(writer, value, allowJsonResolve)) {
                writer.println();
                writer.close();
                return;
            }
        } catch (Exception e) {
        }
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(value);
        oout.close();
        out.close();
    }

    private static <T> boolean encodeObject(Writer writer, T value, boolean allowJsonResolve) throws Exception {
        Converter<T, String> converter = UtilGenerics.cast(Converters.getConverter(value.getClass(), String.class));
        if (converter != null) {
            Class<?> clz = converter.getSourceClass();
            String str = converter.convert(value);
            if (clz != null) {
                writer.write(clz.getName());
            } else {
                writer.write(value.getClass().getName());
            }
            writer.write(':');
            writer.write(str);
            return true;
        } else {
            StringWriter sw = new StringWriter();
            IndentingWriter indenting = new IndentingWriter(writer, true, false);
            JSONWriter jsonWriter;
            if (allowJsonResolve) {
                jsonWriter = new JSONWriter(indenting, JSONWriter.ResolvingFallbackHandler);
            } else {
                jsonWriter = new JSONWriter(indenting);
            }
            jsonWriter.write(value);
            writer.write(sw.toString());
            return true;
        }
    }

    public static void writeObject(StringBuilder sb, Object value) throws IOException {
        writeObject(sb, value, false);
    }

    public static void writeObject(StringBuilder sb, Object value, boolean allowJsonResolve) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            if (encodeObject(writer, value, allowJsonResolve)) {
                sb.append(writer.toString());
                return;
            }
        } catch (Exception e) {} //Empty catch because writeObject() calls encodeObject(), which *always* returns true, unless an error occurs.  
        throw new IOException("Can't write (" + value + ")");            
    }
}
