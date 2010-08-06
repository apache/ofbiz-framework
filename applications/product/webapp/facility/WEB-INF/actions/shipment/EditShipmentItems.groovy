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
    shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
}

if (shipment) {
    shipmentItems = shipment.getRelated("ShipmentItem", null, ['shipmentItemSeqId']);
    shipmentItemDatas = [] as LinkedList;
    if (shipmentItems) {
        shipmentItems.each { shipmentItem ->
            shipmentPackageContents = shipmentItem.getRelated("ShipmentPackageContent");
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
            shipmentItemData.itemIssuances = shipmentItem.getRelated("ItemIssuance");
            shipmentItemData.orderShipments = shipmentItem.getRelated("OrderShipment");
            shipmentItemData.product = shipmentItem.getRelatedOne("Product");
            shipmentItemData.totalQuantityPackaged = totalQuantityPackaged;
            shipmentItemData.totalQuantityToPackage = totalQuantityToPackage;
            shipmentItemDatas.add(shipmentItemData);
        }
    }
    shipmentPackages = shipment.getRelated("ShipmentPackage", null, ['shipmentPackageSeqId']);

    context.shipment = shipment;
    context.shipmentItemDatas = shipmentItemDatas;
    context.shipmentPackages = shipmentPackages;
}
context.shipmentId = shipmentId;
