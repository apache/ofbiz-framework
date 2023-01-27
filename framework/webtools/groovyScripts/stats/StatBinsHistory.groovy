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
import org.apache.ofbiz.webapp.stats.ServerHitBin

id = parameters.statsId
typeStr = parameters.type
type = -1
try {
    type = Integer.valueOf(typeStr)
} catch (NumberFormatException nfe) {
    logError(nfe, 'Caught an exception : ' + nfe.toString())
    errMsgList.add('Entered value is non-numeric for numeric field: ' + field.getName())
}

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
        if (bin != null) {
            Map requestIdMap = [
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
            requestList.add(requestIdMap)
        }
    }
    context.requestList = requestList
}
