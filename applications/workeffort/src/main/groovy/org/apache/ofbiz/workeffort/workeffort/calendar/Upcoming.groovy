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

package org.apache.ofbiz.workeffort.workeffort.calendar


import org.apache.ofbiz.base.util.UtilMisc

facilityId = parameters.get('facilityId')
fixedAssetId = parameters.get('fixedAssetId')
partyId = parameters.get('partyId')
workEffortTypeId = parameters.get('workEffortTypeId')
calendarType = parameters.calendarType
start = nowTimestamp.clone()
eventsParam = ''
if (facilityId != null) {
    eventsParam = 'facilityId=' + facilityId
}
if (fixedAssetId != null) {
    eventsParam = 'fixedAssetId=' + fixedAssetId
}
if (partyId != null) {
    eventsParam = 'partyId=' + partyId
}
if (workEffortTypeId != null) {
    eventsParam = 'workEffortTypeId=' + workEffortTypeId
}

Map serviceCtx = UtilMisc.toMap('userLogin', userLogin, 'start', start, 'numPeriods', 7, 'periodType', Calendar.DATE)
serviceCtx.putAll([partyId: partyId, facilityId: facilityId, fixedAssetId: fixedAssetId, workEffortTypeId: workEffortTypeId,
                   calendarType: calendarType, locale: locale, timeZone: timeZone])

Map result = runService('getWorkEffortEventsByPeriod', serviceCtx)
context.put('days', result.get('periods'))
context.put('start', start)
context.put('eventsParam', eventsParam)
