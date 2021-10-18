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
package org.apache.ofbiz.webtools.artifactinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.testtools.TestRunContainer;

/**
 * Event used to run a Junit test
 */
public class RunTestEvents {

    public static String runTest(HttpServletRequest request, HttpServletResponse response) throws ContainerException {

        String component = (String) request.getAttribute("compName");
        String suiteName = (String) request.getAttribute("suiteName");
        String caseName = (String) request.getAttribute("caseName");

        List<StartupCommand> ofbizCommands = new ArrayList<>();
        Map<String, String> cmdArgs = UtilMisc.toMap("component", component, "suitename", suiteName);
        if (caseName == null) {
            cmdArgs.put("case", caseName);
        }
        ofbizCommands.add(new StartupCommand.Builder("test").properties(cmdArgs).build());

        TestRunContainer testRunContainer = new TestRunContainer();
        testRunContainer.init(ofbizCommands, "frontend test run", "   ");
        if (!testRunContainer.start()) {
            return "error";
        }
        return "success";
    }
}
