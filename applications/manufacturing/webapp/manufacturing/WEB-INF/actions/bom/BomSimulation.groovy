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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.manufacturing.bom.BOMNode;

tree = request.getAttribute("tree");
String currencyUomId = request.getParameter("currencyUomId");
String facilityId = request.getParameter("facilityId");

if (tree != null) {
    List treeArray = new ArrayList();
    Map treeQty = new HashMap();

    tree.print(treeArray);
    tree.sumQuantities(treeQty);

    context.put("tree", treeArray);
    Iterator treeQtyIt = treeQty.values().iterator();
    List productsData = new ArrayList();
    Double grandTotalCost = null;
    while (treeQtyIt.hasNext()) {
        BOMNode node = (BOMNode)treeQtyIt.next();
        Double unitCost = null;
        Double totalCost = null;
        Double qoh = null;
        // The standard cost is retrieved
        try {
            Map outMap = null;
            if (UtilValidate.isNotEmpty(currencyUomId)) {
                outMap = dispatcher.runSync("getProductCost", UtilMisc.toMap("productId", node.getProduct().getString("productId"),
                                                                             "currencyUomId", currencyUomId,
                                                                             "costComponentTypePrefix", "EST_STD",
                                                                             "userLogin", userLogin));
                unitCost = (Double)outMap.get("productCost");
                totalCost = unitCost * node.getQuantity();
                if (grandTotalCost == null) {
                    grandTotalCost = 0;
                }
                grandTotalCost = grandTotalCost + totalCost;
            }
            if (UtilValidate.isNotEmpty(facilityId)) {
                outMap = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", node.getProduct().getString("productId"),
                                                                                              "facilityId", facilityId,
                                                                                              "userLogin", userLogin));
                qoh = (Double)outMap.get("quantityOnHandTotal");
            }
        } catch(GenericServiceException gse) {}
        productsData.add(UtilMisc.toMap("node", node, "unitCost", unitCost, "totalCost", totalCost, "qoh", qoh));
    }
    context.put("productsData", productsData);
    context.put("grandTotalCost", grandTotalCost);
}
