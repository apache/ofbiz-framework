/*
 * $Id: GeneralException.java 6419 2005-12-23 17:18:49Z jaz $
 *
 *  Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Base OFBiz Exception, provides nested exceptions, etc
 *
 *@author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 *@version    $Rev$
 *@since      1.0
 */
public class GeneralException extends Exception {

    Throwable nested = null;
    List messages = null;

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
        super(msg);
        this.nested = nested;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     * @param nested the nested exception.
     */
    public GeneralException(Throwable nested) {
        super();
        this.nested = nested;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     * @param msg the detail message.
     * @param messages error message list.
     */
    public GeneralException(String msg, List messages) {
        super(msg);        
        this.messages = messages;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     * @param msg the detail message.
     * @param messages error message list.
     * @param nested the nexted exception
     */
    public GeneralException(String msg, List messages, Throwable nested) {
        super(msg);
        this.nested = nested;
        this.messages = messages;
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message list and nested Exception.
     * @param messages error message list.
     * @param nested the nested exception.
     */
    public GeneralException(List messages, Throwable nested) {
        super();
        this.nested = nested;
        this.messages = messages;
    }

    public GeneralException(List messages) {
        super();
        this.messages = messages;
    }

    /** Returns the detail message, including the message from the nested exception if there is one. */
    public String getMessage() {
        if (nested != null) {
            if (super.getMessage() == null) {
                return nested.getMessage();
            } else {
                return super.getMessage() + " (" + nested.getMessage() + ")";
            }
        } else {
            return super.getMessage();
        }
    }

    public List getMessageList() {
        return this.messages;
    }

    /** Returns the detail message, NOT including the message from the nested exception. */
    public String getNonNestedMessage() {
        return super.getMessage();
    }

    /** Returns the nested exception if there is one, null if there is not. */
    public Throwable getNested() {
        if (nested == null) {
            return this;
        }
        return nested;
    }

    /** Prints the composite message to System.err. */
    public void printStackTrace() {
        super.printStackTrace();
        if (nested != null) nested.printStackTrace();
    }

    /** Prints the composite message and the embedded stack trace to the specified stream ps. */
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (nested != null) nested.printStackTrace(ps);
    }

    /** Prints the composite message and the embedded stack trace to the specified print writer pw. */
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (nested != null) nested.printStackTrace(pw);
    }
}

