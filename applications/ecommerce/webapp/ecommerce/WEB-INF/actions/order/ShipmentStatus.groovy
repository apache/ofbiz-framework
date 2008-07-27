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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

shipmentId = parameters.shipmentId;
if (shipmentId) {
    shipment = delegator.findByPrimaryKey("Shipment", [shipmentId : shipmentId]);
    shipmentItems = delegator.findByAnd("ShipmentItem", [shipmentId : shipmentId]);

    // get Shipment tracking info
    osisCond = EntityCondition.makeCondition([shipmentId : shipmentId], EntityOperator.AND);
    osisOrder = ["shipmentId", "shipmentRouteSegmentId", "shipmentPackageSeqId"];
    osisFields = ["shipmentId", "shipmentRouteSegmentId", "shipmentPackageSeqId", "carrierPartyId", "trackingCode"] as Set;
    osisFindOptions = new EntityFindOptions();
    osisFindOptions.setDistinct(true);
    orderShipmentInfoSummaryList = delegator.findList("OrderShipmentInfoSummary", osisCond, osisFields, osisOrder, osisFindOptions, false);

    context.shipment = shipment;
    context.shipmentItems = shipmentItems;
    context.orderShipmentInfoSummaryList = orderShipmentInfoSummaryList;
}
