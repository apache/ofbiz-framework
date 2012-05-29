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

import org.ofbiz.base.util.*
import org.ofbiz.order.order.*

facilityId = parameters.facilityId;
if (facilityId) {
    facility = delegator.findOne("Facility", [facilityId : facilityId], false);
    context.facilityId = facilityId;
    context.facility = facility;
}

orderId = parameters.orderId
if (orderId) {
    orderHeader = delegator.findOne("OrderHeader", [orderId : orderId], false);
    if (orderHeader) {
        OrderReadHelper orh = new OrderReadHelper(orderHeader);
        context.orderId = orderId;
        context.orderHeader = orderHeader;
        context.orderReadHelper = orh;
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "<li>Order #" + orderId + " cannot be found.");
    }
}

shipmentId = parameters.shipmentId;
if (shipmentId) {
    shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
    if (shipment) {
        // nuke event message - throws off the flow
        request.setAttribute("_EVENT_MESSAGE_", null);

        // set the shipment context info
        context.shipmentType = shipment.getRelatedOne("ShipmentType", true);
        context.statusItem = shipment.getRelatedOne("StatusItem", false);
        context.primaryOrderHeader = shipment.getRelatedOne("PrimaryOrderHeader", false);
        context.toPerson = shipment.getRelatedOne("ToPerson", false);
        context.toPartyGroup = shipment.getRelatedOne("ToPartyGroup", false);
        context.fromPerson = shipment.getRelatedOne("FromPerson", false);
        context.fromPartyGroup = shipment.getRelatedOne("FromPartyGroup", false);
        context.originFacility = shipment.getRelatedOne("OriginFacility", false);
        context.destinationFacility = shipment.getRelatedOne("DestinationFacility", false);
        context.originPostalAddress = shipment.getRelatedOne("OriginPostalAddress", false);
        context.destinationPostalAddress = shipment.getRelatedOne("DestinationPostalAddress", false);
        context.shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false);
        context.shipmentRoutes = shipment.getRelated("ShipmentRouteSegment", null, ['shipmentRouteSegmentId'], false);
        context.shipment = shipment;
        context.shipmentId = shipmentId;

        weightUoms = delegator.findList("Uom", EntityCondition.makeCondition(['uomTypeId' : 'WEIGHT_MEASURE']), null, ['description'], null, false);
        defaultWeightUom = UtilProperties.getPropertyValue("shipment.properties", "shipment.default.weight.uom");
        if (defaultWeightUom) {
            defaultWeight = delegator.findOne("Uom", [uomId : defaultWeightUom], false);
            if (defaultWeight) {
                weightUoms.add(0, defaultWeight);
            }
        }
        context.weightUomList = weightUoms;
    }
}
