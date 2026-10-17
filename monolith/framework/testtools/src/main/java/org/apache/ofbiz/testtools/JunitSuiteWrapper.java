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
package org.apache.ofbiz.testtools;

import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Use this class in a JUnit test runner to prepare the TestSuite.
 */
public class JunitSuiteWrapper {

    private static final String MODULE = JunitSuiteWrapper.class.getName();
    private List<ModelTestSuite> modelTestSuiteList = new LinkedList<>();
    // Every ModelTestSuite the constructor builds but then discards below (empty test list, so it
    // never makes it into modelTestSuiteList / getAllTestList()). The constructor still creates a
    // real test Delegator/LocalDispatcher for each of these - see getDiscardedModelTestSuites() -
    // so a caller that treats "no tests found" as a plain error must still deregister these, or
    // that dispatcher leaks with no other path ever reaching it.
    private List<ModelTestSuite> discardedModelTestSuiteList = new LinkedList<>();

    public JunitSuiteWrapper(String componentName, String suiteName, String testCase) {
        for (ComponentConfig.TestSuiteInfo testSuiteInfo: ComponentConfig.getAllTestSuiteInfos(componentName)) {
            ResourceHandler testSuiteResource = testSuiteInfo.createResourceHandler();

            try {
                Document testSuiteDocument = testSuiteResource.getDocument();
                // TODO create TestSuite object based on this that will contain its TestCase objects

                Element documentElement = testSuiteDocument.getDocumentElement();
                // Filter on suite-name before constructing ModelTestSuite, not after: the
                // constructor unconditionally creates a test Delegator + LocalDispatcher pair,
                // and for any <jupiter-test-suite> entries inside it also runs a full JUnit
                // Platform Launcher discovery - all of that is wasted work for a testdef file
                // that suitename= was going to discard anyway. The suite-name attribute lives on
                // this same documentElement (ModelTestSuite's constructor reads it from the
                // identical element), so it can be read directly here first.
                if (suiteName != null && !documentElement.getAttribute("suite-name").equals(suiteName)) {
                    continue;
                }
                ModelTestSuite modelTestSuite = new ModelTestSuite(documentElement, testCase);
                if (modelTestSuite.getTestList().size() > 0) {
                    this.modelTestSuiteList.add(modelTestSuite);
                } else {
                    this.discardedModelTestSuiteList.add(modelTestSuite);
                }
            } catch (GenericConfigException e) {
                String errMsg = "Error reading XML document from ResourceHandler for loader [" + testSuiteResource.getLoaderName()
                        + "] and location [" + testSuiteResource.getLocation() + "]";
                Debug.logError(e, errMsg, MODULE);
            }
        }
    }

    /**
     * Gets model test suites.
     * @return the model test suites
     */
    public List<ModelTestSuite> getModelTestSuites() {
        return this.modelTestSuiteList;
    }

    /**
     * Gets all test list.
     * @return the all test list
     */
    public List<SuiteEntry> getAllTestList() {
        List<SuiteEntry> allTestList = new LinkedList<>();

        for (ModelTestSuite modelTestSuite: this.modelTestSuiteList) {
            allTestList.addAll(modelTestSuite.getTestList());
        }

        return allTestList;
    }

    /**
     * Gets the ModelTestSuites the constructor built but discarded because they had no matching
     * test cases (e.g. a testCaseName/suiteName that matched a {@code <test-suite>} element with
     * zero resulting entries). These are not reachable via {@link #getModelTestSuites()} or
     * {@link #getAllTestList()}, but each one still holds a real dispatcher/delegator pair the
     * constructor created - callers that reject this wrapper outright (e.g. "no tests found")
     * still need this list to avoid leaking those.
     * @return the discarded model test suites
     */
    List<ModelTestSuite> getDiscardedModelTestSuites() {
        return this.discardedModelTestSuiteList;
    }
}
