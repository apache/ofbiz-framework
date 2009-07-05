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
import javolution.util.FastList;
import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;
import org.ofbiz.minilang.operation.*;

/**
 * Implements compare to a field condition.
 */
public class CompareFieldCondition implements Conditional {
    public static final class CompareFieldConditionFactory extends ConditionalFactory<CompareFieldCondition> {
        @Override
        public CompareFieldCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CompareFieldCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-compare-field";
        }
    }


    public static final String module = CompareFieldCondition.class.getName();

    SimpleMethod simpleMethod;

    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<Map<String, ? extends Object>> toMapAcsr;
    ContextAccessor<Object> toFieldAcsr;

    String operator;
    String type;
    String format;

    public CompareFieldCondition(Element element, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;

        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }

        // NOTE: this is still supported, but is deprecated
        this.toMapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("to-map-name"));
        // set fieldAcsr to their default value of fieldAcsr if empty
        this.toFieldAcsr = new ContextAccessor<Object>(element.getAttribute("to-field"), element.getAttribute("field"));
        if (this.toFieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.toFieldAcsr = new ContextAccessor<Object>(element.getAttribute("to-field-name"), element.getAttribute("field-name"));
        }

        // do NOT default the to-map-name to the map-name because that
        //would make it impossible to compare from a map field to an
        //environment field

        this.operator = element.getAttribute("operator");
        this.type = element.getAttribute("type");
        this.format = element.getAttribute("format");
    }

    public boolean checkCondition(MethodContext methodContext) {
        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);

        Object fieldVal1 = getFieldVal1(methodContext);
        Object fieldVal2 = getFieldVal2(methodContext);

        List<Object> messages = FastList.newInstance();
        Boolean resultBool = BaseCompare.doRealCompare(fieldVal1, fieldVal2, operator, type, format, messages, null, methodContext.getLoader(), false);

        if (messages.size() > 0) {
            messages.add(0, "Error with comparison in if-compare-field between fields [" + mapAcsr.toString() + "." + fieldAcsr.toString() + "] with value [" + fieldVal1 + "] and [" + toMapAcsr.toString() + "." + toFieldAcsr.toString() + "] with value [" + fieldVal2 + "] with operator [" + operator + "] and type [" + type + "]: ");
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                StringBuilder fullString = new StringBuilder();

                for (Object message: messages) {
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

        if (resultBool != null) return resultBool.booleanValue();

        return false;
    }

    protected Object getFieldVal1(MethodContext methodContext) {
        Object fieldVal1 = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", using null for comparison", module);
            } else {
                fieldVal1 = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal1 = fieldAcsr.get(methodContext);
        }
        return fieldVal1;
    }

    protected Object getFieldVal2(MethodContext methodContext) {
        Object fieldVal2 = null;
        if (!toMapAcsr.isEmpty()) {
            Map<String, ? extends Object> toMap = toMapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.infoOn()) Debug.logInfo("To Map not found with name " + toMapAcsr + ", using null for comparison", module);
            } else {
                fieldVal2 = toFieldAcsr.get(toMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal2 = toFieldAcsr.get(methodContext);
        }
        return fieldVal2;
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);

        Object fieldVal1 = getFieldVal1(methodContext);
        Object fieldVal2 = getFieldVal2(methodContext);

        messageBuffer.append("[");
        if (!this.mapAcsr.isEmpty()) {
            messageBuffer.append(this.mapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.fieldAcsr);
        messageBuffer.append("=");
        messageBuffer.append(fieldVal1);
        messageBuffer.append("] ");

        messageBuffer.append(operator);

        messageBuffer.append(" [");
        if (!this.toMapAcsr.isEmpty()) {
            messageBuffer.append(this.toMapAcsr);
            messageBuffer.append(".");
        }
        messageBuffer.append(this.toFieldAcsr);
        messageBuffer.append("=");
        messageBuffer.append(fieldVal2);
        messageBuffer.append("] ");

        messageBuffer.append(" as ");
        messageBuffer.append(type);
        if (UtilValidate.isNotEmpty(format)) {
            messageBuffer.append(":");
            messageBuffer.append(format);
        }
    }
}
