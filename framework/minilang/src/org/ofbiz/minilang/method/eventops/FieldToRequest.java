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
package org.ofbiz.minilang.method.eventops;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleServletAccessor;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies a map field to a Servlet request attribute
 */
public class FieldToRequest extends MethodOperation {
    
    public static final String module = FieldToRequest.class.getName();
    
    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    FlexibleServletAccessor requestAcsr;

    public FieldToRequest(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
        requestAcsr = new FlexibleServletAccessor(element.getAttribute("request-name"), element.getAttribute("field-name"));
    }

    public boolean exec(MethodContext methodContext) {
        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            Object fieldVal = null;
            if (!mapAcsr.isEmpty()) {
                Map fromMap = (Map) mapAcsr.get(methodContext);
                if (fromMap == null) {
                    Debug.logWarning("Map not found with name " + mapAcsr, module);
                    return true;
                }
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            } else {
                // no map name, try the env
                fieldVal = fieldAcsr.get(methodContext);
            }

            if (fieldVal == null) {
                Debug.logWarning("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr, module);
                return true;
            }

            requestAcsr.put(methodContext.getRequest(), fieldVal, methodContext.getEnvMap());
        }
        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<field-to-request field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
