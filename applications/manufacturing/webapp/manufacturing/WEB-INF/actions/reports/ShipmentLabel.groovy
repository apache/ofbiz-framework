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

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;

shipmentId = parameters.shipmentId;
shipment = delegator.findByPrimaryKey("Shipment", [shipmentId : shipmentId]);

context.shipmentIdPar = shipment.shipmentId;

if (shipment) {
    shipmentPackages = delegator.findByAnd("ShipmentPackage", [shipmentId : shipmentId]);
    records = [];
    orderReaders = [:];
    shipmentPackages.each { shipmentPackage ->
        shipmentPackageComponents = delegator.findByAnd("ShipmentPackageContent", [shipmentId : shipmentId, shipmentPackageSeqId : shipmentPackage.shipmentPackageSeqId]);
        shipmentPackageComponents.each { shipmentPackageComponent ->
            shipmentItem = shipmentPackageComponent.getRelatedOne("ShipmentItem");
            orderShipments = shipmentItem.getRelated("OrderShipment");
            orderShipment = EntityUtil.getFirst(orderShipments);

            String orderId = null;
            String orderItemSeqId = null;
            if (orderShipment) {
                orderId = orderShipment.orderId;
                orderItemSeqId = orderShipment.orderItemSeqId;
            }

            record = [:];
            if (shipmentPackageComponent.subProductId) {
                record.productId = shipmentPackageComponent.subProductId;
                record.quantity = shipmentPackageComponent.subQuantity;
            } else {
                record.productId = shipmentItem.productId;
                record.quantity = shipmentPackageComponent.quantity;
            }
            record.shipmentPackageSeqId = shipmentPackageComponent.shipmentPackageSeqId;
            record.orderId = orderId;
            record.orderItemSeqId = orderItemSeqId;
            product = delegator.findByPrimaryKey("Product", [productId : record.productId]);
            record.productName = product.internalName;
            record.shipDate = shipment.estimatedShipDate;
            // ---
            orderReadHelper = null;
            if (orderReaders.containsKey(orderId)) {
                orderReadHelper = (OrderReadHelper)orderReaders.get(orderId);
            } else {
                orderHeader = delegator.findByPrimaryKey("OrderHeader", [orderId : orderId]);
                orderReadHelper = new OrderReadHelper(orderHeader);
                orderReaders.put(orderId, orderReadHelper);
            }
            displayParty = orderReadHelper.getPlacingParty();
            shippingAddress = orderReadHelper.getShippingAddress();
            record.shippingAddressName = shippingAddress.toName;
            record.shippingAddressAddress = shippingAddress.address1;
            record.shippingAddressCity = shippingAddress.city;
            record.shippingAddressPostalCode = shippingAddress.postalCode;
            record.shippingAddressCountry = shippingAddress.countryGeoId;
            records.add(record);
        }
    }
    context.records = records;

    // check permission
    hasPermission = false;
    if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
        hasPermission = true;
    }
    context.hasPermission = hasPermission;
}

return "success";
