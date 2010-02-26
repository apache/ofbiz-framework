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
package org.ofbiz.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.transaction.Transaction;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.LRUMap;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.DebugXaResource;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityConfigurationException;
import org.ofbiz.security.SecurityFactory;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.security.authz.AuthorizationFactory;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.eca.ServiceEcaRule;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.ofbiz.service.engine.GenericEngine;
import org.ofbiz.service.engine.GenericEngineFactory;
import org.ofbiz.service.group.ServiceGroupReader;
import org.ofbiz.service.jms.JmsListenerFactory;
import org.ofbiz.service.job.JobManager;
import org.ofbiz.service.job.JobManagerException;
import org.ofbiz.service.semaphore.ServiceSemaphore;
import org.w3c.dom.Element;

/**
 * Global Service Dispatcher
 */
public class ServiceDispatcher {

    public static final String module = ServiceDispatcher.class.getName();
    public static final int lruLogSize = 200;
    public static final int LOCK_RETRIES = 3;

    protected static final Map<RunningService, ServiceDispatcher> runLog = new LRUMap<RunningService, ServiceDispatcher>(lruLogSize);
    protected static Map<String, ServiceDispatcher> dispatchers = FastMap.newInstance();
    protected static boolean enableJM = true;
    protected static boolean enableJMS = true;
    protected static boolean enableSvcs = true;
    protected static boolean serviceDebugMode = true;

    protected Delegator delegator = null;
    protected GenericEngineFactory factory = null;
    protected Authorization authz = null;
    protected Security security = null;
    protected Map<String, DispatchContext> localContext = null;
    protected Map<String, List<GenericServiceCallback>> callbacks = null;
    protected JobManager jm = null;
    protected JmsListenerFactory jlf = null;

