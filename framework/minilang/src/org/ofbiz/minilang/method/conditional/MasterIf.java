/*
 * $Id: MasterIf.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Represents the top-level element and only mounted operation for the more flexible if structure.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class MasterIf extends MethodOperation {

    Conditional condition;

    List thenSubOps = new LinkedList();
    List elseSubOps = null;

    List elseIfs = null;

    public MasterIf(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        
        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);
        
        Element thenElement = UtilXml.firstChildElement(element, "then");
        SimpleMethod.readOperations(thenElement, thenSubOps, simpleMethod);
        
        List elseIfElements = UtilXml.childElementList(element, "else-if");
        if (elseIfElements != null && elseIfElements.size() > 0) {
            elseIfs = new LinkedList();
            Iterator eieIter = elseIfElements.iterator();
            while (eieIter.hasNext()) {
                Element elseIfElement = (Element) eieIter.next();
                elseIfs.add(new ElseIf(elseIfElement, simpleMethod));
            }
        }
        
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = new LinkedList();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    public boolean exec(MethodContext methodContext) {
        // if conditions fails, always return true; if a sub-op returns false 
        // return false and stop, otherwise return true
        // return true;

        // only run subOps if element is empty/null
        boolean runSubOps = condition.checkCondition(methodContext);

        if (runSubOps) {
            return SimpleMethod.runSubOps(thenSubOps, methodContext);
        } else {
            
            // try the else-ifs
            if (elseIfs != null && elseIfs.size() > 0) {
                Iterator elseIfIter = elseIfs.iterator();
                while (elseIfIter.hasNext()) {
                    ElseIf elseIf = (ElseIf) elseIfIter.next();
                    if (elseIf.checkCondition(methodContext)) {
                        return elseIf.runSubOps(methodContext);
                    }
                }
            }
            
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    public String rawString() {
        return expandedString(null);
    }

    public String expandedString(MethodContext methodContext) {
        // TODO: fill in missing details, if needed
        StringBuffer messageBuf = new StringBuffer();
        this.condition.prettyPrint(messageBuf, methodContext);
        return "<if><condition>" + messageBuf + "</condition></if>";
    }
}
