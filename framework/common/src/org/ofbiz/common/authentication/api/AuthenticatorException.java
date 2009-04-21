package org.ofbiz.common.authentication.api;

import java.util.List;

import org.ofbiz.base.util.GeneralException;

/**
 * AuthenticatorException
 */
public class AuthenticatorException extends GeneralException {

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
     * @param nested   the nexted exception
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
