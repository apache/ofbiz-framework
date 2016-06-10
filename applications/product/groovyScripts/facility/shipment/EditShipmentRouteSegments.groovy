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
import org.ofbiz.entity.condition.EntityCondition;

shipmentId = request.getParameter("shipmentId");
if (!shipmentId) {
    shipmentId = context.shipmentId;
}

shipment = null;
if (shipmentId) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
}

if (shipment) {
    shipmentRouteSegments = shipment.getRelated("ShipmentRouteSegment", null, ['shipmentRouteSegmentId'], false);
    shipmentRouteSegmentDatas = [] as LinkedList;
    if (shipmentRouteSegments) {
        shipmentRouteSegments.each { shipmentRouteSegment ->
            shipmentRouteSegmentData = [:];
            shipmentRouteSegmentData.shipmentRouteSegment = shipmentRouteSegment;
            shipmentRouteSegmentData.originFacility = shipmentRouteSegment.getRelatedOne("OriginFacility", false);
            shipmentRouteSegmentData.destFacility = shipmentRouteSegment.getRelatedOne("DestFacility", false);
            shipmentRouteSegmentData.originPostalAddress = shipmentRouteSegment.getRelatedOne("OriginPostalAddress", false);
            shipmentRouteSegmentData.originTelecomNumber = shipmentRouteSegment.getRelatedOne("OriginTelecomNumber", false);
            shipmentRouteSegmentData.destPostalAddress = shipmentRouteSegment.getRelatedOne("DestPostalAddress", false);
            shipmentRouteSegmentData.destTelecomNumber = shipmentRouteSegment.getRelatedOne("DestTelecomNumber", false);
            shipmentRouteSegmentData.shipmentMethodType = shipmentRouteSegment.getRelatedOne("ShipmentMethodType", false);
            shipmentRouteSegmentData.carrierPerson = shipmentRouteSegment.getRelatedOne("CarrierPerson", false);
            shipmentRouteSegmentData.carrierPartyGroup = shipmentRouteSegment.getRelatedOne("CarrierPartyGroup", false);
            shipmentRouteSegmentData.shipmentPackageRouteSegs = shipmentRouteSegment.getRelated("ShipmentPackageRouteSeg", null, null, false);
            shipmentRouteSegmentData.carrierServiceStatusItem = shipmentRouteSegment.getRelatedOne("CarrierServiceStatusItem", false);
            shipmentRouteSegmentData.currencyUom = shipmentRouteSegment.getRelatedOne("CurrencyUom", false);
            shipmentRouteSegmentData.billingWeightUom = shipmentRouteSegment.getRelatedOne("BillingWeightUom", false);
            if (shipmentRouteSegment.carrierServiceStatusId) {
                shipmentRouteSegmentData.carrierServiceStatusValidChangeToDetails = from("StatusValidChangeToDetail").where("statusId", shipmentRouteSegment.carrierServiceStatusId).orderBy("sequenceId").queryList();
            } else {
                shipmentRouteSegmentData.carrierServiceStatusValidChangeToDetails = from("StatusValidChangeToDetail").where("statusId", "SHRSCS_NOT_STARTED").orderBy("sequenceId").queryList();
            }
            shipmentRouteSegmentDatas.add(shipmentRouteSegmentData);
        }
    }

    shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false);
    facilities = from("Facility").orderBy("facilityName").queryList();
    shipmentMethodTypes = from("ShipmentMethodType").orderBy("description").queryList();
    weightUoms = from("Uom").where("uomTypeId", "WEIGHT_MEASURE").queryList();
    currencyUoms = from("Uom").where("uomTypeId", "CURRENCY_MEASURE").queryList();

    carrierPartyRoles = from("PartyRole").where("roleTypeId", "CARRIER").queryList();
    carrierPartyDatas = [] as LinkedList;
    carrierPartyRoles.each { carrierPartyRole ->
        party = carrierPartyRole.getRelatedOne("Party", false);
        carrierPartyData = [:];
        carrierPartyData.party = party;
        carrierPartyData.person = party.getRelatedOne("Person", false);
        carrierPartyData.partyGroup = party.getRelatedOne("PartyGroup", false);
        carrierPartyDatas.add(carrierPartyData);
    }

    context.shipment = shipment;
    context.shipmentRouteSegmentDatas = shipmentRouteSegmentDatas;
    context.shipmentPackages = shipmentPackages;
    context.facilities = facilities;
    context.shipmentMethodTypes = shipmentMethodTypes;
    context.weightUoms = weightUoms;
    context.currencyUoms = currencyUoms;
    context.carrierPartyDatas = carrierPartyDatas;
}
context.shipmentId = shipmentId;
context.nowTimestampString = UtilDateTime.nowTimestamp().toString();
