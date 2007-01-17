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

/**
 * Abstract Service Job - Invokes a service
 */
public abstract class AbstractJob implements Job {

    public static final String module = AbstractJob.class.getName();

    protected long runtime = -1;
    protected long sequence = 0;
    private String jobId;
    private String jobName;
    private boolean queued = false;

    protected AbstractJob(String jobId, String jobName) {
        this.jobId = jobId;
        this.jobName = jobName;
    }

    /**
     * Returns the time to run in milliseconds.
     */
    public long getRuntime() {
        return runtime;
    }

    /**
     * Returns true if this job is still valid.
     */
    public boolean isValid() {
        if (runtime > 0)
            return true;
        return false;
    }

    /**
     * Returns the ID of this Job.
     */
    public String getJobId() {
        return this.jobId;
    }

    /**
     * Returns the name of this Job.
     */
    public String getJobName() {
        return this.jobName;
    }

    /**
     * Flags this job as 'is-queued'
     */
    public void queue() throws InvalidJobException {
        this.queued = true;
    }

    /**
     *  Executes the Job.
     */
    public abstract void exec() throws InvalidJobException;
}
