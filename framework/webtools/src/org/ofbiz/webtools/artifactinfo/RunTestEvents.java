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
package org.ofbiz.webtools.artifactinfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ofbiz.base.container.ContainerException;

import org.ofbiz.testtools.*;

/**
 * Event used to run a Junit test
 */
public class RunTestEvents {

    public static final String module = RunTestEvents.class.getName();

    public static String runTest(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        
        String component = request.getParameter("compName");
        String suiteName = request.getParameter("suiteName");
        String caseName = request.getParameter("caseName");
        String result = null;

        String[] args = null;
        if (caseName == null) {
            args = new String[]{"-component=" + component, " -suitename=" + suiteName + " -loglevel=info"};
        } else {
            args = new String[]{"-component=" + component, " -suitename=" + suiteName, " -case=" + caseName, " -loglevel=info"};
        }

        TestRunContainer testRunContainer = new TestRunContainer();
        testRunContainer.init(args, "frontend test run", "   ");
        if (testRunContainer.start() == false) {
            result = "error";
        } else {
            result = "success";
        }

        return result;
    }
}

