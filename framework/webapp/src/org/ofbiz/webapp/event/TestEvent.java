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
package org.ofbiz.webapp.event;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;

/**
 * Test Events
 */
public class TestEvent {

    public static final String module = TestEvent.class.getName();
    
    public static String test(HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute("MESSAGE", "Test Event Ran Fine.");
        Debug.log("Test Event Ran Fine.", module);
        return "success";
    }

    public static String httpClientTest(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpClient http = new HttpClient("http://www.ofbiz.org/cgi-bin/http_test.pl");

            http.setHeader("Cookie", "name=value,value=name");
            http.setHeader("User-Agent", "Mozilla/4.0");
            http.setParameter("testId", "testing");
            Debug.log(http.post(), module);
        } catch (Exception e) {
            Debug.log(e, "HttpClientException Caught.", module);
        }
        return "success";
    }
}
