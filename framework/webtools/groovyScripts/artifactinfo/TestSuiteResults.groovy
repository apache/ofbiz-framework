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
import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.testtools.TestRunContainer
import org.w3c.dom.Document
import org.w3c.dom.Element

List testList = []

for (ComponentConfig.TestSuiteInfo testSuiteInfo : ComponentConfig.getAllTestSuiteInfos(parameters.compName)) {
    String suiteName = getTestSuiteName(testSuiteInfo)
    // if a suiteName has been requested, limit result to it.
    if (parameters.suiteName && suiteName != parameters.suiteName) continue

    boolean firstLine = true
    for (Element testCaseElement : getTestCaseResultsForSuite(suiteName)) {
        List<Element> children = UtilXml.childElementList(testCaseElement)
        Element child = children ? children[0] : null
        String details = ""
        if (child) {
             details = UtilXml.getAttributeValueIgnorePrefix(child, 'message') ?: child.getNodeValue()
        }
        testList << [testName        : testCaseElement.getAttribute('name'),
                     success         : child == null,
                     details         : details,
                     suiteName       : suiteName,
                     displaySuiteName: firstLine,
                     time            : testCaseElement.getAttribute('time')]
        firstLine = false
    }
}
context.results = testList

private List<? extends Element> getTestCaseResultsForSuite(String suiteName) {
    File xmlFile = new File(TestRunContainer.LOG_DIR + suiteName + ".xml")
    if (xmlFile.exists()) {
        Document results = UtilXml.readXmlDocument(xmlFile.getText())
        Element resultElement = results.getDocumentElement()
        return UtilXml.childElementList(resultElement, ["testcase"] as Set)
    }
    return new ArrayList<Element>()
}

private String getTestSuiteName(ComponentConfig.TestSuiteInfo testSuiteInfo) {
    Element documentElement = testSuiteInfo.createResourceHandler().getDocument().getDocumentElement()
    return documentElement.getAttribute("suite-name")
}
