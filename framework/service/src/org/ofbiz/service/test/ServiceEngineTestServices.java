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
package org.ofbiz.service.test;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class ServiceEngineTestServices {

    public static final String module = ServiceEngineTestServices.class.getName();
    
    public static Map testServiceDeadLockRetry(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            dispatcher.runAsync("testServiceDeadLockRetryThreadA", null, false);
            dispatcher.runAsync("testServiceDeadLockRetryThreadB", null, false);
        } catch (GenericServiceException e) {
            String errMsg = "Error running deadlock test services: " + e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        return ServiceUtil.returnSuccess();
    }
    
    public static Map testServiceDeadLockRetryThreadA(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        try {
            // grab entity SVCLRT_A by changing, then wait, then find and change SVCLRT_B
            GenericValue testingTypeA = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLRT_A"));
            testingTypeA.set("description", "New description for SVCLRT_A");
            testingTypeA.store();
            
            // wait at least long enough for the other method to have locked resource B
            ServiceEngineTestServices.class.wait(100);

            GenericValue testingTypeB = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLRT_B"));
            testingTypeB.set("description", "New description for SVCLRT_B");
            testingTypeB.store();
        } catch (GenericEntityException e) {
            String errMsg = "Entity Engine Exception running dead lock test thread A: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (InterruptedException e) {
            String errMsg = "Wait Interrupted Exception running dead lock test thread A: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map testServiceDeadLockRetryThreadB(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        try {
            // grab entity SVCLRT_B by changing, then wait, then change SVCLRT_A
            GenericValue testingTypeB = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLRT_B"));
            testingTypeB.set("description", "New description for SVCLRT_B");
            testingTypeB.store();
            
            // wait at least long enough for the other method to have locked resource B
            ServiceEngineTestServices.class.wait(100);

            GenericValue testingTypeA = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLRT_A"));
            testingTypeA.set("description", "New description for SVCLRT_A");
            testingTypeA.store();
        } catch (GenericEntityException e) {
            String errMsg = "Entity Engine Exception running dead lock test thread B: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (InterruptedException e) {
            String errMsg = "Wait Interrupted Exception running dead lock test thread B: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map testServiceLockWaitTimeoutRetry(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            dispatcher.runAsync("testServiceDeadLockRetryThreadA", null, false);
            dispatcher.runAsync("testServiceDeadLockRetryThreadB", null, false);
        } catch (GenericServiceException e) {
            String errMsg = "Error running deadlock test services: " + e.toString();
            Debug.logError(e, errMsg, module);
        }
        
        return ServiceUtil.returnSuccess();
    }
    public static Map testServiceLockWaitTimeoutRetryGrabber(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        try {
            // grab entity SVCLWTRT by changing, then wait a LONG time, ie more than the wait timeout
            GenericValue testingType = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLWTRT"));
            testingType.set("description", "New description for SVCLWTRT");
            testingType.store();
            
            // wait at least long enough for the other method to have locked resource wiat time out
            // wait 100 seconds
            ServiceEngineTestServices.class.wait(100 * 1000);
        } catch (GenericEntityException e) {
            String errMsg = "Entity Engine Exception running lock wait timeout test Grabber thread: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (InterruptedException e) {
            String errMsg = "Wait Interrupted Exception running lock wait timeout test Grabber thread: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map testServiceLockWaitTimeoutRetryWaiter(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        try {
            // TRY grab entity SVCLWTRT by looking up and changing, should get a lock wait timeout exception because of the Grabber thread
            GenericValue testingType = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "SVCLWTRT"));
            testingType.set("description", "New description for SVCLWTRT");
            testingType.store();
            
        } catch (GenericEntityException e) {
            String errMsg = "Entity Engine Exception running lock wait timeout test Waiter thread: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }
}
