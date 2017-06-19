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

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;to-string&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class ToString extends MethodOperation {

    private final FlexibleMapAccessor<Object> fieldFma;
    private final String format;
    private final Integer numericPadding;

    public ToString(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.handleError("<to-string> element is deprecated (use <set>)", simpleMethod, element);
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "format", "numeric-padding");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "format", "numeric-padding");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        format = element.getAttribute("format");
        Integer numericPadding = null;
        String npAttribute = element.getAttribute("numeric-padding");
        if (!npAttribute.isEmpty()) {
            try {
                numericPadding = Integer.valueOf(npAttribute);
            } catch (Exception e) {
                MiniLangValidate.handleError("Exception thrown while parsing numeric-padding attribute: " + e.getMessage(), simpleMethod, element);
            }
        }
        this.numericPadding = numericPadding;
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object value = fieldFma.get(methodContext.getEnvMap());
        if (value != null) {
            try {
                if (!format.isEmpty()) {
                    value = MiniLangUtil.convertType(value, String.class, methodContext.getLocale(), methodContext.getTimeZone(), format);
                } else {
                    value = value.toString();
                }
            } catch (Exception e) {
                throw new MiniLangRuntimeException("Exception thrown while converting field to a string: " + e.getMessage(), this);
            }
            if (this.numericPadding != null) {
                value = StringUtil.padNumberString(value.toString(), this.numericPadding.intValue());
            }
            fieldFma.put(methodContext.getEnvMap(), value);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<to-string ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        if (!this.format.isEmpty()) {
            sb.append("format=\"").append(this.format).append("\" ");
        }
        if (numericPadding != null) {
            sb.append("numeric-padding=\"").append(this.numericPadding).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;to-string&gt; element.
     */
    public static final class ToStringFactory implements Factory<ToString> {
        @Override
        public ToString createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ToString(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "to-string";
        }
    }
}
