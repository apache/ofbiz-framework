/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.testtools;

import junit.framework.TestResult;

import org.w3c.dom.Element;

/**
 * @author jonesde
 *
 */
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
        // TODO Auto-generated method stub
        
    }
}
