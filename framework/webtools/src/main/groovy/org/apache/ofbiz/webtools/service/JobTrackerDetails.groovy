/*
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
*/
package org.apache.ofbiz.webtools.service

import org.apache.ofbiz.service.tracker.JobTrackerListener
import java.text.SimpleDateFormat

JobTrackerListener listener = new JobTrackerListener(delegator, parameters.jobTrackerId as String)
context.state = listener?.state() ?: [:]
long remainingTime = listener.getEstimatedRemainingTime() ?: 0L
if (remainingTime <= 0) {
    context.state.remainingTime = 'Not applicable'
} else {
    SimpleDateFormat df = remainingTime > (60 * 60 * 1000) ? // > 1h ?
            new SimpleDateFormat('HH:mm:ss', context.locale as Locale) :
            new SimpleDateFormat('mm:ss', context.locale as Locale)

    context.state.remainingTime = df.format(new Date(remainingTime))
}

context.state = listener.state()
