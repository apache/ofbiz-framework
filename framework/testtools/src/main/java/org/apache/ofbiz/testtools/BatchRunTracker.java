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
package org.apache.ofbiz.testtools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of runBatchTestSuite-triggered batches, for getBatchTestRunStatus polling.
 * Deliberately not persisted - does not survive a server restart, same as TestRunTracker. Unlike
 * TestRunTracker, a batch's child list never changes after registration: runBatchTestSuite always
 * finishes fanning every child run out (each already registered in TestRunServices' own TRACKER)
 * before it ever hands a batchId back to a caller, so there is nothing left to mutate here - a
 * batch's aggregate status is instead computed live, at read time, from each child's own tracked
 * TestRunRecord (see BatchRunServices.getBatchTestRunStatus).
 *
 * <p>Like TestRunTracker's own map, nothing ever removes an entry here either - every batch ever
 * triggered stays resident in memory for the life of the server process. This is a much smaller
 * per-entry footprint than the ServiceDispatcher/Delegator leak TestRunServices' own javadoc
 * documents at length (each entry here is just a componentName/runId pair list, not a live
 * dispatcher), but it is still unbounded growth with no TTL, cap, or purge mechanism - a known
 * characteristic of this POC-level implementation, not a bug.
 */
final class BatchRunTracker {

    /**
     * One component queued into a batch run, and the runId TestRunServices' own TRACKER tracks it
     * under.
     * @param componentName the component this child run belongs to
     * @param runId the same runId TestRunServices.TRACKER.get(runId) resolves
     */
    record BatchChildRef(String componentName, String runId) {
    }

    private final Map<String, List<BatchChildRef>> records = new ConcurrentHashMap<>();

    void register(String batchId, List<BatchChildRef> children) {
        records.put(batchId, List.copyOf(children));
    }

    List<BatchChildRef> get(String batchId) {
        return records.get(batchId);
    }
}
