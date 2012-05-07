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

import javolution.util.FastList;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangElement;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public final class CompareCondition extends MiniLangElement implements Conditional {

    public static final String module = CompareCondition.class.getName();

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander formatFse;
    private final String operator;
    private final String type;
    private final FlexibleStringExpander valueFse;

    public CompareCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "format", "operator", "type", "value");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "operator", "value");
            MiniLangValidate.constantAttributes(simpleMethod, element, "operator", "type");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "value", "format");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.formatFse = FlexibleStringExpander.getInstance(element.getAttribute("format"));
        this.operator = element.getAttribute("operator");
        this.type = MiniLangValidate.checkAttribute(element.getAttribute("type"), "PlainString");
        this.valueFse = FlexibleStringExpander.getInstance(element.getAttribute("value"));
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangRuntimeException {
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal == null) {
            fieldVal = "";
        }
        String value = valueFse.expandString(methodContext.getEnvMap());
        String format = formatFse.expandString(methodContext.getEnvMap());
        List<Object> errorMessages = FastList.newInstance();
        Boolean resultBool = ObjectType.doRealCompare(fieldVal, value, operator, type, format, errorMessages, methodContext.getLocale(), methodContext.getLoader(), true);
        if (errorMessages.size() > 0 || resultBool == null) {
            for (Object obj : errorMessages) {
                simpleMethod.addErrorMessage(methodContext, (String) obj);
            }
            return false;
        }
        return resultBool.booleanValue();
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        String value = valueFse.expandString(methodContext.getEnvMap());
        String format = formatFse.expandString(methodContext.getEnvMap());
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        messageBuffer.append("[");
        messageBuffer.append(this.fieldFma);
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
        public CompareCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CompareCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-compare";
        }
    }
}
