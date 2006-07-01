/*
 * $Id: GenericServiceJob.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> *
 * @version    $Rev$
 * @since      2.0
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
