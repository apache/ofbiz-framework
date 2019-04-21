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

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MessageElement;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;add-error&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class AddError extends MethodOperation {

    private final FlexibleMapAccessor<List<String>> errorListFma;
    private final MessageElement messageElement;

    public AddError(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "error-list-name");
            MiniLangValidate.constantAttributes(simpleMethod, element, "error-list-name");
            MiniLangValidate.childElements(simpleMethod, element, "fail-message", "fail-property");
            MiniLangValidate.requireAnyChildElement(simpleMethod, element, "fail-message", "fail-property");
        }
        errorListFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
        messageElement = MessageElement.fromParentElement(element, simpleMethod);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (messageElement != null) {
            String message = messageElement.getMessage(methodContext);
            if (message != null) {
                List<String> messages = errorListFma.get(methodContext.getEnvMap());
                if (messages == null) {
                    messages = new LinkedList<>();
                    errorListFma.put(methodContext.getEnvMap(), messages);
                }
                messages.add(message);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<add-error ");
        if (!"error_list".equals(this.errorListFma.getOriginalName())) {
            sb.append("error-list-name=\"").append(this.errorListFma).append("\"");
        }
        if (messageElement != null) {
            sb.append(">").append(messageElement).append("</add-error>");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }

    /**
     * A factory for the &lt;add-error&gt; element.
    */
    public static final class AddErrorFactory implements Factory<AddError> {
        @Override
        public AddError createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new AddError(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "add-error";
        }
    }
}
