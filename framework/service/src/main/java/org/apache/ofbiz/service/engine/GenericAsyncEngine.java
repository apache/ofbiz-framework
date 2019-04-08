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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericRequester;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.job.GenericServiceJob;
import org.apache.ofbiz.service.job.Job;
import org.apache.ofbiz.service.job.JobManager;
import org.apache.ofbiz.service.job.JobManagerException;

/**
 * Generic Asynchronous Engine
 */
public abstract class GenericAsyncEngine extends AbstractEngine {

    public static final String module = GenericAsyncEngine.class.getName();

    protected GenericAsyncEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    public abstract Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException;

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map)
     */
    public abstract void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException;

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, boolean persist) throws GenericServiceException {
        runAsync(localName, modelService, context, null, persist);
    }

    /**
     * @see org.apache.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.apache.ofbiz.service.ModelService, java.util.Map, org.apache.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, GenericRequester requester, boolean persist) throws GenericServiceException {
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
                // Create the runtime data
                String dataId = dispatcher.getDelegator().getNextSeqId("RuntimeData");

                GenericValue runtimeData = dispatcher.getDelegator().makeValue("RuntimeData", "runtimeDataId", dataId);

                runtimeData.set("runtimeInfo", XmlSerializer.serialize(context));
                runtimeData.create();

                // Get the userLoginId out of the context
                String authUserLoginId = null;
                if (context.get("userLogin") != null) {
                    GenericValue userLogin = (GenericValue) context.get("userLogin");
                    authUserLoginId = userLogin.getString("userLoginId");
                }

                // Create the job info
                String jobId = dispatcher.getDelegator().getNextSeqId("JobSandbox");
                String jobName = Long.toString(System.currentTimeMillis());

                Map<String, Object> jFields = UtilMisc.toMap("jobId", jobId, "jobName", jobName, "runTime", UtilDateTime.nowTimestamp());
                jFields.put("poolId", ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool());
                jFields.put("statusId", "SERVICE_PENDING");
                jFields.put("serviceName", modelService.name);
                jFields.put("loaderName", localName);
                jFields.put("maxRetry", Long.valueOf(modelService.maxRetry));
                jFields.put("runtimeDataId", dataId);
                if (UtilValidate.isNotEmpty(authUserLoginId)) {
                    jFields.put("authUserLoginId", authUserLoginId);
                }

                jobV = dispatcher.getDelegator().makeValue("JobSandbox", jFields);
                jobV.create();
            } catch (GenericEntityException e) {
                throw new GenericServiceException("Unable to create persisted job", e);
            } catch (SerializeException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (FileNotFoundException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (IOException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (GenericConfigException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            }

            Debug.logInfo("Persisted job queued : " + jobV.getString("jobName"), module);
        } else {
            JobManager jMgr = dispatcher.getJobManager();
            if (jMgr != null) {
                String name = Long.toString(System.currentTimeMillis());
                String jobId = modelService.name + "." + name;
                job = new GenericServiceJob(dctx, jobId, name, modelService.name, context, requester);
                try {
                    dispatcher.getJobManager().runJob(job);
                } catch (JobManagerException jse) {
                    throw new GenericServiceException("Cannot run job.", jse);
                }
            } else {
                throw new GenericServiceException("Cannot get JobManager instance to invoke the job");
            }
        }
    }

    @Override
    protected boolean allowCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        return mode == GenericEngine.SYNC_MODE;
    }
}
