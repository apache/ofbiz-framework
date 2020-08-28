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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public class IndentingWriter extends FilterWriter {
    private final StringBuilder indent = new StringBuilder();
    private final boolean doSpace;
    private final boolean doNewline;
    private boolean lastWasNewline;

    public static IndentingWriter makeIndentingWriter(Writer writer) {
        return writer instanceof IndentingWriter ? (IndentingWriter) writer : new IndentingWriter(writer);
    }

    public IndentingWriter(Writer out, boolean doSpace, boolean doNewline) {
        super(out);
        this.doSpace = doSpace;
        this.doNewline = doNewline;
    }

    public IndentingWriter(Writer out) {
        this(out, true, true);
    }

    /**
     * Newline indenting writer.
     * @return the indenting writer
     * @throws IOException the io exception
     */
    public IndentingWriter newline() throws IOException {
        lastWasNewline = true;
        if (doNewline) {
            super.write('\n');
        }
        return this;
    }

    /**
     * Check after newline.
     * @throws IOException the io exception
     */
    protected void checkAfterNewline() throws IOException {
        if (lastWasNewline) {
            if (doSpace) {
                if (doNewline) {
                    super.write(indent.toString(), 0, indent.length());
                } else {
                    super.write(' ');
                }
            }
            lastWasNewline = false;
        }
    }

    /**
     * Push indenting writer.
     * @return the indenting writer
     */
    public IndentingWriter push() {
        indent.append(' ');
        return this;
    }

    /**
     * Pop indenting writer.
     * @return the indenting writer
     */
    public IndentingWriter pop() {
        indent.setLength(indent.length() - 1);
        return this;
    }

    /**
     * Space indenting writer.
     * @return the indenting writer
     * @throws IOException the io exception
     */
    public IndentingWriter space() throws IOException {
        checkAfterNewline();
        if (doSpace) {
            super.write(' ');
        }
        return this;
    }

    @Override
    public void write(char[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(char[] buf, int offset, int length) throws IOException {
        int i;
        for (i = offset; i < length; i++) {
            if (buf[i] == '\n') {
                checkAfterNewline();
                super.write(buf, offset, i - offset + 1);
                offset = i + 1;
                lastWasNewline = true;
            }
        }
        checkAfterNewline();
        super.write(buf, offset, i - offset);
    }

    @Override
    public void write(int c) throws IOException {
        checkAfterNewline();
        super.write(c);
        if (c == '\n') {
            lastWasNewline = true;
            checkAfterNewline();
        }
    }

    @Override
    public void write(String s) throws IOException {
        write(s, 0, s.length());
    }

    @Override
    public void write(String s, int offset, int length) throws IOException {
        int i;
        for (i = offset; i < length; i++) {
            if (s.charAt(i) == '\n') {
                checkAfterNewline();
                super.write(s, offset, i - offset + 1);
                offset = i + 1;
                lastWasNewline = true;
            }
        }
        checkAfterNewline();
        super.write(s, offset, i - offset);
    }
}
