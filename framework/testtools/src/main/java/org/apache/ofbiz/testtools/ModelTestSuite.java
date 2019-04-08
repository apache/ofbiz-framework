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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import org.w3c.dom.Element;

/**
 * Use this class in a JUnit test runner to bootstrap the Test Suite runner.
 */
public class ModelTestSuite {

    public static final String module = ModelTestSuite.class.getName();

    protected String suiteName;
    protected String originalDelegatorName;
    protected String originalDispatcherName;

    protected Delegator delegator;
    protected LocalDispatcher dispatcher;

    protected List<Test> testList = new ArrayList<Test>();

    public ModelTestSuite(Element mainElement, String testCase) {
        this.suiteName = mainElement.getAttribute("suite-name");

        this.originalDelegatorName = mainElement.getAttribute("delegator-name");
        if (UtilValidate.isEmpty(this.originalDelegatorName)) this.originalDelegatorName = "test";

        this.originalDispatcherName = mainElement.getAttribute("dispatcher-name");
        if (UtilValidate.isEmpty(this.originalDispatcherName)) this.originalDispatcherName = "test-dispatcher";

        String uniqueSuffix = "-" + RandomStringUtils.randomAlphanumeric(10);

        this.delegator = DelegatorFactory.getDelegator(this.originalDelegatorName).makeTestDelegator(this.originalDelegatorName + uniqueSuffix);
        this.dispatcher = ServiceContainer.getLocalDispatcher(originalDispatcherName + uniqueSuffix, delegator);

        for (Element testCaseElement : UtilXml.childElementList(mainElement, UtilMisc.toSet("test-case", "test-group"))) {
            String caseName = testCaseElement.getAttribute("case-name");
            String nodeName = testCaseElement.getNodeName();
            if (testCase == null || caseName.equals(testCase)) {
                if (nodeName.equals("test-case")) {
                    parseTestElement(caseName, UtilXml.firstChildElement(testCaseElement));
                } else if (nodeName.equals("test-group")) {
                    int i = 0;
                    for (Element childElement: UtilXml.childElementList(testCaseElement)) {
                        parseTestElement(caseName + '-' + i, childElement);
                        i++;
                    }
                }
            }
        }
    }

    private void parseTestElement(String caseName, Element testElement) {
        String nodeName = testElement.getNodeName();
        if ("junit-test-suite".equals(nodeName)) {
            String className = testElement.getAttribute("class-name");

            try {
                @SuppressWarnings("unchecked")
                Class<? extends TestCase> clz = (Class<? extends TestCase>) ObjectType.loadClass(className);
                TestSuite suite = new TestSuite();
                suite.addTestSuite(clz);
                Enumeration<?> testEnum = suite.tests();
                int testsAdded = 0;
                int casesAdded = 0;
                while (testEnum.hasMoreElements()) {
                    Test tst = (Test) testEnum.nextElement();
                    this.testList.add(tst);
                    casesAdded += tst.countTestCases();
                    testsAdded++;
                }
                Debug.logInfo("Added " + testsAdded + " tests [" + casesAdded + " cases] from the class: " + className, module);
            } catch (Exception e) {
                String errMsg = "Unable to load test suite class : " + className;
                Debug.logError(e, errMsg, module);
            }
        } else if ("service-test".equals(nodeName)) {
            this.testList.add(new ServiceTest(caseName, testElement));
        } else if ("simple-method-test".equals(nodeName)) {
            if (UtilValidate.isNotEmpty(testElement.getAttribute("name"))) {
                this.testList.add(new SimpleMethodTest(caseName, testElement));
            } else {
                String methodLocation = testElement.getAttribute("location");
                List<SimpleMethod> simpleMethods;
                try {
                    simpleMethods = SimpleMethod.getSimpleMethodsList(methodLocation, null);
                    for (SimpleMethod simpleMethod : simpleMethods) {
                        String methodName = simpleMethod.getMethodName();
                        if (methodName.startsWith("test")) {
                            this.testList.add(new SimpleMethodTest(caseName + "." + methodName, methodLocation, methodName));
                        }
                    }
                } catch (MiniLangException e) {
                    Debug.logError(e, module);
                }
            }
        } else if ("webdriver-test".equals(nodeName)) {
            try {
                String className = "org.apache.ofbiz.testtools.WebDriverTest";
                Class<?> cl;
                cl = Class.forName(className);
                Constructor<?> con = cl.getConstructor(String.class, Element.class);
                this.testList.add((Test)con.newInstance(caseName, testElement));
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        } else if ("entity-xml".equals(nodeName)) {
            this.testList.add(new EntityXmlAssertTest(caseName, testElement));
        } else if ("entity-xml-assert".equals(nodeName)) {
            // this is the old, deprecated name for the element, changed because it now does assert or load
            this.testList.add(new EntityXmlAssertTest(caseName, testElement));
        }
    }

    String getSuiteName() {
        return this.suiteName;
    }

    Delegator getDelegator() {
        return this.delegator;
    }

    List<Test> getTestList() {
        return testList;
    }


    public TestSuite makeTestSuite() {
        TestSuite suite = new TestSuite();
        suite.setName(this.getSuiteName());
        for (Test tst: this.getTestList()) {
            prepareTest(tst);
            suite.addTest(tst);
        }

        return suite;
    }

    private void prepareTest(Test test)
    {
        if (test instanceof TestSuite) {
            Enumeration<Test> subTests = UtilGenerics.cast(((TestSuite) test).tests());
            while (subTests.hasMoreElements()) {
                prepareTest(subTests.nextElement());
            }
        } else if (test instanceof EntityTestCase) {
            ((EntityTestCase)test).setDelegator(delegator);
            if (test instanceof OFBizTestCase) {
                ((OFBizTestCase)test).setDispatcher(dispatcher);
            }
        }
    }
}
