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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.minilang.MiniLangElement;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements compare to a constant condition.
 */
public final class EmptyCondition extends MiniLangElement implements Conditional {

    public static final String module = EmptyCondition.class.getName();

    private final FlexibleMapAccessor<Object> fieldFma;

    public EmptyCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        return UtilValidate.isEmpty(fieldFma.get(methodContext.getEnvMap()));
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("empty[");
        messageBuffer.append(fieldFma);
        messageBuffer.append("=");
        messageBuffer.append(fieldFma.get(methodContext.getEnvMap()));
        messageBuffer.append("]");
    }

    public static final class EmptyConditionFactory extends ConditionalFactory<EmptyCondition> {
        @Override
        public EmptyCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new EmptyCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-empty";
        }
    }
}
