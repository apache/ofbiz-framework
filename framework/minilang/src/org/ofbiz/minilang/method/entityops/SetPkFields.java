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
package org.ofbiz.minilang.method.entityops;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Looks for each PK field in the named map and if it exists there it will copy it into the named value object.
 */
public class SetPkFields extends MethodOperation {
    
    public static final String module = SetPkFields.class.getName();
    
    ContextAccessor valueAcsr;
    ContextAccessor mapAcsr;
    String setIfNullStr;

    public SetPkFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        setIfNullStr = element.getAttribute("set-if-null");
    }

    public boolean exec(MethodContext methodContext) {
        // if anything but false it will be true
        boolean setIfNull = !"false".equals(methodContext.expandString(setIfNullStr));
        
        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        if (value == null) {
            String errMsg = "In set-pk-fields a value was not found with the specified valueAcsr: " + valueAcsr + ", not setting fields";

            Debug.logWarning(errMsg, module);
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }

        Map theMap = (Map) mapAcsr.get(methodContext);
        if (theMap == null) {
            Debug.logWarning("In set-pk-fields could not find map with name " + mapAcsr + ", not setting any fields", module);
        } else {
            value.setPKFields(theMap, setIfNull);
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-pk-fields/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
