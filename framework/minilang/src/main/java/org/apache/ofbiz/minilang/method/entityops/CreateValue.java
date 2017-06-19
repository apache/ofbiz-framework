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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;create-value&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class CreateValue extends MethodOperation {

    public static final String module = CreateValue.class.getName();

    private final boolean createOrStore;
    @Deprecated
    private final boolean doCacheClear;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public CreateValue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "do-cache-clear", "or-store");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "do-cache-clear", "or-store");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        doCacheClear = !"false".equals(element.getAttribute("do-cache-clear"));
        createOrStore = "true".equals(element.getAttribute("or-store"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value == null) {
            String errMsg = "In <create-value> the value \"" + valueFma + "\" was not found, not creating";
            Debug.logWarning(errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        try {
            if (createOrStore) {
                value.getDelegator().createOrStore(value);
            } else {
                value.getDelegator().create(value);
            }
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while creating the \"" + valueFma +"\" GenericValue: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<create-value ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        if (!this.doCacheClear) {
            sb.append("do-cache-clear=\"false\"");
        }
        if (this.createOrStore) {
            sb.append("or-store=\"true\"");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;create-value&gt; element.
     */
    public static final class CreateValueFactory implements Factory<CreateValue> {
        @Override
        public CreateValue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CreateValue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "create-value";
        }
    }
}
