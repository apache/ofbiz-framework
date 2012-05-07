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
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements compare to a field condition.
 */
public final class CompareFieldCondition extends MiniLangElement implements Conditional {

    public static final String module = CompareFieldCondition.class.getName();

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        // Correct missing to-field attribute
        String toFieldAttr = element.getAttribute("to-field");
        if (toFieldAttr.isEmpty()) {
            element.setAttribute("to-field", element.getAttribute("field"));
            return true;
        }
        return false;
    }

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander formatFse;
    private final String operator;
    private final FlexibleMapAccessor<Object> toFieldFma;
    private final String type;

    public CompareFieldCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "format", "operator", "type", "to-field");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "operator", "to-field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "operator", "type");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "format");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "to-field");
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.formatFse = FlexibleStringExpander.getInstance(element.getAttribute("format"));
        this.operator = element.getAttribute("operator");
        this.toFieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("to-field"));
        this.type = MiniLangValidate.checkAttribute(element.getAttribute("type"), "PlainString");
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        Object toFieldVal = toFieldFma.get(methodContext.getEnvMap());
        String format = formatFse.expandString(methodContext.getEnvMap());
        List<Object> errorMessages = FastList.newInstance();
        Boolean resultBool = ObjectType.doRealCompare(fieldVal, toFieldVal, operator, type, format, errorMessages, methodContext.getLocale(), methodContext.getLoader(), true);
        if (errorMessages.size() > 0 || resultBool == null) {
            for (Object obj : errorMessages) {
                simpleMethod.addErrorMessage(methodContext, (String) obj);
            }
            return false;
        }
        return resultBool.booleanValue();
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        String format = formatFse.expandString(methodContext.getEnvMap());
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        Object toFieldVal = toFieldFma.get(methodContext.getEnvMap());
        messageBuffer.append("[");
        messageBuffer.append(fieldFma);
        messageBuffer.append("=");
        messageBuffer.append(fieldVal);
        messageBuffer.append("] ");
        messageBuffer.append(operator);
        messageBuffer.append(" [");
        messageBuffer.append(toFieldFma);
        messageBuffer.append("=");
        messageBuffer.append(toFieldVal);
        messageBuffer.append("] ");
        messageBuffer.append(" as ");
        messageBuffer.append(type);
        if (UtilValidate.isNotEmpty(format)) {
            messageBuffer.append(":");
            messageBuffer.append(format);
        }
    }

    public static final class CompareFieldConditionFactory extends ConditionalFactory<CompareFieldCondition> {
        @Override
        public CompareFieldCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CompareFieldCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-compare-field";
        }
    }
}
