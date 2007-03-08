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
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.ModelService;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

public class ServiceTest extends TestCaseBase {

    public static final String module = ServiceTest.class.getName();

    protected String serviceName;

    /**
     * @param modelTestSuite
     */
    public ServiceTest(String caseName, ModelTestSuite modelTestSuite, Element mainElement) {
        super(caseName, modelTestSuite);
        this.serviceName = mainElement.getAttribute("service-name");
    }

    public int countTestCases() {
        return 1;
    }

    public void run(TestResult result) {

        result.startTest(this);

        LocalDispatcher dispatcher = modelTestSuite.getDispatcher();

        try {

            Map serviceResult = dispatcher.runSync(serviceName, UtilMisc.toMap("testCase", this));

            // do something with the errorMessage
            String errorMessage = (String) serviceResult.get(ModelService.ERROR_MESSAGE);
            if (UtilValidate.isNotEmpty(errorMessage)) {
                result.addFailure(this, new AssertionFailedError(errorMessage));
            }

            // do something with the errorMessageList
            List errorMessageList = (List) serviceResult.get(ModelService.ERROR_MESSAGE_LIST);
            if (UtilValidate.isNotEmpty(errorMessageList)) {
                Iterator i = errorMessageList.iterator();
                while (i.hasNext()) {
                    result.addFailure(this, new AssertionFailedError((String) i.next()));
                }
            }

            // do something with the errorMessageMap
            Map errorMessageMap = (Map) serviceResult.get(ModelService.ERROR_MESSAGE_MAP);
            if (!UtilValidate.isEmpty(errorMessageMap)) {
                Iterator i = errorMessageMap.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry entry = (Map.Entry) i.next();
                    result.addFailure(this, new AssertionFailedError(entry.getKey() + ": " + entry.getValue()));
                }
            }

        } catch (GenericServiceException e) {
            result.addError(this, e);
        }

        result.endTest(this);
    }
}
