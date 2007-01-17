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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Writer implementation for writing to a log4j logger.
 *
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

