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

import java.io.Serializable;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

/**
 * Purge job - removes a JobSandbox entity value and its related values.
 */
@SuppressWarnings("serial")
public class PurgeJob extends AbstractJob implements Serializable {

    public static final String module = PurgeJob.class.getName();

    private final GenericValue jobValue;

    public PurgeJob(GenericValue jobValue) {
        super(jobValue.getString("jobId"), "Purge " + jobValue.getString("jobName"));
        this.jobValue = jobValue;
    }

    @Override
    public void exec() throws InvalidJobException {
        if (currentState != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        currentState = State.RUNNING;
        try {
            // TODO: This might need to be in a transaction - to avoid the possibility of
            // leaving orphaned related values.
            jobValue.remove();
            GenericValue relatedValue = jobValue.getRelatedOne("RecurrenceInfo", false);
            if (relatedValue != null) {
                List<GenericValue> valueList = relatedValue.getRelated("JobSandbox", null, null, false);
                if (valueList.isEmpty()) {
                    relatedValue.remove();
                    relatedValue.removeRelated("RecurrenceRule");
                }
            }
            relatedValue = jobValue.getRelatedOne("RuntimeData", false);
            if (relatedValue != null) {
                List<GenericValue> valueList = relatedValue.getRelated("JobSandbox", null, null, false);
                if (valueList.isEmpty()) {
                    relatedValue.remove();
                }
            }
            Debug.logInfo("Purged job " + getJobId(), module);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Exception thrown while purging job: ", module);
        }
    }

    @Override
    public boolean isValid() {
        return currentState == State.CREATED;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        if (currentState != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
    }
}
