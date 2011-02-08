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
package org.ofbiz.base.lang;

public interface ObjectWrapper<T> {
    T getObject() throws ObjectException;

    @SuppressWarnings("serial")
    public class ObjectException extends Exception {
        protected ObjectException(Throwable cause) {
            this(cause.getMessage(), cause);
        }

        protected ObjectException(String msg, Throwable cause) {
            super(msg, cause);
        }

        protected ObjectException(String msg) {
            super(msg);
        }

        public static final <T> T checkException(Throwable t) throws ObjectException {
            if (t instanceof ObjectException) throw (ObjectException) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            throw new NestedException(t);
        }
    }

    @SuppressWarnings("serial")
    public class NestedException extends ObjectException {
        public NestedException(Throwable cause) {
            super(cause);
        }
    }

    @SuppressWarnings("serial")
    public class ConfigurationException extends RuntimeException {
        public ConfigurationException(String msg) {
            super(msg);
        }
    }
}
