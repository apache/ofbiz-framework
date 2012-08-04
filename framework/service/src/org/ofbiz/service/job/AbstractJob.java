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

import org.ofbiz.base.util.Assert;

/**
 * Abstract Job.
 */
public abstract class AbstractJob implements Job {

    private final String jobId;
    private final String jobName;
    protected State currentState = State.CREATED;

    protected AbstractJob(String jobId, String jobName) {
        Assert.notNull("jobId", jobId, "jobName", jobName);
        this.jobId = jobId;
        this.jobName = jobName;
    }

    @Override
    public State currentState() {
        return currentState;
    }

    @Override
    public String getJobId() {
        return this.jobId;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void queue() throws InvalidJobException {
        if (currentState != State.CREATED) {
            throw new InvalidJobException("Illegal state change");
        }
        this.currentState = State.QUEUED;
    }
}
