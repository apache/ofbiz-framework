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
package org.apache.ofbiz.minilang.method;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangElement;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.w3c.dom.Element;

/**
 * Implements the &lt;fail-message&gt; and &lt;fail-property&gt; elements.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class MessageElement extends MiniLangElement {

    public static MessageElement fromParentElement(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        Element childElement = UtilXml.firstChildElement(element, "fail-message");
        if (childElement != null) {
            return new MessageElement(childElement, simpleMethod);
        } else {
            childElement = UtilXml.firstChildElement(element, "fail-property");
            if (childElement != null) {
                return new MessageElement(childElement, simpleMethod);
            } else {
                return null;
            }
        }
    }

    private final FlexibleStringExpander messageFse;
    private final FlexibleStringExpander propertyFse;
    private final String propertyResource;

    public MessageElement(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if ("fail-message".equals(element.getTagName())) {
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, element, "message");
                MiniLangValidate.requiredAttributes(simpleMethod, element, "message");
                MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "message");
            }
            this.messageFse = FlexibleStringExpander.getInstance(element.getAttribute("message"));
            this.propertyFse = null;
            this.propertyResource = null;
        } else {
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, element, "property", "resource");
                MiniLangValidate.requiredAttributes(simpleMethod, element, "property", "resource");
                MiniLangValidate.constantAttributes(simpleMethod, element, "property", "resource");
            }
            this.messageFse = null;
            this.propertyFse = FlexibleStringExpander.getInstance(element.getAttribute("property"));
            this.propertyResource = element.getAttribute("resource");
        }
    }

    public String getMessage(MethodContext methodContext) {
        if (messageFse != null) {
            return messageFse.expandString(methodContext.getEnvMap());
        } else {
            return UtilProperties.getMessage(propertyResource, propertyFse.expandString(methodContext.getEnvMap()), methodContext.getEnvMap(),
                    methodContext.getLocale());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.messageFse != null) {
            sb.append("<fail-message message=\"").append(this.messageFse).append("\" />");
        }
        if (this.propertyFse != null) {
            sb.append("<fail-property property=\"").append(this.propertyFse).append(" resource=\"").append(this.propertyResource).append("\" />");
        }
        return sb.toString();
    }
}
