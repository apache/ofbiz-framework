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
package org.apache.ofbiz.minilang.method.eventops;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;session-to-field&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public class SessionToField extends MethodOperation {

    private final FlexibleStringExpander defaultFse;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander attributeNameFse;

    public SessionToField(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "session-name", "default");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        String attributeName = element.getAttribute("session-name");
        if (!attributeName.isEmpty()) {
            this.attributeNameFse = FlexibleStringExpander.getInstance(attributeName);
        } else {
            this.attributeNameFse = FlexibleStringExpander.getInstance(this.fieldFma.toString());
        }
        this.defaultFse = FlexibleStringExpander.getInstance(element.getAttribute("default"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            String attributeName = attributeNameFse.expandString(methodContext.getEnvMap());
            Object value = methodContext.getRequest().getSession().getAttribute(attributeName);
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                value = defaultFse.expandString(methodContext.getEnvMap());
            }
            fieldFma.put(methodContext.getEnvMap(), value);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<session-to-field ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        if (!this.attributeNameFse.isEmpty()) {
            sb.append("session-name=\"").append(this.attributeNameFse).append("\" ");
        }
        if (!this.defaultFse.isEmpty()) {
            sb.append("default=\"").append(this.defaultFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;session-to-field&gt; element.
     */
    public static final class SessionToFieldFactory implements Factory<SessionToField> {
        @Override
        public SessionToField createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new SessionToField(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "session-to-field";
        }
    }
}
