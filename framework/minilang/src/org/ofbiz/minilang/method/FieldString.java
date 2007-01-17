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
package org.ofbiz.minilang.method;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * A type of MethodString that represents a String constant value
 */
public class FieldString extends MethodString {
    
    public static final String module = FieldString.class.getName();
    
    ContextAccessor fieldAcsr;
    ContextAccessor mapAcsr;

    public FieldString(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
    }

    public String getString(MethodContext methodContext) {
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map fromMap = (Map) mapAcsr.get(methodContext);
            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + ", not getting string value", module);
                return "";
            }
            fieldVal = fieldAcsr.get(fromMap, methodContext);
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        if (fieldVal == null) {
            if (Debug.infoOn()) Debug.logInfo("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr + ", not getting string value", module);
            return "";
        }
        
        return fieldVal.toString();
    }
}
