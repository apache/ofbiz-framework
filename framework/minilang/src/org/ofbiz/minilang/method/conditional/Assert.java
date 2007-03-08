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
package org.ofbiz.minilang.method.conditional;

import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Operation used to check each sub-condition independently and for each one that fails (does not evaluate to true), adds an error to the error message list.
 */
public class Assert extends MethodOperation {

    public static final String module = Assert.class.getName();

    protected ContextAccessor errorListAcsr;
    protected FlexibleStringExpander titleExdr;

    /** List of Conditional objects */
    protected List conditionalList = FastList.newInstance(); 

    public Assert(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);

        errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");
        titleExdr = new FlexibleStringExpander(element.getAttribute("title"));
        
        List conditionalElementList = UtilXml.childElementList(element);
        Iterator conditionalElementIter = conditionalElementList.iterator();
        while (conditionalElementIter.hasNext()) {
            Element conditionalElement = (Element) conditionalElementIter.next();
            this.conditionalList.add(ConditionalFactory.makeConditional(conditionalElement, simpleMethod));
        }
    }

    public boolean exec(MethodContext methodContext) {
        List messages = (List) errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }

        String title = this.titleExdr.expandString(methodContext.getEnvMap());
        
        //  check each conditional and if fails generate a message to add to the error list
        Iterator conditionalIter = conditionalList.iterator();
        while (conditionalIter.hasNext()) {
            Conditional condition = (Conditional) conditionalIter.next();
            boolean conditionTrue = condition.checkCondition(methodContext);
            
            if (!conditionTrue) {
                // pretty print condition
                StringBuffer messageBuffer = new StringBuffer();
                messageBuffer.append("Assertion ");
                if (UtilValidate.isNotEmpty(title)) {
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

    public String rawString() {
        return expandedString(null);
    }
    
    public String expandedString(MethodContext methodContext) {
        String title = this.titleExdr.expandString(methodContext.getEnvMap());

        StringBuffer messageBuf = new StringBuffer();
        messageBuf.append("<assert");
        if (UtilValidate.isNotEmpty(title)) {
            messageBuf.append(" title=\"");
            messageBuf.append(title);
            messageBuf.append("\"");
        }
        messageBuf.append(">");
        Iterator conditionalIter = conditionalList.iterator();
        while (conditionalIter.hasNext()) {
            Conditional condition = (Conditional) conditionalIter.next();
            condition.prettyPrint(messageBuf, methodContext);
        }
        messageBuf.append("</assert>");
        return messageBuf.toString();
    }
}
