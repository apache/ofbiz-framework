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
package org.ofbiz.base.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.IndentingWriter;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilIO;

public class JSONWriter {
    private final IndentingWriter writer;
    private final FallbackHandler fallbackHandler;

    public JSONWriter(IndentingWriter writer) {
        this(writer, StandardFallbackHandler);
    }

    public JSONWriter(IndentingWriter writer, FallbackHandler fallbackHandler) {
        this.writer = writer;
        this.fallbackHandler = fallbackHandler;
    }

    public JSONWriter(Writer writer) {
        this(writer instanceof IndentingWriter ? (IndentingWriter) writer : new IndentingWriter(writer));
    }

    public JSONWriter(Writer writer, FallbackHandler fallbackHandler) {
        this(writer instanceof IndentingWriter ? (IndentingWriter) writer : new IndentingWriter(writer), fallbackHandler);
    }

    public IndentingWriter getWriter() {
        return writer;
    }

    public JSONWriter close() throws IOException {
        getWriter().close();
        return this;
    }

    public JSONWriter write(byte b) throws IOException {
        writer.write(Byte.toString(b));
        return this;
    }

    public JSONWriter write(short s) throws IOException {
        writer.write(Short.toString(s));
        return this;
    }

    public JSONWriter write(int i) throws IOException {
        writer.write(Integer.toString(i));
        return this;
    }

    public JSONWriter write(long l) throws IOException {
        writer.write(Long.toString(l));
        return this;
    }

    public JSONWriter write(float f) throws IOException {
        writer.write(Float.toString(f));
        return this;
    }

    public JSONWriter write(double d) throws IOException {
        writer.write(Double.toString(d));
        return this;
    }

    public JSONWriter write(char c) throws IOException {
        write(Character.toString(c));
        return this;
    }

    public JSONWriter write(String s) throws IOException {
        writer.write('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':  writer.write("\\\\"); continue;
                case '/':  writer.write("\\/"); continue;
                case '"':  writer.write("\\\""); continue;
                case '\b':  writer.write("\\b"); continue;
                case '\f':  writer.write("\\f"); continue;
                case '\n':  writer.write("\\n"); continue;
                case '\r':  writer.write("\\r"); continue;
                case '\t':  writer.write("\\t"); continue;
            }
            if (32 <= c && c >= 256) {
                writer.write("\\u");
                String n = Integer.toString((int) c, 16);
                for (int j = 4 - n.length(); j > 0; j--) writer.write('0');
                writer.write(n);
            } else {
                writer.write(c);
            }
        }
        writer.write('"');
        return this;
    }

    public <K, V> JSONWriter write(Map<K, V> m) throws IOException {
        writer.write('{');
        writer.push();
        Iterator<Map.Entry<K, V>> it = m.entrySet().iterator();
        if (it.hasNext()) writer.newline();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            write(entry.getKey());
            writer.write(':');
            writer.space();
            write(entry.getValue());
            if (it.hasNext()) writer.write(',');
            writer.newline();
        }
        writer.pop();
        writer.write('}');
        return this;
    }

    public <E> JSONWriter write(Collection<E> c) throws IOException {
        writer.write('[');
        writer.push();
        Iterator<E> it = c.iterator();
        if (it.hasNext()) writer.newline();
        while (it.hasNext()) {
            write(it.next());
            if (it.hasNext()) writer.write(',');
            writer.newline();
        }
        writer.pop();
        writer.write(']');
        return this;
    }

    public <T> JSONWriter write(T... o) throws IOException {
        writer.write('[');
        writer.push();
        for (int i = 0; i < o.length; i++) {
            if (i != 0) writer.write(',');
            writer.newline();
            write(o[i]);
        }
        if (o.length > 0) writer.newline();
        writer.pop();
        writer.write(']');
        return this;
    }

    public JSONWriter write(Object o) throws IOException {
        if (o == null) {
            writer.write("null");
            return this;
        } else if (o instanceof Boolean) {
            writer.write(((Boolean) o).booleanValue() ? "true" : "false");
            return this;
        } else if (o instanceof String) {
            return write((String) o);
        } else if (o instanceof Map) {
            return write(UtilGenerics.<Map<?, ?>>cast(o));
        } else if (o instanceof Collection) {
            return write(UtilGenerics.<Collection<?>>cast(o));
        } else if (o instanceof Byte) {
            return write(((Byte) o).byteValue());
        } else if (o instanceof Character) {
            return write(((Character) o).charValue());
        } else if (o instanceof Double) {
            return write(((Double) o).doubleValue());
        } else if (o instanceof Float) {
            return write(((Float) o).floatValue());
        } else if (o instanceof Integer) {
            return write(((Integer) o).intValue());
        } else if (o instanceof Long) {
            return write(((Long) o).longValue());
        } else if (o instanceof Short) {
            return write(((Short) o).shortValue());
        } else if (o.getClass().isArray()) {
            return write((Object[]) o);
        } else {
            fallbackHandler.writeJSON(this, writer, o);
            return this;
        }
    }

    public interface FallbackHandler {
        void writeJSON(JSONWriter json, Writer writer, Object o) throws IOException;
    }

    public static final FallbackHandler StandardFallbackHandler = new FallbackHandler() {
        public void writeJSON(JSONWriter json, Writer writer, Object o) throws IOException {
            throw new IOException("Can't write(" + o + ":" + o.getClass() + ")");
        }
    };

    public static final FallbackHandler ResolvingFallbackHandler = new FallbackHandler() {
        public void writeJSON(JSONWriter json, Writer writer, Object o) throws IOException {
            StringBuilder sb = new StringBuilder();
            UtilIO.writeObject(sb, o);
            writer.write("resolve(");
            json.write(sb.toString());
            writer.write(")");
        }
    };
}
