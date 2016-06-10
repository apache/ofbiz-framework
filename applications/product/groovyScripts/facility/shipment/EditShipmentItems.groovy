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

import org.ofbiz.entity.*
import org.ofbiz.base.util.*

shipmentId = request.getParameter("shipmentId");
if (!shipmentId) {
    shipmentId = context.shipmentId;
}

shipment = null;
if (shipmentId) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne();
}

if (shipment) {
    shipmentItems = shipment.getRelated("ShipmentItem", null, ['shipmentItemSeqId'], false);
    shipmentItemDatas = [] as LinkedList;
    if (shipmentItems) {
        shipmentItems.each { shipmentItem ->
            shipmentPackageContents = shipmentItem.getRelated("ShipmentPackageContent", null, null, false);
            totalQuantityPackaged = 0;
            shipmentPackageContents.each { shipmentPackageContent ->
                if (shipmentPackageContent.quantity) {
                    totalQuantityPackaged += shipmentPackageContent.getDouble("quantity");
                }
            }

            totalQuantityToPackage = 0;
            if (shipmentItem.quantity) {
                totalQuantityToPackage = shipmentItem.getDouble("quantity") - totalQuantityPackaged;
            }

            shipmentItemData = [:];
            shipmentItemData.shipmentItem = shipmentItem;
            shipmentItemData.shipmentPackageContents = shipmentPackageContents;
            shipmentItemData.itemIssuances = shipmentItem.getRelated("ItemIssuance", null, null, false);
            shipmentItemData.orderShipments = shipmentItem.getRelated("OrderShipment", null, null, false);
            shipmentItemData.product = shipmentItem.getRelatedOne("Product", false);
            shipmentItemData.totalQuantityPackaged = totalQuantityPackaged;
            shipmentItemData.totalQuantityToPackage = totalQuantityToPackage;
            shipmentItemDatas.add(shipmentItemData);
        }
    }
    shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId'], false);

    context.shipment = shipment;
    context.shipmentItemDatas = shipmentItemDatas;
    context.shipmentPackages = shipmentPackages;
}
context.shipmentId = shipmentId;
