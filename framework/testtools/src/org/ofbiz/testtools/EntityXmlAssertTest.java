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

import junit.framework.TestResult;
import junit.framework.AssertionFailedError;

import org.w3c.dom.Element;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.util.EntityDataAssert;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.location.FlexibleLocation;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;

public class EntityXmlAssertTest extends TestCaseBase {

    public static final String module = ServiceTest.class.getName();

    protected String entityXmlUrlString;

    /**
     * @param modelTestSuite
     */
    public EntityXmlAssertTest(String caseName, ModelTestSuite modelTestSuite, Element mainElement) {
        super(caseName, modelTestSuite);
        this.entityXmlUrlString = mainElement.getAttribute("entity-xml-url");
    }

    public int countTestCases() {
        int testCaseCount = 0;
        try {
            URL entityXmlURL = FlexibleLocation.resolveLocation(entityXmlUrlString);
            List checkValueList = modelTestSuite.getDelegator().readXmlDocument(entityXmlURL);
            testCaseCount = checkValueList.size();
        } catch (Exception e) {
            Debug.logError(e, "Error getting test case count", module);
        }
        return testCaseCount;
    }

    public void run(TestResult result) {
        result.startTest(this);

        try {
            URL entityXmlURL = FlexibleLocation.resolveLocation(entityXmlUrlString);
            GenericDelegator delegator = modelTestSuite.getDelegator();
            List errorMessages = new ArrayList();

            EntityDataAssert.assertData(entityXmlURL, delegator, errorMessages);

            if (UtilValidate.isNotEmpty(errorMessages)) {
                Iterator failureIterator = errorMessages.iterator();
                while (failureIterator.hasNext()) {
                    String failureMessage = (String) failureIterator.next();
                    result.addFailure(this, new AssertionFailedError(failureMessage));
                }
            }
        } catch (Exception e) {
            result.addError(this, e);
        }
        
        result.endTest(this);
    }
}
