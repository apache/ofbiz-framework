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
import org.ofbiz.product.inventory.InventoryWorker

//If product is virtual gather summary data from variants
if (product.isVirtual != null && "Y".equals(product.isVirtual)) {
    //Get the virtual product feature types
    result = dispatcher.runSync("getProductFeaturesByType", ['productId' : productId, 'productFeatureApplTypeId' : 'SELECTABLE_FEATURE']);
    featureTypeIds = result.productFeatureTypes;
    
    //Get the variants
    result = dispatcher.runSync("getAllProductVariants", ['productId' : productId]);
    variants = result.assocProducts;
    variantIterator = variants.iterator();
    variantInventorySummaries = new ArrayList();
    while(variantIterator.hasNext()) {
        variant = variantIterator.next();

        //create a map of each variant id and inventory summary (all facilities)
        inventoryAvailable = dispatcher.runSync("getProductInventoryAvailable", ['productId' : variant.productIdTo]);

        variantInventorySummary = ['productId' :  variant.productIdTo, 
                                   'availableToPromiseTotal' : inventoryAvailable.availableToPromiseTotal,
                                   'quantityOnHandTotal' : inventoryAvailable.quantityOnHandTotal];

        //add the applicable features to the map
        featureTypeIdsIterator = featureTypeIds.iterator();
        while (featureTypeIdsIterator.hasNext()) {
            featureTypeId = featureTypeIdsIterator.next();
            result = dispatcher.runSync("getProductFeatures", ['productId' : variant.productIdTo, 'type' : 'STANDARD_FEATURE', 'distinct' : featureTypeId]);
            variantFeatures = result.productFeatures;
            if (variantFeatures.size() > 0) {
                //there should only be one result in this collection
                variantInventorySummary.put(featureTypeId, variantFeatures.get(0));
            }
        }
        variantInventorySummaries.add(variantInventorySummary);
    }
    context.featureTypeIds = featureTypeIds;
    context.variantInventorySummaries = variantInventorySummaries;
} else { //Gather information for a non virtual product
    quantitySummaryByFacility = new HashMap();
    manufacturingInQuantitySummaryByFacility = new HashMap();
    manufacturingOutQuantitySummaryByFacility = new HashMap();
    // The warehouse list is selected
    showAllFacilities = parameters.showAllFacilities;
    if (showAllFacilities != null && showAllFacilities.equals("Y")) {
        facilityList = delegator.findList("Facility", null, null, null, null, false);
    } else {
        facilityList = delegator.findByAnd("ProductFacility", ['productId' : productId]);
    }
    facilityIterator = facilityList.iterator();
    dispatcher = request.getAttribute("dispatcher");
    Map contextInput = null;
    Map resultOutput = null;
    
    // inventory quantity summary by facility: For every warehouse the product's atp and qoh 
    // are obtained (calling the "getInventoryAvailableByFacility" service)
    while (facilityIterator.hasNext()) {
        facility = facilityIterator.next();
        resultOutput = dispatcher.runSync("getInventoryAvailableByFacility", ['productId' : productId, 'facilityId' : facility.facilityId]);
        
        quantitySummary = new HashMap();
        quantitySummary.put("facilityId", facility.facilityId);
        quantitySummary.put("totalQuantityOnHand", resultOutput.quantityOnHandTotal);
        quantitySummary.put("totalAvailableToPromise", resultOutput.availableToPromiseTotal);

        // if the product is a MARKETING_PKG_AUTO/PICK, then also get the quantity which can be produced from components
        if ("MARKETING_PKG_AUTO".equals(product.productTypeId) ||
            "MARKETING_PKG_PICK".equals(product.productTypeId)) {
            resultOutput = dispatcher.runSync("getMktgPackagesAvailable", ['productId' : productId, 'facilityId' : facility.facilityId]);
            quantitySummary.put("mktgPkgQOH", resultOutput.quantityOnHandTotal);
            quantitySummary.put("mktgPkgATP", resultOutput.availableToPromiseTotal);
        }
        
        quantitySummaryByFacility.put(facility.facilityId, quantitySummary);
    }

    productInventoryItems = delegator.findByAnd("InventoryItem",
            ['productId' : productId],
            ['facilityId', '-datetimeReceived', '-inventoryItemId']);

    // TODO: get all incoming shipments not yet arrived coming into each facility that this product is in, use a view entity with ShipmentAndItem
    findIncomingShipmentsConds = new LinkedList();

    findIncomingShipmentsConds.add(new EntityExpr('productId', EntityOperator.EQUALS, productId));

    findIncomingShipmentsTypeConds = new LinkedList();
    findIncomingShipmentsTypeConds.add(new EntityExpr("shipmentTypeId", EntityOperator.EQUALS, "INCOMING_SHIPMENT"));
    findIncomingShipmentsTypeConds.add(new EntityExpr("shipmentTypeId", EntityOperator.EQUALS, "PURCHASE_SHIPMENT"));
    findIncomingShipmentsTypeConds.add(new EntityExpr("shipmentTypeId", EntityOperator.EQUALS, "SALES_RETURN"));
    findIncomingShipmentsConds.add(new EntityConditionList(findIncomingShipmentsTypeConds, EntityOperator.OR));

    findIncomingShipmentsStatusConds = new LinkedList();
    findIncomingShipmentsStatusConds.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_DELIVERED"));
    findIncomingShipmentsStatusConds.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_CANCELLED"));
    findIncomingShipmentsStatusConds.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PURCH_SHIP_RECEIVED"));
    findIncomingShipmentsConds.add(new EntityConditionList(findIncomingShipmentsStatusConds, EntityOperator.AND));

    findIncomingShipmentsStatusCondition = new EntityConditionList(findIncomingShipmentsConds, EntityOperator.AND);
    incomingShipmentAndItems = delegator.findList("ShipmentAndItem", findIncomingShipmentsStatusCondition, null, ['-estimatedArrivalDate'], null, false);
    incomingShipmentAndItemIter = incomingShipmentAndItems.iterator();
    while (incomingShipmentAndItemIter.hasNext()) {
        incomingShipmentAndItem = incomingShipmentAndItemIter.next();
        facilityId = incomingShipmentAndItem.destinationFacilityId;

        quantitySummary = quantitySummaryByFacility.get(facilityId);
        if (quantitySummary == null) {
            quantitySummary = new HashMap();
            quantitySummary.put("facilityId", facilityId);
            quantitySummaryByFacility.put(facilityId, quantitySummary);
        }

        incomingShipmentAndItemList = quantitySummary.incomingShipmentAndItemList;
        if (incomingShipmentAndItemList == null) {
            incomingShipmentAndItemList = new LinkedList();
            quantitySummary.put("incomingShipmentAndItemList", incomingShipmentAndItemList);
        }

        incomingShipmentAndItemList.add(incomingShipmentAndItem);
    }

    // --------------------
    // Production Runs
    resultOutput = dispatcher.runSync("getProductManufacturingSummaryByFacility", 
                   ['productId' : productId, 'userLogin' : userLogin]);
    // incoming products
    manufacturingInQuantitySummaryByFacility = resultOutput.summaryInByFacility;
    // outgoing products (materials)
    manufacturingOutQuantitySummaryByFacility = resultOutput.summaryOutByFacility;

    showEmpty = "true".equals(request.getParameter("showEmpty"));

    // Find oustanding purchase orders for this item. 
    purchaseOrders = InventoryWorker.getOutstandingPurchaseOrders(productId, delegator);

    context.productInventoryItems = productInventoryItems;
    context.quantitySummaryByFacility = quantitySummaryByFacility;
    context.manufacturingInQuantitySummaryByFacility = manufacturingInQuantitySummaryByFacility;
    context.manufacturingOutQuantitySummaryByFacility = manufacturingOutQuantitySummaryByFacility;
    context.showEmpty = showEmpty;
    context.purchaseOrders = purchaseOrders;
}