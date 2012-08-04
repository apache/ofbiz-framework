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
package org.ofbiz.workflow.client;

import java.util.HashMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.job.AbstractJob;
import org.ofbiz.service.job.InvalidJobException;
import org.ofbiz.workflow.WfActivity;

/**
 * Workflow Client API - Start Activity Async-Job
 */
public class StartActivityJob extends AbstractJob {

    public static final String module = StartActivityJob.class.getName();

    protected WfActivity activity = null;
    protected GenericRequester requester = null;
    private final long runtime = System.currentTimeMillis();

    public StartActivityJob(WfActivity activity) {
        this(activity, null);
    }

    public StartActivityJob(WfActivity activity, GenericRequester requester) {
        super(activity.toString() + "." + System.currentTimeMillis(), activity.toString());
        this.activity = activity;
        this.requester = requester;
        if (Debug.verboseOn()) Debug.logVerbose("Created new StartActivityJob : " + activity, module);
    }

    @Override
    public void exec() throws InvalidJobException {
        if (currentState != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        currentState = State.RUNNING;
        try {
            Debug.logVerbose("Executing job now : " + activity, module);
            activity.activate();
            if (requester != null)
                requester.receiveResult(new HashMap<String, Object>());
        } catch (Exception e) {
            Debug.logError(e, "Start Activity [" + activity + "] Failed", module);
            if (requester != null)
                requester.receiveThrowable(e);
        }
        currentState = State.FINISHED;
    }

    @Override
    public long getRuntime() {
        return runtime;
    }

    @Override
    public boolean isValid() {
        return currentState == State.CREATED;
    }
}
