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

import org.apache.ofbiz.base.component.ComponentConfig
import org.apache.ofbiz.base.config.GenericConfigException
import org.apache.ofbiz.base.config.ResourceHandler
import org.apache.ofbiz.base.util.UtilXml
import org.w3c.dom.Element

List testList = []
for (ComponentConfig.TestSuiteInfo testSuiteInfo: ComponentConfig.getAllTestSuiteInfos(parameters.compName)) {
    ResourceHandler testSuiteResource = testSuiteInfo.createResourceHandler()

    try {
        Element documentElement = testSuiteResource.getDocument().getDocumentElement()
        String suiteName = testSuiteResource.getDocument().getDocumentElement().getAttribute("suite-name")
        boolean firstLine = true
        for (Element testCaseElement : UtilXml.childElementList(documentElement, ["test-case", "test-group"] as Set)) {
            testList << [suiteName     : suiteName,
                         caseName      : testCaseElement.getAttribute("case-name"),
                         firstSuiteLine: firstLine ? 'Y' : 'N']
            firstLine = false
        }
    } catch (GenericConfigException e) {
        String errMsg = "Error reading XML document from ResourceHandler for loader [${testSuiteResource.getLoaderName()}] and location [${testSuiteResource.getLocation()}]"
        logError(e, errMsg)
        throw new IllegalArgumentException(errMsg)
    }
}

context.suits = testList
