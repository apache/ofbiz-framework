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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GroovyUtil;
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Use this class in a JUnit test runner to bootstrap the Test Suite runner.
 */
public class ModelTestSuite {
    private static final String MODULE = ModelTestSuite.class.getName();
    public static final String DELEGATOR_NAME = "test";
    public static final String DISPATCHER_NAME = "test-dispatcher";

    private String suiteName;
    private Delegator delegator;
    private LocalDispatcher dispatcher;
    private List<Test> testList = new ArrayList<>();

    public ModelTestSuite(Element mainElement, String testCase) {
        String uniqueSuffix = "-" + RandomStringUtils.randomAlphanumeric(10);
        this.suiteName = mainElement.getAttribute("suite-name");
        this.delegator = DelegatorFactory.getDelegator(DELEGATOR_NAME).makeTestDelegator(DELEGATOR_NAME + uniqueSuffix);
        this.dispatcher = ServiceContainer.getLocalDispatcher(DISPATCHER_NAME + uniqueSuffix, delegator);

        for (Element testCaseElement : UtilXml.childElementList(mainElement, UtilMisc.toSet("test-case", "test-group"))) {
            String caseName = testCaseElement.getAttribute("case-name");
            String nodeName = testCaseElement.getNodeName();
            if (testCase == null || caseName.equals(testCase)) {
                if ("test-case".equals(nodeName)) {
                    parseTestElement(caseName, UtilXml.firstChildElement(testCaseElement));
                } else if ("test-group".equals(nodeName)) {
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
                Debug.logInfo("Added " + testsAdded + " tests [" + casesAdded + " cases] from the class: " + className, MODULE);
            } catch (Exception e) {
                String errMsg = "Unable to load test suite class : " + className;
                Debug.logError(e, errMsg, MODULE);
            }
        } else if ("groovy-test-suite".equals(nodeName)) {
            try {
                Class<? extends TestCase> testClass =
                        UtilGenerics.cast(GroovyUtil.getScriptClassFromLocation(testElement.getAttribute("location")));
                this.testList.add(new TestSuite(testClass, testElement.getAttribute("name")));
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
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
                    Debug.logError(e, MODULE);
                }
            }
        } else if ("webdriver-test".equals(nodeName)) {
            try {
                String className = "org.apache.ofbiz.testtools.WebDriverTest";
                Class<?> cl;
                cl = Class.forName(className);
                Constructor<?> con = cl.getConstructor(String.class, Element.class);
                this.testList.add((Test) con.newInstance(caseName, testElement));
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        } else if ("entity-xml".equals(nodeName)) {
            this.testList.add(new EntityXmlAssertTest(caseName, testElement));
        } else if ("entity-xml-assert".equals(nodeName)) {
            // this is the old, deprecated name for the element, changed because it now does assert or load
            this.testList.add(new EntityXmlAssertTest(caseName, testElement));
        }
    }

    /**
     * Gets suite name.
     * @return the suite name
     */
    String getSuiteName() {
        return this.suiteName;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    Delegator getDelegator() {
        return this.delegator;
    }

    /**
     * Gets test list.
     * @return the test list
     */
    List<Test> getTestList() {
        return testList;
    }


    /**
     * Make test suite test suite.
     * @return the test suite
     */
    public TestSuite makeTestSuite() {
        TestSuite suite = new TestSuite();
        suite.setName(this.getSuiteName());
        for (Test tst: this.getTestList()) {
            prepareTest(tst);
            suite.addTest(tst);
        }

        return suite;
    }

    private void prepareTest(Test test) {
        if (test instanceof TestSuite) {
            Enumeration<Test> subTests = UtilGenerics.cast(((TestSuite) test).tests());
            while (subTests.hasMoreElements()) {
                prepareTest(subTests.nextElement());
            }
        } else if (test instanceof EntityTestCase) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            ((EntityTestCase) test).setDelegator(delegator);
            if (test instanceof OFBizTestCase) {
                ((OFBizTestCase) test).setDispatcher(dispatcher);
            }
            // CHECKSTYLE_ON: ALMOST_ALL
        } else if (test instanceof GroovyScriptTestCase) {
            prepareGroovyScriptTestCase((GroovyScriptTestCase) test);
        }
    }

    private void prepareGroovyScriptTestCase(GroovyScriptTestCase test) {
        test.setDelegator(delegator);
        test.setDispatcher(dispatcher);
        test.setSecurity(dispatcher.getSecurity());
    }
}
