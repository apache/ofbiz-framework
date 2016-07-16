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
package org.apache.ofbiz.service.job;

import java.io.Serializable;
import java.util.Map;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * A generic async-service job.
 */
@SuppressWarnings("serial")
public class GenericServiceJob extends AbstractJob implements Serializable {

    public static final String module = GenericServiceJob.class.getName();

    protected final transient GenericRequester requester;
    protected final transient DispatchContext dctx;
    private final String service;
    private final Map<String, Object> context;

    public GenericServiceJob(DispatchContext dctx, String jobId, String jobName, String service, Map<String, Object> context, GenericRequester req) {
        super(jobId, jobName);
        Assert.notNull("dctx", dctx);
        this.dctx = dctx;
        this.service = service;
        this.context = context;
        this.requester = req;
    }

    /**
     * Invokes the service.
     */
    @Override
    public void exec() throws InvalidJobException {
        if (currentState != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        currentState = State.RUNNING;
        init();
        Throwable thrown = null;
        Map<String, Object> result = null;
        // no transaction is necessary since runSync handles this
        try {
            // get the dispatcher and invoke the service via runSync -- will run all ECAs
            LocalDispatcher dispatcher = dctx.getDispatcher();
            result = dispatcher.runSync(getServiceName(), getContext());
            // check for a failure
            if (ServiceUtil.isError(result)) {
                thrown = new Exception(ServiceUtil.getErrorMessage(result));
            }
            if (requester != null) {
                requester.receiveResult(result);
            }
        } catch (Throwable t) {
            if (requester != null) {
                // pass the exception back to the requester.
                requester.receiveThrowable(t);
            }
            thrown = t;
        }
        if (thrown == null) {
            finish(result);
        } else {
            failed(thrown);
        }
    }

    /**
     * Method is called prior to running the service.
     */
    protected void init() throws InvalidJobException {
        if (Debug.verboseOn()) Debug.logVerbose("Async-Service initializing.", module);
    }

    /**
     * Method is called after the service has finished successfully.
     */
    protected void finish(Map<String, Object> result) throws InvalidJobException {
        if (currentState != State.RUNNING) {
            throw new InvalidJobException("Illegal state change");
        }
        currentState = State.FINISHED;
        if (Debug.verboseOn()) Debug.logVerbose("Async-Service finished.", module);
    }

    /**
     * Method is called when the service fails.
     * @param t Throwable
     */
    protected void failed(Throwable t) throws InvalidJobException {
        if (currentState != State.RUNNING) {
            throw new InvalidJobException("Illegal state change");
        }
        currentState = State.FAILED;
        Debug.logError(t, "Async-Service failed.", module);
    }

    /**
     * Gets the context for the service invocation.
     * @return Map of name value pairs making up the service context.
     */
    protected Map<String, Object> getContext() throws InvalidJobException {
        return context;
    }

    /**
     * Gets the name of the service as defined in the definition file.
     * @return The name of the service to be invoked.
     */
    protected String getServiceName() {
        return service;
    }

    @Override
    public boolean isValid() {
        return currentState == State.CREATED;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        super.deQueue();
        throw new InvalidJobException("Unable to queue job [" + getJobId() + "]");
    }
}
