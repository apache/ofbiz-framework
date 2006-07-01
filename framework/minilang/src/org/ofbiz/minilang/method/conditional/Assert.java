/*
 * $Id: Assert.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.method.conditional;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.3
 */
public class Assert extends MethodOperation {

    public static final String module = Assert.class.getName();

    protected ContextAccessor errorListAcsr;
    protected FlexibleStringExpander titleExdr;

    /** List of Conditional objects */
    protected List conditionalList = new LinkedList(); 

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
