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
package org.ofbiz.service.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * Generic Services Remote Dispatcher Implementation
 */
public class RemoteDispatcherImpl extends UnicastRemoteObject implements RemoteDispatcher {

    public static final String module = RemoteDispatcherImpl.class.getName();
    private static final boolean exportAll = false;

    protected LocalDispatcher dispatcher = null;

    public RemoteDispatcherImpl(LocalDispatcher dispatcher, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(0, csf, ssf);
        this.dispatcher = dispatcher;
    }

    // RemoteDispatcher methods

    public Map runSync(String serviceName, Map context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runSync(serviceName, context);
    }

    public Map runSync(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runSync(serviceName, context, transactionTimeout, requireNewTransaction);
    }

    public void runSyncIgnore(String serviceName, Map context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runSyncIgnore(serviceName, context);
    }

    public void runSyncIgnore(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runSyncIgnore(serviceName, context, transactionTimeout, requireNewTransaction);
    }

    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester, persist, transactionTimeout, requireNewTransaction);
    }

    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester, persist);
    }

    public void runAsync(String serviceName, Map context, GenericRequester requester) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, requester);
    }

    public void runAsync(String serviceName, Map context, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context, persist);
    }

    public void runAsync(String serviceName, Map context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.runAsync(serviceName, context);
    }

    public GenericResultWaiter runAsyncWait(String serviceName, Map context, boolean persist) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runAsyncWait(serviceName, context, persist);
    }

    public GenericResultWaiter runAsyncWait(String serviceName, Map context) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        return dispatcher.runAsyncWait(serviceName, context);
    }

    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count, long endTime) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, count, endTime);
    }

    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, count);
    }

    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, long endTime) throws GenericServiceException, RemoteException {
        this.checkExportFlag(serviceName);
        dispatcher.schedule(serviceName, context, startTime, frequency, interval, endTime);
    }

    public void schedule(String serviceName, Map context, long startTime) throws GenericServiceException, RemoteException {
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
