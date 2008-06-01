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

import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.order.order.*;
import org.ofbiz.content.report.*;

delegator = request.getAttribute("delegator");
dispatcher = request.getAttribute("dispatcher");
userLogin = request.getSession().getAttribute("userLogin");

shipmentId = request.getParameter("shipmentId");
shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));

context.put("shipmentIdPar", shipment.getString("shipmentId"));

if (shipment != null) {    
    shipmentPackages = delegator.findByAnd("ShipmentPackage", UtilMisc.toMap("shipmentId", shipmentId));
    shipmentPackagesIt = shipmentPackages.iterator();
    records = new ArrayList();
    orderReaders = new HashMap();
    while(shipmentPackagesIt.hasNext()) {
        shipmentPackage = shipmentPackagesIt.next();

        shipmentPackageComponents = delegator.findByAnd("ShipmentPackageContent", UtilMisc.toMap("shipmentId", shipmentId, "shipmentPackageSeqId", shipmentPackage.getString("shipmentPackageSeqId")));
        shipmentPackageComponentsIt = shipmentPackageComponents.iterator();
        while(shipmentPackageComponentsIt.hasNext()) {
            shipmentPackageComponent = shipmentPackageComponentsIt.next();

            shipmentItem = shipmentPackageComponent.getRelatedOne("ShipmentItem");
            orderShipments = shipmentItem.getRelated("OrderShipment");
            orderShipment = EntityUtil.getFirst(orderShipments);
            
            String orderId = null;
            String orderItemSeqId = null;
            if (orderShipment != null) {            
                orderId = orderShipment.getString("orderId");
                orderItemSeqId = orderShipment.getString("orderItemSeqId");
            }

            record = new HashMap();
            if (shipmentPackageComponent.get("subProductId") != null) {
                record.put("productId", shipmentPackageComponent.getString("subProductId"));
                record.put("quantity", shipmentPackageComponent.getDouble("subQuantity"));
            } else {
                record.put("productId", shipmentItem.getString("productId"));
                record.put("quantity", shipmentPackageComponent.getDouble("quantity"));
            }
            record.put("shipmentPackageSeqId", shipmentPackageComponent.getString("shipmentPackageSeqId"));
            record.put("orderId", orderId);
            record.put("orderItemSeqId", orderItemSeqId);            
            product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", (String)record.get("productId")));
            record.put("productName", product.getString("internalName"));
            record.put("shipDate", shipment.getString("estimatedShipDate"));
            // ---
            orderReadHelper = null;
            if (orderReaders.containsKey(orderId)) {
                orderReadHelper = (OrderReadHelper)orderReaders.get(orderId);
            } else {
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                orderReadHelper = new OrderReadHelper(orderHeader);
                orderReaders.put(orderId, orderReadHelper);
            }
            displayParty = orderReadHelper.getPlacingParty();
            shippingAddress = orderReadHelper.getShippingAddress();
            record.put("shippingAddressName", shippingAddress.getString("toName"));
            record.put("shippingAddressAddress", shippingAddress.getString("address1"));
            record.put("shippingAddressCity", shippingAddress.getString("city"));
            record.put("shippingAddressPostalCode", shippingAddress.getString("postalCode"));
            record.put("shippingAddressCountry", shippingAddress.getString("countryGeoId"));
            records.add(record);
        }
    }
    context.put("records", records);
    
    // check permission
    hasPermission = false;
    if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
        hasPermission = true;
    } 
    context.put("hasPermission", hasPermission);    
}

return "success";
