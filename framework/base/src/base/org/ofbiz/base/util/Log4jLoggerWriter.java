/*
 * $Id: Log4jLoggerWriter.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.base.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Writer implementation for writing to a log4j logger.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class Log4jLoggerWriter extends PrintWriter {

    public Log4jLoggerWriter(Logger logger) {
        this(logger, Priority.INFO);
    }

    public Log4jLoggerWriter(Logger logger, Priority priority) {
        super(new Log4jPrintWriter(logger, priority), true);
    }

    static class Log4jPrintWriter extends Writer {

        private Logger logger = null;
        private Priority priority = null;
        private boolean closed = false;

        public Log4jPrintWriter(Logger logger, Priority priority) {
            lock = logger;
            this.logger = logger;
            this.priority = priority;
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Writer is closed");
            }

            // Remove the eol
            while (len > 0 && (cbuf[len - 1] == '\n' || cbuf[len - 1] == '\r')) {
                len--;
            }

            // send to log4j
            if (len > 0) {
                logger.log(priority, String.copyValueOf(cbuf, off, len));
            }
        }

        public void flush() throws IOException {
            if (closed) {
                throw new IOException("Writer is closed");
            }
        }

        public void close() {
            closed = true;
        }
    }
}

