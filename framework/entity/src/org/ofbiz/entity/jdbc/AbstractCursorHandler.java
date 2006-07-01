/*
 * $Id: AbstractCursorHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 * @version    $Rev$
 * @since      3.1
 */
public abstract class AbstractCursorHandler implements InvocationHandler {
    
    protected String cursorName;
    protected int fetchSize;

    protected AbstractCursorHandler(String cursorName, int fetchSize) {
        this.cursorName = cursorName;
        this.fetchSize = fetchSize;
    }

    public void setCursorName(String cursorName) {
        this.cursorName = cursorName;
    }

    public String getCursorName() {
        return cursorName;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    protected Object invoke(Object obj, Object proxy, Method method, Object[] args) throws Throwable {
        if ("toString".equals(method.getName())) {
            String str = obj.toString();
            return getClass().getName() + "{" + str + "}";
        }
        return method.invoke(obj, args);
    }

    protected static Object newHandler(InvocationHandler handler, Class implClass) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
        ClassLoader loader = implClass.getClassLoader();
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        Class proxyClass = Proxy.getProxyClass(loader, new Class[]{implClass});
        Constructor constructor = proxyClass.getConstructor(new Class[]{InvocationHandler.class});
        return constructor.newInstance(new Object[]{handler});
    }
}
