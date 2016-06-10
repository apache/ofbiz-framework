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
import org.ofbiz.entity.util.EntityTypeUtil
import org.ofbiz.product.inventory.InventoryWorker

if (product) {
    boolean isMarketingPackage = EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG");
    context.isMarketingPackage = (isMarketingPackage? "true": "false");
    //If product is virtual gather summary data from variants
    if (product.isVirtual && "Y".equals(product.isVirtual)) {
        //Get the virtual product feature types
        result = runService('getProductFeaturesByType', [productId : productId, productFeatureApplTypeId : 'SELECTABLE_FEATURE']);
        featureTypeIds = result.productFeatureTypes;
    
        //Get the variants
        result = runService('getAllProductVariants', [productId : productId]);
        variants = result.assocProducts;
        variantIterator = variants.iterator();
        variantInventorySummaries = [];
        while (variantIterator) {
            variant = variantIterator.next();
    
            //create a map of each variant id and inventory summary (all facilities)
            inventoryAvailable = runService('getProductInventoryAvailable', [productId : variant.productIdTo]);
    
            variantInventorySummary = [productId : variant.productIdTo,
                                       availableToPromiseTotal : inventoryAvailable.availableToPromiseTotal,
                                       quantityOnHandTotal : inventoryAvailable.quantityOnHandTotal];
    
            //add the applicable features to the map
            featureTypeIdsIterator = featureTypeIds.iterator();
            while (featureTypeIdsIterator) {
                featureTypeId = featureTypeIdsIterator.next();
                result = runService('getProductFeatures', [productId : variant.productIdTo, type : 'STANDARD_FEATURE', distinct : featureTypeId]);
                variantFeatures = result.productFeatures;
                if (variantFeatures) {
                    //there should only be one result in this collection
                    variantInventorySummary.put(featureTypeId, variantFeatures.get(0));
                }
            }
            variantInventorySummaries.add(variantInventorySummary);
        }
        context.featureTypeIds = featureTypeIds;
        context.variantInventorySummaries = variantInventorySummaries;
    } else { //Gather information for a non virtual product
        quantitySummaryByFacility = [:];
        manufacturingInQuantitySummaryByFacility = [:];
        manufacturingOutQuantitySummaryByFacility = [:];
        // The warehouse list is selected
        showAllFacilities = parameters.showAllFacilities;
        if (showAllFacilities && "Y".equals(showAllFacilities)) {
            facilityList = from("Facility").queryList();
        } else {
            facilityList = from("ProductFacility").where("productId", productId).queryList();
        }
        facilityIterator = facilityList.iterator();
        dispatcher = request.getAttribute("dispatcher");
        Map contextInput = null;
        Map resultOutput = null;
    
        // inventory quantity summary by facility: For every warehouse the product's atp and qoh
        // are obtained (calling the "getInventoryAvailableByFacility" service)
        while (facilityIterator) {
            facility = facilityIterator.next();
            resultOutput = runService('getInventoryAvailableByFacility', [productId : productId, facilityId : facility.facilityId]);
    
            quantitySummary = [:];
            quantitySummary.facilityId = facility.facilityId;
            quantitySummary.totalQuantityOnHand = resultOutput.quantityOnHandTotal;
            quantitySummary.totalAvailableToPromise = resultOutput.availableToPromiseTotal;
    
            // if the product is a MARKETING_PKG_AUTO/PICK, then also get the quantity which can be produced from components
            if (isMarketingPackage) {
                resultOutput = runService('getMktgPackagesAvailable', [productId : productId, facilityId : facility.facilityId]);
                quantitySummary.mktgPkgQOH = resultOutput.quantityOnHandTotal;
                quantitySummary.mktgPkgATP = resultOutput.availableToPromiseTotal;
            }
    
            quantitySummaryByFacility.put(facility.facilityId, quantitySummary);
        }
        
        productInventoryItems = from("InventoryItem").where("productId", productId).orderBy("facilityId", "-datetimeReceived", "-inventoryItemId").queryList();
    
        // TODO: get all incoming shipments not yet arrived coming into each facility that this product is in, use a view entity with ShipmentAndItem
        findIncomingShipmentsConds = [];
    
        findIncomingShipmentsConds.add(EntityCondition.makeCondition('productId', EntityOperator.EQUALS, productId));
    
        findIncomingShipmentsTypeConds = [];
        findIncomingShipmentsTypeConds.add(EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, "INCOMING_SHIPMENT"));
        findIncomingShipmentsTypeConds.add(EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, "PURCHASE_SHIPMENT"));
        findIncomingShipmentsTypeConds.add(EntityCondition.makeCondition("shipmentTypeId", EntityOperator.EQUALS, "SALES_RETURN"));
        findIncomingShipmentsConds.add(EntityCondition.makeCondition(findIncomingShipmentsTypeConds, EntityOperator.OR));
    
        findIncomingShipmentsStatusConds = [];
        findIncomingShipmentsStatusConds.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_DELIVERED"));
        findIncomingShipmentsStatusConds.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "SHIPMENT_CANCELLED"));
        findIncomingShipmentsStatusConds.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PURCH_SHIP_RECEIVED"));
        findIncomingShipmentsConds.add(EntityCondition.makeCondition(findIncomingShipmentsStatusConds, EntityOperator.AND));
    
        findIncomingShipmentsStatusCondition = EntityCondition.makeCondition(findIncomingShipmentsConds, EntityOperator.AND);
        incomingShipmentAndItems = from("ShipmentAndItem").where(findIncomingShipmentsStatusCondition).orderBy("-estimatedArrivalDate").queryList();
        incomingShipmentAndItemIter = incomingShipmentAndItems.iterator();
        while (incomingShipmentAndItemIter) {
            incomingShipmentAndItem = incomingShipmentAndItemIter.next();
            facilityId = incomingShipmentAndItem.destinationFacilityId;
    
            quantitySummary = quantitySummaryByFacility.get(facilityId);
            if (!quantitySummary) {
                quantitySummary = [:];
                quantitySummary.facilityId = facilityId;
                quantitySummaryByFacility.facilityId = quantitySummary;
            }
    
            incomingShipmentAndItemList = quantitySummary.incomingShipmentAndItemList;
            if (!incomingShipmentAndItemList) {
                incomingShipmentAndItemList = [];
                quantitySummary.incomingShipmentAndItemList = incomingShipmentAndItemList;
            }
    
            incomingShipmentAndItemList.add(incomingShipmentAndItem);
        }
    
        // --------------------
        // Production Runs
        resultOutput = runService('getProductManufacturingSummaryByFacility',
                       [productId : productId, userLogin : userLogin]);
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
}
