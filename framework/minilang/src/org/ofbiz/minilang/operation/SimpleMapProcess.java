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
 * A complete string process for a given field; contains multiple string operations
 */
public class SimpleMapProcess {
    
    public static final String module = SimpleMapProcess.class.getName();
    
    List simpleMapOperations = new LinkedList();
    String field = "";

    public SimpleMapProcess(Element simpleMapProcessElement) {
        this.field = simpleMapProcessElement.getAttribute("field");
        readOperations(simpleMapProcessElement);
    }

    public String getFieldName() {
        return field;
    }

    public void exec(Map inMap, Map results, List messages, Locale locale, ClassLoader loader) {
        Iterator strOpsIter = simpleMapOperations.iterator();

        while (strOpsIter.hasNext()) {
            SimpleMapOperation simpleMapOperation = (SimpleMapOperation) strOpsIter.next();

            simpleMapOperation.exec(inMap, results, messages, locale, loader);
        }
    }

    void readOperations(Element simpleMapProcessElement) {
        List operationElements = UtilXml.childElementList(simpleMapProcessElement);

        if (operationElements != null && operationElements.size() > 0) {
            Iterator operElemIter = operationElements.iterator();

            while (operElemIter.hasNext()) {
                Element curOperElem = (Element) operElemIter.next();
                String nodeName = curOperElem.getNodeName();

                if ("validate-method".equals(nodeName)) {
                    simpleMapOperations.add(new ValidateMethod(curOperElem, this));
                } else if ("compare".equals(nodeName)) {
                    simpleMapOperations.add(new Compare(curOperElem, this));
                } else if ("compare-field".equals(nodeName)) {
                    simpleMapOperations.add(new CompareField(curOperElem, this));
                } else if ("regexp".equals(nodeName)) {
                    simpleMapOperations.add(new Regexp(curOperElem, this));
                } else if ("not-empty".equals(nodeName)) {
                    simpleMapOperations.add(new NotEmpty(curOperElem, this));
                } else if ("copy".equals(nodeName)) {
                    simpleMapOperations.add(new Copy(curOperElem, this));
                } else if ("convert".equals(nodeName)) {
                    simpleMapOperations.add(new Convert(curOperElem, this));
                } else {
                    Debug.logWarning("[SimpleMapProcessor.SimpleMapProcess.readOperations] Operation element \"" + nodeName + "\" not recognized", module);
                }
            }
        }
    }
}
