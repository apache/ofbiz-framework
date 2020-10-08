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

import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;check-id&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class CheckId extends MethodOperation {

    private static final String MODULE = CheckId.class.getName();

    private final FlexibleMapAccessor<List<String>> errorListFma;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander messageFse;
    private final String propertykey;
    private final String propertyResource;

    public CheckId(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "error-list-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "error-list-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.childElements(simpleMethod, element, "fail-message", "fail-property");
        }
        this.errorListFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        Element childElement = UtilXml.firstChildElement(element, "fail-message");
        if (childElement != null) {
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, childElement, "message");
                MiniLangValidate.requiredAttributes(simpleMethod, childElement, "message");
                MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, childElement, "message");
            }
            this.messageFse = FlexibleStringExpander.getInstance(childElement.getAttribute("message"));
            this.propertykey = null;
            this.propertyResource = null;
        } else {
            childElement = UtilXml.firstChildElement(element, "fail-property");
            if (childElement != null) {
                if (MiniLangValidate.validationOn()) {
                    MiniLangValidate.attributeNames(simpleMethod, childElement, "property", "resource");
                    MiniLangValidate.requiredAttributes(simpleMethod, childElement, "property", "resource");
                    MiniLangValidate.constantAttributes(simpleMethod, childElement, "property", "resource");
                }
                this.messageFse = FlexibleStringExpander.getInstance(null);
                this.propertykey = childElement.getAttribute("property");
                this.propertyResource = childElement.getAttribute("resource");
            } else {
                this.messageFse = FlexibleStringExpander.getInstance(null);
                this.propertykey = null;
                this.propertyResource = null;
            }
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String errorMsg = null;
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal == null) {
            errorMsg = "field \"" + fieldFma + "\" is null";
        } else {
            errorMsg = UtilValidate.checkValidDatabaseId(fieldVal.toString());
        }
        if (errorMsg != null) {
            String message = null;
            if (!this.messageFse.isEmpty()) {
                message = this.messageFse.expandString(methodContext.getEnvMap());
            } else if (this.propertyResource != null) {
                message = UtilProperties.getMessage(this.propertyResource, this.propertykey, methodContext.getEnvMap(), methodContext.getLocale());
            }
            if (message != null) {
                List<String> messages = errorListFma.get(methodContext.getEnvMap());
                if (messages == null) {
                    messages = new LinkedList<>();
                }
                errorListFma.put(methodContext.getEnvMap(), messages);
                messages.add(message.concat(": ").concat(errorMsg));
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<check-id ");
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        if (!"error_list".equals(this.errorListFma.getOriginalName())) {
            sb.append("error-list-name=\"").append(this.errorListFma).append("\" ");
        }
        sb.append(">");
        if (!this.messageFse.isEmpty()) {
            sb.append("<fail-message message=\"").append(this.messageFse).append("\" />");
        }
        if (this.propertykey != null) {
            sb.append("<fail-property property=\"").append(this.propertykey).append(" resource=\"").append(this.propertyResource).append("\" />");
        }
        sb.append("</check-id>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;check-id&gt; element.
     */
    public static final class CheckIdFactory implements Factory<CheckId> {
        @Override
        public CheckId createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CheckId(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "check-id";
        }
    }
}
