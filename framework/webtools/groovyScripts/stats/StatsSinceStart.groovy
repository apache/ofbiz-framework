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

import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.webapp.stats.ServerHitBin

clearBins = parameters.clear
if ('true' == clearBins) {
    ServerHitBin.requestSinceStarted.clear()
    ServerHitBin.eventSinceStarted.clear()
    ServerHitBin.viewSinceStarted.clear()
}

// Requests
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.requestSinceStarted.keySet()))
requestList = []
while (iterator.hasNext()) {
    statsId = iterator.next()
    bin = ServerHitBin.requestSinceStarted.get(statsId)
    if (bin) {
        requestList.add(prepareRequestIdMap(bin))
    }
}
context.requestList = requestList

// Events
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.eventSinceStarted.keySet()))
eventList = []
while (iterator.hasNext()) {
    statsId = iterator.next()
    bin = ServerHitBin.eventSinceStarted.get(statsId)
    if (bin) {
        eventList.add(prepareRequestIdMap(bin))
    }
}
context.eventList = eventList


// Views
iterator = UtilMisc.toIterator(new TreeSet(ServerHitBin.viewSinceStarted.keySet()))
viewList = []
while (iterator.hasNext()) {
    requestIdMap = [:]
    statsId = iterator.next()
    bin = ServerHitBin.viewSinceStarted.get(statsId)
    if (bin) {
        viewList.add(prepareRequestIdMap(bin))
    }
}
context.viewList = viewList

private LinkedHashMap<String, String> prepareRequestIdMap(bin) {
    return [
            requestId: bin.getId(),
            requestType: bin.getType(),
            startTime: bin.getStartTimeString(),
            endTime: bin.getEndTimeString(),
            lengthMins: UtilFormatOut.formatQuantity(bin.getBinLengthMinutes()),
            numberHits: UtilFormatOut.formatQuantity(bin.getNumberHits()),
            minTime: UtilFormatOut.formatQuantity(bin.getMinTimeSeconds()),
            avgTime: UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds()),
            maxTime: UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds()),
            hitsPerMin: UtilFormatOut.formatQuantity(bin.getHitsPerMinute())
    ]
}

