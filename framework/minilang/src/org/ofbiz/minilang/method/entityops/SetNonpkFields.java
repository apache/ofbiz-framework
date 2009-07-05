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
 * Looks for each non-PK field in the named map and if it exists there it will copy it into the named value object.
 */
public class SetNonpkFields extends MethodOperation {
    public static final class SetNonpkFieldsFactory implements Factory<SetNonpkFields> {
        public SetNonpkFields createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new SetNonpkFields(element, simpleMethod);
        }

        public String getName() {
            return "set-nonpk-fields";
        }
    }

    public static final String module = SetNonpkFields.class.getName();

    ContextAccessor<GenericValue> valueAcsr;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    String setIfNullStr;

    public SetNonpkFields(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor<GenericValue>(element.getAttribute("value-field"), element.getAttribute("value-name"));
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
        setIfNullStr = element.getAttribute("set-if-null");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        // if anything but false it will be true
        boolean setIfNull = !"false".equals(methodContext.expandString(setIfNullStr));

        GenericValue value = valueAcsr.get(methodContext);
        if (value == null) {
            String errMsg = "In set-nonpk-fields a value was not found with the specified valueAcsr: " + valueAcsr + ", not setting fields";
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

        Map<String, ? extends Object> theMap = mapAcsr.get(methodContext);
        if (theMap == null) {
            Debug.logWarning("In set-nonpk-fields could not find map with name " + mapAcsr + ", not setting any fields", module);
        } else {
            value.setNonPKFields(theMap, setIfNull);
        }
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<set-nonpk-fields/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
