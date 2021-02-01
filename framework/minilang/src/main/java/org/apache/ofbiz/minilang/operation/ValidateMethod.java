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
package org.apache.ofbiz.minilang.operation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.w3c.dom.Element;

/**
 * A string operation that calls a validation method
 */
public class ValidateMethod extends SimpleMapOperation {

    private static final String MODULE = ValidateMethod.class.getName();

    private String className;
    private String methodName;

    public ValidateMethod(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        this.methodName = element.getAttribute("method");
        this.className = MiniLangValidate.checkAttribute(element.getAttribute("class"), "org.apache.ofbiz.base.util.UtilValidate");
    }

    @Override
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        Object obj = inMap.get(getFieldName());
        String fieldValue = null;
        try {
            fieldValue = (String) ObjectType.simpleTypeOrObjectConvert(obj, "String", null, locale);
        } catch (GeneralException e) {
            messages.add("Could not convert field value for comparison: " + e.getMessage());
            return;
        }
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        Class<?>[] paramTypes = new Class<?>[] {String.class };
        Object[] params = new Object[] {fieldValue };
        Class<?> valClass;
        try {
            valClass = loader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            String msg = "Could not find validation class: " + className;
            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, MODULE);
            return;
        }
        Method valMethod;
        try {
            valMethod = valClass.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException cnfe) {
            String msg = "Could not find validation method: " + methodName + " of class " + className;
            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, MODULE);
            return;
        }
        Boolean resultBool = Boolean.FALSE;
        try {
            resultBool = (Boolean) valMethod.invoke(null, params);
        } catch (Exception e) {
            String msg = "Error in validation method " + methodName + " of class " + className + ": " + e.getMessage();

            messages.add(msg);
            Debug.logError("[ValidateMethod.exec] " + msg, MODULE);
            return;
        }
        if (!resultBool) {
            addMessage(messages, loader, locale);
        }
    }
}
