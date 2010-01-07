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

import java.util.*
import java.sql.Timestamp
import org.ofbiz.entity.*
import org.ofbiz.entity.util.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.transaction.*
import org.ofbiz.base.util.*


lookupFlag = parameters.lookupFlag;
shipmentTypeId = parameters.shipmentTypeId;
originFacilityId = parameters.originFacilityId;
destinationFacilityId = parameters.destinationFacilityId;
statusId = parameters.statusId;
minDate = parameters.minDate;
maxDate = parameters.maxDate;

// set the page parameters
viewIndex = Integer.valueOf(parameters.VIEW_INDEX  ?: 0);
viewSize = Integer.valueOf(parameters.VIEW_SIZE ?: UtilProperties.getPropertyValue("widget", "widget.form.defaultViewSize"));
context.viewIndex = viewIndex;
context.viewSize = viewSize;

findShipmentExprs = [] as LinkedList;
paramListBuffer = new StringBuffer();

if (parameters.shipmentId) {
    findShipmentExprs.add(EntityCondition.makeCondition("shipmentId", EntityOperator.EQUALS, parameters.shipmentId));
}

if (shipmentTypeId) {
    paramListBuffer.append("&shipmentTypeId=");
    paramListBuffer.append(shipmentTypeId);
    findShipmentExprs.add(EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, shipmentTypeId));
    currentShipmentType = delegator.findOne("ShipmentType", [shipmentTypeId : shipmentTypeId], true);
    context.currentShipmentType = currentShipmentType;
}
if (originFacilityId) {
    paramListBuffer.append("&originFacilityId=");
    paramListBuffer.append(originFacilityId);
    findShipmentExprs.add(EntityCondition.makeCondition("originFacilityId", EntityOperator.EQUALS, originFacilityId));
    currentOriginFacility = delegator.findOne("Facility", [facilityId : originFacilityId], true);
    context.currentOriginFacility = currentOriginFacility;
}
if (destinationFacilityId) {
    paramListBuffer.append("&destinationFacilityId=");
    paramListBuffer.append(destinationFacilityId);
    findShipmentExprs.add(EntityCondition.makeCondition("destinationFacilityId", EntityOperator.EQUALS, destinationFacilityId));
    currentDestinationFacility = delegator.findOne("Facility", [facilityId : destinationFacilityId], true);
    context.currentDestinationFacility = currentDestinationFacility;
}
if (statusId) {
    paramListBuffer.append("&statusId=");
    paramListBuffer.append(statusId);
    findShipmentExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
    currentStatus = delegator.findOne("StatusItem", [statusId : statusId], true);
    context.currentStatus = currentStatus;
}
if (minDate && minDate.length() > 8) {
    minDate = minDate.trim();
    if (minDate.length() < 14) {
        minDate = minDate + " " + "00:00:00.000";
    }
    paramListBuffer.append("&minDate=");
    paramListBuffer.append(minDate);
    findShipmentExprs.add(EntityCondition.makeCondition("estimatedShipDate", EntityOperator.GREATER_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null)));
}
if (maxDate && maxDate.length() > 8) {
    maxDate = maxDate.trim();
    if (maxDate.length() < 14) {
        maxDate = maxDate + " " + "23:59:59.999";
    }
    paramListBuffer.append("&maxDate=");
    paramListBuffer.append(maxDate);
    findShipmentExprs.add(EntityCondition.makeCondition("estimatedShipDate", EntityOperator.LESS_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null)));
}

if ("Y".equals(lookupFlag)) {
    context.paramList = paramListBuffer.toString();

    findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
    mainCond = null;
    if (findShipmentExprs.size() > 0) {
        mainCond = EntityCondition.makeCondition(findShipmentExprs, EntityOperator.AND);
    }
    orderBy = ['-estimatedShipDate'];

    beganTransaction = false;
    try {
        beganTransaction = TransactionUtil.begin();

        // get the indexes for the partial list
        lowIndex = viewIndex * viewSize + 1;
        highIndex = (viewIndex + 1) * viewSize;
        findOpts.setMaxRows(highIndex);
        // using list iterator
        orli = delegator.find("Shipment", mainCond, null, null, orderBy, findOpts);

        shipmentListSize = orli.getResultsSizeAfterPartialList();
        if (highIndex > shipmentListSize) {
            highIndex = shipmentListSize;
        }

        // get the partial list for this page
        if (shipmentListSize > 0) {
            shipmentList = orli.getPartialList(lowIndex, viewSize);
        } else {
            shipmentList = [] as ArrayList;
        }

        // close the list iterator
        orli.close();
    } catch (GenericEntityException e) {
        errMsg = "Failure in operation, rolling back transaction";
        Debug.logError(e, errMsg, module);
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, errMsg, e);
        } catch (GenericEntityException e2) {
            Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
        }
        // after rolling back, rethrow the exception
        throw e;
    } finally {
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction);
    }

    context.shipmentList = shipmentList;
    context.listSize = shipmentListSize;
    context.highIndex = highIndex;
    context.lowIndex = lowIndex;
}

// =============== Prepare the Option Data for the Find Form =================

context.shipmentTypes = delegator.findList("ShipmentType", null, null, ['description'], null, false);

context.facilities = delegator.findList("Facility", null, null, ['facilityName'], null, false);

// since purchase and sales shipments have different status codes, we'll need to make two separate lists
context.shipmentStatuses = delegator.findList("StatusItem", EntityCondition.makeCondition([statusTypeId : 'SHIPMENT_STATUS']), null, ['sequenceId'], null, false);
context.purchaseShipmentStatuses = delegator.findList("StatusItem", EntityCondition.makeCondition([statusTypeId : 'PURCH_SHIP_STATUS']), null, ['sequenceId'], null, false);

// create the fromDate for calendar
fromCal = Calendar.getInstance();
fromCal.setTimeInMillis(System.currentTimeMillis());
//fromCal.set(Calendar.DAY_OF_WEEK, fromCal.getActualMinimum(Calendar.DAY_OF_WEEK));
fromCal.set(Calendar.HOUR_OF_DAY, fromCal.getActualMinimum(Calendar.HOUR_OF_DAY));
fromCal.set(Calendar.MINUTE, fromCal.getActualMinimum(Calendar.MINUTE));
fromCal.set(Calendar.SECOND, fromCal.getActualMinimum(Calendar.SECOND));
fromCal.set(Calendar.MILLISECOND, fromCal.getActualMinimum(Calendar.MILLISECOND));
fromTs = new Timestamp(fromCal.getTimeInMillis());
fromStr = fromTs.toString();
fromStr = fromStr.substring(0, fromStr.indexOf('.'));
context.fromDateStr = fromStr;

// create the thruDate for calendar
toCal = Calendar.getInstance();
toCal.setTimeInMillis(System.currentTimeMillis());
//toCal.set(Calendar.DAY_OF_WEEK, toCal.getActualMaximum(Calendar.DAY_OF_WEEK));
toCal.set(Calendar.HOUR_OF_DAY, toCal.getActualMaximum(Calendar.HOUR_OF_DAY));
toCal.set(Calendar.MINUTE, toCal.getActualMaximum(Calendar.MINUTE));
toCal.set(Calendar.SECOND, toCal.getActualMaximum(Calendar.SECOND));
toCal.set(Calendar.MILLISECOND, toCal.getActualMaximum(Calendar.MILLISECOND));
toTs = new Timestamp(toCal.getTimeInMillis());
toStr = toTs.toString();
context.thruDateStr = toStr;