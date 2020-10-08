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

import java.util.Date;

/**
 * A scheduled job.
 * <p>A job starts out in the created state. When the job is queued for execution, it
 * transitions to the queued state. While the job is executing it is in the running state.
 * When the job execution ends, it transitions to the finished or failed state - depending
 * on the outcome of the task that was performed.</p>
 */
public interface Job extends Runnable {

    enum State { CREATED, QUEUED, RUNNING, FINISHED, FAILED }

    /**
     * Returns the current state of this job.
     */
    State currentState();

    /**
     * Returns the ID of this Job.
     */
    String getJobId();

    /**
     * Returns the name of this Job.
     */
    String getJobName();

    /**
     *  Returns the job execution time in milliseconds.
     *  Returns zero if the job has not run.
     */
    long getRuntime();

    /**
     * Returns true if this job is ready to be queued.
     */
    boolean isValid();

    /**
     * Transitions this job to the pre-queued (created) state. The job manager
     * will call this method when there was a problem adding this job to the queue.
     */
    void deQueue() throws InvalidJobException;

    /**
     * Transitions this job to the queued state.
     */
    void queue() throws InvalidJobException;

    /**
     * Returns the time this job is scheduled to start.
     */
    Date getStartTime();

    /**
     * Returns the priority of this job, higher the number the higher the priority
     */
    long getPriority();
}

