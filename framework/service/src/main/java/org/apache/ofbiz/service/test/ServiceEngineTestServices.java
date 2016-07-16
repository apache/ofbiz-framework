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
package org.apache.ofbiz.service.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericResultWaiter;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ServiceEngineTestServices {

    public static final String module = ServiceEngineTestServices.class.getName();
    public static final String resource = "ServiceErrorUiLabels";

    public static Map<String, Object> testServiceDeadLockRetry(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            // NOTE using persist=false so that the lock retry will have to fix the problem instead of the job poller picking it up again
            GenericResultWaiter threadAWaiter = dispatcher.runAsyncWait("testServiceDeadLockRetryThreadA", null, false);
            GenericResultWaiter threadBWaiter = dispatcher.runAsyncWait("testServiceDeadLockRetryThreadB", null, false);
            // make sure to wait for these to both finish to make sure results aren't checked until they are done
            Map<String, Object> threadAResult = threadAWaiter.waitForResult();
            Map<String, Object> threadBResult = threadBWaiter.waitForResult();
            List<Object> errorList = new LinkedList<Object>();
            if (ServiceUtil.isError(threadAResult)) {
                errorList.add(UtilProperties.getMessage(resource, "ServiceTestDeadLockThreadA", UtilMisc.toMap("errorString", ServiceUtil.getErrorMessage(threadAResult)), locale));
            }
            if (ServiceUtil.isError(threadBResult)) {
                errorList.add(UtilProperties.getMessage(resource, "ServiceTestDeadLockThreadB", UtilMisc.toMap("errorString", ServiceUtil.getErrorMessage(threadBResult)), locale));
            }
            if (errorList.size() > 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestDeadLockRetry", locale), errorList, null, null);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error running deadlock test services: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestDeadLockError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> testServiceDeadLockRetryThreadA(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            // grab entity SVCLRT_A by changing, then wait, then find and change SVCLRT_B
            GenericValue testingTypeA = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_A").queryOne();
            testingTypeA.set("description", "New description for SVCLRT_A");
            testingTypeA.store();

            // wait at least long enough for the other method to have locked resource B
            Debug.logInfo("In testServiceDeadLockRetryThreadA just updated SVCLRT_A, beginning wait", module);
            Thread.sleep(100);

            Debug.logInfo("In testServiceDeadLockRetryThreadA done with wait, updating SVCLRT_B", module);
            GenericValue testingTypeB = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_B").queryOne();
            testingTypeB.set("description", "New description for SVCLRT_B");
            testingTypeB.store();

            Debug.logInfo("In testServiceDeadLockRetryThreadA done with updating SVCLRT_B, updating SVCLRT_AONLY", module);
            GenericValue testingTypeAOnly = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_AONLY").queryOne();
            testingTypeAOnly.set("description", "New description for SVCLRT_AONLY; this is only changed by thread A so if it doesn't match something happened to thread A!");
            testingTypeAOnly.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running dead lock test thread A: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineExceptionThreadA", UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, "Wait Interrupted Exception running dead lock test thread A: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineWaitInterruptedExceptionThreadA", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceDeadLockRetryThreadB(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            // grab entity SVCLRT_B by changing, then wait, then change SVCLRT_A
            GenericValue testingTypeB = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_B").queryOne();
            testingTypeB.set("description", "New description for SVCLRT_B");
            testingTypeB.store();

            // wait at least long enough for the other method to have locked resource B
            Debug.logInfo("In testServiceDeadLockRetryThreadB just updated SVCLRT_B, beginning wait", module);
            Thread.sleep(100);

            Debug.logInfo("In testServiceDeadLockRetryThreadB done with wait, updating SVCLRT_A", module);
            GenericValue testingTypeA = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_A").queryOne();
            testingTypeA.set("description", "New description for SVCLRT_A");
            testingTypeA.store();

            Debug.logInfo("In testServiceDeadLockRetryThreadA done with updating SVCLRT_A, updating SVCLRT_BONLY", module);
            GenericValue testingTypeAOnly = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLRT_BONLY").queryOne();
            testingTypeAOnly.set("description", "New description for SVCLRT_BONLY; this is only changed by thread B so if it doesn't match something happened to thread B!");
            testingTypeAOnly.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running dead lock test thread B: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineExceptionThreadB", UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, "Wait Interrupted Exception running dead lock test thread B: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineWaitInterruptedExceptionThreadB", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    public static Map<String, Object> testServiceLockWaitTimeoutRetry(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            // NOTE using persist=false so that the lock retry will have to fix the problem instead of the job poller picking it up again
            GenericResultWaiter grabberWaiter = dispatcher.runAsyncWait("testServiceLockWaitTimeoutRetryGrabber", null, false);
            GenericResultWaiter waiterWaiter = dispatcher.runAsyncWait("testServiceLockWaitTimeoutRetryWaiter", null, false);
            // make sure to wait for these to both finish to make sure results aren't checked until they are done
            Map<String, Object> grabberResult = grabberWaiter.waitForResult();
            Map<String, Object> waiterResult = waiterWaiter.waitForResult();
            List<Object> errorList = new LinkedList<Object>();
            if (ServiceUtil.isError(grabberResult)) {
                errorList.add("Error running testServiceLockWaitTimeoutRetryGrabber: " + ServiceUtil.getErrorMessage(grabberResult));
            }
            if (ServiceUtil.isError(waiterResult)) {
                errorList.add("Error running testServiceLockWaitTimeoutRetryWaiter: " + ServiceUtil.getErrorMessage(waiterResult));
            }
            if (errorList.size() > 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockWaitTimeoutRetry", locale), errorList, null, null);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error running deadlock test services: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestDeadLockError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceLockWaitTimeoutRetryGrabber(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            // grab entity SVCLWTRT by changing, then wait a LONG time, ie more than the wait timeout
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLWTRT").queryOne();
            testingType.set("description", "New description for SVCLWTRT from the GRABBER service, this should be replaced by Waiter service in the service engine auto-retry");
            testingType.store();

            Debug.logInfo("In testServiceLockWaitTimeoutRetryGrabber just updated SVCLWTRT, beginning wait", module);

            // wait at least long enough for the other method to have locked resource wait time out
            // (tx timeout 6s on this the Grabber and 2s on the Waiter): wait 4 seconds because timeout on this
            Thread.sleep(4 * 1000);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running lock wait timeout test Grabber thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockWaitTimeoutRetryGrabber", UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, "Wait Interrupted Exception running lock wait timeout test Grabber thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockInterruptedExceptionRetryGrabber", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceLockWaitTimeoutRetryWaiter(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            // wait for a small amount of time to make sure the grabber does it's thing first
            Thread.sleep(100);

            Debug.logInfo("In testServiceLockWaitTimeoutRetryWaiter about to update SVCLWTRT, wait starts here", module);

            // TRY grab entity SVCLWTRT by looking up and changing, should get a lock wait timeout exception because of the Grabber thread
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLWTRT").queryOne();
            testingType.set("description", "New description for SVCLWTRT from Waiter service, this is the value that should be there.");
            testingType.store();

            Debug.logInfo("In testServiceLockWaitTimeoutRetryWaiter successfully updated SVCLWTRT", module);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running lock wait timeout test Waiter thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockWaitTimeoutRetryWaiter", UtilMisc.toMap("errorString", e.toString()), locale));
        } catch (InterruptedException e) {
            Debug.logError(e, "Wait Interrupted Exception running lock wait timeout test Waiter thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockInterruptedExceptionRetryWaiter", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    /**
     * NOTE that this is a funny case where the auto-retry in the service engine for the call to
     * testServiceLockWaitTimeoutRetryCantRecoverWaiter would NOT be able to recover because it would try again
     * given the new transaction and all, but the lock for the waiting thread would still be there... so it will fail
     * repeatedly.
     *
     * TODO: there's got to be some way to do this, but how?!? :(
     *
     * NOTE: maybe this will work: create a list that the service engine maintains of services it will run after the
     * current service run is complete, and AFTER it has committed or rolled back its transaction; if a service finds
     * it has a lock wait timeout, add itself to the list for its parent service (somehow...) and off we go!
     *
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the results of the service execution
     */
    public static Map<String, Object> testServiceLockWaitTimeoutRetryCantRecover(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            // grab entity SVCLWTRTCR by changing, then wait a LONG time, ie more than the wait timeout
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLWTRTCR").queryOne();
            testingType.set("description", "New description for SVCLWTRTCR from Lock Wait Timeout Lock GRABBER, this should be replaced by the one in the Waiter service.");
            testingType.store();

            Debug.logInfo("In testServiceLockWaitTimeoutRetryCantRecover (grabber) just updated SVCLWTRTCR, running sub-service in own transaction", module);
            // timeout is 5 seconds so it is longer than the tx timeout for this service, so will fail quickly; with this transaction keeping a lock on the record and that one trying to get it, bam we cause the error
            Map<String, Object> waiterResult = dispatcher.runSync("testServiceLockWaitTimeoutRetryCantRecoverWaiter", null, 5, true);
            if (ServiceUtil.isError(waiterResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockWaitTimeoutRetryCantRecoverWaiter", locale), null, null, waiterResult);
            }

            Debug.logInfo("In testServiceLockWaitTimeoutRetryCantRecover (grabber) successfully finished running sub-service in own transaction", module);
        } catch (GenericServiceException e) {
            String errMsg = "Error running deadlock test services: " + e.toString();
            Debug.logError(e, errMsg, module);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running lock wait timeout test main/Grabber thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockInterruptedExceptionRetryGrabber", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceLockWaitTimeoutRetryCantRecoverWaiter(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            Debug.logInfo("In testServiceLockWaitTimeoutRetryCantRecoverWaiter updating SVCLWTRTCR", module);

            // TRY grab entity SVCLWTRTCR by looking up and changing, should get a lock wait timeout exception because of the Grabber thread
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVCLWTRTCR").queryOne();
            testingType.set("description", "New description for SVCLWTRTCR from Lock Wait Timeout Lock Waiter, this is the value that should be there.");
            testingType.store();

            Debug.logInfo("In testServiceLockWaitTimeoutRetryCantRecoverWaiter successfully updated SVCLWTRTCR", module);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception running lock wait timeout test Waiter thread: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestLockInterruptedExceptionRetryWaiter", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    // ==================================================

    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentErrorCatchWrapper(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            Map<String, Object> resultMap = dispatcher.runSync("testServiceOwnTxSubServiceAfterSetRollbackOnlyInParent", null, 60, true);
            if (ServiceUtil.isError(resultMap)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestOwnTxSubServiceAfterSetRollbackOnlyInParentErrorCatchWrapper", locale), null, null, resultMap);
            }
        } catch (GenericServiceException e) {
            String errMsg = "This is the expected error running sub-service with own tx after the parent has set rollback only, logging and ignoring: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            // change the SVC_SRBO value first to test that the rollback really does revert/reset
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVC_SRBO").queryOne();
            testingType.set("description", "New description for SVC_SRBO; this should be reset on the rollback, if this is in the db then the test failed");
            testingType.store();

            TransactionUtil.setRollbackOnly("Intentionally setting rollback only for testing purposes", null);

            Map<String, Object> resultMap = dispatcher.runSync("testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentSubService", null, 60, true);
            if (ServiceUtil.isError(resultMap)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestOwnTxSubServiceAfterSetRollbackOnlyInParent", locale), null, null, resultMap);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error running sub-service with own tx: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestOwnTxError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceOwnTxSubServiceAfterSetRollbackOnlyInParentSubService(DispatchContext dctx, Map<String, ? extends Object> context) {
        // this service doesn't actually have to do anything, the problem was in just pausing and resuming the transaciton with setRollbackOnly
        return ServiceUtil.returnSuccess();
    }


    // ==================================================

    public static Map<String, Object> testServiceEcaGlobalEventExec(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            // this will return an error, but we'll ignore the result
            dispatcher.runSync("testServiceEcaGlobalEventExecToRollback", null, 60, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling sub-service, it should return an error but not throw an exception, so something went wrong: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestSubServiceError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // this service doesn't actually have to do anything, just a placeholder for ECA rules, this one should commit
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceEcaGlobalEventExecOnCommit(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVC_SECAGC").queryOne();
            testingType.set("description", "New description for SVC_SECAGC, what it should be after the global-commit test");
            testingType.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> testServiceEcaGlobalEventExecToRollback(DispatchContext dctx, Map<String, ? extends Object> context) {
        // this service doesn't actually have to do anything, just a placeholder for ECA rules, this one should rollback
        Locale locale = (Locale) context.get("locale");
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestRollback", locale));
    }
    public static Map<String, Object> testServiceEcaGlobalEventExecOnRollback(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try {
            GenericValue testingType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "SVC_SECAGR").queryOne();
            testingType.set("description", "New description for SVC_SECAGR, what it should be after the global-rollback test");
            testingType.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity Engine Exception: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ServiceTestEntityEngineError", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
