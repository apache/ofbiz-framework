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
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
}

if (shipment) {
    shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false);
    shipmentPackageDatas = [] as LinkedList;
    if (shipmentPackages) {
        shipmentPackages.each { shipmentPackage ->
            shipmentPackageData = [:];
            shipmentPackageData.shipmentPackage = shipmentPackage;
            shipmentPackageData.shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent", null, null, false);
            shipmentPackageData.shipmentPackageRouteSegs = shipmentPackage.getRelated("ShipmentPackageRouteSeg", null, null, false);
            shipmentPackageData.weightUom = shipmentPackage.getRelatedOne("WeightUom", false);
            shipmentPackageDatas.add(shipmentPackageData);
        }
    }

    shipmentItems = shipment.getRelated("ShipmentItem", null, ['shipmentItemSeqId'], false);
    shipmentRouteSegments = shipment.getRelated("ShipmentRouteSegment", null, ['shipmentRouteSegmentId'], false);
    weightUoms = from("Uom").where("uomTypeId", "WEIGHT_MEASURE").orderBy("description").queryList();
    boxTypes = from("ShipmentBoxType").queryList();

    context.shipment = shipment;
    context.shipmentPackageDatas = shipmentPackageDatas;
    context.shipmentItems = shipmentItems;
    context.shipmentRouteSegments = shipmentRouteSegments;
    context.weightUoms = weightUoms;
    context.boxTypes = boxTypes;
}
context.shipmentId = shipmentId;
