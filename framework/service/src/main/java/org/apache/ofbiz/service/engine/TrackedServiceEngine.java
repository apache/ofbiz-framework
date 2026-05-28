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

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.tracker.JobTracker;
import org.apache.ofbiz.service.tracker.JobTrackerFactory;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;

import java.util.Map;
import java.util.Set;

public class TrackedServiceEngine extends GroovyEngine {

    private static final String MODULE = TrackedServiceEngine.class.getName();
    private static final Object[] EMPTY_ARGS = {};
    private static final Set<String> PROTECTED_KEYS = createProtectedKeys();

    private static Set<String> createProtectedKeys() {
        return Set.of("dctx", "dispatcher", "delegator", "visualTheme");
    }

    public TrackedServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @return service result called
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) {
        DispatchContext dctx = getDispatcher().getLocalContext(localName);

        JobTracker tracker;
        Map<String, Object> result;
        try {
            tracker = new JobTrackerFactory(dctx.getDispatcher())
                    .setServiceName(modelService.getName())
                    .setServiceParams(context)
                    .persistResult()
                    .instantiate();
            tracker.persist();

            context.put("jobTrackerId", tracker.getJobTrackerId());

            tracker.updateStatus("JOB_T_SCHEDULED", Map.of("startDate", UtilDateTime.nowTimestamp()));
            try {
                result = super.runSync(localName, modelService, context);
            } catch (GeneralException e) {
                tracker.updateStatus("JOB_T_FAILED", Map.of("cancelDate", UtilDateTime.nowTimestamp()));
                return ServiceUtil.returnError(e.getMessage());
            }
            tracker.computeJobsTotalQty();
            tracker.updateStatus("JOB_T_RUNNING");
            result.put("jobTrackerId", tracker.getJobTrackerId());
        } catch (GenericServiceException | GenericEntityException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
