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

import java.util.Map;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.security.Security;
import org.ofbiz.service.jms.JmsListenerFactory;
import org.ofbiz.service.job.JobManager;

/**
 * Generic Services Local Dispatcher
 */
public interface LocalDispatcher {

    /**
     * Run the service synchronously and return the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map runSync(String serviceName, Map context) throws GenericServiceException;

    /**
     * Run the service synchronously with a specified timeout and return the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map runSync(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service synchronously and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String serviceName, Map context) throws GenericServiceException;

    /**
     * Run the service synchronously with a specified timeout and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String serviceName, Map context, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException;
    
    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @param transactionTimeout the overriding timeout for the transaction (if we started it).
     * @param requireNewTransaction if true we will suspend and create a new transaction so we are sure to start.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist, int transactionTimeout, boolean requireNewTransaction) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context, GenericRequester requester) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously and IGNORE the result.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously and IGNORE the result. This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String serviceName, Map context) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @return A new GenericRequester object.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Run the service asynchronously. This method WILL persist the job.
     * @param serviceName Name of the service to run.
     * @param context Map of name, value pairs composing the context.
     * @return A new GenericRequester object.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public GenericResultWaiter runAsyncWait(String serviceName, Map context) throws ServiceAuthException, ServiceValidationException, GenericServiceException;

    /**
     * Register a callback listener on a specific service.
     * @param serviceName Name of the service to link callback to.
     * @param cb The callback implementation.
     */
    public void registerCallback(String serviceName, GenericServiceCallback cb);

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param poolName Name of the service pool to send to.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param count The number of times to repeat.
     * @param endTime The time in milliseconds the service should expire
     * @param maxRetry The number of times we should retry on failure
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void schedule(String poolName, String serviceName, Map context, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws GenericServiceException;

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param jobName Name of the job
     * @param poolName Name of the service pool to send to.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param count The number of times to repeat.
     * @param endTime The time in milliseconds the service should expire
     * @param maxRetry The number of times we should retry on failure
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void schedule(String jobName, String poolName, String serviceName, Map context, long startTime, int frequency, int interval, int count, long endTime, int maxRetry) throws GenericServiceException;


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
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count, long endTime) throws GenericServiceException;

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param count The number of times to repeat.
     * @throws GenericServiceException
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, int count) throws GenericServiceException;

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @param frequency The frequency of the recurrence (RecurrenceRule.DAILY, etc).
     * @param interval The interval of the frequency recurrence.
     * @param endTime The time in milliseconds the service should expire
     * @throws GenericServiceException
     */
    public void schedule(String serviceName, Map context, long startTime, int frequency, int interval, long endTime) throws GenericServiceException;

    /**
     * Schedule a service to run asynchronously at a specific start time.
     * @param serviceName Name of the service to invoke.
     * @param context The name/value pairs composing the context.
     * @param startTime The time to run this service.
     * @throws GenericServiceException
     */
    public void schedule(String serviceName, Map context, long startTime) throws GenericServiceException;


    /**
     * Adds a rollback service to the current TX using the ServiceXaWrapper
     * @param serviceName
     * @param context
     * @param persist
     * @throws GenericServiceException
     */
    public void addRollbackService(String serviceName, Map context, boolean persist) throws GenericServiceException;

    /**
     * Adds a commit service to the current TX using the ServiceXaWrapper
     * @param serviceName
     * @param context
     * @param persist
     * @throws GenericServiceException
     */
    public void addCommitService(String serviceName, Map context, boolean persist) throws GenericServiceException;

    /**
     * Gets the JobManager associated with this dispatcher
     * @return JobManager that is associated with this dispatcher
     */
    public JobManager getJobManager();

    /**
     * Gets the JmsListenerFactory which holds the message listeners.
     * @return JmsListenerFactory
     */
    public JmsListenerFactory getJMSListeneFactory();

    /**
     * Gets the GenericEntityDelegator associated with this dispatcher
     * @return GenericEntityDelegator associated with this dispatcher
     */
    public GenericDelegator getDelegator();

    /**
     * Gets the Security object associated with this dispatcher
     * @return Security object associated with this dispatcher
     */
    public Security getSecurity();

    /**
     * Returns the Name of this local dispatcher
     * @return String representing the name of this local dispatcher
     */
    public String getName();

    /**
     * Returns the DispatchContext created by this dispatcher
     * @return DispatchContext created by this dispatcher
     */
    public DispatchContext getDispatchContext();

    /**
     * De-Registers this LocalDispatcher with the ServiceDispatcher
     */
    public void deregister();
}

