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
package org.ofbiz.service.job;

import java.util.Date;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * Generic Service Job - A generic async-service Job.
 */
public class GenericServiceJob extends AbstractJob {

    public static final String module = GenericServiceJob.class.getName();

    protected transient GenericRequester requester = null;
    protected transient DispatchContext dctx = null;

    private String service = null;
    private Map context = null;

    public GenericServiceJob(DispatchContext dctx, String jobId, String jobName, String service, Map context, GenericRequester req) {
        super(jobId, jobName);
        this.dctx = dctx;
        this.service = service;
        this.context = context;
        this.requester = req;
        runtime = new Date().getTime();
    }

    protected GenericServiceJob(String jobId, String jobName) {
        super(jobId, jobName);
        this.dctx = null;
        this.requester = null;
        this.service = null;
        this.context = null;
    }

    /**
     * Invokes the service.
     */
    public void exec() throws InvalidJobException {
        init();

        // no transaction is necessary since runSync handles this
        try {
            // get the dispatcher and invoke the service via runSync -- will run all ECAs
            LocalDispatcher dispatcher = dctx.getDispatcher();
            Map result = dispatcher.runSync(getServiceName(), getContext());

            // check for a failure
            boolean isError = ModelService.RESPOND_ERROR.equals(result.get(ModelService.RESPONSE_MESSAGE));
            if (isError) {
                 String errorMessage = (String) result.get(ModelService.ERROR_MESSAGE);
                 this.failed(new Exception(errorMessage));
            }

            if (requester != null) {
                requester.receiveResult(result);
            }

        } catch (Throwable t) {
            // pass the exception back to the requester.
            if (requester != null) {
                requester.receiveThrowable(t);
            }

            // call the failed method
            this.failed(t);
        }

        // call the finish method
        this.finish();
    }

    /**
     * Method is called prior to running the service.
     */
    protected void init() throws InvalidJobException {
        if (Debug.verboseOn()) Debug.logVerbose("Async-Service initializing.", module);
    }

    /**
     * Method is called after the service has finished.
     */
    protected void finish() throws InvalidJobException {
        if (Debug.verboseOn()) Debug.logVerbose("Async-Service finished.", module);
        runtime = 0;
    }

    /**
     * Method is called when the service fails.
     * @param t Throwable
     */
    protected void failed(Throwable t) throws InvalidJobException {
        Debug.logError(t, "Async-Service failed.", module);
        runtime = 0;
    }

    /**
     * Gets the context for the service invocation.
     * @return Map of name value pairs making up the service context.
     */
    protected Map getContext() throws InvalidJobException {
        return context;
    }

    /**
     * Gets the name of the service as defined in the definition file.
     * @return The name of the service to be invoked.
     */
    protected String getServiceName() throws InvalidJobException {
        return service;
    }        
}
