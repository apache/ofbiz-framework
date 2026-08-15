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
    private List<SuiteEntry> testList = new ArrayList<>();

    public ModelTestSuite(Element mainElement, String testCase) {
        String uniqueSuffix = "-" + RandomStringUtils.randomAlphanumeric(10);
        this.suiteName = mainElement.getAttribute("suite-name");
        this.delegator = DelegatorFactory.getDelegator(DELEGATOR_NAME).makeTestDelegator(DELEGATOR_NAME + uniqueSuffix);
        this.dispatcher = ServiceContainer.getLocalDispatcher(DISPATCHER_NAME + uniqueSuffix, delegator);

        List<Element> testCaseElements = List.copyOf(UtilXml.childElementList(mainElement, UtilMisc.toSet("test-case", "test-group")));
        for (Element testCaseElement : selectTestCaseElements(testCaseElements, testCase)) {
            String caseName = testCaseElement.getAttribute("case-name");
            String nodeName = testCaseElement.getNodeName();
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

    /**
     * Selects which of a suite's ordered test-case/test-group elements should run for a given
     * {@code case=} filter.
     *
     * <p>A single case is rarely self-contained: every testdef file in this repo follows the
     * convention of a data-load case (an {@code entity-xml action="load"}, typically named
     * {@code *-data-load}/{@code load*TestData}) declared immediately before the case(s) that consume
     * it - see e.g. applications/party/testdef/PartyTests.xml's {@code party-tests-data-load} feeding
     * {@code party-tests}. Filtering down to only the exact requested case-name, the way this method
     * used to work, silently drops that prerequisite and leaves the requested case failing for lack of
     * data it never asked to be responsible for loading itself.
     *
     * <p>Only that one shape - an {@code entity-xml}/{@code entity-xml-assert} case whose
     * {@code action} is {@code "load"} - is auto-included as a prerequisite; every other preceding
     * case is skipped unless it is itself the requested case. That is deliberately narrower than
     * "run everything declared before the requested case": a {@code load} is additive and
     * side-effect-free relative to every other case in the suite, so running it unconditionally is
     * safe, but nothing else in a testdef file has that property -
     * <ul>
     * <li>A {@code jupiter-test-suite}/{@code junit-test-suite} case, or a {@code test-group} (a
     * repo-wide scan found 14 of 15 {@code test-group} children are themselves
     * {@code jupiter-test-suite}), is a whole independent test class in its own right. Several
     * testdef files (e.g. framework/entity/testdef/entitytests.xml) bundle multiple such classes into
     * one {@code <test-suite>} purely for organization, with no data relationship between them -
     * requesting entity-crypto-tests (declared third) used to also run the unrelated, much larger and
     * deliberately slow entity-tests/entity-util-tests classes ahead of it, turning a single-case
     * request into most of the suite's runtime.</li>
     * <li>A {@code service-test} is always a real functional test in every testdef file in this repo,
     * never data loading (see framework/service/testdef/servicetests.xml's
     * service-dead-lock-retry-test and friends).</li>
     * <li>An {@code entity-xml} whose action is {@code "assert"} (the default when unset) is a check,
     * not a load - in servicetests.xml, service-eca-global-event-exec-assert-data verifies the side
     * effects of the service-test case declared immediately before it. Auto-including an assert case
     * without the real test whose effects it checks would assert against data that was never
     * created - trading one bug (a missing prerequisite) for another (a spurious failure) rather than
     * fixing it.</li>
     * </ul>
     *
     * <p>Nothing declared after the requested case runs, so this is not simply "ignore the filter":
     * a case still can't see data set up by a case that only runs after it in the suite's own
     * declared order.
     *
     * <p>When {@code testCase} is {@code null} (no filter - run the whole suite), every element is
     * selected unchanged. When {@code testCase} names a case-name that isn't present in this
     * particular document at all, nothing is selected - unchanged from the old exact-match
     * behavior, and still what lets JunitSuiteWrapper's suite-name filtering keep a testdef file
     * that simply doesn't define the requested case from contributing anything.
     * @param testCaseElements the suite's test-case/test-group elements, in declared order
     * @param testCase the requested case-name, or {@code null} to select every element
     * @return the elements to run, in the same order
     */
    static List<Element> selectTestCaseElements(List<Element> testCaseElements, String testCase) {
        if (testCase == null) {
            return testCaseElements;
        }
        List<Element> selected = new ArrayList<>();
        for (Element element : testCaseElements) {
            boolean isTarget = testCase.equals(element.getAttribute("case-name"));
            if (isTarget) {
                selected.add(element);
                return selected;
            }
            if (isDataLoadCase(element)) {
                selected.add(element);
            }
        }
        return List.of();
    }

    /**
     * True for a {@code <test-case>} whose sole child is an {@code entity-xml}/{@code entity-xml-assert}
     * element with {@code action="load"}: the one shape safe to auto-include as a prerequisite ahead
     * of a requested case. See selectTestCaseElements()'s javadoc for why every other shape - including
     * the same elements with a different action - is excluded instead.
     * @param testCaseElement one of a suite's test-case/test-group elements
     * @return true if this element is a genuine data-load case
     */
    private static boolean isDataLoadCase(Element testCaseElement) {
        if (!"test-case".equals(testCaseElement.getNodeName())) {
            return false;
        }
        Element child = UtilXml.firstChildElement(testCaseElement);
        if (child == null) {
            return false;
        }
        String nodeName = child.getNodeName();
        boolean isEntityXml = "entity-xml".equals(nodeName) || "entity-xml-assert".equals(nodeName);
        return isEntityXml && "load".equals(child.getAttribute("action"));
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
                    this.testList.add(new SuiteEntry.Junit3Entry(tst));
                    casesAdded += tst.countTestCases();
                    testsAdded++;
                }
                Debug.logInfo("Added " + testsAdded + " tests [" + casesAdded + " cases] from the class: " + className, MODULE);
            } catch (Exception e) {
                String errMsg = "Unable to load test suite class : " + className;
                Debug.logError(e, errMsg, MODULE);
            }
        } else if ("service-test".equals(nodeName)) {
            this.testList.add(new SuiteEntry.Junit3Entry(new ServiceTest(caseName, testElement)));
        } else if ("simple-method-test".equals(nodeName)) {
            if (UtilValidate.isNotEmpty(testElement.getAttribute("name"))) {
                this.testList.add(new SuiteEntry.Junit3Entry(new SimpleMethodTest(caseName, testElement)));
            } else {
                String methodLocation = testElement.getAttribute("location");
                List<SimpleMethod> simpleMethods;
                try {
                    simpleMethods = SimpleMethod.getSimpleMethodsList(methodLocation, null);
                    for (SimpleMethod simpleMethod : simpleMethods) {
                        String methodName = simpleMethod.getMethodName();
                        if (methodName.startsWith("test")) {
                            this.testList.add(new SuiteEntry.Junit3Entry(
                                    new SimpleMethodTest(caseName + "." + methodName, methodLocation, methodName)));
                        }
                    }
                } catch (MiniLangException e) {
                    Debug.logError(e, MODULE);
                }
            }
        } else if ("jupiter-test-suite".equals(nodeName)) {
            String className = testElement.getAttribute("class-name");
            try {
                Class<?> clz = ObjectType.loadClass(className);
                this.testList.add(new SuiteEntry.JupiterEntry(clz));
                Debug.logInfo("Added Jupiter test class: " + className, MODULE);
            } catch (Exception e) {
                Debug.logError(e, "Unable to load jupiter test suite class : " + className, MODULE);
            }
        } else if ("webdriver-test".equals(nodeName)) {
            try {
                String className = "org.apache.ofbiz.testtools.WebDriverTest";
                Class<?> cl;
                cl = Class.forName(className);
                Constructor<?> con = cl.getConstructor(String.class, Element.class);
                this.testList.add(new SuiteEntry.Junit3Entry((Test) con.newInstance(caseName, testElement)));
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        } else if ("entity-xml".equals(nodeName)) {
            this.testList.add(new SuiteEntry.Junit3Entry(new EntityXmlAssertTest(caseName, testElement)));
        } else if ("entity-xml-assert".equals(nodeName)) {
            // this is the old, deprecated name for the element, changed because it now does assert or load
            this.testList.add(new SuiteEntry.Junit3Entry(new EntityXmlAssertTest(caseName, testElement)));
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
     * Gets dispatcher.
     * @return the dispatcher
     */
    LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * Gets test list.
     * @return the test list
     */
    List<SuiteEntry> getTestList() {
        return testList;
    }


    /**
     * Gets the suite's ordered, delegator/dispatcher-prepared test entries, ready to execute.
     * Replaces makeTestSuite(), which built a junit.framework.TestSuite purely so the result could
     * satisfy junit.framework.Test's contract - no longer needed now that TestRunContainer executes
     * SuiteEntry.Junit3Entry/SuiteEntry.JupiterEntry directly instead of running everything through
     * one shared TestSuite.
     * @return the ordered, prepared test entries
     */
    public List<SuiteEntry> getPreparedTestList() {
        testList.forEach(this::prepareTest);
        return testList;
    }

    private void prepareTest(SuiteEntry entry) {
        if (entry instanceof SuiteEntry.Junit3Entry junit3Entry) {
            prepareJunit3Test(junit3Entry.test());
        }
        // SuiteEntry.JupiterEntry: nothing to prepare here - TestRunContainer arms JupiterClassRunner
        // with the delegator/dispatcher directly, immediately before running that one entry.
    }

    private void prepareJunit3Test(Test test) {
        if (test instanceof TestSuite) {
            Enumeration<Test> subTests = UtilGenerics.cast(((TestSuite) test).tests());
            while (subTests.hasMoreElements()) {
                prepareJunit3Test(subTests.nextElement());
            }
        } else if (test instanceof EntityTestCase) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            ((EntityTestCase) test).setDelegator(delegator);
            if (test instanceof OFBizTestCase) {
                ((OFBizTestCase) test).setDispatcher(dispatcher);
            }
            // CHECKSTYLE_ON: ALMOST_ALL
        } else if (test instanceof GroovyScriptAssert) {
            prepareGroovyScriptAssert((GroovyScriptAssert) test);
        }
    }

    private void prepareGroovyScriptAssert(GroovyScriptAssert test) {
        test.setDelegator(delegator);
        test.setDispatcher(dispatcher);
        test.setSecurity(dispatcher.getSecurity());
    }
}
