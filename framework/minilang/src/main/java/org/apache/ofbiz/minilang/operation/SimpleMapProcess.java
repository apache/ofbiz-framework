/*
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
import java.util.Collections;
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

    private static final String MODULE = SimpleMapProcess.class.getName();

    private final String field;
    private final List<SimpleMapOperation> simpleMapOperations;

    public SimpleMapProcess(Element simpleMapProcessElement) {
        this.field = simpleMapProcessElement.getAttribute("field");
        this.simpleMapOperations = readOperations(simpleMapProcessElement, field);
    }

    /**
     * Exec.
     * @param inMap    the in map
     * @param results  the results
     * @param messages the messages
     * @param locale   the locale
     * @param loader   the loader
     */
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        for (SimpleMapOperation simpleMapOperation : simpleMapOperations) {
            simpleMapOperation.exec(inMap, results, messages, locale, loader);
        }
    }

    /**
     * Gets field name.
     * @return the field name
     */
    public String getFieldName() {
        return field;
    }

    /**
     * Builds the operations of a process from its child elements.
     * @param simpleMapProcessElement the simple map process element
     * @param fieldName the name of the field the operations work on
     * @return the operations, in document order
     */
    private static List<SimpleMapOperation> readOperations(Element simpleMapProcessElement, String fieldName) {
        List<SimpleMapOperation> simpleMapOperations = new ArrayList<>();
        List<? extends Element> operationElements = UtilXml.childElementList(simpleMapProcessElement);
        if (UtilValidate.isNotEmpty(operationElements)) {
            for (Element curOperElem : operationElements) {
                String nodeName = curOperElem.getNodeName();
                if ("validate-method".equals(nodeName)) {
                    simpleMapOperations.add(new ValidateMethod(curOperElem, fieldName));
                } else if ("compare".equals(nodeName)) {
                    simpleMapOperations.add(new Compare(curOperElem, fieldName));
                } else if ("compare-field".equals(nodeName)) {
                    simpleMapOperations.add(new CompareField(curOperElem, fieldName));
                } else if ("regexp".equals(nodeName)) {
                    simpleMapOperations.add(new Regexp(curOperElem, fieldName));
                } else if ("not-empty".equals(nodeName)) {
                    simpleMapOperations.add(new NotEmpty(curOperElem, fieldName));
                } else if ("copy".equals(nodeName)) {
                    simpleMapOperations.add(new Copy(curOperElem, fieldName));
                } else if ("convert".equals(nodeName)) {
                    simpleMapOperations.add(new Convert(curOperElem, fieldName));
                } else {
                    Debug.logWarning("[SimpleMapProcessor.SimpleMapProcess.readOperations] Operation element \"" + nodeName
                            + "\" not recognized", MODULE);
                }
            }
        }
        return Collections.unmodifiableList(simpleMapOperations);
    }
}
