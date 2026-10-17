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
 * The container of MakeInString operations to make a new input String
 */
public class MakeInString {

    private static final String MODULE = MakeInString.class.getName();

    private String fieldName;
    private List<MakeInStringOperation> operations = new ArrayList<>();

    public MakeInString(Element makeInStringElement) {
        fieldName = makeInStringElement.getAttribute("field");
        List<? extends Element> operationElements = UtilXml.childElementList(makeInStringElement);
        if (UtilValidate.isNotEmpty(operationElements)) {
            for (Element curOperElem : operationElements) {
                String nodeName = curOperElem.getNodeName();
                if ("in-field".equals(nodeName)) {
                    operations.add(new InFieldOper(curOperElem));
                } else if ("property".equals(nodeName)) {
                    operations.add(new PropertyOper(curOperElem));
                } else if ("constant".equals(nodeName)) {
                    operations.add(new ConstantOper(curOperElem));
                } else {
                    Debug.logWarning("[SimpleMapProcessor.MakeInString.MakeInString] Operation element \"" + nodeName + "\" not recognized", MODULE);
                }
            }
        }
    }

    /**
     * Exec.
     * @param inMap the in map
     * @param results the results
     * @param messages the messages
     * @param locale the locale
     * @param loader the loader
     */
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        StringBuilder buffer = new StringBuilder();
        for (MakeInStringOperation oper : operations) {
            String curStr = oper.exec(inMap, messages, locale, loader);
            if (curStr != null) {
                buffer.append(curStr);
            }
        }
        inMap.put(fieldName, buffer.toString());
    }
}
