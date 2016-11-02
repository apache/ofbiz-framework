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
import org.apache.ofbiz.order.order.*
import org.apache.ofbiz.entity.util.EntityUtilProperties

facilityId = parameters.facilityId
if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne()
    context.facilityId = facilityId
    context.facility = facility
}

orderId = parameters.orderId
if (orderId) {
    orderHeader = from("OrderHeader").where("orderId", orderId).queryOne()
    if (orderHeader) {
        OrderReadHelper orh = new OrderReadHelper(orderHeader)
        context.orderId = orderId
        context.orderHeader = orderHeader
        context.orderReadHelper = orh
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "<li>Order #" + orderId + " cannot be found.")
    }
}

shipmentId = parameters.shipmentId
if (shipmentId) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne()
    if (shipment) {
        // nuke event message - throws off the flow
        request.setAttribute("_EVENT_MESSAGE_", null)

        // set the shipment context info
        context.shipmentType = shipment.getRelatedOne("ShipmentType", true)
        context.statusItem = shipment.getRelatedOne("StatusItem", false)
        context.primaryOrderHeader = shipment.getRelatedOne("PrimaryOrderHeader", false)
        context.toPerson = shipment.getRelatedOne("ToPerson", false)
        context.toPartyGroup = shipment.getRelatedOne("ToPartyGroup", false)
        context.fromPerson = shipment.getRelatedOne("FromPerson", false)
        context.fromPartyGroup = shipment.getRelatedOne("FromPartyGroup", false)
        context.originFacility = shipment.getRelatedOne("OriginFacility", false)
        context.destinationFacility = shipment.getRelatedOne("DestinationFacility", false)
        context.originPostalAddress = shipment.getRelatedOne("OriginPostalAddress", false)
        context.destinationPostalAddress = shipment.getRelatedOne("DestinationPostalAddress", false)
        context.shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false)
        context.shipmentRoutes = shipment.getRelated("ShipmentRouteSegment", null, ['shipmentRouteSegmentId'], false)
        context.shipment = shipment
        context.shipmentId = shipmentId

        weightUoms = from("Uom").where("uomTypeId", "WEIGHT_MEASURE").orderBy("description").queryList()
        defaultWeightUom = EntityUtilProperties.getPropertyValue("shipment", "shipment.default.weight.uom", delegator)
        if (defaultWeightUom) {
            defaultWeight = from("Uom").where("uomId", defaultWeightUom).queryOne()
            if (defaultWeight) {
                weightUoms.add(0, defaultWeight)
            }
        }
        context.weightUomList = weightUoms
    }
}
