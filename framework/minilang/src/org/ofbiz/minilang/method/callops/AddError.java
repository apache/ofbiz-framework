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
package org.ofbiz.minilang.method.callops;

import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Adds the fail-message or fail-property value to the error-list.
 */
public class AddError extends MethodOperation {

    ContextAccessor<List<Object>> errorListAcsr;
    boolean isProperty = false;
    String message = null;
    String propertyResource = null;

    public AddError(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        errorListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("error-list-name"), "error_list");
        Element failMessage = UtilXml.firstChildElement(element, "fail-message");
        Element failProperty = UtilXml.firstChildElement(element, "fail-property");
        if (failMessage != null) {
            this.message = failMessage.getAttribute("message");
            this.isProperty = false;
        } else if (failProperty != null) {
            this.propertyResource = failProperty.getAttribute("resource");
            this.message = failProperty.getAttribute("property");
            this.isProperty = true;
        }
    }

    public void addMessage(List<Object> messages, ClassLoader loader, MethodContext methodContext) {
        String message = methodContext.expandString(this.message);
        String propertyResource = methodContext.expandString(this.propertyResource);
        if (!isProperty && message != null) {
            messages.add(message);
        } else if (isProperty && propertyResource != null && message != null) {
            String propMsg = UtilProperties.getMessage(propertyResource, message, methodContext.getEnvMap(), methodContext.getLocale());
            if (UtilValidate.isEmpty(propMsg)) {
                messages.add("Simple Method error occurred, but no message was found, sorry.");
            } else {
                messages.add(methodContext.expandString(propMsg));
            }
        } else {
            messages.add("Simple Method error occurred, but no message was found, sorry.");
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        List<Object> messages = errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }
        this.addMessage(messages, methodContext.getLoader(), methodContext);
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<add-error/>";
    }

    public static final class AddErrorFactory implements Factory<AddError> {
        public AddError createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new AddError(element, simpleMethod);
        }

        public String getName() {
            return "add-error";
        }
    }
}
