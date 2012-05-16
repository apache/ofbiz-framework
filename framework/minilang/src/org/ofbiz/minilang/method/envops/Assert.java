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
package org.ofbiz.minilang.method.envops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.method.conditional.Conditional;
import org.ofbiz.minilang.method.conditional.ConditionalFactory;
import org.w3c.dom.Element;

/**
 * Adds an error to the error list for each condition that evaluates to false.
 */
public final class Assert extends MethodOperation {

    public static final String module = Assert.class.getName();

    private final List<Conditional> conditionalList;
    private final FlexibleMapAccessor<List<Object>> errorListFma;
    private final FlexibleStringExpander titleExdr;

    public Assert(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "title", "error-list-name");
            MiniLangValidate.constantAttributes(simpleMethod, element, "title", "error-list-name");
        }
        errorListFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"), "error_list"));
        titleExdr = FlexibleStringExpander.getInstance(element.getAttribute("title"));
        List<? extends Element> childElements = UtilXml.childElementList(element);
        if (MiniLangValidate.validationOn() && childElements.isEmpty()) {
            MiniLangValidate.handleError("No conditional elements.", simpleMethod, element);
        }
        List<Conditional> conditionalList = new ArrayList<Conditional>(childElements.size());
        for (Element conditionalElement : UtilXml.childElementList(element)) {
            conditionalList.add(ConditionalFactory.makeConditional(conditionalElement, simpleMethod));
        }
        this.conditionalList = Collections.unmodifiableList(conditionalList);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> messages = errorListFma.get(methodContext.getEnvMap());
        if (messages == null) {
            messages = FastList.newInstance();
            errorListFma.put(methodContext.getEnvMap(), messages);
        }
        String title = titleExdr.expandString(methodContext.getEnvMap());
        for (Conditional condition : conditionalList) {
            if (!condition.checkCondition(methodContext)) {
                StringBuilder messageBuffer = new StringBuilder("Assertion ");
                if (!title.isEmpty()) {
                    messageBuffer.append("[");
                    messageBuffer.append(title);
                    messageBuffer.append("] ");
                }
                messageBuffer.append("failed: ");
                condition.prettyPrint(messageBuffer, methodContext);
                messages.add(messageBuffer.toString());
            }
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    @Override
    public String toString() {
        StringBuilder messageBuf = new StringBuilder("<assert");
        if (!titleExdr.isEmpty()) {
            messageBuf.append(" title=\"");
            messageBuf.append(titleExdr);
            messageBuf.append("\"");
        }
        messageBuf.append(">");
        for (Conditional condition : conditionalList) {
            messageBuf.append(condition);
        }
        messageBuf.append("</assert>");
        return messageBuf.toString();
    }

    @Override
    public String rawString() {
        return expandedString(null);
    }

    public static final class AssertFactory implements Factory<Assert> {
        public Assert createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Assert(element, simpleMethod);
        }

        public String getName() {
            return "assert";
        }
    }
}
