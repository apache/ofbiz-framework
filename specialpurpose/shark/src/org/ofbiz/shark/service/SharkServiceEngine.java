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
package org.ofbiz.shark.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.Transaction;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.AbstractEngine;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.shark.requester.SimpleRequester;

import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfmodel.*;
import org.enhydra.shark.api.client.wfservice.AdminInterface;
import org.enhydra.shark.api.client.wfservice.ConnectFailed;
import org.enhydra.shark.api.client.wfservice.ExecutionAdministration;
import org.enhydra.shark.api.client.wfservice.NotConnected;

/**
 * Shark Service Engine
 */
public class SharkServiceEngine extends AbstractEngine {

    public static final String module = SharkServiceEngine.class.getName();

    public SharkServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * Run the service synchronously and return the result.
     *
     * @param localName    Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context      Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws org.ofbiz.service.GenericServiceException
     *
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        GenericResultWaiter waiter = new GenericResultWaiter();
        this.runAsync(localName, modelService, context, waiter, false);
        return waiter.waitForResult();
    }

    /**
     * Run the service synchronously and IGNORE the result.
     *
     * @param localName    Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context      Map of name, value pairs composing the context.
     * @throws org.ofbiz.service.GenericServiceException
     *
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        this.runSync(localName, modelService, context);
    }

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     *
     * @param localName    Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context      Map of name, value pairs composing the context.
     * @param requester    Object implementing GenericRequester interface which will receive the result.
     * @param persist      True for store/run; False for run.
     * @throws org.ofbiz.service.GenericServiceException
     *
     */
    public void runAsync(String localName, ModelService modelService, Map context, GenericRequester requester, boolean persist) throws GenericServiceException {
        this.runWf(modelService, context, requester);
    }

    /**
     * Run the service asynchronously and IGNORE the result.
     *
     * @param localName    Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context      Map of name, value pairs composing the context.
     * @param persist      True for store/run; False for run.
     * @throws org.ofbiz.service.GenericServiceException
     *
     */
    public void runAsync(String localName, ModelService modelService, Map context, boolean persist) throws GenericServiceException {
        this.runAsync(localName, modelService, context, new GenericResultWaiter(), persist);
    }

    private GenericRequester runWf(ModelService model, Map context, GenericRequester waiter) throws GenericServiceException {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            userLogin = SharkContainer.getAdminUser();
        }

        AdminInterface admin = SharkContainer.getAdminInterface();
        ExecutionAdministration exec = admin.getExecutionAdministration();

        boolean beganTrans = false;
        boolean hasError = false;
        Transaction trans = null;

        try {
            beganTrans = TransactionUtil.begin();
            if (!beganTrans) {
                trans = TransactionUtil.suspend();
                beganTrans = TransactionUtil.begin();
            }

            try {
                // connect to admin API
                try {
                    exec.connect(userLogin.getString("userLoginId"), userLogin.getString("currentPassword"), null, null);
                } catch (BaseException e) {
                    throw new GenericServiceException(e);
                } catch (ConnectFailed e) {
                    throw new GenericServiceException(e);
                }

                try {
                    // create the requester
                    WfRequester req = new SimpleRequester(userLogin, model, waiter);
                    WfProcessMgr mgr = null;
                    String location = this.getLocation(model);
                    String version = null;

                    // locate packageId::version
                    if (location.indexOf("::") != -1) {
                        List splitList = StringUtil.split(location, "::");
                        location = (String) splitList.get(0);
                        version = (String) splitList.get(1);
                    }

                    // obtain the process manager
                    try {
                        if (version == null) {
                            mgr = exec.getProcessMgr(location, model.invoke);
                        } else {
                            mgr = exec.getProcessMgr(location, version, model.invoke);
                        }
                    } catch (BaseException e) {
                        throw new GenericServiceException(e);
                    } catch (NotConnected e) {
                        throw new GenericServiceException(e);
                    }

                    // make sure the manager exists
                    if (mgr == null) {
                        throw new GenericServiceException("Unable to obtain Process Manager for : " + this.getLocation(model) + " / " + model.invoke);
                    }

                    // create the process instance
                    WfProcess proc = null;
                    try {
                        proc = mgr.create_process(req);
                    } catch (BaseException e) {
                        throw new GenericServiceException(e);
                    } catch (NotEnabled e) {
                        throw new GenericServiceException(e);
                    } catch (InvalidRequester e) {
                        throw new GenericServiceException(e);
                    } catch (RequesterRequired e) {
                        throw new GenericServiceException(e);
                    }

                    Map contextSig = null;
                    try {
                        contextSig = mgr.context_signature();
                    } catch (BaseException e) {
                        throw new GenericServiceException(e);
                    }

                    if (contextSig != null) {
                        Iterator sigKeys = contextSig.keySet().iterator();
                        Map formalParams = new HashMap();
                        while (sigKeys.hasNext()) {
                            String key = (String) sigKeys.next();
                            formalParams.put(key, context.get(key));
                        }

                        // set the initial WRD
                        try {
                            proc.set_process_context(formalParams);
                        } catch (BaseException e) {
                            throw new GenericServiceException(e);
                        } catch (InvalidData e) {
                            throw new GenericServiceException(e);
                        } catch (UpdateNotAllowed e) {
                            throw new GenericServiceException(e);
                        }
                    }

                    // start the process
                    try {
                        proc.start();
                    } catch (BaseException e) {
                        throw new GenericServiceException(e);
                    } catch (CannotStart e) {
                        throw new GenericServiceException(e);
                    } catch (AlreadyRunning e) {
                        throw new GenericServiceException(e);
                    }
                } catch (GenericServiceException e) {
                    throw e;
                } finally {
                    // disconnect from admin API
                    try {
                        exec.disconnect();
                    } catch (NotConnected e) {
                        throw new GenericServiceException(e);
                    } catch (BaseException e) {
                        throw new GenericServiceException(e);
                    }
                }
            } catch (GenericServiceException e) {
                hasError = true;
                throw e;
            } finally {
                if (hasError) {
                    try {
                        TransactionUtil.rollback(beganTrans, "Transaction rollback from Shark", null);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not rollback transaction", module);
                    }
                } else {
                    try {
                        TransactionUtil.commit(beganTrans);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not commit transaction", module);
                        throw new GenericServiceException("Commit transaction failed");
                    }
                }
            }
        } catch (GenericTransactionException e) {
            throw new GenericServiceException(e);
        } finally {
            if (trans != null) {
                try {
                    TransactionUtil.resume(trans);
                } catch (GenericTransactionException e) {
                    throw new GenericServiceException(e);
                }
            }
        }

        return waiter;
    }
}
