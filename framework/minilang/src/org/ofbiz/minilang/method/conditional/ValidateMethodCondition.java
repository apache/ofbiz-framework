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

import java.lang.reflect.Method;

import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.minilang.MiniLangElement;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements validate method condition.
 */
public final class ValidateMethodCondition extends MiniLangElement implements Conditional {

    public static final String module = ValidateMethodCondition.class.getName();
    private static final Class<?>[] paramTypes = new Class<?>[] { String.class };

    private final String className;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final String methodName;

    public ValidateMethodCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "method", "class");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "method");
            MiniLangValidate.constantAttributes(simpleMethod, element, "method", "class");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.methodName = element.getAttribute("method");
        this.className = MiniLangValidate.checkAttribute(element.getAttribute("class"), "org.ofbiz.base.util.UtilValidate");
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal == null) {
            fieldVal = "";
        } else if (!(fieldVal instanceof String)) {
            try {
                fieldVal = MiniLangUtil.convertType(fieldVal, String.class, methodContext.getLocale(), methodContext.getTimeZone(), null);
            } catch (Exception e) {
                throw new MiniLangRuntimeException(e, this);
            }
        }
        Object[] params = new Object[] { fieldVal };
        try {
            Class<?> valClass = methodContext.getLoader().loadClass(className);
            Method valMethod = valClass.getMethod(methodName, paramTypes);
            Boolean resultBool = (Boolean) valMethod.invoke(null, params);
            return resultBool.booleanValue();
        } catch (Exception e) {
            throw new MiniLangRuntimeException(e, this);
        }
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        // allow methodContext to be null
        String methodName = methodContext == null ? this.methodName : methodContext.expandString(this.methodName);
        String className = methodContext == null ? this.className : methodContext.expandString(this.className);
        messageBuffer.append("validate-method[");
        messageBuffer.append(className);
        messageBuffer.append(".");
        messageBuffer.append(methodName);
        messageBuffer.append("(");
        messageBuffer.append(this.fieldFma);
        if (methodContext != null) {
            messageBuffer.append("=");
            messageBuffer.append(fieldFma.get(methodContext.getEnvMap()));
        }
        messageBuffer.append(")]");
    }

    public static final class ValidateMethodConditionFactory extends ConditionalFactory<ValidateMethodCondition> {
        @Override
        public ValidateMethodCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ValidateMethodCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-validate-method";
        }
    }
}
