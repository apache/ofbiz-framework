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

package org.apache.ofbiz.service.tracker;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.ofbiz.base.util.UtilDateTime.adjustTimestamp;
import static org.apache.ofbiz.base.util.UtilDateTime.nowTimestamp;
import static org.apache.ofbiz.entity.condition.EntityCondition.makeCondition;
import static org.apache.ofbiz.entity.condition.EntityOperator.IN;

public class JobTrackerListener {
    private String trackerId;
    private Delegator delegator;

    public JobTrackerListener(Delegator delegator, String trackerId) {
        this.delegator = delegator;
        this.trackerId = trackerId;
    }

    public JobTrackerListener(Delegator delegator, JobTracker tracker) {
        this.delegator = delegator;
        this.trackerId = tracker.getJobTrackerId();
    }

    /**
     * give information of jobTracker instance with followed jobs
     *
     * @return a Map with all jobtracker information and
     * for each jobs status the number and percentage of job in it
     */
    public Map<String, Object> state() {
        List<GenericValue> jobQtyByStatus;
        Map<String, Object> state;
        try {
            jobQtyByStatus = EntityQuery.use(delegator)
                    .from("JobSandboxQtyByStatusAndJobTrackerView")
                    .where("jobTrackerId", trackerId)
                    .queryList();

            GenericValue jobTracker = EntityQuery.use(delegator)
                    .from("JobTracker")
                    .where("jobTrackerId", trackerId)
                    .queryOne();
            if (UtilValidate.isEmpty(jobTracker)) {
                return Collections.emptyMap();
            }
            state = jobTracker.getAllFields();
            List<String> completedStatusIds = List.of("SERVICE_FAILED", "SERVICE_FINISHED", "SERVICE_CRASHED", "SERVICE_CANCELLED");
            jobQtyByStatus.forEach(jobQty -> {
                BigDecimal jobsCurrentQuantityForStatus = jobQty.getBigDecimal("quantity");
                state.put(jobQty.getString("statusId"), jobsCurrentQuantityForStatus);
                String percentageName = String.format("%s_percentage", jobQty.getString("statusId"));
                BigDecimal jobsTotalQty = BigDecimal.valueOf((Long) state.get("jobsTotalQty"));
                BigDecimal percentageValue = jobsCurrentQuantityForStatus.divide(
                        BigDecimal.ZERO.compareTo(jobsTotalQty) < 0 ? jobsTotalQty : jobsCurrentQuantityForStatus,
                        2, RoundingMode.HALF_UP);
                state.put(percentageName, percentageValue);
                if (completedStatusIds.contains(jobQty.getString("statusId"))) {
                    UtilMisc.addToBigDecimalInMap(state, "totalCompleted", jobsCurrentQuantityForStatus);
                    UtilMisc.addToBigDecimalInMap(state, "totalCompleted_percentage", percentageValue);
                }
            });
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
        return state;
    }

    /**
     * "SERVICE_PENDING" , "SERVICE_QUEUED" , "SERVICE_RUNNING" , "SERVICE_FINISHED" ,
     * "SERVICE_FAILED" , "SERVICE_CRASHED" , "SERVICE_ON_HOLD" , "SERVICE_CANCELLED" ,
     * Checks that all job related to this tracker are done running
     *
     * @return true if all related jobs are at a terminal status
     */
    public boolean isFinished() {
        final List<String> notFinishedStatuses = List.of("SERVICE_PENDING", "SERVICE_QUEUED", "SERVICE_RUNNING", "SERVICE_ON_HOLD");
        try {
            return EntityQuery.use(delegator).from("JobSandbox").where(makeCondition(
                            makeCondition("jobTrackerId", trackerId),
                            makeCondition("statusId", IN, notFinishedStatuses)))
                    .queryCount() < 1;
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the estimated completion time when the jobTacker seems to be completed
     */
    public Timestamp getEstimatedCompletionTime() {
        long remainingSeconds = getEstimatedTotalTime();
        return adjustTimestamp(nowTimestamp(), Calendar.MILLISECOND, Math.toIntExact(remainingSeconds));
    }

    /**
     * Return the estimated number of seconds remaining for the full process to end.
     * Return -1 if finished
     *
     * @return
     */
    public long getEstimatedTotalTime() {
        Map<String, Object> state = state();
        if (UtilValidate.isEmpty(state)) {
            return -1;
        }
        BigDecimal totalCompleted = (BigDecimal) state.get("totalCompleted");
        if (totalCompleted == null) {
            return -1;
        }
        long totalJobsCompleted = totalCompleted.longValue();
        Timestamp startDate = (Timestamp) state.get("startDate");
        long jobsTotalQty = (long) state.get("jobsTotalQty");
        if (totalJobsCompleted == jobsTotalQty) {
            return -1;
        }
        long milliSecSinceStart = (nowTimestamp().getTime() - startDate.getTime());
        return totalJobsCompleted == 0L ? 0L : (milliSecSinceStart * jobsTotalQty) / totalJobsCompleted; // Produit en croix
    }

    /**
     * @return the remaining time when the jobTacker seems to be completed
     */
    public long getEstimatedRemainingTime() {
        Timestamp startDate = (Timestamp) state().get("startDate");
        if (startDate == null) {
            return 0;
        }
        long milliSecSinceStart = (nowTimestamp().getTime() - startDate.getTime());
        return getEstimatedTotalTime() - milliSecSinceStart;
    }

}
