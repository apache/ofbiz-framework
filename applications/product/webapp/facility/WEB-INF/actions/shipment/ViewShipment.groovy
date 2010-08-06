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

import org.ofbiz.entity.condition.*

shipmentId = parameters.shipmentId;
if (!shipmentId) {
    shipmentId = request.getAttribute("shipmentId");
}
shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);

context.shipmentId = shipmentId;
context.shipment = shipment;

if (shipment) {
    context.shipmentType = shipment.getRelatedOne("ShipmentType");
    context.statusItem = shipment.getRelatedOne("StatusItem");
    context.primaryOrderHeader = shipment.getRelatedOne("PrimaryOrderHeader");
    context.toPerson = shipment.getRelatedOne("ToPerson");
    context.toPartyGroup = shipment.getRelatedOne("ToPartyGroup");
    context.fromPerson = shipment.getRelatedOne("FromPerson");
    context.fromPartyGroup = shipment.getRelatedOne("FromPartyGroup");
    context.originFacility = shipment.getRelatedOne("OriginFacility");
    context.destinationFacility = shipment.getRelatedOne("DestinationFacility");
    context.originPostalAddress = shipment.getRelatedOne("OriginPostalAddress");
    context.destinationPostalAddress = shipment.getRelatedOne("DestinationPostalAddress");
}

// check permission
hasPermission = false;
if (security.hasEntityPermission("FACILITY", "_VIEW", userLogin)) {
    hasPermission = true;
} else {
    if (shipment) {
        if (shipment.primaryOrderId) {
            // allow if userLogin is associated with the primaryOrderId with the SUPPLIER_AGENT roleTypeId
            orderRoleCheckMap = [orderId : shipment.primaryOrderId, partyId : userLogin.partyId, roleTypeId : 'SUPPLIER_AGENT'];
            orderRole = delegator.findOne("OrderRole", orderRoleCheckMap, false);
            if (orderRole) {
                hasPermission = true;
            }
        }
    }
}
context.hasPermission = hasPermission;
