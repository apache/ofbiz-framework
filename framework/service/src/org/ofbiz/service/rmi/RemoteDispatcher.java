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

import java.util.Map;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.GenericServiceException;

/**
 * Generic Services Remote Dispatcher
 */
public interface RemoteDispatcher extends Remote {
    
    /**
     * Run the service synchronously and return the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public Map runSync(String serviceName, Map context) throws GenericServiceException, RemoteException;

    /**
     * Run the service synchronously with a specified timeout and return the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @return Map of name, value pairs composing the result.
     * @throws GenericServiceException
     */
    public Map runSync(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException;

    /**
     * Run the service synchronously and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void runSyncIgnore(String serviceName, Map context) throws GenericServiceException, RemoteException;

    /**
     * Run the service synchronously with a specified timeout and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void runAsync(String serviceName, Map context, boolean persist) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously and IGNORE the result. This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void runAsync(String serviceName, Map context) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @return A new GenericRequester object.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context, boolean persist) throws GenericServiceException, RemoteException;

    /**
     * Run the service asynchronously. This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @return A new GenericRequester object.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context) throws GenericServiceException, RemoteException;

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param count The number of times to repeat.
     * @param endTime The time in milliseconds the service should expire
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count, long endTime) throws GenericServiceException, RemoteException;
                
    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param count The number of times to repeat.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count) throws GenericServiceException, RemoteException;
   
    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param endTime The time in milliseconds the service should expire
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, long endTime) throws GenericServiceException, RemoteException;
             
    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @throws GenericServiceException
     * @throws RemoteException
     */
    public void schedule(String serviceName, Map context, long startTime) throws GenericServiceException, RemoteException;

}

