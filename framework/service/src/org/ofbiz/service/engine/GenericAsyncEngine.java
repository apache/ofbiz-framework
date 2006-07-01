/*
 * $Id: GenericAsyncEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.job.GenericServiceJob;
import org.ofbiz.service.job.Job;
import org.ofbiz.service.job.JobManagerException;

/**
 * Generic Asynchronous Engine
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public abstract class GenericAsyncEngine extends AbstractEngine {
    
    public static final String module = GenericAsyncEngine.class.getName();

    protected GenericAsyncEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);    
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public abstract Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException;
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public abstract void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException;

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map context, boolean persist) throws GenericServiceException {
        runAsync(localName, modelService, context, null, persist);
    }
    
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map, org.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map context, GenericRequester requester, boolean persist) throws GenericServiceException {
        DispatchContext dctx = dispatcher.getLocalContext(localName);
        Job job = null;

        if (persist) {
            // check for a delegator
            if (dispatcher.getDelegator() == null) {
                throw new GenericServiceException("No reference to delegator; cannot run persisted services.");
            }

            GenericValue jobV = null;
            // Build the value object(s).
            try {
                List toBeStored = new LinkedList();

                // Create the runtime data
                String dataId = dispatcher.getDelegator().getNextSeqId("RuntimeData");

                GenericValue runtimeData = dispatcher.getDelegator().makeValue("RuntimeData",
                        UtilMisc.toMap("runtimeDataId", dataId));

                runtimeData.set("runtimeInfo", XmlSerializer.serialize(context));
                toBeStored.add(runtimeData);

                // Create the job info
                String jobId = dispatcher.getDelegator().getNextSeqId("JobSandbox").toString();
                String jobName = new String(new Long((new Date().getTime())).toString());


                Map jFields = UtilMisc.toMap("jobId", jobId, "jobName", jobName, "runTime", UtilDateTime.nowTimestamp());
                jFields.put("poolId", ServiceConfigUtil.getSendPool());
                jFields.put("statusId", "SERVICE_PENDING");
                jFields.put("serviceName", modelService.name);
                jFields.put("loaderName", localName);
                jFields.put("maxRetry", new Long(modelService.maxRetry));
                jFields.put("runtimeDataId", dataId);

                jobV = dispatcher.getDelegator().makeValue("JobSandbox", jFields);
                toBeStored.add(jobV);
                dispatcher.getDelegator().storeAll(toBeStored);

            } catch (GenericEntityException e) {
                throw new GenericServiceException("Unable to create persisted job", e);
            } catch (SerializeException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (FileNotFoundException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (IOException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            }

            // make sure we stored okay
            if (jobV == null) {
                throw new GenericServiceException("Persisted job not created");
            } else {
                Debug.logInfo("Persisted job queued : " + jobV.getString("jobName"), module);
            }
        } else {
            String name = new Long(new Date().getTime()).toString();
            String jobId = modelService.name + "." + name;
            job = new GenericServiceJob(dctx, jobId, name, modelService.name, context, requester);
            try {
                dispatcher.getJobManager().runJob(job);
            } catch (JobManagerException jse) {
                throw new GenericServiceException("Cannot run job.", jse);
            }
        }
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#sendCallbacks(org.ofbiz.service.ModelService, java.util.Map, java.lang.Object, int)
     */
    public void sendCallbacks(ModelService model, Map context, Object cbObj, int mode) throws GenericServiceException {
        if (mode == GenericEngine.SYNC_MODE) {
            super.sendCallbacks(model, context, cbObj, mode);
        }
    }
}

