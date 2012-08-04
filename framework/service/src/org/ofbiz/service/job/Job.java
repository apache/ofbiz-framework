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
 * A scheduled job.
 * <p>A job starts out in the created state. When the job is queued for execution, it
 * transitions to the queued state. While the job is executing it is in the running state.
 * When the job execution ends, it transitions to the finished or failed state - depending
 * on the outcome of the task that was performed.</p>
 */
public interface Job {

    public static enum State {CREATED, QUEUED, RUNNING, FINISHED, FAILED};

    /**
     * Returns the current state of this job.
     */
    State currentState();

    /**
     *  Executes this Job.
     */
    void exec() throws InvalidJobException;

    /**
     * Returns the ID of this Job.
     */
    String getJobId();

    /**
     * Returns the name of this Job.
     */
    String getJobName();

    /**
     *  Returns the time to run in milliseconds.
     */
    long getRuntime();

    /**
     * Returns true if this job is ready to be queued.
     */
    boolean isValid();

    /**
     * Transitions the job to the queued state.
     */
    void queue() throws InvalidJobException;
}

