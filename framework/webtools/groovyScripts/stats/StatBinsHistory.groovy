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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.webapp.stats.*

id = parameters.statsId
typeStr = parameters.type
type = -1
try {
    type = Integer.valueOf(typeStr)
} catch (NumberFormatException e) {}

binList = null
if (type == ServerHitBin.REQUEST) {
    binList = ServerHitBin.requestHistory.get(id)
} else if (type == ServerHitBin.EVENT) {
    binList = ServerHitBin.eventHistory.get(id)
} else if (type == ServerHitBin.VIEW) {
    binList = ServerHitBin.viewHistory.get(id)
}

if (binList) {
    requestList = []
    binList.each { bin ->
        requestIdMap = [:]
        if (bin != null) {
            requestIdMap.requestId = bin.getId()
            requestIdMap.requestType = bin.getType()
            requestIdMap.startTime = bin.getStartTimeString()
            requestIdMap.endTime = bin.getEndTimeString()
            requestIdMap.lengthMins = UtilFormatOut.formatQuantity(bin.getBinLengthMinutes())
            requestIdMap.numberHits = UtilFormatOut.formatQuantity(bin.getNumberHits())
            requestIdMap.minTime = UtilFormatOut.formatQuantity(bin.getMinTimeSeconds())
            requestIdMap.avgTime = UtilFormatOut.formatQuantity(bin.getAvgTimeSeconds())
            requestIdMap.maxTime = UtilFormatOut.formatQuantity(bin.getMaxTimeSeconds())
            requestIdMap.hitsPerMin = UtilFormatOut.formatQuantity(bin.getHitsPerMinute())
            requestList.add(requestIdMap)
        }
    }
    context.requestList = requestList
}
