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
package org.apache.ofbiz.service.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.GenericResultWaiter;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;

/**
 * Generic Services Remote Dispatcher Implementation
 */
@SuppressWarnings("serial")
public class RemoteDispatcherImpl extends UnicastRemoteObject implements RemoteDispatcher {

    public static final String module = RemoteDispatcherImpl.class.getName();
    private static boolean exportAll = false;

    protected LocalDispatcher dispatcher = null;

    public RemoteDispatcherImpl(LocalDispatcher dispatcher, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(0, csf, ssf);
        this.dispatcher = dispatcher;
        Delegator delegator = dispatcher.getDelegator();
        exportAll = "true".equals(EntityUtilProperties.getPropertyValue("service", "remotedispatcher.exportall", "false", delegator));
    }

    // RemoteDispatcher methods

    @Override
    public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runSync(serviceName, context);
    }

    @Override
    public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runSync(serviceName, context, transactionTimeout, requireNewTransaction);
    }

    @Override
    public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runSyncIgnore(serviceName, context);
    }

    @Override
    public void runSyncIgnore(String serviceName, Map<String, ? extends Object> context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runSyncIgnore(serviceName, context, transactionTimeout, requireNewTransaction);
    }

    @Override
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester, persist, transactionTimeout, requireNewTransaction);
    }

    @Override
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester, persist);
    }

    @Override
    public void runAsync(String serviceName, Map<String, ? extends Object> context, GenericRequester requester) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester);
    }

    @Override
    public void runAsync(String serviceName, Map<String, ? extends Object> context, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, persist);
    }

    @Override
    public void runAsync(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context);
    }

    @Override
    public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runAsyncWait(serviceName, context, persist);
    }

    @Override
    public GenericResultWaiter runAsyncWait(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runAsyncWait(serviceName, context);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count, long endTime) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, count, endTime);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, int count) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, count);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime, int frequency, int interval, long endTime) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, endTime);
    }

    @Override
    public void schedule(String serviceName, Map<String, ? extends Object> context, long startTime) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime);
    }

    public void deregister() {
        dispatcher.deregister();
    }

    protected void checkExportFlag(String serviceName) throws GenericServiceException {
        ModelService model = dispatcher.getDispatchContext().getModelService(serviceName);
        if (!model.export && !exportAll) {
            // TODO: make this log on the server rather than the client
            //Debug.logWarning("Attempt to invoke a non-exported service: " + serviceName, module);
            throw new GenericServiceException("Cannot find requested service");
        }
    }

}
