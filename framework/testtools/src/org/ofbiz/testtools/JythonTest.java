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

import org.ofbiz.service.testtools.OFBizTestCase;
import org.w3c.dom.Element;

import junit.framework.TestResult;

public class JythonTest extends OFBizTestCase {

    public static final String module = JythonTest.class.getName();

    protected String scriptLocation;

    /**
     * @param modelTestSuite
     */
    public JythonTest(String caseName, Element mainElement) {
        super(caseName);
        this.scriptLocation = mainElement.getAttribute("script-location");
    }

    @Override
    public int countTestCases() {
        return 1;
    }

    @Override
    public void run(TestResult result) {
        // TODO Auto-generated method stub
        result.startTest(this);

        result.endTest(this);
    }
}
