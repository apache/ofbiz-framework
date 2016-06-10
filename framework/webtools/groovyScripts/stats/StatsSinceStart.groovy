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

import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.security.Security;
import org.ofbiz.webapp.stats.*;

clearBins = parameters.clear;
if ("true".equals(clearBins)) {
    ServerHitBin.requestSinceStarted.clear();
    ServerHitBin.eventSinceStarted.clear();
    ServerHitBin.viewSinceStarted.clear();
}

// Requests
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.requestSinceStarted.keySet()));
requestList = [];
while (iterator.hasNext()) {
    requestIdMap = [:];
    statsId = iterator.next();
    bin = ServerHitBin.requestSinceStarted.get(statsId);
    if (bin) {
        requestIdMap.requestId = bin.getId();
        requestIdMap.requestType = bin.getType();
        requestIdMap.startTime = bin.getStartTimeString();
        requestIdMap.endTime = bin.getEndTimeString();
        requestIdMap.lengthMins = UtilFormatOut.formatQuantity(bin.getBinLengthMinutes());
        requestIdMap.numberHits = UtilFormatOut.formatQuantity(bin.getNumberHits());
        requestIdMap.minTime = UtilFormatOut.formatQuantity(bin.getMinTimeSeconds());
        requestIdMap.avgTime = UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds());
        requestIdMap.maxTime = UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds());
        requestIdMap.hitsPerMin = UtilFormatOut.formatQuantity(bin.getHitsPerMinute());
        requestList.add(requestIdMap);
    }
}
context.requestList = requestList;

// Events
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.eventSinceStarted.keySet()));
eventList = [];
while (iterator.hasNext()) {
    requestIdMap = [:];
    statsId = iterator.next();
    bin = ServerHitBin.eventSinceStarted.get(statsId);
    if (bin) {
        requestIdMap.requestId = bin.getId();
        requestIdMap.requestType = bin.getType();
        requestIdMap.startTime = bin.getStartTimeString();
        requestIdMap.endTime = bin.getEndTimeString();
        requestIdMap.lengthMins = UtilFormatOut.formatQuantity(bin.getBinLengthMinutes());
        requestIdMap.numberHits = UtilFormatOut.formatQuantity(bin.getNumberHits());
        requestIdMap.minTime = UtilFormatOut.formatQuantity(bin.getMinTimeSeconds());
        requestIdMap.avgTime = UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds());
        requestIdMap.maxTime = UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds());
        requestIdMap.hitsPerMin = UtilFormatOut.formatQuantity(bin.getHitsPerMinute());
        eventList.add(requestIdMap);
    }
}
context.eventList = eventList;


// Views
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.viewSinceStarted.keySet()));
viewList = [];
while (iterator.hasNext()) {
    requestIdMap = [:];
    statsId = iterator.next();
    bin = ServerHitBin.viewSinceStarted.get(statsId);
    if (bin) {
        requestIdMap.requestId = bin.getId();
        requestIdMap.requestType = bin.getType();
        requestIdMap.startTime = bin.getStartTimeString();
        requestIdMap.endTime = bin.getEndTimeString();
        requestIdMap.lengthMins = UtilFormatOut.formatQuantity(bin.getBinLengthMinutes());
        requestIdMap.numberHits = UtilFormatOut.formatQuantity(bin.getNumberHits());
        requestIdMap.minTime = UtilFormatOut.formatQuantity(bin.getMinTimeSeconds());
        requestIdMap.avgTime = UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds());
        requestIdMap.maxTime = UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds());
        requestIdMap.hitsPerMin = UtilFormatOut.formatQuantity(bin.getHitsPerMinute());
        viewList.add(requestIdMap);
    }
}
context.viewList = viewList;
