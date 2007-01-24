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
import org.ofbiz.base.util.*;
import org.ofbiz.content.report.*;

delegator = request.getAttribute("delegator");
security = request.getAttribute("security");
dispatcher = request.getAttribute("dispatcher");
userLogin = request.getSession().getAttribute("userLogin");

inventoryStock = new HashMap();
shipmentId = request.getParameter("shipmentId");
shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));

context.put("shipmentIdPar", shipment.getString("shipmentId"));
context.put("estimatedReadyDatePar", shipment.getString("estimatedReadyDate"));
context.put("estimatedShipDatePar", shipment.getString("estimatedShipDate"));

if (shipment != null) {
    shipmentPlans = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId));
    shipmentPlansIt = shipmentPlans.iterator();
    records = new ArrayList();

    while(shipmentPlansIt.hasNext()) {
        shipmentPlan = shipmentPlansIt.next();
        orderLine = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", shipmentPlan.getString("orderId"), "orderItemSeqId", shipmentPlan.getString("orderItemSeqId")));
        recordGroup = new HashMap();
        recordGroup.put("ORDER_ID", shipmentPlan.getString("orderId"));
        recordGroup.put("ORDER_ITEM_SEQ_ID", shipmentPlan.getString("orderItemSeqId"));
        recordGroup.put("SHIPMENT_ID", shipmentPlan.getString("shipmentId"));
        recordGroup.put("SHIPMENT_ITEM_SEQ_ID", shipmentPlan.getString("shipmentItemSeqId"));

        recordGroup.put("PRODUCT_ID", orderLine.getString("productId"));
        recordGroup.put("QUANTITY", shipmentPlan.getDouble("quantity"));
        product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", orderLine.getString("productId")));
        recordGroup.put("PRODUCT_NAME", product.getString("internalName"));
     
        Map inputPar = UtilMisc.toMap("productId", orderLine.getString("productId"),
                                     "quantity", shipmentPlan.getDouble("quantity"),
                                     "fromDate", "" + new Date(),
                                     "userLogin", userLogin);
                            
        Map result = null;
        result = dispatcher.runSync("getNotAssembledComponents",inputPar);
        if (result != null)
            components = (List)result.get("notAssembledComponents");
        componentsIt = components.iterator();
        while(componentsIt.hasNext()) {
            oneComponent = (org.ofbiz.manufacturing.bom.BOMNode)componentsIt.next();
            record = new HashMap(recordGroup);
            record.put("componentId", oneComponent.getProduct().getString("productId"));
            record.put("componentName", oneComponent.getProduct().getString("internalName"));
            record.put("componentQuantity", new Float(oneComponent.getQuantity()));
            facilityId = shipment.getString("originFacilityId");
            float qty = 0;
            if (facilityId != null) {
                if (!inventoryStock.containsKey(oneComponent.getProduct().getString("productId"))) {
                    serviceInput = UtilMisc.toMap("productId",oneComponent.getProduct().getString("productId"), "facilityId", facilityId);
                    serviceOutput = dispatcher.runSync("getInventoryAvailableByFacility",serviceInput);
                    qha = serviceOutput.get("quantityOnHandTotal");
                    if (qha == null) qha = new Double(0);
                    inventoryStock.put(oneComponent.getProduct().getString("productId"), qha);
                }
                qty = ((Double)inventoryStock.get(oneComponent.getProduct().getString("productId"))).floatValue();
                qty = (float)(qty - oneComponent.getQuantity());
                inventoryStock.put(oneComponent.getProduct().getString("productId"), new Double(qty));
            }
            record.put("componentOnHand", new Float(qty));
            // Now we get the products qty already reserved by production runs
            serviceInput = UtilMisc.toMap("productId", oneComponent.getProduct().getString("productId"),
                                          "userLogin", userLogin);
            serviceOutput = dispatcher.runSync("getProductionRunTotResQty", serviceInput);
            resQty = serviceOutput.get("reservedQuantity");
            record.put("reservedQuantity", resQty);
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
