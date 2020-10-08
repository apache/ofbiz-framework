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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Base OFBiz Exception, provides nested exceptions, etc
 *
 */
@SuppressWarnings("serial")
public class GeneralException extends Exception {

    public static <T> T checkException(Throwable t) throws GeneralException {
        return GeneralException.<T>checkException(t.getMessage(), t);
    }

    public static <T> T checkException(String message, Throwable t) throws GeneralException {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof GeneralException) {
            throw (GeneralException) t;
        }
        throw (GeneralException) new GeneralException(message).initCause(t);
    }

    private List<String> messages = null;

    /**
     * Creates new <code>GeneralException</code> without detail message.
     */
    public GeneralException() {
        super();
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public GeneralException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     * @param msg the detail message.
     * @param nested the nested exception.
     */
    public GeneralException(String msg, Throwable nested) {
        super(msg, nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     * @param nested the nested exception.
     */
    public GeneralException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     * @param msg the detail message.
     * @param messages error message list.
     */
    public GeneralException(String msg, List<String> messages) {
        super(msg);
        this.messages = messages;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     * @param msg the detail message.
     * @param messages error message list.
     * @param nested the nexted exception
     */
    public GeneralException(String msg, List<String> messages, Throwable nested) {
        super(msg, nested);
        this.messages = messages;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message list and nested Exception.
     * @param messages error message list.
     * @param nested the nested exception.
     */
    public GeneralException(List<String> messages, Throwable nested) {
        super(nested);
        this.messages = messages;
    }

    public GeneralException(List<String> messages) {
        super();
        this.messages = messages;
    }

    /** Returns the detail message, including the message from the nested exception if there is one. */
    @Override
    public String getMessage() {
        Throwable nested = getCause();
        if (nested != null) {
            if (super.getMessage() == null) {
                return nested.getMessage();
            }
            return super.getMessage() + " (" + nested.getMessage() + ")";
        }
        return super.getMessage();
    }

    /**
     * Gets message list.
     * @return the message list
     */
    public List<String> getMessageList() {
        return this.messages;
    }

    /** Returns the detail message, NOT including the message from the nested exception. */
    public String getNonNestedMessage() {
        return super.getMessage();
    }

    /** Returns the nested exception if there is one, null if there is not. */
    public Throwable getNested() {
        Throwable nested = getCause();
        if (nested == null) {
            return this;
        }
        return nested;
    }

    /** Prints the composite message to System.err. */
    @Override
    public void printStackTrace() {
        super.printStackTrace();
    }

    /** Prints the composite message and the embedded stack trace to the specified stream ps. */
    @Override
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
    }

    /** Prints the composite message and the embedded stack trace to the specified print writer pw. */
    @Override
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
    }
}

