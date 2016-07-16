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
package org.apache.ofbiz.service.engine;

import java.util.Map;

import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;

/**
 * Generic Engine Interface
 */
public interface GenericEngine {

    public static final int ASYNC_MODE = 22;
    public static final int SYNC_MODE = 21;

    /**
     * Run the service synchronously and return the result.
     *
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws GenericServiceException
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException;

    /**
     * Run the service synchronously and IGNORE the result.
     *
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     *
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, GenericRequester requester, boolean persist)
        throws GenericServiceException;

    /**
     * Run the service asynchronously and IGNORE the result.
     *
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, boolean persist) throws GenericServiceException;

    /**
     * Send the service callbacks
     * @param modelService Service model object
     * @param context Map of name, value pairs composing the context
     * @param mode Service mode (sync or async)
     * @throws GenericServiceException
     */
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, int mode) throws GenericServiceException;
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, Map<String, Object> result, int mode) throws GenericServiceException;
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, Throwable t, int mode) throws GenericServiceException;
}

