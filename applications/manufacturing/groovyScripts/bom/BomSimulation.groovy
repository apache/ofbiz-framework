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

import org.ofbiz.base.util.Debug;
import org.ofbiz.manufacturing.bom.BOMNode;

tree = request.getAttribute("tree");
currencyUomId = parameters.currencyUomId;
facilityId = parameters.facilityId;

if (tree) {
    treeArray = [];
    treeQty = [:];

    tree.print(treeArray);
    tree.sumQuantities(treeQty);

    context.tree = treeArray;
    Iterator treeQtyIt = treeQty.values().iterator();
    productsData = [];
    grandTotalCost = 0.0;
    while (treeQtyIt) {
        BOMNode node = (BOMNode)treeQtyIt.next();
        unitCost = null;
        totalCost = null;
        qoh = null;
        // The standard cost is retrieved
        try {
            outMap = [:];
            if (currencyUomId) {
                outMap = runService('getProductCost', [productId : node.getProduct().productId,
                                                                             currencyUomId : currencyUomId,
                                                                             costComponentTypePrefix : "EST_STD",
                                                                             userLogin : userLogin]);
                unitCost = outMap.productCost;
                totalCost = unitCost * node.getQuantity();
                grandTotalCost = grandTotalCost + totalCost ?: 0;
            }
            if (facilityId) {
                outMap = runService('getInventoryAvailableByFacility', [productId : node.getProduct().productId,
                                                                                              facilityId : facilityId,
                                                                                              userLogin : userLogin]);
                qoh = outMap.quantityOnHandTotal;
            }
        } catch (Exception e) {
            Debug.logError("Error retrieving bom simulation data: " + e.getMessage(), "BomSimulation");
        }
        productsData.add([node : node, unitCost : unitCost, totalCost : totalCost, qoh : qoh]);
    }
    context.productsData = productsData;
    context.grandTotalCost = grandTotalCost;
}
