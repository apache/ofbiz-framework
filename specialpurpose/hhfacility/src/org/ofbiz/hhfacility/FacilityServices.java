/*******************************************************************************
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
 *******************************************************************************/

package org.ofbiz.hhfacility;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class FacilityServices {

    public static final String module = FacilityServices.class.getName();

    public static Map findProductsById(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String facilityId = (String) context.get("facilityId");
        String idValue = (String) context.get("idValue");
        GenericValue product = null;
        List productsFound = null;
        
        GenericValue productItem = null;
        if (UtilValidate.isNotEmpty(idValue)) {
            // First lets find the productId from the Sku(s)
            try {
                productsFound = delegator.findByAnd("GoodIdentificationAndProduct",
                   UtilMisc.toMap("idValue", idValue), UtilMisc.toList("productId"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                throw new GeneralRuntimeException(e.getMessage());
            }
        }

        // Now do a direct lookup..
        productItem = null;
        try {
            productItem = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", idValue));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }
        if (productItem != null) {
            if (productsFound == null) {
                productsFound = new ArrayList();
            }
            productsFound.add(productItem);
        }

        // Send back the results
        Map result = ServiceUtil.returnSuccess();
        if (productsFound != null && productsFound.size() > 0) {
            result.put("productList", productsFound);
        }
        return result;
    }    

    public static Map fixProductNegativeQOH(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String facilityId = (String) context.get("facilityId");
        String productId = (String) context.get("productId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // Now we build a list of inventory items against the facility and product.
        // todo: change this to a select from inv_items where productId and facilityId matches distinct (locationSeqId).
        List invItemList = null;
        try {
            invItemList = delegator.findByAnd("InventoryItem",
                UtilMisc.toMap("productId", productId, "facilityId", facilityId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e.getMessage());
        }

        Map locations = new HashMap();
        Iterator invItemListIter = invItemList.iterator();
        while (invItemListIter.hasNext()) {
            GenericValue invItem = (GenericValue)invItemListIter.next();
            if ( invItem != null) {
                int qoh = ((Double)invItem.get("quantityOnHandTotal")).intValue();
                
                if ( qoh < 0 ) {
                    // Got a negative qoh so lets balance if off to zero.
                    Map contextInput = UtilMisc.toMap("userLogin", userLogin, "inventoryItemId", invItem.get("inventoryItemId"), 
                            "varianceReasonId", "VAR_LOST", "availableToPromiseVar", new Double(qoh*-1), 
                            "quantityOnHandVar", new Double(qoh*-1), "comments", "QOH < 0 stocktake correction");
                    try {
                        dispatcher.runSync("createPhysicalInventoryAndVariance",contextInput);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "fixProductNegativeQOH failed on createPhysicalInventoryAndVariance invItemId"+invItem.get("inventoryItemId"), module);
                        return ServiceUtil.returnError("fixProductNegativeQOH failed on createPhysicalInventoryAndVariance invItemId"+invItem.get("inventoryItemId"));
                    }
                }
            }
        }
        Map result = ServiceUtil.returnSuccess();
        return result;
    }

    public static Map updateProductStocktake(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String facilityId = (String) context.get("facilityId");
        String productId = (String) context.get("productId");
        String locationSeqId = (String) context.get("locationSeqId");
        String locationSeqIdNew = (String) context.get("locationSeqIdNew");
        Double quantity = (Double) context.get("quantity");
        if ( UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(facilityId) ) {
            return ServiceUtil.returnError("productId or facilityId not found");
        }

        // First identify the location and get a list of inventoryItemIds for that location.
        if ( UtilValidate.isEmpty(locationSeqId) ) {
            // Assume this is the null field version
            locationSeqId = "nullField";
        }

        // Get the current atp/qoh values for the location(s).
        Map contextInput = UtilMisc.toMap("productId",productId, "facilityId", facilityId, "locationSeqId", locationSeqId);
        Map invAvailability = null;
        try {
            invAvailability = dispatcher.runSync("getInventoryAvailableByLocationSeq",contextInput);
        } catch (GenericServiceException e) {
            Debug.logError(e, "updateProductStocktake failed getting inventory counts", module);
            return ServiceUtil.returnError("updateProductStocktake failed getting inventory counts");
        }
        int atp = ((Double)invAvailability.get("availableToPromiseTotal")).intValue();
        int qoh = ((Double)invAvailability.get("quantityOnHandTotal")).intValue();
        if ( quantity.intValue() == qoh ) {
            // No change required.
            Debug.logInfo("updateProductStocktake No change required quantity("+quantity+") = qoh("+qoh+")", module);
            return ServiceUtil.returnSuccess();
        }

        // Now get the inventory items that are found for that location, facility and product
        List invItemList = null;
        try {
            invItemList = delegator.findByAnd("InventoryItem",
                UtilMisc.toMap("productId", productId, "facilityId", facilityId, "locationSeqId", locationSeqId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "updateProductStocktake failed getting inventory items", module);
            return ServiceUtil.returnError("updateProductStocktake failed getting inventory items");
        }

        Iterator invItemListIter = invItemList.iterator();
        while (invItemListIter.hasNext()) {
            GenericValue invItem = (GenericValue)invItemListIter.next();
            String locationFound = invItem.getString("locationSeqId");
            Debug.logInfo("updateProductStocktake: InvItemId("+invItem.getString("inventoryItemId")+")", module);
            if ( locationFound == null ) {
                locationFound = "nullField";
            }
        }
        // Check if there is a request to change the locationSeqId
        GenericValue product = null;
        try {
            Map resultOutput = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, 
                    "facilityId", facilityId));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Inventory atp/qoh lookup problem [" + e.getMessage() + "]");
        }

/*
        try {
            inventoryTransfer = delegator.findByPrimaryKey("InventoryTransfer", 
                    UtilMisc.toMap("inventoryTransferId", inventoryTransferId));
            inventoryItem = inventoryTransfer.getRelatedOne("InventoryItem");
            destinationFacility = inventoryTransfer.getRelatedOne("ToFacility");
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Inventory Item/Transfer lookup problem [" + e.getMessage() + "]");
        }
        
        if (inventoryTransfer == null || inventoryItem == null) {
            return ServiceUtil.returnError("ERROR: Lookup of InventoryTransfer and/or InventoryItem failed!");
        }
            
        String inventoryType = inventoryItem.getString("inventoryItemTypeId");
        
        // set the fields on the transfer record            
        if (inventoryTransfer.get("receiveDate") == null) {
            inventoryTransfer.set("receiveDate", UtilDateTime.nowTimestamp());
        }
            
        if (inventoryType.equals("NON_SERIAL_INV_ITEM")) { 
            // add an adjusting InventoryItemDetail so set ATP back to QOH: ATP = ATP + (QOH - ATP), diff = QOH - ATP
            double atp = inventoryItem.get("availableToPromiseTotal") == null ? 0 : inventoryItem.getDouble("availableToPromiseTotal").doubleValue();
            double qoh = inventoryItem.get("quantityOnHandTotal") == null ? 0 : inventoryItem.getDouble("quantityOnHandTotal").doubleValue();
            Map createDetailMap = UtilMisc.toMap("availableToPromiseDiff", new Double(qoh - atp), 
                    "inventoryItemId", inventoryItem.get("inventoryItemId"), "userLogin", userLogin);
            try {
                Map result = dctx.getDispatcher().runSync("createInventoryItemDetail", createDetailMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError("Inventory Item Detail create problem in complete inventory transfer", null, null, result);
                }
            } catch (GenericServiceException e1) {
                return ServiceUtil.returnError("Inventory Item Detail create problem in complete inventory transfer: [" + e1.getMessage() + "]");
            }
            try {
                inventoryItem.refresh();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError("Inventory refresh problem [" + e.getMessage() + "]");
            }
        } else if (inventoryType.equals("SERIALIZED_INV_ITEM")) {
            inventoryItem.set("statusId", "INV_AVAILABLE");
        }

        // set the fields on the item
        Map updateInventoryItemMap = UtilMisc.toMap("inventoryItemId", inventoryItem.getString("inventoryItemId"),
                                                    "facilityId", inventoryTransfer.get("facilityIdTo"),
                                                    "containerId", inventoryTransfer.get("containerIdTo"),
                                                    "locationSeqId", inventoryTransfer.get("locationSeqIdTo"),
                                                    "userLogin", userLogin);
        // if the destination facility's owner is different 
        // from the inventory item's ownwer, 
        // the inventory item is assigned to the new owner.
        if (destinationFacility != null && destinationFacility.get("ownerPartyId") != null) {
            String fromPartyId = inventoryItem.getString("ownerPartyId");
            String toPartyId = destinationFacility.getString("ownerPartyId");
            if (fromPartyId == null || !fromPartyId.equals(toPartyId)) {
                updateInventoryItemMap.put("ownerPartyId", toPartyId);
            }
        }
        try {
            Map result = dctx.getDispatcher().runSync("updateInventoryItem", updateInventoryItemMap);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError("Inventory item store problem", null, null, result);
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError("Inventory item store problem [" + exc.getMessage() + "]");
        }

        // set the inventory transfer record to complete
        inventoryTransfer.set("statusId", "IXF_COMPLETE");
        
        // store the entities
        try {
            inventoryTransfer.store();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Inventory store problem [" + e.getMessage() + "]");
        }
         */
        return ServiceUtil.returnSuccess();
    }    
}
