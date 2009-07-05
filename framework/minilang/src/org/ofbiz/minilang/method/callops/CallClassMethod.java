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

package org.ofbiz.minilang.method.callops;

import java.util.*;

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Calls a Java class method using the given fields as parameters
 */
public class CallClassMethod extends MethodOperation {
    public static final class CallClassMethodFactory implements Factory<CallClassMethod> {
        public CallClassMethod createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new CallClassMethod(element, simpleMethod);
        }

        public String getName() {
            return "call-class-method";
        }
    }

    public static final String module = CallClassMethod.class.getName();

    String className;
    String methodName;
    ContextAccessor<Object> retFieldAcsr;
    ContextAccessor<Map<String, Object>> retMapAcsr;

    /** A list of MethodObject objects to use as the method call parameters */
    List<MethodObject<?>> parameters;

    public CallClassMethod(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        className = element.getAttribute("class-name");
        methodName = element.getAttribute("method-name");
        // the schema for this element now just has the "ret-field" attribute, though the old "ret-field-name" and "ret-map-name" pair is still supported
        retFieldAcsr = new ContextAccessor<Object>(element.getAttribute("ret-field"), element.getAttribute("ret-field-name"));
        retMapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("ret-map-name"));

        List<? extends Element> parameterElements = UtilXml.childElementList(element);
        if (parameterElements.size() > 0) {
            parameters = FastList.newInstance();

            for (Element parameterElement: parameterElements) {
                MethodObject<?> methodObject = null;
                if ("string".equals(parameterElement.getNodeName())) {
                    methodObject = new StringObject(parameterElement, simpleMethod);
                } else if ("field".equals(parameterElement.getNodeName())) {
                    methodObject = new FieldObject<Object>(parameterElement, simpleMethod);
                } else {
                    //whoops, invalid tag here, print warning
                    Debug.logWarning("Found an unsupported tag under the call-object-method tag: " + parameterElement.getNodeName() + "; ignoring", module);
                }
                if (methodObject != null) {
                    parameters.add(methodObject);
                }
            }
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String className = methodContext.expandString(this.className);
        String methodName = methodContext.expandString(this.methodName);

        Class<?> methodClass = null;
        try {
            methodClass = ObjectType.loadClass(className, methodContext.getLoader());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Class to create not found with name " + className + " in create-object operation", module);

            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Class to create not found with name " + className + ": " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }

        return CallObjectMethod.callMethod(simpleMethod, methodContext, parameters, methodClass, null, methodName, retFieldAcsr, retMapAcsr);
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-class-method/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
