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
package org.ofbiz.service.test;

import java.util.Map;

import junit.framework.TestCase;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

public class ServiceEngineTests extends TestCase {

    public static final String DELEGATOR_NAME = "test";
    public static final String DISPATCHER_NAME = "test-dispatcher";

    private LocalDispatcher dispatcher = null;

    public ServiceEngineTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(DELEGATOR_NAME);
        dispatcher = new GenericDispatcher(DISPATCHER_NAME, delegator);
    }

    protected void tearDown() throws Exception {
        dispatcher.deregister();
    }

    public void testBasicJavaInvocation() throws Exception {
        Map result = dispatcher.runSync("testScv", UtilMisc.toMap("message", "Unit Test"));
        assertEquals("Service result success", ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE));        
    }
}
