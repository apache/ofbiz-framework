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

shipmentId = request.getParameter("shipmentId");
if (!shipmentId) {
    shipmentId = context.shipmentId;
}

shipment = null;
if (shipmentId) {
    shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
}

if (shipment) {
    shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId']);
    shipmentPackageDatas = [] as LinkedList;
    if (shipmentPackages) {
        shipmentPackages.each { shipmentPackage ->
            shipmentPackageData = [:];
            shipmentPackageData.shipmentPackage = shipmentPackage;
            shipmentPackageData.shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent");
            shipmentPackageData.shipmentPackageRouteSegs = shipmentPackage.getRelated("ShipmentPackageRouteSeg");
            shipmentPackageData.weightUom = shipmentPackage.getRelatedOne("WeightUom");
            shipmentPackageDatas.add(shipmentPackageData);
        }
    }

    shipmentItems = shipment.getRelated("ShipmentItem", null, ['shipmentItemSeqId']);
    shipmentRouteSegments = shipment.getRelated("ShipmentRouteSegment", null, ['shipmentRouteSegmentId']);
    weightUoms = delegator.findList("Uom", EntityCondition.makeCondition([uomTypeId : 'WEIGHT_MEASURE']), null, ['description'], null, false);
    boxTypes = delegator.findList("ShipmentBoxType", null, null, null, null, false);

    context.shipment = shipment;
    context.shipmentPackageDatas = shipmentPackageDatas;
    context.shipmentItems = shipmentItems;
    context.shipmentRouteSegments = shipmentRouteSegments;
    context.weightUoms = weightUoms;
    context.boxTypes = boxTypes;
}
context.shipmentId = shipmentId;
