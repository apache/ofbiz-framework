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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchRunTrackerTest {

    @Test
    void registerMakesTheChildListRetrievableByBatchId() {
        BatchRunTracker tracker = new BatchRunTracker();
        List<BatchRunTracker.BatchChildRef> children = List.of(
                new BatchRunTracker.BatchChildRef("example", "run-1"),
                new BatchRunTracker.BatchChildRef("party", "run-2"));

        tracker.register("batch-1", children);

        assertThat(tracker.get("batch-1"), is(children));
    }

    @Test
    void getReturnsNullForAnUnknownBatchId() {
        BatchRunTracker tracker = new BatchRunTracker();

        assertThat(tracker.get("no-such-batch"), nullValue());
    }

    @Test
    void registerDefensivelyCopiesTheCallersChildList() {
        BatchRunTracker tracker = new BatchRunTracker();
        List<BatchRunTracker.BatchChildRef> children = new ArrayList<>();
        children.add(new BatchRunTracker.BatchChildRef("example", "run-1"));

        tracker.register("batch-1", children);
        children.add(new BatchRunTracker.BatchChildRef("party", "run-2"));

        assertThat(tracker.get("batch-1"), is(List.of(new BatchRunTracker.BatchChildRef("example", "run-1"))));
    }

    @Test
    void registerRejectsFurtherMutationOfTheStoredList() {
        BatchRunTracker tracker = new BatchRunTracker();
        tracker.register("batch-1", List.of(new BatchRunTracker.BatchChildRef("example", "run-1")));

        List<BatchRunTracker.BatchChildRef> stored = tracker.get("batch-1");

        assertThrows(UnsupportedOperationException.class, () ->
                stored.add(new BatchRunTracker.BatchChildRef("party", "run-2")));
    }
}
