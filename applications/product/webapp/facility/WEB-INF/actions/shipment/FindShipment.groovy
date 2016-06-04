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
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAlias;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasField;
import org.ofbiz.entity.model.ModelViewEntity.ComplexAliasMember;

lookupFlag = parameters.lookupFlag;
shipmentTypeId = parameters.shipmentTypeId;
originFacilityId = parameters.originFacilityId;
destinationFacilityId = parameters.destinationFacilityId;
statusId = parameters.statusId;
minDate = parameters.minDate;
maxDate = parameters.maxDate;

// set the page parameters
viewIndex = Integer.valueOf(parameters.VIEW_INDEX  ?: 0);
viewSize = Integer.valueOf(parameters.VIEW_SIZE ?: EntityUtilProperties.getPropertyValue("widget", "widget.form.defaultViewSize", "20", delegator));
context.viewIndex = viewIndex;
context.viewSize = viewSize;

findShipmentExprs = [] as LinkedList;
paramListBuffer = new StringBuffer();

orderReturnValue = false;
if (UtilValidate.isNotEmpty(statusId) && statusId.startsWith("RETURN_")) {
    orderReturnValue = true;
}
if (parameters.shipmentId) {
    findShipmentExprs.add(EntityCondition.makeCondition("shipmentId", EntityOperator.LIKE, "%"+parameters.shipmentId+"%"));
}

if (shipmentTypeId) {
    paramListBuffer.append("&shipmentTypeId=");
    paramListBuffer.append(shipmentTypeId);
    findShipmentExprs.add(EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, shipmentTypeId));
    currentShipmentType = from("ShipmentType").where("shipmentTypeId", shipmentTypeId).cache(true).queryOne();
    context.currentShipmentType = currentShipmentType;
}
if (originFacilityId) {
    paramListBuffer.append("&originFacilityId=");
    paramListBuffer.append(originFacilityId);
    findShipmentExprs.add(EntityCondition.makeCondition("originFacilityId", EntityOperator.EQUALS, originFacilityId));
    currentOriginFacility = from("Facility").where("facilityId", originFacilityId).cache(true).queryOne();
    context.currentOriginFacility = currentOriginFacility;
}
if (destinationFacilityId) {
    paramListBuffer.append("&destinationFacilityId=");
    paramListBuffer.append(destinationFacilityId);
    findShipmentExprs.add(EntityCondition.makeCondition("destinationFacilityId", EntityOperator.EQUALS, destinationFacilityId));
    currentDestinationFacility = from("Facility").where("facilityId", destinationFacilityId).cache(true).queryOne();
    context.currentDestinationFacility = currentDestinationFacility;
}
if (statusId) {
    paramListBuffer.append("&statusId=");
    paramListBuffer.append(statusId);
    if (!orderReturnValue) {
        findShipmentExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId));
    }
    currentStatus = from("StatusItem").where("statusId", statusId).cache(true).queryOne();
    context.currentStatus = currentStatus;
}
if (minDate && minDate.length() > 8) {
    minDate = minDate.trim();
    if (minDate.length() < 14) {
        minDate = minDate + " " + "00:00:00.000";
    }
    paramListBuffer.append("&minDate=");
    paramListBuffer.append(minDate);
    if (orderReturnValue) {
        findShipmentExprs.add(EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null)));
    } else {
        findShipmentExprs.add(EntityCondition.makeCondition("estimatedShipDate", EntityOperator.GREATER_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null)));
    }
    
}
if (maxDate && maxDate.length() > 8) {
    maxDate = maxDate.trim();
    if (maxDate.length() < 14) {
        maxDate = maxDate + " " + "23:59:59.999";
    }
    paramListBuffer.append("&maxDate=");
    paramListBuffer.append(maxDate);
    if (orderReturnValue) {
        findShipmentExprs.add(EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null)));
    } else {
        findShipmentExprs.add(EntityCondition.makeCondition("estimatedShipDate", EntityOperator.LESS_THAN_EQUAL_TO, ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null)));
    }
    
}

if ("Y".equals(lookupFlag)) {
    context.paramList = paramListBuffer.toString();

    beganTransaction = false;
    try {
        beganTransaction = TransactionUtil.begin();

        // get the indexes for the partial list
        lowIndex = viewIndex * viewSize + 1;
        highIndex = (viewIndex + 1) * viewSize;
        
        if (!orderReturnValue) {
            // using list iterator
            if (findShipmentExprs.size() > 0) {
                orli = from("Shipment").where(EntityCondition.makeCondition(findShipmentExprs, EntityOperator.AND)).orderBy("-estimatedShipDate").cursorScrollInsensitive().distinct().maxRows(highIndex).queryIterator();
            } else {
                orli = from("Shipment").orderBy("-estimatedShipDate").cursorScrollInsensitive().distinct().maxRows(highIndex).queryIterator();
            }
    
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
        }
        
        if (orderReturnValue) {
            returnCond = null;
            findShipmentExprs.add(EntityCondition.makeCondition("returnStatusId", EntityOperator.EQUALS, statusId));
            returnCond = EntityCondition.makeCondition(findShipmentExprs, EntityOperator.AND);
            OrderReturnViewEntity = new DynamicViewEntity();
            OrderReturnViewEntity.addMemberEntity("SM", "Shipment");
            OrderReturnViewEntity.addMemberEntity("RH", "ReturnHeader");
            OrderReturnViewEntity.addViewLink("SM", "RH", false, ModelKeyMap.makeKeyMapList("primaryReturnId", "returnId"));
            OrderReturnViewEntity.addAlias("SM", "shipmentId");
            OrderReturnViewEntity.addAlias("SM", "shipmentTypeId");
            OrderReturnViewEntity.addAlias("SM", "primaryReturnId");
            OrderReturnViewEntity.addAlias("SM", "destinationFacilityId");
            OrderReturnViewEntity.addAlias("SM", "originFacilityId");
            OrderReturnViewEntity.addAlias("SM", "estimatedShipDate");
            OrderReturnViewEntity.addAlias("SM", "statusId");
            OrderReturnViewEntity.addAlias("RH", "returnId");
            OrderReturnViewEntity.addAlias("RH", "entryDate");
            OrderReturnViewEntity.addAlias("RH", "returnStatusId", "statusId", null, null, null, null);
            
            orderReturnIt = from(OrderReturnViewEntity).where(returnCond).queryList();
            shipmentListSize = orderReturnIt.getResultsSizeAfterPartialList();
            
            if (highIndex > shipmentListSize) {
                highIndex = shipmentListSize;
            }
            
            // get the partial list for this page
            if (shipmentListSize > 0) {
                shipmentList = orderReturnIt.getPartialList(lowIndex, viewSize);
            } else {
                shipmentList = [] as ArrayList;
            }
            orderReturnIt.close();
        }
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

context.shipmentTypes = from("ShipmentType").orderBy("description").queryList();

context.facilities = from("Facility").orderBy("facilityName").queryList();

// since purchase and sales shipments have different status codes, we'll need to make two separate lists
context.shipmentStatuses = from("StatusItem").where("statusTypeId", "SHIPMENT_STATUS").orderBy("sequenceId").queryList();
context.purchaseShipmentStatuses = from("StatusItem").where("statusTypeId", "PURCH_SHIP_STATUS").orderBy("sequenceId").queryList();

/// Get return status lists
context.returnStatuses = from("StatusItem").where("statusTypeId", "ORDER_RETURN_STTS").orderBy("sequenceId").queryList();

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
