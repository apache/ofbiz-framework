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
import org.ofbiz.base.util.UtilValidate;
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

                // Get the userLoginId out of the context
                String authUserLoginId = null;
                if (context.containsKey("userLogin")) {
                    GenericValue userLogin = (GenericValue)context.get("userLogin");
                    authUserLoginId = userLogin.getString("userLoginId");
                }

                // Create the job info
                String jobId = dispatcher.getDelegator().getNextSeqId("JobSandbox");
                String jobName = Long.toString((new Date().getTime()));

                Map jFields = UtilMisc.toMap("jobId", jobId, "jobName", jobName, "runTime", UtilDateTime.nowTimestamp());
                jFields.put("poolId", ServiceConfigUtil.getSendPool());
                jFields.put("statusId", "SERVICE_PENDING");
                jFields.put("serviceName", modelService.name);
                jFields.put("loaderName", localName);
                jFields.put("maxRetry", new Long(modelService.maxRetry));
                jFields.put("runtimeDataId", dataId);
                if (UtilValidate.isNotEmpty(authUserLoginId)) {
                    jFields.put("authUserLoginId", authUserLoginId);
                }

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
            String name = Long.toString(new Date().getTime());
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

