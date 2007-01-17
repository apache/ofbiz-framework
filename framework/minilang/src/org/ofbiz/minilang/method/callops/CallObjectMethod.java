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
 * Calls a Java object method using the given fields as parameters
 */
public class CallObjectMethod extends MethodOperation {
    
    public static final String module = CallClassMethod.class.getName();

    ContextAccessor objFieldAcsr;
    ContextAccessor objMapAcsr;
    String methodName;
    ContextAccessor retFieldAcsr;
    ContextAccessor retMapAcsr;

    /** A list of MethodObject objects to use as the method call parameters */
    List parameters;

    public CallObjectMethod(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        objFieldAcsr = new ContextAccessor(element.getAttribute("obj-field-name"));
        objMapAcsr = new ContextAccessor(element.getAttribute("obj-map-name"));
        methodName = element.getAttribute("method-name");
        retFieldAcsr = new ContextAccessor(element.getAttribute("ret-field-name"));
        retMapAcsr = new ContextAccessor(element.getAttribute("ret-map-name"));
        
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
        String methodName = methodContext.expandString(this.methodName);

        Object methodObject = null;
        if (!objMapAcsr.isEmpty()) {
            Map fromMap = (Map) objMapAcsr.get(methodContext);
            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + objMapAcsr + ", which should contain the object to execute a method on; not executing method, rerturning error.", module);
                
                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Map not found with name " + objMapAcsr + ", which should contain the object to execute a method on]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            }
            methodObject = objFieldAcsr.get(fromMap, methodContext);
        } else {
            // no map name, try the env
            methodObject = objFieldAcsr.get(methodContext);
        }

        if (methodObject == null) {
            if (Debug.infoOn()) Debug.logInfo("Object not found to execute method on with name " + objFieldAcsr + " in Map with name " + objMapAcsr + ", not executing method, rerturning error.", module);
            
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Object not found to execute method on with name " + objFieldAcsr + " in Map with name " + objMapAcsr + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }

        Class methodClass = methodObject.getClass();
        return CallObjectMethod.callMethod(simpleMethod, methodContext, parameters, methodClass, methodObject, methodName, retFieldAcsr, retMapAcsr);
    }
    
    public static boolean callMethod(SimpleMethod simpleMethod, MethodContext methodContext, List parameters, Class methodClass, Object methodObject, String methodName, ContextAccessor retFieldAcsr, ContextAccessor retMapAcsr) {
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
            Method method = methodClass.getMethod(methodName, parameterTypes);
            try {
                Object retValue = method.invoke(methodObject, args);
                
                //if retFieldAcsr is empty, ignore return value
                if (!retFieldAcsr.isEmpty()) {
                    if (!retMapAcsr.isEmpty()) {
                        Map retMap = (Map) retMapAcsr.get(methodContext);

                        if (retMap == null) {
                            retMap = new HashMap();
                            retMapAcsr.put(methodContext, retMap);
                        }
                        retFieldAcsr.put(retMap, retValue, methodContext);
                    } else {
                        // no map name, use the env
                        retFieldAcsr.put(methodContext, retValue);
                    }
                }
            } catch (IllegalAccessException e) {
                Debug.logError(e, "Could not access method in call method operation", module);
                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Could not access method to execute named " + methodName + ": " + e.toString() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            } catch (IllegalArgumentException e) {
                Debug.logError(e, "Illegal argument calling method in call method operation", module);
                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Illegal argument calling method to execute named " + methodName + ": " + e.toString() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            } catch (InvocationTargetException e) {
                Debug.logError(e.getTargetException(), "Method in call method operation threw an exception", module);
                String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Method to execute named " + methodName + " threw an exception: " + e.getTargetException() + "]";
                methodContext.setErrorReturn(errMsg, simpleMethod);
                return false;
            }
        } catch (NoSuchMethodException e) {
            Debug.logError(e, "Could not find method to execute in simple-method call method operation", module);            
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Could not find method to execute named " + methodName + ": " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        } catch (SecurityException e) {
            Debug.logError(e, "Security exception finding method to execute in simple-method call method operation", module);            
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [Security exception finding method to execute named " + methodName + ": " + e.toString() + "]";
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<call-object-method/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
