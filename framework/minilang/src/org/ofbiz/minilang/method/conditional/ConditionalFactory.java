/*
 * $Id: ConditionalFactory.java 5462 2005-08-05 18:35:48Z jonesde $
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

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;

/**
 * Creates Conditional objects according to the element that is passed.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class ConditionalFactory {
    
    public static final String module = ConditionalFactory.class.getName();
    
    public static Conditional makeConditional(Element element, SimpleMethod simpleMethod) {
        String tagName = element.getTagName();
        
        if ("or".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.OR, simpleMethod);
        } else if ("xor".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.XOR, simpleMethod);
        } else if ("and".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.AND, simpleMethod);
        } else if ("not".equals(tagName)) {
            return new CombinedCondition(element, CombinedCondition.NOT, simpleMethod);
        } else if ("if-validate-method".equals(tagName)) {
            return new ValidateMethodCondition(element);
        } else if ("if-compare".equals(tagName)) {
            return new CompareCondition(element, simpleMethod);
        } else if ("if-compare-field".equals(tagName)) {
            return new CompareFieldCondition(element, simpleMethod);
        } else if ("if-empty".equals(tagName)) {
            return new EmptyCondition(element, simpleMethod);
        } else if ("if-regexp".equals(tagName)) {
            return new RegexpCondition(element, simpleMethod);
        } else if ("if-has-permission".equals(tagName)) {
            return new HasPermissionCondition(element, simpleMethod);
        } else {
            Debug.logWarning("Found an unknown if condition: " + tagName, module);
            return null;
        }
    }
}
