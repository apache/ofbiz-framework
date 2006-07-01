/*
 * $Id: CombinedCondition.java 5462 2005-08-05 18:35:48Z jonesde $
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
 * Implements generic combining conditions such as or, and, etc.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class CombinedCondition implements Conditional {
    
    public static final int OR = 1;
    public static final int XOR = 2;
    public static final int AND = 3;
    public static final int NOT = 4;

    SimpleMethod simpleMethod;    
    int conditionType;
    List subConditions = new LinkedList();
    
    public CombinedCondition(Element element, int conditionType, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        this.conditionType = conditionType;
        List subElements = UtilXml.childElementList(element);
        Iterator subElIter = subElements.iterator();
        while (subElIter.hasNext()) {
            Element subElement = (Element) subElIter.next();
            subConditions.add(ConditionalFactory.makeConditional(subElement, simpleMethod));
        }
    }

    public boolean checkCondition(MethodContext methodContext) {
        if (subConditions.size() == 0) return true;
        
        Iterator subCondIter = subConditions.iterator();
        switch (this.conditionType) {
            case OR:
                while (subCondIter.hasNext()) {
                    Conditional subCond = (Conditional) subCondIter.next();
                    if (subCond.checkCondition(methodContext)) {
                        return true;
                    }
                }
                return false;
            case XOR:
                boolean trueFound = false;
                while (subCondIter.hasNext()) {
                    Conditional subCond = (Conditional) subCondIter.next();
                    if (subCond.checkCondition(methodContext)) {
                        if (trueFound) {
                            return false;
                        } else {
                            trueFound = true;
                        }
                    }
                }
                return trueFound;
            case AND:
                while (subCondIter.hasNext()) {
                    Conditional subCond = (Conditional) subCondIter.next();
                    if (!subCond.checkCondition(methodContext)) {
                        return false;
                    }
                }
                return true;
            case NOT:
                Conditional subCond = (Conditional) subCondIter.next();
                return !subCond.checkCondition(methodContext);
            default:
                return false;
        }
    }

    public void prettyPrint(StringBuffer messageBuffer, MethodContext methodContext) {
        messageBuffer.append("(");
        Iterator subCondIter = subConditions.iterator();
        while (subCondIter.hasNext()) {
            Conditional subCond = (Conditional) subCondIter.next();
            subCond.prettyPrint(messageBuffer, methodContext);
            if (subCondIter.hasNext()) {
                switch (this.conditionType) {
                case OR:
                    messageBuffer.append(" OR ");
                    break;
                case XOR:
                    messageBuffer.append(" XOR ");
                    break;
                case AND:
                    messageBuffer.append(" AND ");
                    break;
                case NOT:
                    messageBuffer.append(" NOT ");
                    break;
                default:
                    messageBuffer.append("?");
                }
            }
        }
        messageBuffer.append(")");
    }
}