    protected ServiceDispatcher(Delegator delegator, boolean enableJM, boolean enableJMS, boolean enableSvcs) {
        Debug.logInfo("[ServiceDispatcher] : Creating new instance.", module);
        factory = new GenericEngineFactory(this);
        ServiceGroupReader.readConfig();
        ServiceEcaUtil.readConfig();

        this.delegator = delegator;
        this.localContext = FastMap.newInstance();
        this.callbacks = FastMap.newInstance();

        if (delegator != null) {
            try {
                this.authz = AuthorizationFactory.getInstance(delegator);
                this.security = SecurityFactory.getInstance(delegator);
            } catch (SecurityConfigurationException e) {
                Debug.logError(e, "[ServiceDispatcher.init] : No instance of security implementation found.", module);
            }
        }

        // job manager needs to always be running, but the poller thread does not
        try {
            Delegator origDelegator = this.delegator;
            if (!this.delegator.getOriginalDelegatorName().equals(this.delegator.getDelegatorName())) {
                origDelegator = DelegatorFactory.getDelegator(this.delegator.getOriginalDelegatorName());
            }
            this.jm = JobManager.getInstance(origDelegator, enableJM);
        } catch (GeneralRuntimeException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        // make sure we haven't disabled these features from running
        if (enableJMS) {
            this.jlf = new JmsListenerFactory(this);
        }

        if (enableSvcs) {
            this.runStartupServices();
        }
        serviceDebugMode = "true".equals(UtilProperties.getPropertyValue("service", "servicedispatcher.servicedebugmode", "true"));
    }

    protected ServiceDispatcher(Delegator delegator) {
        this(delegator, enableJM, enableJMS, enableSvcs);
    }

    /**
     * Returns a pre-registered instance of the ServiceDispatcher associated with this delegator.
     * @param delegator the local delegator
     * @return A reference to this global ServiceDispatcher
     */
    public static ServiceDispatcher getInstance(String name, Delegator delegator) {
        ServiceDispatcher sd = getInstance(null, null, delegator);

        if (!sd.containsContext(name)) {
            return null;
        }
        return sd;
    }

    /**
     * Returns an instance of the ServiceDispatcher associated with this delegator and registers the loader.
     * @param name the local dispatcher
     * @param context the context of the local dispatcher
     * @param delegator the local delegator
     * @return A reference to this global ServiceDispatcher
     */
    public static ServiceDispatcher getInstance(String name, DispatchContext context, Delegator delegator) {
        ServiceDispatcher sd;

        String dispatcherKey = delegator != null ? delegator.getDelegatorName() : "null";
        sd = dispatchers.get(dispatcherKey);
        if (sd == null) {
            synchronized (ServiceDispatcher.class) {
                if (Debug.verboseOn()) Debug.logVerbose("[ServiceDispatcher.getInstance] : No instance found (" + dispatcherKey + ").", module);
                sd = dispatchers.get(dispatcherKey);
                if (sd == null) {
                    sd = new ServiceDispatcher(delegator);
                    dispatchers.put(dispatcherKey, sd);
                }
            }
        }

        if (name != null && context != null) {
            sd.register(name, context);
        }
        return sd;
    }

    /**
     * Registers the loader with this ServiceDispatcher
     * @param name the local dispatcher
     * @param context the context of the local dispatcher
     */
    public void register(String name, DispatchContext context) {
        if (Debug.verboseOn()) Debug.logVerbose("Registered dispatcher: " + context.getName(), module);
        this.localContext.put(name, context);
    }

    /**
     * De-Registers the loader with this ServiceDispatcher
     * @param local the LocalDispatcher to de-register
     */
    public void deregister(LocalDispatcher local) {
        if (Debug.infoOn()) Debug.logInfo("De-Registering dispatcher: " + local.getName(), module);
        localContext.remove(local.getName());
         if (localContext.size() == 1) { // 1 == the JMSDispatcher
             try {
                 this.shutdown();
             } catch (GenericServiceException e) {
                 Debug.logError(e, "Trouble shutting down ServiceDispatcher!", module);
             }
         }
    }

    public synchronized void registerCallback(String serviceName, GenericServiceCallback cb) {
        List<GenericServiceCallback> callBackList = callbacks.get(serviceName);
        if (callBackList == null) {
            callBackList = FastList.newInstance();
        }
        callBackList.add(cb);
        callbacks.put(serviceName, callBackList);
    }

    public List<GenericServiceCallback> getCallbacks(String serviceName) {
        return callbacks.get(serviceName);
    }

    /**
     * Run the service synchronously and return the result.
     * @param localName Name of the context to use.
     * @param service Service model object.
     * @param context Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map<String, Object> runSync(String localName, ModelService service, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runSync(localName, service, context, true);
    }

    /**
     * Run the service synchronously and IGNORE the result.
     * @param localName Name of the context to use.
     * @param service Service model object.
     * @param context Map of name, value pairs composing the context.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String localName, ModelService service, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runSync(localName, service, context, false);
    }

    /**
     * Run the service synchronously and return the result.
     * @param localName Name of the context to use.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param validateOut Validate OUT parameters
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, ? extends Object> params, boolean validateOut) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        // check for semaphore and aquire a lock
        ServiceSemaphore lock = null;
        if ("wait".equals(modelService.semaphore) || "fail".equals(modelService.semaphore)) {
            lock = new ServiceSemaphore(delegator, modelService);
            lock.acquire();
        }

        long serviceStartTime = System.currentTimeMillis();
        boolean debugging = checkDebug(modelService, 1, true);
        if (Debug.verboseOn()) {
            Debug.logVerbose("[ServiceDispatcher.runSync] : invoking service " + modelService.name + " [" + modelService.location +
                "/" + modelService.invoke + "] (" + modelService.engineName + ")", module);
        }

        Map<String, Object> context = FastMap.newInstance();
        if (params != null) {
            context.putAll(params);
        }

        // setup the result map and other initial settings
        Map<String, Object> result = FastMap.newInstance();
        boolean isFailure = false;
        boolean isError = false;

        // set up the running service log
        RunningService rs = this.logService(localName, modelService, GenericEngine.SYNC_MODE);

        // get eventMap once for all calls for speed, don't do event calls if it is null
        Map<String, List<ServiceEcaRule>> eventMap = ServiceEcaUtil.getServiceEventMap(modelService.name);

        // check the locale
        Locale locale = this.checkLocale(context);

        // setup the engine and context
        DispatchContext ctx = localContext.get(localName);
        GenericEngine engine = this.getGenericEngine(modelService.engineName);

        // set IN attributes with default-value as applicable
        modelService.updateDefaultValues(context, ModelService.IN_PARAM);

        Map<String, Object> ecaContext = null;

        // for isolated transactions
        Transaction parentTransaction = null;

        // start the transaction
        boolean beganTrans = false;
        try {
            //Debug.logInfo("=========================== " + modelService.name + " 1 tx status =" + TransactionUtil.getStatusString() + ", modelService.requireNewTransaction=" + modelService.requireNewTransaction + ", modelService.useTransaction=" + modelService.useTransaction + ", TransactionUtil.isTransactionInPlace()=" + TransactionUtil.isTransactionInPlace(), module);
            if (modelService.useTransaction) {
                if (TransactionUtil.isTransactionInPlace()) {
                    // if a new transaction is needed, do it here; if not do nothing, just use current tx
                    if (modelService.requireNewTransaction) {
                        parentTransaction = TransactionUtil.suspend();
                        if (TransactionUtil.isTransactionInPlace()) {
                            throw new GenericTransactionException("In service " + modelService.name + " transaction is still in place after suspend, status is " + TransactionUtil.getStatusString());
                        }
                        // now start a new transaction
                        beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                    }
                } else {
                    beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                }
                // enlist for XAResource debugging
                if (beganTrans && TransactionUtil.debugResources) {
                    DebugXaResource dxa = new DebugXaResource(modelService.name);
                    try {
                        dxa.enlist();
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            }

            try {
                int lockRetriesRemaining = LOCK_RETRIES;
                boolean needsLockRetry = false;

                do {
                    lockRetriesRemaining--;

                    // NOTE: general pattern here is to do everything up to the main service call, and retry it all if
                    //needed because those will be part of the same transaction and have been rolled back
                    // TODO: if there is an ECA called async or in a new transaction it won't get rolled back
                    //but will be called again, which means the service may complete multiple times! that would be for
                    //pre-invoke and earlier events only of course


                    // setup global transaction ECA listeners to execute later
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-rollback", ctx, context, result, isError, isFailure);
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-commit", ctx, context, result, isError, isFailure);

                    // pre-auth ECA
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "auth", ctx, context, result, isError, isFailure);

                    // check for pre-auth failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-auth ECA, before auth; isFailure=" + isFailure + ", isError=" + isError, module);

                    context = checkAuth(localName, context, modelService);
                    GenericValue userLogin = (GenericValue) context.get("userLogin");

                    if (modelService.auth && userLogin == null) {
                        throw new ServiceAuthException("User authorization is required for this service: " + modelService.name + modelService.debugInfo());
                    }

                    // now that we have authed, if there is a userLogin, set the EE userIdentifier
                    if (userLogin != null && userLogin.getString("userLoginId") != null) {
                        GenericDelegator.pushUserIdentifier(userLogin.getString("userLoginId"));
                    }

                    // pre-validate ECA
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "in-validate", ctx, context, result, isError, isFailure);

                    // check for pre-validate failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-in-validate ECA, before in-validate; isFailure=" + isFailure + ", isError=" + isError, module);

                    // validate the context
                    if (modelService.validate && !isError && !isFailure) {
                        try {
                            modelService.validate(context, ModelService.IN_PARAM, locale);
                        } catch (ServiceValidationException e) {
                            Debug.logError(e, "Incoming context (in runSync : " + modelService.name + ") does not match expected requirements", module);
                            throw e;
                        }
                    }

                    // pre-invoke ECA
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "invoke", ctx, context, result, isError, isFailure);

                    // check for pre-invoke failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-invoke ECA, before invoke; isFailure=" + isFailure + ", isError=" + isError, module);

                    // ===== invoke the service =====
                    if (!isError && !isFailure) {
                        Map<String, Object> invokeResult = null;
                        if (serviceDebugMode) {
                            invokeResult = modelService.invoker.runSync(localName, engine, context);
                            modelService.invoker.sendCallbacks(engine, context, invokeResult, null, GenericEngine.SYNC_MODE);
                        } else {
                            invokeResult = engine.runSync(localName, modelService, context);
                            engine.sendCallbacks(modelService, context, invokeResult, GenericEngine.SYNC_MODE);
                        }
                        if (invokeResult != null) {
                            result.putAll(invokeResult);
                        } else {
                            Debug.logWarning("Service (in runSync : " + modelService.name + ") returns null result", module);
                        }
                    }

                    // re-check the errors/failures
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] invoke; isFailure=" + isFailure + ", isError=" + isError, module);

                    if (beganTrans) {
                        // crazy stuff here: see if there was a deadlock or other such error and if so retry... which we can ONLY do if we own the transaction!

                        String errMsg = ServiceUtil.getErrorMessage(result);

                        // look for the string DEADLOCK in an upper-cased error message; tested on: Derby, MySQL
                        // - Derby 10.2.2.0 deadlock string: "A lock could not be obtained due to a deadlock"
                        // - MySQL ? deadlock string: "Deadlock found when trying to get lock; try restarting transaction"
                        // - Postgres ? deadlock string: TODO
                        // - Other ? deadlock string: TODO
                        // TODO need testing in other databases because they all return different error messages for this!

                        // NOTE DEJ20070908 are there other things we need to check? I don't think so because these will
                        //be Entity Engine errors that will be caught and come back in an error message... IFF the
                        //service is written to not ignore it of course!
                        if (errMsg != null && errMsg.toUpperCase().indexOf("DEADLOCK") >= 0) {
                            // it's a deadlock! retry...
                            String retryMsg = "RETRYING SERVICE [" + modelService.name + "]: Deadlock error found in message [" + errMsg + "]; retry [" + (LOCK_RETRIES - lockRetriesRemaining) + "] of [" + LOCK_RETRIES + "]";

                            // make sure the old transaction is rolled back, and then start a new one

                            // if there is an exception in these things, let the big overall thing handle it
                            TransactionUtil.rollback(beganTrans, retryMsg, null);

                            beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                            // enlist for XAResource debugging
                            if (beganTrans && TransactionUtil.debugResources) {
                                DebugXaResource dxa = new DebugXaResource(modelService.name);
                                try {
                                    dxa.enlist();
                                } catch (Exception e) {
                                    Debug.logError(e, module);
                                }
                            }

                            if (!beganTrans) {
                                // just log and let things roll through, will be considered an error and ECAs, etc will run according to that
                                Debug.logError("After rollback attempt for lock retry did not begin a new transaction!", module);
                            } else {
                                // deadlocks can be resolved by retring immediately as conflicting operations in the other thread will have cleared
                                needsLockRetry = true;

                                // reset state variables
                                result = FastMap.newInstance();
                                isFailure = false;
                                isError = false;

                                Debug.logWarning(retryMsg, module);
                            }

                            // look for lock wait timeout error, retry in a different way by running after the parent transaction finishes, ie attach to parent tx
                            // - Derby 10.2.2.0 lock wait timeout string: "A lock could not be obtained within the time requested"
                            // - MySQL ? lock wait timeout string: "Lock wait timeout exceeded; try restarting transaction"
                            if (errMsg != null && (errMsg.indexOf("A lock could not be obtained within the time requested") >= 0 ||
                                    errMsg.indexOf("Lock wait timeout exceeded") >= 0)) {
                                // TODO: add to run after parent tx
                            }
                        }
                    }
                } while (needsLockRetry && lockRetriesRemaining > 0);

                // create a new context with the results to pass to ECA services; necessary because caller may reuse this context
                ecaContext = FastMap.newInstance();
                ecaContext.putAll(context);
                // copy all results: don't worry parameters that aren't allowed won't be passed to the ECA services
                ecaContext.putAll(result);

                // setup default OUT values
                modelService.updateDefaultValues(context, ModelService.OUT_PARAM);

                // validate the result
                if (modelService.validate && validateOut) {
                    // pre-out-validate ECA
                    if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "out-validate", ctx, ecaContext, result, isError, isFailure);
                    try {
                        modelService.validate(result, ModelService.OUT_PARAM, locale);
                    } catch (ServiceValidationException e) {
                        Debug.logError(e, "Outgoing result (in runSync : " + modelService.name + ") does not match expected requirements", module);
                        throw e;
                    }
                }

                // pre-commit ECA
                if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "commit", ctx, ecaContext, result, isError, isFailure);

                // check for pre-commit failure/errors
                isFailure = ServiceUtil.isFailure(result);
                isError = ServiceUtil.isError(result);

                // global-commit-post-run ECA, like global-commit but gets the context after the service is run
                if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-commit-post-run", ctx, ecaContext, result, isError, isFailure);

                // check for failure and log on info level; this is used for debugging
                if (isFailure) {
                    Debug.logWarning("Service Failure [" + modelService.name + "]: " + ServiceUtil.getErrorMessage(result), module);
                }
            } catch (Throwable t) {
                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + modelService.name, "Sync service failed...", module);
                }
                String errMsg = "Service [" + modelService.name + "] threw an unexpected exception/error";
                Debug.logError(t, errMsg, module);
                if (serviceDebugMode) {
                    modelService.invoker.sendCallbacks(engine, context, null, t, GenericEngine.SYNC_MODE);
                } else {
                    engine.sendCallbacks(modelService, context, t, GenericEngine.SYNC_MODE);
                }
                try {
                    TransactionUtil.rollback(beganTrans, errMsg, t);
                } catch (GenericTransactionException te) {
                    Debug.logError(te, "Cannot rollback transaction", module);
                }
                checkDebug(modelService, 0, debugging);
                rs.setEndStamp();
                if (t instanceof ServiceAuthException) {
                    throw (ServiceAuthException) t;
                } else if (t instanceof ServiceValidationException) {
                    throw (ServiceValidationException) t;
                } else if (t instanceof GenericServiceException) {
                    throw (GenericServiceException) t;
                } else {
                    throw new GenericServiceException("Service [" + modelService.name + "] Failed" + modelService.debugInfo() , t);
                }
            } finally {
                // if there was an error, rollback transaction, otherwise commit
                if (isError) {
                    String errMsg = "Error in Service [" + modelService.name + "]: " + ServiceUtil.getErrorMessage(result);
                    Debug.logError(errMsg, module);

                    // rollback the transaction
                    try {
                        TransactionUtil.rollback(beganTrans, errMsg, null);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not rollback transaction: " + e.toString(), module);
                    }
                } else {
                    // commit the transaction
                    try {
                        TransactionUtil.commit(beganTrans);
                    } catch (GenericTransactionException e) {
                        String errMsg = "Could not commit transaction for service [" + modelService.name + "] call";
                        Debug.logError(e, errMsg, module);
                        if (e.getMessage() != null) {
                            errMsg = errMsg + ": " + e.getMessage();
                        }
                        throw new GenericServiceException(errMsg);
                    }
                }

                // call notifications -- event is determined from the result (success, error, fail)
                modelService.evalNotifications(this.getLocalContext(localName), context, result);

                // clear out the EE userIdentifier
                GenericDelegator.popUserIdentifier();
            }
        } catch (GenericTransactionException te) {
            Debug.logError(te, "Problems with the transaction", module);
            throw new GenericServiceException("Problems with the transaction.", te.getNested());
        } finally {
            // release the semaphore lock
            if (lock != null) {
                lock.release();
            }

            // resume the parent transaction
            if (parentTransaction != null) {
                try {
                    TransactionUtil.resume(parentTransaction);
                } catch (GenericTransactionException ite) {
                    Debug.logWarning(ite, "Transaction error, not resumed", module);
                    throw new GenericServiceException("Resume transaction exception, see logs");
                }
            }
        }

        // pre-return ECA
        if (eventMap != null) ServiceEcaUtil.evalRules(modelService.name, eventMap, "return", ctx, ecaContext, result, isError, isFailure);

        checkDebug(modelService, 0, debugging);
        rs.setEndStamp();

        long timeToRun = System.currentTimeMillis() - serviceStartTime;
        if (Debug.timingOn() && timeToRun > 50) {
            Debug.logTiming("Sync service [" + localName + "/" + modelService.name + "] finished in [" + timeToRun + "] milliseconds", module);
        } else if (timeToRun > 200) {
            Debug.logInfo("Sync service [" + localName + "/" + modelService.name + "] finished in [" + timeToRun + "] milliseconds", module);
        }

        return result;
    }

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * @param localName Name of the context to use.
     * @param service Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService service, Map<String, ? extends Object> params, GenericRequester requester, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        if (Debug.timingOn()) {
            UtilTimer.timerLog(localName + " / " + service.name, "ASync service started...", module);
        }
        boolean debugging = checkDebug(service, 1, true);
        if (Debug.verboseOn()) {
            Debug.logVerbose("[ServiceDispatcher.runAsync] : preparing service " + service.name + " [" + service.location + "/" + service.invoke +
                "] (" + service.engineName + ")", module);
        }

        Map<String, Object> context = FastMap.newInstance();
        if (params != null) {
            context.putAll(params);
        }
        // setup the result map
        Map<String, Object> result = FastMap.newInstance();
        boolean isFailure = false;
        boolean isError = false;

        // set up the running service log
        this.logService(localName, service, GenericEngine.ASYNC_MODE);

        // check the locale
        Locale locale = this.checkLocale(context);

        // setup the engine and context
        DispatchContext ctx = localContext.get(localName);
        GenericEngine engine = this.getGenericEngine(service.engineName);

        // for isolated transactions
        Transaction parentTransaction = null;
        // start the transaction
        boolean beganTrans = false;

        try {
            if (service.useTransaction) {
                if (TransactionUtil.isTransactionInPlace()) {
                    // if a new transaction is needed, do it here; if not do nothing, just use current tx
                    if (service.requireNewTransaction) {
                        parentTransaction = TransactionUtil.suspend();
                        // now start a new transaction
                        beganTrans = TransactionUtil.begin(service.transactionTimeout);
                    }
                } else {
                    beganTrans = TransactionUtil.begin(service.transactionTimeout);
                }
                // enlist for XAResource debugging
                if (beganTrans && TransactionUtil.debugResources) {
                    DebugXaResource dxa = new DebugXaResource(service.name);
                    try {
                        dxa.enlist();
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            }

            try {
                // get eventMap once for all calls for speed, don't do event calls if it is null
                Map<String, List<ServiceEcaRule>> eventMap = ServiceEcaUtil.getServiceEventMap(service.name);

                // pre-auth ECA
                if (eventMap != null) ServiceEcaUtil.evalRules(service.name, eventMap, "auth", ctx, context, result, isError, isFailure);

                context = checkAuth(localName, context, service);
                Object userLogin = context.get("userLogin");

                if (service.auth && userLogin == null) {
                    throw new ServiceAuthException("User authorization is required for this service: " + service.name + service.debugInfo());
                }

                // pre-validate ECA
                if (eventMap != null) ServiceEcaUtil.evalRules(service.name, eventMap, "in-validate", ctx, context, result, isError, isFailure);

                // check for pre-validate failure/errors
                isFailure = ModelService.RESPOND_FAIL.equals(result.get(ModelService.RESPONSE_MESSAGE));
                isError = ModelService.RESPOND_ERROR.equals(result.get(ModelService.RESPONSE_MESSAGE));

                // validate the context
                if (service.validate && !isError && !isFailure) {
                    try {
                        service.validate(context, ModelService.IN_PARAM, locale);
                    } catch (ServiceValidationException e) {
                        Debug.logError(e, "Incoming service context (in runAsync: " + service.name + ") does not match expected requirements", module);
                        throw e;
                    }
                }

                // run the service
                if (!isError && !isFailure) {
                    if (serviceDebugMode) {
                        service.invoker.runAsync(localName, engine, context, requester, persist);
                        service.invoker.sendCallbacks(engine, context, null, null, GenericEngine.ASYNC_MODE);
                    } else {
                        if (requester != null) {
                            engine.runAsync(localName, service, context, requester, persist);
                        } else {
                            engine.runAsync(localName, service, context, persist);
                        }
                        engine.sendCallbacks(service, context, GenericEngine.ASYNC_MODE);
                    }
                }

                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + service.name, "ASync service finished...", module);
                }
                checkDebug(service, 0, debugging);
            } catch (Throwable t) {
                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + service.name, "ASync service failed...", module);
                }
                String errMsg = "Service [" + service.name + "] threw an unexpected exception/error";
                Debug.logError(t, errMsg, module);
                if (serviceDebugMode) {
                    service.invoker.sendCallbacks(engine, context, null, t, GenericEngine.ASYNC_MODE);
                } else {
                    engine.sendCallbacks(service, context, t, GenericEngine.ASYNC_MODE);
                }
                try {
                    TransactionUtil.rollback(beganTrans, errMsg, t);
                } catch (GenericTransactionException te) {
                    Debug.logError(te, "Cannot rollback transaction", module);
                }
                checkDebug(service, 0, debugging);
                if (t instanceof ServiceAuthException) {
                    throw (ServiceAuthException) t;
                } else if (t instanceof ServiceValidationException) {
                    throw (ServiceValidationException) t;
                } else if (t instanceof GenericServiceException) {
                    throw (GenericServiceException) t;
                } else {
                    throw new GenericServiceException("Service [" + service.name + "] Failed" + service.debugInfo() , t);
                }
            } finally {
                // always try to commit the transaction since we don't know in this case if its was an error or not
                try {
                    TransactionUtil.commit(beganTrans);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Could not commit transaction", module);
                    throw new GenericServiceException("Commit transaction failed");
                }
            }
        } catch (GenericTransactionException se) {
            Debug.logError(se, "Problems with the transaction", module);
            throw new GenericServiceException("Problems with the transaction: " + se.getMessage() + "; See logs for more detail");
        } finally {
            // resume the parent transaction
            if (parentTransaction != null) {
                try {
                    TransactionUtil.resume(parentTransaction);
                } catch (GenericTransactionException ise) {
                    Debug.logError(ise, "Trouble resuming parent transaction", module);
                    throw new GenericServiceException("Resume transaction exception: " + ise.getMessage() + "; See logs for more detail");
                }
            }
        }
    }

    /**
     * Run the service asynchronously and IGNORE the result.
     * @param localName Name of the context to use.
     * @param service Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService service, Map<String, ? extends Object> context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        this.runAsync(localName, service, context, null, persist);
    }

    /**
     * Gets the GenericEngine instance that corresponds to the given name
     * @param engineName Name of the engine
     * @return GenericEngine instance that corresponds to the engineName
     */
    public GenericEngine getGenericEngine(String engineName) throws GenericServiceException {
        return factory.getGenericEngine(engineName);
    }

    /**
     * Gets the JobManager associated with this dispatcher
     * @return JobManager that is associated with this dispatcher
     */
    public JobManager getJobManager() {
        return this.jm;
    }

    /**
     * Gets the JmsListenerFactory which holds the message listeners.
     * @return JmsListenerFactory
     */
    public JmsListenerFactory getJMSListenerFactory() {
        return this.jlf;
    }

    /**
     * Gets the Delegator associated with this dispatcher
     * @return Delegator associated with this dispatcher
     */
    public Delegator getDelegator() {
        return this.delegator;
    }

    /**
     * Gets the Authorization object associated with this dispatcher
     * @return Authorization object associated with this dispatcher
     */
    public Authorization getAuthorization() {
        return this.authz;
    }

    /**
     * Gets the Security object associated with this dispatcher
     * @return Security object associated with this dispatcher
     */
    @Deprecated
    public Security getSecurity() {
        return this.security;
    }

    /**
     * Gets the local context from a name
     * @param name of the context to find.
     */
    public DispatchContext getLocalContext(String name) {
        return localContext.get(name);
    }

    /**
     * Gets the local dispatcher from a name
     * @param name of the LocalDispatcher to find.
     * @return LocalDispatcher matching the loader name
     */
    public LocalDispatcher getLocalDispatcher(String name) {
        return localContext.get(name).getDispatcher();
    }

    /**
     * Test if this dispatcher instance contains the local context.
     * @param name of the local context
     * @return true if the local context is found in this dispatcher.
     */
    public boolean containsContext(String name) {
        return localContext.containsKey(name);
    }

    protected void shutdown() throws GenericServiceException {
        Debug.logImportant("Shutting down the service engine...", module);
        // shutdown JMS listeners
        jlf.closeListeners();
        // shutdown the job scheduler
        jm.shutdown();
    }

    // checks if parameters were passed for authentication
    private Map<String, Object> checkAuth(String localName, Map<String, Object> context, ModelService origService) throws ServiceAuthException, GenericServiceException {
        String service = ServiceConfigUtil.getElementAttr("authorization", "service-name");

        if (service == null) {
            throw new GenericServiceException("No Authentication Service Defined");
        }
        if (service.equals(origService.name)) {
            // manually calling the auth service, don't continue...
            return context;
        }

        if (UtilValidate.isNotEmpty(context.get("login.username"))) {
            // check for a username/password, if there log the user in and make the userLogin object
            String username = (String) context.get("login.username");

            if (UtilValidate.isNotEmpty(context.get("login.password"))) {
                String password = (String) context.get("login.password");

                context.put("userLogin", getLoginObject(service, localName, username, password, (Locale) context.get("locale")));
                context.remove("login.password");
            } else {
                context.put("userLogin", getLoginObject(service, localName, username, null, (Locale) context.get("locale")));
            }
            context.remove("login.username");
        } else {
            // if a userLogin object is there, make sure the given username/password exists in our local database
            GenericValue userLogin = (GenericValue) context.get("userLogin");

            if (userLogin != null) {
                // Because of encrypted passwords we can't just pass in the encrypted version of the password from the data, so we'll do something different and not run the userLogin service...

                //The old way: GenericValue newUserLogin = getLoginObject(service, localName, userLogin.getString("userLoginId"), userLogin.getString("currentPassword"), (Locale) context.get("locale"));
                GenericValue newUserLogin = null;
                try {
                    newUserLogin = this.getDelegator().findByPrimaryKeyCache("UserLogin", "userLoginId", userLogin.get("userLoginId"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error looking up service authentication UserLogin: " + e.toString(), module);
                    // leave newUserLogin null, will be handled below
                }

                if (newUserLogin == null) {
                    // uh oh, couldn't validate that one...
                    // we'll have to remove it from the incoming context which will cause an auth error later if auth is required
                    Debug.logInfo("Service auth failed for userLoginId [" + userLogin.get("userLoginId") + "] because UserLogin record not found.", module);
                    context.remove("userLogin");
                } else if (newUserLogin.getString("currentPassword") != null && !newUserLogin.getString("currentPassword").equals(userLogin.getString("currentPassword"))) {
                    // passwords didn't match, remove the userLogin for failed auth
                    Debug.logInfo("Service auth failed for userLoginId [" + userLogin.get("userLoginId") + "] because UserLogin record currentPassword fields did not match; note that the UserLogin object passed into a service may need to have the currentPassword encrypted.", module);
                    context.remove("userLogin");
                }
            }
        }

        // evaluate permissions for the service or throw exception if fail.
        DispatchContext dctx = this.getLocalContext(localName);
        if (UtilValidate.isNotEmpty(origService.permissionServiceName)) {
            Map<String, Object> permResp = origService.evalPermission(dctx, context);
            Boolean hasPermission = (Boolean) permResp.get("hasPermission");
            if (hasPermission == null) {
                throw new ServiceAuthException("ERROR: the permission-service [" + origService.permissionServiceName + "] did not return a result. Not running the service [" + origService.name + "]");
            }
            if (hasPermission.booleanValue()) {
                context.putAll(permResp);
                context = origService.makeValid(context, ModelService.IN_PARAM);
            } else {
                String message = (String) permResp.get("failMessage");
                if (UtilValidate.isEmpty(message)) {
                    message = ServiceUtil.getErrorMessage(permResp);
                }
                if (UtilValidate.isEmpty(message)) {
                    message = "You do not have permission to invoke the service [" + origService.name + "]";
                }
                throw new ServiceAuthException(message);
            }
        } else {
            if (!origService.evalPermissions(dctx, context)) {
                throw new ServiceAuthException("You do not have permission to invoke the service [" + origService.name + "]");
            }
        }

        return context;
    }

    // gets a value object from name/password pair
    private GenericValue getLoginObject(String service, String localName, String username, String password, Locale locale) throws GenericServiceException {
        Map<String, Object> context = FastMap.newInstance();
        context.putAll(UtilMisc.toMap("login.username", username, "login.password", password, "isServiceAuth", true, "locale", locale));

        if (Debug.verboseOn()) Debug.logVerbose("[ServiceDispathcer.authenticate] : Invoking UserLogin Service", module);

        // get the dispatch context and service model
        DispatchContext dctx = getLocalContext(localName);
        ModelService model = dctx.getModelService(service);

        // get the service engine
        GenericEngine engine = getGenericEngine(model.engineName);

        // invoke the service and get the UserLogin value object
        Map<String, Object> result = engine.runSync(localName, model, context);
        return (GenericValue) result.get("userLogin");
    }

    // checks the locale object in the context
    private Locale checkLocale(Map<String, Object> context) {
        Object locale = context.get("locale");
        Locale newLocale = null;

        if (locale != null) {
            if (locale instanceof Locale) {
                return (Locale) locale;
            } else if (locale instanceof String) {
                // en_US = lang_COUNTRY
                newLocale = UtilMisc.parseLocale((String) locale);
            }
        }

        if (newLocale == null) {
            newLocale = Locale.getDefault();
        }
        context.put("locale", newLocale);
        return newLocale;
    }

    // mode 1 = beginning (turn on) mode 0 = end (turn off)
    private boolean checkDebug(ModelService model, int mode, boolean enable) {
        boolean debugOn = Debug.verboseOn();
        switch (mode) {
            case 0:
                if (model.debug && enable && debugOn) {
                    // turn it off
                    Debug.set(Debug.VERBOSE, false);
                    Debug.logInfo("Verbose logging turned OFF", module);
                    return true;
                }
                break;
            case 1:
                if (model.debug && enable && !debugOn) {
                    // turn it on
                    Debug.set(Debug.VERBOSE, true);
                    Debug.logInfo("Verbose logging turned ON", module);
                    return true;
                }
                break;
            default:
                Debug.logError("Invalid mode for checkDebug should be (0 or 1)", module);
        }
        return false;
    }

    // run startup services
    private synchronized int runStartupServices() {
        if (jm == null) return 0;

        Element root;
        try {
            root = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return 0;
        }

        int servicesScheduled = 0;
        List<? extends Element> startupServices = UtilXml.childElementList(root, "startup-service");
        if (UtilValidate.isNotEmpty(startupServices)) {
            for (Element ss: startupServices) {
                String serviceName = ss.getAttribute("name");
                String runtimeDataId = ss.getAttribute("runtime-data-id");
                String delayStr = ss.getAttribute("runtime-delay");
                String sendToPool = ss.getAttribute("run-in-pool");
                if (UtilValidate.isEmpty(sendToPool)) {
                    sendToPool = ServiceConfigUtil.getSendPool();
                }

                long runtimeDelay;
                try {
                    runtimeDelay = Long.parseLong(delayStr);
                } catch (Exception e) {
                    Debug.logError(e, "Unable to parse runtime-delay value; using 0", module);
                    runtimeDelay = 0;
                }

                // current time + 1 sec delay + extended delay
                long runtime = System.currentTimeMillis() + 1000 + runtimeDelay;
                try {
                    jm.schedule(sendToPool, serviceName, runtimeDataId, runtime);
                } catch (JobManagerException e) {
                    Debug.logError(e, "Unable to schedule service [" + serviceName + "]", module);
                }
            }
        }

        return servicesScheduled;
    }

    private RunningService logService(String localName, ModelService modelService, int mode) {
        // set up the running service log
        RunningService rs = new RunningService(localName, modelService, mode);
        if (runLog == null) {
            Debug.logWarning("LRUMap is null", module);
        } else {
            synchronized(runLog) {
                try {
                    runLog.put(rs, this);
                } catch (Throwable t) {
                    Debug.logWarning("LRUMap problem; resetting LRU [" + runLog.size() + "]", module);
                    runLog.clear();
                    try {
                        runLog.put(rs, this);
                    } catch (Throwable t2) {
                        Debug.logError(t2, "Unable to put() in reset LRU map!", module);
                    }
                }
            }
        }
        return rs;
    }

    /**
     * Enabled/Disables the Job Manager/Scheduler globally
     * (this will not effect any dispatchers already running)
     * @param enable
     */
    public static void enableJM(boolean enable) {
        ServiceDispatcher.enableJM = enable;
    }

    /**
     * Enabled/Disables the JMS listeners globally
     * (this will not effect any dispatchers already running)
     * @param enable
     */
    public static void enableJMS(boolean enable) {
        ServiceDispatcher.enableJMS = enable;
    }

    /**
     * Enabled/Disables the startup services globally
     * (this will not effect any dispatchers already running)
     * @param enable
     */
    public static void enableSvcs(boolean enable) {
        ServiceDispatcher.enableSvcs = enable;
    }

    public static Map<RunningService, ServiceDispatcher> getServiceLogMap() {
        return runLog;
    }

}
