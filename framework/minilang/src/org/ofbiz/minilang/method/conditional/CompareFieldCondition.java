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

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangElement;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements compare to a field condition.
 */
public final class CompareFieldCondition extends MiniLangElement implements Conditional {

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

    private final Compare compare;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander formatFse;
    private final String operator;
    private final FlexibleMapAccessor<Object> toFieldFma;
    private final Class<?> targetClass;
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
        this.compare = Compare.getInstance(this.operator);
        if (this.compare == null) {
            MiniLangValidate.handleError("Invalid operator " + this.operator, simpleMethod, element);
        }
        this.toFieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("to-field"));
        this.type = element.getAttribute("type");
        Class<?> targetClass = null;
        if (!this.type.isEmpty()) {
            try {
                targetClass = ObjectType.loadClass(this.type);
            } catch (ClassNotFoundException e) {
                MiniLangValidate.handleError("Invalid type " + this.type, simpleMethod, element);
            }
        }
        this.targetClass = targetClass;
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        if (this.compare == null) {
            throw new MiniLangRuntimeException("Invalid operator " + this.operator, this);
        }
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        Object toFieldVal = toFieldFma.get(methodContext.getEnvMap());
        Class<?> targetClass = this.targetClass;
        if (targetClass == null) {
            targetClass = MiniLangUtil.getObjectClassForConversion(fieldVal);
        }
        String format = formatFse.expandString(methodContext.getEnvMap());
        try {
            return this.compare.doCompare(fieldVal, toFieldVal, targetClass, methodContext.getLocale(), methodContext.getTimeZone(), format);
        } catch (Exception e) {
            simpleMethod.addErrorMessage(methodContext, e.getMessage());
        }
        return false;
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
