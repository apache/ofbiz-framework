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
package org.apache.ofbiz.minilang.method.entityops;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;clone-value&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cclonevalue%3E}}">Mini-language Reference</a>
 */
public final class CloneValue extends MethodOperation {

    private final FlexibleMapAccessor<GenericValue> newValueFma;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public CloneValue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "new-value-field");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "new-value-field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "new-value-field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        newValueFma = FlexibleMapAccessor.getInstance(element.getAttribute("new-value-field"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value != null) {
            newValueFma.put(methodContext.getEnvMap(), GenericValue.create(value));
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<clone-value ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("new-value-field=\"").append(this.newValueFma).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;clone-value&gt; element.
     */
    public static final class CloneValueFactory implements Factory<CloneValue> {
        @Override
        public CloneValue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CloneValue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "clone-value";
        }
    }
}
