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

import java.awt.Dimension
import org.ofbiz.entity.util.*
import org.ofbiz.manufacturing.jobshopmgt.ProductionRun
import org.ofbiz.manufacturing.jobshopmgt.ProductionRunHelper

if (security.hasEntityPermission("MANUFACTURING", "_VIEW", session)) {
    context.hasPermission = Boolean.TRUE;
} else {
    context.hasPermission = Boolean.FALSE;
}

// -----------------------------
// Report's parameters
//groupByFeatureTypeIdParameter = "COLOR";
selectWorkEffortNameParameter = "O-PREL_A"; // sezionatura
selectPrimaryCategoryIdParameter = "CABINETS"; // struttura
// -----------------------------

shipmentId = request.getParameter("shipmentId");
context.shipmentId = shipmentId;

shipment = delegator.findOne("Shipment", [shipmentId : shipmentId], false);
context.shipment = shipment;

// dimensionsByFeatureMap [key=feature; value=productsByShapeMap]
// productsByShapeMap [key=dimension; value=quantityByProductsMap]
// quantityByProductsMap [key=product; value=quantity]
// dimension={width*, height*, qty}
// product={productId*,...}

productIdToQuantity = [:]; // key=productId, value=quantity
productIdToProduct = [:]; // key=productId, value=product
dimensionToProducts = [:]; // key=Dimension, value=list of products
dimensionToQuantity = [:]; // key=Dimension, value=tot qty (of products)
    
shipmentPlans = from("OrderShipment").where("shipmentId", shipmentId).queryList()

if (shipmentPlans) {
    shipmentPlans.each { shipmentPlan ->
        // Select the production run, if available
        weIds = from("WorkOrderItemFulfillment").where("orderId", shipmentPlan.orderId, "orderItemSeqId", shipmentPlan.orderItemSeqId).orderBy("workEffortId").queryList(); // TODO: add shipmentId
        weId = EntityUtil.getFirst(weIds);
        productionRunTree = [] as ArrayList;
        // TODO
        if (weId) {
            ProductionRunHelper.getLinkedProductionRuns(delegator, dispatcher, weId.workEffortId, productionRunTree);
            for (int i = 0; i < productionRunTree.size(); i++) {
                oneProductionRun = (ProductionRun)productionRunTree.get(i);
                if (ProductionRunHelper.hasTask(delegator, selectWorkEffortNameParameter, oneProductionRun.getGenericValue().workEffortId)) {
                    product = oneProductionRun.getProductProduced();
                    primaryCategory = product.primaryProductCategoryId;
                    if (primaryCategory && selectPrimaryCategoryIdParameter.equals(primaryCategory)) {
                        productId = product.productId;
                        productIdToProduct.put(productId, product);
                        if (!productIdToQuantity.containsKey(productId)) {
                            productIdToQuantity.put(productId, 0.0);
                        }
                        qty = productIdToQuantity.get(productId);
                        productIdToQuantity.put(productId, oneProductionRun.getGenericValue().quantityToProduce + qty);
                    }
                }
            }
        }
    }
    productIdToProduct.values().each { product ->
        heightD = product.productHeight;
        height = 0;
        if (heightD) {
            height = (heightD * 1000) as int;
        }

        widthD = product.productWidth;
        width = 0;
        if (widthD) {
            width = (widthD * 1000) as int;
        }
        Dimension dim = new Dimension(width, height);
        if (!dimensionToProducts.containsKey(dim)) {
            dimensionToProducts.put(dim, new ArrayList());
        }
        prodList = (List)dimensionToProducts.get(dim);
        prodList.add(product);
        // tot qty per dimension
        if (!dimensionToQuantity.containsKey(dim)) {
            dimensionToQuantity.put(dim, 0.0);
        }
        qty = dimensionToQuantity[dim];
        dimensionToQuantity.put(dim, productIdToQuantity[product.productId] + qty);
    }
    //
    //
    //
    list1 = [] as ArrayList;
    dimensionToProducts.keySet().each { dim ->
        map1 = [:];
        list1.add(map1);
        map1.width = (dim.getWidth() / 1000);
        map1.height = (dim.getHeight() / 1000);
        map1.quantity = dimensionToQuantity.get(dim);
        list2 = [] as ArrayList;
        map1.products = list2;
        products = (List)dimensionToProducts.get(dim);
        for (int i = 0; i < products.size(); i++) {
            product = products.get(i);
            Map map2 = [:];
            list2.add(map2);
            map2.product = product;
            map2.quantity = productIdToQuantity.get(product.productId);
        }
    }
    context.cuttingList = list1;
}