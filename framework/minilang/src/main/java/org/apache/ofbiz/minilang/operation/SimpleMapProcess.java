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
package org.apache.ofbiz.minilang.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * A complete string process for a given field; contains multiple string operations
 */
public class SimpleMapProcess {

    public static final String module = SimpleMapProcess.class.getName();

    String field = "";
    List<SimpleMapOperation> simpleMapOperations = new ArrayList<>();

    public SimpleMapProcess(Element simpleMapProcessElement) {
        this.field = simpleMapProcessElement.getAttribute("field");
        readOperations(simpleMapProcessElement);
    }

    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        for (SimpleMapOperation simpleMapOperation : simpleMapOperations) {
            simpleMapOperation.exec(inMap, results, messages, locale, loader);
        }
    }

    public String getFieldName() {
        return field;
    }

    void readOperations(Element simpleMapProcessElement) {
        List<? extends Element> operationElements = UtilXml.childElementList(simpleMapProcessElement);
        if (UtilValidate.isNotEmpty(operationElements)) {
            for (Element curOperElem : operationElements) {
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
