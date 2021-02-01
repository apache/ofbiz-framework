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
package org.apache.ofbiz.minilang.method.otherops;

import java.text.MessageFormat;
import java.util.List;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;property-to-field&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class PropertyToField extends MethodOperation {

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        // Correct deprecated arg-list-name attribute
        String listAttr = element.getAttribute("arg-list-name");
        if (!listAttr.isEmpty()) {
            element.setAttribute("arg-list", listAttr);
            element.removeAttribute("arg-list-name");
            return true;
        }
        return false;
    }

    private final FlexibleMapAccessor<List<? extends Object>> argListFma;
    private final FlexibleStringExpander defaultFse;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final boolean noLocale;
    private final FlexibleStringExpander propertyFse;
    private final FlexibleStringExpander resourceFse;

    public PropertyToField(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.deprecatedAttribute(simpleMethod, element, "arg-list-name", "replace with \"arg-list\"");
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "resource", "property", "arg-list", "default", "no-locale");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "resource", "property");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "arg-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        resourceFse = FlexibleStringExpander.getInstance(element.getAttribute("resource"));
        propertyFse = FlexibleStringExpander.getInstance(element.getAttribute("property"));
        argListFma = FlexibleMapAccessor.getInstance(element.getAttribute("arg-list"));
        defaultFse = FlexibleStringExpander.getInstance(element.getAttribute("default"));
        noLocale = "true".equals(element.getAttribute("no-locale"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String resource = resourceFse.expandString(methodContext.getEnvMap());
        String property = propertyFse.expandString(methodContext.getEnvMap());
        String value = null;
        if (noLocale) {
            value = EntityUtilProperties.getPropertyValue(resource, property, methodContext.getDelegator());
        } else {
            value = EntityUtilProperties.getMessage(resource, property, methodContext.getLocale(), methodContext.getDelegator());
        }
        value = FlexibleStringExpander.expandString(value, methodContext.getEnvMap());
        if (value.isEmpty()) {
            value = defaultFse.expandString(methodContext.getEnvMap());
        }
        List<? extends Object> argList = argListFma.get(methodContext.getEnvMap());
        if (argList != null) {
            try {
                value = MessageFormat.format(value, argList.toArray());
            } catch (IllegalArgumentException e) {
                throw new MiniLangRuntimeException("Exception thrown while formatting the property value: " + e.getMessage(), this);
            }
        }
        fieldFma.put(methodContext.getEnvMap(), value);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<property-to-field ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        sb.append("resource=\"").append(this.resourceFse).append("\" ");
        sb.append("property=\"").append(this.propertyFse).append("\" ");
        if (!this.argListFma.isEmpty()) {
            sb.append("arg-list=\"").append(this.argListFma).append("\" ");
        }
        if (!this.defaultFse.isEmpty()) {
            sb.append("default=\"").append(this.defaultFse).append("\" ");
        }
        if (noLocale) {
            sb.append("no-locale=\"true\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;property-to-field&gt; element.
     */
    public static final class PropertyToFieldFactory implements Factory<PropertyToField> {
        @Override
        public PropertyToField createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new PropertyToField(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "property-to-field";
        }
    }
}
