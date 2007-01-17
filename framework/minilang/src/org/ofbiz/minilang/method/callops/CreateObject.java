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

import java.lang.reflect.*;
import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Creates a Java object using the given fields as parameters
 */
public class CreateObject extends MethodOperation {
    
    public static final String module = CreateObject.class.getName();

    String className;
    ContextAccessor fieldAcsr;
    ContextAccessor mapAcsr;

    /** A list of MethodObject objects to use as the method call parameters */
    List parameters;

    public CreateObject(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        className = element.getAttribute("class-name");
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        
        List parameterElements = UtilXml.childElementList(element);
        if (parameterElements.size() > 0) {
            parameters = new ArrayList(parameterElements.size());
            
            Iterator parameterIter = parameterElements.iterator();
            while (parameterIter.hasNext()) {
                Element parameterElement = (Element) parameterIter.next();
                MethodObject methodObject = null;
                if ("string".equals(parameterElement.getNodeName())) {
                    methodObject = new StringObject(parameterElement, simpleMethod); 
                } else if ("field".equals(parameterElement.getNodeName())) {
                    methodObject = new FieldObject(parameterElement, simpleMethod);
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

    public boolean exec(MethodContext methodContext) {
        String className = methodContext.expandString(this.className);

        Class methodClass = null;
        try {
            methodClass = ObjectType.loadClass(className, methodContext.getLoader());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Class to create not found with name " + className + " in create-object operation", module);

            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Class to create not found with name " + className + ": " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        
        Object[] args = null;
        Class[] parameterTypes = null;
        if (parameters != null) {
            args = new Object[parameters.size()];
            parameterTypes = new Class[parameters.size()];

            Iterator parameterIter = parameters.iterator();
            int i = 0;
            while (parameterIter.hasNext()) {
                MethodObject methodObjectDef = (MethodObject) parameterIter.next();
                args[i] = methodObjectDef.getObject(methodContext);

                Class typeClass = methodObjectDef.getTypeClass(methodContext.getLoader());
                if (typeClass == null) {
                    String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Parameter type not found with name " + methodObjectDef.getTypeName() + "]";
                    Debug.logError(errMsg, module);
                    methodContext.setErrorReturn(errMsg, simpleMethod);
                    return false;
                }

                parameterTypes[i] = typeClass;
                i++;
            }
        }
        
        try {
            Constructor constructor = methodClass.getConstructor(parameterTypes);
            try {
                Object newObject = constructor.newInstance(args);
                
                //if fieldAcsr is empty, ignore return value
                if (!fieldAcsr.isEmpty()) {
                    if (!mapAcsr.isEmpty()) {
                        Map retMap = (Map) mapAcsr.get(methodContext);

                        if (retMap == null) {
                            retMap = new HashMap();
                            mapAcsr.put(methodContext, retMap);
                        }
                        fieldAcsr.put(retMap, newObject, methodContext);
                    } else {
                        // no map name, use the env
                        fieldAcsr.put(methodContext, newObject);
                    }
                }
            } catch (InstantiationException e) {
                Debug.logError(e, "Could not instantiate object in create-object operation", module);

                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Could not instantiate object: " + e.toString() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            } catch (IllegalAccessException e) {
                Debug.logError(e, "Illegal access constructing object in create-object operation", module);

                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Illegal access constructing object: " + e.toString() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            } catch (IllegalArgumentException e) {
                Debug.logError(e, "Illegal argument calling method in create-object operation", module);

                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Illegal argument calling constructor: " + e.toString() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            } catch (InvocationTargetException e) {
                Debug.logError(e.getTargetException(), "Constructor in create-object operation threw an exception", module);

                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Constructor in create-object threw an exception: " + e.getTargetException() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            }
        } catch (NoSuchMethodException e) {
            Debug.logError(e, "Could not find constructor to execute in simple-method create-object operation", module);
            
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Could not find constructor to execute: " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        } catch (SecurityException e) {
            Debug.logError(e, "Security exception finding constructor to execute in simple-method create-object operation", module);
            
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Security exception finding constructor to execute: " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<create-object/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
