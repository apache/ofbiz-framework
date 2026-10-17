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

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Map Processor Main Class
 */
public class MapProcessor {

    private List<MakeInString> makeInStrings = new ArrayList<>();
    private String name;
    private List<SimpleMapProcess> simpleMapProcesses = new ArrayList<>();

    public MapProcessor(Element simpleMapProcessorElement) {
        name = simpleMapProcessorElement.getAttribute("name");
        for (Element makeInStringElement : UtilXml.childElementList(simpleMapProcessorElement, "make-in-string")) {
            MakeInString makeInString = new MakeInString(makeInStringElement);
            makeInStrings.add(makeInString);
        }
        for (Element simpleMapProcessElement : UtilXml.childElementList(simpleMapProcessorElement, "process")) {
            SimpleMapProcess strProc = new SimpleMapProcess(simpleMapProcessElement);
            simpleMapProcesses.add(strProc);
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
        if (UtilValidate.isNotEmpty(makeInStrings)) {
            for (MakeInString makeInString : makeInStrings) {
                makeInString.exec(inMap, results, messages, locale, loader);
            }
        }
        if (UtilValidate.isNotEmpty(simpleMapProcesses)) {
            for (SimpleMapProcess simpleMapProcess : simpleMapProcesses) {
                simpleMapProcess.exec(inMap, results, messages, locale, loader);
            }
        }
    }

    /**
     * Gets name.
     * @return the name
     */
    public String getName() {
        return name;
    }
}
