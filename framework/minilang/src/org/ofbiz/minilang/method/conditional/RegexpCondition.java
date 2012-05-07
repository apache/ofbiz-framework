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

import org.apache.oro.text.regex.MalformedPatternException;
import org.ofbiz.base.util.CompilerMatcher;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
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
public class RegexpCondition extends MiniLangElement implements Conditional {

    public static final String module = RegexpCondition.class.getName();
    private transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander exprFse;

    public RegexpCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "expr");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "expr");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "expr");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.exprFse = FlexibleStringExpander.getInstance(element.getAttribute("expr"));
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal == null) {
            fieldVal = "";
        } else if (!(fieldVal instanceof String)) {
            try {
                fieldVal = ObjectType.simpleTypeConvert(fieldVal, "String", null, methodContext.getTimeZone(), methodContext.getLocale(), true);
            } catch (GeneralException e) {
                throw new MiniLangRuntimeException(e, this);
            }
        }
        String regExp = exprFse.expandString(methodContext.getEnvMap());
        try {
            return compilerMatcher.get().matches((String) fieldVal, regExp);
        } catch (MalformedPatternException e) {
            throw new MiniLangRuntimeException(e, this);
        }
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("regexp[");
        messageBuffer.append("[");
        messageBuffer.append(this.fieldFma);
        messageBuffer.append("=");
        messageBuffer.append(fieldFma.get(methodContext.getEnvMap()));
        messageBuffer.append("] matches ");
        messageBuffer.append(exprFse.expandString(methodContext.getEnvMap()));
        messageBuffer.append("]");
    }

    public static final class RegexpConditionFactory extends ConditionalFactory<RegexpCondition> {
        @Override
        public RegexpCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RegexpCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-regexp";
        }
    }
}
