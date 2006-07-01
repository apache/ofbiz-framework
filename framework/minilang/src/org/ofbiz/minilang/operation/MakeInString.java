/*
 * $Id: MakeInString.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * The container of MakeInString operations to make a new input String
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class MakeInString {
    
    public static final String module = MakeInString.class.getName();
    
    String fieldName;
    List operations = new LinkedList();

    public MakeInString(Element makeInStringElement) {
        fieldName = makeInStringElement.getAttribute("field");

        List operationElements = UtilXml.childElementList(makeInStringElement);

        if (operationElements != null && operationElements.size() > 0) {
            Iterator operElemIter = operationElements.iterator();

            while (operElemIter.hasNext()) {
                Element curOperElem = (Element) operElemIter.next();
                String nodeName = curOperElem.getNodeName();

                if ("in-field".equals(nodeName)) {
                    operations.add(new InFieldOper(curOperElem));
                } else if ("property".equals(nodeName)) {
                    operations.add(new PropertyOper(curOperElem));
                } else if ("constant".equals(nodeName)) {
                    operations.add(new ConstantOper(curOperElem));
                } else {
                    Debug.logWarning("[SimpleMapProcessor.MakeInString.MakeInString] Operation element \"" + nodeName + "\" not recognized", module);
                }
            }
        }
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        Iterator iter = operations.iterator();
        StringBuffer buffer = new StringBuffer();

        while (iter.hasNext()) {
            MakeInStringOperation oper = (MakeInStringOperation) iter.next();
            String curStr = oper.exec(inMap, messages, locale, loader);

            if (curStr != null)
                buffer.append(curStr);
        }
        inMap.put(fieldName, buffer.toString());
    }
}
