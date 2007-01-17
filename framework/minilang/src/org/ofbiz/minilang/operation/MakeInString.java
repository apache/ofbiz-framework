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
package org.ofbiz.minilang.operation;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * The container of MakeInString operations to make a new input String
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
