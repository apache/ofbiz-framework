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
package org.apache.ofbiz.minilang.method.envops;

import java.text.MessageFormat;
import java.util.List;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;string-append&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class StringAppend extends MethodOperation {

    private final FlexibleMapAccessor<List<? extends Object>> argListFma;
    private final FlexibleMapAccessor<String> fieldFma;
    private final FlexibleStringExpander prefixFse;
    private final FlexibleStringExpander stringFse;
    private final FlexibleStringExpander suffixFse;

    public StringAppend(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "arg-list", "prefix", "string", "suffix");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "string");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "arg-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        argListFma = FlexibleMapAccessor.getInstance(element.getAttribute("arg-list"));
        fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        prefixFse = FlexibleStringExpander.getInstance(element.getAttribute("prefix"));
        stringFse = FlexibleStringExpander.getInstance(element.getAttribute("string"));
        suffixFse = FlexibleStringExpander.getInstance(element.getAttribute("suffix"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String value = stringFse.expandString(methodContext.getEnvMap());
        List<? extends Object> argList = argListFma.get(methodContext.getEnvMap());
        if (argList != null) {
            try {
                value = MessageFormat.format(value, argList.toArray());
            } catch (IllegalArgumentException e) {
                throw new MiniLangRuntimeException("Exception thrown while formatting the string attribute: " + e.getMessage(), this);
            }
        }
        if (!value.isEmpty()) {
            String prefixValue = prefixFse.expandString(methodContext.getEnvMap());
            String suffixValue = suffixFse.expandString(methodContext.getEnvMap());
            StringBuilder newValue = new StringBuilder();
            String oldValue = fieldFma.get(methodContext.getEnvMap());
            if (oldValue != null) {
                newValue.append(oldValue);
            }
            newValue.append(prefixValue).append(value).append(suffixValue);
            fieldFma.put(methodContext.getEnvMap(), newValue.toString());
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<string-append ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        sb.append("string=\"").append(this.stringFse).append("\" ");
        if (!this.argListFma.isEmpty()) {
            sb.append("arg-list=\"").append(this.argListFma).append("\" ");
        }
        if (!this.prefixFse.isEmpty()) {
            sb.append("prefix=\"").append(this.prefixFse).append("\" ");
        }
        if (!this.suffixFse.isEmpty()) {
            sb.append("suffix=\"").append(this.suffixFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;string-append&gt; element.
     */
    public static final class StringAppendFactory implements Factory<StringAppend> {
        @Override
        public StringAppend createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new StringAppend(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "string-append";
        }
    }
}
