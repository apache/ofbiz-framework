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

package org.apache.ofbiz.common.authentication.api;

import java.util.List;

import org.apache.ofbiz.base.util.GeneralException;

/**
 * AuthenticatorException
 */
public class AuthenticatorException extends GeneralException {

    private static final long serialVersionUID = 2836939874682240962L;

    /**
     * Creates new <code>GeneralException</code> without detail message.
     */
    public AuthenticatorException() {
        super();
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public AuthenticatorException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     *
     * @param msg    the detail message.
     * @param nested the nested exception.
     */
    public AuthenticatorException(String msg, Throwable nested) {
        super(msg, nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message and nested Exception.
     *
     * @param nested the nested exception.
     */
    public AuthenticatorException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     *
     * @param msg      the detail message.
     * @param messages error message list.
     */
    public AuthenticatorException(String msg, List<String> messages) {
        super(msg, messages);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message, list and nested Exception.
     *
     * @param msg      the detail message.
     * @param messages error message list.
     * @param nested   the nested exception
     */
    public AuthenticatorException(String msg, List<String> messages, Throwable nested) {
        super(msg, messages, nested);
    }

    /**
     * Constructs an <code>GeneralException</code> with the specified detail message list and nested Exception.
     *
     * @param messages error message list.
     * @param nested   the nested exception.
     */
    public AuthenticatorException(List<String> messages, Throwable nested) {
        super(messages, nested);
    }

    public AuthenticatorException(List<String> messages) {
        super(messages);
    }
}
