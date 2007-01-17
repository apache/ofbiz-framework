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
package org.ofbiz.minilang.method.conditional;

import java.util.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.method.*;

/**
 * Implements validate method condition.
 */
public class ValidateMethodCondition implements Conditional {
    
    public static final String module = ValidateMethodCondition.class.getName();
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    String methodName;
    String className;
    
    public ValidateMethodCondition(Element element) {
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        this.methodName = element.getAttribute("method");
        this.className = element.getAttribute("class");
    }

    public boolean checkCondition(MethodContext methodContext) {
        String methodName = methodContext.expandString(this.methodName);
        String className = methodContext.expandString(this.className);

        String fieldString = getFieldString(methodContext);

        Class[] paramTypes = new Class[] {String.class};
        Object[] params = new Object[] {fieldString};

        Class valClass;
        try {
            valClass = methodContext.getLoader().loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            Debug.logError("Could not find validation class: " + className, module);
            return false;
        }

        Method valMethod;
        try {
            valMethod = valClass.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException cnfe) {
            Debug.logError("Could not find validation method: " + methodName + " of class " + className, module);
            return false;
        }

        Boolean resultBool = Boolean.FALSE;
        try {
            resultBool = (Boolean) valMethod.invoke(null, params);
        } catch (Exception e) {
            Debug.logError(e, "Error in IfValidationMethod " + methodName + " of class " + className + ", not processing sub-ops ", module);
        }
        
        if (resultBool != null) return resultBool.booleanValue();
        
        return false;
    }
    
    protected String getFieldString(MethodContext methodContext) {
        String fieldString = null;
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", using empty string for comparison", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal != null) {
            try {
                fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null, null);
            } catch (GeneralException e) {
                Debug.logError(e, "Could not convert object to String, using empty String", module);
            }
        }

        // always use an empty string by default
        if (fieldString == null) fieldString = "";

        return fieldString;
    }

    public void prettyPrint(StringBuffer messageBuffer, MethodContext methodContext) {
        // allow methodContext to be null
        String methodName = methodContext == null ? this.methodName : methodContext.expandString(this.methodName);
        String className = methodContext == null ? this.className : methodContext.expandString(this.className);

        messageBuffer.append("validate-method[");
        messageBuffer.append(className);
        messageBuffer.append(".");
        messageBuffer.append(methodName);
        messageBuffer.append("(");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        if (methodContext != null) {
            messageBuffer.append("=");
            messageBuffer.append(getFieldString(methodContext));
        }
        messageBuffer.append(")]");
    }
}
