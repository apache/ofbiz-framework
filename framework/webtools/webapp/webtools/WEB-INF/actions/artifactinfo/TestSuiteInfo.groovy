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
 */

import org.ofbiz.base.util.*;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.testtools.TestListContainer.FoundTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.List;

List testList = [];
for (ComponentConfig.TestSuiteInfo testSuiteInfo: ComponentConfig.getAllTestSuiteInfos(parameters.compName)) {
    String componentName = testSuiteInfo.componentConfig.getComponentName();
    ResourceHandler testSuiteResource = testSuiteInfo.createResourceHandler();

    try {
        Document testSuiteDocument = testSuiteResource.getDocument();
        Element documentElement = testSuiteDocument.getDocumentElement();
        suiteName =  documentElement.getAttribute("suite-name")
        firstLine = true;
        for (Element testCaseElement : UtilXml.childElementList(documentElement, UtilMisc.toSet("test-case", "test-group"))) {
            testMap = [:];
            String caseName = testCaseElement.getAttribute("case-name");
            if (firstLine == true) {
                testMap = UtilMisc.toMap("suiteName", suiteName, "suiteNameSave", suiteName, "caseName", caseName);
                firstLine = false;
            } else {
                testMap = UtilMisc.toMap("suiteNameSave", suiteName, "caseName", caseName);
            }
            testList.add(testMap);
        }
    } catch (GenericConfigException e) {
        String errMsg = "Error reading XML document from ResourceHandler for loader [" + testSuiteResource.getLoaderName() + "] and location [" + testSuiteResource.getLocation() + "]";
        Debug.logError(e, errMsg, module);
        throw new IllegalArgumentException(errMsg);
    }



}

context.suits = testList;
