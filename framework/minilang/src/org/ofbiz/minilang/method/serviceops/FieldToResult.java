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
package org.ofbiz.minilang.method.serviceops;

import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies a field to the simple-method result Map.
 */
public final class FieldToResult extends MethodOperation {

    public static final String module = FieldToResult.class.getName();

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleMapAccessor<Object> resultFma;

    public FieldToResult(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "result-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "result-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        String resultNameAttribute = element.getAttribute("result-name");
        if (resultNameAttribute.length() == 0) {
            this.resultFma = this.fieldFma;
        } else {
            this.resultFma = FlexibleMapAccessor.getInstance(resultNameAttribute);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = this.fieldFma.get(methodContext.getEnvMap());
        if (fieldVal != null) {
            if (this.resultFma.containsNestedExpression()) {
                String expression = (String) this.resultFma.get(methodContext.getEnvMap());
                FlexibleMapAccessor<Object> resultFma = FlexibleMapAccessor.getInstance(expression);
                resultFma.put(methodContext.getResults(), fieldVal);
            } else {
                this.resultFma.put(methodContext.getResults(), fieldVal);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<field-to-result ");
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        if (!this.resultFma.isEmpty()) {
            sb.append("result-name=\"").append(this.resultFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    public static final class FieldToResultFactory implements Factory<FieldToResult> {
        public FieldToResult createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FieldToResult(element, simpleMethod);
        }

        public String getName() {
            return "field-to-result";
        }
    }
}
