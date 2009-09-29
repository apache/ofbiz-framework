/*
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
 */
package org.ofbiz.testtools.seleniumxml;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.ofbiz.testtools.seleniumxml.util.TestUtils;
import org.python.util.PythonInterpreter;

public class JythonRunner {

    private String urlName;
    private SeleniumXml parent;
    private SeleniumXml currentTest;
    
    private int currentRowIndx;
    
    
    public JythonRunner(String urlName, SeleniumXml parent) {
        super();
        this.urlName = urlName;
        this.parent = parent;
    }

    public void runTest() {

        PythonInterpreter interp = InitJython.getInterpreter();
        
        Map<String, Object> map = this.parent.getMap();
        map.put("url", this.urlName);
        try {
            String scriptText = TestUtils.readUrlText(this.urlName);
            interp.set("context", map);
            interp.exec(scriptText);
        } catch(MalformedURLException e) {
            System.out.println("Scriptrunner, runTest, MalformedURLException error: " + e.getMessage());
        } catch(IOException e) {
            System.out.println("Scriptrunner, runTest, IOException error: " + e.getMessage());
        }
        
        
    }
    
}
