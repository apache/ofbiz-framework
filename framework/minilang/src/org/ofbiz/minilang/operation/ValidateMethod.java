/*
 * $Id: ValidateMethod.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.operation;

import java.util.*;
import java.lang.reflect.*;

import org.w3c.dom.*;

import org.ofbiz.base.util.*;

/**
 * A string operation that calls a validation method
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ValidateMethod extends SimpleMapOperation {
    
    public static final String module = ValidateMethod.class.getName();
    
    String methodName;
    String className;

    public ValidateMethod(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        this.methodName = element.getAttribute("method");
        this.className = element.getAttribute("class");
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(fieldName);

        String fieldValue = null;

        try {
            fieldValue = (String) ObjectType.simpleTypeConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert field value for comparison: " + e.getMessage());
            return;
        }

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        Class[] paramTypes = new Class[] {String.class};
        Object[] params = new Object[] {fieldValue};

        Class valClass;

        try {
            valClass = loader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            String msg = "Could not find validation class: " + className;

            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, module);
            return;
        }

        Method valMethod;

        try {
            valMethod = valClass.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException cnfe) {
            String msg = "Could not find validation method: " + methodName + " of class " + className;

            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, module);
            return;
        }

        Boolean resultBool = Boolean.FALSE;

        try {
            resultBool = (Boolean) valMethod.invoke(null, params);
        } catch (Exception e) {
            String msg = "Error in validation method " + methodName + " of class " + className + ": " + e.getMessage();

            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, module);
            return;
        }

        if (!resultBool.booleanValue()) {
            addMessage(messages, loader, locale);
        }
    }
}
