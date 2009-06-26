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

import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;

module = "FindOrders.groovy";

// get the order types
orderTypes = delegator.findList("OrderType", null, null, ["description"], null, false);
context.orderTypes = orderTypes;

// get the role types
roleTypes = delegator.findList("RoleType", null, null, ["description"], null, false);
context.roleTypes = roleTypes;

// get the order statuses
orderStatuses = delegator.findByAnd("StatusItem", [statusTypeId : "ORDER_STATUS"], ["sequenceId", "description"]);
context.orderStatuses = orderStatuses;

// get websites
websites = delegator.findList("WebSite", null, null, ["siteName"], null, false);
context.webSites = websites;

// get the stores
stores = delegator.findList("ProductStore", null, null, ["storeName"], null, false);
context.productStores = stores;

// get the channels
channels = delegator.findByAnd("Enumeration", [enumTypeId : "ORDER_SALES_CHANNEL"], ["sequenceId"]);
context.salesChannels = channels;

// get the Shipping Methods
carrierShipmentMethods = delegator.findList("CarrierShipmentMethod", null, null, null, null, false);
context.carrierShipmentMethods = carrierShipmentMethods;

// current role type
currentRoleTypeId = request.getParameter("roleTypeId");
if (currentRoleTypeId) {
    currentRole = delegator.findByPrimaryKeyCache("RoleType", [roleTypeId : currentRoleTypeId]);
    context.currentRole = currentRole;
}

// current selected type
currentTypeId = request.getParameter("orderTypeId");
if (currentTypeId) {
    currentType = delegator.findByPrimaryKeyCache("OrderType", [orderTypeId : currentTypeId]);
    context.currentType = currentType;
}
// current selected status
currentStatusId = request.getParameter("orderStatusId");
if (currentStatusId) {
    currentStatus = delegator.findByPrimaryKeyCache("StatusItem", [statusId : currentStatusId]);
    context.currentStatus = currentStatus;
}

// current website
currentWebSiteId = request.getParameter("orderWebSiteId");
if (currentWebSiteId) {
    currentWebSite = delegator.findByPrimaryKeyCache("WebSite", [webSiteId : currentWebSiteId]);
    context.currentWebSite = currentWebSite;
}

// current store
currentProductStoreId = request.getParameter("productStoreId");
if (currentProductStoreId) {
    currentProductStore = delegator.findByPrimaryKeyCache("ProductStore", [productStoreId : currentProductStoreId]);
    context.currentProductStore = currentProductStore;
}

// current Shipping Method
shipmentMethod = request.getParameter("shipmentMethod");
if (shipmentMethod) {
    carrierPartyId = shipmentMethod.substring(0, shipmentMethod.indexOf("@"));
    shipmentMethodTypeId = shipmentMethod.substring(shipmentMethod.indexOf("@")+1);
    if (carrierPartyId && shipmentMethodTypeId) {
        currentCarrierShipmentMethod = delegator.findByAnd("CarrierShipmentMethod", [partyId : carrierPartyId, shipmentMethodTypeId : shipmentMethodTypeId]);
        context.currentCarrierShipmentMethod = currentCarrierShipmentMethod;
    }
}

// current channel
currentSalesChannelId = request.getParameter("salesChannelEnumId");
if (currentSalesChannelId) {
    currentSalesChannel = delegator.findByPrimaryKey("Enumeration", [enumId : currentSalesChannelId]);
    context.currentSalesChannel = currentSalesChannel;
}

// create the fromDate for calendar
fromCal = Calendar.getInstance();
fromCal.setTime(new java.util.Date());
fromCal.set(Calendar.DAY_OF_WEEK, fromCal.getActualMinimum(Calendar.DAY_OF_WEEK));
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
toCal.setTime(new java.util.Date());
toCal.set(Calendar.DAY_OF_WEEK, toCal.getActualMaximum(Calendar.DAY_OF_WEEK));
toCal.set(Calendar.HOUR_OF_DAY, toCal.getActualMaximum(Calendar.HOUR_OF_DAY));
toCal.set(Calendar.MINUTE, toCal.getActualMaximum(Calendar.MINUTE));
toCal.set(Calendar.SECOND, toCal.getActualMaximum(Calendar.SECOND));
toCal.set(Calendar.MILLISECOND, toCal.getActualMaximum(Calendar.MILLISECOND));
toTs = new Timestamp(toCal.getTimeInMillis());
toStr = toTs.toString();
context.thruDateStr = toStr;

// set the page parameters
viewIndex = request.getParameter("viewIndex") ? Integer.valueOf(request.getParameter("viewIndex")) : 1;
context.viewIndex = viewIndex;

viewSize = request.getParameter("viewSize") ? Integer.valueOf(request.getParameter("viewSize")) : 20;
context.viewSize = viewSize;

// get the lookup flag
lookupFlag = request.getParameter("lookupFlag");

// fields from the service call
paramList = request.getAttribute("paramList") ?: "";
context.paramList = paramList;

if (paramList) {
    paramIds = paramList.split("&amp;");
    context.paramIdList = Arrays.asList(paramIds);
}

orderList = request.getAttribute("orderList");
context.orderList = orderList;

orderListSize = request.getAttribute("orderListSize");
context.orderListSize = orderListSize;

context.filterInventoryProblems = request.getAttribute("filterInventoryProblemsList");
context.filterPOsWithRejectedItems = request.getAttribute("filterPOsWithRejectedItemsList");
context.filterPOsOpenPastTheirETA = request.getAttribute("filterPOsOpenPastTheirETAList");
context.filterPartiallyReceivedPOs = request.getAttribute("filterPartiallyReceivedPOsList");

lowIndex = request.getAttribute("lowIndex");
context.lowIndex = lowIndex;

highIndex = request.getAttribute("highIndex");
context.highIndex = highIndex;
