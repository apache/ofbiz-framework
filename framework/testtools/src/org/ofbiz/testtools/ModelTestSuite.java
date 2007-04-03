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
package org.ofbiz.testtools;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

/**
 * Use this class in a JUnit test runner to bootstrap the Test Suite runner. 
 */
public class ModelTestSuite {

    public static final String module = ModelTestSuite.class.getName();
    
    protected String suiteName;
    protected String delegatorName;
    protected String dispatcherName;
    
    protected GenericDelegator delegator;
    protected LocalDispatcher dispatcher;

    protected List testList = FastList.newInstance();

    public ModelTestSuite(Element mainElement) {
        this.suiteName = mainElement.getAttribute("suite-name");

        this.delegatorName = mainElement.getAttribute("delegator-name");
        if (UtilValidate.isEmpty(this.delegatorName)) this.delegatorName = "test";
        
        this.dispatcherName = mainElement.getAttribute("dispatcher-name");
        if (UtilValidate.isEmpty(this.dispatcherName)) this.dispatcherName = "test-dispatcher";
        
        this.delegator = GenericDelegator.getGenericDelegator(this.delegatorName);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(this.dispatcherName, delegator);
        
        List testCaseElementList = UtilXml.childElementList(mainElement, "test-case");
        Iterator testCaseElementIter = testCaseElementList.iterator();
        while (testCaseElementIter.hasNext()) {
            Element testCaseElement = (Element) testCaseElementIter.next();
            String caseName = testCaseElement.getAttribute("case-name");
            
            Element childElement = UtilXml.firstChildElement(testCaseElement);
            String nodeName = childElement.getNodeName();
            if ("junit-test-suite".equals(nodeName)) {
                String className = childElement.getAttribute("class-name");

                try {
                    Class clz = ObjectType.loadClass(className);
                    TestSuite suite = new TestSuite();
                    suite.addTestSuite(clz);
                    Enumeration testEnum = suite.tests();
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
                this.testList.add(new ServiceTest(caseName, this, childElement));
            } else if ("simple-method-test".equals(nodeName)) {
                this.testList.add(new SimpleMethodTest(caseName, this, childElement));
            } else if ("entity-xml-assert".equals(nodeName)) {
                this.testList.add(new EntityXmlAssertTest(caseName, this, childElement));
            } else if ("jython-test".equals(nodeName)) {
                this.testList.add(new JythonTest(caseName, this, childElement));
            }
        }
    }
    
    String getSuiteName() {
        return this.suiteName;
    }
    
    GenericDelegator getDelegator() {
        return this.delegator;
    }
    
    LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }
    
    List getTestList() {
        return testList;
    }
}
