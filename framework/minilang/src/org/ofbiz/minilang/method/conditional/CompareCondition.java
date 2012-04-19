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

import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.operation.BaseCompare;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public class CompareCondition implements Conditional {

    public static final String module = CompareCondition.class.getName();

    ContextAccessor<Object> fieldAcsr;
    String format;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    String operator;
    SimpleMethod simpleMethod;
    String type;
    String value;

    public CompareCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }
        this.value = element.getAttribute("value");
        this.operator = element.getAttribute("operator");
        this.type = element.getAttribute("type");
        this.format = element.getAttribute("format");
    }

    public boolean checkCondition(MethodContext methodContext) {
        String value = methodContext.expandString(this.value);
        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);
        Object fieldVal = getFieldVal(methodContext);
        List<Object> messages = FastList.newInstance();
        Boolean resultBool = BaseCompare.doRealCompare(fieldVal, value, operator, type, format, messages, null, methodContext.getLoader(), true);
        if (messages.size() > 0) {
            messages.add(0, "Error with comparison in if-compare between field [" + mapAcsr.toString() + "." + fieldAcsr.toString() + "] with value [" + fieldVal + "] and value [" + value + "] with operator [" + operator + "] and type [" + type + "]: ");
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                StringBuilder fullString = new StringBuilder();

                for (Object message : messages) {
                    fullString.append(message);
                }
                Debug.logWarning(fullString.toString(), module);
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), fullString.toString());
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        if (resultBool != null)
            return resultBool.booleanValue();
        return false;
    }

    protected Object getFieldVal(MethodContext methodContext) {
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn())
                    Debug.logInfo("Map not found with name " + mapAcsr + ", using empty string for comparison", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }
        // always use an empty string by default
        if (fieldVal == null) {
            fieldVal = "";
        }
        return fieldVal;
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        String value = methodContext.expandString(this.value);
        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);
        Object fieldVal = getFieldVal(methodContext);
        messageBuffer.append("[");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        messageBuffer.append("=");
        messageBuffer.append(fieldVal);
        messageBuffer.append("] ");
        messageBuffer.append(operator);
        messageBuffer.append(" ");
        messageBuffer.append(value);
        messageBuffer.append(" as ");
        messageBuffer.append(type);
        if (UtilValidate.isNotEmpty(format)) {
            messageBuffer.append(":");
            messageBuffer.append(format);
        }
    }

    public static final class CompareConditionFactory extends ConditionalFactory<CompareCondition> {
        @Override
        public CompareCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CompareCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-compare";
        }
    }
}
