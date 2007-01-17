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
import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Implements compare to a constant condition.
 */
public class EmptyCondition implements Conditional {
    
    public static final String module = EmptyCondition.class.getName();
    
    SimpleMethod simpleMethod;
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    
    public EmptyCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
    }

    public boolean checkCondition(MethodContext methodContext) {
        // only run subOps if element is empty/null
        boolean runSubOps = false;
        Object fieldVal = getFieldVal(methodContext);

        if (fieldVal == null) {
            runSubOps = true;
        } else {
            if (fieldVal instanceof String) {
                String fieldStr = (String) fieldVal;

                if (fieldStr.length() == 0) {
                    runSubOps = true;
                }
            } else if (fieldVal instanceof Collection) {
                Collection fieldCol = (Collection) fieldVal;

                if (fieldCol.size() == 0) {
                    runSubOps = true;
                }
            } else if (fieldVal instanceof Map) {
                Map fieldMap = (Map) fieldVal;

                if (fieldMap.size() == 0) {
                    runSubOps = true;
                }
            }
        }
        
        return runSubOps;
    }
    
    protected Object getFieldVal(MethodContext methodContext) {
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", running operations", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }
        return fieldVal;
    }

    public void prettyPrint(StringBuffer messageBuffer, MethodContext methodContext) {
        messageBuffer.append("empty[");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        if (methodContext != null) {
            messageBuffer.append("=");
            messageBuffer.append(getFieldVal(methodContext));
        }
        messageBuffer.append("]");
    }
}
