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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

import java.sql.Timestamp

facilityId = parameters.facilityId;
if (!facilityId && facilities) {
    facilityId = facilities[0].facilityId
}
context.facilityId = facilityId;
exprBldr = new EntityConditionBuilder();

Timestamp lastCountDate = null;
countDays = parameters.countDays;
if (countDays && UtilValidate.isInteger(countDays)) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, -(new Integer(parameters.countDays)));
    lastCountDate = new Timestamp(cal.getTimeInMillis());
}
Timestamp nextCountDate = null;
nextCountDays = parameters.nextCountDays;
if (nextCountDays && UtilValidate.isInteger(nextCountDays)) {
    Calendar nextCountCal = Calendar.getInstance();
    nextCountCal.add(Calendar.DAY_OF_YEAR, (new Integer(parameters.nextCountDays)));
    nextCountDate = new Timestamp(nextCountCal.getTimeInMillis());
}

cond = exprBldr.AND() {
    if (facilityId) {
        EQUALS("facilityId": facilityId)
    }
    OR {
        NOT_EQUAL("locked": "Y")
        EQUALS("locked": null)
    }
    if (parameters.locationSeqId) {
        EQUALS("locationSeqId": parameters.locationSeqId)
    }
    if (parameters.areaId) {
        LIKE("areaId": (parameters.areaId).toUpperCase() + "%")
    }
    if (parameters.aisleId) {
        LIKE("aisleId": (parameters.aisleId).toUpperCase() + "%")
    }
    if (parameters.sectionId) {
        LIKE("sectionId": (parameters.sectionId).toUpperCase() + "%")
    }
    if (parameters.levelId) {
        LIKE("levelId": (parameters.levelId).toUpperCase() + "%")
    }
    if (parameters.positionId) {
        LIKE("positionId": (parameters.positionId).toUpperCase() + "%")
    }
    if (countDays && UtilValidate.isInteger(countDays)) {
        LESS_THAN_EQUAL_TO(lastCountDate: new java.sql.Date(lastCountDate.getTime()))
        lastCountCond = " lastCountDate <= '" + new java.sql.Date(lastCountDate.getTime()) + "'"
    }
    if (nextCountDays && UtilValidate.isInteger(nextCountDays)) {
        OR() {
            LESS_THAN_EQUAL_TO(nextCountDate: new java.sql.Date(UtilDateTime.nowTimestamp().getTime()))
            GREATER_THAN_EQUAL_TO(nextCountDate: new java.sql.Date(nextCountDate.getTime()))
        }
    }
}

viewIndex = parameters.VIEW_INDEX ? Integer.valueOf(parameters.VIEW_INDEX) : 0
viewSize = parameters.VIEW_SIZE ? Integer.valueOf(parameters.VIEW_SIZE) : 20

int lowIndex = viewIndex * viewSize + 1
int highIndex = (viewIndex + 1) * viewSize

context.viewIndexFirst = 0
context.viewIndex = viewIndex
context.viewIndexPrevious = viewIndex-1
context.viewIndexNext = viewIndex+1

listIt = from("FacilityLocation").where(cond).orderBy("nextCountDate").cursorScrollInsensitive().cache(true).queryIterator()
resultPartialList = listIt.getPartialList(lowIndex, highIndex - lowIndex + 1)

listSize = listIt.getResultsSizeAfterPartialList()
if (listSize < highIndex) {
    highIndex = listSize
}

context.highIndex = highIndex
context.listSize = listSize
context.resultPartialList = resultPartialList
context.viewIndexLast = UtilMisc.getViewLastIndex(listSize, viewSize)

def pendingLocations = [];
if (resultPartialList) {
    resultPartialList.each { pendingLocation ->
        def pendingLocationMap = [:];
        facilityId = pendingLocation?.facilityId;
        locationSeqId = pendingLocation?.locationSeqId;
        def facility = from("Facility").where("facilityId", facilityId).queryOne();
        pendingLocationMap.locationSeqId = locationSeqId;

        facilityLocation = from("FacilityLocation").where("facilityId", facilityId, "locationSeqId", locationSeqId).queryOne();
        pendingLocationMap.areaId = facilityLocation.areaId;
        pendingLocationMap.aisleId = facilityLocation.aisleId;
        pendingLocationMap.sectionId = facilityLocation.sectionId;
        pendingLocationMap.levelId = facilityLocation.levelId
        pendingLocationMap.positionId = facilityLocation.positionId;
        pendingLocationMap.facilityId = facilityLocation.facilityId;
        pendingLocationMap.facilityName = facility.facilityName;
        pendingLocationMap.lastCountDate = pendingLocation?.lastCountDate;
        pendingLocationMap.nextCountDate = pendingLocation?.nextCountDate;
        long count = from("InventoryItem").where("facilityId", facilityId, "locationSeqId", locationSeqId).queryCount();
        pendingLocationMap.totalInventoryItems = count
        if (pendingLocation?.lastCountDate) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pendingLocation?.lastCountDate);
            days = cal.get(Calendar.DAY_OF_YEAR);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(UtilDateTime.nowDate());
            nowDays = nowCal.get(Calendar.DAY_OF_YEAR);
            if (days && nowDays)
                pendingLocationMap.lastCountDay = (nowDays - days);
        }
        if (pendingLocation?.nextCountDate) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pendingLocation?.nextCountDate);
            days = cal.get(Calendar.DAY_OF_YEAR);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(UtilDateTime.nowDate());
            nowDays = nowCal.get(Calendar.DAY_OF_YEAR);
            if (days && nowDays)
                nextCountDay = (nowDays - days);
                pendingLocationMap.nextCountDay = nextCountDay * -1;
        }
        pendingLocations.add(pendingLocationMap);
    }
}

context.pendingLocations = pendingLocations;